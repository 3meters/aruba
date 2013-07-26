package com.aircandi.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

import com.aircandi.beta.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.DefaultType;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Routing;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapForm extends BaseEntityForm {

	private GoogleMap	mMap;
	
	

	@Override
	protected void databind(final Boolean refreshProposed) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("GetEntity");

				Entity entity = EntityManager.getEntity(mEntityId);
				Boolean refresh = refreshProposed;
				if (entity == null) {
					refresh = true;
				}
				else if (!entity.shortcuts && !entity.synthetic) {
					refresh = true;
				}

				final ModelResult result = EntityManager.getInstance().getEntity(mEntityId
						, refresh
						, LinkOptions.getDefault(DefaultType.LinksForPlace));

				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;

				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					if (result.data != null) {
						mEntity = (Entity) result.data;
						mEntityModelRefreshDate = ProximityManager.getInstance().getLastBeaconLoadDate();
						mEntityModelActivityDate = EntityManager.getEntityCache().getLastActivityDate();
						setActivityTitle(mEntity.name);

						draw();
					}
				}
				else {
					Routing.serviceError(MapForm.this, result.serviceResponse);
				}
				mBusyManager.hideBusy();
			}

		}.execute();
	}

	@Override
	protected void draw() {
		setUpMapIfNeeded();
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onCancel(Boolean force) {
		setResult(Activity.RESULT_CANCELED);
		finish();
		Animate.doOverridePendingTransition(this, TransitionType.HelpToPage);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	/**
	 * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
	 * installed) and the map has not already been instantiated.. This will ensure that we only ever
	 * call {@link #setUpMap()} once when {@link #mMap} is not null.
	 * <p>
	 * If it isn't installed {@link SupportMapFragment} (and {@link com.google.android.gms.maps.MapView MapView}) will
	 * show a prompt for the user to install/update the Google Play services APK on their device.
	 * <p>
	 * A user can return to this FragmentActivity after following the prompt and correctly installing/updating/enabling
	 * the Google Play services. Since the FragmentActivity may not have been completely destroyed during this process
	 * (it is likely that it would only be stopped or paused), {@link #onCreate(Bundle)} may not be called again so we
	 * should call this method in {@link #onResume()} to guarantee that it will be called.
	 */
	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the map.
		if (mMap == null) {
			// Try to obtain the map from the SupportMapFragment.
			mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
					.getMap();
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				setUpMap();
			}
		}
	}

	/**
	 * This is where we can add markers or lines, add listeners or move the camera. In this case, we
	 * just add a marker near Africa.
	 * <p>
	 * This should only be called once and when we are sure that {@link #mMap} is not null.
	 */
	private void setUpMap() {
		AirLocation location = mEntity.getLocation();
		if (location != null) {
			MarkerOptions marker = new MarkerOptions().position(new LatLng(location.lat.doubleValue(), location.lng.doubleValue()));
			if (mEntity.name != null) {
				marker.title(mEntity.name);
			}
			mMap.addMarker(marker);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.map_form;
	}

}