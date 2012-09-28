package com.aircandi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockMapActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AnimUtils;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.CandiItemizedOverlay;
import com.aircandi.components.CandiMapView;
import com.aircandi.components.GeoLocationManager;
import com.aircandi.components.ImageUtils;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.objects.Beacon;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class CandiMap extends SherlockMapActivity {

	protected AircandiCommon		mCommon;
	private CandiMapView			mMapView		= null;
	private MapController			mMapController	= null;
	private MyLocationOverlay		mMyLocationOverlay;
	private List<Overlay>			mMapOverlays;
	private CandiItemizedOverlay	mBeaconOverlay;
	private LocationManager			mLocationManager;
	private LocationListener		mLocationListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		if (!Aircandi.getInstance().getLaunchedFromRadar()) {
			/* Try to detect case where this is being created after a crash and bail out. */
			super.onCreate(savedInstanceState);
			finish();
		}
		else {
			mCommon = new AircandiCommon(this, savedInstanceState);
			mCommon.setTheme(false, false);
			mCommon.unpackIntent();

			setContentView(getLayoutId());
			super.onCreate(savedInstanceState);

			mCommon.initialize();

			initialize();
			initializeLocation();

			if (GeoLocationManager.getInstance().getCurrentLocation() != null) {
				bind(true);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void initialize() {

		mMapView = new CandiMapView(this, BuildConfig.DEBUG ? CandiConstants.GOOGLE_API_KEY_DEBUG : CandiConstants.GOOGLE_API_KEY_RELEASE);
		mMapView.setBuiltInZoomControls(true);
		mMapView.setReticleDrawMode(MapView.ReticleDrawMode.DRAW_RETICLE_OVER);
		mMapView.setSatellite(false);
		mMapView.setClickable(true);

		mMapController = mMapView.getController();

		mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
		mMyLocationOverlay.runOnFirstFix(new Runnable() {

			public void run() {
				mMapController.animateTo(mMyLocationOverlay.getMyLocation());
				mMapController.setZoom(16);
			}
		});

		mMapView.getOverlays().add(mMyLocationOverlay);

		/* Add map to layout */
		ViewGroup mapHolder = (ViewGroup) findViewById(R.id.map_holder);
		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		mapHolder.addView(mMapView, params);

		mMapView.postInvalidate();
	}

	private void initializeLocation() {
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mLocationListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				if (GeoLocationManager.isBetterLocation(location, GeoLocationManager.getInstance().getCurrentLocation())) {
					GeoLocationManager.getInstance().setCurrentLocation(location);
				}
			}

			@Override
			public void onProviderDisabled(String provider) {
				ImageUtils.showToastNotification(provider + ": disabled", Toast.LENGTH_SHORT);
			}

			@Override
			public void onProviderEnabled(String provider) {
				ImageUtils.showToastNotification(provider + ": enabled", Toast.LENGTH_SHORT);
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				if (status == LocationProvider.AVAILABLE) {
					ImageUtils.showToastNotification(provider + ": available", Toast.LENGTH_SHORT);
				}
				else if (status == LocationProvider.OUT_OF_SERVICE) {
					ImageUtils.showToastNotification(provider + ": out of service", Toast.LENGTH_SHORT);
				}
				else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
					ImageUtils.showToastNotification(provider + ": temporarily unavailable", Toast.LENGTH_SHORT);
				}
			}
		};

		Location lastKnownLocationNetwork = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (lastKnownLocationNetwork != null) {
			if (GeoLocationManager.isBetterLocation(lastKnownLocationNetwork, GeoLocationManager.getInstance().getCurrentLocation())) {
				GeoLocationManager.getInstance().setCurrentLocation(lastKnownLocationNetwork);
			}
		}

		Location lastKnownLocationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (lastKnownLocationGPS != null) {
			if (GeoLocationManager.isBetterLocation(lastKnownLocationGPS, GeoLocationManager.getInstance().getCurrentLocation())) {
				GeoLocationManager.getInstance().setCurrentLocation(lastKnownLocationGPS);
			}
		}
	}

	private void bind(final Boolean refresh) {

		/*
		 * If user is anonymous we will only get back candi that are flagged as public
		 */

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, getString(R.string.progress_loading));
			}

			@Override
			protected Object doInBackground(Object... params) {

				Location location = GeoLocationManager.getInstance().getCurrentLocation();
				ModelResult result = ProxiExplorer.getInstance().getEntityModel().getMapBeacons(location, refresh);
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				ModelResult result = (ModelResult) modelResult;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					((ViewGroup) findViewById(R.id.map_holder)).setVisibility(View.VISIBLE);
					showBeacons();
					mCommon.showProgressDialog(false, null);
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.MapBrowse);
				}

			}
		
		}.execute();
	}

	public void doRefresh() {
		bind(true);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiMapToCandiPage);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Logger.d(this, "Configuration changed");
		super.onConfigurationChanged(newConfig);
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	public boolean onCreateOptionsMenu(Menu menu) {
		/*
		 * These are here instead of common because CandiMap does not extend CandiActivity
		 */
		mCommon.doCreateOptionsMenu(menu);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		mCommon.doPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mCommon.doOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	public void zoomToCurrentLocation(Location location) {
		GeoPoint point = new GeoPoint((int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));
		mMapController.animateTo(point);
		mMapController.setZoom(16);
		mMapView.invalidate();
	}

	public void zoomToGeoPoint(GeoPoint geoPoint) {
		mMapController.animateTo(geoPoint);
		mMapController.setZoom(15);
		mMapView.invalidate();
	}

	public void moveToGeoPoint(GeoPoint geoPoint) {
		mMapController.animateTo(geoPoint);
		mMapView.invalidate();
	}

	public void showBeacons() {

		mMapOverlays = mMapView.getOverlays();
		mMapOverlays.remove(mBeaconOverlay);
		Drawable marker = getResources().getDrawable(R.drawable.icon_map_candi_iii);

		List<MapBeacon> mapBeacons = new ArrayList<MapBeacon>();
		List<GeoPoint> geoPoints = new ArrayList<GeoPoint>();

		for (Beacon beaconByLocation : ProxiExplorer.getInstance().getEntityModel().getBeacons()) {
			if (beaconByLocation.entityCount != null 
					&& beaconByLocation.entityCount.intValue() > 0
					&& beaconByLocation.bssid != null
					&& !beaconByLocation.bssid.equals(ProxiExplorer.mWifiGlobal.BSSID)
					&& beaconByLocation.latitude != null
					&& beaconByLocation.longitude != null) {
				MapBeacon mapBeacon = new MapBeacon();
				mapBeacon.point = new GeoPoint((int) (beaconByLocation.latitude.doubleValue() * 1E6)
						, (int) (beaconByLocation.longitude.doubleValue() * 1E6));
				mapBeacon.title = beaconByLocation.label;
				mapBeacon.message = beaconByLocation.id;
				mapBeacons.add(mapBeacon);
				geoPoints.add(mapBeacon.point);
			}
		}

		/*
		 * Create overlays
		 */
		mBeaconOverlay = new CandiItemizedOverlay(mapBeacons, geoPoints, marker, mMapView);
		Collections.sort(mapBeacons, new CandiItemizedOverlay.SortMapBeaconsByLatitude());
		for (MapBeacon mapBeacon : mapBeacons) {
			OverlayItem overlayItem = new OverlayItem(mapBeacon.point, mapBeacon.title, mapBeacon.message);
			overlayItem.setMarker(marker);
			mBeaconOverlay.addOverlay(overlayItem);
		}
		mBeaconOverlay.doPopulate();
		mMapOverlays.add(mBeaconOverlay);
		mMapView.invalidate();
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@SuppressLint("NewApi")
	@Override
	protected void onRestart() {
		Logger.d(this, "CandiActivity restarting");
		super.onRestart();

		if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, CandiConstants.THEME_DEFAULT))) {
			Logger.d(this, "Restarting because of theme change");
			mCommon.mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, CandiConstants.THEME_DEFAULT);
			mCommon.reload();
		}
		else {

			/* Make sure the right tab is active */
			mCommon.setActiveTab(2);

			/* User could have changed */
			/* Make sure onPrepareOptionsMenu gets called (since api 11) */
			((SherlockMapActivity) this).invalidateOptionsMenu();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mCommon.doResume();
		if (mLocationManager != null) {
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, mLocationListener);
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, mLocationListener);
		}
		if (mMyLocationOverlay != null) {
			mMyLocationOverlay.enableMyLocation();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mCommon.doPause();
		if (mLocationManager != null) {
			mLocationManager.removeUpdates(mLocationListener);
		}
		if (mMyLocationOverlay != null) {
			mMyLocationOverlay.disableMyLocation();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		mCommon.doStop();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mCommon.doStart();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	protected int getLayoutId() {
		return R.layout.candi_map;
	}

	@Override
	protected boolean isLocationDisplayed() {
		return mMyLocationOverlay.isMyLocationEnabled();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public class MapBeacon {
		public GeoPoint	point;
		public String	title;
		public String	message;
	}
}