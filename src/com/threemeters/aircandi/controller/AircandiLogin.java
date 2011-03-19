package com.threemeters.aircandi.controller;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.DialogError;
import com.facebook.android.FacebookError;
import com.facebook.android.FacebookRunner;
import com.facebook.android.Facebook.DialogListener;
import com.threemeters.aircandi.utilities.Utilities;
import com.threemeters.sdk.android.core.RippleService;

public class AircandiLogin extends Activity
{
	protected final static int	ACTION_SIGNIN	= 101;
	private TextView			mSigninButton;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ripple_login);

		Utilities.Log(Aircandi.APP_NAME, "AircandiLogin", "Creating new facebook and facebook runner classes using AircandiLogin context");
		FacebookService.facebookRunner = new FacebookRunner(AircandiLogin.this, getApplicationContext());

		mSigninButton = (TextView) findViewById(R.id.Signin_Button);
		mSigninButton.getBackground().mutate().setColorFilter(getResources().getColor(R.color.button_color_filter), PorterDuff.Mode.MULTIPLY);		
		mSigninButton.setVisibility(View.VISIBLE);
	}

	public void onSignInClick(View view)
	{
		if (!RippleService.isConnectedToNetwork(getApplicationContext()))
		{
			AircandiUI.showToastNotification(AircandiLogin.this, R.string.alert_toast_networkmissing, Toast.LENGTH_SHORT);
			return;
		}
		Utilities.Log(Aircandi.APP_NAME, "AircandiLogin", "Calling facebook.authorize and waiting for a callback");
		FacebookService.facebookRunner.authorize(new LoginDialogListener());
	}

	private final class LoginDialogListener implements DialogListener
	{
		public void onComplete(Bundle values)
		{
			AircandiLogin.this.runOnUiThread(new Runnable() {
				public void run()
				{
					Utilities.Log(Aircandi.APP_NAME, "AircandiLogin", "Received callback from facebook.authorize");
					AircandiUI.showToastNotification(AircandiLogin.this, "Signed in!", Toast.LENGTH_SHORT);
					Intent intent = new Intent(getApplicationContext(), Dashboard.class);
					startActivity(intent);
				}
			});
		}

		public void onFacebookError(FacebookError error)
		{
			error.printStackTrace();
		}

		public void onError(DialogError error)
		{
			error.printStackTrace();
		}

		public void onCancel()
		{}
	}
}