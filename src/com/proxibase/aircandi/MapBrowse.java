package com.proxibase.aircandi;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.Query;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

public class MapBrowse extends MapActivity {

	protected AircandiCommon		mCommon;
	private List<Beacon>			mBeacons		= new ArrayList<Beacon>();
	private MapView					mMapView		= null;
	private MapController			mMapController	= null;
	private List<Overlay>			mMapOverlays;
	private CandiItemizedOverlay	mItemizedOverlay;

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
		bind();

		Tracker.trackPageView("/CandiMap");
	}

	private void initialize() {

		mMapView = new MapView(this, Aircandi.isDebugBuild(this) ? CandiConstants.GOOGLE_API_KEY_DEBUG : CandiConstants.GOOGLE_API_KEY_RELEASE);
		mMapView.setBuiltInZoomControls(true);
		mMapView.setReticleDrawMode(MapView.ReticleDrawMode.DRAW_RETICLE_OVER);
		mMapView.setClickable(true);
		mMapController = mMapView.getController();
		ViewGroup mapHolder = (ViewGroup) findViewById(R.id.map_holder);
		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		mapHolder.addView(mMapView, params);
		mMapView.postInvalidate();
	}

	private void bind() {
		/*
		 * This form is always for editing. We always reload the user to make sure
		 * we have the freshest data.
		 */
		if (mCommon.mBeaconId != null) {
			mBeacons.clear();
			Query query = new Query("Beacons").filter("Id eq '" + mCommon.mBeaconId + "'");

			ServiceResponse serviceResponse = NetworkManager.getInstance().request(
					new ServiceRequest(ProxiConstants.URL_AIRCANDI_SERVICE_ODATA, query, RequestType.Get, ResponseFormat.Json));

			if (serviceResponse.responseCode == ResponseCode.Success) {
				String jsonResponse = (String) serviceResponse.data;
				Beacon beacon = (Beacon) ProxibaseService.convertJsonToObject(jsonResponse, Beacon.class, GsonType.ProxibaseService);
				mBeacons.add(beacon);
				showCandi();
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onRefreshClick(View view) {}

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

		for (Beacon beacon : mBeacons) {
			GeoPoint point = new GeoPoint((int) (beacon.latitude * 1E6), (int) (beacon.longitude * 1E6));
			OverlayItem overlayitem = new OverlayItem(point, "Candi", "");
			mItemizedOverlay.addOverlay(overlayitem);
			zoomToGeoPoint(point);
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
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	protected int getLayoutId() {
		return R.layout.map_browse;
	}

	@Override
	protected boolean isLocationDisplayed() {
		return false;
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