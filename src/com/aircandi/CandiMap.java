package com.aircandi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;

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
import com.aircandi.components.GeoLocationManager.BaseLocationListener;
import com.aircandi.components.GeoLocationManager.LocationSensorOptions;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Beacon;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

@SuppressWarnings("unused")
public class CandiMap extends SherlockMapActivity {

	protected AircandiCommon		mCommon;
	private CandiMapView			mMapView		= null;
	private MapController			mMapController	= null;
	private MyLocationOverlay		mMyLocationOverlay;
	private List<Overlay>			mMapOverlays;
	private CandiItemizedOverlay	mBeaconOverlay;
	private Boolean					mBeaconsLoaded	= false;

	private BaseLocationListener	mLocationListener;
	private LocationSensorOptions	mLocationSensorOptions;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		if (!Aircandi.getInstance().getLaunchedFromRadar()) {
			/* Try to detect case where this is being created after a crash and bail out. */
			super.onCreate(savedInstanceState);
			finish();
		}
		else {
			mCommon = new AircandiCommon(this, savedInstanceState);
			mCommon.setTheme(null, false);
			mCommon.unpackIntent();
			setContentView(getLayoutId());
			super.onCreate(savedInstanceState);
			mCommon.initialize();

			initialize();
		}
	}

	@SuppressWarnings("deprecation")
	private void initialize() {

		/* Get location support setup */
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		GeoLocationManager.getInstance().ensureLocation(GeoLocationManager.MINIMUM_ACCURACY
				, GeoLocationManager.MAXIMUM_AGE
				, criteria, new RequestListener(){

					@Override
					public void onComplete(Object response) {
						Location location = (Location) response;
						if (location != null) {
							bind(true, location);
						}
						
					}});

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

	private void bind(final Boolean refresh, final Location location) {

		if (location == null) {
			mCommon.showAlertDialogSimple(null, getString(R.string.alert_location_unavailable));
		}
		else {

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showProgressDialog(getString(R.string.progress_loading), true);
				}

				@Override
				protected Object doInBackground(Object... params) {
					/*
					 * The fetched beacons are loaded into the beacon cache by the call.
					 */
					ModelResult result = ProxiExplorer.getInstance().getEntityModel().getMapBeacons(location, refresh);
					return result;
				}

				@Override
				protected void onPostExecute(Object modelResult) {
					ModelResult result = (ModelResult) modelResult;
					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						((ViewGroup) findViewById(R.id.map_holder)).setVisibility(View.VISIBLE);
						mBeaconsLoaded = true;
						showBeacons((Collection<Beacon>) result.data);
						mCommon.hideProgressDialog();
					}
					else {
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.MapBrowse);
					}
				}

			}.execute();
		}
	}

	public void doRefresh() {
		bind(true, GeoLocationManager.getInstance().getLocation());
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

	public void showBeacons(Collection<Beacon> data) {

		mMapOverlays = mMapView.getOverlays();
		mMapOverlays.remove(mBeaconOverlay);
		Drawable marker = getResources().getDrawable(R.drawable.ic_map_candi_iii);

		List<MapBeacon> mapBeacons = new ArrayList<MapBeacon>();
		List<GeoPoint> geoPoints = new ArrayList<GeoPoint>();

		for (Beacon beaconByLocation : data) {
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
		if (mMyLocationOverlay != null) {
			mMyLocationOverlay.enableMyLocation();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mCommon.doPause();
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