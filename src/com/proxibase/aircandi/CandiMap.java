package com.proxibase.aircandi;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextPaint;
import android.text.TextUtils;
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
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.EntityPoint;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

public class CandiMap extends MapActivity {

	protected AircandiCommon		mCommon;
	private List<Object>			mEntityPoints	= new ArrayList<Object>();
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

		mMapView = new MapView(this, Aircandi.isDebugBuild(this) ? CandiConstants.GOOGLE_API_KEY_DEBUG : CandiConstants.GOOGLE_API_KEY_RELEASE);
		mMapView.setBuiltInZoomControls(true);
		mMapView.setReticleDrawMode(MapView.ReticleDrawMode.DRAW_RETICLE_OVER);
		mMapView.setSatellite(false);
		mMapView.setStreetView(false);
		mMapView.setClickable(true);

		mMapController = mMapView.getController();

		mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
		mMyLocationOverlay.runOnFirstFix(new Runnable() {

			public void run() {
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
		if (lastKnownLocationNetwork != null) {
			if (Aircandi.isBetterLocation(lastKnownLocationNetwork, Aircandi.getInstance().getCurrentLocation())) {
				Aircandi.getInstance().setCurrentLocation(lastKnownLocationNetwork);
			}
		}

		Location lastKnownLocationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (lastKnownLocationGPS != null) {
			if (Aircandi.isBetterLocation(lastKnownLocationGPS, Aircandi.getInstance().getCurrentLocation())) {
				Aircandi.getInstance().setCurrentLocation(lastKnownLocationGPS);
			}
		}
	}

	private void bind() {

		mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(4));
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
				parameters.putDouble("radiusInMeters", 5000d);
				parameters.putString("userId", Aircandi.getInstance().getUser().id);

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "GetEntitiesNearLocation");
				serviceRequest.setRequestType(RequestType.Method);
				serviceRequest.setParameters(parameters);
				serviceRequest.setResponseFormat(ResponseFormat.Json);

				ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				if (serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) serviceResponse.data;
					mEntityPoints = ProxibaseService.convertJsonToObjects(jsonResponse, EntityPoint.class, GsonType.ProxibaseService);
				}
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				if (serviceResponse.responseCode == ResponseCode.Success) {
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

	public void onHomeClick(View view) {
		mCommon.doHomeClick(view);
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

	public void moveToGeoPoint(GeoPoint geoPoint) {
		mMapController.animateTo(geoPoint);
		mMapView.invalidate();
	}

	public void showCandi() {
		mMapOverlays = mMapView.getOverlays();
		Drawable drawable = getResources().getDrawable(R.drawable.icon_map_candi);
		//drawable.setBounds(0, 0, 40, 40);
		mItemizedOverlay = new CandiItemizedOverlay(drawable, false);

		for (Object entityPointObject : mEntityPoints) {
			EntityPoint entityPoint = (EntityPoint) entityPointObject;
			GeoPoint point = new GeoPoint((int) (entityPoint.latitude.doubleValue() * 1E6), (int) (entityPoint.longitude.doubleValue() * 1E6));
			OverlayItem overlayitem = new OverlayItem(point, entityPoint.label, entityPoint.label);

			/* User custom marker */
			//			if (entityPoint.imagePreviewUri != null && !entityPoint.imagePreviewUri.equals("")) {
			//				/*
			//				 * If we find it in the cache we'll use it otherwise we fall back to the
			//				 * default marker. It would be expensive to deal with images way outside the users
			//				 * current radar range.
			//				 */
			//				//				Bitmap bitmap = ImageManager.getInstance().getImageLoader().getImageCache().get(entityPoint.imagePreviewUri);
			//				//				if (bitmap != null) {
			//				//					BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
			//				//					bitmapDrawable.setBounds(0, 0, 80, 80);
			//				//					overlayitem.setMarker(bitmapDrawable);
			//				//				}
			//				//				else {
			//				Drawable marker = getResources().getDrawable(R.drawable.icon_picture);
			//				marker.setBounds(0, 0, 40, 35);
			//				overlayitem.setMarker(marker);
			//				//				}
			//			}
			//			else if (entityPoint.linkUri != null && !entityPoint.linkUri.equals("")) {
			//				Drawable marker = getResources().getDrawable(R.drawable.icon_link);
			//				marker.setBounds(0, 0, 40, 40);
			//				overlayitem.setMarker(marker);
			//			}
			//			else {
			//				Drawable marker = getResources().getDrawable(R.drawable.icon_post);
			//				marker.setBounds(0, 0, 40, 35);
			//				overlayitem.setMarker(marker);
			//			}

			overlayitem.setMarker(drawable);
			mItemizedOverlay.addOverlay(overlayitem);
		}
		mMapOverlays.add(mItemizedOverlay);
		mMapController.zoomToSpan(mItemizedOverlay.getLatSpanE6() + 1500, mItemizedOverlay.getLonSpanE6() + 1500);
		mMapView.invalidate();
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onRestart() {
		Logger.d(this, "CandiActivity restarting");
		super.onRestart();

		if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight"))) {
			Logger.d(this, "Restarting because of theme change");
			mCommon.mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight");
			mCommon.reload();
		}
		else {

			/* User could have changed */
			if (findViewById(R.id.image_user) != null && Aircandi.getInstance().getUser() != null) {
				User user = Aircandi.getInstance().getUser();
				mCommon.setUserPicture(user.imageUri, user.linkUri, (WebImageView) findViewById(R.id.image_user));
			}

			/* Currrent tab could have changed */
			mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(4));
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

	public static class CandiItemizedOverlay extends ItemizedOverlay {

		private ArrayList<OverlayItem>	mOverlays	= new ArrayList<OverlayItem>();
		private boolean					mShowTitles	= false;

		public CandiItemizedOverlay(Drawable defaultMarker, Boolean showTitles) {
			super(boundCenterBottom(defaultMarker));
			mShowTitles = showTitles;
			populate();
		}

		@Override
		protected OverlayItem createItem(int i) {
			return mOverlays.get(i);
		}

		@Override
		public int size() {
			return mOverlays.size();
		}

		@Override
		protected boolean onTap(int index) {
			return true;
		}

		public void addOverlay(OverlayItem overlay) {
			mOverlays.add(overlay);
			populate();
		}

		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {

			super.draw(canvas, mapView, shadow);

			if (!shadow && mShowTitles) {
				for (OverlayItem item : mOverlays) {
					/*
					 * Converts latitude & longitude of this overlay item to coordinates on screen.
					 * As we have called boundCenterBottom() in constructor, so these coordinates
					 * will be of the bottom center position of the displayed marker.
					 */
					GeoPoint point = item.getPoint();
					Point markerBottomCenterCoords = new Point();
					mapView.getProjection().toPixels(point, markerBottomCenterCoords);

					/* Find the width and height of the title */
					TextPaint paintText = new TextPaint();
					Paint paintRect = new Paint();

					Rect rect = new Rect();
					paintText.setTextSize(CandiConstants.MAP_VIEW_FONT_SIZE);
					String title = (String) TextUtils.ellipsize(item.getTitle(), paintText, CandiConstants.MAP_VIEW_TITLE_LENGTH_MAX,
							TextUtils.TruncateAt.END);
					paintText.getTextBounds(title, 0, title.length(), rect);

					rect.inset(-CandiConstants.MAP_VIEW_TITLE_MARGIN, -CandiConstants.MAP_VIEW_TITLE_MARGIN);
					rect.offsetTo(markerBottomCenterCoords.x - rect.width() / 2, markerBottomCenterCoords.y - CandiConstants.MAP_VIEW_MARKER_HEIGHT
																					- rect.height());

					paintText.setTextAlign(Paint.Align.CENTER);
					paintText.setTextSize(CandiConstants.MAP_VIEW_FONT_SIZE);
					paintText.setARGB(255, 255, 255, 255);
					paintText.setAntiAlias(true);
					paintRect.setARGB(130, 0, 0, 0);

					canvas.drawRoundRect(new RectF(rect), 4, 4, paintRect);
					canvas.drawText(title, rect.left + rect.width() / 2, rect.bottom - CandiConstants.MAP_VIEW_TITLE_MARGIN, paintText);
				}
			}
		}
	}
}