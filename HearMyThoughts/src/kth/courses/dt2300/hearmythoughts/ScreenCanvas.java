package kth.courses.dt2300.hearmythoughts;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

public class ScreenCanvas extends View implements Runnable {

	boolean running = true;
	
	float x, y;
	float radius = 25;
	
	LinkedList<Float[]> trace = new LinkedList<Float[]>();
	final int TRACE_LENGTH = 30;
	
	float[] debug;

	int color;
	int background;
	Paint paint;
	
	boolean visuals = false;

	public ScreenCanvas(Context context) {
		super(context);

		setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));

		paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);
		
		for (int i = 0;i < TRACE_LENGTH; i++) {
			trace.add(new Float[]{0f, 0f});
		}
		
		setColor(255, 255, 255);
		background = Color.GRAY;
		
		new Thread(this).start();
	}
	
	public void setDebugValues(float[] values) {
		debug = values;
	}

	public void setXY(float x, float y) {
		this.x = x;
		this.y = y;
		//invalidate();
	}
	
	public void setBackgroundColor(int r, int g, int b) {
		background = Color.rgb(r, g, b);
	}
	
	public void setColor(int r, int g, int b) {
		color = Color.rgb(r, g, b);
	}
	
	public void toggleVisuals() {
		visuals = !visuals;
	}

	@Override
	public void onDraw(Canvas c) {
		if (!visuals) {
			c.drawColor(Color.BLACK);
			return;
		}
		
		c.drawColor(background);

		Float[] p;
		float r;
		for (int i=0; i < TRACE_LENGTH; i++) {
			p = trace.get(i);
			if (p[0] != 0 && p[1] != 0) {
				paint.setColor(color);
				r = radius * (float) i / TRACE_LENGTH;
				c.drawCircle(p[0] - r, p[1] - r, 2 * r, paint);
			}
		}
		
		/*
		if (debug != null) {
			int _x, width = 50, height = 200;
			for (int i = 0; i < debug.length; i++) {
				_x = i*(width+4);
				paint.setColor(Color.WHITE);
				c.drawRect(_x, 0, _x+width, height, paint);
				paint.setColor(Color.GREEN);
				c.drawRect(_x, height-debug[i]*height, _x+width, height, paint);
			}
		}
		*/
		// paint.setColor(Color.WHITE);
		// paint.setTextSize(20);
		// c.drawText("0,0 is top left", 0, 0 + 20, paint);
	}

	@Override
	public void run() {
		while (running) {
			trace.add(new Float[]{x, y});
			trace.removeFirst();
			
			postInvalidate();
			
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
