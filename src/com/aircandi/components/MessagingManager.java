package com.aircandi.components;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import com.aircandi.service.objects.Action.EventCategory;
import com.aircandi.service.objects.ActivityBase;
import com.aircandi.service.objects.ActivityBase.TriggerType;
import com.aircandi.service.objects.Install;
import com.aircandi.service.objects.ServiceMessage;
import com.aircandi.ui.AircandiForm;
import com.aircandi.utilities.Errors;
import com.google.android.gms.gcm.GoogleCloudMessaging;

@SuppressWarnings("ucd")
public class MessagingManager {

	public static NotificationManager	mNotificationManager;
	private GoogleCloudMessaging		mGcm;
	private Install						mInstall;

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

		result = EntityManager.getInstance().registerInstall(install);

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

	public void broadcastMessage(final ServiceMessage message) {
		BusProvider.getInstance().post(new MessageEvent(message));
	}

	public void notificationForMessage(final ServiceMessage message, Context context) {
		/*
		 * Small icon displays on left unless a large icon is specified
		 * and then it moves to the right.
		 */
		if (message.trigger != null) {
			if (message.getTriggerCategory().equals(TriggerType.NEARBY)) {
				if (!Aircandi.settings.getBoolean(Constants.PREF_NOTIFICATIONS_NEARBY, Constants.PREF_NOTIFICATIONS_NEARBY_DEFAULT)) {
					return;
				}
			}
			else if (message.getTriggerCategory().equals(TriggerType.OWN)) {
				if (!Aircandi.settings.getBoolean(Constants.PREF_NOTIFICATIONS_OWN, Constants.PREF_NOTIFICATIONS_OWN_DEFAULT)) {
					return;
				}
			}
			else if (message.getTriggerCategory().equals(TriggerType.WATCH)) {
				if (!Aircandi.settings.getBoolean(Constants.PREF_NOTIFICATIONS_WATCH, Constants.PREF_NOTIFICATIONS_WATCH_DEFAULT)) {
					return;
				}
			}

			if (message.action.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
				if (!Aircandi.settings.getBoolean(Constants.PREF_NOTIFICATIONS_COMMENTS, Constants.PREF_NOTIFICATIONS_COMMENTS_DEFAULT)) {
					return;
				}
			}
			else if (message.action.entity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
				if (!Aircandi.settings.getBoolean(Constants.PREF_NOTIFICATIONS_PICTURES, Constants.PREF_NOTIFICATIONS_PICTURES_DEFAULT)) {
					return;
				}
			}
			else if (message.action.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
				if (!Aircandi.settings.getBoolean(Constants.PREF_NOTIFICATIONS_CANDIGRAMS, Constants.PREF_NOTIFICATIONS_CANDIGRAMS_DEFAULT)) {
					return;
				}
			}
		}

		message.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		message.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		final PendingIntent pendingIntent = PendingIntent.getActivity(Aircandi.applicationContext, 0
				, message.intent
				, PendingIntent.FLAG_CANCEL_CURRENT);

		/* Default base notification configuration */

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(Aircandi.applicationContext)
				.setContentTitle(message.title)
				.setContentText(message.subtitle)
				.setSmallIcon(R.drawable.ic_stat_notification)
				.setAutoCancel(true)
				.setOnlyAlertOnce(true)
				.setContentIntent(pendingIntent)
				.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS)
				.setWhen(System.currentTimeMillis());

		String byImageUri = message.photoBy.getUri();

		/* Large icon */

		if (byImageUri != null) {
			final BitmapRequest bitmapRequest = new BitmapRequest()
					.setBitmapUri(byImageUri)
					.setBitmapRequestor(message)
					.setBitmapSize((int) Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.notification_large_icon_width))
					.setRequestListener(new RequestListener() {

						@Override
						public void onComplete(Object response) {

							final ServiceResponse serviceResponse = (ServiceResponse) response;
							if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

								final BitmapResponse bitmapResponse = (BitmapResponse) serviceResponse.data;
								builder.setLargeIcon(bitmapResponse.bitmap);

								/* Enhance or go with default */

								if (message.action.entity != null && message.action.getEventCategory().equals(EventCategory.INSERT)) {
									if ((message.action.entity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)
											|| message.action.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM))
											&& message.action.entity.getPhoto().getUri() != null) {
										useBigPicture(builder, message);
									}
									else if (message.action.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
										useBigText(builder, message);
									}
								}
								else {
									String tag = getTag(message);
									mNotificationManager.notify(tag, 0, builder.build());
								}
							}
						}
					});

			BitmapManager.getInstance().masterFetch(bitmapRequest);
		}
		else {
			mNotificationManager.notify(getTag(message), 0, builder.build());
		}
	}

	public void useBigPicture(final NotificationCompat.Builder builder, final ServiceMessage message) {

		String imageUri = message.action.entity.getPhoto().getUri();
		final BitmapRequest bitmapRequest = new BitmapRequest()
				.setBitmapUri(imageUri)
				.setBitmapRequestor(message)
				.setBitmapSize((int) Aircandi.applicationContext.getResources().getDimensionPixelSize(R.dimen.notification_big_picture_width))
				.setRequestListener(new RequestListener() {

					@Override
					public void onComplete(Object response) {

						final ServiceResponse serviceResponse = (ServiceResponse) response;
						if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

							final BitmapResponse bitmapResponse = (BitmapResponse) serviceResponse.data;
							NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle()
									.bigPicture(bitmapResponse.bitmap)
									.setBigContentTitle(message.title)
									.setSummaryText(message.subtitle);

							builder.setStyle(style);
							String tag = getTag(message);
							mNotificationManager.notify(tag, 0, builder.build());
						}
					}
				});

		BitmapManager.getInstance().masterFetch(bitmapRequest);
	}

	public void useBigText(NotificationCompat.Builder builder, ServiceMessage message) {
		NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle()
				.setBigContentTitle(message.title)
				.bigText(message.action.entity.description)
				.setSummaryText(message.subtitle);

		builder.setStyle(style);

		mNotificationManager.notify(getTag(message), 0, builder.build());
	}

	public void cancelNotification(String tag) {
		mNotificationManager.cancel(tag, 0);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public String getTag(ActivityBase activity) {
		if (activity.action.getEventCategory().equals(EventCategory.EXPAND)) {
			return Tag.UPDATE;
		}
		else if (activity.action.getEventCategory().equals(EventCategory.MOVE)) {
			return Tag.UPDATE;
		}
		else if (activity.action.getEventCategory().equals(EventCategory.INSERT)) {
			return Tag.INSERT;
		}
		else if (activity.action.getEventCategory().equals(EventCategory.REFRESH)) {
			return Tag.REFRESH;
		}
		return Tag.UPDATE;
	}

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------	

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
		public static String	REFRESH	= "refresh";
	}
}
