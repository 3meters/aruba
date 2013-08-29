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

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.events.MessageEvent;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ObjectType;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.service.objects.Device;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.AircandiForm;
import com.google.android.gcm.GCMRegistrar;

@SuppressWarnings("ucd")
public class NotificationManager {

	private Device									mDevice;

	public static android.app.NotificationManager	mNotificationManager;
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

					ModelResult result = EntityManager.getInstance().registerDevice(device);

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						Logger.i(this, "GCM: Device registered with Aircandi notification service");
						final String jsonResponse = (String) result.serviceResponse.data;
						mDevice = (Device) HttpService.jsonToObject(jsonResponse, ObjectType.Device);
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

				/* Service notifications */
				if (registrationId != null && registrationId.length() > 0) {

					Logger.i(this, "GCM: Unregistering device with Aircandi notification service");
					ModelResult result = EntityManager.getInstance().unregisterDevice(registrationId);

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						Logger.i(this, "GCM: Device unregistered with Aircandi notification service");
						GCMRegistrar.setRegisteredOnServer(Aircandi.applicationContext, false);
					}
					else {
						if (result.serviceResponse.exception != null && result.serviceResponse.exception.getStatusCode() != null) {
							if (result.serviceResponse.exception.getStatusCode() == (float) HttpStatus.SC_NOT_FOUND) {
								Logger.i(this, "GCM: Device already unregistered with Aircandi notification service");
								GCMRegistrar.setRegisteredOnServer(Aircandi.applicationContext, false);
							}
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

	public void broadcastNotification(final AirNotification notification) {
		BusProvider.getInstance().post(new MessageEvent(notification));
	}

	public void decorateNotification(AirNotification notification) {
		/*
		 * Title and subtitle properties are added base on the context
		 * of the notification and the type of entity.
		 */
		if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)
				&& notification.entity.type.equals(Constants.TYPE_APP_TOUR)
				&& notification.action.equals("move")) {
			notification.title = "A " + Constants.SCHEMA_ENTITY_CANDIGRAM.toString();
			if (notification.entity.name != null) {
				notification.title += "called " + notification.entity.name;
			}
			if (notification.type.equals("nearby")) {
				notification.subtitle = "Has dropped in near you";
			}
			else if (notification.type.equals("watch")) {
				notification.subtitle = "Has dropped in";
			}
			
			if (notification.toEntity != null && notification.toEntity.name != null) {
				notification.subtitle += " at \"" + notification.toEntity.name + "\"";
			}
			return;
		}
		
		notification.title = notification.user.name;

		String category = null;
		if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			Place place = (Place) notification.entity;
			if (place.category != null && place.category.name != null) {
				category = place.category.name;
			}
		}
		if (category == null) {
			category = notification.entity.schema;
		}

		if (notification.type.equals("nearby")) {
			if (notification.action.equals("insert")) {
				notification.subtitle = "Added a new " + category + " near you";
			}
			else if (notification.action.equals("move")) {
				notification.subtitle = "Kicked a " + category + " near you";
			}
		}
		else if (notification.type.equals("watch")) {
			if (notification.action.equals("insert")) {
				notification.subtitle = "Added a new " + category;
			}
			else if (notification.action.equals("move")) {
				notification.subtitle = "Kicked a " + category;
			}
		}

		if (notification.entity.name != null) {
			notification.subtitle += " called \"" + notification.entity.name + "\"";
		}
		if (notification.toEntity != null && notification.toEntity.name != null) {
			notification.subtitle += " to \"" + notification.toEntity.name + "\"";
		}
	}

	public void showNotification(final AirNotification airNotification, Context context) {
		/*
		 * Small icon displays on left unless a large icon is specified
		 * and then it moves to the right.
		 */
		if (airNotification.type != null) {
			if (airNotification.type.equals(Constants.TYPE_NOTIFICATION_NEARBY)) {
				if (!Aircandi.settings.getBoolean(Constants.PREF_NOTIFICATIONS_NEARBY, Constants.PREF_NOTIFICATIONS_NEARBY_DEFAULT)) {
					return;
				}
			}
			else if (airNotification.type.equals(Constants.TYPE_NOTIFICATION_WATCH)) {
				if (airNotification.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
					if (!Aircandi.settings.getBoolean(Constants.PREF_NOTIFICATIONS_COMMENTS, Constants.PREF_NOTIFICATIONS_COMMENTS_DEFAULT)) {
						return;
					}
				}
				else {
					if (!Aircandi.settings.getBoolean(Constants.PREF_NOTIFICATIONS_PICTURES, Constants.PREF_NOTIFICATIONS_PICTURES_DEFAULT)) {
						return;
					}
				}
			}
		}

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(Aircandi.applicationContext)
				.setContentTitle(airNotification.title)
				.setContentText(airNotification.subtitle)
				.setSmallIcon(R.drawable.ic_stat_notification)
				.setAutoCancel(true)
				.setOnlyAlertOnce(true)
				.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS)
				.setWhen(System.currentTimeMillis());

		if (airNotification.entity != null && airNotification.entity.description != null) {
			NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
			style.setBigContentTitle(airNotification.title);
			style.bigText(airNotification.entity.description);
			style.setSummaryText(airNotification.subtitle);
			builder.setStyle(style);
		}

		airNotification.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		airNotification.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		final PendingIntent pendingIntent = PendingIntent.getActivity(Aircandi.applicationContext
				, 0
				, airNotification.intent
				, PendingIntent.FLAG_CANCEL_CURRENT);
		
		String imageUri = null;
		if (airNotification.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)
				&& airNotification.entity.type.equals(Constants.TYPE_APP_TOUR)
				&& airNotification.action.equals("move")) {
			imageUri = airNotification.entity.getPhotoUri();
		}
		else if (airNotification.user != null) {
			imageUri = airNotification.user.getPhotoUri();
		}

		if (imageUri != null) {
			final BitmapRequest bitmapRequest = new BitmapRequest();
			bitmapRequest.setImageUri(imageUri);
			bitmapRequest.setImageRequestor(airNotification);
			bitmapRequest.setImageSize((int) Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.notification_large_icon_width));
			bitmapRequest.setRequestListener(new RequestListener() {

				@Override
				public void onComplete(Object response) {

					final ServiceResponse serviceResponse = (ServiceResponse) response;
					if (serviceResponse.responseCode == ResponseCode.Success) {

						final ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
						builder.setLargeIcon(imageResponse.bitmap);
						Notification notification = builder.build();
						notification.contentIntent = pendingIntent;
						mNotificationManager.notify(airNotification.type, 0, notification);
					}
				}
			});
			BitmapManager.getInstance().masterFetch(bitmapRequest);
		}
		else {
			Notification notification = builder.build();
			notification.contentIntent = pendingIntent;
			mNotificationManager.notify(airNotification.type, 0, notification);
		}

	}

	public void cancelNotification(NotificationType type) {
		mNotificationManager.cancel(type.name(), 0);
	}

	public void storeNotification(final AirNotification notification, String jsonNotification) {

		ContentValues values = new ContentValues();
		values.put(NotificationTable.COLUMN_SENT_DATE, notification.sentDate.longValue());
		values.put(NotificationTable.COLUMN_OBJECT, jsonNotification);
		Aircandi.applicationContext.getContentResolver().insert(NotificationsContentProvider.CONTENT_URI, values);
		mNewCount++;
	}

	public Device getDevice() {
		return mDevice;
	}

	public void setDevice(Device device) {
		this.mDevice = device;
	}

	public Integer getNewCount() {
		return mNewCount;
	}

	public void setNewCount(Integer newCount) {
		mNewCount = newCount;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------	

	public enum NotificationType {
		network,
		nearby,
		watch
	}

}
