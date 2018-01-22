package com.aubet.francois.VacumBotControler.activities;

import android.graphics.Bitmap;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.aubet.francois.VacumBotControler.R;
import com.aubet.francois.VacumBotControler.activities.MainActivity;
import com.aubet.francois.VacumBotControler.model.DrawablePoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.graphics.Color.argb;
import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;


public class MapActivity extends AppCompatActivity implements View.OnClickListener  {
 	ImageView miamageview;

	Button btnWall, btnOpen, btnForbid, btnClean;

    private final static int BIG_CIRCLE_SIZE = 120;
	private final static int FINGER_CIRCLE_SIZE = 20;
	private final static int POINT_MAP = 2;

	private static int changingMode = 0;

	private int[][] temporaryMap = new int [MainActivity.mapMatrixSize][MainActivity.mapMatrixSize];

	int width, heigth;
	int factor = 1;
	private boolean resent = false;



    private int motorLeft = 0;
    private int motorRight = 0;

    float radiuscircle = 160; //200; (old value)


	Bitmap bitmap;


	int comptResent = 0;


    private boolean needtheshow = true;
    int[] trackValues = new int[(MainActivity.mapMatrixSize) * (MainActivity.mapMatrixSize)];

    double decayFactor = 9;

    ShowTheEvents theShow;

	Button btn_return;

	int[] rgbValues;





    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);



		LinearLayout layout1 = (LinearLayout) findViewById(R.id.toDrawMap);


		width = (MainActivity.mapMatrixSize*factor);
		heigth = (MainActivity.mapMatrixSize*factor);

		//define the array size
		rgbValues = new int[width * heigth];


		for (int i = 0; i < width; i++) {
			for (int j = 0; j < heigth; j++) {
				rgbValues[(j * width) + i] = Color.WHITE;
			}
		}

		btnWall = (Button) findViewById(R.id.btnWall);
		btnWall.setOnClickListener(this);

		btnOpen = (Button) findViewById(R.id.btnOpen);
		btnOpen.setOnClickListener(this);

		btnForbid = (Button) findViewById(R.id.btnForbid);
		btnForbid.setOnClickListener(this);

		btnClean = (Button) findViewById(R.id.btnClean);
		btnClean.setOnClickListener(this);



		miamageview = (ImageView) findViewById(R.id.matrix);

		miamageview.setOnTouchListener(new View.OnTouchListener()		{

			@Override
			public boolean onTouch(View v, MotionEvent event){

				// the event values schould be scaled
				int dispWidth = (int) Math.round(miamageview.getRight()-miamageview.getLeft());
				int dispHeight = (int) Math.round(miamageview.getBottom()-miamageview.getTop());

				float evX = event.getX();
				float evY = event.getY();

				int index_x = (int) (0.5 + evX * MainActivity.mapMatrixSize / dispWidth);
				int index_y = (int) (0.5 + evY * MainActivity.mapMatrixSize / dispHeight);
				//System.out.println("evX :"+evX);
				//System.out.println("evY :"+evY);

				//System.out.println("dispWidth :"+dispWidth);

				//System.out.println("index_x :"+index_x);
				//System.out.println("index_y :"+index_y);

				int sizeOfInvalidate = 4;

				//invalidate( (int)(evX - sizeOfInvalidate),(int)(evY - sizeOfInvalidate), (int)(evX + sizeOfInvalidate) ,(int)(evY + sizeOfInvalidate) );
				//invalidate();
				for(int i = index_x - sizeOfInvalidate; i < index_x + sizeOfInvalidate; i++) {
					for (int j = index_y - sizeOfInvalidate; j < index_y + sizeOfInvalidate; j++) {
						if(i < MainActivity.mapMatrixSize && j < MainActivity.mapMatrixSize && i > 0 && j > 0) {
							if((MainActivity.theMapMatrix[i][j] == 1) && (changingMode == 3 || changingMode == 2) ){
							} else {
								MainActivity.theMapMatrix[i][j] = changingMode;
								MainActivity.sendCommand(i + "," + j + "," + changingMode);
							}
						}
					}
				}
				return true;
			}
		});


		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				bitmap = Bitmap.createBitmap(rgbValues, width, heigth, Bitmap.Config.ARGB_8888);
				miamageview.setImageBitmap(bitmap);
			}
		});



		theShow = new ShowTheEvents(bitmap);
		theShow.start(); //start the show!

	}

	// btnWall, btnOpen, btnForbid, btnClean;

    public void onClick(View v) {
            switch (v.getId()) {
            case R.id.btnWall:
            	changingMode = 1;
                break;
            case R.id.btnOpen:
            	changingMode = 0;
                break;
            case R.id.btnForbid:
				changingMode = 2;
            	break;
            case R.id.btnClean:
            	changingMode = 3;
                break;
            default:
                break;
            }
    }

    @Override
    protected void onResume() {
    // Register a listener for the sensor.
    	super.onResume();
    }

    @Override
    protected void onPause() {
    // Be sure to unregister the sensor when the activity pauses.
 	   	super.onPause();
		needtheshow = false;

		try {
			theShow.join();
		} catch (Exception e){

		}

		//super.onDestroy();
    }

    @Override
    public void onBackPressed() {
		needtheshow = false;

		try {
			theShow.join();
		} catch (Exception e){

		}

        this.finish();
        super.onBackPressed();
	}



	//+++++++++++++++++++++++++++++++++++              Thread to show the DVS events           +++++++++++++++++++++++++++
/*
    This thread is basically a while loop that looks at the values of the event recieved by the robotsocketmanager.
    It shows this values and reset them to black after showing them.
    I made here again a tampon array to change the value there, it is better for performance.
 */
	private class ShowTheEvents extends Thread {
		Bitmap bitmap;
		ImageView miamageview = (ImageView) findViewById(R.id.matrix);
		int[] rgbValuesShow = new int[(MainActivity.mapMatrixSize * 5) * (MainActivity.mapMatrixSize * 5)];       // the tampon array of colors

		public ShowTheEvents(Bitmap bitmap) {
			this.bitmap = bitmap;
		}


		@Override
		public void run() {
			int xint, yint, tmp;
			miamageview = (ImageView) findViewById(R.id.matrix);
			bitmap = Bitmap.createBitmap(width, heigth, Bitmap.Config.ARGB_8888);
			int redc, greenc, bluec;

			for (int i = 0; i < width; i++) {
				for (int j = 0; j < heigth; j++) {
					rgbValuesShow[(j * width) + i] = Color.WHITE;
				}
			}



			while(needtheshow){
				try{
					rgbValuesShow = rgbValues;
				} catch(Exception e){}
				for(int i = 0; i < width - 1; i++){
					for(int j = 0; j < width - 1; j++){

						tmp = rgbValuesShow[(j * width) + i];

							if(MainActivity.theMapMatrix[i / factor][j/ factor] != temporaryMap[i/ factor][j/ factor]){

								//System.out.println(x + " , "+ y);

								switch (MainActivity.theMapMatrix[i/ factor][j/ factor]){
									case 0:
										rgbValues[(j * width) + i] = Color.WHITE;
										break;
									case 1:
										rgbValues[(j * width) + i] = Color.BLACK;
										//pointPaint.setColor(Color.RED);
										break;
									case 2:
										rgbValues[(j * width) + i] = Color.RED;
										//pointPaint.setColor(Color.GREEN);
										break;
									case 3:
										rgbValues[(j * width) + i] = Color.BLUE;
										//pointPaint.setColor(Color.GREEN);
										break;
									default:

								}


								temporaryMap[i/ factor][j/ factor] = MainActivity.theMapMatrix[i/ factor][j/ factor];
								//pointList.add(i);

						}

						//    this is used to slowly reduce the color of each event if the
						/*if(tmp != Color.BLACK) {
							redc = red(tmp);
							redc = (int)( redc / decayFactor);
							greenc = green(tmp);
							greenc = (int)( greenc / decayFactor);
							bluec = blue(tmp);
							bluec = (int)( bluec / decayFactor);

							tmp = argb(0xFF, redc, greenc, bluec);
							rgbValuesShow[(j * width) + i] = tmp;
						}*/
					}
				}


				try {
					rgbValues = rgbValuesShow;
					Thread.sleep(1);
				} catch (Exception e) {}

				// showing the values we got
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// ? miamageview = (ImageView) findViewById(R.id.matrix);
						try {
							bitmap.setPixels(rgbValues, 0, width, 0 ,0, width, heigth);
							miamageview.setImageBitmap(bitmap);
						} catch (Exception e) {}
					}
				});





			}

		}

	}






class MyView extends View {

		Paint fingerPaint, borderPaint, textPaint, pointPaint;

		List<DrawablePoint> pointList = new ArrayList<DrawablePoint>();

		Canvas theCanvas;

        int dispWidth;
        int dispHeight;

        float x;
        float y;

        float xcirc;
        float ycirc;

        boolean drag = false;

		public MyView(Context context) {
        	super(context);

			/*for(int i = 0; i < MainActivity.mapMatrixSize; i++){
				for(int j = 0; j < MainActivity.mapMatrixSize; j++){
					if(MainActivity.theMapMatrix[i][j] != 0){

						//pointList.add(i);
					}
				}
			}*/

			for(int i = 0; i < MainActivity.mapMatrixSize; i++){
				for(int j = 0; j < MainActivity.mapMatrixSize; j++){
					temporaryMap[i][j] = 0;
				}
			}

            pointPaint = new Paint();
            pointPaint.setAntiAlias(true);
            pointPaint.setColor(Color.RED);


        	fingerPaint = new Paint();
        	fingerPaint.setAntiAlias(true);
        	fingerPaint.setColor(Color.RED);

        	borderPaint = new Paint();
        	borderPaint.setColor(Color.BLUE);
        	borderPaint.setAntiAlias(true);
        	borderPaint.setStyle(Style.STROKE);
        	borderPaint.setStrokeWidth(3);

	        textPaint = new Paint();
	        textPaint.setColor(Color.WHITE);
	        textPaint.setStyle(Style.FILL);
	        textPaint.setColor(Color.BLACK);
	        textPaint.setTextSize(14);
        }






        protected void onDraw(Canvas canvas) {

			theCanvas = canvas;

			System.out.println("Right: " + this.getRight());
			System.out.println("Left: " + this.getLeft());
			System.out.println("Bottom: " + this.getBottom());
			System.out.println("Top: " + this.getTop());

			dispWidth = (int) Math.round(this.getRight()-this.getLeft());;
			dispHeight = (int) Math.round(this.getBottom()-this.getTop());
			//System.out.println(dispWidth + " , "+ dispHeight);

			for(int i = 0; i < MainActivity.mapMatrixSize; i++){
				for(int j = 0; j < MainActivity.mapMatrixSize; j++){
					if(MainActivity.theMapMatrix[i][j] != temporaryMap[i][j]){

						x = (int) ((j+0.5) * dispWidth / MainActivity.mapMatrixSize);
						y = (int) ((i+0.5) * dispHeight / MainActivity.mapMatrixSize);
						//System.out.println(x + " , "+ y);

						switch (MainActivity.theMapMatrix[i][j]){
							case 1:
								pointPaint.setColor(Color.RED);
								break;
							case 2:
								pointPaint.setColor(Color.GREEN);
								break;
							default:

						}

						canvas.drawCircle(x, y, POINT_MAP, pointPaint);

						temporaryMap[i][j] = MainActivity.theMapMatrix[i][j];
						//pointList.add(i);
					}
				}
			}


        }






        @Override
        public boolean onTouchEvent(MotionEvent event) {


            /*
            den touch event muss "quantisiert" werden.

            Es muss geschaut sein wo es berührt wird und entsprechen in die matrix das ändern.

             */


        	float evX = event.getX();
        	float evY = event.getY();

        	xcirc = event.getX() - dispWidth;
        	ycirc = event.getY() - dispHeight;

        	// float radius = (float) Math.sqrt(Math.pow(Math.abs(xcirc),2)+Math.pow(Math.abs(ycirc),2));



			// Quantisieren:
			// here we get the matrix position depending on the finger position

			int index_y = (int) (0.5 + evX * MainActivity.mapMatrixSize / dispWidth);
			int index_x = (int) (0.5 + evY * MainActivity.mapMatrixSize / dispHeight);

			MainActivity.theMapMatrix[index_x][index_y] = changingMode;

			System.out.println("evX :"+evX);
			System.out.println("evY :"+evY);
			System.out.println("index_x :"+index_x);
			System.out.println("index_y :"+index_y);

			int sizeOfInvalidate = 20;

			invalidate( (int)(evX - sizeOfInvalidate),(int)(evY - sizeOfInvalidate), (int)(evX + sizeOfInvalidate) ,(int)(evY + sizeOfInvalidate) );
			//invalidate();
			for(int i = index_x - 5; i < index_x + 5; i++) {
				for (int j = index_y - 5; j < index_y + 5; j++) {
					MainActivity.theMapMatrix[i][j] = changingMode;

				}
			}



			/*for(int i = 0; i < MainActivity.mapMatrixSize; i++){
				for(int j = 0; j < MainActivity.mapMatrixSize; j++){
					if(MainActivity.theMapMatrix[i][j] != temporaryMap[i][j]){

						x = (int) ((j+0.5) * dispWidth / MainActivity.mapMatrixSize);
						y = (int) ((i+0.5) * dispHeight / MainActivity.mapMatrixSize);
						//System.out.println(x + " , "+ y);

						switch (MainActivity.theMapMatrix[i][j]){
							case 0:
								pointPaint.setColor(Color.WHITE);
								break;
							case 1:
								pointPaint.setColor(Color.RED);
								break;
							case 2:
								pointPaint.setColor(Color.GREEN);
								break;
							default:

						}

						theCanvas.drawCircle(x, y, POINT_MAP, pointPaint);

						temporaryMap[i][j] = MainActivity.theMapMatrix[i][j];
						//pointList.add(i);
					}
				}
			}

			/*for(int i = 0; i < MainActivity.mapMatrixSize; i++){
				for(int j = 0; j < MainActivity.mapMatrixSize; j++){

						x = (int) ((j+0.5) * dispWidth / MainActivity.mapMatrixSize);
						y = (int) ((i+0.5) * dispHeight / MainActivity.mapMatrixSize);

						switch (MainActivity.theMapMatrix[i][j]){
							case 0:
								pointPaint.setColor(Color.WHITE);
								break;
							case 1:
								pointPaint.setColor(Color.RED);
								break;
							case 2:
								pointPaint.setColor(Color.GREEN);
								break;
							default:

						}

						pointPaint.setColor(Color.WHITE);

						theCanvas.drawCircle(x, y, POINT_MAP, pointPaint);

						//pointList.add(i);

				}
			}

			/*
        	switch (event.getAction()) {
        	case MotionEvent.ACTION_DOWN:
        		if(radius >= 0 && radius <= radiuscircle){
        			x = evX;
        			y = evY;
        			fingerPaint.setColor(Color.GREEN);
					CalcMotor(xcirc,ycirc);
        			invalidate();
        			drag = true;
        		}
        		break;

        	case MotionEvent.ACTION_MOVE:

        		if (drag && radius >= 0 && radius <= radiuscircle) {
        			x = evX;
        			y = evY;
        			fingerPaint.setColor(Color.GREEN);
					CalcMotor(xcirc,ycirc);
        			invalidate();

        		} else if(drag && radius > radiuscircle){

					double angle = Math.acos(xcirc/Math.sqrt((xcirc)*(xcirc) + (ycirc)*(ycirc)));

					if(evY > dispHeight){
						x = (float) Math.cos(angle) * radiuscircle + dispWidth;
						y = (float) Math.sin(angle) * radiuscircle + dispHeight;
					} else {
						x = (float) Math.cos(angle) * radiuscircle + dispWidth;
						y = -(float) Math.sin(angle) * radiuscircle + dispHeight;
					}

					xcirc = x - dispWidth - 5;
					ycirc = y - dispHeight - 5;


					fingerPaint.setColor(Color.GREEN);
					CalcMotor(xcirc,ycirc);
					invalidate();
				}
        		break;

        	case MotionEvent.ACTION_UP:

        		xcirc = 0;
        		ycirc = 0;
        		drag = false;
				CalcMotor(xcirc,ycirc);
        		invalidate();
        		break;
        	}*/
        	return true;
        }



}



	private void CalcMotor(float calc_x, float calc_y){


	}










}
