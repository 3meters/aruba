package com.aircandi;

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
import com.aircandi.components.CandiItemizedOverlay;
import com.aircandi.components.GeoLocationManager;
import com.aircandi.components.ImageUtils;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.ProxibaseService.GsonType;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.aircandi.BuildConfig;
import com.aircandi.R;

public class CandiMap extends SherlockMapActivity {

	protected AircandiCommon		mCommon;
	private MapView					mMapView		= null;
	private MapController			mMapController	= null;
	private MyLocationOverlay		mMyLocationOverlay;
	private List<Overlay>			mMapOverlays;
	private CandiItemizedOverlay	mItemizedOverlay;
	private LocationManager			mLocationManager;
	private LocationListener		mLocationListener;
	private static double			RADIUS_EARTH	= 6378000;	// meters
	private static double			SEARCH_RANGE	= 1000000;		// meters

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
				bind();
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void initialize() {

		mMapView = new MapView(this, BuildConfig.DEBUG ? CandiConstants.GOOGLE_API_KEY_DEBUG : CandiConstants.GOOGLE_API_KEY_RELEASE);
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

		mMapView.getOverlays().clear();
		mMapView.getOverlays().add(mMyLocationOverlay);
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

	private void bind() {

		/*
		 * If user is anonymous we will only get back candi that are flagged as public
		 */
		if (ProxiExplorer.getInstance().getEntityModel().getMapBeacons().size() == 0) {

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showProgressDialog(true, getString(R.string.progress_loading));
				}

				@Override
				protected Object doInBackground(Object... params) {

					Bundle parameters = new Bundle();
					Location location = GeoLocationManager.getInstance().getCurrentLocation();
					parameters.putDouble("latitude", location.getLatitude());
					parameters.putDouble("longitude", location.getLongitude());
					parameters.putDouble("radius", SEARCH_RANGE / RADIUS_EARTH); // to radians
					parameters.putString("userId", Aircandi.getInstance().getUser().id);

					ServiceRequest serviceRequest = new ServiceRequest();
					serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getBeaconsNearLocation");
					serviceRequest.setRequestType(RequestType.Method);
					serviceRequest.setParameters(parameters);
					serviceRequest.setResponseFormat(ResponseFormat.Json);

					ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

					if (serviceResponse.responseCode == ResponseCode.Success) {
						String jsonResponse = (String) serviceResponse.data;
						List<Beacon> beacons = (List<Beacon>) ProxibaseService.convertJsonToObjects(jsonResponse, Beacon.class, GsonType.ProxibaseService).data;
						ProxiExplorer.getInstance().getEntityModel().setMapBeacons(beacons);
					}
					return serviceResponse;
				}

				@Override
				protected void onPostExecute(Object result) {
					ServiceResponse serviceResponse = (ServiceResponse) result;
					if (serviceResponse.responseCode == ResponseCode.Success) {
						((ViewGroup) findViewById(R.id.map_holder)).setVisibility(View.VISIBLE);
						showBeacons();
						mCommon.showProgressDialog(false, null);
					}
					else {
						mCommon.handleServiceError(serviceResponse, ServiceOperation.MapBrowse);
					}

				}
			}.execute();
		}
		else {
			((ViewGroup) findViewById(R.id.map_holder)).setVisibility(View.VISIBLE);
			showBeacons();
		}
	}

	public void doRefresh() {
		bind();
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

	public void showCandi() {
		mMapOverlays = mMapView.getOverlays();
		Drawable drawable = getResources().getDrawable(R.drawable.icon_map_candi);
		mItemizedOverlay = new CandiItemizedOverlay(drawable, mMapView, false);

		for (Object entityPointObject : ProxiExplorer.getInstance().getEntityModel().getMapEntities()) {
			Entity entityPoint = (Entity) entityPointObject;
			if (entityPoint.location != null) {
				/*
				 * If there are entities to map that are attached to beacons we have scanned then
				 * we want to override the location info from the service with our fresher location info.
				 * Important because a beacon could have moved and this is the only way to show things
				 * correctly.
				 * 
				 * This will cause all the candi from scanned beacons to stack on the same location.
				 * 
				 * We might want to show map indicators for beacons with a candi count instead of
				 * for each candi.
				 */
				Beacon beacon = ProxiExplorer.getInstance().getEntityModel().getBeaconById(entityPoint.beaconId);
				if (beacon != null) {
					Location currentLocation = GeoLocationManager.getInstance().getCurrentLocation();
					entityPoint.location.latitude = currentLocation.getLatitude();
					entityPoint.location.longitude = currentLocation.getLongitude();
				}
				GeoPoint point = new GeoPoint((int) (entityPoint.location.latitude.doubleValue() * 1E6),
						(int) (entityPoint.location.longitude.doubleValue() * 1E6));
				OverlayItem overlayitem = new OverlayItem(point, entityPoint.label, entityPoint.label);
				overlayitem.setMarker(drawable);
				mItemizedOverlay.addOverlay(overlayitem);
			}
		}
		mMapOverlays.add(mItemizedOverlay);
		mMapView.invalidate();
	}

	public void showBeacons() {

		mMapOverlays = mMapView.getOverlays();
		Drawable drawable = getResources().getDrawable(R.drawable.icon_map_candi_ii);
		mItemizedOverlay = new CandiItemizedOverlay(drawable, mMapView, true);
		/*
		 * First check to see if radar is seeing a beacon that didn't come back
		 * in the service call.
		 */
		for (Beacon beaconByRadar : ProxiExplorer.getInstance().getEntityModel().getBeacons()) {
			if (beaconByRadar.entities != null && beaconByRadar.entities.size() > 0) {
				beaconByRadar.entityCount = beaconByRadar.entities.size();
				boolean beaconHit = false;
				for (Beacon beaconByLocation : ProxiExplorer.getInstance().getEntityModel().getMapBeacons()) {
					if (beaconByLocation.id.equals(beaconByRadar.id)) {
						beaconHit = true;
						break;
					}
				}
				if (!beaconHit) {
					ProxiExplorer.getInstance().getEntityModel().getMapBeacons().add(beaconByRadar);
				}
			}
		}

		for (Beacon beaconByLocation : ProxiExplorer.getInstance().getEntityModel().getMapBeacons()) {
			if (beaconByLocation.entityCount.intValue() > 0
					&& beaconByLocation.bssid != null
					&& !beaconByLocation.bssid.equals(ProxiExplorer.globalBssid)
					&& beaconByLocation.latitude != null
					&& beaconByLocation.longitude != null) {
				/*
				 * If a beacon is currently within radar detection range we want to
				 * override any lat/lon settings using the users current location.
				 * 
				 * Multiple beacons in radar range will stack on the same map location unless
				 * we do something to offset them.
				 */
				Beacon beaconByRadar = ProxiExplorer.getInstance().getEntityModel().getBeaconById(beaconByLocation.id);
				if (beaconByRadar != null) {
					Location currentLocation = GeoLocationManager.getInstance().getCurrentLocation();
					if (currentLocation != null) {
						beaconByLocation.latitude = currentLocation.getLatitude();
						beaconByLocation.longitude = currentLocation.getLongitude();
					}
				}
				GeoPoint point = new GeoPoint((int) (beaconByLocation.latitude.doubleValue() * 1E6),
						(int) (beaconByLocation.longitude.doubleValue() * 1E6));
				String title = String.valueOf(beaconByLocation.entityCount);
				String message = beaconByLocation.id;
				OverlayItem overlayItem = new OverlayItem(point, title, message);
				overlayItem.setMarker(drawable);
				mItemizedOverlay.addOverlay(overlayItem);
			}
		}
		mMapOverlays.add(mItemizedOverlay);
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

}