package com.aircandi.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Maps;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.AirMarker;
import com.aircandi.service.objects.Cursor;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.ServiceBase;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapForm extends BaseEntityForm {

	SupportMapFragment			mMapFragment;
	List<AirMarker>				mMarkers							= new ArrayList<AirMarker>();
	LatLngBounds				mBounds;
	private static final int	REQUEST_CODE_RECOVER_PLAY_SERVICES	= 1001;
	private static final int	DEFAULT_ZOOM						= 16;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final List<String> jsonMarkers = extras.getStringArrayList(Constants.EXTRA_MARKERS);
			if (jsonMarkers != null) {
				for (String jsonMarker : jsonMarkers) {
					AirMarker marker = (AirMarker) Json.jsonToObject(jsonMarker, Json.ObjectType.AIR_MARKER);
					mMarkers.add(marker);
				}
			}
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		mLinkProfile = LinkProfile.LINKS_FOR_PLACE;
		mMapFragment = new MapFragment();
		getSupportFragmentManager().beginTransaction().add(R.id.fragment_holder, mMapFragment).commit();
	}

	@Override
	public void databind(final BindingMode mode) {
		/*
		 * Just here for a pre-databinding check.
		 */
		if (checkPlayServices()) {

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					showBusy();
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("GetMapMarkers");

					List<String> linkTypes = new ArrayList<String>();
					List<String> schemas = new ArrayList<String>();
					linkTypes.add(Constants.TYPE_LINK_CONTENT);
					schemas.add(Constants.SCHEMA_ENTITY_PLACE);

					Cursor cursor = new Cursor()
							.setLimit(ServiceConstants.PAGE_SIZE_PLACES_MAP)
							.setSort(Maps.asMap("modifiedDate", -1))
							.setSkip(0)
							.setSchemas(schemas)
							.setLinkTypes(linkTypes)
							.setDirection(Direction.out.name());;

					ModelResult result = EntityManager.getInstance().loadEntitiesForEntity(mEntityId, null, cursor, null);

					return result;
				}

				@Override
				protected void onPostExecute(Object response) {

					ModelResult result = (ModelResult) response;
					if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
						List<Entity> entities = (List<Entity>) result.data;
						Collections.sort(entities, new ServiceBase.SortByPositionSortDate());
						for (Entity entity : entities) {

							if (entity.location != null) {
								Boolean exists = false;
								for (AirMarker marker : mMarkers) {
									if (marker.id.equals(entity.id)) {
										exists = true;
										break;
									}
								}

								if (!exists) {
									AirMarker marker = new AirMarker(entity.id, entity.name, null, entity.location.lat, entity.location.lng, false,
											R.drawable.img_marker_candigram_inactive);
									mMarkers.add(marker);
								}
							}
						}
					}
					hideBusy();
					findViewById(R.id.fragment_holder).setVisibility(View.VISIBLE);
					MapForm.super.databind(mode);
				}

			}.execute();
		}
	}

	@Override
	public void draw() {
		mFirstDraw = false;
		if (!TextUtils.isEmpty(mEntity.name)) {
			setActivityTitle(mEntity.name);
		}

		if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
			UI.showToastNotification("Google Play Services not available", Toast.LENGTH_SHORT);
			return;
		}

		if (getVersionFromPackageManager(this) >= 2) {
			setUpMap();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_CODE_RECOVER_PLAY_SERVICES:
				if (resultCode == RESULT_CANCELED) {
					Toast.makeText(this, "Google Play Services must be installed and up-to-date.", Toast.LENGTH_SHORT).show();
					finish();
				}
				return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	/**
	 * This is where we can add markers or lines, add listeners or move the camera. In this case, we
	 * just add a marker near Africa.
	 * <p>
	 * This should only be called once and when we are sure that {@link #mMap} is not null.
	 */
	private void setUpMap() {
		if (mMarkers.size() == 0) {
			AirLocation location = mEntity.getLocation();
			if (location != null) {
				MarkerOptions options = new MarkerOptions().position(new LatLng(location.lat.doubleValue(), location.lng.doubleValue()));
				if (mEntity.name != null) {
					options.title(mEntity.name);
				}
				Marker marker = mMapFragment.getMap().addMarker(options);
				marker.showInfoWindow();
				mMapFragment.getMap().moveCamera(
						CameraUpdateFactory.newLatLngZoom(new LatLng(location.lat.doubleValue(), location.lng.doubleValue()), DEFAULT_ZOOM));
			}
		}
		else {

			LatLngBounds.Builder bc = new LatLngBounds.Builder();
			for (AirMarker airMarker : mMarkers) {
				LatLng latLng = new LatLng(airMarker.lat.doubleValue(), airMarker.lng.doubleValue());
				MarkerOptions options = new MarkerOptions().position(latLng);
				bc.include(latLng);

				if (airMarker.title != null) {
					options.title(airMarker.title);
				}
				if (airMarker.snippet != null) {
					options.snippet(airMarker.snippet);
				}
				if (airMarker.iconResId != null) {
					options.icon(BitmapDescriptorFactory.fromResource(airMarker.iconResId));
				}
				Marker marker = mMapFragment.getMap().addMarker(options);
				if (airMarker.current != null && airMarker.current) {
					marker.showInfoWindow();
				}
			}
			mBounds = bc.build();
			mMapFragment.getView().post(new Runnable() {

				@Override
				public void run() {
					mMapFragment.getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(mBounds, 75));
					float zoom = mMapFragment.getMap().getCameraPosition().zoom;
					if (zoom > DEFAULT_ZOOM) {
						mMapFragment.getMap().animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM), 1000, null);
					}
				}
			});
		}
	}

	private boolean checkPlayServices() {
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (status != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
				showErrorDialog(status);
			}
			else {
				UI.showToastNotification("Maps are not supported for this device", Toast.LENGTH_LONG);
				finish();
			}
			return false;
		}
		return true;
	}

	void showErrorDialog(int code) {
		GooglePlayServicesUtil.getErrorDialog(code, this, REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.map_form;
	}

	private static int getVersionFromPackageManager(Context context) {
		PackageManager packageManager = context.getPackageManager();
		FeatureInfo[] featureInfos = packageManager.getSystemAvailableFeatures();
		if (featureInfos != null && featureInfos.length > 0) {
			for (FeatureInfo featureInfo : featureInfos) {
				// Null feature name means this feature is the open gl es version feature.
				if (featureInfo.name == null) {
					if (featureInfo.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED) {
						return getMajorVersion(featureInfo.reqGlEsVersion);
					}
					else {
						return 1; // Lack of property means OpenGL ES version 1
					}
				}
			}
		}
		return 1;
	}

	/** @see FeatureInfo#getGlEsVersion() */
	private static int getMajorVersion(int glEsVersion) {
		return ((glEsVersion & 0xffff0000) >> 16);
	}

}