package com.aircandi.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpStatus;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.service.objects.Device;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.ServiceEntryBase.UpdateScope;
import com.aircandi.ui.CandiRadar;
import com.google.android.gcm.GCMRegistrar;

@SuppressWarnings("ucd")
public class NotificationManager {

	private Device									mDevice;
	private final List<AirNotification>				mNotifications	= Collections.synchronizedList(new ArrayList<AirNotification>());

	public static android.app.NotificationManager	mNotificationManager;

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
			GCMRegistrar.register(Aircandi.applicationContext, CandiConstants.SENDER_ID);
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
					device.clientVersionName = Aircandi.getVersionName(Aircandi.applicationContext, CandiRadar.class);
					device.clientVersionCode = Aircandi.getVersionCode(Aircandi.applicationContext, CandiRadar.class);

					device.updateScope = UpdateScope.Property;
					ModelResult result = ProxiManager.getInstance().getEntityModel().registerDevice(device);

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						Logger.i(this, "GCM: Device registered with Aircandi notification service");
						final String jsonResponse = (String) result.serviceResponse.data;
						final ServiceData serviceData = HttpService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.Device);
						mDevice = (Device) serviceData.data;
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
					ModelResult result = ProxiManager.getInstance().getEntityModel().unregisterDevice(registrationId);

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

	public void showNotification(final AirNotification airNotification, Context context) {
		/*
		 * Small icon displays on left unless a large icon is specified
		 * and then it moves to the right.
		 */
		if (airNotification.subtype != null) {
			if (airNotification.subtype.equals("nearby")) {
				if (!Aircandi.settings.getBoolean(CandiConstants.PREF_NOTIFICATIONS_NEARBY, CandiConstants.PREF_NOTIFICATIONS_NEARBY_DEFAULT)) {
					return;
				}
			}
			else if (airNotification.subtype.equals("owner")) {
				if (airNotification.type.equals("comment")) {
					if (!Aircandi.settings.getBoolean(CandiConstants.PREF_NOTIFICATIONS_COMMENTS, CandiConstants.PREF_NOTIFICATIONS_COMMENTS_DEFAULT)) {
						return;
					}
				}
				else if (airNotification.type.equals("entity_insert")) {
					if (!Aircandi.settings.getBoolean(CandiConstants.PREF_NOTIFICATIONS_CANDIGRAMS, CandiConstants.PREF_NOTIFICATIONS_CANDIGRAMS_DEFAULT)) {
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

		if (airNotification.message != null) {
			NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
			style.setBigContentTitle(airNotification.title);
			style.bigText(airNotification.message);
			style.setSummaryText(airNotification.subtitle);
			builder.setStyle(style);
		}

		airNotification.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		airNotification.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		final PendingIntent pendingIntent = PendingIntent.getActivity(Aircandi.applicationContext
				, 0
				, airNotification.intent
				, PendingIntent.FLAG_CANCEL_CURRENT);

		if (airNotification.user != null) {

			final BitmapRequest bitmapRequest = new BitmapRequest();
			bitmapRequest.setImageUri(airNotification.user.getUserPhotoUri());
			bitmapRequest.setImageRequestor(airNotification);
			bitmapRequest.setImageSize((int) Aircandi.applicationContext.getResources().getDimension(android.R.dimen.notification_large_icon_width));
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

	public Device getDevice() {
		return mDevice;
	}

	public void setDevice(Device device) {
		this.mDevice = device;
	}

	public List<AirNotification> getNotifications() {
		return mNotifications;
	}
}
