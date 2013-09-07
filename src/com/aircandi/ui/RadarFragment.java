package com.aircandi.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher.DefaultHeaderTransformer;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ScanService;
import com.aircandi.components.BusProvider;
import com.aircandi.components.EntityManager;
import com.aircandi.components.FontManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ConnectedState;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ScanReason;
import com.aircandi.components.ProximityManager.WifiScanResult;
import com.aircandi.components.Tracker;
import com.aircandi.events.BeaconsLockedEvent;
import com.aircandi.events.EntitiesByProximityFinishedEvent;
import com.aircandi.events.EntitiesChangedEvent;
import com.aircandi.events.LocationChangedEvent;
import com.aircandi.events.LocationLockedEvent;
import com.aircandi.events.LocationTimeoutEvent;
import com.aircandi.events.MessageEvent;
import com.aircandi.events.MonitoringWifiScanReceivedEvent;
import com.aircandi.events.PlacesNearLocationFinishedEvent;
import com.aircandi.events.QueryWifiScanReceivedEvent;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.base.IDatabind;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.DateTime.IntervalContext;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

public class RadarFragment extends BaseFragment implements IDatabind,
		PullToRefreshAttacher.OnRefreshListener {

	private final Handler			mHandler				= new Handler();

	private Number					mEntityModelBeaconDate;
	private Integer					mEntityModelWifiState	= WifiManager.WIFI_STATE_UNKNOWN;

	private ListView				mList;
	private MenuItem				mMenuItemBeacons;
	private TextView				mBeaconIndicator;
	private String					mDebugWifi;
	private String					mDebugLocation			= "--";
	private PullToRefreshAttacher	mPullToRefreshAttacher;

	private SoundPool				mSoundPool;
	private int						mNewCandiSoundId;
	private final List<Entity>		mEntities				= new ArrayList<Entity>();
	private RadarListAdapter		mRadarAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSoundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 100);
		mNewCandiSoundId = mSoundPool.load(getSherlockActivity(), R.raw.notification_candi_discovered, 1);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = super.onCreateView(inflater, container, savedInstanceState);
		mList = (ListView) view.findViewById(R.id.radar_list);

		// Now get the PullToRefresh attacher from the Activity. An exercise to the reader
		// is to create an implicit interface instead of casting to the concrete Activity
		mPullToRefreshAttacher = ((AircandiForm) getSherlockActivity()).getPullToRefreshAttacher();

		// Now set the ScrollView as the refreshable view, and the refresh listener (this)
		mPullToRefreshAttacher.addRefreshableView(mList, this);

		DefaultHeaderTransformer header = (DefaultHeaderTransformer) mPullToRefreshAttacher.getHeaderTransformer();
		header.setRefreshingText(getString(R.string.refresh_scanning_for_location));
		header.setPullText(getString(R.string.refresh_label_pull));
		header.setReleaseText(getString(R.string.refresh_label_release));
		FontManager.getInstance().setTypefaceDefault((TextView) getSherlockActivity().findViewById(R.id.ptr_text));

		/* Other UI references */
		mList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final Place entity = (Place) mRadarAdapter.getItems().get(position);
				Bundle extras = null;
				if (entity.synthetic) {
					extras = new Bundle();
					extras.putBoolean(Constants.EXTRA_UPSIZE_SYNTHETIC, true);
				}
				Routing.route(getSherlockActivity(), Route.Browse, entity, null, extras);
			}
		});

		return view;
	}

	@Override
	public void databind(BindingMode mode) {
		/*
		 * Cases that trigger a search
		 * 
		 * - First time radar is run
		 * - Preference change
		 * - Didn't complete location fix before user switched away from radar
		 * - While away, user enabled wifi
		 * - Beacons we used for last fix have changed
		 * - Beacon fix is thirty minutes old or more
		 * 
		 * Cases that trigger a ui refresh
		 * 
		 * - Preference change
		 * - EntityModel has changed since last search
		 */

		/* Adapter snapshots the items in mEntities */
		if (mRadarAdapter == null) {
			Logger.d(getSherlockActivity(), "Start first place search");

			mRadarAdapter = new RadarListAdapter(getSherlockActivity(), mEntities);
			mList.setAdapter(mRadarAdapter);
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					searchForPlaces();
				}
			}, 500);

		}
		else if (LocationManager.getInstance().getLocationLocked() == null) {
			/*
			 * Gets set everytime we accept a location change in onLocationChange. Means
			 * we didn't get an acceptable fix yet from either the network or gps providers.
			 */
			Logger.d(getSherlockActivity(), "Start place search because didn't complete location fix");
			LocationManager.getInstance().lockLocationBurst();
		}
		else if (ProximityManager.getInstance().beaconRefreshNeeded(LocationManager.getInstance().getLocationLocked())) {
			/*
			 * We check to see if it's been awhile since the last search.
			 */
			Logger.d(getSherlockActivity(), "Start place search because of staleness");
			searchForPlaces();
		}
		else if (NetworkManager.getInstance().getWifiState() == WifiManager.WIFI_STATE_DISABLED
				&& mEntityModelWifiState == WifiManager.WIFI_STATE_ENABLED) {
			/*
			 * Wifi has been disabled since our last search
			 */
			Integer wifiApState = NetworkManager.getInstance().getWifiApState();
			if (wifiApState != null && (wifiApState == NetworkManager.WIFI_AP_STATE_ENABLED
					|| wifiApState == NetworkManager.WIFI_AP_STATE_ENABLED + 10)) {
				Logger.d(getSherlockActivity(), "Wifi Ap enabled, clearing beacons");
				UI.showToastNotification("Hotspot or tethering enabled", Toast.LENGTH_SHORT);
			}
			else {
				UI.showToastNotification("Wifi disabled", Toast.LENGTH_SHORT);
			}
			ProximityManager.getInstance().getWifiList().clear();
			EntityManager.getEntityCache().removeEntities(Constants.SCHEMA_ENTITY_BEACON, null, null);
			LocationManager.getInstance().setLocationLocked(null);
		}
		else if (NetworkManager.getInstance().getWifiState() == WifiManager.WIFI_STATE_ENABLED
				&& mEntityModelWifiState == WifiManager.WIFI_STATE_DISABLED) {
			/*
			 * Wifi has been enabled since our last search
			 */
			UI.showToastNotification("Wifi enabled", Toast.LENGTH_SHORT);
			searchForPlaces();
		}
		else if ((ProximityManager.getInstance().getLastBeaconLockedDate() != null && mEntityModelBeaconDate != null)
				&& (ProximityManager.getInstance().getLastBeaconLockedDate().longValue() > mEntityModelBeaconDate.longValue())) {
			/*
			 * The beacons we are locked to have changed while we were away so we need to
			 * search for new places linked to beacons.
			 */
			Logger.d(getSherlockActivity(), "Refresh places for beacons because beacon date has changed");
			mEntityModelBeaconDate = ProximityManager.getInstance().getLastBeaconLockedDate();
			new AsyncTask() {

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("GetEntitiesForBeacons");
					final ServiceResponse serviceResponse = ProximityManager.getInstance().getEntitiesByProximity();
					return serviceResponse;
				}

			}.execute();
		}
		else if (mList.getAdapter() == null) {
			/*
			 * View is being recreated but we already have data.
			 */
			mList.setAdapter(mRadarAdapter);
			mRadarAdapter.notifyDataSetChanged();
		}
		else if (unsynchronized()) {
			/*
			 * Everytime we show details for a place, we fetch place details from the service
			 * when in turn get pushed into the cache and activityDate gets tickled.
			 */
			Logger.d(getSherlockActivity(), "Update radar ui because of detected entity model change");
			mBusyManager.showBusy();
			mRadarAdapter.getItems().clear();
			mRadarAdapter.getItems().addAll(EntityManager.getInstance().getPlaces(null, null));
			mRadarAdapter.notifyDataSetChanged();
			hideBusy();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Bus: beacons
	// --------------------------------------------------------------------------------------------

	@Subscribe
	@SuppressWarnings("ucd")
	public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

		updateDevIndicator(event.wifiList, null);
		getSherlockActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Aircandi.stopwatch1.segmentTime("Wifi scan received event fired");
				Tracker.sendTiming("radar", Aircandi.stopwatch1.getTotalTimeMills(), "wifi_scan_received", null, Aircandi.getInstance().getUser());
				Logger.d(getSherlockActivity(), "Query wifi scan received event: locking beacons");

				if (event.wifiList != null) {
					ProximityManager.getInstance().lockBeacons();
				}
				else {
					BusProvider.getInstance().post(new EntitiesByProximityFinishedEvent());
				}
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onBeaconsLocked(BeaconsLockedEvent event) {

		getSherlockActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Aircandi.stopwatch1.segmentTime("Beacons locked event fired");
				mEntityModelBeaconDate = ProximityManager.getInstance().getLastBeaconLockedDate();
				new AsyncTask() {

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("GetEntitiesForBeacons");
						final ServiceResponse serviceResponse = ProximityManager.getInstance().getEntitiesByProximity();
						return serviceResponse;
					}

					@Override
					protected void onPostExecute(Object result) {
						final ServiceResponse serviceResponse = (ServiceResponse) result;
						if (serviceResponse.responseCode != ResponseCode.Success) {
							Routing.serviceError(getSherlockActivity(), serviceResponse);
							onError();
						}
					}

				}.execute();
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onEntitiesByProximityFinished(EntitiesByProximityFinishedEvent event) {

		getSherlockActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Aircandi.stopwatch1.segmentTime("Entities by proximity finished event fired");
				Tracker.sendTiming("radar", Aircandi.stopwatch1.getTotalTimeMills(), "entities_for_beacons", null, Aircandi.getInstance().getUser());
				Logger.d(getSherlockActivity(), "Entities for beacons finished event: ** done **");
				mEntityModelWifiState = NetworkManager.getInstance().getWifiState();
				mPullToRefreshAttacher.setRefreshComplete();
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Bus: location
	// --------------------------------------------------------------------------------------------

	@Subscribe
	@SuppressWarnings("ucd")
	public void onLocationTimeout(final LocationTimeoutEvent event) {
		Aircandi.stopwatch2.stop("Location fix attempt aborted: timeout");
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Logger.d(getSherlockActivity(), "Location fix attempt aborted: timeout: ** done **");
				if (LocationManager.getInstance().getLocationLocked() == null) {
					/* We only show toast if we timeout without getting any location fix */
					UI.showToastNotification(getString(R.string.error_location_poor), Toast.LENGTH_SHORT);
				}
				hideBusy();
				mPullToRefreshAttacher.setRefreshComplete();
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onLocationChanged(final LocationChangedEvent event) {

		getSherlockActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {

				final Location locationCandidate = event.location;
				if (locationCandidate != null) {

					final Location locationLocked = LocationManager.getInstance().getLocationLocked();

					if (locationLocked != null) {

						/* We never go from gps provider to network provider */
						if (locationLocked.getProvider().equals("gps") && locationCandidate.getProvider().equals("network")) {
							return;
						}

						/* If gps provider or same provider then look for improved accuracy */
						if (locationLocked.getProvider().equals(locationCandidate.getProvider())) {
							final float accuracyImprovement = locationLocked.getAccuracy() / locationCandidate.getAccuracy();
							boolean isSignificantlyMoreAccurate = (accuracyImprovement >= 1.5);
							if (!isSignificantlyMoreAccurate) {
								return;
							}
						}
					}

					LocationManager.getInstance().setLocationLocked(locationCandidate);
					mBusyManager.updateAccuracyIndicator();
					BusProvider.getInstance().post(new LocationLockedEvent(LocationManager.getInstance().getLocationLocked()));

					if (Aircandi.stopwatch2.isStarted()) {
						Aircandi.stopwatch2.stop("Location acquired");
						Tracker.sendTiming("location", Aircandi.stopwatch2.getTotalTimeMills(), "location_acquired", null, Aircandi.getInstance().getUser());
					}

					final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
					if (location != null) {

						mBusyManager.showBusy();

						new AsyncTask() {

							@Override
							protected Object doInBackground(Object... params) {

								Logger.d(getSherlockActivity(), "Location changed event: getting places near location");
								Thread.currentThread().setName("GetPlacesNearLocation");
								Aircandi.stopwatch2.start("Get places near location");

								final ServiceResponse serviceResponse = ProximityManager.getInstance().getEntitiesNearLocation(location);
								return serviceResponse;
							}

							@Override
							protected void onPostExecute(Object result) {
								final ServiceResponse serviceResponse = (ServiceResponse) result;
								if (serviceResponse.responseCode != ResponseCode.Success) {
									Routing.serviceError(getSherlockActivity(), serviceResponse);
									onError();
								}
							}

						}.execute();
					}
					else {
						hideBusy();
					}
				}
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onPlacesNearLocationFinished(final PlacesNearLocationFinishedEvent event) {
		Aircandi.stopwatch2.stop("Places near location complete");
		Tracker.sendTiming("radar", Aircandi.stopwatch2.getTotalTimeMills(), "places_near_location", null, Aircandi.getInstance().getUser());
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Logger.d(getSherlockActivity(), "Places near location finished event: ** done **");
				hideBusy();
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Bus: general
	// --------------------------------------------------------------------------------------------

	@Subscribe
	@SuppressWarnings("ucd")
	public void onEntitiesChanged(final EntitiesChangedEvent event) {

		getSherlockActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Logger.d(getSherlockActivity(), "Entities changed event: updating radar");
				Aircandi.stopwatch1.segmentTime("Entities changed: start radar display");
				Aircandi.stopwatch3.stop("Aircandi initialization finished and got first entities");
				
				synchronize();

				/* Point radar adapter at the updated entities */
				final int previousCount = mRadarAdapter.getCount();
				final List<Entity> entities = event.entities;
				mRadarAdapter.setItems(entities);
				mRadarAdapter.notifyDataSetChanged();
				Aircandi.stopwatch1.stop("Search for places by beacon complete");

				/* Add some sparkle */
				if (previousCount == 0 && entities.size() > 0) {
					if (Aircandi.settings.getBoolean(Constants.PREF_SOUND_EFFECTS, Constants.PREF_SOUND_EFFECTS_DEFAULT)) {
						new AsyncTask() {

							@Override
							protected Object doInBackground(Object... params) {
								Thread.currentThread().setName("PlaySound");
								mSoundPool.play(mNewCandiSoundId, 0.2f, 0.2f, 0, 0, 1f);
								return null;
							}

						}.execute();
					}
				}
			}
		});

	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onLocationLocked(LocationLockedEvent event) {
		updateDevIndicator(null, event.location);
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onWifiScanReceived(MonitoringWifiScanReceivedEvent event) {
		updateDevIndicator(event.wifiList, null);
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
		/*
		 * Refreshes radar so newly created place can pop in.
		 */
		if (event.notification.entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			getSherlockActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onRefresh();
				}
			});
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onRefreshStarted(View view) {
		onRefresh();
	}

	@Override
	public void onRefresh() {
		/*
		 * This only gets called by a user clicking the refresh button.
		 */
		Logger.d(getSherlockActivity(), "Starting refresh");
		Tracker.sendEvent("ui_action", "refresh_radar", null, 0, Aircandi.getInstance().getUser());
		searchForPlaces();
	}

	@Override
	public void onAdd() {
		if (Aircandi.getInstance().getUser() != null) {
			Routing.route(getSherlockActivity(), Route.New, null, Constants.SCHEMA_ENTITY_PLACE, null);
		}
	}

	@Override
	public void onHelp() {
		Bundle extras = new Bundle();
		extras.putInt(Constants.EXTRA_HELP_ID, R.layout.radar_help);
		Routing.route(getSherlockActivity(), Route.Help, null, null, extras);
	}

	@Override
	public void onError() {
		/*
		 * Location updates can trigger service calls. Gets restarted
		 * when the user manually triggers a refresh.
		 */
		LocationManager.getInstance().stopLocationBurst();

		/* Kill busy */
		mPullToRefreshAttacher.setRefreshComplete();
		hideBusy();
		hideBusy();
	}

	@Override
	public void showBusy() {
		super.showBusy();
	}

	@Override
	public void hideBusy() {
		super.hideBusy();
		mPullToRefreshAttacher.setRefreshing(false);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	private void searchForPlaces() {

		/* We won't perform a search if location access is disabled */
		if (!LocationManager.getInstance().isLocationAccessEnabled()) {
			Routing.route(getSherlockActivity(), Route.SettingsLocation);
		}
		else {
			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mPullToRefreshAttacher.setRefreshing(true);
					showBusy();
				}

				@Override
				protected Object doInBackground(Object... params) {
					/*
					 * We have special handling for connected state because search triggers a wifi scan which
					 * which in turn seems to have its own need to access the network. Their logic works for quite
					 * awhile before giving up.
					 * 
					 */
					ConnectedState connectedState = NetworkManager.getInstance().checkConnectedState();
					if (connectedState == ConnectedState.Normal) {
						searchForPlacesByBeacon();

						/* We give the beacon query a bit of a head start */
						mHandler.postDelayed(new Runnable() {

							@Override
							public void run() {
								searchForPlacesByLocation();
							}
						}, 500);
					}
					return connectedState;
				}

				@Override
				protected void onPostExecute(Object result) {
					ConnectedState connectedState = (ConnectedState) result;
					
					if (connectedState != ConnectedState.Normal) {
						if (Aircandi.stopwatch3.isStarted()) {
							Aircandi.stopwatch3.stop("Aircandi initialization finished: network problem");
						}
						hideBusy();
						mPullToRefreshAttacher.setRefreshing(false);

						if (connectedState == ConnectedState.WalledGarden) {
							Dialogs.alertDialogSimple(getSherlockActivity(), null, getString(R.string.error_connection_walled_garden));
						}
						else if (connectedState == ConnectedState.None) {
							Dialogs.alertDialogSimple(getSherlockActivity(), null, getString(R.string.error_connection_none));
						}
					}
				}
			}.execute();
		}
	}

	private void searchForPlacesByBeacon() {
		Aircandi.stopwatch1.start("Search for places by beacon");
		mEntityModelWifiState = NetworkManager.getInstance().getWifiState();
		if (NetworkManager.getInstance().isWifiEnabled()) {
			EntityManager.getEntityCache().removeEntities(Constants.SCHEMA_ENTITY_BEACON, null, null);
			ProximityManager.getInstance().scanForWifi(ScanReason.query);
		}
	}

	private void searchForPlacesByLocation() {
		Aircandi.stopwatch2.start("Lock location");
		LocationManager.getInstance().setLocationLocked(null);
		LocationManager.getInstance().lockLocationBurst();

	}

	@SuppressWarnings("ucd")
	public void startScanService(int scanInterval) {

		/* Start first scan right away */
		Logger.d(this, "Starting wifi scan service");
		final Intent scanIntent = new Intent(Aircandi.applicationContext, ScanService.class);
		getSherlockActivity().startService(scanIntent);

		/* Setup a scanning schedule */
		if (scanInterval > 0) {
			final AlarmManager alarmManager = (AlarmManager) getSherlockActivity().getSystemService(Service.ALARM_SERVICE);
			final PendingIntent pendingIntent = PendingIntent.getService(Aircandi.applicationContext, 0, scanIntent, 0);
			alarmManager.cancel(pendingIntent);
			alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP
					, SystemClock.elapsedRealtime() + scanInterval
					, scanInterval, pendingIntent);
		}
	}

	@SuppressWarnings("ucd")
	public void stopScanService() {
		final AlarmManager alarmManager = (AlarmManager) getSherlockActivity().getSystemService(Service.ALARM_SERVICE);
		final Intent scanIntent = new Intent(Aircandi.applicationContext, ScanService.class);
		final PendingIntent pendingIntent = PendingIntent.getService(Aircandi.applicationContext, 0, scanIntent, 0);
		alarmManager.cancel(pendingIntent);
		Logger.d(this, "Stopped wifi scan service");
	}

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

	private void doBeaconIndicatorClick() {
		if (mBeaconIndicator != null) {
			final StringBuilder beaconMessage = new StringBuilder(500);
			List<WifiScanResult> wifiList = ProximityManager.getInstance().getWifiList();
			synchronized (wifiList) {
				if (Aircandi.getInstance().getUser() != null
						&& Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
						&& Aircandi.getInstance().getUser().developer != null
						&& Aircandi.getInstance().getUser().developer) {
					if (Aircandi.wifiCount > 0) {
						for (WifiScanResult wifi : wifiList) {
							if (!wifi.SSID.equals("candi_feed")) {
								beaconMessage.append(wifi.SSID + ": (" + String.valueOf(wifi.level) + ") " + wifi.BSSID + System.getProperty("line.separator"));
							}
						}
						beaconMessage.append(System.getProperty("line.separator"));
						beaconMessage.append("Wifi fix: " + DateTime.interval(ProximityManager.getInstance().mLastWifiUpdate.getTime(), DateTime.nowDate().getTime(), IntervalContext.past));
					}

					final Location location = LocationManager.getInstance().getLocationLocked();
					if (location != null) {
						final Date fixDate = new Date(location.getTime());
						beaconMessage.append(System.getProperty("line.separator") + "Location fix: " + DateTime.interval(fixDate.getTime(), DateTime.nowDate().getTime(), IntervalContext.past));
						beaconMessage.append(System.getProperty("line.separator") + "Location accuracy: " + String.valueOf(location.getAccuracy()));
						beaconMessage.append(System.getProperty("line.separator") + "Location provider: " + location.getProvider());
					}
					else {
						beaconMessage.append(System.getProperty("line.separator") + "Location fix: none");
					}
				}
				else {
					return;
				}
			}
			Dialogs.alertDialog(R.drawable.ic_launcher
					, getString(R.string.alert_beacons_title)
					, beaconMessage.toString()
					, null
					, getSherlockActivity()
					, android.R.string.ok
					, null
					, null
					, new
					DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {}
					}, null);
		}
	}

	public void updateDevIndicator(final List<WifiScanResult> scanList, Location location) {

		if (mBeaconIndicator == null) return;

		if (scanList != null) {

			synchronized (scanList) {
				/*
				 * In case we get called from a background thread.
				 */
				getSherlockActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {

						WifiScanResult wifiStrongest = null;
						int wifiCount = 0;
						for (WifiScanResult wifi : scanList) {
							wifiCount++;
							if (wifiStrongest == null) {
								wifiStrongest = wifi;
							}
							else if (wifi.level > wifiStrongest.level) {
								wifiStrongest = wifi;
							}
						}

						Aircandi.wifiCount = wifiCount;
						mDebugWifi = String.valueOf(wifiCount);
					}
				});

			}
		}

		if (location != null) {
			Location locationLocked = LocationManager.getInstance().getLocationLocked();
			if (locationLocked != null) {
				if (location.getProvider().equals(locationLocked.getProvider()) && (int) location.getAccuracy() == (int) locationLocked.getAccuracy()) {
					mBeaconIndicator.setTextColor(Aircandi.getInstance().getResources().getColor(R.color.accent_blue));
				}
				else {
					if (((BaseActivity) getSherlockActivity()).getThemeTone().equals("dark")) {
						mBeaconIndicator.setTextColor(Aircandi.getInstance().getResources().getColor(R.color.text_dark));
					}
					else {
						mBeaconIndicator.setTextColor(Aircandi.getInstance().getResources().getColor(R.color.text_light));
					}
				}
			}

			String debugLocation = location.getProvider().substring(0, 1).toUpperCase(Locale.ROOT);
			if (location.hasAccuracy()) {
				debugLocation += String.valueOf((int) location.getAccuracy());
			}
			else {
				debugLocation += "--";
			}
			mDebugLocation = debugLocation;
		}

		mBeaconIndicator.setText(mDebugWifi + ":" + mDebugLocation);
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------	

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		/*
		 * Setup menu items local to radar.
		 */
		mMenuItemBeacons = menu.findItem(R.id.beacons);
		if (mMenuItemBeacons != null) {

			/* Only show beacon indicator if user is a developer */
			if (!Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
					|| Aircandi.getInstance().getUser() == null
					|| Aircandi.getInstance().getUser().developer == null
					|| !Aircandi.getInstance().getUser().developer) {
				mMenuItemBeacons.setVisible(false);
			}
			else {
				mBeaconIndicator = (TextView) mMenuItemBeacons.getActionView().findViewById(R.id.beacon_indicator);
				mMenuItemBeacons.getActionView().findViewById(R.id.beacon_frame).setTag(mMenuItemBeacons);
				mMenuItemBeacons.getActionView().findViewById(R.id.beacon_frame).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						MenuItem item = (MenuItem) view.getTag();
						onOptionsItemSelected(item);
					}
				});
			}
		}

		MenuItem refresh = menu.findItem(R.id.refresh);
		if (refresh != null) {
			if (mBusyManager != null) {
				mBusyManager.setAccuracyIndicator(refresh.getActionView().findViewById(R.id.accuracy_indicator));
				mBusyManager.updateAccuracyIndicator();
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*
		 * The action bar home/up action should open or close the drawer.
		 * ActionBarDrawerToggle will take care of this.
		 */
		if (item.getItemId() == R.id.beacons) {
			doBeaconIndicatorClick();
			return true;
		}
		else if (item.getItemId() == R.id.add) {
			onAdd();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	public void onStart() {
		/*
		 * Called everytime the fragment is started or restarted.
		 */
		super.onStart();
		/*
		 * Check for location service everytime we start.
		 */
		if (!LocationManager.getInstance().isLocationAccessEnabled()) {
			/* We won't continue if location services are disabled */
			Routing.route(getSherlockActivity(), Route.SettingsLocation);
			getSherlockActivity().finish();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (getSherlockActivity().isFinishing()) return;

		if (Aircandi.getInstance().getUser() != null
				&& Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
				&& Aircandi.getInstance().getUser().developer != null
				&& Aircandi.getInstance().getUser().developer) {
			startScanService(Constants.INTERVAL_SCAN_WIFI);
		}

		databind(BindingMode.auto);
	}

	@Override
	public void onPause() {
		stopScanService();
		super.onPause();
	}

	@Override
	public void onStop() {
		/*
		 * Fired when fragment is being deactivated.
		 */

		/*
		 * Stop any location burst that might be active unless
		 * this activity is being restarted. We do this because there
		 * is a race condition that can stop location burst after it
		 * has been started by the reload.
		 */
		LocationManager.getInstance().stopLocationBurst();

		/* Kill busy */
		mPullToRefreshAttacher.setRefreshComplete();
		hideBusy();
		super.onStop();
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.radar_fragment;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	private class RadarListAdapter extends ArrayAdapter<Entity> {

		private final LayoutInflater	mInflater;
		private final Integer			mItemLayoutId	= R.layout.temp_listitem_radar;
		private List<Entity>			mItems;

		public RadarListAdapter(Context context, List<Entity> entities) {
			super(context, 0, entities);
			mItems = entities;
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final RadarViewHolder holder;
			final Entity itemData = mItems.get(position);
			Logger.v(this, "getView: position = " + String.valueOf(position) + " name = " + itemData.name);

			if (view == null) {
				view = mInflater.inflate(mItemLayoutId, null);
				holder = new RadarViewHolder();
				holder.candiView = (CandiView) view.findViewById(R.id.candi_view);
				/* Need this line so clicks bubble up to the listview click handler */
				holder.candiView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
				view.setTag(holder);
			}
			else {
				holder = (RadarViewHolder) view.getTag();
			}

			if (itemData != null) {
				final Place entity = (Place) itemData;
				holder.candiView.databind(entity);
			}
			return view;
		}

		@Override
		public Entity getItem(int position) {
			return mItems.get(position);
		}

		@Override
		public int getCount() {
			return mItems.size();
		}

		public List<Entity> getItems() {
			return mItems;
		}

		public void setItems(List<Entity> items) {
			mItems = items;
		}

		@SuppressWarnings("ucd")
		private class RadarViewHolder {
			private CandiView	candiView;
		}
	}

}