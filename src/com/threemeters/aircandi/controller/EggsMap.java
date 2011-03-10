package com.threemeters.aircandi.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.threemeters.aircandi.model.Basket;
import com.threemeters.sdk.android.core.BaseQueryListener;
import com.threemeters.sdk.android.core.RippleRunner;
import com.threemeters.sdk.android.core.RippleService;
import com.threemeters.sdk.android.core.RippleService.QueryFormat;

public class EggsMap extends MapActivity
{
	private ArrayList<Object>	mListItems		= null;
	private Class				mClass			= Basket.class;
	public static EggsMap		mSelf			= null;
	private MapView				mMapView		= null;
	private MapController		mMapController	= null;
	private ProgressDialog		mProgressDialog	= null;
	protected ImageView			mProgressIndicator;
	protected ImageView			mButtonRefresh;

	List<Overlay>				mapOverlays;
	Drawable					drawable;
	BasketItemizedOverlay		itemizedOverlay;
	private LocationManager		mLocManager;
	private LocationListener	mLocListener;
	private MyLocationOverlay	mMyLocationOverlay;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("layoutForTab"))
			setContentView(R.layout.eggs_map_tab);
		else
			setContentView(R.layout.eggs_map);

		mSelf = this;

		mLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mLocListener = new MyLocationListener();
		mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocListener);
		mMapView = (MapView) findViewById(R.id.mapview);
		mMapController = mMapView.getController();
		mMapView.setBuiltInZoomControls(true);
		mMapView.setReticleDrawMode(MapView.ReticleDrawMode.DRAW_RETICLE_OVER);
		mMapView.postInvalidate();

		// Get the data for the list
		configureAppHeader();
		configure();
	}
	
	private void configureAppHeader()
	{
		TableRow row = (TableRow) findViewById(R.id.Texture_Row);
		if (row != null)
		{
			Drawable bgDrawable = row.getBackground().mutate();
			if (bgDrawable != null)
				bgDrawable.setAlpha(64);
		}
	}

	private void configure()
	{
		// Get view references
		mProgressIndicator = (ImageView) findViewById(R.id.Application_ProgressIndicator);
		if (mProgressIndicator != null)
			mProgressIndicator.setVisibility(View.INVISIBLE);
		mButtonRefresh = (ImageView) findViewById(R.id.Application_Button_Refresh);
		if (mButtonRefresh != null)
			mButtonRefresh.setVisibility(View.VISIBLE);
	}

	@Override
	protected boolean isRouteDisplayed()
	{
		return false;
	}

	@Override
	protected boolean isLocationDisplayed()
	{
		return mMyLocationOverlay.isMyLocationEnabled();
	}

	public void showBaskets()
	{
		mapOverlays = mMapView.getOverlays();
		drawable = this.getResources().getDrawable(R.drawable.eggs_mini_cluster2);
		itemizedOverlay = new BasketItemizedOverlay(drawable);

		for (Object basketObj : mListItems)
		{
			Basket basket = (Basket) basketObj;
			GeoPoint point = new GeoPoint((int) (basket.latitude * 1E6), (int) (basket.longitude * 1E6));
			OverlayItem overlayitem = new OverlayItem(point, basket.label, "");
			itemizedOverlay.addOverlay(overlayitem);
		}
		mapOverlays.add(itemizedOverlay);
		mMapView.invalidate();
	}

	public void zoomToCurrentLocation(Location location)
	{
		GeoPoint point = new GeoPoint((int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));
		mMapController.animateTo(point);
		mMapController.setZoom(16);
		mMapView.invalidate();
	}

	public void onHomeClick(View view)
	{
		Intent intent = new Intent(this, Dashboard.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	// For this activity, refresh means rescan and reload point data from the service
	public void onRefreshClick(View view)
	{
		loadData();
	}

	// Titlebar search
	public void onSearchClick(View view)
	{
		AircandiUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
		return;
	}

	// ----------------------------------------------------------------------------------
	// Data query
	// ----------------------------------------------------------------------------------

	public void loadData()
	{
		mProgressDialog = ProgressDialog.show(EggsMap.this, "", "Loading...", true);
		RippleRunner ripple = new RippleRunner();
		Bundle parameters = new Bundle();
		String method = "GetBaskets";
		ripple.post(method, parameters, QueryFormat.Json, new GetBasketsListener());
	}

	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
		Location locationLastKnown = mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (Aircandi.isBetterLocation(locationLastKnown, getCurrentLocation()))
			setCurrentLocation(locationLastKnown);

		if (getCurrentLocation() != null)
			zoomToCurrentLocation(getCurrentLocation());

		mMapView.getOverlays().clear();
		mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
		mMyLocationOverlay.enableMyLocation();
		mMapView.getOverlays().add(mMyLocationOverlay);

		if (mLocManager != null)
			mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocListener);

		mMyLocationOverlay.runOnFirstFix(new Runnable() {
			public void run()
			{
				mMapController.animateTo(mMyLocationOverlay.getMyLocation());
			}
		});

		// Check the egg count again because we configure
		// based on the most current info.
		loadData();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		mLocManager.removeUpdates(mLocListener);
		mMyLocationOverlay.disableMyLocation();
	}

	public class GetBasketsListener extends BaseQueryListener
	{
		public void onComplete(final String response)
		{
			mListItems = RippleService.convertJsonToObjects(response, mClass);

			// Post the processed result back to the UI thread
			EggsMap.this.runOnUiThread(new Runnable() {
				public void run()
				{
					mProgressDialog.dismiss();

					if (mListItems != null && mListItems.size() != 0)
						showBaskets();

					// if (mLocationMeasured != null)
					// zoomToCurrentLocation(mLocationMeasured);
					// else if (mLocationLastKnown != null)
					// zoomToCurrentLocation(mLocationLastKnown);
				}
			});
		}

		@Override
		public void onIOException(IOException e)
		{
			// TODO Auto-generated method stub
			super.onIOException(e);
			EggsMap.this.runOnUiThread(new Runnable() {
				public void run()
				{
					mProgressDialog.dismiss();
					AircandiUI.showToastNotification(EggsMap.this, "Network error", Toast.LENGTH_SHORT);
				}
			});
		}
	}

	public class MyLocationListener implements LocationListener
	{
		@Override
		public void onLocationChanged(Location location)
		{
			if (Aircandi.isBetterLocation(location, getCurrentLocation()))
				setCurrentLocation(location);
		}

		@Override
		public void onProviderDisabled(String provider)
		{
			Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onProviderEnabled(String provider)
		{
			Toast.makeText(getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras)
		{}

	}

	protected void setCurrentLocation(Location currentLocation)
	{
		((Aircandi) getApplicationContext()).currentLocation = currentLocation;
	}

	protected Location getCurrentLocation()
	{
		return ((Aircandi) getApplicationContext()).currentLocation;
	}
}