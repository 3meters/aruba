package com.aircandi.components;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.BitmapResponse;
import com.aircandi.events.MessageEvent;
import com.aircandi.service.GcmRegistrationIOException;
import com.aircandi.service.RequestListener;
import com.aircandi.service.ServiceResponse;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.service.objects.AirNotification.ActionType;
import com.aircandi.service.objects.AirNotification.NotificationType;
import com.aircandi.service.objects.Install;
import com.aircandi.ui.AircandiForm;
import com.aircandi.utilities.Errors;
import com.google.android.gms.gcm.GoogleCloudMessaging;

@SuppressWarnings("ucd")
public class MessagingManager {

	public static NotificationManager	mNotificationManager;
	private GoogleCloudMessaging		mGcm;
	private Install						mInstall;
	private Integer						mNewCount	= 0;

	private MessagingManager() {
		mNotificationManager = (NotificationManager) Aircandi.applicationContext.getSystemService(Service.NOTIFICATION_SERVICE);
	}

	private static class NotificationManagerHolder {
		public static final MessagingManager	instance	= new MessagingManager();
	}

	public static MessagingManager getInstance() {
		return NotificationManagerHolder.instance;
	}

	// --------------------------------------------------------------------------------------------
	// GCM
	// --------------------------------------------------------------------------------------------	

	public void registerInstallWithGCM() {
		/*
		 * Only called when aircandi application first runs.
		 * 
		 * Returns as not registered if no registration id or version has changed which clears any current registration
		 * id forcing us to fetch a new one. Registration id and associated app version code are stored in the gcm
		 * shared prefs.
		 */
		String registrationId = getRegistrationId(Aircandi.applicationContext);
		if (registrationId.isEmpty()) {
			try {
				if (mGcm == null) {
					mGcm = GoogleCloudMessaging.getInstance(Aircandi.applicationContext);
				}
				registrationId = mGcm.register(Constants.SENDER_ID);
				setRegistrationId(Aircandi.applicationContext, registrationId);
				Logger.i(this, "Registered aircandi install with GCM");
			}
			catch (IOException ex) {
				ServiceResponse serviceResponse = new ServiceResponse(ResponseCode.FAILED, null, new GcmRegistrationIOException());
				serviceResponse.errorResponse = Errors.getErrorResponse(Aircandi.applicationContext, serviceResponse);
				Errors.handleError(null, serviceResponse);
			}
		}
	}

	public ModelResult registerInstallWithAircandi() {

		ModelResult result = new ModelResult();
		Logger.i(this, "Registering install with Aircandi service");

		String registrationId = getRegistrationId(Aircandi.applicationContext);

		Install install = new Install(Aircandi.getInstance().getCurrentUser().id
				, registrationId
				, Aircandi.getInstallationId());

		install.clientVersionName = Aircandi.getVersionName(Aircandi.applicationContext, AircandiForm.class);
		install.clientVersionCode = Aircandi.getVersionCode(Aircandi.applicationContext, AircandiForm.class);

		result = EntityManager.getInstance().registerInstall(true, install);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Logger.i(this, "Install successfully registered with Aircandi service");
		}
		return result;
	}

	private void setRegistrationId(Context context, String registrationId) {
		final SharedPreferences prefs = getGcmPreferences(context);
		int versionCode = Aircandi.getVersionCode(Aircandi.applicationContext, MessagingManager.class);

		Logger.i(this, "Saving GCM registrationId for app version code " + String.valueOf(versionCode));
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(Constants.SETTING_GCM_REGISTRATION_ID, registrationId);
		editor.putInt(Constants.SETTING_GCM_VERSION_CODE, versionCode);
		editor.commit();
	}

	private String getRegistrationId(Context context) {
		final SharedPreferences prefs = getGcmPreferences(context);
		String registrationId = prefs.getString(Constants.SETTING_GCM_REGISTRATION_ID, "");
		if (registrationId.isEmpty()) {
			Logger.i(this, "GCM registration not found in settings.");
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersionCode = prefs.getInt(Constants.SETTING_GCM_VERSION_CODE, Integer.MIN_VALUE);
		int currentVersionCode = Aircandi.getVersionCode(Aircandi.applicationContext, MessagingManager.class);
		if (registeredVersionCode != currentVersionCode) {
			Logger.i(this, "GCM app version changed.");
			return "";
		}
		return registrationId;
	}

	private SharedPreferences getGcmPreferences(Context context) {
		return Aircandi.applicationContext.getSharedPreferences(MessagingManager.class.getSimpleName(), Context.MODE_PRIVATE);
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
									String tag = getTag(airNotification);
									mNotificationManager.notify(tag, 0, builder.build());
								}
							}
						}
					});

			BitmapManager.getInstance().masterFetch(bitmapRequest);
		}
		else {
			mNotificationManager.notify(getTag(airNotification), 0, builder.build());
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
							String tag = getTag(airNotification);
							mNotificationManager.notify(tag, 0, builder.build());
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

		mNotificationManager.notify(getTag(airNotification), 0, builder.build());
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
	// Methods
	// --------------------------------------------------------------------------------------------

	public String getTag(AirNotification notification) {
		if (notification.action.equals(ActionType.EXPAND)) {
			return Tag.UPDATE;
		}
		else if (notification.action.equals(ActionType.MOVE)) {
			return Tag.UPDATE;
		}
		else if (notification.action.equals(ActionType.INSERT)) {
			return Tag.INSERT;
		}
		return Tag.UPDATE;
	}

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------	

	public void setNewCount(Integer newCount) {
		mNewCount = newCount;
	}

	public Install getInstall() {
		return mInstall;
	}

	public void setInstall(Install device) {
		mInstall = device;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------	

	public static class Tag {
		public static String	INSERT	= "insert";
		public static String	UPDATE	= "update";
	}
}
