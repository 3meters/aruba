package com.proxibase.aircandi;

import java.util.List;

import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.proxibase.aircandi.CandiMap.CandiItemizedOverlay;
import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.sdk.android.proxi.consumer.Entity;

public class MapBrowse extends MapActivity {

	protected AircandiCommon		mCommon;
	private MapView					mMapView		= null;
	private MapController			mMapController	= null;
	private List<Overlay>			mMapOverlays;
	private CandiItemizedOverlay	mItemizedOverlay;
	private GeoPoint				mGeoPoint;
	private String					mTitle;
	private String					mDescription;

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
		mMapView.setSatellite(false);
		mMapView.setStreetView(false);
		mMapView.setClickable(true);
		mMapController = mMapView.getController();
		ViewGroup mapHolder = (ViewGroup) findViewById(R.id.map_holder);
		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		mapHolder.addView(mMapView, params);
		mMapView.postInvalidate();
	}

	private void bind() {
		if (mCommon.mEntity == null && mCommon.mEntityId != null) {

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showProgressDialog(true, "Loading...");
				}

				@Override
				protected Object doInBackground(Object... params) {
					ServiceResponse serviceResponse = ProxiExplorer.getInstance().getEntityFromService(mCommon.mEntityId, false);
					return serviceResponse;
				}

				@Override
				protected void onPostExecute(Object response) {
					ServiceResponse serviceResponse = (ServiceResponse) response;

					if (serviceResponse.responseCode == ResponseCode.Success) {
						mCommon.mEntity = (Entity) serviceResponse.data;
						mGeoPoint = new GeoPoint((int) (mCommon.mEntity.drops.get(0).latitude.doubleValue() * 1E6), (int) (mCommon.mEntity.drops.get(0).longitude.doubleValue() * 1E6));
						mTitle = mCommon.mEntity.label;
						mDescription = mCommon.mEntity.description;
						((ViewGroup) findViewById(R.id.map_holder)).setVisibility(View.VISIBLE);
						showCandi();
						mCommon.showProgressDialog(false, null);
						mCommon.stopTitlebarProgress();						
					}
					else {
						mCommon.handleServiceError(serviceResponse);
					}
				}
			}.execute();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onRefreshClick(View view) {}

	public void onProfileClick(View view) {
		mCommon.doProfileClick(view);
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
		mItemizedOverlay = new CandiItemizedOverlay(drawable, true);
		OverlayItem overlayitem = new OverlayItem(mGeoPoint, mTitle, mDescription);
		mItemizedOverlay.addOverlay(overlayitem);
		zoomToGeoPoint(mGeoPoint);
		mMapOverlays.add(mItemizedOverlay);
		mMapView.invalidate();
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

}