package com.aircandi.components;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.aircandi.CandiConstants;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.ui.CandiForm;
import com.aircandi.ui.CandiRadar;
import com.aircandi.ui.CommentList;
import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {

	public static Activity		currentActivity;
	public static final Object	lock	= new Object();

	public GCMIntentService() {
		super(CandiConstants.SENDER_ID);
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
		AirNotification airNotification = (AirNotification) HttpService.convertJsonToObjectInternalSmart(jsonNotification, ServiceDataType.AirNotification);

		/* Build intent that can be used in association with the notification */
		if (airNotification.subject.equals("entity") || airNotification.subject.equals("comment")) {
			final IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class)
					.setCommandType(CommandType.View)
					.setEntityId(airNotification.entity.id)
					.setEntityType(airNotification.entity.type)
					.setForceRefresh(true);
			Intent intent = intentBuilder.create();
			airNotification.intent = intent;
		}

		/* Stash in our queue */
		NotificationManager.getInstance().getNotifications().add(airNotification);

		/* See if target is visible and refresh */
		synchronized (lock) {
			if (currentActivity != null) {
				if (airNotification.subject.equals("comment")) {
					if (currentActivity.getClass() == CandiForm.class) {
						final CandiForm activity = (CandiForm) currentActivity;
						if (activity.getCommon().mEntityId.equals(airNotification.entity.id)) {
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									activity.getCommon().doRefreshClick();
								}
							});
						}
					}
					else if (currentActivity.getClass() == CommentList.class) {
						final CommentList activity = (CommentList) currentActivity;
						if (activity.getCommon().mEntityId.equals(airNotification.entity.id)) {
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									activity.getCommon().doRefreshClick();
								}
							});
						}
					}
				}
				else if (airNotification.subject.equals("entity")) {
					if (airNotification.entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
						if (currentActivity.getClass() == CandiRadar.class) {
							final CandiRadar activity = (CandiRadar) currentActivity;
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									activity.getCommon().doRefreshClick();
								}
							});
						}
					}
					else {
						if (currentActivity.getClass() == CandiForm.class) {
							final CandiForm activity = (CandiForm) currentActivity;
							if (airNotification.entity.parentId != null) {
								if (activity.getCommon().mEntityId.equals(airNotification.entity.parentId)) {
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
		ProxiManager.getInstance().getEntityModel().processNotification(airNotification);

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
