package com.aubet.francois.VacumBotControler;

import android.app.Instrumentation;
import android.content.Context;
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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener  {
    Button btnCoPi, btnCoWifi, btnCoWeb, btnCoIPSEC;
    private static final BlockingQueue<String> queue = new ArrayBlockingQueue<String>(100);
    static ConnectedFeedback theFeedback;
    static SocketManager sock;
    static final String ipAddress = "10.180.27.117";

    private SensorManager mSensorManager;
    private Sensor mLight;

    EditText ssid = null;
    EditText pass = null;

    State theState = new State();




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
                    //sendCommand("Lumi:");

                    sendCommand(Double.toString((State.lightValue)));
                break;

            default:
                break;
            }
    }



    public static void onReceivedCommand(String com) {
        switch (com) {
            case "WIFIfalse":
                State.connectedWifi = false;
                break;
            case "askLum":
                sendCommand(Double.toString((State.lightValue)));
                break;

            default:
        }

    }

    private static void sendCommand(String com) {
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
                            if(com.aubet.francois.VacumBotControler.State.connectedPi) {
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
                            if(com.aubet.francois.VacumBotControler.State.connectedWifi) {
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
