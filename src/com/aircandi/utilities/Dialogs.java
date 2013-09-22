package com.aircandi.utilities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.Logger;
import com.aircandi.components.Tracker;
import com.aircandi.service.RequestListener;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutMeta;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Routing.Route;

public class Dialogs {

	private static AlertDialog	mWifiAlertDialog;

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public static AlertDialog alertDialog(Integer iconResource // $codepro.audit.disable largeNumberOfParameters
			, String titleText
			, String message
			, View customView
			, Context context
			, Integer okButtonId
			, Integer cancelButtonId
			, Integer neutralButtonId
			, OnClickListener listenerClick
			, OnCancelListener listenerCancel) {

		final AlertDialog.Builder builder = new AlertDialog.Builder(context);

		if (iconResource != null) {
			builder.setIcon(iconResource);
		}

		if (titleText != null) {
			View title = LayoutInflater.from(context).inflate(R.layout.temp_dialog_title, null);
			((TextView) title.findViewById(R.id.name)).setText(titleText);
			builder.setCustomTitle(title);
		}

		if (customView == null) {
			builder.setMessage(message);
		}
		else {
			builder.setView(customView);
		}

		if (okButtonId != null) {
			builder.setPositiveButton(okButtonId, listenerClick);
		}

		if (cancelButtonId != null) {
			builder.setNegativeButton(cancelButtonId, listenerClick);
		}

		if (neutralButtonId != null) {
			builder.setNeutralButton(neutralButtonId, listenerClick);
		}

		if (listenerCancel != null) {
			builder.setOnCancelListener(listenerCancel);
		}

		final AlertDialog alert = builder.create();
		alert.show();

		/* Hardcoded size for body text in the alert */
		final TextView textView = (TextView) alert.findViewById(android.R.id.message);
		if (textView != null) {
			textView.setTextSize(14);
		}

		/* Prevent dimming the background */
		if (Constants.SUPPORTS_ICE_CREAM_SANDWICH) {
			alert.getWindow().setDimAmount(Constants.DIALOGS_DIM_AMOUNT);
		}

		return alert;
	}

	public static void alertDialogSimple(final Activity activity, final String titleText, final String message) {
		if (!activity.isFinishing()) {
			activity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					alertDialog(R.drawable.ic_launcher
							, titleText
							, message
							, null
							, activity
							, android.R.string.ok
							, null
							, null
							, null
							, null);
				}
			});
		}
	}

	public static void wifi(final Activity activity, final Integer messageResId, final RequestListener listener) {
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (mWifiAlertDialog == null || !mWifiAlertDialog.isShowing()) {
					mWifiAlertDialog = alertDialog(R.drawable.ic_launcher
							, activity.getString(R.string.alert_wifi_title)
							, activity.getString(messageResId)
							, null
							, activity
							, R.string.alert_wifi_settings
							, R.string.alert_wifi_cancel
							, null
							, new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (which == Dialog.BUTTON_POSITIVE) {
										Logger.d(this, "Wifi check: navigating to wifi settings");
										Routing.route(activity, Route.SETTINGS_WIFI);
									}
									else if (which == Dialog.BUTTON_NEGATIVE) {
										Logger.d(this, "Wifi check: user declined");
										if (listener != null) {
											listener.onComplete();
										}
									}
								}
							}
							, new DialogInterface.OnCancelListener() {

								@Override
								public void onCancel(DialogInterface dialog) {
									/* Back button can trigger this */
									if (listener != null) {
										listener.onComplete();
									}
								}
							});
					mWifiAlertDialog.setCanceledOnTouchOutside(false);
				}
			}
		});
	}

	public static void update(final Activity activity) {

		final AlertDialog updateDialog = alertDialog(null
				, activity.getString(R.string.dialog_update_title)
				, activity.getString(R.string.dialog_update_message)
				, null
				, activity
				, R.string.dialog_update_ok
				, R.string.dialog_update_cancel
				, null
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							try {
								Tracker.sendEvent("ui_action", "update_aircandi", "com.aircandi", 0, Aircandi.getInstance().getUser());
								Logger.d(this, "Update: navigating to market install/update page");
								final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(Constants.APP_MARKET_URI));
								intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
								activity.startActivity(intent);
							}
							catch (Exception e) {
								/*
								 * In case the market app isn't installed on the phone
								 */
								Logger.d(this, "Install: navigating to play website install page");
								final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://play.google.com/store/apps/details?id="
										+ "com.aircandi&referrer=utm_source%3Dcom.aircandi"));
								intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
								activity.startActivityForResult(intent, Constants.ACTIVITY_MARKET);
							}
							dialog.dismiss();
							Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
						}
						else if (which == Dialog.BUTTON_NEGATIVE) {
							dialog.dismiss();
						}
					}
				}
				, null);
		updateDialog.setCanceledOnTouchOutside(false);
		updateDialog.show();
	}

	public static void install(final Activity activity, final Shortcut shortcut, final Entity entity) {

		final AlertDialog installDialog = Dialogs.alertDialog(null
				, activity.getString(R.string.dialog_install_title)
				, activity.getString(R.string.dialog_install_message)
				, null
				, activity
				, R.string.dialog_install_ok
				, R.string.dialog_install_cancel
				, null
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							try {
								Tracker.sendEvent("ui_action", "install_source", shortcut.getPackageName(), 0, Aircandi.getInstance().getUser());
								Logger.d(this, "Install: navigating to market install page");
								final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=" + shortcut.getPackageName()
										+ "&referrer=utm_source%3Dcom.aircandi"));
								intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
								activity.startActivity(intent);
							}
							catch (Exception e) {
								/*
								 * In case the market app isn't installed on the phone
								 */
								Logger.d(this, "Install: navigating to play website install page");
								final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://play.google.com/store/apps/details?id="
										+ shortcut.getPackageName() + "&referrer=utm_source%3Dcom.aircandi"));
								intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
								activity.startActivityForResult(intent, Constants.ACTIVITY_MARKET);
							}
							dialog.dismiss();
							Animate.doOverridePendingTransition(activity, TransitionType.PAGE_TO_FORM);
						}
						else if (which == Dialog.BUTTON_NEGATIVE) {
							final ShortcutMeta meta = Shortcut.shortcutMeta.get(shortcut.app);
							meta.installDeclined = true;
							Routing.route(activity, Route.SHORTCUT, entity, shortcut, null, null);
							dialog.dismiss();
						}
					}
				}
				, null);

		installDialog.setCanceledOnTouchOutside(false);
		installDialog.show();
	}

	public static void sendPassword(final Activity activity) {
		Dialogs.alertDialog(R.drawable.ic_launcher
				, activity.getResources().getString(R.string.alert_send_password_title)
				, activity.getResources().getString(R.string.alert_send_password_message)
				, null
				, activity, android.R.string.ok, null, null, null, null);
		Tracker.sendEvent("ui_action", "recover_password", null, 0, Aircandi.getInstance().getUser());
	}

	public static void locked(final Activity activity, Entity entity) {
		Integer stringResId = null;
		if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			stringResId = R.string.alert_entity_locked_place;
		}
		else if (entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
			stringResId = R.string.alert_entity_locked_candigram;
		}
		else if (entity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
			stringResId = R.string.alert_entity_locked_picture;
		}
		if (stringResId != null) {
			Dialogs.alertDialogSimple(activity, null, activity.getString(stringResId));
		}
	}
}