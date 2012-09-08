package com.aircandi.components;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.google.android.maps.MapView;

public class CandiMapView extends MapView {

	private long	lastTouchTime	= -1;

	public CandiMapView(Context context, String key) {
		super(context, key);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			long thisTime = System.currentTimeMillis();
			if (thisTime - lastTouchTime < ViewConfiguration.getDoubleTapTimeout()) {
				// Double tap
				this.getController().zoomInFixing((int) event.getX(), (int) event.getY());
				lastTouchTime = -1;
			}
			else {
				// Too slow :)
				lastTouchTime = thisTime;
			}
		}

		return super.onInterceptTouchEvent(event);
	}

}
