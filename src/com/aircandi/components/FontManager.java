package com.aircandi.components;

import android.graphics.Typeface;
import android.widget.TextView;

import com.aircandi.Aircandi;

public class FontManager {

	private static FontManager	singletonObject;
	public static Typeface		fontRobotoThin;
	public static Typeface		fontRobotoLight;
	public static Typeface		fontRobotoRegular;
	public static Typeface		fontRobotoCondensed;
	public static Typeface		fontRobotoBoldCondensed;

	public static synchronized FontManager getInstance() {
		if (singletonObject == null) {
			singletonObject = new FontManager();
		}
		return singletonObject;
	}

	public void initialize() {
		fontRobotoThin = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "Roboto-Thin.ttf");
		fontRobotoLight = Typeface.createFromAsset(Aircandi.applicationContext.getAssets(), "Roboto-Light.ttf");
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
			view.setTypeface(fontRobotoLight);
		}
	}

	public void setTypefaceThin(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoThin);
		}
	}

	public void setTypefaceDefault(TextView view) {
		if (view != null) {
			view.setTypeface(fontRobotoLight);
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

}
