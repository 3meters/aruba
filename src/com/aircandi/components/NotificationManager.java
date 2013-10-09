package com.aircandi.components;

import org.apache.http.HttpStatus;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.BitmapResponse;
import com.aircandi.events.MessageEvent;
import com.aircandi.service.RequestListener;
import com.aircandi.service.ServiceResponse;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.service.objects.AirNotification.ActionType;
import com.aircandi.service.objects.AirNotification.NotificationType;
import com.aircandi.service.objects.Device;
import com.aircandi.service.objects.User;
import com.aircandi.ui.AircandiForm;
import com.aircandi.utilities.Json;
import com.google.android.gcm.GCMRegistrar;

@SuppressWarnings("ucd")
public class NotificationManager {

	public static android.app.NotificationManager	mNotificationManager;
	private Device									mDevice;
	private Integer									mNewCount	= 0;

	private NotificationManager() {
		mNotificationManager = (android.app.NotificationManager) Aircandi.applicationContext.getSystemService(Service.NOTIFICATION_SERVICE);
	}

	private static class NotificationManagerHolder {
		public static final NotificationManager	instance	= new NotificationManager();
	}

	public static NotificationManager getInstance() {
		return NotificationManagerHolder.instance;
	}

	// --------------------------------------------------------------------------------------------
	// GCM
	// --------------------------------------------------------------------------------------------	

	public void registerDeviceWithGCM() {
		/*
		 * Registration gets redone if app version code changes. Registration id
		 * and associated app version code are stored in shared prefs.
		 */
		if (!GCMRegistrar.isRegistered(Aircandi.applicationContext)) {
			GCMRegistrar.register(Aircandi.applicationContext, Constants.SENDER_ID);
		}
	}

	public void unregisterDeviceWithGCM() {
		if (GCMRegistrar.isRegistered(Aircandi.applicationContext)) {
			GCMRegistrar.unregister(Aircandi.applicationContext);
		}
	}

	public void registerDeviceWithAircandi() {
		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("RegisterDevice");

				if (GCMRegistrar.isRegistered(Aircandi.applicationContext)
						&& !GCMRegistrar.isRegisteredOnServer(Aircandi.applicationContext)
						&& Aircandi.getInstance().getUser() != null) {

					Logger.i(this, "GCM: Registering device with Aircandi notification service");

					Device device = new Device(Aircandi.getInstance().getUser().id, GCMRegistrar.getRegistrationId(Aircandi.applicationContext));
					device.clientVersionName = Aircandi.getVersionName(Aircandi.applicationContext, AircandiForm.class);
					device.clientVersionCode = Aircandi.getVersionCode(Aircandi.applicationContext, AircandiForm.class);

					ModelResult result = EntityManager.getInstance().registerDevice(true, device);

					if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
						Logger.i(this, "GCM: device registered with Aircandi notification service");
						final String jsonResponse = (String) result.serviceResponse.data;
						mDevice = (Device) Json.jsonToObject(jsonResponse, Json.ObjectType.DEVICE);
						GCMRegistrar.setRegisteredOnServer(Aircandi.applicationContext, true);
					}
				}
				return null;
			}
		}.execute();
	}

	public void unregisterDeviceWithAircandi(final String registrationId) {
		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("UnregisterDevice");

				/* service notifications */
				if (!TextUtils.isEmpty(registrationId)) {

					Logger.i(this, "GCM: Unregistering device with Aircandi notification service");
					User user = Aircandi.getInstance().getUser();
					Device device = new Device(user != null ? user.id : null, registrationId);
					ModelResult result = EntityManager.getInstance().registerDevice(false, device);

					if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
						Logger.i(this, "GCM: device successfully unregistered with Aircandi notification service");
						GCMRegistrar.setRegisteredOnServer(Aircandi.applicationContext, false);
					}
					else {
						if (result.serviceResponse.statusCode != null && result.serviceResponse.statusCode == HttpStatus.SC_NOT_FOUND) {
							Logger.i(this, "GCM: device already unregistered with Aircandi notification service");
							GCMRegistrar.setRegisteredOnServer(Aircandi.applicationContext, false);
						}
						else {
							/* TODO: What should we do if unregister fails? */
						}
					}
				}
				else {
					GCMRegistrar.setRegisteredOnServer(Aircandi.applicationContext, false);
				}
				return null;
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Notifications
	// --------------------------------------------------------------------------------------------	

	public void broadcastNotification(final AirNotification notification) {
		BusProvider.getInstance().post(new MessageEvent(notification));
	}

	public void showNotification(final AirNotification airNotification, Context context) {
		/*
		 * Small icon displays on left unless a large icon is specified
		 * and then it moves to the right.
		 */
		if (airNotification.type != null) {
			if (airNotification.type.equals(NotificationType.NEARBY)) {
				if (!Aircandi.settings.getBoolean(Constants.PREF_NOTIFICATIONS_NEARBY, Constants.PREF_NOTIFICATIONS_NEARBY_DEFAULT)) {
					return;
				}
			}
			else if (airNotification.type.equals(NotificationType.OWN)) {
				if (!Aircandi.settings.getBoolean(Constants.PREF_NOTIFICATIONS_OWN, Constants.PREF_NOTIFICATIONS_OWN_DEFAULT)) {
					return;
				}
			}
			else if (airNotification.type.equals(NotificationType.WATCH)
					|| airNotification.type.equals(NotificationType.WATCH_USER)) {
				if (!Aircandi.settings.getBoolean(Constants.PREF_NOTIFICATIONS_WATCH, Constants.PREF_NOTIFICATIONS_WATCH_DEFAULT)) {
					return;
				}
			}

			if (airNotification.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
				if (!Aircandi.settings.getBoolean(Constants.PREF_NOTIFICATIONS_COMMENTS, Constants.PREF_NOTIFICATIONS_COMMENTS_DEFAULT)) {
					return;
				}
			}
			else if (airNotification.entity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
				if (!Aircandi.settings.getBoolean(Constants.PREF_NOTIFICATIONS_PICTURES, Constants.PREF_NOTIFICATIONS_PICTURES_DEFAULT)) {
					return;
				}
			}
			else if (airNotification.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
				if (!Aircandi.settings.getBoolean(Constants.PREF_NOTIFICATIONS_CANDIGRAMS, Constants.PREF_NOTIFICATIONS_CANDIGRAMS_DEFAULT)) {
					return;
				}
			}
		}

		airNotification.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		airNotification.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		final PendingIntent pendingIntent = PendingIntent.getActivity(Aircandi.applicationContext, 0
				, airNotification.intent
				, PendingIntent.FLAG_CANCEL_CURRENT);

		/* Default base notification configuration */

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(Aircandi.applicationContext)
				.setContentTitle(airNotification.title)
				.setContentText(airNotification.subtitle)
				.setSmallIcon(R.drawable.ic_stat_notification)
				.setAutoCancel(true)
				.setOnlyAlertOnce(true)
				.setContentIntent(pendingIntent)
				.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS)
				.setWhen(System.currentTimeMillis());

		String byImageUri = airNotification.photoBy.getUri();

		/* Large icon */

		if (byImageUri != null) {
			final BitmapRequest bitmapRequest = new BitmapRequest()
					.setBitmapUri(byImageUri)
					.setBitmapRequestor(airNotification)
					.setBitmapSize((int) Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.notification_large_icon_width))
					.setRequestListener(new RequestListener() {

						@Override
						public void onComplete(Object response) {

							final ServiceResponse serviceResponse = (ServiceResponse) response;
							if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

								final BitmapResponse bitmapResponse = (BitmapResponse) serviceResponse.data;
								builder.setLargeIcon(bitmapResponse.bitmap);

								/* Enhance or go with default */

								if (airNotification.entity != null && airNotification.action.equals(ActionType.INSERT)) {
									if ((airNotification.entity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)
											|| airNotification.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM))
											&& airNotification.entity.getPhoto().getUri() != null) {
										useBigPicture(builder, airNotification);
									}
									else if (airNotification.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
										useBigText(builder, airNotification);
									}
								}
								else {
									mNotificationManager.notify(airNotification.action, 0, builder.build());
								}
							}
						}
					});

			BitmapManager.getInstance().masterFetch(bitmapRequest);
		}
		else {
			mNotificationManager.notify(airNotification.action, 0, builder.build());
		}
	}

	public void useBigPicture(final NotificationCompat.Builder builder, final AirNotification airNotification) {

		String imageUri = airNotification.entity.getPhoto().getUri();
		final BitmapRequest bitmapRequest = new BitmapRequest()
				.setBitmapUri(imageUri)
				.setBitmapRequestor(airNotification)
				.setBitmapSize((int) Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.notification_big_picture_width))
				.setRequestListener(new RequestListener() {

					@Override
					public void onComplete(Object response) {

						final ServiceResponse serviceResponse = (ServiceResponse) response;
						if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

							final BitmapResponse bitmapResponse = (BitmapResponse) serviceResponse.data;
							NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle()
									.bigPicture(bitmapResponse.bitmap)
									.setBigContentTitle(airNotification.title)
									.setSummaryText(airNotification.subtitle);

							builder.setStyle(style);

							mNotificationManager.notify(airNotification.action, 0, builder.build());
						}
					}
				});

		BitmapManager.getInstance().masterFetch(bitmapRequest);
	}

	public void useBigText(NotificationCompat.Builder builder, AirNotification airNotification) {
		NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle()
				.setBigContentTitle(airNotification.title)
				.bigText(airNotification.entity.description)
				.setSummaryText(airNotification.subtitle);

		builder.setStyle(style);

		mNotificationManager.notify(airNotification.action, 0, builder.build());
	}

	public void cancelNotification(String tag) {
		mNotificationManager.cancel(tag, 0);
	}

	public void storeNotification(final AirNotification notification, String jsonNotification) {

		ContentValues values = new ContentValues();
		values.put(NotificationTable.COLUMN_SENT_DATE, notification.sentDate.longValue());
		values.put(NotificationTable.COLUMN_OBJECT, jsonNotification);

		String where = NotificationTable.COLUMN_TARGET_ID
				+ "=? AND "
				+ NotificationTable.COLUMN_ACTION + "=?";

		int updateCount = Aircandi.applicationContext.getContentResolver().update(NotificationsContentProvider.CONTENT_URI
				, values
				, where
				, new String[] { notification.entity.id, notification.action });

		if (updateCount == 0) {
			values.put(NotificationTable.COLUMN_ACTION, notification.action);
			values.put(NotificationTable.COLUMN_TARGET_ID, notification.entity.id);
			Aircandi.applicationContext.getContentResolver().insert(NotificationsContentProvider.CONTENT_URI, values);
		}

		mNewCount++;
	}

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------	

	public void setNewCount(Integer newCount) {
		mNewCount = newCount;
	}

	public Device getDevice() {
		return mDevice;
	}

	public void setDevice(Device device) {
		mDevice = device;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------	
}
