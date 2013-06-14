package com.aircandi.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestType;
import com.aircandi.service.HttpService.ResponseFormat;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.DefaultType;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.utilities.DateUtils;
import com.google.android.gcm.GCMRegistrar;

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

	private ProximityManager() {
		if (!Aircandi.getInstance().isUsingEmulator()) {
			mWifiManager = (WifiManager) Aircandi.applicationContext.getSystemService(Context.WIFI_SERVICE);
		}
		mEntityCache = EntityManager.getInstance().getEntityCache();
	}

	public static class ProxiManagerHolder {
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

						Logger.v(ProximityManager.this, "Received wifi scan results for " + reason.name());
						Aircandi.applicationContext.unregisterReceiver(this);

						/* Get the latest scan results */
						mWifiList.clear();

						for (ScanResult scanResult : mWifiManager.getScanResults()) {
							mWifiList.add(new WifiScanResult(scanResult));
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

						mLastWifiUpdate = DateUtils.nowDate();
						if (reason == ScanReason.monitoring) {
							BusProvider.getInstance().post(new MonitoringWifiScanReceivedEvent(mWifiList));
						}
						else if (reason == ScanReason.query) {
							BusProvider.getInstance().post(new QueryWifiScanReceivedEvent(mWifiList));
						}

					}
				}, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

				mWifiManager.startScan();
			}
			else {
				mWifiList.clear();
				Logger.d(ProximityManager.this, "Emulator enabled so using dummy scan results");
				mWifiList.add(mWifiMassenaUpper);
				if (reason == ScanReason.monitoring) {
					BusProvider.getInstance().post(new MonitoringWifiScanReceivedEvent(mWifiList));
				}
				else if (reason == ScanReason.query) {
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
		mEntityCache.removeEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null);
		/*
		 * Insert beacons for the latest scan results.
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
				mEntityCache.upsertEntity(beacon);
			}
		}

		mLastBeaconLockedDate = DateUtils.nowDate().getTime();
		BusProvider.getInstance().post(new BeaconsLockedEvent());
	}

	// --------------------------------------------------------------------------------------------
	// Load beacon related entities
	// --------------------------------------------------------------------------------------------

	public synchronized ServiceResponse getEntitiesForBeacons() {
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
		Aircandi.stopwatch1.segmentTime("Entities for beacons: processing started");

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

		/* Add current registrationId */
		String registrationId = GCMRegistrar.getRegistrationId(Aircandi.applicationContext);

		serviceResponse = mEntityCache.loadEntities(beaconIds
				, LinkOptions.getDefault(DefaultType.BeaconEntities)
				, registrationId
				, Aircandi.stopwatch1);

		if (serviceResponse.responseCode == ResponseCode.Success) {
			mLastBeaconLoadDate = ((ServiceData) serviceResponse.data).date.longValue();

			/* All cached place entities that qualify based on current distance pref setting */
			final List<Entity> entitiesForEvent = (List<Entity>) EntityManager.getInstance().getPlaces(null, null);
			Aircandi.stopwatch1.segmentTime("Entities for beacons: objects processed");

			BusProvider.getInstance().post(new EntitiesForBeaconsFinishedEvent());
			BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent));
		}

		return serviceResponse;
	}

	public synchronized ServiceResponse getPlacesNearLocation(AirLocation location) {

		ServiceResponse serviceResponse = new ServiceResponse();
		final Bundle parameters = new Bundle();
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
			if (!place.getProvider().type.equals("aircandi")) {
				excludePlaceIds.add(place.getProvider().id);
			}
		}

		if (excludePlaceIds.size() > 0) {
			parameters.putStringArrayList("excludePlaceIds", (ArrayList<String>) excludePlaceIds);
		}

		parameters.putString("location", "object:" + HttpService.convertObjectToJsonSmart(location, true, true));
		parameters.putInt("limit", 50);

		parameters.putString("provider",
				Aircandi.settings.getString(
						Constants.PREF_TESTING_PLACE_PROVIDER,
						Constants.PREF_TESTING_PLACE_PROVIDER_DEFAULT));

		parameters.putInt("radius", Integer.parseInt(
				Aircandi.settings.getString(
						Constants.PREF_SEARCH_RADIUS,
						Constants.PREF_SEARCH_RADIUS_DEFAULT)));

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_PLACES + "getNearLocation")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		if (Aircandi.getInstance().getUser() != null) {
			serviceRequest.setSession(Aircandi.getInstance().getUser().session);
		}

		serviceResponse = EntityManager.getInstance().dispatch(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.Success) {

			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Place);
			serviceResponse.data = serviceData;

			/* Do a bit of fixup */
			final List<Entity> entities = (List<Entity>) serviceData.data;
			for (Entity entity : entities) {
				/* No id means it's a synthetic */
				Place place = (Place) entity;
				if (entity.id == null) {
					place.id = place.getProvider().id;
					place.modifiedDate = DateUtils.nowDate().getTime();
					place.synthetic = true;
				}
				else {
					place.synthetic = false;
				}
			}

			/* Places locked in by proximity trump places locked in by location */
			final List<Place> proximityPlaces = (List<Place>) EntityManager.getInstance().getPlaces(null, true);

			Iterator<Place> iterProximityPlaces = proximityPlaces.iterator();
			Iterator<Entity> iterLocationPlaces = entities.iterator();

			while (iterLocationPlaces.hasNext()) {
				Entity locPlace = iterLocationPlaces.next();

				while (iterProximityPlaces.hasNext()) {
					Place proxPlace = iterProximityPlaces.next();

					if (proxPlace.id.equals(locPlace.id)) {
						iterLocationPlaces.remove();
					}
					else if (!proxPlace.getProvider().type.equals("aircandi")) {
						if (proxPlace.getProvider().id.equals(locPlace.id)) {
							iterLocationPlaces.remove();
						}
					}
				}
			}

			/* Remove all synthetic places from the cache just to help constrain the cache size */
			mEntityCache.removeEntities(Constants.SCHEMA_ENTITY_PLACE, null, true);

			/* Push place entities to cache */
			mEntityCache.upsertEntities(entities);

			final List<Entity> entitiesForEvent = (List<Entity>) EntityManager.getInstance().getPlaces(null, null);

			BusProvider.getInstance().post(new PlacesNearLocationFinishedEvent());
			BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent));
		}
		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	public Boolean beaconRefreshNeeded(Location activeLocation) {
		if (mLastBeaconLoadDate != null) {
			final Long interval = DateUtils.nowDate().getTime() - mLastBeaconLoadDate;
			if (interval > Constants.INTERVAL_REFRESH) {
				Logger.v(this, "Refresh needed: past interval");
				return true;
			}
		}

		//		final Location lastKnownLocation = LocationManager.getInstance().getLastKnownLocation();
		//		if (lastKnownLocation != null) {
		//			final Boolean hasMoved = LocationManager.hasMoved(lastKnownLocation, activeLocation, PlacesConstants.DIST_ONE_HUNDRED_METERS);
		//			if (hasMoved) {
		//				Logger.v(this, "Refresh needed: moved location");
		//				return true;
		//			}
		//		}

		return false;
	}

	public List<Beacon> getStrongestBeacons(int max) {

		final List<Beacon> beaconStrongest = new ArrayList<Beacon>();
		int beaconCount = 0;
		List<Beacon> beacons = (List<Beacon>) mEntityCache.getEntities(Constants.SCHEMA_ENTITY_BEACON, Constants.TYPE_ANY, null, null, null);

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
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public static class ModelResult {
		public Object			data;
		public ServiceResponse	serviceResponse	= new ServiceResponse();
	}

	static class WifiScanResult {

		String			BSSID;
		String			SSID;
		int				level	= 0;
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

	public static enum ArrayListType {
		TunedPlaces, SyntheticPlaces, OwnedByUser, Collections, InCollection
	}

	public static enum ScanReason {
		query,
		monitoring
	}

}