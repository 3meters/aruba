package com.aircandi.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.events.BeaconsLockedEvent;
import com.aircandi.events.EntitiesByProximityFinishedEvent;
import com.aircandi.events.EntitiesChangedEvent;
import com.aircandi.events.MonitoringWifiScanReceivedEvent;
import com.aircandi.events.PlacesNearLocationFinishedEvent;
import com.aircandi.events.QueryWifiScanReceivedEvent;
import com.aircandi.service.ServiceResponse;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Cursor;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Reporting;

public class ProximityManager {

	public Date							mLastWifiUpdate;
	private Long						mLastBeaconLockedDate;
	private Long						mLastBeaconLoadDate;

	private WifiManager					mWifiManager;
	private EntityCache					mEntityCache;

	/*
	 * Continuously updated as we perform wifi scans. Beacons are only build from the wifi info on demand.
	 */
	private List<WifiScanResult>		mWifiList				= Collections.synchronizedList(new ArrayList<WifiScanResult>());

	private static final WifiScanResult	mWifiMassenaUpper		= new WifiScanResult("00:1c:b3:ae:bf:f0", "test_massena_upper", -50, true);
	private static final WifiScanResult	mWifiMassenaLower		= new WifiScanResult("00:1c:b3:ae:bb:57", "test_massena_lower", -50, true);
	private static final WifiScanResult	mWifiMassenaLowerStrong	= new WifiScanResult("00:1c:b3:ae:bb:57", "test_massena_lower_strong", -20, true);
	private static final WifiScanResult	mWifiMassenaLowerWeak	= new WifiScanResult("00:1c:b3:ae:bb:57", "test_massena_lower_weak", -100, true);
	private static final WifiScanResult	mWifiEmpty				= new WifiScanResult("aa:aa:bb:bb:cc:cc", "test_empty", -50, true);
	private static final String			MockBssid				= "00:00:00:00:00:00";

	private ProximityManager() {
		if (!Aircandi.getInstance().isUsingEmulator()) {
			mWifiManager = (WifiManager) Aircandi.applicationContext.getSystemService(Context.WIFI_SERVICE);
		}
		mEntityCache = EntityManager.getEntityCache();
	}

	private static class ProxiManagerHolder {
		public static final ProximityManager	instance	= new ProximityManager();
	}

	public static ProximityManager getInstance() {
		return ProxiManagerHolder.instance;
	}

	// --------------------------------------------------------------------------------------------
	// Manage beacons
	// --------------------------------------------------------------------------------------------

	public void scanForWifi(final ScanReason reason) {
		/*
		 * If context is null then we probably crashed and the scan service is still calling.
		 */
		if (Aircandi.applicationContext == null) {
			return;
		}

		synchronized (mWifiList) {

			if (!Aircandi.getInstance().isUsingEmulator()) {

				Aircandi.applicationContext.registerReceiver(new BroadcastReceiver() {

					@Override
					public void onReceive(Context context, Intent intent) {

						Aircandi.applicationContext.unregisterReceiver(this);
						Aircandi.stopwatch1.segmentTime("Wifi scan received from system: reason = " + reason.toString());
						Logger.v(ProximityManager.this, "Received wifi scan results for " + reason.name());

						/* get the latest scan results */
						mWifiList.clear();

						for (ScanResult scanResult : mWifiManager.getScanResults()) {
							/*
							 * Dev/test could trigger a mock access point and we filter for it
							 * just to prevent confusion. We add our own below if emulator is active.
							 */
							if (!scanResult.BSSID.equals(MockBssid)) {
								mWifiList.add(new WifiScanResult(scanResult));
							}
						}

						final String testingBeacons = Aircandi.settings.getString(Constants.PREF_TESTING_BEACONS, "natural");

						if (!ListPreferenceMultiSelect.contains("natural", testingBeacons, null)) {
							mWifiList.clear();
						}

						if (ListPreferenceMultiSelect.contains("massena_upper", testingBeacons, null)) {
							mWifiList.add(mWifiMassenaUpper);
						}

						if (ListPreferenceMultiSelect.contains("massena_lower", testingBeacons, null)) {
							mWifiList.add(mWifiMassenaLower);
						}

						if (ListPreferenceMultiSelect.contains("massena_lower_strong", testingBeacons, null)) {
							mWifiList.add(mWifiMassenaLowerStrong);
						}

						if (ListPreferenceMultiSelect.contains("massena_lower_weak", testingBeacons, null)) {
							mWifiList.add(mWifiMassenaLowerWeak);
						}

						if (ListPreferenceMultiSelect.contains("empty", testingBeacons, null)) {
							mWifiList.add(mWifiEmpty);
						}
						Collections.sort(mWifiList, new WifiScanResult.SortWifiBySignalLevel());

						mLastWifiUpdate = DateTime.nowDate();
						if (reason == ScanReason.MONITORING) {
							BusProvider.getInstance().post(new MonitoringWifiScanReceivedEvent(mWifiList));
						}
						else if (reason == ScanReason.QUERY) {
							BusProvider.getInstance().post(new QueryWifiScanReceivedEvent(mWifiList));
						}

					}
				}, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

				Reporting.updateCrashKeys();
				mWifiManager.startScan();
			}
			else {
				mWifiList.clear();
				Logger.d(ProximityManager.this, "Emulator enabled so using dummy scan results");
				mWifiList.add(mWifiMassenaUpper);
				if (reason == ScanReason.MONITORING) {
					BusProvider.getInstance().post(new MonitoringWifiScanReceivedEvent(mWifiList));
				}
				else if (reason == ScanReason.QUERY) {
					BusProvider.getInstance().post(new QueryWifiScanReceivedEvent(mWifiList));
				}
			}
		}
	}

	public void lockBeacons() {
		/*
		 * Makes sure that the beacon collection is an accurate representation
		 * of the latest wifi scan.
		 */
		mEntityCache.removeEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null, null);
		/*
		 * insert beacons for the latest scan results.
		 */
		synchronized (mWifiList) {
			WifiScanResult scanResult = null;
			for (int i = 0; i < mWifiList.size(); i++) {
				scanResult = mWifiList.get(i);
				Beacon beacon = new Beacon(scanResult.BSSID
						, scanResult.SSID
						, scanResult.SSID
						, scanResult.level
						, scanResult.test);

				beacon.synthetic = true;
				beacon.schema = Constants.SCHEMA_ENTITY_BEACON;
				mEntityCache.upsertEntity(beacon);
			}
		}

		mLastBeaconLockedDate = DateTime.nowDate().getTime();
		BusProvider.getInstance().post(new BeaconsLockedEvent());
	}

	// --------------------------------------------------------------------------------------------
	// Load beacon related entities
	// --------------------------------------------------------------------------------------------

	public synchronized ServiceResponse getEntitiesByProximity() {
		/*
		 * All current beacons ids are sent to the service. Previously discovered beacons are included in separate
		 * array along with a their freshness date.
		 * 
		 * To force a full rebuild of all entities for all beacons, clear the beacon collection.
		 * 
		 * The service returns all entities for new beacons and entities that have had activity since the freshness
		 * date for old beacons. Unchanged entities from previous scans will still be updated for local changes in
		 * visibility.
		 */
		Logger.d(this, "Processing beacons from scan");
		Aircandi.stopwatch1.segmentTime("Entities for beacons (synchronized): processing started");

		/*
		 * Call the proxi service to see if the new beacons have been tagged with any entities. If call comes back
		 * null then there was a network or service problem. The user got a toast notification from the service. We
		 * are making synchronous calls inside an asynchronous thread.
		 */
		ServiceResponse serviceResponse = new ServiceResponse();

		/* Construct string array of the beacon ids */
		List<String> beaconIds = new ArrayList<String>();
		List<Beacon> beacons = (List<Beacon>) mEntityCache.getEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null, null, null);

		for (Beacon beacon : beacons) {
			beaconIds.add(beacon.id);
		}

		Integer removeCount = mEntityCache.removeEntities(Constants.SCHEMA_ENTITY_PLACE, null, false, true);
		Logger.v(this, "Removed proximity places from cache: count = " + String.valueOf(removeCount));

		/*
		 * Early exit if there aren't any beacons around
		 */
		if (beaconIds.size() == 0) {
			mLastBeaconLoadDate = DateTime.nowDate().getTime();

			/* All cached place entities that qualify based on current distance pref setting */
			final List<Entity> entitiesForEvent = (List<Entity>) EntityManager.getInstance().getPlaces(null, null);
			Aircandi.stopwatch1.segmentTime("Entities for beacons: objects processed");

			BusProvider.getInstance().post(new EntitiesByProximityFinishedEvent());
			BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent, "getEntitiesByProximity"));
			return serviceResponse;

		}

		/* Add current registrationId */
		String installationId = Aircandi.getInstallationId();

		/* Cursor */
		Cursor cursor = new Cursor()
				.setLimit(Aircandi.applicationContext.getResources().getInteger(R.integer.limit_places_radar))
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(0);

		serviceResponse = mEntityCache.loadEntitiesByProximity(beaconIds
				, LinkOptions.getDefault(LinkProfile.LINKS_FOR_BEACONS)
				, cursor
				, installationId
				, Aircandi.stopwatch1);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			mLastBeaconLoadDate = ((ServiceData) serviceResponse.data).date.longValue();

			/* All cached place entities that qualify based on current distance pref setting */
			final List<Entity> entitiesForEvent = (List<Entity>) EntityManager.getInstance().getPlaces(null, null);
			Aircandi.stopwatch1.segmentTime("Entities for beacons: objects processed");

			BusProvider.getInstance().post(new EntitiesByProximityFinishedEvent());
			BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent, "getEntitiesByProximity"));
		}

		return serviceResponse;
	}

	public synchronized ServiceResponse getEntitiesNearLocation(AirLocation location) {
		/*
		 * We find all aircandi place entities in the cache (via proximity or location) that are active based
		 * on the current search parameters (beacons and search radius) and could be supplied by the place provider. We
		 * create an array of the provider place id's and pass them so they can be excluded from the places
		 * that get returned.
		 */
		final List<String> excludePlaceIds = new ArrayList<String>();
		for (Entity entity : EntityManager.getInstance().getPlaces(false, null)) {
			Place place = (Place) entity;
			excludePlaceIds.add(place.id);
			if (!place.getProvider().type.equals(Constants.TYPE_PROVIDER_AIRCANDI)) {
				excludePlaceIds.add(place.getProvider().id);
			}
		}

		ServiceResponse serviceResponse = mEntityCache.loadEntitiesNearLocation(location
				, LinkOptions.getDefault(LinkProfile.LINKS_FOR_PLACE)
				, excludePlaceIds);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final List<Entity> entitiesForEvent = (List<Entity>) EntityManager.getInstance().getPlaces(null, null);
			BusProvider.getInstance().post(new PlacesNearLocationFinishedEvent());
			BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent, "getEntitiesNearLocation"));
		}
		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	public Boolean beaconRefreshNeeded(Location activeLocation) {
		if (mLastBeaconLoadDate != null) {
			final Long interval = DateTime.nowDate().getTime() - mLastBeaconLoadDate;
			if (interval > Constants.INTERVAL_REFRESH) {
				Logger.v(this, "Refresh needed: past interval");
				return true;
			}
		}
		return false;
	}

	public List<Beacon> getStrongestBeacons(int max) {

		final List<Beacon> beaconStrongest = new ArrayList<Beacon>();
		int beaconCount = 0;
		List<Beacon> beacons = (List<Beacon>) mEntityCache.getEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null, null, null);
		Collections.sort(beacons, new Beacon.SortBySignalLevel());

		for (Beacon beacon : beacons) {
			if (beacon.test) continue;
			beaconStrongest.add(beacon);
			beaconCount++;
			if (beaconCount >= max) break;
		}
		return beaconStrongest;
	}

	public Beacon getStrongestBeacon() {
		final List<Beacon> beacons = getStrongestBeacons(1);
		if (beacons.size() > 1) {
			return beacons.get(0);
		}
		return null;
	}

	public Number getLastBeaconLockedDate() {
		return mLastBeaconLockedDate;
	}

	public Number getLastBeaconLoadDate() {
		return mLastBeaconLoadDate;
	}

	public List<WifiScanResult> getWifiList() {
		return mWifiList;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public static class ModelResult {
		public Object			data;
		public ServiceResponse	serviceResponse	= new ServiceResponse();
	}

	public static class WifiScanResult {

		public String	BSSID;
		public String	SSID;
		public int		level	= 0;
		public Boolean	test	= false;

		private WifiScanResult(String bssid, String ssid, int level, Boolean test) {
			this.BSSID = bssid;
			this.SSID = ssid;
			this.level = level;
			this.test = test;
		}

		private WifiScanResult(ScanResult scanResult) {
			this.BSSID = scanResult.BSSID;
			this.SSID = scanResult.SSID;
			this.level = scanResult.level;
		}

		private static class SortWifiBySignalLevel implements Comparator<WifiScanResult> {

			@Override
			public int compare(WifiScanResult object1, WifiScanResult object2) {
				if (object1.level > object2.level) {
					return -1;
				}
				else if (object1.level < object2.level) {
					return 1;
				}
				else {
					return 0;
				}
			}
		}
	}

	public static enum ScanReason {
		QUERY,
		MONITORING
	}

}