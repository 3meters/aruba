package com.proxibase.aircandi.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AsyncPlayer;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
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

import com.proxibase.aircandi.utilities.DateUtils;
import com.proxibase.aircandi.utilities.Utilities;
import com.proxibase.sdk.android.core.BaseBeaconScanListener;
import com.proxibase.sdk.android.core.BaseModifyListener;
import com.proxibase.sdk.android.core.BaseQueryListener;
import com.proxibase.sdk.android.core.BaseTagScanListener;
import com.proxibase.sdk.android.core.Entity;
import com.proxibase.sdk.android.core.LinkedTreeList;
import com.proxibase.sdk.android.core.Query;
import com.proxibase.sdk.android.core.ProxibaseError;
import com.proxibase.sdk.android.core.ProxibaseRunner;
import com.proxibase.sdk.android.core.ProxibaseService;
import com.proxibase.sdk.android.core.Stream;
import com.proxibase.sdk.android.core.ProxiExplorer;
import com.proxibase.sdk.android.core.ProxibaseService.GsonType;
import com.proxibase.sdk.android.widgets.ImageCache;
import com.proxibase.sdk.android.widgets.RippleAdapterView;
import com.proxibase.sdk.android.widgets.RippleView;
import com.proxibase.sdk.android.widgets.RippleView.DisplayExtra;
import com.proxibase.sdk.android.widgets.RippleView.GroupBy;
import com.proxibase.sdk.android.widgets.RippleView.OnEntityClickListener;
import com.proxibase.sdk.android.widgets.RippleView.OnEntitySelectedListener;
import com.proxibase.sdk.android.widgets.RippleView.SortProperty;
import com.proxibase.sdk.android.widgets.RippleView.SoundNotification;

@SuppressWarnings("unused")
public class Tricorder extends AircandiActivity {

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


	private Boolean			prefAutoscan_				= true;
	private int				prefAutoscanInterval_		= 5000;
	private int				prefTagLevelCutoff_			= -100;
	private boolean			prefTagsWithEntitiesOnly_	= true;
	private DisplayExtra	prefDisplayExtras_			= DisplayExtra.None;
	private float			prefTileScale_				= 1.0f;
	private boolean			prefTileRotate_;
	private boolean			prefSoundEffects_;

	private List<Entity>	entityList_;
	private List<Entity>	entityListFiltered_;

	private Boolean			isReadyToRun_				= false;
	private Handler			handler_					= new Handler();
	private CxMediaPlayer	soundEffects_;
	private MediaPlayer		mediaPlayer_;

	private RippleView		rippleView_;
	protected ImageCache	imageCache_;
	private LinearLayout	rippleContainer_;
	private FrameLayout		entitySummaryView_;
	private boolean			isDetailVisible_			= false;

	private boolean			userRefusedWifiEnable_		= false;
	private ProxiExplorer		proxiExplorer_;
	private Runnable		tagScanTask_				= new Runnable() {

															public void run() {

																scanForTags(false);
															}
														};


	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {

		// Start normal processing
		isReadyToRun_ = false;
		setContentView(R.layout.tricorder);
		super.onCreate(savedInstanceState);

		// Image cache
		imageCache_ = new ImageCache(getApplicationContext(), "aircandi", 100, 16);

		mediaPlayer_ = new MediaPlayer();
		soundEffects_ = new CxMediaPlayer(this);

		// Ui Hookup
		rippleContainer_ = (LinearLayout) findViewById(R.id.RippleContainer);
		// rippleContainer_.setPersistentDrawingCache(ViewGroup.PERSISTENT_ANIMATION_CACHE);

		entitySummaryView_ = (FrameLayout) findViewById(R.id.EntitySummaryView);

		// Ripple sdk components
		proxiExplorer_ = new ProxiExplorer(this);

		if (!proxiExplorer_.isWifiEnabled()) {
			this.wifiAskToEnable();
			if (userRefusedWifiEnable_)
				return;
		}

		// Property settings get overridden once we retrieve preferences
		rippleView_ = (RippleView) findViewById(R.id.RippleView);
		rippleView_.setDataSource(entityListFiltered_);
		rippleView_.setEnableRotation(true);
		rippleView_.setScale(1.0f);
		rippleView_.setGroupByLevelOne(GroupBy.EntityType);
		rippleView_.setSortLevelOne(SortProperty.DiscoveryTime);
		rippleView_.getMediaPlayer().putSound(SoundNotification.EntityNew,
				rippleView_.getMediaPlayer().getSoundPool().load(this, R.raw.notification1, 1));
		rippleView_.setSoundsEnabled(true);
		rippleView_.setOnEntitySelectedListener(new OnEntitySelectedListener() {

			@Override
			public void onEntitySelected(Entity entity) {

				Utilities.Log(ProxibaseService.APP_NAME, "RippleView", entity.label + " selected");

			}


			@Override
			public void onNothingSelected() {

			}
		});
		rippleView_.setOnEntityClickListener(new OnEntityClickListener() {

			public void onEntityClick(Entity entity) {

				setCurrentEntity(entity);
				isDetailVisible_ = true;

				new ShowDetailsTask().execute(entitySummaryView_);

				if (entity.pointResourceId != null && entity.pointResourceId != "")
					((ImageView) entitySummaryView_.findViewById(R.id.Image)).setImageBitmap(rippleView_
							.getImageCache().get(entity.pointResourceId));

				((TextView) entitySummaryView_.findViewById(R.id.Title)).setText(entity.title);
				((TextView) entitySummaryView_.findViewById(R.id.Subtitle)).setText(Html.fromHtml(entity.subtitle));
				((TextView) entitySummaryView_.findViewById(R.id.Description)).setText(Html
						.fromHtml(entity.description));

				applyRotation(rippleContainer_, 0, 90);
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
			TableLayout table = configureMenus(getCurrentEntity(), landscape, Tricorder.this);

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
			TableLayout table = configureMenus(getCurrentEntity(), landscape, Tricorder.this);
			FrameLayout frame = (FrameLayout) findViewById(R.id.frameMenu);
			frame.removeAllViews();
			frame.addView(table);
		}
	}


	// ==========================================================================
	// EVENT routines
	// ==========================================================================

	public void onStreamButtonClick(View view) {

		Stream stream = (Stream) view.getTag();
		String fullyQualifiedClass = "com.proxibase.aircandi.controller." + stream.streamClass;
		String streamName = stream.streamName.toLowerCase();

		if (stream.streamClass.toLowerCase().equals("list")) {
			AircandiUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
			return;
		}

		try {
			if (streamName.toLowerCase().equals("audio")) {
				String audioFile = "";
				if (stream.entityResourceId.contains("hopper_house"))
					audioFile = "hopper_house_by_the_railroad.mp3";
				else if (stream.entityResourceId.contains("klimt_hope"))
					audioFile = "klimt_hope.mp3";
				else if (stream.entityResourceId.contains("monet_japanese"))
					audioFile = "monet_japanese_footbridge.mp3";
				else if (stream.entityResourceId.contains("starry_night"))
					audioFile = "vangogh_starry_night.mp3";
				else if (stream.entityResourceId.contains("cezanne"))
					audioFile = "cezanne2.mp3";
				// MediaPlayer mediaPlayer = new MediaPlayer();
				// Uri uri = Uri.parse(Aircandi.URL_AIRCANDI_MEDIA + "audio.3meters.com/signsoflove.mp3");

				Uri uri = Uri.parse("http://dev.aircandi.com/media/" + audioFile);
				mediaPlayer_.setDataSource(Tricorder.this, uri);
				mediaPlayer_.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mediaPlayer_.prepare();
				mediaPlayer_.start();

				// Uri url = Uri.parse("http://dev.aircandi.com/media/cezanne2.mp3");
				// AsyncPlayer player = new AsyncPlayer("Aircandi");
				// player.stop();
				// player.play(Tricorder.this, url, false, AudioManager.STREAM_MUSIC);

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
				String jsonStream = ProxibaseService.getGson(GsonType.Internal).toJson(stream);
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
		catch (SecurityException exception) {
			// TODO Auto-generated catch block
			exception.printStackTrace();
		}
		catch (IOException exception) {
			// TODO Auto-generated catch block
			exception.printStackTrace();
		}
	}


	@Override
	public void onBackPressed() {

		if (!isDetailVisible_) {
			if (!rippleView_.navigateUp())
				super.onBackPressed();
		}
		else {
			mediaPlayer_.stop();
			mediaPlayer_.reset();
			applyRotation(rippleContainer_, 360, 270);
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
		mediaPlayer_.reset();
		applyRotation(rippleContainer_, 360, 270);
		setCurrentEntity(null);
		isDetailVisible_ = false;
	}


	// ==========================================================================
	// ENTITY routines
	// ==========================================================================

	private void scanForTags(Boolean showProgress) {

		if (showProgress)
			startProgress();

		Utilities.Log(Aircandi.APP_NAME, "Tricorder", "Calling Ripple.ProxiExplorer to scan for tags");
		proxiExplorer_.scanForTags(new TagScanListener());

		// Show search message if there aren't any current points
		TextView message = (TextView) findViewById(R.id.Tricorder_Message);
		if (message != null) {
			message.setVisibility(View.VISIBLE);
			message.setText("Searching for\ntags...");
		}
	}


	public class TagScanListener extends BaseBeaconScanListener {

		@Override
		public void onComplete(ArrayList<ProxiEntity> entities) {

			Utilities.Log(Aircandi.APP_NAME, "Tricorder", "Tag scan results returned from Ripple.ProxiExplorer");
			Utilities.Log(Aircandi.APP_NAME, "Tricorder", "Entity count: " + String.valueOf(entities.size()));

			try {

				// Scanning is complete so change the heading back to normal
				// Show search message if there aren't any current points
				TextView message = (TextView) findViewById(R.id.Tricorder_Message);
				if (message != null) {
					message.setVisibility(View.GONE);
				}

				// Refresh the RippleView to reflect any updates to the collection of entities.
				// If using autoscan, we skip a refresh if the user is doing some scrolling because
				// layout can make the UI jerky.
				if (!prefAutoscan_ || rippleView_.isMotionless()) {
					Utilities.Log(Aircandi.APP_NAME, "Tricorder", "Refreshing RippleView");
					// Replace the collection
					entityListFiltered_ = (ArrayList<Entity>) entities.clone();
					rippleView_.setDataSource(entityListFiltered_);
					rippleView_.refresh();
				}
				else
					Utilities.Log(Aircandi.APP_NAME, "Tricorder", "UI *busy* so skipping RippleView refresh");

				stopProgress();

				// Schedule the next wifi scan run
				if (prefAutoscan_) {
					handler_.removeCallbacks(tagScanTask_);
					handler_.postDelayed(tagScanTask_, prefAutoscanInterval_);
				}
			}
			catch (Exception exception) {
				AircandiUI.showToastNotification(Tricorder.this, "Unknown error", Toast.LENGTH_SHORT);
			}
		}


		public void onIOException(IOException exception) {

			Utilities.Log(ProxibaseService.APP_NAME, "Tricorder", exception.getMessage());
			AircandiUI.showToastNotification(Tricorder.this, "Network error", Toast.LENGTH_SHORT);
			stopProgress();
			exception.printStackTrace();
		}


		@Override
		public void onProxiExplorerError(ProxibaseError error) {

			AircandiUI.showToastNotification(Tricorder.this, error.getMessage(), Toast.LENGTH_SHORT);
			Utilities.Log(ProxibaseService.APP_NAME, "Tricorder", error.getMessage());
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

		proxiExplorer_.enableWifi();
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
	private void applyRotation(View view, float start, float end) {

		boolean rotateRight = (end - start > 0);

		// Find the center of the container
		final float centerX = view.getWidth() / 2.0f;
		final float centerY = view.getHeight() / 2.0f;

		// Create a new 3D rotation with the supplied parameter
		// The animation listener is used to trigger the next animation
		final Rotate3dAnimation rotation = new Rotate3dAnimation(start, end, centerX, centerY, 310.0f, true);
		rotation.setDuration(500);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(new DisplayNextView(rotateRight));

		view.startAnimation(rotation);
	}


	/**
	 * This class listens for the end of the first half of the animation. It then posts a new action that effectively
	 * swaps the views when the container is rotated 90 degrees and thus invisible.
	 */
	private final class DisplayNextView implements Animation.AnimationListener {

		private final boolean	rotateRight_;


		private DisplayNextView(boolean rotateRight) {

			rotateRight_ = rotateRight;
		}


		public void onAnimationStart(Animation animation) {

		}


		public void onAnimationEnd(Animation animation) {

			rippleContainer_.post(new SwapViews(rotateRight_));
		}


		public void onAnimationRepeat(Animation animation) {

		}
	}

	/**
	 * This class is responsible for swapping the views and start the second half of the animation.
	 */
	private final class SwapViews implements Runnable {

		private boolean	rotateRight_;


		public SwapViews(boolean rotateRight) {

			rotateRight_ = rotateRight;
		}


		public void run() {

			final float centerX = rippleContainer_.getWidth() / 2.0f;
			final float centerY = rippleContainer_.getHeight() / 2.0f;
			Rotate3dAnimation rotation;

			if (rotateRight_) {
				rippleView_.setVisibility(View.GONE);
				entitySummaryView_.setVisibility(View.VISIBLE);
				entitySummaryView_.requestFocus();

				rotation = new Rotate3dAnimation(270, 360, centerX, centerY, 310.0f, false);
			}
			else {
				entitySummaryView_.setVisibility(View.GONE);
				rippleView_.setVisibility(View.VISIBLE);
				rippleView_.requestFocus();

				rotation = new Rotate3dAnimation(90, 0, centerX, centerY, 310.0f, false);
			}

			rotation.setDuration(500);
			rotation.setFillAfter(true);
			rotation.setInterpolator(new DecelerateInterpolator());

			rippleContainer_.startAnimation(rotation);
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
			this.prefTileScale_ = Float.parseFloat(prefs.getString(Preferences.PREF_TILE_SCALE, "1.0"));
			this.prefTileRotate_ = prefs.getBoolean(Preferences.PREF_TILE_ROTATE, true);
			this.prefSoundEffects_ = prefs.getBoolean(Preferences.PREF_SOUND_EFFECTS, true);
		}
	}


	private void configureProxiExplorer() {

		if (proxiExplorer_ != null) {
			proxiExplorer_.setPrefTagLevelCutoff(prefTagLevelCutoff_);
			proxiExplorer_.setPrefTagsWithEntitiesOnly(prefTagsWithEntitiesOnly_);
		}
	}


	private void configureRippleView() {

		if (rippleView_ != null) {
			rippleView_.setPrefDisplayExtras(prefDisplayExtras_);
			rippleView_.setScale(prefTileScale_);
			rippleView_.setEnableRotation(prefTileRotate_);
			rippleView_.setSoundsEnabled(prefSoundEffects_);
		}
	}


	@Override
	// Don't count on this always getting called when this activity is killed
	protected void onDestroy() {

		try {
			super.onDestroy();
			proxiExplorer_.onDestroy();
			soundEffects_.Release();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	protected void onPause() {

		try {
			super.onPause();
			proxiExplorer_.onPause();
			stopProgress();
		}
		catch (Exception exception) {
			exception.printStackTrace();
		}
	}


	@Override
	protected void onResume() {

		super.onResume();
		proxiExplorer_.onResume();

		// OnResume gets called after OnCreate (always) and whenever the activity is
		// being brought back to the foreground. Because code in OnCreate could have
		// determined that we aren't ready to roll, isReadyToRun is used to indicate
		// that prep work is complete.

		// This is also called when the user jumps out and back from setting preferences
		// so we need to refresh the places where they get used.

		if (isReadyToRun_) {
			loadPreferences();
			configureProxiExplorer();
			configureRippleView();

			// We always try to kick off a scan when radar is started or resumed
			scanForTags(true);
		}
	}
}