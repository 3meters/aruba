package com.threemeters.aircandi.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.BaseRequestListener;
import com.facebook.android.FacebookRunner;
import com.threemeters.aircandi.utilities.DateUtils;
import com.threemeters.aircandi.utilities.Utilities;
import com.threemeters.sdk.android.core.BaseModifyListener;
import com.threemeters.sdk.android.core.BaseQueryListener;
import com.threemeters.sdk.android.core.Query;
import com.threemeters.sdk.android.core.RippleRunner;
import com.threemeters.sdk.android.core.RippleService;
import com.threemeters.sdk.android.core.Stream;
import com.threemeters.sdk.android.core.UserFb;
import com.threemeters.sdk.android.core.RippleService.GsonType;
import com.threemeters.sdk.android.widgets.ImageCache;

public class Dashboard extends AircandiActivity {

	protected ImageView		mUserPicture;
	protected TextView		mUserName;
	protected LinearLayout	mUserInfo;
	protected Bitmap		mUserBitmap	= null;
	protected ImageCache	mImageCache;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		setContentView(R.layout.dashboard);
		super.onCreate(savedInstanceState);

		// Make sure shared preferences have been initialized to defaults
		PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);

		// Make sure we are signed in with a valid token before we do anything else
		if (FacebookService.facebookRunner == null) {
			Utilities.Log(Aircandi.APP_NAME, "Dashboard",
					"Creating new facebook and facebook runner classes using application context");
			FacebookService.facebookRunner = new FacebookRunner(Dashboard.this, getApplicationContext());
			Utilities.Log(Aircandi.APP_NAME, "Dashboard",
					"Attempting to restore facebook credentials from shared preferences");
			FacebookService.facebookRunner.restoreCredentials(getApplicationContext(),
					FacebookService.facebookRunner.facebook);
		}

		// There is a chance that our credentials could get invalidated while the application
		// is running so we always check them and route back to the sign in if they need to be refreshed.
		if (!FacebookService.facebookRunner.facebook.isSessionValid()) {
			Utilities.Log(Aircandi.APP_NAME, "Dashboard",
					"Current facebook credentials from shared prefs are not valid so starting AircandiLogin activity");
			Intent intent = new Intent(getApplicationContext(), AircandiLogin.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			startActivity(intent);
			return;
		}
		Utilities.Log(Aircandi.APP_NAME, "Dashboard",
				"Current facebook credentials from shared prefs appear to be valid");

		// We'll use these later when we get called back
		mUserPicture = (ImageView) findViewById(R.id.User_Picture);
		mUserName = (TextView) findViewById(R.id.User_Name);
		mUserInfo = (LinearLayout) findViewById(R.id.User_Info);
		// ImageView userPlaceholder = (ImageView) findViewById(R.id.User_Placeholder);
		// userPlaceholder.setColorFilter(getResources().getColor(R.color.button_color_filter),
		// PorterDuff.Mode.MULTIPLY);

		// Image cache
		mImageCache = new ImageCache(getApplicationContext(), "aircandy", 100, 16);

		// Initialize the current user
		ensureCurrentUser();

		theming();
	}


	public void ensureCurrentUser() {

		// If we don't have a current user object, we create one.
		startProgress();
		if (getCurrentUser() == null)
			FacebookService.facebookRunner.request("me", new UserRequestListener());
		else
			showUserInfo();
	}


	public void theming() {

		Button btn = (Button) findViewById(R.id.Button_Radar);
		btn.getBackground().mutate().setColorFilter(getResources().getColor(R.color.button_color_filter),
				PorterDuff.Mode.MULTIPLY);
		btn = (Button) findViewById(R.id.Button_Friends);
		btn.getBackground().mutate().setColorFilter(getResources().getColor(R.color.button_color_filter),
				PorterDuff.Mode.MULTIPLY);
		btn = (Button) findViewById(R.id.Button_Eggs);
		btn.getBackground().mutate().setColorFilter(getResources().getColor(R.color.button_color_filter),
				PorterDuff.Mode.MULTIPLY);
	}


	// For this activity, refresh means rescan and reload point data from the service
	@Override
	public void onRefreshClick(View view) {

		startProgress();
		AircandiUI.showToastNotification(Dashboard.this, "Configuring for current user...", Toast.LENGTH_SHORT);
		ensureCurrentUser();
	}


	public void enableUI(Boolean state) {

		Button btn = (Button) findViewById(R.id.Button_Radar);
		btn.setEnabled(state);
		btn = (Button) findViewById(R.id.Button_Friends);
		btn.setEnabled(state);
		btn = (Button) findViewById(R.id.Button_Eggs);
		btn.setEnabled(state);
	}


	public class UserRequestListener extends BaseRequestListener {

		public void onComplete(final String response) {

			// Process the response here: executed in background thread
			setCurrentUser(RippleService.getGson(GsonType.Internal).fromJson(response, UserFb.class));

			// Update the user with the most current facebook token
			getCurrentUser().token = FacebookService.facebookRunner.facebook.getAccessToken();
			getCurrentUser().tokenDate = DateUtils.nowString();

			// Make sure they are in the Aircandi service
			RippleRunner rippleRunner = new RippleRunner();
			Query query = new Query("Users").filter("Id eq '" + getCurrentUser().id + "'");
			rippleRunner.select(query, UserFb.class, "", new UserQueryListener());
		}


		@Override
		public void onIOException(IOException e) {

			// TODO Auto-generated method stub
			super.onIOException(e);
			Dashboard.this.runOnUiThread(new Runnable() {

				public void run() {

					AircandiUI.showToastNotification(Dashboard.this, "Network error", Toast.LENGTH_SHORT);
					setCurrentUser(null);
					enableUI(false);
					stopProgress();
				}
			});
		}
	}

	public class UserQueryListener extends BaseQueryListener {

		public void onComplete(String response) {

			List<Object> users = RippleService.convertJsonToObjects(response, UserFb.class);

			// We need to insert if we don't have them yet
			RippleRunner rippleRunner = new RippleRunner();
			if (users == null || users.size() == 0)
				rippleRunner.insert(getCurrentUser(), "Users", "", new UserReadyListener());
			else
				rippleRunner.update(getCurrentUser(), getCurrentUser().getUriOdata(), new UserReadyListener());
		}


		@Override
		public void onIOException(IOException e) {

			// TODO Auto-generated method stub
			super.onIOException(e);
			Dashboard.this.runOnUiThread(new Runnable() {

				public void run() {

					AircandiUI.showToastNotification(Dashboard.this, "Network error", Toast.LENGTH_SHORT);
					setCurrentUser(null);
					enableUI(false);
					stopProgress();
				}
			});
		}
	}

	public class UserReadyListener extends BaseModifyListener {

		public void onComplete() {

			// Post the processed result back to the UI thread
			Dashboard.this.runOnUiThread(new Runnable() {

				public void run() {

					showUserInfo();
				}
			});
		}


		@Override
		public void onIOException(IOException e) {

			// TODO Auto-generated method stub
			super.onIOException(e);
			Dashboard.this.runOnUiThread(new Runnable() {

				public void run() {

					AircandiUI.showToastNotification(Dashboard.this, "Network error", Toast.LENGTH_SHORT);
					setCurrentUser(null);
					enableUI(false);
					stopProgress();
				}
			});
		}
	}


	public void showUserInfo() {

		// Get their picture
		String userId = getCurrentUser().id;
		String imageFormat = "large";
		mUserName.setText(getCurrentUser().name);
		Bitmap bitmap = mImageCache.get(getCurrentUser().id);
		if (bitmap != null) {
			Utilities.Log(Aircandi.APP_NAME, "Dashboard", "Cache hit for image '" + userId + "'");
			getCurrentUser().picture_bitmap = bitmap;
			showUserPicture();
			stopProgress();
		}
		else
			new GetFacebookImageTask().execute(userId, imageFormat); // Will set the picture when finished
		enableUI(true);
	}


	public void showUserPicture() {

		mUserPicture.setImageBitmap(getCurrentUser().picture_bitmap);
		mUserPicture.setBackgroundColor(0xffffffff);
		mUserPicture.setAdjustViewBounds(true);
		Animation animation = AnimationUtils.loadAnimation(Dashboard.this, R.anim.fade_in_normal);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		mUserPicture.startAnimation(animation);
		stopProgress();
	}


	class GetFacebookImageTask extends AsyncTask<String, Void, Bitmap> {

		@Override
		protected Bitmap doInBackground(String... params) {

			// We are on the background thread
			Utilities.Log(Aircandi.APP_NAME, "Dashboard", "Getting facebook image for " + params[0]);
			Bitmap bitmap = null;
			bitmap = FacebookService.getFacebookPicture(params[0], params[1]);
			if (bitmap != null) {
				bitmap = AircandiUI.cropToSquare(bitmap);
				mImageCache.put(params[0], bitmap);
			}
			return bitmap;
		}


		@Override
		protected void onPostExecute(Bitmap bitmap) {

			// We are on the UI thread
			super.onPostExecute(bitmap);
			if (bitmap != null) {
				getCurrentUser().picture_bitmap = bitmap;
				showUserPicture();
			}
		}
	}


	public void onSpotClick(View view) {

		// We always clear any current point when task flow start from the dashboard
		setCurrentEntity(null);
		((Aircandi) getApplicationContext()).currentEntityX = null;

		try {
			String target = (String) view.getTag();
			Class activityClass = Class.forName(this.getPackageName() + "." + target);
			Intent intent = new Intent(this, activityClass);

			if (activityClass == FriendsList.class) {
				Stream stream = new Stream();
				stream.showHeader = true;
				stream.showFooter = false;
				stream.headerTitle = "Friends";
				stream.headerIconResource = "none";
				stream.layoutTemplate = "list_standard";
				String jsonStream = RippleService.getGson(GsonType.Internal).toJson(stream);
				intent.putExtra("stream", jsonStream);
			}
//			if (activityClass == EggsMineList.class) {
//				Stream stream = new Stream();
//				stream.showHeader = true;
//				stream.showFooter = false;
//				stream.headerTitle = "Egg Collection By Sets";
//				stream.headerIconResource = "none";
//				stream.layoutTemplate = "list_standard";
//				String jsonStream = RippleService.getGson(GsonType.Internal).toJson(stream);
//				intent.putExtra("stream", jsonStream);
//			}
			if (activityClass == RewardsList.class)
				return;

			startActivity(intent);
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}