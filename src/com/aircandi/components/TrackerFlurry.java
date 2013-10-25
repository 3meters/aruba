package com.aircandi.components;

import java.util.Map;

import android.app.Activity;
import android.util.Log;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.Type;

@SuppressWarnings("ucd")
public class TrackerFlurry extends TrackerBase {

	public static final boolean	FLURRY_ENABLED			= true;
	public static final String	FLURRY_API_KEY			= "TD3X5PH7JG8SCKZVGFRT";
	public static final long	FLURRY_SESSION_TIMEOUT	= 10000L;					// milliseconds
	public static final int		FLURRY_LOG_LEVEL		= Log.VERBOSE;

	@Override
	public void sendEvent(String category, String action, String target, long value) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				Map<String, String> map = Maps.asMap("target", target);
				map.put("category", category.toString());
				//FlurryAgent.logEvent(action, map);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendException(Exception exception) {
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				//FlurryAgent.onError(exception.getClass().getSimpleName(), exception.getMessage(), exception);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void activityStart(Activity activity) {
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				startNewSession(activity);
				//FlurryAgent.onPageView();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void activityStop(Activity activity) {
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				stopSession(activity);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void applicationStart() {
//		FlurryAgent.setContinueSessionMillis(FLURRY_SESSION_TIMEOUT);
//		FlurryAgent.setCaptureUncaughtExceptions(true);
//		FlurryAgent.setUseHttps(true);
//		FlurryAgent.setLogEnabled(true);
//		FlurryAgent.setLogEvents(true);
//		FlurryAgent.setLogLevel(FLURRY_LOG_LEVEL);
	}

	private void startNewSession(Activity activity) {
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				//FlurryAgent.onStartSession(activity, FLURRY_API_KEY);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void stopSession(Activity activity) {
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				//FlurryAgent.onEndSession(activity);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
