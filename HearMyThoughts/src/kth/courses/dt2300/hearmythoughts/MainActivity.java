package kth.courses.dt2300.hearmythoughts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class MainActivity extends Activity implements SensorEventListener,
		OnClickListener, Runnable {

	final float V_BLEED = 0.3f; // overlap between different emotions
	final float V_SAD = 0.02f;
	final float V_HAPPY = 0.4f;
	final float V_ANGRY = 0.9f;

	String outputDir = Environment.getExternalStorageDirectory()
			+ "/puredata-records/";

	Patch patch;
	File output;
	BufferedWriter writer;

	int width, height;

	float x, y;
	float ang;
	float vx, vy, vres; // x, y and resulting velocity
	float vang; // angular velocity
	float ax, ay, ares; // x, y and resulting acceleration
	float aang; // angular acceleration
	long lastPosUpdate;

	RMS vresRMS = new RMS(30);
	RMS vangRMS = new RMS(20);

	LinkedList<Float> vangAverage = new LinkedList<Float>();
	LinkedList<Float> vang_bps_avg = new LinkedList<Float>();
	long vangLastBang;

	boolean touch = false;
	boolean recording = false;

	PureDataHandler pdHandler;

	private SensorManager senSensorManager;
	private Sensor senAccelerometer;

	ScreenCanvas canvas;
	Button button;
	
	boolean sound = false;

	// Initiating Menu XML file (menu.xml)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.layout.menu, menu);
		return true;
	}

	/**
	 * Event Handling for Individual menu item selected Identify single menu
	 * item by it's id
	 * */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_sound:
			if (pdHandler.ready()) {
				sound = !sound;
				if (sound) {
					pdHandler.startAudio();
				} else {
					pdHandler.stopAudio();
				}
			}
			break;
		case R.id.menu_visuals:
			canvas.toggleVisuals();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// setup averages
		for (int i = 0; i < 4; i++) {
			vangAverage.add(0f);
		}
		for (int i = 0; i < 20; i++) {
			vang_bps_avg.add(0f);
		}
		// setup bang timers
		vangLastBang = System.currentTimeMillis();

		setContentView(R.layout.activity_main);

		FrameLayout container = (FrameLayout) findViewById(R.id.container);
		canvas = new ScreenCanvas(this);
		container.addView(canvas);

		button = (Button) findViewById(R.id.record_button);
		button.setBackgroundColor(Color.WHITE);
		button.setOnClickListener(this);
		
		Button playButton = (Button) findViewById(R.id.play_button);
		playButton.setBackgroundColor(Color.GREEN);
		playButton.setOnClickListener(this);

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		width = size.x;
		height = size.y;

		pdHandler = new PureDataHandler(this);
		pdHandler.addReadyListener(new PureDataHandler.ReadyListener() {
			@Override
			public void ready() {
				patch = new Patch("mapping_1.pd");
				patch.open();

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

		new Thread(this).start();
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
		}
	}

	private float getListAverage(List<Float> list) {
		float average = 0;
		for (Float e : list) {
			average += e;
		}
		average = average / list.size();
		return average;
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
			// PdBase.sendFloat("x", x / width);
			// PdBase.sendFloat("y", y / height);
			PdBase.sendFloat("touch", 1);
			break;
		case MotionEvent.ACTION_UP:
			touch = false;
			canvas.setXY(0, 0);
			// TODO: add a sound off/on
			// PdBase.sendFloat("x", 0);
			// PdBase.sendFloat("y", 0);
			PdBase.sendFloat("touch", 0);

			try {
				if (writer != null) {
					writer.write("0:0,0\n");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}

		if (_x != -1) {
			long millis = System.currentTimeMillis();
			float t = (millis - lastPosUpdate);

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
			// ang *= 180 / Math.PI;

			float dang = ang - _ang;
			if (dang > Math.PI) {
				dang -= Math.PI * 2;
			}
			ang = _ang + dang;
			// Log.v("dt2300", "ang: " + ang);

			float _vang = vang;
			// calculate angular velocity
			vang = dang / t;

			aang = (vang - _vang) / t;

			if (Math.abs(vres) > 3) {
				// Log.v("dt2300", "vres: " + vres);
				// PdBase.sendBang("ang_vel_high");
			}

			float _vx = vx;
			float _vy = vy;

			vx = dx / t;
			vy = dy / t;
			vres = (float) Math.sqrt(Math.pow(vx, 2) + Math.pow(vy, 2));

			ax = (vx - _vx) / t;
			ay = (vy - _vy) / t;
			ares = (float) Math.sqrt(Math.pow(ax, 2) + Math.pow(ay, 2));

			// update old variables
			x = _x;
			y = _y;
			// ang = _ang;

			// send to pd
			PdBase.sendFloat("x", x);
			PdBase.sendFloat("y", y);
			PdBase.sendFloat("vel", vres);
			PdBase.sendFloat("acc", ares);
			PdBase.sendFloat("ang_vel", vang);
			PdBase.sendFloat("ang_acc", aang);

			lastPosUpdate = millis;

			try {
				if (writer != null) {
					writer.write(millis + ":" + x + "," + y + "\n");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return false;
	}

	@Override
	public void onClick(View v) {
		Button button;
		switch (v.getId()) {
		case R.id.record_button:
			button = (Button) v;
			if (recording) {
				PdBase.sendSymbol("record", "stop");
				button.setText(R.string.record_start);
				button.setBackgroundColor(Color.WHITE);

				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else {
				PdBase.sendSymbol("record", "start");
				button.setText(R.string.record_stop);
				button.setBackgroundColor(Color.RED);

				output = new File(outputDir + System.currentTimeMillis()
						+ ".txt");
				try {
					// if file doesnt exists, then create it
					if (!output.exists()) {
						output.createNewFile();

					}
					FileWriter fw = new FileWriter(output.getAbsoluteFile());
					writer = new BufferedWriter(fw);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

					writer = null;
				}
			}
			recording = !recording;
			break;
		case R.id.play_button:
			PdBase.sendBang("play");
		default:
			break;
		}
	}

	@Override
	public void run() {
		while (true) {
			long millis = System.currentTimeMillis();

			// resulting velocity
			vresRMS.add(touch ? vres : 0f);
			float vres_rms = vresRMS.getRMS();
			// scaling to get between 0 and 1
			vres_rms = vres_rms / 6;
			vres_rms = vres_rms > 1 ? 1 : vres_rms;
			// distribute emotions
			float sad = 0f;
			float happy = 0f;
			float angry = 0f;
			if (vres_rms <= V_SAD) {
				sad = 1 - (V_SAD - vres_rms) / V_SAD;
			} else if (vres_rms <= V_HAPPY) {
				sad = 1 - (vres_rms - V_SAD) / (V_HAPPY - V_SAD);
				happy = 1 - (V_HAPPY - vres_rms) / (V_HAPPY - V_SAD);
			} else if (vres_rms <= V_ANGRY) {
				happy = 1 - (vres_rms - V_HAPPY) / (V_ANGRY - V_HAPPY);
				angry = 1 - (V_ANGRY - vres_rms) / (V_ANGRY - V_HAPPY);
			} else {
				angry = 1;// - (vres_rms - V_ANGRY) / (1 - V_ANGRY);
			}
			if (touch) {
				canvas.setBackgroundColor((int) (155 + 100 * angry),
						(int) (155 + 100 * happy), (int) (155 + 100 * sad));
			} else {
				canvas.setBackgroundColor(155, 155, 155);
			}

			vangRMS.add(touch ? vang : 0f);
			float vang_rms = vangRMS.getRMS();

			vangAverage.add(vang);
			vangAverage.removeFirst();
			float vang_avg = getListAverage(vangAverage);

			// Log.v("ang", "aang: " + aang + " vang: " + vang + " ang: " +
			// _ang);
			if (Math.abs(vang_rms) > 2) {
				// vang_bps_avg.add(1000f / (millis - vangLastBang));
				// vang_bps_avg.removeFirst();
				Log.v("dt2300", "vang_rms: " + vang_rms);
				vangLastBang = millis;
				// PdBase.sendBang("ang_vel_high");
			} else {
				vang_bps_avg.add(0f);
				vang_bps_avg.removeFirst();
			}

			float vang_bps = getListAverage(vang_bps_avg);

			canvas.setDebugValues(new float[] { sad, happy, angry });
			// Log.v("dt2300", "ang: " + ang + " vang_rms: " + vang_rms);

			float velocity = vres_rms;
			// velocity = velocity > 1 ? 1 : velocity;
			
			float volume = 0.2f + 0.8f*(1f - y / height);

			PdBase.sendFloat("velocity", velocity);
			PdBase.sendFloat("sad", sad);
			PdBase.sendFloat("happy", happy);
			PdBase.sendFloat("angry", angry);
			PdBase.sendFloat("volume", volume);

			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
