package com.proxibase.aircandi;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.FrameLayout.LayoutParams;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.proxibase.aircandi.Aircandi.CandiTask;
import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

public class CandiMap extends MapActivity {

	protected AircandiCommon		mCommon;
	private List<Object>			mBeacons		= new ArrayList<Object>();
	private MapView					mMapView		= null;
	private MapController			mMapController	= null;
	private MyLocationOverlay		mMyLocationOverlay;
	private List<Overlay>			mMapOverlays;
	private CandiItemizedOverlay	mItemizedOverlay;
	private LocationManager			mLocationManager;
	private LocationListener		mLocationListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		mCommon = new AircandiCommon(this);
		mCommon.setTheme();
		mCommon.unpackIntent();

		setContentView(getLayoutId());
		super.onCreate(savedInstanceState);

		mCommon.initialize();
		mCommon.initializeDialogs();

		initialize();
		initializeLocation();
		bind();

		Tracker.trackPageView("/CandiMap");
	}

	private void initialize() {

		mMapView = new MapView(this, "0NLNz2BjIntNt_gkO2_iEs5wIIxNGrG_pmHjPbA");
		mMapView.setBuiltInZoomControls(true);
		mMapView.setReticleDrawMode(MapView.ReticleDrawMode.DRAW_RETICLE_OVER);
		mMapView.setClickable(true);

		mMapController = mMapView.getController();

		mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
		mMyLocationOverlay.runOnFirstFix(new Runnable() {

			public void run() {
				mMapController.setZoom(13);
				mMapController.animateTo(mMyLocationOverlay.getMyLocation());
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
				if (Aircandi.isBetterLocation(location, Aircandi.getInstance().getCurrentLocation())) {
					Aircandi.getInstance().setCurrentLocation(location);
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
		if (Aircandi.isBetterLocation(lastKnownLocationNetwork, Aircandi.getInstance().getCurrentLocation())) {
			Aircandi.getInstance().setCurrentLocation(lastKnownLocationNetwork);
		}

		Location lastKnownLocationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (Aircandi.isBetterLocation(lastKnownLocationGPS, Aircandi.getInstance().getCurrentLocation())) {
			Aircandi.getInstance().setCurrentLocation(lastKnownLocationGPS);
		}
	}

	private void bind() {
		/*
		 * This form is always for editing. We always reload the user to make sure
		 * we have the freshest data.
		 */
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, "Loading...");
			}

			@Override
			protected Object doInBackground(Object... params) {

				Bundle parameters = new Bundle();
				parameters.putDouble("latitude", Aircandi.getInstance().getCurrentLocation().getLatitude());
				parameters.putDouble("longitude", Aircandi.getInstance().getCurrentLocation().getLongitude());
				parameters.putDouble("radiusInKilometers", 1d);

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE + "GetBeaconsNearLocation");
				serviceRequest.setRequestType(RequestType.Method);
				serviceRequest.setParameters(parameters);
				serviceRequest.setResponseFormat(ResponseFormat.Json);

				ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				if (serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) serviceResponse.data;
					mBeacons = ProxibaseService.convertJsonToObjects(jsonResponse, Beacon.class, GsonType.ProxibaseService);
				}
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					showCandi();
				}
				mCommon.showProgressDialog(false, null);
				mCommon.stopTitlebarProgress();
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onRefreshClick(View view) {
		bind();
	}

	public void onTabClick(View view) {
		mCommon.setActiveTab(view);
		if (view.getTag().equals("radar")) {
			Aircandi.getInstance().setCandiTask(CandiTask.RadarCandi);
			Intent intent = new Intent(this, CandiRadar.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			overridePendingTransition(R.anim.fade_in_short, R.anim.fade_out_short);
		}
		else if (view.getTag().equals("mycandi")) {
			Aircandi.getInstance().setCandiTask(CandiTask.MyCandi);
			IntentBuilder intentBuilder = new IntentBuilder(this, CandiList.class);
			Intent intent = intentBuilder.create();
			startActivity(intent);
			overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
		}
		else if (view.getTag().equals("map")) {
			Aircandi.getInstance().setCandiTask(CandiTask.Map);
			IntentBuilder intentBuilder = new IntentBuilder(this, CandiMap.class);
			Intent intent = intentBuilder.create();
			startActivity(intent);
			overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean rebind = mCommon.doOptionsItemSelected(item);
		if (rebind) {
		}
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

	public void showCandi() {
		mMapOverlays = mMapView.getOverlays();
		Drawable drawable = getResources().getDrawable(R.drawable.icon_map_candi);
		mItemizedOverlay = new CandiItemizedOverlay(drawable);

		for (Object beaconObject : mBeacons) {
			Beacon beacon = (Beacon) beaconObject;
			GeoPoint point = new GeoPoint((int) (beacon.latitude * 1E6), (int) (beacon.longitude * 1E6));
			OverlayItem overlayitem = new OverlayItem(point, "Candi", "");
			mItemizedOverlay.addOverlay(overlayitem);
		}
		mMapOverlays.add(mItemizedOverlay);
		mMapView.invalidate();
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------
	@Override
	protected void onResume() {
		super.onResume();
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
		if (mLocationManager != null) {
			mLocationManager.removeUpdates(mLocationListener);
		}
		if (mMyLocationOverlay != null) {
			mMyLocationOverlay.disableMyLocation();
		}
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

	public class CandiItemizedOverlay extends ItemizedOverlay {

		private ArrayList<OverlayItem>	mOverlays	= new ArrayList<OverlayItem>();

		public CandiItemizedOverlay(Drawable defaultMarker) {
			super(boundCenterBottom(defaultMarker));
		}

		@Override
		protected OverlayItem createItem(int i) {
			return mOverlays.get(i);
		}

		@Override
		public int size() {
			return mOverlays.size();
		}

		public void addOverlay(OverlayItem overlay) {
			mOverlays.add(overlay);
			populate();
		}
	}
}