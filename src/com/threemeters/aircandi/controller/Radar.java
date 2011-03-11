package com.threemeters.aircandi.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
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
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.facebook.android.BaseRequestListener;
import com.threemeters.aircandi.model.UserFb;
import com.threemeters.aircandi.utilities.DateUtils;
import com.threemeters.aircandi.utilities.Utilities;
import com.threemeters.sdk.android.core.BaseModifyListener;
import com.threemeters.sdk.android.core.BaseQueryListener;
import com.threemeters.sdk.android.core.Entity;
import com.threemeters.sdk.android.core.Query;
import com.threemeters.sdk.android.core.RippleRunner;
import com.threemeters.sdk.android.core.RippleService;
import com.threemeters.sdk.android.core.Stream;
import com.threemeters.sdk.android.core.RippleService.GsonType;
import com.threemeters.sdk.android.core.RippleService.QueryFormat;
import com.threemeters.sdk.android.widgets.ImageCache;
import com.threemeters.sdk.android.widgets.RippleView;

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


	public Boolean				isSuspended_			= false;

	private Boolean				prefAutoscan_			= true;
	private int					prefAutoscanInterval_	= 5000;

	private ArrayList<Entity>	entityList_				= new ArrayList<Entity>();
	private ArrayList<Entity>	entityListFiltered_		= new ArrayList<Entity>();

	private Context				context_;
	private Boolean				isReadyToRun_			= false;
	private Handler				handler_				= new Handler();
	private CxMediaPlayer		mediaPlayerX_;
	private MediaPlayer			mediaPlayer_;

	private RippleView			rippleView_;
	protected ImageCache		imageCache_;
	private LinearLayout		container_;
	private FrameLayout			rippleViewFrame_;
	private FrameLayout			detailViewFrame_;
	private boolean				isDetailVisible_		= false;

	private Boolean				scanRequestActive_		= false;
	private Boolean				scanRequestProcessing_	= false;
	private boolean				userRefusedWifiEnable_	= false;
	private List<ScanResult>	wifiList_;
	private WifiManager			wifiManager_;
	private WifiLock			wifiLock_;
	private WifiReceiver		wifiReceiver_			= new WifiReceiver();
	private Runnable			wifiScanTask_			= new Runnable() {

															public void run() {

																wifiScan(false);
															}
														};


	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {

		// Start normal processing
		isReadyToRun_ = false;
		setContentView(R.layout.radar);
		super.onCreate(savedInstanceState);

		// We stash some preference settings
		loadPreferences();

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

			Utilities.Log(Aircandi.APP_NAME, "Radar: starting facebook graph request for user");
			FacebookService.facebookRunner.request("me", new UserRequestListener());
		}

		// Stashing
		wifiManager_ = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		mediaPlayer_ = new MediaPlayer();
		mediaPlayerX_ = new CxMediaPlayer(this);

		// Ui Hookup
		container_ = (LinearLayout) findViewById(R.id.container);
		container_.setPersistentDrawingCache(ViewGroup.PERSISTENT_ANIMATION_CACHE);

		rippleViewFrame_ = (FrameLayout) findViewById(R.id.flowframe);
		detailViewFrame_ = (FrameLayout) findViewById(R.id.detailframe);

		rippleView_ = (RippleView) findViewById(R.id.RippleView);
		rippleView_.setDataSource(entityListFiltered_);
		rippleView_.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView parent, View v, int position, long id) {

				Entity entity = entityListFiltered_.get(position);
				setCurrentEntity(entity);
				isDetailVisible_ = true;

				new ShowDetailsTask().execute(detailViewFrame_);

				if (entity.pointResourceId != null && entity.pointResourceId != "")
					((ImageView) detailViewFrame_.findViewById(R.id.Image)).setImageBitmap(imageCache_
							.get(entity.pointResourceId));

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
			Utilities.Log(Aircandi.APP_NAME, "Radar: returning facebook graph request for user");
			setCurrentUser(RippleService.getGson(GsonType.Internal).fromJson(response, UserFb.class));

			// Once we have a current user, we launch our first wifi scan (which
			// leads to data requests). Other calls to start wifi scans are in Resume, manual refresh,
			// and autoscan (setup at the end of processing a previous wifi scan).

			// Turn on our receiver that it listens for wifi scan results
			registerReceiver(wifiReceiver_, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			
			// Launch an asychronous task to perform a wifi scan.
			handler_.removeCallbacks(wifiScanTask_);
			handler_.post(wifiScanTask_);

			// Update the user with the most current facebook token
			getCurrentUser().token = FacebookService.facebookRunner.facebook.getAccessToken();
			getCurrentUser().tokenDate = DateUtils.nowString();

			// Make sure the current user is registered in the Aircandi service
			RippleRunner rippleRunner = new RippleRunner();
			Query query = new Query("Users").filter("Id eq '" + getCurrentUser().id + "'");
			Utilities.Log(Aircandi.APP_NAME,
					"Radar: starting ripple query to see if a facebook user '" + getCurrentUser().id
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
				Utilities.Log(Aircandi.APP_NAME, "Radar: starting ripple insert for '" + getCurrentUser().id + "'");
				rippleRunner.insert(getCurrentUser(), "Users", new UserReadyListener());
			}
			else {
				Utilities.Log(Aircandi.APP_NAME, "Radar: starting ripple update for '" + getCurrentUser().id + "'");
				rippleRunner.update(getCurrentUser(), getCurrentUser().getUriOdata(), new UserReadyListener());
			}
		}
	}

	class UserReadyListener extends BaseModifyListener {

		public void onComplete() {

			// This is where to start any post processing needed on the UI thread.
			// For now, we just log the completion of the interaction with the Aircandi service.
			Utilities.Log(Aircandi.APP_NAME, "Radar: user '" + getCurrentUser().id + "' has been inserted or updated");
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
		wifiScan(true);
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

	class WifiReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(final Context context, Intent intent) {

			// We might be getting called back but the user has signed out so there is no
			// current user. In that case, we should just exit.
			if (getCurrentUser() == null)
				return;

			// We only process scan results we have requested
			if (scanRequestActive_ && !scanRequestProcessing_) {
				scanRequestActive_ = false;
				scanRequestProcessing_ = true;

				// Get the latest scan results
				wifiList_ = wifiManager_.getScanResults();
				Utilities.Log(Aircandi.APP_NAME, "Radar: received: " + intent.getAction().toString());
				Utilities.Log(Aircandi.APP_NAME,
						"Radar: starting AsyncTask to rebuild entities (includes calls to ripple service)");
				
				// Start asych task to process the results of the just completed wifi scan.
				new RebuildEntitiesTask().execute(entityList_, wifiList_);
			}
		}
	}

	class RebuildEntitiesTask extends AsyncTask<Object, Void, ArrayList<Entity>> {

		IOException	exception	= null;


		@Override
		protected ArrayList<Entity> doInBackground(Object... params) {

			// We are on the background thread
			ArrayList<Entity> entityListNew = null;
			try {
				entityListNew = lookupEntities((List<Entity>) params[0], (List<ScanResult>) params[1]);
			}
			catch (IOException e) {
				exception = e;
				e.printStackTrace();
			}
			return entityListNew;
		}


		@Override
		protected void onPostExecute(ArrayList<Entity> pointListNew) {

			// We are on the UI thread
			super.onPostExecute(pointListNew);

			if (exception != null) {
				AircandiUI.showToastNotification(Radar.this, "Network error", Toast.LENGTH_SHORT);
				stopProgress();
				return;
			}

			Utilities.Log(Aircandi.APP_NAME, "Radar: returning AsyncTask to rebuild points");
			updatePointCollection(pointListNew);

			// Let the tagexplorerradar ui know that there are fresh results to process
			try {

				// Refresh the RippleView to reflect any updates to the collection of entities
				rippleView_.refresh();

				// Scanning is complete so change the heading back to normal
				// Show search message if there aren't any current points
				TextView message = (TextView) findViewById(R.id.Radar_Message);
				if (message != null) {
					message.setVisibility(View.GONE);
				}

				stopProgress();

				// Schedule the next wifi scan run
				scanRequestProcessing_ = false;
				if (prefAutoscan_) {
					handler_.removeCallbacks(wifiScanTask_);
					handler_.postDelayed(wifiScanTask_, prefAutoscanInterval_);
				}
			}
			catch (Exception e) {
				AircandiUI.showToastNotification(context_, "Radar callback failed.", Toast.LENGTH_SHORT);
			}
		}
	}


	public ArrayList<Entity> lookupEntities(List<Entity> entityList, List<ScanResult> scanList) throws IOException {

		ArrayList<Entity> entityListNew = new ArrayList<Entity>();

		// Walk all the wifi scan hits
		for (int i = 0; i < scanList.size(); i++) {
			
			ScanResult scanResult = scanList.get(i);
			String tagId = scanResult.BSSID;
			String label = scanResult.SSID;
			String ssid = scanResult.SSID;
			int levelDb = scanResult.level;

			// Check to see if this wifi point is already being tracked. Every wifi point (tagged or not) has an 
			// associated entity. For untagged wifi points the entity is a default null type reserved just for 
			// them and gives us a way to represent them for interaction when needed.
			//
			// Jayma: As this code is currently written, we won't catch cases where a tracked raw wifi point has been
			// externally modified unless the tracked entities are cleared before this method is called.
			
			ArrayList<Entity> entitiesExisting = null;
			if (entityList != null)
				entitiesExisting = getEntitiesByTagId(entityList, tagId);

			// Any entities already being tracked are carried forward
			if (entitiesExisting != null && entitiesExisting.size() != 0) {
				for (Entity entity : entitiesExisting) {
					entity.isNew = false;
					entity.addScanPass(levelDb);
					entityListNew.add(entity);
				}
			}
			else {
				
				// Call the ripple service to see if this wifi point has been tagged with any entities.
				// If call comes back null then there was a network or service problem.
				// The user got a toast notification from the service and we will still display
				// the point as raw wifi only. We are making synchronous calls inside 
				// an asynchronous thread.
				
				RippleService ripple = new RippleService();

				String response;
				try {
					
					// New way to get entity data
					Bundle parameters = new Bundle();
					parameters.putString("userId", getCurrentUser().id);
					parameters.putString("bssid", tagId);
					response = ripple.webMethod("GetEntitiesForTag", parameters, QueryFormat.Json);

					ArrayList<Object> entities = RippleService.convertJsonToObjects(response, Entity.class);

					if (entities == null || entities.size() == 0) {
						
						// Get a fake entity to represent the untagged wifi point
						Entity entity = Entity.getEntityStub(label, tagId, ssid, levelDb, DateUtils.nowDate(), (entities != null), true);
						entityListNew.add(entity);
					}
					else {
						for (Object obj : entities) {
							Entity entity = (Entity) obj;

							entity.isTagged = true;
							entity.isServiceVerified = true;
							entity.levelDb = levelDb;
							entity.isNew = true;
							entity.imageUrl = Aircandi.URL_AIRCANDI_MEDIA + "3meters_images/" + entity.pointResourceId;
							entity.discoveryTime = DateUtils.nowDate();
							entity.addScanPass(levelDb);

							entityListNew.add(entity);
						}
					}
				}
				catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
					throw e;
				}
				catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// We need to move over any points in the previous pointList that were
		// not in the current scanList
		for (Entity point : entityList)
			if (getEntitiesByTagId(entityListNew, point.tagId) == null) {
				point.scanMisses++;
				if (point.scanMisses < 3)
					entityListNew.add(point);
			}

		// Replace the core point list
		return entityListNew;
	}


	public void updatePointCollection(ArrayList<Entity> pointListNew) {

		/**
		 * This is called whenever we get the results of a new wifi scan. We need to merge any changes together with our
		 * existing collection of points to reflect the current state of affairs.
		 */
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		Boolean prefRippleSpotsOnly = prefs.getBoolean(Preferences.PREF_RIPPLE_SPOTS_ONLY, false);
		Integer prefLevelCutoff = Integer.parseInt(prefs.getString(Preferences.PREF_LEVEL_CUTOFF, "-100"));

		if (pointListNew == null)
			return;

		entityList_.clear();
		entityListFiltered_.clear();

		for (Entity point : pointListNew) {
			entityList_.add(point);

			// Restore the current entity even if it might be filtered out based
			// on other criteria.
			// This is done so it isn't yanked out from under the user when they
			// come back from a downstream operation.
			// We wouldn't have to do this if we didn't always refresh when the
			// activity is resumed but we refresh
			// because downstream operations could have dirtied our current
			// state.
			if (getCurrentEntity() != null && getCurrentEntity().tagId.equals(point.tagId)
				&& getCurrentEntity().label.equals(point.label))
				entityListFiltered_.add(getCurrentEntity());
			else {
				// Filtering
				if (prefRippleSpotsOnly && !point.isTagged)
					continue;

				if (point.isTagged && point.getAvgPointLevel() < point.signalThreshold) {
					point.isHidden = true;
					continue;
				}

				int avgPointLevel = point.getAvgPointLevel();
				if (avgPointLevel < prefLevelCutoff) {
					point.isHidden = true;
					continue;
				}

				if (point.isHidden)
					point.isNew = true;
				point.isHidden = false;
				entityListFiltered_.add(point);
			}
		}

		// Sort the point collection by point type and how recently discovered
		Collections.sort(entityListFiltered_, new SortByDiscoveryTime());
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

		wifiManager_.setWifiEnabled(true);
		AircandiUI.showToastNotification(this, "Wifi enabling...", Toast.LENGTH_LONG);
	}


	private void wifiLockAcquire(int lockType) {

		if (wifiLock_ == null) {
			wifiLock_ = wifiManager_.createWifiLock(lockType, "Aircandi");
			wifiLock_.setReferenceCounted(false);
		}
		if (!wifiLock_.isHeld())
			wifiLock_.acquire();
	}


	private void wifiReleaseLock() {

		if (wifiLock_.isHeld())
			wifiLock_.release();
	}


	private void wifiScan(boolean showProgress) {

		if (wifiManager_.isWifiEnabled()) {
			scanRequestActive_ = true;
			if (showProgress)
				startProgress();

			// Show search message if there aren't any current points
			TextView message = (TextView) findViewById(R.id.Radar_Message);
			if (message != null) {
				message.setVisibility(View.VISIBLE);
				message.setText("Searching for\nspots...");
			}

			wifiLockAcquire(WifiManager.WIFI_MODE_FULL);
			wifiList_ = wifiManager_.getScanResults();
			Utilities.Log(Aircandi.APP_NAME, "Radar: requesting wifi scan");
			wifiManager_.startScan();
		}
		else if (wifiManager_.getWifiState() == WifiManager.WIFI_STATE_ENABLING)
			AircandiUI.showToastNotification(this, "Wifi still enabling...", Toast.LENGTH_SHORT);
		else if (!userRefusedWifiEnable_)
			wifiAskToEnable();
		else
			AircandiUI.showToastNotification(this, "Wifi is disabled.", Toast.LENGTH_SHORT);
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
			Integer.parseInt(prefs.getString(Preferences.PREF_LEVEL_CUTOFF, "-100"));
			this.setPrefAutoscan(prefs.getBoolean(Preferences.PREF_AUTOSCAN, true));
			prefAutoscanInterval_ = Integer.parseInt(prefs.getString(Preferences.PREF_AUTOSCAN_INTERVAL, "5000"));
		}
	}


	public void setPrefAutoscan(Boolean prefAutoScan) {

		this.prefAutoscan_ = prefAutoScan;
	}


	@Override
	// Don't count on this always getting called when this activity is killed
	protected void onDestroy() {

		try {
			super.onDestroy();
			// We are aggressive about hold our wifi lock so we need to be sure
			// it gets released when we are destroyed.
			wifiReleaseLock();
			mediaPlayerX_.Release();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	@Override
	protected void onPause() {

		try {
			super.onPause();

			// Start blocking the processing of any scan messages even
			// if we have an active request
			scanRequestActive_ = false;
			if (wifiReceiver_ != null)
				getApplicationContext().unregisterReceiver(wifiReceiver_);

			// Make sure we don't get called back by a running scan task
			handler_.removeCallbacks(wifiScanTask_);

			// So we don't leave the UI in the middle of processing
			stopProgress();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	@Override
	protected void onResume() {

		super.onResume();

		// OnResume gets called even if we bailed early to get signed in so we
		// use a flag to track whether we are completely ready for full
		// operations.
		// We also don't want to launch a wifi scan (which leads to user
		// specific data operations) until
		// we have a current user. The very first wifi scan request gets kicked
		// off
		// at the end of the code that establishes a current user.
		if (isReadyToRun_ && getCurrentUser() != null) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			if (prefs != null) {
				Integer.parseInt(prefs.getString(Preferences.PREF_LEVEL_CUTOFF, "-100"));
				this.setPrefAutoscan(prefs.getBoolean(Preferences.PREF_AUTOSCAN, true));
				prefAutoscanInterval_ = Integer.parseInt(prefs.getString(Preferences.PREF_AUTOSCAN_INTERVAL, "5000"));
			}

			// We always try to kick off a scan when radar is started or resumed
			registerReceiver(wifiReceiver_, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			handler_.removeCallbacks(wifiScanTask_);
			handler_.post(wifiScanTask_);
		}
	}


	class SortBySpotLevelDb implements Comparator<Entity> {

		@Override
		public int compare(Entity object1, Entity object2) {

			if (object1.isTagged && !object2.isTagged)
				return -1;
			else if (object2.isTagged && !object1.isTagged)
				return 1;
			else
				return object2.getAvgPointLevel() - object1.getAvgPointLevel();
		}
	}

	class SortByDiscoveryTime implements Comparator<Entity> {

		@Override
		public int compare(Entity object1, Entity object2) {

			if (object1.isTagged && !object2.isTagged)
				return -1;
			else if (object2.isTagged && !object1.isTagged)
				return 1;
			else {
				if ((object2.discoveryTime.getTime() / 100) - (object1.discoveryTime.getTime() / 100) < 0)
					return -1;
				else if ((object2.discoveryTime.getTime() / 100) - (object1.discoveryTime.getTime() / 100) > 0)
					return 1;
				else {
					if (object2.label.compareToIgnoreCase(object1.label) < 0)
						return 1;
					else if (object2.label.compareToIgnoreCase(object1.label) > 0)
						return -1;
					else
						return 0;
				}
			}
		}
	}

	public enum WifiAction {
		Enabled, Enabling, Disabled, EnableAndRetry
	}

}