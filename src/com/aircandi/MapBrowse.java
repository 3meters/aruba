package com.aircandi;

import java.util.List;

import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;

import com.actionbarsherlock.app.SherlockMapActivity;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.CandiItemizedOverlay;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.GeoLocation;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

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
		mCommon.setTheme(null, false);
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
		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		mapHolder.addView(mMapView, params);
		mMapView.postInvalidate();
	}

	private void bind() {
		ViewGroup mapHolder = (ViewGroup) findViewById(R.id.map_holder);
		mapHolder.setVisibility(View.VISIBLE);
		showCandi();
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
		Drawable drawable = getResources().getDrawable(R.drawable.ic_map_candi_iii);

		GeoLocation entityLocation = new GeoLocation(mEntity.place.location.lat.doubleValue(), mEntity.place.location.lng.doubleValue());
		GeoPoint geoPoint = new GeoPoint((int) (entityLocation.latitude.doubleValue() * 1E6), (int) (entityLocation.longitude.doubleValue() * 1E6));

		OverlayItem overlayItem = new OverlayItem(geoPoint, mEntity.name, mEntity.description);
		overlayItem.setMarker(drawable);

		mItemizedOverlay = new CandiItemizedOverlay(null, null, drawable, mMapView);
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

	public class MapBeacon {
		public GeoPoint	point;
		public String	title;
		public String	message;
	}
}