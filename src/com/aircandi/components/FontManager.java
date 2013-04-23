package com.aircandi.components;

import android.graphics.Typeface;
import android.widget.TextView;

import com.aircandi.Aircandi;

public class FontManager {

	private static Typeface	fontRobotoThin;
	private static Typeface	fontRobotoLight;
	private static Typeface	fontRobotoLightItalic;
	private static Typeface	fontRobotoRegular;
	private static Typeface	fontRobotoCondensed;
	private static Typeface	fontRobotoBoldCondensed;

	private static class FontManagerHolder {
		public static final FontManager	instance	= new FontManager();
	}

	public static FontManager getInstance() {
		return FontManagerHolder.instance;
	}

	private void initialize() {
		fontRobotoThin = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "Roboto-Thin.ttf");
		fontRobotoLight = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "Roboto-Light.ttf");
		fontRobotoLightItalic = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "Roboto-LightItalic.ttf");
		fontRobotoRegular = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "Roboto-Regular.ttf");
		fontRobotoCondensed = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "Roboto-Condensed.ttf");
		fontRobotoBoldCondensed = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "Roboto-BoldCondensed.ttf");
	}

	/**
	 * Designed as a singleton. The private Constructor prevents any other class from instantiating.
	 */
	private FontManager() {
		initialize();
	}

	public void setTypefaceLight(TextView view) {
		if (view != null) {
			view.setTypeface(getFontRobotoLight());
		}
	}

	public void setTypefaceLightItalic(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoLightItalic);
		}
	}

	public void setTypefaceThin(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoThin);
		}
	}

	public void setTypefaceDefault(TextView view) {
		if (view != null) {
			view.setTypeface(getFontRobotoLight());
		}
	}

	public void setTypefaceBoldDefault(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoRegular);
		}
	}

	public void setTypefaceRegular(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoRegular);
		}
	}

	public void setTypefaceCondensed(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoCondensed);
		}
	}

	public void setTypefaceBoldCondensed(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoBoldCondensed);
		}
	}

	public static Typeface getFontRobotoLight() {
		return fontRobotoLight;
	}

	public static Typeface getFontRobotoRegular() {
		return fontRobotoRegular;
	}

}
