package com.threemeters.aircandi.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.facebook.android.DialogError;
import com.facebook.android.FacebookError;
import com.facebook.android.Facebook.DialogListener;

public class FacebookWallPost extends Activity
{
	private String	wallMessage	= "";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.facebook_login);
		wallMessage = this.getIntent().getExtras().getString("facebookMessage");
		if (!FacebookService.facebookRunner.isSessionValid())
		{
			Intent intent = new Intent(this, AircandiLogin.class);
			startActivityForResult(intent, 0);
		}
		else
		{
			postToWall(getApplicationContext(), wallMessage);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
			postToWall(getApplicationContext(), wallMessage);
	}

	public void postToWall(Context context, String message)
	{
		// Bundle parameters = new Bundle();
		// parameters.putString("message", message);
		// FacebookService.facebook.dialog(context, "stream.publish", parameters, new WallPostDialogListener());
		finish();
	}

	class WallPostDialogListener implements DialogListener
	{
		public void onComplete(Bundle values)
		{
			final String postId = values.getString("post_id");
			if (postId != null)
			{
				showToast("Message posted to your facebook wall!");
			}
			else
			{
				showToast("Wall post cancelled!");
			}
		}

		public void onFacebookError(FacebookError e)
		{
			showToast("Failed to post to wall!");
			e.printStackTrace();
		}

		public void onError(DialogError e)
		{
			showToast("Failed to post to wall!");
			e.printStackTrace();
		}

		public void onCancel()
		{
			showToast("Wall post cancelled!");
		}
	}

	private void showToast(String message)
	{
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}
}
