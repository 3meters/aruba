package com.proxibase.aircandi.widgets;

import android.content.Context;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.ImageView.ScaleType;

/**
 * We use a TileView to display each tagged entity. It's just a LinearLayout with nested views.
 * RippleView uses any LayoutParams associated with a TileView otherwise it will generate
 * and use some defaults.
 */
@SuppressWarnings("unused")
public class CandiButton extends Button {

	private Button	button;

	public CandiButton(final Context context, final int width, final float titleTextSize, final float bodyTextSize) {

		super(context);

		/* Button configuration. */
		ViewConfiguration configuration = ViewConfiguration.get(context);
		int maxCacheSize = configuration.getScaledMaximumDrawingCacheSize();
		ScaleType scaleType = ScaleType.FIT_CENTER;

		/* Text layer */
		button = new Button(context);
		//		this.setWidth(80)
		//		button.setMaxHeight(width_);
		//		FrameLayout.LayoutParams paramsTextLayer = new FrameLayout.LayoutParams(width_, width_);
		//		paramsTextLayer.gravity = Gravity.LEFT | Gravity.CLIP_VERTICAL;
		//		button.setLayoutParams(paramsTextLayer);
		//		button.setTextSize(TypedValue.COMPLEX_UNIT_SP, bodyTextSize);
		//		button.setTextColor(Color.WHITE);
		//		button.setPadding(4, 4, 4, 4);

	}

}
