package com.aircandi.components;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.aircandi.Constants;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ObjectType;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.ui.EntityList;
import com.aircandi.ui.RadarForm;
import com.aircandi.ui.base.BaseEntityView;
import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {

	public static Activity		currentActivity;
	public static final Object	lock	= new Object();

	public GCMIntentService() {
		super(Constants.SENDER_ID);
	}

	/*
	 * Note: The methods below run in the intent service's thread and hence are free to make network calls without the
	 * risk of blocking the UI thread.
	 */

	@Override
	protected void onRegistered(Context context, String registrationId) {
		/*
		 * Called after a registration intent is received, passes the registration ID assigned by GCM to that
		 * device/application pair as parameter. Typically, you should send the regid to your server so it can use it to
		 * send messages to this device.
		 */
		Logger.i(this, "GCM: Device registration id is: " + registrationId);
	}

	@Override
	protected void onMessage(Context context, Intent messageIntent) {
		/*
		 * Called when our server sends a message to GCM, and GCM delivers it to the device. If the message has a
		 * payload, its contents are available as extras in the intent.
		 */
		String jsonNotification = messageIntent.getStringExtra("notification");
		AirNotification airNotification = (AirNotification) HttpService.jsonToObject(jsonNotification, ObjectType.AirNotification);

		/* Build intent that can be used in association with the notification */
		if (airNotification.entity != null) {
			final IntentBuilder intentBuilder = new IntentBuilder()
					.setEntityId(airNotification.entity.id)
					.setEntitySchema(airNotification.entity.type)
					.setForceRefresh(true);
			Intent intent = intentBuilder.create();
			airNotification.intent = intent;
		}

		/* Stash in our queue */
		NotificationManager.getInstance().getNotifications().add(airNotification);

		/* See if target is visible and refresh */
		synchronized (lock) {
			if (currentActivity != null) {
				if (airNotification.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
					if (currentActivity.getClass() == BaseEntityView.class) {
						final BaseEntityView activity = (BaseEntityView) currentActivity;
						if (activity.getCommon().mEntityId.equals(airNotification.entity.toId)) {
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									activity.getCommon().doRefreshClick();
								}
							});
						}
					}
					else if (currentActivity.getClass() == EntityList.class) {
						final EntityList activity = (EntityList) currentActivity;
						if (activity.getCommon().mEntityId.equals(airNotification.entity.toId)) {
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									activity.getCommon().doRefreshClick();
								}
							});
						}
					}
				}
				else {
					if (airNotification.entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
						if (currentActivity.getClass() == RadarForm.class) {
							final RadarForm activity = (RadarForm) currentActivity;
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									activity.getCommon().doRefreshClick();
								}
							});
						}
					}
					else {
						if (currentActivity.getClass() == BaseEntityView.class) {
							final BaseEntityView activity = (BaseEntityView) currentActivity;
							if (airNotification.entity.toId != null) {
								if (activity.getCommon().mEntityId.equals(airNotification.entity.toId)) {
									activity.runOnUiThread(new Runnable() {
										@Override
										public void run() {
											activity.getCommon().doRefreshClick();
										}
									});
								}
							}
						}
					}
				}
			}
		}

		/* Pass to the cache */
		EntityManager.getInstance().processNotification(airNotification);

		/* Display */
		NotificationManager.getInstance().showNotification(airNotification, context);
	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		/*
		 * Called after the device has been unregistered from GCM. Typically, you should send the regid to the server so
		 * it unregisters the device.
		 */
		Logger.i(this, "GCM: Unregistered");
		NotificationManager.getInstance().unregisterDeviceWithAircandi(registrationId);
	}

	@Override
	protected void onError(Context context, String errorId) {
		/*
		 * Called when the device tries to register or unregister, but GCM returned an error. Typically, there is
		 * nothing to be done other than evaluating the error (returned by errorId) and trying to fix the problem.
		 */
		Logger.i(this, "GCM: Error: " + errorId);
	}
}
