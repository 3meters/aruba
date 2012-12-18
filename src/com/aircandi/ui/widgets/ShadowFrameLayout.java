package com.aircandi.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class ShadowFrameLayout extends FrameLayout {
	Paint mShadow = new Paint();

	public ShadowFrameLayout(Context context) {
		super(context);
	}

	public ShadowFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ShadowFrameLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void initialize() {
		/* radius=10, y-offset=2, color=black */
		mShadow.setShadowLayer(10.0f, 0.0f, 2.0f, 0xFF000000);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		//canvas.drawBitmap(bitmap, 0.0f, 0.0f, mShadow);
	}

}
