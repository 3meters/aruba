package com.aircandi.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockMapFragment;
import com.aircandi.utilities.UI;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;

public class MapFragment extends SherlockMapFragment {

	private GoogleMap	mMap;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = super.onCreateView(inflater, container, savedInstanceState);
		mMap = getMap();
		// Check if we were successful in obtaining the map.
		if (checkReady()) {
			setUpMap();
		}
		return root;
	}

	private boolean checkReady() {
		if (mMap == null) {
			UI.showToastNotification("map not ready", Toast.LENGTH_SHORT);
			return false;
		}
		return true;
	}

	private void setUpMap() {

		mMap.setMyLocationEnabled(true);
		mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		mMap.setLocationSource(null);

		UiSettings uiSettings = mMap.getUiSettings();

		uiSettings.setZoomControlsEnabled(false);
		uiSettings.setMyLocationButtonEnabled(true);
		uiSettings.setAllGesturesEnabled(true);
		uiSettings.setCompassEnabled(true);

	}
}
