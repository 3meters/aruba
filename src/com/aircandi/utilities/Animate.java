package com.aircandi.utilities;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import com.aircandi.Aircandi;
import com.aircandi.R;

public class Animate {

	private static Animation	mFadeInMedium;
	private static Animation	mFadeOutMedium;

	public static Animation fadeInMedium() {
		/*
		 * We make a new animation object each time because when I
		 * tried sharing one, there was lots of flashing and weird behavior.
		 * 
		 * If there is a better way to do this later then this will serve
		 * as a choke point for the implementation.
		 */
		mFadeInMedium = Animate.loadAnimation(R.anim.fade_in_medium);
		return mFadeInMedium;
	}

	public static Animation fadeOutMedium() {
		/*
		 * Same comment as above
		 */
		mFadeOutMedium = Animate.loadAnimation(R.anim.fade_out_medium);
		return mFadeOutMedium;
	}

	public static enum TransitionType {
		PAGE_TO_FORM,
		FORM_TO_PAGE,
		RADAR_TO_PAGE,
		PAGE_TO_SOURCE,
		PAGE_BACK,
		PAGE_TO_PAGE,
		FORM_TO_PAGE_AFTER_DELETE,
		PAGE_TO_RADAR_AFTER_DELETE,
		PAGE_TO_HELP,
		CANDIGRAM_OUT,
		HELP_TO_PAGE,
		NONE,
	}

	public static void doOverridePendingTransition(Activity activity, TransitionType transitionType) {
		doOverridePendingTransitionDefault(activity, transitionType);
	}

	@SuppressWarnings("ucd")
	public static void doOverridePendingTransitionStackTop(Activity activity, TransitionType transitionType) {
		/*
		 * Browsing transitions
		 */
		if (transitionType == TransitionType.RADAR_TO_PAGE) {
			activity.overridePendingTransition(R.anim.slide_in_right, R.anim.page_fade_zoom_out);
		}
		else if (transitionType == TransitionType.PAGE_BACK) {
			activity.overridePendingTransition(R.anim.page_fade_zoom_in, R.anim.slide_out_right);
		}
		else if (transitionType == TransitionType.PAGE_TO_PAGE) {
			activity.overridePendingTransition(R.anim.slide_in_right, R.anim.page_fade_zoom_out);
		}
		else if (transitionType == TransitionType.PAGE_TO_SOURCE) {
			activity.overridePendingTransition(R.anim.slide_in_right, R.anim.page_fade_zoom_out);
		}
		else if (transitionType == TransitionType.PAGE_TO_HELP) {
			activity.overridePendingTransition(R.anim.slide_in_right, R.anim.page_fade_zoom_out);
		}
		/*
		 * Jumping to and from forms
		 */
		else if (transitionType == TransitionType.PAGE_TO_FORM) {
			activity.overridePendingTransition(R.anim.fade_zoom_in, R.anim.hold);
		}
		else if (transitionType == TransitionType.FORM_TO_PAGE) {
			activity.overridePendingTransition(R.anim.hold, R.anim.fade_zoom_out);
		}
		else if (transitionType == TransitionType.PAGE_TO_RADAR_AFTER_DELETE) {}
		else if (transitionType == TransitionType.FORM_TO_PAGE_AFTER_DELETE) {
			activity.overridePendingTransition(R.anim.hold, R.anim.help_zoom_out);
		}
		else if (transitionType == TransitionType.NONE) {
			activity.overridePendingTransition(0, 0);
		}
	}

	@SuppressWarnings("ucd")
	public static void doOverridePendingTransitionDefault(Activity activity, TransitionType transitionType) {
		/*
		 * Default android animations are used unless overridden here.
		 */
		if (transitionType == TransitionType.PAGE_TO_HELP) {
			activity.overridePendingTransition(R.anim.fade_in_short, R.anim.hold);
		}
		else if (transitionType == TransitionType.CANDIGRAM_OUT) {
			activity.overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
		}
		else if (transitionType == TransitionType.HELP_TO_PAGE) {
			activity.overridePendingTransition(R.anim.hold, R.anim.fade_out_short);
		}
		
	}

	@SuppressWarnings("ucd")
	public static void doOverridePendingTransitionStackBottom(Activity activity, TransitionType transitionType) {
		/*
		 * Browsing transitions
		 */
		if (transitionType == TransitionType.RADAR_TO_PAGE) {
			activity.overridePendingTransition(R.anim.slide_in_right, R.anim.fade_zoom_in);
		}
		else if (transitionType == TransitionType.PAGE_BACK) {
			activity.overridePendingTransition(R.anim.fade_zoom_out, R.anim.slide_out_right);
		}
		else if (transitionType == TransitionType.PAGE_TO_PAGE) {
			activity.overridePendingTransition(R.anim.slide_in_right, R.anim.fade_zoom_in);
		}
		else if (transitionType == TransitionType.PAGE_TO_SOURCE) {
			activity.overridePendingTransition(R.anim.slide_in_right, R.anim.fade_zoom_in);
		}
		/*
		 * Jumping to and from forms
		 */
		else if (transitionType == TransitionType.PAGE_TO_FORM) {
			activity.overridePendingTransition(R.anim.fade_zoom_out, R.anim.hold);
		}
		else if (transitionType == TransitionType.FORM_TO_PAGE) {
			activity.overridePendingTransition(R.anim.hold, R.anim.fade_zoom_in);
		}
		else if (transitionType == TransitionType.PAGE_TO_RADAR_AFTER_DELETE) {}
		else if (transitionType == TransitionType.FORM_TO_PAGE_AFTER_DELETE) {
			activity.overridePendingTransition(R.anim.hold, R.anim.help_zoom_out);
		}
		else if (transitionType == TransitionType.NONE) {
			activity.overridePendingTransition(0, 0);
		}
	}

	@SuppressWarnings("ucd")
	public static void doOverridePendingTransitionOld(Activity activity, TransitionType transitionType) {
		/*
		 * Browsing transitions
		 */
		if (transitionType == TransitionType.RADAR_TO_PAGE) {
			activity.overridePendingTransition(R.anim.activity_open_enter, R.anim.hold);
		}
		else if (transitionType == TransitionType.PAGE_TO_PAGE) {
			activity.overridePendingTransition(R.anim.fade_in_medium, R.anim.fade_out_medium);
		}
		else if (transitionType == TransitionType.PAGE_BACK) {
			activity.overridePendingTransition(R.anim.hold, R.anim.activity_close_exit);
		}
		else if (transitionType == TransitionType.PAGE_TO_SOURCE) {
			activity.overridePendingTransition(R.anim.activity_open_enter, R.anim.hold);
		}
		/*
		 * Jumping to and from forms
		 */
		else if (transitionType == TransitionType.PAGE_TO_FORM) {
			activity.overridePendingTransition(R.anim.fade_zoom_in, R.anim.hold);
		}
		else if (transitionType == TransitionType.FORM_TO_PAGE) {
			activity.overridePendingTransition(R.anim.hold, R.anim.fade_zoom_out);
		}
		else if (transitionType == TransitionType.PAGE_TO_RADAR_AFTER_DELETE) {}
		else if (transitionType == TransitionType.FORM_TO_PAGE_AFTER_DELETE) {
			activity.overridePendingTransition(R.anim.hold, R.anim.help_zoom_out);
		}
		else if (transitionType == TransitionType.NONE) {
			activity.overridePendingTransition(0, 0);
		}
	}

	public static Animation loadAnimation(int animationResId) throws NotFoundException {
		/*
		 * Loads an animation object from a resource
		 * 
		 * @param id The resource id of the animation to load
		 * 
		 * @return The animation object reference by the specified id
		 * 
		 * @throws NotFoundException when the animation cannot be loaded
		 */

		XmlResourceParser parser = null;
		try {
			parser = Aircandi.applicationContext.getResources().getAnimation(animationResId);
			return Animate.createAnimationFromXml(Aircandi.applicationContext, parser);
		}
		catch (XmlPullParserException ex) {
			final NotFoundException rnf = new NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(animationResId));
			rnf.initCause(ex);
			throw rnf;
		}
		catch (IOException ex) {
			final NotFoundException rnf = new NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(animationResId));
			rnf.initCause(ex);
			throw rnf;
		}
		finally {
			if (parser != null) {
				parser.close();
			}
		}
	}

	private static Animation createAnimationFromXml(Context c, XmlPullParser parser) throws XmlPullParserException, IOException {
		return Animate.createAnimationFromXml(c, parser, null, Xml.asAttributeSet(parser));
	}

	private static Animation createAnimationFromXml(Context c, XmlPullParser parser, AnimationSet parent, AttributeSet attrs) throws XmlPullParserException,
			IOException {

		Animation anim = null;

		/* Make sure we are on a start tag. */
		int type;
		final int depth = parser.getDepth();

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			String name = parser.getName();

			if (name.equals("set")) {
				anim = new AnimationSet(c, attrs);
				createAnimationFromXml(c, parser, (AnimationSet) anim, attrs);
			}
			else if (name.equals("alpha")) {
				anim = new AlphaAnimation(c, attrs);
			}
			else if (name.equals("scale")) {
				anim = new ScaleAnimation(c, attrs);
			}
			else if (name.equals("rotate")) {
				anim = new RotateAnimation(c, attrs);
			}
			else if (name.equals("translate")) {
				anim = new TranslateAnimation(c, attrs);
			}
			else {
				throw new RuntimeException("Unknown animation name: " + parser.getName());
			}

			if (parent != null) {
				parent.addAnimation(anim);
			}
		}
		return anim;
	}
}
