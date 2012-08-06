package com.proxibase.aircandi;

import java.util.List;

import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;

import com.actionbarsherlock.app.SherlockMapActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.CandiItemizedOverlay;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.GeoLocation;
import com.proxibase.service.objects.ServiceData;

public class MapBrowse extends SherlockMapActivity {

	protected AircandiCommon		mCommon;
	private MapView					mMapView		= null;
	private MapController			mMapController	= null;
	private List<Overlay>			mMapOverlays;
	private CandiItemizedOverlay	mItemizedOverlay;
	private Entity					mEntity;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		mCommon = new AircandiCommon(this, savedInstanceState);
		mCommon.setTheme(false, false);
		mCommon.unpackIntent();

		setContentView(getLayoutId());
		super.onCreate(savedInstanceState);

		mCommon.initialize();

		initialize();
		bind();
	}

	@SuppressWarnings("deprecation")
	private void initialize() {

		mMapView = new MapView(this, BuildConfig.DEBUG ? CandiConstants.GOOGLE_API_KEY_DEBUG : CandiConstants.GOOGLE_API_KEY_RELEASE);
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
		if (mEntity == null && mCommon.mEntityId != null) {

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showProgressDialog(true, getString(R.string.progress_loading));
				}

				@Override
				protected Object doInBackground(Object... params) {
					String jsonEagerLoad = "{\"children\":false,\"parents\":false,\"comments\":false}";
					ServiceResponse serviceResponse = ProxiExplorer.getInstance().getEntity(mCommon.mEntityId, jsonEagerLoad, null, null);
					return serviceResponse;
				}

				@Override
				protected void onPostExecute(Object response) {
					ServiceResponse serviceResponse = (ServiceResponse) response;

					if (serviceResponse.responseCode == ResponseCode.Success) {
						mEntity = (Entity) ((ServiceData) serviceResponse.data).data;
						ViewGroup mapHolder = (ViewGroup) findViewById(R.id.map_holder);
						mapHolder.setVisibility(View.VISIBLE);
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
		mCommon.doProfileClick();
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

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

	public void showCandi() {
		mMapOverlays = mMapView.getOverlays();
		Drawable drawable = getResources().getDrawable(R.drawable.icon_map_candi_ii);
		mItemizedOverlay = new CandiItemizedOverlay(drawable, mMapView, true);

		GeoLocation entityLocation = mEntity.location != null ? mEntity.location : mCommon.mEntityLocation;
		GeoPoint geoPoint = new GeoPoint((int) (entityLocation.latitude.doubleValue() * 1E6), (int) (entityLocation.longitude.doubleValue() * 1E6));

		OverlayItem overlayItem = new OverlayItem(geoPoint, mEntity.title, mEntity.description);
		overlayItem.setMarker(drawable);
		mItemizedOverlay.addOverlay(overlayItem);
		zoomToGeoPoint(geoPoint);
		mMapOverlays.add(mItemizedOverlay);
		mMapView.invalidate();
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------
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