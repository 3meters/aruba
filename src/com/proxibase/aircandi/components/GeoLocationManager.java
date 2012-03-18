package com.proxibase.aircandi.components;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.widget.Toast;

import com.proxibase.aircandi.Aircandi;
import com.proxibase.aircandi.components.Events.EventHandler;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.service.objects.Observation;

public class GeoLocationManager {

	private static GeoLocationManager	singletonObject;

	private Context						mContext;
	private Location					mCurrentLocation;
	private LocationManager				mLocationManager;
	private LocationListener			mLocationListener;
	private EventHandler				mEventLocationChanged;
	private AtomicBoolean				mLocationScanActive	= new AtomicBoolean(false);
	private Runnable					mLocationScanRunnable;

	public static synchronized GeoLocationManager getInstance() {
		if (singletonObject == null) {
			singletonObject = new GeoLocationManager();
		}
		return singletonObject;
	}

	/**
	 * Designed as a singleton. The private Constructor prevents any other class from instantiating.
	 */
	private GeoLocationManager() {}

	public void setContext(Context context) {
		mContext = context;
	}

	public void initialize() {

		/* Location */
		mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
		mLocationListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				if (isBetterLocation(location, getCurrentLocation())) {
					setCurrentLocation(location);
					location.setTime(System.currentTimeMillis());
					Events.EventBus.onLocationChanged(location);
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

		mLocationScanRunnable = new Runnable() {

			@Override
			public void run() {
				if (mLocationScanActive.get()) {
					Logger.d(GeoLocationManager.this, "Location scan stopped: time limit");
					stopLocationUpdates();
				}
			}
		};

		mEventLocationChanged = new EventHandler() {

			@Override
			public void onEvent(Object data) {

				Location location = getCurrentLocation();
				if (location.hasAccuracy() && location.getAccuracy() <= 30) {
					Logger.d(GeoLocationManager.this, "Location scan stopped: accurate fix obtained");
					stopLocationUpdates();
					return;
				}
			}
		};

		synchronized (Events.EventBus.locationChanged) {
			Events.EventBus.locationChanged.add(mEventLocationChanged);
		}

		/*
		 * We grab the last known location if it is less than two minutes old.
		 */
		Location lastKnownLocationNetwork = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (lastKnownLocationNetwork != null && timeSinceLocationInMillis(lastKnownLocationNetwork) >= CandiConstants.LOCATION_EXPIRATION) {
			if (isBetterLocation(lastKnownLocationNetwork, getCurrentLocation())) {
				Logger.d(this, "Location stored: last known network location");
				setCurrentLocation(lastKnownLocationNetwork);
			}
		}

		Location lastKnownLocationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (lastKnownLocationGPS != null && timeSinceLocationInMillis(lastKnownLocationGPS) >= CandiConstants.LOCATION_EXPIRATION) {
			if (isBetterLocation(lastKnownLocationGPS, getCurrentLocation())) {
				Logger.d(this, "Location stored: last known GPS location");
				setCurrentLocation(lastKnownLocationGPS);
			}
		}
	}

	public Observation getObservation() {

		if (mCurrentLocation == null || !mCurrentLocation.hasAccuracy()) {
			return null;
		}

		Observation observation = new Observation();

		observation.latitude = mCurrentLocation.getLatitude();
		observation.longitude = mCurrentLocation.getLongitude();

		if (mCurrentLocation.hasAltitude()) {
			observation.altitude = mCurrentLocation.getAltitude();
		}
		if (mCurrentLocation.hasAccuracy()) {
			/* In meters. */
			observation.accuracy = mCurrentLocation.getAccuracy();
		}
		if (mCurrentLocation.hasBearing()) {
			/* Direction of travel in degrees East of true North. */
			observation.bearing = mCurrentLocation.getBearing();
		}
		if (mCurrentLocation.hasSpeed()) {
			/* Speed of the device over ground in meters/second. */
			observation.speed = mCurrentLocation.getSpeed();
		}

		return observation;
	}

	public void startLocationUpdates(int scanDurationInMillis, int locationExpirationInMillis) {

		if (!mLocationScanActive.get()) {

			Location location = getCurrentLocation();
			/*
			 * If a provider is disabled, we won't get any updates. If the user enables the provider
			 * during our time window, we will start getting updates.
			 */
			int locationAge = timeSinceLocationInMillis(location);
			if (location != null && locationAge >= locationExpirationInMillis) {
				mLocationScanActive.set(true);
				Logger.d(this, "Location scan started");
				setCurrentLocation(null);
				mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
						getLocationListener());
				mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
						getLocationListener());
				Aircandi.applicationHandler.postDelayed(mLocationScanRunnable, scanDurationInMillis);
			}
			else {
				Logger.d(this, "Location scan skipped: current location still good: " + String.valueOf(locationAge) + "ms");
			}
		}
	}

	public void stopLocationUpdates() {
		mLocationScanActive.set(false);
		Aircandi.applicationHandler.removeCallbacks(mLocationScanRunnable);
		mLocationManager.removeUpdates(getLocationListener());
	}

	public static int timeSinceLocationInMillis(Location location) {
		if (location == null) {
			return Integer.MAX_VALUE;
		}
		long locationTime = location.getTime();
		long currentTime = System.currentTimeMillis();
		long timeDelta = currentTime - locationTime;
		return (int) timeDelta;
	}

	/**
	 * Determines whether one Location reading is better than the current Location fix *
	 * 
	 * @param location The new Location that you want to evaluate
	 * @param currentBestLocation The current Location fix, to which you want to compare the new one
	 */
	public static boolean isBetterLocation(Location location, Location currentBestLocation) {

		if (currentBestLocation == null) {
			/* A new location is always better than no location */
			return true;
		}

		/* Check whether the new location fix is newer or older */
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > CandiConstants.TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -CandiConstants.TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		/*
		 * If it's been more than two minutes since the current location, use the new location
		 * because the user has likely moved
		 */
		if (isSignificantlyNewer) {
			/* If the new location is more than two minutes older, it must be worse */
			return true;
		}
		else if (isSignificantlyOlder) {
			return false;
		}

		/* Check whether the new location fix is more or less accurate */
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		/* Check if the old and new location are from the same provider */
		boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

		/* Determine location quality using a combination of timeliness and accuracy */
		if (isMoreAccurate) {
			return true;
		}
		else if (isNewer && !isLessAccurate) {
			return true;
		}
		else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}

	public static long locationFixAge(Location location) {
		Calendar cal = Calendar.getInstance();
		long timeDelta = cal.getTimeInMillis() - location.getTime();
		return timeDelta;
	}

	/** Checks whether two providers are the same */
	private static boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	public void setLocationListener(LocationListener locationListener) {
		this.mLocationListener = locationListener;
	}

	public LocationListener getLocationListener() {
		return mLocationListener;
	}

	public void setCurrentLocation(Location currentLocation) {
		this.mCurrentLocation = currentLocation;
	}

	public Location getCurrentLocation() {
		return mCurrentLocation;
	}

}
