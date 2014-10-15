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
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class MainActivity extends Activity implements SensorEventListener,
		OnClickListener {

	int width, height;

	float x, y;
	float ang;
	float vx, vy, vres; // x, y and resulting velocity
	float vang; // angular velocity
	float ax, ay, ares; // x, y and resulting acceleration
	float aang; // angular acceleration
	long lastPosUpdate;

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
				
				// init the pd sends
				PdBase.sendFloat("touch", 0);
				PdBase.sendFloat("x", 0);
				PdBase.sendFloat("y", 0);
				PdBase.sendFloat("vel", 0);
				PdBase.sendFloat("acc", 0);
				PdBase.sendFloat("ang_vel", 0);
				PdBase.sendFloat("ang_acc", 0);
			}
		});

		// setup sensors
		senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		senAccelerometer = senSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		senSensorManager.registerListener(this, senAccelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);

		lastPosUpdate = System.currentTimeMillis();
	}

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
	public boolean onTouchEvent(MotionEvent event) {
		float _x = -1, _y = -1;

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			touch = true;
			_x = event.getX();
			_y = event.getY();
			canvas.setXY(x, y);
			//PdBase.sendFloat("x", x / width);
			//PdBase.sendFloat("y", y / height);
			PdBase.sendFloat("touch", 1);
			break;
		case MotionEvent.ACTION_UP:
			touch = false;
			canvas.setXY(0, 0);
			// TODO: add a sound off/on
			//PdBase.sendFloat("x", 0);
			//PdBase.sendFloat("y", 0);
			PdBase.sendFloat("touch", 0);
			break;
		}

		if (_x != -1) {
			float t = (System.currentTimeMillis() - lastPosUpdate);

			float dx = _x - x;
			float dy = _y - y;
			float h = (float) Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));

			float _ang = ang; 
			ang = (float) Math.asin(dy / h);
			if (dy >= 0 && dx < 0) {
				ang = (float) Math.PI - ang;
			} else if (dy <= 0) {
				if (dx < 0) {
					ang = (float) Math.PI - ang;
				} else {
					ang = (float) Math.PI * 2 + ang;
				}
			}
			ang *= 180 / Math.PI;

			float _vang = vang;

			float dang = ang - _ang;
			if (dang > Math.PI) {
				dang -= Math.PI * 2;
			}
			ang = _ang + dang;
			
			vang = dang / t;

			aang = (vang - _vang) / t;

			//Log.v("ang", "aang: " + aang + " vang: " + vang + " ang: " + _ang);
			if (h > 5 && Math.abs(vang) > 2) {
				//Log.v("ang", "angular velocity: " + vang + " h: " + h + " t: " + t);
				PdBase.sendBang("ang_vel_high");
			}
			
			if (Math.abs(vres) > 3) {
				Log.v("dt2300", "vres: " + vres);
				//PdBase.sendBang("ang_vel_high");
			}

			float _vx = vx;
			float _vy = vy;

			vx = dx / t;
			vy = dy / t;
			vres = (float) Math.sqrt(Math.pow(vx, 2) + Math.pow(vy, 2));

			ax = (vx - _vx) / t;
			ay = (vy - _vy) / t;
			ares = (float) Math.sqrt(Math.pow(ax, 2) + Math.pow(ay, 2));

			canvas.setDebugValues(new float[] { 0.5f + aang,
					0.5f + vang / 10, _ang / 360, ares,vres/5, h/100 });
			
			// update old variables
			x = _x;
			y = _y;
			//ang = _ang;
			
			// send to pd
			PdBase.sendFloat("x", x);
			PdBase.sendFloat("y", y);
			PdBase.sendFloat("vel", vres);
			PdBase.sendFloat("acc", ares);
			PdBase.sendFloat("ang_vel", vang);
			PdBase.sendFloat("ang_acc", aang);

			lastPosUpdate = System.currentTimeMillis();
		}

		return false;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
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
