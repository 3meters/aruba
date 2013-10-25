package com.aircandi.components;

import android.app.Activity;
import android.support.v4.app.Fragment;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.Type;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GAServiceManager;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Logger.LogLevel;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;
import com.google.analytics.tracking.android.Tracker;

/*
 * Tracker strategy
 * 
 * - Every activity is a page view when initialized.
 * - Page views and events info is dispatched to google service automatically
 * by EasyTracker.
 * 
 * - Select events are tracked
 * - Insert, update, delete entity
 * - user clicks refresh
 * - Insert, update user
 * - Comment created
 * - user signin, signout
 * 
 * More candidates
 * - Preferences modified
 */

@SuppressWarnings("ucd")
public class TrackerGoogle extends TrackerBase {

	public static final boolean		GA_ENABLED			= true;
	public static final String		GA_PROPERTY_ID		= "UA-33660954-1";
	public static final int			GA_DISPATCH_PERIOD	= 10;				// seconds																				// Dispatch period in seconds.
	public static final int			GA_SESSION_TIMEOUT	= 300;				// seconds																				// Dispatch period in seconds.
	public static final boolean		GA_IS_DRY_RUN		= false;
	public static final LogLevel	GA_LOG_VERBOSITY	= LogLevel.VERBOSE;
	public static final double		GA_SAMPLE_RATE		= 100.0d;

	public static Tracker			tracker;
	public static GoogleAnalytics	googleAnalytics;

	@Override
	public void sendEvent(String category, String action, String target, long value) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				tracker.send(MapBuilder.createEvent(category.toString(), action, target, value).build());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendTiming(String category, Long timing, String name, String label) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				tracker.send(MapBuilder.createTiming(category.toString(), timing, name, label).build());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendException(Exception exception) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				tracker.send(MapBuilder.createException(new StandardExceptionParser(Aircandi.applicationContext, null)
						.getDescription(Thread.currentThread().getName(), exception), false)
						.build());
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
				/*
				 * Screen name as set will be included in all subsequent sends.
				 */
				tracker.set(Fields.SCREEN_NAME, activity.getClass().getSimpleName());
				tracker.send(MapBuilder.createAppView().build());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void fragmentStart(Fragment fragment) {
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				/*
				 * Screen name as set will be included in all subsequent sends.
				 */
				tracker.set(Fields.SCREEN_NAME, fragment.getClass().getSimpleName());
				tracker.send(MapBuilder.createAppView().build());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void applicationStart() {
		GAServiceManager.getInstance().setLocalDispatchPeriod(GA_DISPATCH_PERIOD);

		googleAnalytics = GoogleAnalytics.getInstance(Aircandi.applicationContext);
		googleAnalytics.setDryRun(GA_IS_DRY_RUN);
		googleAnalytics.getLogger().setLogLevel(GA_LOG_VERBOSITY);

		tracker = googleAnalytics.getTracker(GA_PROPERTY_ID);
		tracker.set(Fields.SAMPLE_RATE, String.valueOf(GA_SAMPLE_RATE));
	}

	@SuppressWarnings("unused")
	private void startNewSession() {
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				tracker.send(MapBuilder
						.createEvent(TrackerCategory.SYSTEM, "session_start", null, null)
						.set(Fields.SESSION_CONTROL, "start")
						.build()
						);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private void stopSession() {
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				tracker.send(MapBuilder
						.createEvent(TrackerCategory.SYSTEM, "session_end", null, null)
						.set(Fields.SESSION_CONTROL, "end")
						.build()
						);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
