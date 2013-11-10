package com.aircandi.components;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.applications.Comments;
import com.aircandi.service.objects.Action.EventType;
import com.aircandi.service.objects.Activity;
import com.aircandi.ui.ActivityFragment;
import com.aircandi.ui.AircandiForm;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.utilities.Activities;
import com.aircandi.utilities.Json;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmIntentService extends IntentService {

	// wakelock
	private static final String				WAKELOCK_KEY	= "GCM_LIB";
	private static PowerManager.WakeLock	sWakeLock;

	// Java lock used to synchronize access to sWakelock
	private static final Object				LOCK			= GcmIntentService.class;

	public GcmIntentService() {
		super(Constants.SENDER_ID);
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		try {

			Bundle extras = intent.getExtras();
			GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
			String messageType = gcm.getMessageType(intent);

			if (!extras.isEmpty()) {  // has effect of unparcelling Bundle

				/*
				 * Filter messages based on message type. Since it is likely that GCM will be
				 * extended in the future with new message types, just ignore any message types you're
				 * not interested in, or that you don't recognize.
				 */
				if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

					/*
					 * Called when our server sends a message to GCM, and GCM delivers it to the install. If the message
					 * has
					 * a payload, its contents are available as extras in the intent.
					 */
					String jsonActivity = extras.getString("activity");
					Activity activity = (Activity) Json.jsonToObject(jsonActivity, Json.ObjectType.ACTIVITY);

					/* We don't self notify unless dev settings are on and self notify is enabled */
					if (!Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
							|| !Aircandi.settings.getBoolean(Constants.PREF_TESTING_SELF_NOTIFY, Constants.PREF_TESTING_SELF_NOTIFY_DEFAULT)) {
						if (activity.action.user != null
								&& Aircandi.getInstance().getCurrentUser() != null
								&& activity.action.user.id.equals(Aircandi.getInstance().getCurrentUser().id)) {
							return;
						}
					}
					
					/* Cherry pick refresh notifications */
					if (activity.action.getEventCategory().equals(EventType.REFRESH)) {
						MessagingManager.getInstance().broadcastActivity(activity);
						return;
					}

					/* Build intent that can be used in association with the notification */
					if (activity.action.entity != null) {
						if (activity.action.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
							activity.intent = Comments.viewForGetIntent(Aircandi.applicationContext, activity.action.toEntity.id, Constants.TYPE_LINK_CONTENT,
									null, null);
						}
						else {
							Class<?> clazz = BaseEntityForm.viewFormBySchema(activity.action.entity.schema);
							IntentBuilder intentBuilder = new IntentBuilder(Aircandi.applicationContext, clazz)
									.setEntityId(activity.action.entity.id)
									.setEntitySchema(activity.action.entity.schema)
									.setForceRefresh(true);
							activity.intent = intentBuilder.create();
						}
					}

					/* Customize title and subtitle before storing and broadcasting */
					Activities.decorate(activity);

					/* Trigger event so subscribers can decide if they should refresh */
					MessagingManager.getInstance().broadcastActivity(activity);

					/* Display if user is not currently using the notifications activity */
					android.app.Activity currentActivity = Aircandi.getInstance().getCurrentActivity();
					if (currentActivity != null && currentActivity.getClass().equals(AircandiForm.class)) {
						BaseFragment fragment = ((AircandiForm) currentActivity).getCurrentFragment();
						if (fragment.getClass().equals(ActivityFragment.class)) {
							return;
						}
					}

					MessagingManager.getInstance().showActivity(activity, Aircandi.applicationContext);

				}
			}
		}
		finally {
			/*
			 * Release the power lock, so phone can get back to sleep. The lock is reference-counted by default, so
			 * multiple messages are ok.
			 */
			synchronized (LOCK) {
				if (sWakeLock != null) {
					Logger.v(this, "Releasing wakelock");
					sWakeLock.release();
				}
				else {
					Logger.e(this, "Wakelock reference is null");
				}
			}
		}

	}

	static void runIntentInService(Context context, Intent intent, String className) {
		synchronized (LOCK) {
			if (sWakeLock == null) {
				/* This is called from BroadcastReceiver, there is no init. */
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_KEY);
			}
		}
		Logger.v(context, "Acquiring wakelock");
		sWakeLock.acquire();
		intent.setClassName(context, className);
		context.startService(intent);
	}
}
