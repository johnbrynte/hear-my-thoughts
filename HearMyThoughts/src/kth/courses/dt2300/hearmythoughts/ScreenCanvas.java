package kth.courses.dt2300.hearmythoughts;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

public class ScreenCanvas extends View {

	float x, y;
	float radius = 25;

	Paint paint;

	public ScreenCanvas(Context context) {
		super(context);

		setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));

		paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);
	}

	public void setXY(float x, float y) {
		this.x = x;
		this.y = y;
		invalidate();
	}

	@Override
	public void onDraw(Canvas c) {
		c.drawColor(Color.GRAY);

		if (x != 0 && y != 0) {
			paint.setColor(Color.WHITE);
			c.drawCircle(x - radius, y - radius, 2 * radius, paint);
		}
		// paint.setColor(Color.WHITE);
		// paint.setTextSize(20);
		// c.drawText("0,0 is top left", 0, 0 + 20, paint);
	}

}
