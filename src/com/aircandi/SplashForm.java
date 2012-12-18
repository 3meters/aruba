package com.aircandi;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Window;
import com.aircandi.components.CommandType;
import com.aircandi.components.FontManager;
import com.aircandi.components.ImageCache;
import com.aircandi.components.ImageManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.Tracker;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.Session;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

public class SplashForm extends SherlockActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * Used by other activities to determine if they were launched normally or auto launched after a crash
		 */
		Aircandi.getInstance().setLaunchedNormally(true);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		if (Aircandi.firstStartApp) {
			initializeApp();
		}

		signinAuto();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.splash);

		initialize();
	}

	private void initializeApp() {

		if (Build.PRODUCT.contains("sdk")) {
			Aircandi.usingEmulator = true;
		}

		/* Connectivity monitoring */
		NetworkManager.getInstance().setContext(getApplicationContext());
		NetworkManager.getInstance().initialize();

		/* Proxibase sdk components */
		ProxiExplorer.getInstance().setContext(getApplicationContext());
		ProxiExplorer.getInstance().setUsingEmulator(Aircandi.usingEmulator);
		ProxiExplorer.getInstance().initialize();

		/* Image cache */
		ImageManager.getInstance().setImageCache(new ImageCache(getApplicationContext(), CandiConstants.CACHE_PATH, 100, 16));
		ImageManager.getInstance().setFileCacheOnly(true);
		ImageManager.getInstance().setActivity(this);

		Aircandi.firstStartApp = false;
	}

	private void initialize() {
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.splash_title));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.splash_message));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_signup));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_signin));
	}

	public void signinAuto() {

		String jsonUser = Aircandi.settings.getString(Preferences.PREF_USER, null);
		String jsonSession = Aircandi.settings.getString(Preferences.PREF_USER_SESSION, null);

		if (jsonUser != null && jsonSession != null) {
			Logger.i(this, "Auto sign in...");
			User user = (User) ProxibaseService.convertJsonToObjectInternalSmart(jsonUser, ServiceDataType.User);
			user.session = (Session) ProxibaseService.convertJsonToObjectInternalSmart(jsonSession, ServiceDataType.Session);
			Tracker.startNewSession();
			Tracker.trackEvent("User", "AutoSignin", null, 0);
			Aircandi.getInstance().setUser(user);
			startMainApp();
		}
	}

	public void startMainApp() {
		Intent intent = new Intent(this, CandiRadar.class);
		startActivity(intent);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onSigninButtonClick(View view) {
		IntentBuilder intentBuilder = new IntentBuilder(this, SignInForm.class);
		Intent intent = intentBuilder.create();
		startActivityForResult(intent, CandiConstants.ACTIVITY_SIGNIN);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	public void onSignupButtonClick(View view) {
		IntentBuilder intentBuilder = new IntentBuilder(this, RegisterForm.class);
		intentBuilder.setCommandType(CommandType.New);
		Intent intent = intentBuilder.create();
		startActivityForResult(intent, CandiConstants.ACTIVITY_SIGNIN);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == CandiConstants.ACTIVITY_SIGNIN) {
			if (resultCode == CandiConstants.RESULT_USER_SIGNED_IN) {
				startMainApp();
			}
		}
	}
}