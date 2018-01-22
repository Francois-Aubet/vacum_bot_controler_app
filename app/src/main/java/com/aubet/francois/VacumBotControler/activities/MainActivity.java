package com.aubet.francois.VacumBotControler.activities;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.aubet.francois.VacumBotControler.R;
import com.aubet.francois.VacumBotControler.model.State;
import com.aubet.francois.VacumBotControler.connection.SocketManager;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;


public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener  {
    Button btnCoPi, btnCoWifi, btnCoWeb, btnCoIPSEC, btnMap;
    private static final BlockingQueue<String> queue = new ArrayBlockingQueue<String>(100);
    static ConnectedFeedback theFeedback;
    static SocketManager sock;
    static final String ipAddress = "10.2.2.113";

    private SensorManager mSensorManager;
    private Sensor mLight;

    EditText ssid = null;
    EditText pass = null;

    State theState = new State();

    public static final int mapMatrixSize = 200;
    public static int[][] theMapMatrix = new int [mapMatrixSize][mapMatrixSize];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


		System.out.println("trying");

		sock = new SocketManager(queue,ipAddress, 20009);
		sock.start();

        //sendCommand("getWIFI");
        try {
            Thread.sleep(100);
        }catch(InterruptedException e){}
        //System.out.println("rep:" + answer);

        /*try {
        sock.join();
        }catch(InterruptedException e){}*/

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);


        btnCoPi = (Button) findViewById(R.id.button_copi);
        btnCoPi.setOnClickListener(this);

        btnCoWifi = (Button) findViewById(R.id.button_cowi);
        btnCoWifi.setOnClickListener(this);

        btnMap = (Button) findViewById(R.id.button_acticvityMap);
        btnMap.setOnClickListener(this);

        Random rn = new Random();
        for(int i = 0; i < mapMatrixSize; i++){
            for(int j = 0; j < mapMatrixSize; j++){
                theMapMatrix[i][j] = 0; //rn.nextInt() % 3;
            }
        }



        theFeedback = new ConnectedFeedback();
        theFeedback.start();

		System.out.println("end");
    }



    public void onClick(View v) {
            switch (v.getId()) {
            case R.id.button_copi:
                if(!State.connectedPi) {
                    sock.interrupt();
                    sock = new SocketManager(queue, ipAddress, 20009);
                    sock.start();
                }
                break;
            case R.id.button_cowi:
                    sendCommand("startMapping");

                    //sendCommand(Double.toString((State.lightValue)));
                break;
            case R.id.button_acticvityMap:
                	Intent intent1 = new Intent(this, MapActivity.class);
                    startActivity(intent1);

            default:
                break;
            }
    }



    public static void onReceivedCommand(String com) {

        System.out.println(com);

        switch (com) {
            case "WIFIfalse":
                State.connectedWifi = false;
                break;
            case "askLum":
                sendCommand(Double.toString((State.lightValue)));
                break;

            default:
                // parse the matrix, of form:      "i,j,value"
                try {
                    String[] parts = com.split(",");
                    int i = Integer.parseInt(parts[0]);
                    int j = Integer.parseInt(parts[1]);
                    int value = Integer.parseInt(parts[2]);
                    theMapMatrix[i][j] = value;

                   // System.out.println("just gave field" + i + " , " + j + " : " + value);

                    /*String[] rows = com.split(";");

                    for (int i = 0; i < mapMatrixSize; i++) {    //for(String row : rows){
                        String[] values = rows[i].split(",");

                        for (int j = 0; j < mapMatrixSize; j++) {    //for(String value : values){
                            theMapMatrix[i][j] = Integer.parseInt(values[j]);
                        }

                    }*/
                } catch (Exception e){

                }

            break;
        }

    }

    public static void sendCommand(String com) {
        while (!queue.offer(com)) {
            queue.poll();
        }
    }



    	//+++++++++++++++++++++			sensor methods to show the connection feedbacks			+++++++++++++++++++++++

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
    // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
    float lightMeasure = event.values[0];
    State.lightValue = lightMeasure;
    // Do something with this sensor data.
    }

    @Override
    protected void onResume() {
    // Register a listener for the sensor.
    super.onResume();
    mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
    // Be sure to unregister the sensor when the activity pauses.
    super.onPause();
    mSensorManager.unregisterListener(this);
    }

    	//+++++++++++++++++++++			to show the connection feedbacks			+++++++++++++++++++++++
	private class ConnectedFeedback extends Thread {
		int i = 0;
		Instrumentation inst = new Instrumentation();


		public ConnectedFeedback() {
		}


		@Override
		public void run() {
			boolean a = false;

			while (true) {
				//onResume();

				try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(com.aubet.francois.VacumBotControler.model.State.connectedPi) {
                                //btnCoIPSEC.setHighlightColor(Color.GREEN);
                                btnCoPi.setBackgroundColor(Color.GREEN);
                            } else {
                                btnCoPi.setBackgroundColor(Color.LTGRAY);
                            }
                        }
                    });
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(com.aubet.francois.VacumBotControler.model.State.connectedWifi) {
                                btnCoWifi.setBackgroundColor(Color.GREEN);
                                btnCoWifi.setText(R.string.btn_cowi_done);
                            } else {
                                btnCoWifi.setBackgroundColor(Color.LTGRAY);
                            }
                        }
                    });



				} catch (Exception e) {}

				try {
					Thread.sleep(390);
				} catch (Exception e) {
				}

			}

		}

	}




}
