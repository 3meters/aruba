package com.aircandi.components;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.applications.Comments;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.ui.AircandiForm;
import com.aircandi.ui.NewsFragment;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Notifications;
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
					String jsonNotification = extras.getString("notification");
					AirNotification notification = (AirNotification) Json.jsonToObject(jsonNotification, Json.ObjectType.AIR_NOTIFICATION);

					/* We don't self notify unless dev settings are on and self notify is enabled */
					if (!Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
							|| !Aircandi.settings.getBoolean(Constants.PREF_TESTING_SELF_NOTIFY, Constants.PREF_TESTING_SELF_NOTIFY_DEFAULT)) {
						if (notification.user != null
								&& Aircandi.getInstance().getCurrentUser() != null
								&& notification.user.id.equals(Aircandi.getInstance().getCurrentUser().id)) {
							return;
						}
					}

					/* Build intent that can be used in association with the notification */
					if (notification.entity != null) {
						if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
							notification.intent = Comments.viewForGetIntent(Aircandi.applicationContext, notification.toEntity.id, Constants.TYPE_LINK_CONTENT,
									null, null);
						}
						else {
							Class<?> clazz = BaseEntityForm.viewFormBySchema(notification.entity.schema);
							IntentBuilder intentBuilder = new IntentBuilder(Aircandi.applicationContext, clazz)
									.setEntityId(notification.entity.id)
									.setEntitySchema(notification.entity.schema)
									.setForceRefresh(true);
							notification.intent = intentBuilder.create();
						}
					}

					/* Customize title and subtitle before storing and broadcasting */
					Notifications.decorate(notification);

					/* Stash in our local database */
					MessagingManager.getInstance().storeNotification(notification, jsonNotification);

					/* Trigger event so subscribers can decide if they should refresh */
					MessagingManager.getInstance().broadcastNotification(notification);

					/* Display if user is not currently using the notifications activity */
					Activity currentActivity = Aircandi.getInstance().getCurrentActivity();
					if (currentActivity != null && currentActivity.getClass().equals(AircandiForm.class)) {
						BaseFragment fragment = ((AircandiForm) currentActivity).getCurrentFragment();
						if (fragment.getClass().equals(NewsFragment.class)) {
							return;
						}
					}

					MessagingManager.getInstance().showNotification(notification, Aircandi.applicationContext);

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
