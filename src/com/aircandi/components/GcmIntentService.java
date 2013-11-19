package com.aircandi.components;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.applications.Comments;
import com.aircandi.service.objects.Action.EventCategory;
import com.aircandi.service.objects.ServiceMessage;
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
					String jsonMessage = extras.getString("message");
					ServiceMessage message = (ServiceMessage) Json.jsonToObject(jsonMessage, Json.ObjectType.SERVICE_MESSAGE);

					if (message != null) {

						/* Is this a message event we know how to handle */
						if (message.action.getEventCategory().equals(EventCategory.UNKNOWN)) return;

						/*
						 * If user is currently on the activities list, it will be auto refreshed so don't show
						 * notification in the status bar. Don't need to broadcast either.
						 */
						android.app.Activity currentActivity = Aircandi.getInstance().getCurrentActivity();
						if (currentActivity != null && currentActivity.getClass().equals(AircandiForm.class)) {
							BaseFragment fragment = ((AircandiForm) currentActivity).getCurrentFragment();
							if (fragment != null && fragment.getClass().equals(ActivityFragment.class)) {
								return;
							}
						}

						/* Cherry pick pure refresh notifications */
						if (message.action.getEventCategory().equals(EventCategory.REFRESH)) {
							MessagingManager.getInstance().broadcastMessage(message);
							return;
						}

						/* Build intent that can be used in association with the notification */
						if (message.action.entity != null) {
							if (message.action.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
								message.intent = Comments.viewForGetIntent(Aircandi.applicationContext, message.action.toEntity.id,
										Constants.TYPE_LINK_CONTENT,
										null, null);
							}
							else {
								Class<?> clazz = BaseEntityForm.viewFormBySchema(message.action.entity.schema);
								IntentBuilder intentBuilder = new IntentBuilder(Aircandi.applicationContext, clazz)
										.setEntityId(message.action.entity.id)
										.setEntitySchema(message.action.entity.schema)
										.setForceRefresh(true);
								message.intent = intentBuilder.create();
							}
						}

						/* Customize title and subtitle before storing and broadcasting */
						Activities.decorate(message);

						/* Trigger event so subscribers can decide if they care about the activity */
						MessagingManager.getInstance().broadcastMessage(message);

						/*
						 * Tickle activity date on current user to flag auto refresh for activity list. This service
						 * can be woken up when we don't have a current user.
						 */
						if (Aircandi.getInstance().getCurrentUser() != null) {
							Aircandi.getInstance().getCurrentUser().activityDate = message.sentDate;
						}

						/* Send notification */
						MessagingManager.getInstance().notificationForMessage(message, Aircandi.applicationContext);
					}
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
