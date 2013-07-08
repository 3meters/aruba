package com.aircandi.ui;

import java.io.InputStream;
import java.util.Properties;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Window;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.EntityManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ConnectedState;
import com.aircandi.components.NotificationManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ObjectType;
import com.aircandi.service.objects.Session;
import com.aircandi.service.objects.User;
import com.aircandi.ui.user.RegisterEdit;
import com.aircandi.ui.user.SignInEdit;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.amazonaws.auth.BasicAWSCredentials;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.android.gcm.GCMRegistrar;

public class SplashForm extends SherlockActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * Used by other activities to determine if they were launched normally or auto launched after a crash
		 */
		Aircandi.LAUNCHED_NORMALLY = true;

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		if (!Aircandi.applicationUpdateRequired) {
			if (Aircandi.firstStartApp) {
				initializeApp();
			}

			signinAuto();
		}

		if (!isFinishing()) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.splash_form);
			initialize();

			if (Aircandi.applicationUpdateRequired) {
				updateRequired();
			}
		}
	}

	private void initializeApp() {

		if (Build.PRODUCT.contains("sdk")) {
			Aircandi.usingEmulator = true;
		}

		/* Tickle the bitmap manager to get it initialized */
		BitmapManager.getInstance();

		/* AWS Credentials */
		startGetAWSCredentials();

		/* Google analytics tracking */
		GoogleAnalytics.getInstance(this).setDebug(false);
		EasyTracker.getInstance().setContext(getApplicationContext());

		/* Connectivity monitoring */
		NetworkManager.getInstance().setContext(getApplicationContext());
		NetworkManager.getInstance().initialize();

		/* Service notifications */
		GCMRegistrar.checkDevice(this); 	// Does device support GCM
		GCMRegistrar.checkManifest(this); 	// Is manifest setup correctly for GCM
		String registrationId = GCMRegistrar.getRegistrationId(Aircandi.applicationContext);
		if (registrationId != null) {
			NotificationManager.getInstance().unregisterDeviceWithAircandi(registrationId);
			GCMRegistrar.setRegisteredOnServer(Aircandi.applicationContext, false);
		}
		NotificationManager.getInstance().registerDeviceWithGCM();

		/* Proxibase sdk components */
		Aircandi.getInstance().setUsingEmulator(Aircandi.usingEmulator);

		/* Cache categories - we delay until after the initial rush for data */
		Aircandi.mainThreadHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				loadCategories();
			}
		}, Constants.INTERVAL_CATEGORIES_DOWNLOAD);

		Aircandi.firstStartApp = false;
	}

	private void initialize() {
		((ImageView) findViewById(R.id.image_background)).setBackgroundResource(R.drawable.img_splash_v);
	}

	private void signinAuto() {

		final String jsonUser = Aircandi.settings.getString(Constants.SETTING_USER, null);
		final String jsonSession = Aircandi.settings.getString(Constants.SETTING_USER_SESSION, null);

		if (jsonUser != null && jsonSession != null) {
			Logger.i(this, "Auto sign in...");
			final User user = (User) HttpService.jsonToObject(jsonUser, ObjectType.User);
			if (user != null) {
				user.session = (Session) HttpService.jsonToObject(jsonSession, ObjectType.Session);
				if (user.session != null) {
					Aircandi.getInstance().setUser(user);
					Tracker.startNewSession(Aircandi.getInstance().getUser());
					Tracker.sendEvent("action", "signin_auto", null, 0, Aircandi.getInstance().getUser());
					startMainApp();
				}
			}
		}
	}

	private void startMainApp() {
		final Intent intent = new Intent(this, RadarForm.class);
		startActivity(intent);
		finish();
	}

	private void updateRequired() {
		showUpdateDialog();
	}

	// --------------------------------------------------------------------------------------------
	// Dialogs
	// --------------------------------------------------------------------------------------------

	private void showUpdateDialog() {

		final AlertDialog updateDialog = AircandiCommon.showAlertDialog(null
				, getString(R.string.dialog_update_title)
				, getString(R.string.dialog_update_message)
				, null
				, this
				, R.string.dialog_update_ok
				, R.string.dialog_update_cancel
				, null
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							try {
								Tracker.sendEvent("ui_action", "update_aircandi", "com.aircandi.beta", 0, Aircandi.getInstance().getUser());
								Logger.d(this, "Update: navigating to market install/update page");
								final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(Constants.APP_MARKET_URI));
								intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
								startActivity(intent);
							}
							catch (Exception e) {
								/*
								 * In case the market app isn't installed on the phone
								 */
								Logger.d(this, "Install: navigating to play website install page");
								final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://play.google.com/store/apps/details?id="
										+ "com.aircandi.beta&referrer=utm_source%3Dcom.aircandi"));
								intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
								startActivityForResult(intent, Constants.ACTIVITY_MARKET);
							}
							dialog.dismiss();
							AnimUtils.doOverridePendingTransition(SplashForm.this, TransitionType.PageToForm);
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

	private void loadCategories() {
		if (NetworkManager.getInstance().getConnectedState() == ConnectedState.Normal) {
			new AsyncTask() {

				@Override
				protected void onPostExecute(Object result) {
					// TODO Auto-generated method stub
					super.onPostExecute(result);
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("LoadCategories");
					ModelResult result = new ModelResult();
					if (EntityManager.getInstance().getCategories().size() == 0) {
						result = EntityManager.getInstance().loadCategories();
					}
					return result;
				}
			}.execute();
		}
		else {
			/* Schedule the next attempt */
			Aircandi.mainThreadHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					loadCategories();
				}
			}, Constants.INTERVAL_CATEGORIES_DOWNLOAD);

		}
	}

	private void startGetAWSCredentials() {
		final Thread t = new Thread() {

			@Override
			public void run() {
				Thread.currentThread().setName("GetAwsCredentials");
				try {
					final Properties properties = new Properties();
					final InputStream inputStream = getClass().getResourceAsStream("/com/aircandi/aws.properties");
					properties.load(inputStream);

					final String accessKeyId = properties.getProperty("accessKey");
					final String secretKey = properties.getProperty("secretKey");

					if ((accessKeyId == null) || (accessKeyId.equals(""))
							|| (accessKeyId.equals("CHANGEME"))
							|| (secretKey == null)
							|| (secretKey.equals(""))
							|| (secretKey.equals("CHANGEME"))) {
						Logger.e(SplashForm.this, "Aws Credentials not configured correctly.");
					}
					else {
						Aircandi.awsCredentials = new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"));
					}
				}
				catch (Exception exception) {
					Logger.e(SplashForm.this, exception.getMessage(), exception);
				}
			}
		};
		t.start();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onSigninButtonClick(View view) {
		if (Aircandi.applicationUpdateRequired) {
			updateRequired();
			return;
		}

		final IntentBuilder intentBuilder = new IntentBuilder(this, SignInEdit.class);
		final Intent intent = intentBuilder.create();
		startActivityForResult(intent, Constants.ACTIVITY_SIGNIN);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@SuppressWarnings("ucd")
	public void onSignupButtonClick(View view) {
		if (Aircandi.applicationUpdateRequired) {
			updateRequired();
			return;
		}

		final IntentBuilder intentBuilder = new IntentBuilder(this, RegisterEdit.class);
		final Intent intent = intentBuilder.create();
		startActivityForResult(intent, Constants.ACTIVITY_SIGNIN);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == Constants.ACTIVITY_SIGNIN) {
			if (resultCode == Constants.RESULT_USER_SIGNED_IN) {
				startMainApp();
			}
		}
	}
}