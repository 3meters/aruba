package com.aircandi.ui;

import java.io.InputStream;
import java.util.Properties;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Window;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NotificationManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ObjectType;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.Session;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;
import com.amazonaws.auth.BasicAWSCredentials;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.android.gcm.GCMRegistrar;

public class SplashForm extends SherlockActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * Used by other activities to determine if they were launched normally or AUTO launched after a crash
		 */
		Aircandi.LAUNCHED_NORMALLY = true;

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.splash_form);
		initialize();
	}

	private void initialize() {
		if (!Aircandi.applicationUpdateRequired) {
			if (Aircandi.firstStartApp) {
				warmup();
			}
			else {
				signinAuto();
			}
		}
		else {
			updateRequired();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("unused")
	private void attribution() {
		/* Should be the first method called in initialize */
		UI.setVisibility(findViewById(R.id.image_foursquare), View.GONE);
		UI.setVisibility(findViewById(R.id.image_google), View.GONE);
		String provider = Aircandi.settings.getString(
				Constants.PREF_PLACE_PROVIDER,
				Constants.PREF_PLACE_PROVIDER_DEFAULT);
		if (provider.equals(Constants.TYPE_PROVIDER_FOURSQUARE)) {
			UI.setVisibility(findViewById(R.id.image_foursquare), View.VISIBLE);
		}
		else if (provider.equals(Constants.TYPE_PROVIDER_GOOGLE)) {
			UI.setVisibility(findViewById(R.id.image_google), View.VISIBLE);
		}
	}

	private void warmup() {
		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("InitializeApp");
				ModelResult result = new ModelResult();

				if (Build.PRODUCT.contains("sdk")) {
					Aircandi.usingEmulator = true;
				}

				/* Tickle the bitmap manager to get it initialized */
				BitmapManager.getInstance();

				/* AWS Credentials */
				startGetAWSCredentials();

				/* Google analytics tracking */
				GoogleAnalytics.getInstance(SplashForm.this).setDebug(false);
				EasyTracker.getInstance().setContext(getApplicationContext());

				/* Connectivity MONITORING */
				NetworkManager.getInstance().setContext(getApplicationContext());
				NetworkManager.getInstance().initialize();

				/*
				 * Fire off a check to make sure the session is valid. This will also
				 * be the first opportunity to check our NETWORK connection. Also, the
				 * users session window will be extended assuming the session is valid.
				 */
				result = EntityManager.getInstance().checkSession();

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					/* Cache categories - we delay until after the initial rush for data */
					if (EntityManager.getInstance().getCategories().size() == 0) {
						result = EntityManager.getInstance().loadCategories();
					}
				}

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					/* service notifications */
					GCMRegistrar.checkDevice(SplashForm.this); 	// Does device support GCM
					GCMRegistrar.checkManifest(SplashForm.this); 	// Is manifest setup correctly for GCM
					String registrationId = GCMRegistrar.getRegistrationId(Aircandi.applicationContext);
					if (registrationId != null) {
						NotificationManager.getInstance().unregisterDeviceWithAircandi(registrationId);
						GCMRegistrar.setRegisteredOnServer(Aircandi.applicationContext, false);
					}

					NotificationManager.getInstance().registerDeviceWithGCM();

					/* Proxibase sdk components */
					Aircandi.getInstance().setUsingEmulator(Aircandi.usingEmulator);

					/*
					 * get setup for location snapshots. Initialize will populate location
					 * with the best of any cached location fixes. A single update will
					 * be launched if the best cached location fix doesn't meet our freshness
					 * and accuracy requirements.
					 */
					LocationManager.getInstance().initialize(getApplicationContext());
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				ModelResult result = (ModelResult) modelResult;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Aircandi.firstStartApp = false;
					signinAuto();
				}
				else {
					if (Routing.isNetworkError(result.serviceResponse)) {
						Routing.serviceError(SplashForm.this, result.serviceResponse);
						showButtons(Buttons.RETRY);
					}
					else {
						Routing.serviceError(SplashForm.this, result.serviceResponse);
						showButtons(Buttons.ACCOUNT);
					}
				}
			}

		}.execute();
	}

	private void signinAuto() {

		final String jsonUser = Aircandi.settings.getString(Constants.SETTING_USER, null);
		final String jsonSession = Aircandi.settings.getString(Constants.SETTING_USER_SESSION, null);

		if (jsonUser != null && jsonSession != null) {
			Logger.i(this, "Auto sign in...");
			final User user = (User) HttpService.jsonToObject(jsonUser, ObjectType.USER);
			if (user != null) {
				user.session = (Session) HttpService.jsonToObject(jsonSession, ObjectType.SESSION);
				if (user.session != null) {
					Aircandi.getInstance().setUser(user);
					Tracker.startNewSession(Aircandi.getInstance().getUser());
					Tracker.sendEvent("action", "signin_auto", null, 0, Aircandi.getInstance().getUser());
					Aircandi.mainThreadHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							startMainApp();
						}
					}, 500);
					return;
				}
			}
		}
		showButtons(Buttons.ACCOUNT);
	}

	private void showButtons(Buttons buttons) {
		if (buttons == Buttons.NONE) {
			findViewById(R.id.button_retry_holder).setVisibility(View.GONE);
			findViewById(R.id.button_holder).setVisibility(View.GONE);
		}
		else if (buttons == Buttons.RETRY) {
			findViewById(R.id.button_retry_holder).setVisibility(View.VISIBLE);
			findViewById(R.id.button_holder).setVisibility(View.GONE);
		}
		else if (buttons == Buttons.ACCOUNT) {
			findViewById(R.id.button_retry_holder).setVisibility(View.GONE);
			findViewById(R.id.button_holder).setVisibility(View.VISIBLE);
		}
	}

	private void startMainApp() {

		/* Always reset the entity cache */
		EntityManager.getEntityCache().clear();

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				ModelResult result = new ModelResult();

				if (Aircandi.getInstance().getUser() != null) {
					LinkOptions options = LinkOptions.getDefault(LinkProfile.LINKS_FOR_USER);
					result = EntityManager.getInstance().getEntity(Aircandi.getInstance().getUser().id, true, options);
				}

				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					final Intent intent = new Intent(SplashForm.this, AircandiForm.class);
					startActivity(intent);
					finish();
					Animate.doOverridePendingTransition(SplashForm.this, TransitionType.PAGE_TO_HELP);
				}
				else {
					if (Routing.isNetworkError(result.serviceResponse)) {
						Routing.serviceError(SplashForm.this, result.serviceResponse);
						showButtons(Buttons.RETRY);
					}
					else {
						Routing.serviceError(SplashForm.this, result.serviceResponse);
						showButtons(Buttons.ACCOUNT);
					}
				}
			}

		}.execute();

	}

	private void updateRequired() {
		Dialogs.update(this);
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
	// Dialogs
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onSigninButtonClick(View view) {
		if (Aircandi.applicationUpdateRequired) {
			updateRequired();
			return;
		}
		Routing.route(this, Route.SIGNIN);
	}

	@SuppressWarnings("ucd")
	public void onSignupButtonClick(View view) {
		if (Aircandi.applicationUpdateRequired) {
			updateRequired();
			return;
		}
		Routing.route(this, Route.REGISTER);
	}

	@SuppressWarnings("ucd")
	public void onRetryButtonClick(View view) {
		showButtons(Buttons.NONE);
		if (Aircandi.applicationUpdateRequired) {
			updateRequired();
			return;
		}
		else {
			if (Aircandi.firstStartApp) {
				warmup();
			}
			else {
				signinAuto();
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == Constants.ACTIVITY_SIGNIN) {
			if (resultCode == Constants.RESULT_USER_SIGNED_IN) {
				startMainApp();
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	private enum Buttons {
		ACCOUNT,
		RETRY,
		NONE
	}
}