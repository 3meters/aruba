package com.aircandi.ui;

import java.io.InputStream;
import java.util.Properties;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Window;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MessagingManager;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.service.objects.Session;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;
import com.amazonaws.auth.BasicAWSCredentials;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class SplashForm extends SherlockActivity {

	private static final int	PLAY_SERVICES_RESOLUTION_REQUEST	= 9000;

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
		/* Clear the current user */
		Aircandi.getInstance().setCurrentUser(null);

		/* Always reset the entity cache */
		EntityManager.getEntityCache().clear();

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

				/* Connectivity monitoring */
				NetworkManager.getInstance().setContext(getApplicationContext());
				NetworkManager.getInstance().initialize();
				Reporting.updateCrashKeys();

				/* Cache categories */
				if (EntityManager.getInstance().getCategories().size() == 0) {
					result = EntityManager.getInstance().loadCategories();
				}

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					/* service notifications */
					if (checkPlayServices()) {
						MessagingManager.getInstance().registerInstallWithGCM();
					}

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
					if (Errors.isNetworkError(result.serviceResponse)) {
						Errors.handleError(SplashForm.this, result.serviceResponse);
						showButtons(Buttons.RETRY);
					}
					else {
						Errors.handleError(SplashForm.this, result.serviceResponse);
						if (Aircandi.applicationUpdateRequired) {
							updateRequired();
							return;
						}
						showButtons(Buttons.ACCOUNT);
					}
				}
			}

		}.execute();
	}

	private void signinAuto() {
		/*
		 * Gets called on app create and after restart and ending with the back key.
		 */
		final String jsonUser = Aircandi.settings.getString(Constants.SETTING_USER, null);
		final String jsonSession = Aircandi.settings.getString(Constants.SETTING_USER_SESSION, null);

		if (jsonUser != null && jsonSession != null) {
			Logger.i(this, "Auto sign in...");
			final User user = (User) Json.jsonToObject(jsonUser, Json.ObjectType.USER);
			if (user != null) {
				user.session = (Session) Json.jsonToObject(jsonSession, Json.ObjectType.SESSION);
				if (user.session != null) {
					Aircandi.getInstance().setCurrentUser(user);
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

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				ModelResult result = EntityManager.getInstance().activateCurrentUser();
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					result = MessagingManager.getInstance().registerInstallWithAircandi();
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
					if (Errors.isNetworkError(result.serviceResponse)) {
						showButtons(Buttons.RETRY);
						Errors.handleError(SplashForm.this, result.serviceResponse);
					}
					else {
						Errors.handleError(SplashForm.this, result.serviceResponse);
						if (Aircandi.applicationUpdateRequired) {
							updateRequired();
							return;
						}
						Aircandi.getInstance().setCurrentUser(null);
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

	private boolean checkPlayServices() {
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (status != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
				GooglePlayServicesUtil.getErrorDialog(status, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
			}
			else {
				Logger.w(this, "This device is not supported by google play services");
				UI.showToastNotification("This device is not supported", Toast.LENGTH_LONG);
				finish();
			}
			return false;
		}
		return true;
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
	public void onStartButtonClick(View view) {
		if (Aircandi.applicationUpdateRequired) {
			updateRequired();
			return;
		}
		User anonymous = (User) EntityManager.getInstance().loadEntityFromResources(R.raw.user_entity, Json.ObjectType.ENTITY);
		Aircandi.getInstance().setCurrentUser(anonymous);
		startMainApp();
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
		super.onActivityResult(requestCode, resultCode, intent);
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onStart() {
		super.onStart();
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	private enum Buttons {
		ACCOUNT,
		RETRY,
		NONE
	}
}