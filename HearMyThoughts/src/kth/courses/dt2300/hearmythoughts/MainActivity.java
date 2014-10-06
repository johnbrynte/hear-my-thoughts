package kth.courses.dt2300.hearmythoughts;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

public class MainActivity extends Activity implements Runnable, SensorEventListener {

	OSCPortOut port;
	float x, y;
	boolean touch = false;
	
	private SensorManager senSensorManager;
	private Sensor senAccelerometer;
	private long lastUpdate = 0;
	private float last_x, last_y, last_z;
	private static final int SHAKE_THRESHOLD = 600;

	public void onAccuracyChanged(Sensor sensor, int accuracy){
	
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		 Sensor mySensor = event.sensor;
		 
		    if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
		    	 float x = event.values[0];
		         float y = event.values[1];
		         float z = event.values[2];
		         
		         last_x = x;
	             last_y = y;
	             last_z = z;
		         
	             /*
		         long curTime = System.currentTimeMillis();
		         
		         if ((curTime - lastUpdate) > 100) {
		             long diffTime = (curTime - lastUpdate);
		             lastUpdate = curTime;
		             
		             float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;
		             
		             if (speed > SHAKE_THRESHOLD) {
		  
		             }
		  
		             last_x = x;
		             last_y = y;
		             last_z = z;
		         }
		         */
		    }
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// setup sensors
		senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	    senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);

	    // setup port
		port = null;
		InetAddress ip = null;
		try {
			ip = InetAddress.getByName("130.229.172.26");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			port = new OSCPortOut(ip, 9000);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		new Thread(this).start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void run() {
		while (true) {
			if (port != null) {
				Collection<Object> arguments;
				OSCMessage msg;
				// send touch events
				if (touch) {
					arguments = new ArrayList<Object>();
					arguments.add(x);
					arguments.add(y);
					msg = new OSCMessage("/touch", arguments);
					try {
						port.send(msg);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				// send accelerometer data
				arguments = new ArrayList<Object>();
				arguments.add(last_x);
				arguments.add(last_y);
				arguments.add(last_z);
				msg = new OSCMessage("/acc", arguments);
				try {
					port.send(msg);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean onTouchEvent( MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			touch = true;
			x = event.getX();
			y = event.getY();
			break;
		case MotionEvent.ACTION_UP:
			x = 0;
			y = 0;
			touch = false;
			break;
		}
		return false;
	}
}
