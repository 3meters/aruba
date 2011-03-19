package com.threemeters.aircandi.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.BaseRequestListener;
import com.threemeters.aircandi.utilities.DateUtils;
import com.threemeters.aircandi.utilities.Utilities;
import com.threemeters.sdk.android.core.BaseModifyListener;
import com.threemeters.sdk.android.core.BaseQueryListener;
import com.threemeters.sdk.android.core.BaseTagScanListener;
import com.threemeters.sdk.android.core.Entity;
import com.threemeters.sdk.android.core.Query;
import com.threemeters.sdk.android.core.RippleError;
import com.threemeters.sdk.android.core.RippleRunner;
import com.threemeters.sdk.android.core.RippleService;
import com.threemeters.sdk.android.core.Stream;
import com.threemeters.sdk.android.core.TagExplorer;
import com.threemeters.sdk.android.core.UserFb;
import com.threemeters.sdk.android.core.RippleService.GsonType;
import com.threemeters.sdk.android.widgets.ImageCache;
import com.threemeters.sdk.android.widgets.RippleAdapterView;
import com.threemeters.sdk.android.widgets.RippleView;
import com.threemeters.sdk.android.widgets.RippleAdapterView.OnItemClickListener;
import com.threemeters.sdk.android.widgets.RippleView.DisplayExtra;

public class Radar extends AircandiActivity {

	public static ArrayList<Entity> getEntitiesByTagId(List<Entity> entityList, String tagId) {

		ArrayList<Entity> entities = new ArrayList<Entity>();
		int entityListCount = entityList.size();
		for (int i = 0; i < entityListCount; i++)
			if (entityList.get(i).tagId.equals(tagId.toString()))
				entities.add(entityList.get(i));
		return entities;
	}


	public static List<Entity> getEntitiesSelected(List<Entity> entityList) {

		List<Entity> entityListSelected = new ArrayList<Entity>();
		for (int i = 0; i < entityList.size(); i++)
			if (entityList.get(i).isSelected)
				entityListSelected.add(entityList.get(i));
		return entityListSelected;
	}


	private Boolean				prefAutoscan_				= true;
	private int					prefAutoscanInterval_		= 5000;
	private int					prefTagLevelCutoff_			= -100;
	private boolean				prefTagsWithEntitiesOnly_	= true;
	private DisplayExtra		prefDisplayExtras_			= DisplayExtra.None;

	@SuppressWarnings("unused")
	private ArrayList<Entity>	entityList_					= new ArrayList<Entity>();
	private ArrayList<Entity>	entityListFiltered_			= new ArrayList<Entity>();

	private Boolean				isReadyToRun_				= false;
	private Handler				handler_					= new Handler();
	private CxMediaPlayer		mediaPlayerX_;
	private MediaPlayer			mediaPlayer_;

	private RippleView			rippleView_;
	protected ImageCache		imageCache_;
	private LinearLayout		container_;
	private RippleView			rippleViewFrame_;
	private FrameLayout			detailViewFrame_;
	private boolean				isDetailVisible_			= false;

	private boolean				userRefusedWifiEnable_		= false;
	private TagExplorer			tagExplorer_;
	private Runnable			tagScanTask_				= new Runnable() {

																public void run() {

																	scanForTags(false);
																}
															};


	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {

		// Start normal processing
		isReadyToRun_ = false;
		setContentView(R.layout.radar);
		super.onCreate(savedInstanceState);

		// Image cache
		imageCache_ = new ImageCache(getApplicationContext(), "aircandi", 100, 16);

		// If we don't have a current user object, we create one.
		if (getCurrentUser() == null) {
			if (FacebookService.facebookRunner == null) {
				Intent intent = new Intent(getApplicationContext(), Dashboard.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				startActivity(intent);
				return;
			}

			Utilities.Log(Aircandi.APP_NAME, "Radar", "Starting facebook graph request for user");
			FacebookService.facebookRunner.request("me", new UserRequestListener());
		}

		mediaPlayer_ = new MediaPlayer();
		mediaPlayerX_ = new CxMediaPlayer(this);

		// Ui Hookup
		container_ = (LinearLayout) findViewById(R.id.container);
		container_.setPersistentDrawingCache(ViewGroup.PERSISTENT_ANIMATION_CACHE);

		rippleViewFrame_ = (RippleView) findViewById(R.id.RippleView);
		detailViewFrame_ = (FrameLayout) findViewById(R.id.detailframe);

		// Ripple sdk components
		tagExplorer_ = new TagExplorer(this);

		if (!tagExplorer_.isWifiEnabled()) {
			this.wifiAskToEnable();
			if (userRefusedWifiEnable_)
				return;
		}

		rippleView_ = (RippleView) findViewById(R.id.RippleView);
		rippleView_.setScale(1.0f);
		rippleView_.setSpacing(-25);
		rippleView_.setDataSource(entityListFiltered_);
		
		rippleView_.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(RippleAdapterView parent, View v, int position, long id) {

				Entity entity = entityListFiltered_.get(position);
				setCurrentEntity(entity);
				isDetailVisible_ = true;

				new ShowDetailsTask().execute(detailViewFrame_);

				if (entity.pointResourceId != null && entity.pointResourceId != "")
					((ImageView) detailViewFrame_.findViewById(R.id.Image)).setImageBitmap(rippleView_.getImageCache().get(entity.pointResourceId));

				((TextView) detailViewFrame_.findViewById(R.id.Title)).setText(entity.title);
				((TextView) detailViewFrame_.findViewById(R.id.Subtitle)).setText(Html.fromHtml(entity.subtitle));
				((TextView) detailViewFrame_.findViewById(R.id.Description)).setText(Html.fromHtml(entity.description));

				applyRotation(position, 0, 90);
			}

		});

		isReadyToRun_ = true;
	}


	class ShowDetailsTask extends AsyncTask<FrameLayout, Void, TableLayout> {

		FrameLayout	frame;


		@Override
		protected TableLayout doInBackground(FrameLayout... params) {

			// We are on the background thread
			frame = params[0];
			boolean landscape = false;
			Display getOrient = getWindowManager().getDefaultDisplay();
			if (getOrient.getRotation() == Surface.ROTATION_90 || getOrient.getRotation() == Surface.ROTATION_270)
				landscape = true;
			TableLayout table = configureMenus(getCurrentEntity(), landscape, Radar.this);

			return table;
		}


		@Override
		protected void onPostExecute(TableLayout table) {

			// We are on the UI thread
			super.onPostExecute(table);
			FrameLayout frame = (FrameLayout) findViewById(R.id.frameMenu);
			frame.removeAllViews();
			frame.addView(table);
		}
	}


	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		super.onConfigurationChanged(newConfig);
		if (isDetailVisible_) {
			boolean landscape = false;
			Display getOrient = getWindowManager().getDefaultDisplay();
			if (getOrient.getRotation() == Surface.ROTATION_90 || getOrient.getRotation() == Surface.ROTATION_270)
				landscape = true;
			TableLayout table = configureMenus(getCurrentEntity(), landscape, Radar.this);
			FrameLayout frame = (FrameLayout) findViewById(R.id.frameMenu);
			frame.removeAllViews();
			frame.addView(table);
		}
	}


	// ==========================================================================
	// USER routines
	// ==========================================================================

	class UserRequestListener extends BaseRequestListener {

		public void onComplete(final String response) {

			// Process the response here: executed in background thread
			Utilities.Log(Aircandi.APP_NAME, "Radar", "Returning facebook graph request for user");
			setCurrentUser(RippleService.getGson(GsonType.Internal).fromJson(response, UserFb.class));

			// Once we have a current user, we launch our first wifi scan (which
			// leads to data requests). Other calls to start wifi scans are in Resume, manual refresh,
			// and autoscan (setup at the end of processing a previous wifi scan).
			tagExplorer_.scanForTags(new TagScanListener(), getCurrentUser());

			// Update the user with the most current facebook token
			getCurrentUser().token = FacebookService.facebookRunner.facebook.getAccessToken();
			getCurrentUser().tokenDate = DateUtils.nowString();

			// Make sure the current user is registered in the Aircandi service
			RippleRunner rippleRunner = new RippleRunner();
			Query query = new Query("Users").filter("Id eq '" + getCurrentUser().id + "'");
			Utilities.Log(Aircandi.APP_NAME, "Radar",
					"Starting ripple query to see if a facebook user '" + getCurrentUser().id
							+ "' already exists in ripple");
			rippleRunner.select(query, UserFb.class, new UserQueryListener());
		}
	}

	class UserQueryListener extends BaseQueryListener {

		public void onComplete(String response) {

			ArrayList<Object> users = RippleService.convertJsonToObjects(response, UserFb.class);

			// We need to insert if we don't have them yet or update if we do.
			// Update makes sure we have the latest access token stored with the service for later use.
			RippleRunner rippleRunner = new RippleRunner();
			if (users == null || users.size() == 0) {
				Utilities.Log(Aircandi.APP_NAME, "Radar", "Starting ripple insert for '" + getCurrentUser().id + "'");
				rippleRunner.insert(getCurrentUser(), "Users", new UserReadyListener());
			}
			else {
				Utilities.Log(Aircandi.APP_NAME, "Radar", "Starting ripple update for '" + getCurrentUser().id + "'");
				rippleRunner.update(getCurrentUser(), getCurrentUser().getUriOdata(), new UserReadyListener());
			}
		}
	}

	class UserReadyListener extends BaseModifyListener {

		public void onComplete() {

			// This is where to start any post processing needed on the UI thread.
			// For now, we just log the completion of the interaction with the Aircandi service.
			Utilities
					.Log(Aircandi.APP_NAME, "Radar", "User '" + getCurrentUser().id + "' has been inserted or updated");
		}
	}


	// ==========================================================================
	// EVENT routines
	// ==========================================================================

	public void onStreamButtonClick(View view) {

		Stream stream = (Stream) view.getTag();
		String fullyQualifiedClass = "com.threemeters.aircandi.controller." + stream.streamClass;
		String streamName = stream.streamName.toLowerCase();

		if (stream.streamClass.toLowerCase().equals("list")) {
			AircandiUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
			return;
		}

		try {
			if (streamName.toLowerCase().equals("audio")) {
				mediaPlayer_.setDataSource(Aircandi.URL_AIRCANDI_MEDIA + "3meters_audio/cezanne.mp3");
				mediaPlayer_.prepare();
				mediaPlayer_.start();
			}
			else if (streamName.toLowerCase().equals("video")) {
				// String movieUrl = Aircandi.URL_RIPPLEMEDIA +
				// "video/cezanne.mp3";
				// //String movieUrl =
				// "http://www.youtube.com/watch?v=q2mMSTlgWcU";
				// Intent tostart = new Intent(Intent.ACTION_VIEW);
				// tostart.setDataAndType(Uri.parse(movieUrl), "video/*");
				// startActivity(tostart);
			}
			else {
				Class clazz = Class.forName(fullyQualifiedClass, false, this.getClass().getClassLoader());

				Intent intent = new Intent(this, clazz);
				String jsonStream = RippleService.getGson(GsonType.Internal).toJson(stream);
				intent.putExtra("stream", jsonStream);

				if (streamName.toLowerCase().equals("friends"))
					intent.putExtra("FriendsFilter", "FriendsByPoint");
				else if (streamName.toLowerCase().equals("eggs")) {
					if (stream.itemCount == 0)
						intent.putExtra("TabId", "eggsdrop");
					else
						intent.putExtra("TabId", "egghunt");
				}

				startActivity(intent);
			}
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (IllegalStateException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}


	@Override
	public void onBackPressed() {

		if (!isDetailVisible_)
			super.onBackPressed();
		else {
			mediaPlayer_.stop();
			applyRotation(-1, 360, 270);
			setCurrentEntity(null);
			isDetailVisible_ = false;
		}
	}


	@Override
	public void onRefreshClick(View view) {

		// For this activity, refresh means rescan and reload point data from the service
		if (isReadyToRun_)
			scanForTags(true);
	}


	public void onDetailsClick(View v) {

		mediaPlayer_.stop();
		applyRotation(-1, 360, 270);
		setCurrentEntity(null);
		isDetailVisible_ = false;
	}


	// ==========================================================================
	// ENTITY routines
	// ==========================================================================

	private void scanForTags(Boolean showProgress) {

		if (showProgress)
			startProgress();

		Utilities.Log(Aircandi.APP_NAME, "Radar", "Calling Ripple.TagExplorer to scan for tags");
		tagExplorer_.scanForTags(new TagScanListener(), getCurrentUser());

		// Show search message if there aren't any current points
		TextView message = (TextView) findViewById(R.id.Radar_Message);
		if (message != null) {
			message.setVisibility(View.VISIBLE);
			message.setText("Searching for\ntags...");
		}
	}


	public class TagScanListener extends BaseTagScanListener {

		@Override
		public void onComplete(ArrayList<Entity> entities) {

			Utilities.Log(Aircandi.APP_NAME, "Radar", "Tag scan results returned from Ripple.TagExplorer");
			Utilities.Log(Aircandi.APP_NAME, "Radar", "Entity count: " + String.valueOf(entities.size()));

			try {

				// Replace the collection
				entityListFiltered_ = entities;
				Utilities.Log(Aircandi.APP_NAME, "Radar", "Setting RippleView dataSource property");
				rippleView_.setDataSource(entityListFiltered_);

				// Scanning is complete so change the heading back to normal
				// Show search message if there aren't any current points
				TextView message = (TextView) findViewById(R.id.Radar_Message);
				if (message != null) {
					message.setVisibility(View.GONE);
				}

				// Refresh the RippleView to reflect any updates to the collection of entities
				Utilities.Log(Aircandi.APP_NAME, "Radar", "Refreshing RippleView");
				rippleView_.refresh();

				stopProgress();

				// Schedule the next wifi scan run
				if (prefAutoscan_) {
					handler_.removeCallbacks(tagScanTask_);
					handler_.postDelayed(tagScanTask_, prefAutoscanInterval_);
				}
			}
			catch (Exception exception) {
				AircandiUI.showToastNotification(Radar.this, "Unknown error", Toast.LENGTH_SHORT);
			}
		}


		public void onIOException(IOException exception) {

			Utilities.Log(RippleService.APP_NAME, "Radar", exception.getMessage());
			AircandiUI.showToastNotification(Radar.this, "Network error", Toast.LENGTH_SHORT);
			stopProgress();
			exception.printStackTrace();
		}


		@Override
		public void onTagExplorerError(RippleError error) {

			AircandiUI.showToastNotification(Radar.this, error.getMessage(), Toast.LENGTH_SHORT);
			Utilities.Log(RippleService.APP_NAME, "Radar", error.getMessage());
			stopProgress();
		}

	}


	// ==========================================================================
	// MENU routines
	// ==========================================================================

	private TableLayout configureMenus(Entity entity, boolean landscape, Context context) {

		Boolean needMoreButton = false;
		if (entity.streams.size() > 6)
			needMoreButton = true;

		// Get the table we use for grouping and clear it
		final TableLayout table = new TableLayout(context);

		// Make the first row
		TableRow tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_streams, null);
		final TableRow.LayoutParams rowLp = new TableRow.LayoutParams();
		rowLp.setMargins(0, 0, 0, 0);
		TableLayout.LayoutParams tableLp;

		// Loop the streams
		Integer streamCount = 0;
		RelativeLayout streamButtonContainer;
		for (Stream stream : entity.streams) {
			// Make a button and configure it
			streamButtonContainer = (RelativeLayout) this.getLayoutInflater()
					.inflate(R.layout.temp_button_stream, null);

			final TextView streamButton = (TextView) streamButtonContainer.findViewById(R.id.StreamButton);
			final TextView streamBadge = (TextView) streamButtonContainer.findViewById(R.id.StreamBadge);
			streamButtonContainer.setTag(stream);
			if (needMoreButton && streamCount == 5) {
				streamButton.setText("More...");
				streamButton.setTag(stream);
			}
			else {
				streamButton.setText(stream.streamLabel);
				streamButton.setTag(stream);
				if (stream.itemCount > 0) {
					if (stream.streamName.toLowerCase().equals("eggs"))
						streamBadge.setVisibility(View.INVISIBLE);
					else {
						streamBadge.setText(String.valueOf(stream.itemCount));
						streamBadge.setTag(stream);
					}
				}
				else
					streamBadge.setVisibility(View.INVISIBLE);
			}
			// streamButton.getBackground().mutate().setColorFilter(getResources().getColor(R.color.button_color_filter),
			// PorterDuff.Mode.MULTIPLY);

			// Add button to row
			tableRow.addView(streamButtonContainer, rowLp);
			streamCount++;

			// If we have three in a row then commit it and make a new row
			int newRow = 2;
			if (landscape)
				newRow = 4;

			if (streamCount % newRow == 0) {
				tableLp = new TableLayout.LayoutParams();
				tableLp.setMargins(0, 0, 0, 0);
				table.addView(tableRow, tableLp);
				tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_streams, null);
			}
			else if (streamCount == 6)
				break;
		}

		// We might have an uncommited row with stream buttons in it
		if (streamCount != 3) {
			tableLp = new TableLayout.LayoutParams();
			tableLp.setMargins(0, 3, 0, 3);
			table.addView(tableRow, tableLp);
		}
		return table;
	}


	// ==========================================================================
	// WIFI routines
	// ==========================================================================

	private void wifiAskToEnable() {

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				switch (which) {
					case DialogInterface.BUTTON_POSITIVE :
						userRefusedWifiEnable_ = false;
						wifiEnable();
						break;
					case DialogInterface.BUTTON_NEGATIVE :
						userRefusedWifiEnable_ = true;
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setIcon(R.drawable.icon_alert_dialog).setMessage(R.string.alert_dialog_wifidisabled).setPositiveButton(
				R.string.alert_dialog_yes, dialogClickListener).setNegativeButton(R.string.alert_dialog_no,
				dialogClickListener).show();
	}


	private void wifiEnable() {

		tagExplorer_.enableWifi();
		AircandiUI.showToastNotification(this, "Wifi enabling...", Toast.LENGTH_LONG);
	}


	// ==========================================================================
	// UI routines
	// ==========================================================================

	/**
	 * Setup a new 3D rotation on the container view.
	 * 
	 * @param position the item that was clicked to show a picture, or -1 to show the list
	 * @param start the start angle at which the rotation must begin
	 * @param end the end angle of the rotation
	 */
	private void applyRotation(int position, float start, float end) {

		// Find the center of the container
		final float centerX = container_.getWidth() / 2.0f;
		final float centerY = container_.getHeight() / 2.0f;

		// Create a new 3D rotation with the supplied parameter
		// The animation listener is used to trigger the next animation
		final Rotate3dAnimation rotation = new Rotate3dAnimation(start, end, centerX, centerY, 310.0f, true);
		rotation.setDuration(500);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(new DisplayNextView(position));

		container_.startAnimation(rotation);
	}


	/**
	 * This class listens for the end of the first half of the animation. It then posts a new action that effectively
	 * swaps the views when the container is rotated 90 degrees and thus invisible.
	 */
	private final class DisplayNextView implements Animation.AnimationListener {

		private final int	mPosition;


		private DisplayNextView(int position) {

			mPosition = position;
		}


		public void onAnimationStart(Animation animation) {

		}


		public void onAnimationEnd(Animation animation) {

			container_.post(new SwapViews(mPosition));
		}


		public void onAnimationRepeat(Animation animation) {

		}
	}

	/**
	 * This class is responsible for swapping the views and start the second half of the animation.
	 */
	private final class SwapViews implements Runnable {

		private final int	mPosition;


		public SwapViews(int position) {

			mPosition = position;
		}


		public void run() {

			final float centerX = container_.getWidth() / 2.0f;
			final float centerY = container_.getHeight() / 2.0f;
			Rotate3dAnimation rotation;

			if (mPosition > -1) {
				rippleViewFrame_.setVisibility(View.GONE);
				detailViewFrame_.setVisibility(View.VISIBLE);
				detailViewFrame_.requestFocus();

				rotation = new Rotate3dAnimation(270, 360, centerX, centerY, 310.0f, false);
			}
			else {
				detailViewFrame_.setVisibility(View.GONE);
				rippleViewFrame_.setVisibility(View.VISIBLE);
				rippleViewFrame_.requestFocus();

				rotation = new Rotate3dAnimation(90, 0, centerX, centerY, 310.0f, false);
			}

			rotation.setDuration(500);
			rotation.setFillAfter(true);
			rotation.setInterpolator(new DecelerateInterpolator());

			container_.startAnimation(rotation);
		}
	}


	// ==========================================================================
	// MISC routines
	// ==========================================================================

	private void loadPreferences() {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs != null) {
			this.prefAutoscan_ = prefs.getBoolean(Preferences.PREF_AUTOSCAN, true);
			this.prefAutoscanInterval_ = Integer.parseInt(prefs.getString(Preferences.PREF_AUTOSCAN_INTERVAL, "5000"));
			this.prefTagLevelCutoff_ = Integer.parseInt(prefs.getString(Preferences.PREF_TAG_LEVEL_CUTOFF, "-100"));
			this.prefTagsWithEntitiesOnly_ = prefs.getBoolean(Preferences.PREF_TAGS_WITH_ENTITIES_ONLY, true);
			this.prefDisplayExtras_ = DisplayExtra.valueOf(prefs.getString(Preferences.PREF_DISPLAY_EXTRAS, "None"));
		}
	}


	private void configureTagExplorer() {

		if (tagExplorer_ != null) {
			tagExplorer_.setPrefAutoscan(prefAutoscan_);
			tagExplorer_.setPrefAutoscanInterval(prefAutoscanInterval_);
			tagExplorer_.setPrefTagLevelCutoff(prefTagLevelCutoff_);
			tagExplorer_.setPrefTagsWithEntitiesOnly(prefTagsWithEntitiesOnly_);
		}
	}


	private void configureRippleView() {

		if (rippleView_ != null) {
			rippleView_.setPrefDisplayExtras(prefDisplayExtras_);
		}
	}


	@Override
	// Don't count on this always getting called when this activity is killed
	protected void onDestroy() {

		try {
			super.onDestroy();
			tagExplorer_.onDestroy();
			mediaPlayerX_.Release();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	protected void onPause() {

		try {
			super.onPause();
			tagExplorer_.onPause();
			stopProgress();
		}
		catch (Exception exception) {
			exception.printStackTrace();
		}
	}


	@Override
	protected void onResume() {

		super.onResume();
		tagExplorer_.onResume();

		// OnResume gets called after OnCreate (always) and whenever the activity is
		// being brought back to the foreground. Because code in OnCreate could have
		// determined that we aren't ready to roll, isReadyToRun is used to indicate
		// that prep work is complete.

		// This is also called when the user jumps out and back from setting preferences
		// so we need to refresh the places where they get used.

		if (isReadyToRun_ && getCurrentUser() != null) {
			loadPreferences();
			configureTagExplorer();
			configureRippleView();

			// We always try to kick off a scan when radar is started or resumed
			scanForTags(true);
		}
	}
}