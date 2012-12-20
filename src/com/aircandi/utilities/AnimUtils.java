package com.aircandi.utilities;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.aircandi.Aircandi;
import com.aircandi.R;

import android.app.Activity;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.content.res.Resources.NotFoundException;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;

public class AnimUtils {

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
		mFadeInMedium = AnimUtils.loadAnimation(R.anim.fade_in_medium);
		return mFadeInMedium;
	}

	public static Animation fadeOutMedium() {
		/*
		 * Same comment as above
		 */
		mFadeOutMedium = AnimUtils.loadAnimation(R.anim.fade_out_medium);
		return mFadeOutMedium;
	}

	public static void showView(final View view) {
		Animation animation = AnimUtils.fadeInMedium();
		if (view.getVisibility() != View.VISIBLE) {
			view.setVisibility(View.VISIBLE);
		}
		else {
			return;
		}
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {}

			@Override
			public void onAnimationEnd(Animation animation) {
				view.clearAnimation();
			}

			@Override
			public void onAnimationRepeat(Animation animation) {}
		});
		view.startAnimation(animation);
	}

	public static void hideView(final View view) {
		Animation animation = AnimUtils.fadeOutMedium();
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {}

			@Override
			public void onAnimationEnd(Animation animation) {
				view.clearAnimation();
			}

			@Override
			public void onAnimationRepeat(Animation animation) {}
		});
		view.startAnimation(animation);
	}

	public static enum TransitionType {
		CandiPageToForm,
		FormToCandiPage,
		FormToCandiPageAfterDelete,
		CandiPageToCandiPage,
		CandiMapToCandiPage,
		CandiFormToCandiList,
		CandiRadarToCandiForm,
		CandiListToCandiForm,
		CandiPageToMyCandi,
		CandiPageToCandiMap,
		CandiPageToCandiRadar,
		CandiPageToAndroidApp,
		CandiPageBack,
		HelpShow,
		HelpHide,
		None
	}

	public static void doOverridePendingTransition(Activity activity, TransitionType transitionType) {
		/*
		 * Generic candi to candi
		 */
		if (transitionType == TransitionType.CandiRadarToCandiForm) {
			activity.overridePendingTransition(R.anim.activity_open_enter, R.anim.hold);
		}
		else if (transitionType == TransitionType.CandiPageBack) {
			activity.overridePendingTransition(R.anim.hold, R.anim.activity_close_exit);
		}
		else if (transitionType == TransitionType.CandiFormToCandiList) {
			activity.overridePendingTransition(R.anim.fade_in_medium, R.anim.fade_out_medium);
		}
		else if (transitionType == TransitionType.CandiListToCandiForm) {
			activity.overridePendingTransition(R.anim.fade_in_medium, R.anim.fade_out_medium);
		}
		else if (transitionType == TransitionType.CandiPageToAndroidApp) {
			activity.overridePendingTransition(R.anim.activity_open_enter, R.anim.hold);
		}
		/*
		 * Moving between primary tabs
		 */
		else if (transitionType == TransitionType.CandiMapToCandiPage) {
			activity.overridePendingTransition(R.anim.fade_in_medium, R.anim.fade_out_medium);
		}
		else if (transitionType == TransitionType.CandiPageToCandiRadar) {
			activity.overridePendingTransition(R.anim.fade_in_medium, R.anim.fade_out_medium);
		}
		else if (transitionType == TransitionType.CandiPageToMyCandi) {
			activity.overridePendingTransition(R.anim.fade_in_medium, R.anim.fade_out_long);
		}
		else if (transitionType == TransitionType.CandiPageToCandiMap) {
			activity.overridePendingTransition(R.anim.fade_in_medium, R.anim.fade_out_long);
		}
		/*
		 * Jumping to and from forms
		 */
		else if (transitionType == TransitionType.CandiPageToForm) {
			activity.overridePendingTransition(R.anim.fade_zoom_in, R.anim.hold);
		}
		else if (transitionType == TransitionType.FormToCandiPage) {
			activity.overridePendingTransition(R.anim.hold, R.anim.fade_zoom_out);
		}
		else if (transitionType == TransitionType.FormToCandiPageAfterDelete) {
			activity.overridePendingTransition(R.anim.hold, R.anim.help_zoom_out);
		}
		else if (transitionType == TransitionType.None) {
			activity.overridePendingTransition(0, 0);
		}
		/*
		 * Jumping to and from help
		 */
		else if (transitionType == TransitionType.HelpShow) {
			activity.overridePendingTransition(R.anim.help_zoom_in, R.anim.hold);
		}
		else if (transitionType == TransitionType.HelpHide) {
			activity.overridePendingTransition(R.anim.hold, R.anim.help_zoom_out);
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
			return AnimUtils.createAnimationFromXml(Aircandi.applicationContext, parser);
		}
		catch (XmlPullParserException ex) {
			NotFoundException rnf = new NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(animationResId));
			rnf.initCause(ex);
			throw rnf;
		}
		catch (IOException ex) {
			NotFoundException rnf = new NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(animationResId));
			rnf.initCause(ex);
			throw rnf;
		}
		finally {
			if (parser != null)
				parser.close();
		}
	}

	private static Animation createAnimationFromXml(Context c, XmlPullParser parser) throws XmlPullParserException, IOException {
		return AnimUtils.createAnimationFromXml(c, parser, null, Xml.asAttributeSet(parser));
	}

	private static Animation createAnimationFromXml(Context c, XmlPullParser parser, AnimationSet parent, AttributeSet attrs) throws XmlPullParserException,
			IOException {

		Animation anim = null;

		/* Make sure we are on a start tag. */
		int type;
		int depth = parser.getDepth();

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