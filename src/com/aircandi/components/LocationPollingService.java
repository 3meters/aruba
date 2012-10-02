package com.aircandi.components;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

import com.aircandi.core.CandiConstants;

/**
 * Service providing the guts of the location polling engine. Uses a WakeLock to
 * ensure the CPU stays on while the location lookup is going on. Handles both
 * successful and timeout conditions.
 */
@SuppressLint("Registered")
public class LocationPollingService extends Service {

	private static final String						LOCK_NAME_STATIC	= "com.aircandi.LocationPoller";
	private static volatile PowerManager.WakeLock	mLockStatic			= null;

	/**
	 * This is called on 2.0+ (API level 5 or higher). Returning
	 * START_NOT_STICKY tells the system to not restart the service if it is
	 * killed because of poor resource (memory/cpu) conditions.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		if (gpsEnabled || networkEnabled) {
			Logger.d(LocationPollingService.this, "Location polling started");
			getLock(getApplicationContext()).acquire();
			new PollingThread(getLock(this), locationManager).start();
		}
		return (START_NOT_STICKY);
	}

	synchronized private static PowerManager.WakeLock getLock(Context context) {
		/*
		 * Lazy-initializes the WakeLock when we first use it. We use a partial
		 * WakeLock since we only need the CPU on, not the screen.
		 */
		if (mLockStatic == null) {
			PowerManager powerManager = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
			mLockStatic = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME_STATIC);
			mLockStatic.setReferenceCounted(true);
		}
		return (mLockStatic);
	}

	@Override
	public IBinder onBind(Intent i) {
		return (null);
	}

	// --------------------------------------------------------------------------------------------
	// Polling thread
	// --------------------------------------------------------------------------------------------

	private class PollingThread extends WakefulThread {

		private LocationManager		mLocationManager	= null;
		private Runnable			mTimeoutRunnable	= null;
		private Handler				mHandler			= new Handler();
		private LocationListener	mLocationListener	= null;

		PollingThread(PowerManager.WakeLock lock, LocationManager locationManager) {
			super(lock, "LocationPolling");

			mLocationManager = locationManager;
			mLocationListener = new LocationListener() {

				public void onLocationChanged(Location location) {
					Logger.v(LocationPollingService.this, "Location update received: " + location.getProvider() + String.valueOf(location.getAccuracy()));

					/* Cancel the timeout */
					mHandler.removeCallbacks(mTimeoutRunnable);

					/* Update the current location */
					GeoLocationManager.getInstance().setLocation(location);
					location.setTime(System.currentTimeMillis());

					/* Notify interested parties */
					Events.EventBus.onLocationChanged(location);

					/*
					 * Stop if we have an accurate enough network location fix
					 * and the gps provider is not enabled.
					 */
					if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
						if (location.hasAccuracy() && location.getAccuracy() <= 50) {
							if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
								Logger.d(LocationPollingService.this, "Location polling stopped: accurate network fix obtained");
								quit();
							}
						}
					}
					else if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
						if (location.hasAccuracy() && location.getAccuracy() <= 30) {
							Logger.d(LocationPollingService.this, "Location polling stopped: accurate gps fix obtained");
							quit();
						}
					}
				}

				public void onProviderDisabled(String provider) {}

				public void onProviderEnabled(String provider) {}

				public void onStatusChanged(String provider, int status, Bundle extras) {}
			};
			
			mTimeoutRunnable = new Runnable() {

				public void run() {
					Logger.d(LocationPollingService.this, "Location polling stopped: timeout");
					quit();
				}
			};
		}

		@Override
		protected void onPreExecute() {
			mHandler.postDelayed(mTimeoutRunnable, CandiConstants.LOCATION_POLLING_TIMEOUT);
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
		}

		@Override
		protected void onPostExecute() {
			mLocationManager.removeUpdates(mLocationListener);
			super.onPostExecute();
		}

		/**
		 * Called when the WakeLock is completely unlocked. Stops the service,
		 * so everything shuts down.
		 */
		@Override
		protected void onUnlocked() {
			stopSelf();
		}
	}
}