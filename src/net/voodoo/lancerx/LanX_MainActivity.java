package net.voodoo.lancerx;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

public class LanX_MainActivity extends Activity {
	private static final String TAG = "bt";

	// =============================================================================
	// byte[] msgBuffer = new byte[13];
	int upPI = 0;
	int downPI = 0;
	boolean inTouch = false;
	String result = "";
	public int pointerCount;

	TextView tv;
	public int HEIGHT;
	private float tahoCoeff;
	ImageView imageView, steering;

	// ==== sensor =====
	SensorManager sensorManager;
	Sensor sensorAccel;

	StringBuilder sb = new StringBuilder();

	// =============================================================================

	int accelPedal, steeringWheel;
	Timer timer;
	ImageButton gearButton, avarButton, lightButton;
	Button hornButton;
	boolean gearDirection = true, avar = false, light = false, horn = false;

	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private OutputStream outStream = null;

	// SPP UUID сервиса
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	// MAC-адрес Bluetooth модуля
	private static String address = "20:14:05:08:13:09"; // HC-05
	//private static String address = "00:15:83:15:A3:10"; // kbp-bd

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.lx_activity_main);

		hornButton = (Button) findViewById(R.id.horn);
		gearButton = (ImageButton) findViewById(R.id.gearButton);
		avarButton = (ImageButton) findViewById(R.id.avar);
		lightButton =(ImageButton) findViewById(R.id.light);
		tv = (TextView) findViewById(R.id.textView1);
		imageView = (ImageView) findViewById(R.id.taho);
		steering = (ImageView) findViewById(R.id.steering);

		// tv.measure(0, 0);
		// HEIGHT = tv.getMeasuredHeight();
		HEIGHT = 480;
		accelPedal = HEIGHT;
		tahoCoeff = (270 / (float) HEIGHT);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		btAdapter = BluetoothAdapter.getDefaultAdapter();
		checkBTState();

		BTConnect();
		
		// =========== Horn Button ==========================================
        hornButton.setOnTouchListener(new OnTouchListener(){//;.setOnClickListener(new OnClickListener() {			 
			
			//@Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN ) 
                	{
                	    horn = true;                
                	    hornButton.setBackgroundResource(R.drawable.hornpressed);
                	}
                if (event.getAction() == MotionEvent.ACTION_UP)    
                	{
	                	horn = false;  
	                	hornButton.setBackgroundResource(R.drawable.horn);
                	}
                return true;
            }
 
		});
//		hornButton.setOnClickListener(new OnClickListener() {			 
//			
//			public void onClick(View arg0) {				
//			}
// 
//		});
		// =========== Gear Button ==========================================
		gearButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// меняем изображение на кнопке
				if (gearDirection)
					// gearButton.setImageResource(R.drawable.forward);
					gearButton.setBackgroundResource(R.drawable.backward);
				else
					// возвращаем первую картинку
					// gearButton.setImageResource(R.drawable.backward);
					gearButton.setBackgroundResource(R.drawable.forward);
				gearDirection = !gearDirection;
			}
		});
		
		// =========== Avar Button ==========================================
				avarButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						// меняем изображение на кнопке
						if (avar);
							// gearButton.setImageResource(R.drawable.forward);
							//avarButton.setBackgroundResource(R.drawable.avaroff);
						else
							// возвращаем первую картинку
							// gearButton.setImageResource(R.drawable.backward);
							avarButton.setBackgroundResource(R.drawable.avaroff);
						avar = !avar;
					}
				});
				
		// =========== Avar Button ==========================================
				lightButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						// меняем изображение на кнопке
						if (light)
							// gearButton.setImageResource(R.drawable.forward);
							lightButton.setBackgroundResource(R.drawable.lightoff);
						else
							// возвращаем первую картинку
							// gearButton.setImageResource(R.drawable.backward);
							lightButton.setBackgroundResource(R.drawable.lighton);
						light = !light;
					}
				});

		// =========== Accelerator ==========================================
		tv.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View view, MotionEvent event) {
				// событие
				int actionMask = event.getActionMasked();
				// индекс касания
				int pointerIndex = event.getActionIndex();
				// число касаний
				pointerCount = event.getPointerCount();
				// int y = 0;

				switch (actionMask) {
				case MotionEvent.ACTION_DOWN: // первое касание
					inTouch = true;
				case MotionEvent.ACTION_POINTER_DOWN: // последующие касания
					downPI = pointerIndex;
					break;

				case MotionEvent.ACTION_UP: // прерывание последнего касания
					inTouch = false;
					// sb.setLength(0);
				case MotionEvent.ACTION_POINTER_UP: // прерывания касаний
					upPI = pointerIndex;
					accelPedal = HEIGHT;
					break;

				case MotionEvent.ACTION_MOVE: // движение
					// int x = (int)event.getX(0);
					accelPedal = (int) event.getY(0);
					// String s = /*"x = " + x + */"  y = " + y + "\n\r" + sb +
					// "\n\r";
					// tv.setText( s);
					// sendData(s);

					break;
				}
				Matrix matrix = new Matrix();
				imageView.setScaleType(ScaleType.MATRIX); // required
				matrix.postRotate(getTahoAngle(accelPedal), 150, 150);
				imageView.setImageMatrix(matrix);
				return true;
			}
		});

	}

	// ===============================================================================
	private int getTahoAngle(int aValue) {

		return (int) ((HEIGHT - aValue) * tahoCoeff);
	}

	// ===============================================================================
	private void BTConnect() {
		Log.d(TAG, "...onResume - попытка соединения...");

		// Set up a pointer to the remote node using it's address.
		BluetoothDevice device = btAdapter.getRemoteDevice(address);

		// Two things are needed to make a connection:
		// A MAC address, which we got above.
		// A Service ID or UUID. In this case we are using the
		// UUID for SPP.
		try {
			btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
		} catch (IOException e) {
			errorExit("Fatal Error", "In onResume() and socket create failed: "
					+ e.getMessage() + ".");
		}

		// Discovery is resource intensive. Make sure it isn't going on
		// when you attempt to connect and pass your message.
		btAdapter.cancelDiscovery();

		// Establish the connection. This will block until it connects.
		Log.d(TAG, "...Соединяемся...");
		try {
			btSocket.connect();
			Log.d(TAG,
					"...Соединение установлено и готово к передачи данных...");
			Toast.makeText(getBaseContext(), "...Соединение установлено",
					Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			try {
				btSocket.close();
			} catch (IOException e2) {
				errorExit("Fatal Error",
						"In onResume() and unable to close socket during connection failure"
								+ e2.getMessage() + ".");
			}
		}

		// Create a data stream so we can talk to server.
		Log.d(TAG, "...Создание Socket...");

		try {
			outStream = btSocket.getOutputStream();
		} catch (IOException e) {
			errorExit(
					"Fatal Error",
					"In onResume() and output stream creation failed:"
							+ e.getMessage() + ".");
		}
	}

	// ================================================================================
	String outString = "";
    int cntr = 3;
    boolean bAvarAnim = true;
	// ===============================================================================
	@Override
	public void onResume() {
		super.onResume();
		sensorManager.registerListener(listener, sensorAccel,
				SensorManager.SENSOR_DELAY_NORMAL);
		// ========== timer =================
		timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					public void run() {
						if(avar)
						{
								cntr--;
								if (cntr == 0)
								{
									cntr = 3;
									if( bAvarAnim)
										{
												bAvarAnim = false;
												avarButton.setBackgroundResource(R.drawable.avaroff);
										}
									else           
										{
												bAvarAnim = true;
												avarButton.setBackgroundResource(R.drawable.avaron);
										}
								}
						}
						
						outString = "";
						if (gearDirection)
							outString = "F";
						else
							outString = "B";
						int a = getAccelPercentage(HEIGHT - accelPedal);
						if (a < 10)
							outString += '0';
						outString += a;
						//outString += 'A';

						if (steeringWheel >= -2 && steeringWheel <= 2)	outString += 'F';
						else if (steeringWheel <  2)outString += 'L';
						else if (steeringWheel > -2)outString += 'R';

						//outString += steeringWheel;
						
						if (avar)
							outString += "A";
						else
							outString += "0";
						
						if (light)
							outString += "L";
						else
							outString += "0";
						
						if (horn)
							outString += "H";
						else
							outString += "0";
						
						outString += '\r';
						sendData(outString);
						// sendData("ACC: " +(480 - accelPedal) + ", ANG: " +
						// steeringWheel + "___\r");
					}
				});
			}
		};
		timer.schedule(task, 0, 100);
	}

	// ===============================================================================
	private int getAccelPercentage(int aValue) {
		int a = (int) (((float) 100 / HEIGHT) * aValue);
		if (a == 100)
			a = 99;
		return a;
	}

	// ===============================================================================
	@Override
	public void onPause() {
		super.onPause();
		timer.cancel();

		sensorManager.unregisterListener(listener);

		Log.d(TAG, "...In onPause()...");

		if (outStream != null) {
			try {
				outStream.flush();
			} catch (IOException e) {
				errorExit(
						"Fatal Error",
						"In onPause() and failed to flush output stream: "
								+ e.getMessage() + ".");
			}
		}

		try {
			btSocket.close();
		} catch (IOException e2) {
			errorExit("Fatal Error", "In onPause() and failed to close socket."
					+ e2.getMessage() + ".");
		}
	}

	private void checkBTState() {
		if (btAdapter == null) {
			errorExit("Fatal Error", "Bluetooth не поддерживается");
		}
	}

	private void errorExit(String title, String message) {
		Toast.makeText(getBaseContext(), title + " - " + message,
				Toast.LENGTH_LONG).show();
		finish();
	}

	private void sendData(String message) {

		byte[] msgBuffer = message.getBytes();
		try {
			outStream.write(msgBuffer);
		} catch (IOException e) {

		}
	}

	// String format(float value) {
	// return String.format("%1$.1f", value);
	// }

	float valueAccel;

	SensorEventListener listener = new SensorEventListener() {

		// @Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		// @Override
		public void onSensorChanged(SensorEvent event) {
			switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				// for (int i = 0; i < 3; i++) valuesAccel[i] = event.values[i];
				// valueAccel = event.values[1];
				steeringWheel = (int) event.values[1];
				// sb.setLength(0);
				// sb.append("Accel: " + format(valueAccel));

				Matrix matrix = new Matrix();
				steering.setScaleType(ScaleType.MATRIX); // required
				matrix.postRotate((steeringWheel * 18), 150, 150);
				steering.setImageMatrix(matrix);
				break;
			}

		}

	};
}
