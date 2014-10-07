package kth.courses.dt2300.hearmythoughts;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

import org.puredata.core.PdBase;

import kth.courses.dt2300.hearmythoughts.puredata.Patch;
import kth.courses.dt2300.hearmythoughts.puredata.PureDataHandler;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class MainActivity extends Activity implements Runnable,
		SensorEventListener, OnClickListener {

	final float SMALLEST_POSITIVE_FLOAT = 0.000001f;
	final float MIN_FLOAT = -1000.0f;
	final float MAX_FLOAT = 1000000.0f;
	
	int width, height;

	OSCPortOut port;
	float x, y;
	boolean touch = false;
	boolean recording = false;
	
	PureDataHandler pdHandler;

	private SensorManager senSensorManager;
	private Sensor senAccelerometer;
	private long lastUpdate = 0;
	private float last_x, last_y, last_z;
	private static final int SHAKE_THRESHOLD = 600;
	
	ScreenCanvas canvas;
	Button button;

	public void onAccuracyChanged(Sensor sensor, int accuracy) {

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
			 * long curTime = System.currentTimeMillis();
			 * 
			 * if ((curTime - lastUpdate) > 100) { long diffTime = (curTime -
			 * lastUpdate); lastUpdate = curTime;
			 * 
			 * float speed = Math.abs(x + y + z - last_x - last_y - last_z)/
			 * diffTime * 10000;
			 * 
			 * if (speed > SHAKE_THRESHOLD) {
			 * 
			 * }
			 * 
			 * last_x = x; last_y = y; last_z = z; }
			 */
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		FrameLayout container = (FrameLayout) findViewById(R.id.container);
		canvas = new ScreenCanvas(this);
		container.addView(canvas);
		
		button = (Button) findViewById(R.id.record_button);
		button.setBackgroundColor(Color.WHITE);
		button.setOnClickListener(this);
		
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		width = size.x;
		height = size.y;
		
		pdHandler = new PureDataHandler(this);
		pdHandler.addReadyListener(new PureDataHandler.ReadyListener() {
			@Override
			public void ready() {
				Patch patch = pdHandler.createPatch(R.raw.test);
				patch.open();
				pdHandler.startAudio();
			}
		});

		// setup sensors
		senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		senAccelerometer = senSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		senSensorManager.registerListener(this, senAccelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);

		// setup port
		port = null;
		InetAddress ip = null;
		try {
			ip = InetAddress.getByName("192.168.0.16");
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
	public void run() {
		while (true) {
			if (port != null) {
				try {
					Collection<Object> arguments;
					OSCMessage msg;
					// send touch events
					if (touch) {
						arguments = new ArrayList<Object>();
						arguments.add(x);
						arguments.add(y);
						msg = new OSCMessage("/touch", arguments);
						port.send(msg);
					}
					// send accelerometer data
					// arguments.add(getValidFloat(last_x));
					// arguments.add(getValidFloat(last_y));
					// arguments.add(getValidFloat(last_z));
					msg = new OSCMessage("/acc/x");// , arguments);
					arguments = new ArrayList<Object>();
					msg.addArgument(new Float(last_x));
					port.send(msg);
					msg = new OSCMessage("/acc/y");
					arguments = new ArrayList<Object>();
					msg.addArgument(new Float(last_y));
					port.send(msg);
					msg = new OSCMessage("/acc/z");
					arguments = new ArrayList<Object>();
					msg.addArgument(new Float(last_z));
					port.send(msg);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			touch = true;
			x = event.getX();
			y = event.getY();
			canvas.setXY(x, y);
			PdBase.sendFloat("x", x/width);
			PdBase.sendFloat("y", y/height);
			break;
		case MotionEvent.ACTION_UP:
			x = 0;
			y = 0;
			touch = false;
			canvas.setXY(x, y);
			// TODO: add a sound off/on
			PdBase.sendFloat("x", 0);
			PdBase.sendFloat("y", 0);
			break;
		}
		return false;
	}

	private Float getValidFloat(float f) {
		Float _f = new Float(f);
		if (_f < MIN_FLOAT) {
			_f = MIN_FLOAT;
		} else if (_f > MAX_FLOAT) {
			_f = MAX_FLOAT;
		}
		if (_f > 0.0f && _f < SMALLEST_POSITIVE_FLOAT) {
			_f = SMALLEST_POSITIVE_FLOAT;
		}
		return _f;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.record_button:
			Button button = (Button) v;
			if (recording) {
				PdBase.sendSymbol("record", "stop");
				button.setText(R.string.record_start);
				button.setBackgroundColor(Color.WHITE);
			} else {
				PdBase.sendSymbol("record", "start");
				button.setText(R.string.record_stop);
				button.setBackgroundColor(Color.RED);
			}
			recording = !recording;
			break;
		default:
			break;
		}
	}
}
