package com.aircandi.components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.BuildConfig;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestType;
import com.aircandi.service.HttpService.ResponseFormat;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.HttpServiceException;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.LinkType;
import com.aircandi.service.objects.Observation;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.DateUtils;
import com.aircandi.utilities.ImageUtils;
import com.google.android.gcm.GCMRegistrar;

public class ProxiManager {

	private Context						mContext;
	final EntityModel					mEntityModel			= new EntityModel(this);
	private final AtomicBoolean			mScanRequestActive		= new AtomicBoolean(false);

	public Date							mLastWifiUpdate;
	private WifiManager					mWifiManager;
	private static final WifiScanResult	mWifiMassenaUpper		= new WifiScanResult("00:1c:b3:ae:bf:f0", "test_massena_upper", -50, true);
	private static final WifiScanResult	mWifiMassenaLower		= new WifiScanResult("00:1c:b3:ae:bb:57", "test_massena_lower", -50, true);
	private static final WifiScanResult	mWifiMassenaLowerStrong	= new WifiScanResult("00:1c:b3:ae:bb:57", "test_massena_lower_strong", -20, true);
	private static final WifiScanResult	mWifiMassenaLowerWeak	= new WifiScanResult("00:1c:b3:ae:bb:57", "test_massena_lower_weak", -100, true);
	private static final WifiScanResult	mWifiEmpty				= new WifiScanResult("aa:aa:bb:bb:cc:cc", "test_empty", -50, true);

	private ProxiManager() {}

	private static class ProxiManagerHolder {
		public static final ProxiManager	instance	= new ProxiManager();
	}

	public static ProxiManager getInstance() {
		return ProxiManagerHolder.instance;
	}

	public void initialize() {
		if (!Aircandi.getInstance().isUsingEmulator()) {
			mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		}
	}

	public void scanForWifi(final ScanReason reason) {
		/*
		 * If context is null then we probably crashed and the scan service is still calling.
		 */
		if (mContext == null) {
			return;
		}

		synchronized (mEntityModel.getWifiList()) {

			if (!Aircandi.getInstance().isUsingEmulator()) {

				mContext.registerReceiver(new BroadcastReceiver() {

					@Override
					public void onReceive(Context context, Intent intent) {

						Logger.v(ProxiManager.this, "Received wifi scan results for " + reason.name());
						mContext.unregisterReceiver(this);

						/* Get the latest scan results */
						mEntityModel.getWifiList().clear();

						for (ScanResult scanResult : mWifiManager.getScanResults()) {
							mEntityModel.getWifiList().add(new WifiScanResult(scanResult));
						}

						final String testingBeacons = Aircandi.settings.getString(CandiConstants.PREF_TESTING_BEACONS, "natural");

						if (!ListPreferenceMultiSelect.contains("natural", testingBeacons, null)) {
							mEntityModel.getWifiList().clear();
						}

						if (ListPreferenceMultiSelect.contains("massena_upper", testingBeacons, null)) {
							mEntityModel.getWifiList().add(mWifiMassenaUpper);
						}

						if (ListPreferenceMultiSelect.contains("massena_lower", testingBeacons, null)) {
							mEntityModel.getWifiList().add(mWifiMassenaLower);
						}

						if (ListPreferenceMultiSelect.contains("massena_lower_strong", testingBeacons, null)) {
							mEntityModel.getWifiList().add(mWifiMassenaLowerStrong);
						}

						if (ListPreferenceMultiSelect.contains("massena_lower_weak", testingBeacons, null)) {
							mEntityModel.getWifiList().add(mWifiMassenaLowerWeak);
						}

						if (ListPreferenceMultiSelect.contains("empty", testingBeacons, null)) {
							mEntityModel.getWifiList().add(mWifiEmpty);
						}
						Collections.sort(mEntityModel.getWifiList(), new WifiScanResult.SortWifiBySignalLevel());

						mLastWifiUpdate = DateUtils.nowDate();
						if (reason == ScanReason.monitoring) {
							BusProvider.getInstance().post(new MonitoringWifiScanReceivedEvent(mEntityModel.getWifiList()));
						}
						else if (reason == ScanReason.query) {
							BusProvider.getInstance().post(new QueryWifiScanReceivedEvent(mEntityModel.getWifiList()));
						}
						mScanRequestActive.set(false);

					}
				}, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

				mWifiManager.startScan();
			}
			else {
				mEntityModel.getWifiList().clear();
				Logger.d(ProxiManager.this, "Emulator enabled so using dummy scan results");
				mEntityModel.getWifiList().add(mWifiMassenaUpper);
				if (reason == ScanReason.monitoring) {
					BusProvider.getInstance().post(new MonitoringWifiScanReceivedEvent(mEntityModel.getWifiList()));
				}
				else if (reason == ScanReason.query) {
					BusProvider.getInstance().post(new QueryWifiScanReceivedEvent(mEntityModel.getWifiList()));
				}
			}
		}
	}

	public void lockBeacons() {
		mEntityModel.updateBeacons();
	}

	// --------------------------------------------------------------------------------------------
	// Public entry points for service calls
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
		final Bundle parameters = new Bundle();
		ServiceResponse serviceResponse = new ServiceResponse();

		/* Construct string array of the beacon ids */
		final List<String> beaconIds = new ArrayList<String>();
		synchronized (mEntityModel.getBeacons()) {
			for (Beacon beacon : mEntityModel.getBeacons()) {
				beaconIds.add(beacon.id);
			}
		}

		/* Add current registrationId */
		String registrationId = GCMRegistrar.getRegistrationId(Aircandi.applicationContext);
		if (registrationId != null) {
			parameters.putString("registrationId", registrationId);
		}

		if (beaconIds.size() == 0) {
			mEntityModel.removeBeaconEntities();
		}
		else {
			parameters.putStringArrayList("beaconIdsNew", (ArrayList<String>) beaconIds);

			final ArrayList<Integer> levels = new ArrayList<Integer>();
			for (String beaconId : beaconIds) {
				Beacon beacon = mEntityModel.getBeacon(beaconId);
				levels.add(beacon.level.intValue());
			}
			parameters.putIntegerArrayList("beaconLevels", levels);

			/* Only entities linked by proximity */
			parameters.putString("linkType", "proximity");

			/*
			 * The observation is used two ways:
			 * 1) To include entities that have loc info but are not linked to a beacon
			 * 2) To update the location info for the new beacons if it is better than
			 * what is already stored.
			 */
			final Observation observation = LocationManager.getInstance().getObservationLocked();
			if (observation != null) {
				parameters.putString("observation", "object:" + HttpService.convertObjectToJsonSmart(observation, true, true));
			}

			parameters.putString("eagerLoad", "object:{\"children\":true,\"parents\":false,\"comments\":false}");
			parameters.putString("options", "object:{\"limit\":"
					+ String.valueOf(ProxiConstants.RADAR_ENTITY_LIMIT)
					+ ",\"skip\":0"
					+ ",\"sort\":{\"modifiedDate\":-1} "
					+ ",\"children\":{\"limit\":"
					+ String.valueOf(ProxiConstants.RADAR_CHILDENTITY_LIMIT)
					+ ",\"skip\":0"
					+ ",\"sort\":{\"modifiedDate\":-1}}"
					+ "}");
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForLocation")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		Aircandi.stopwatch1.segmentTime("Entities for beacons: service call started");
		serviceResponse = dispatch(serviceRequest);

		Aircandi.stopwatch1.segmentTime("Entities for beacons: service call complete");
		if (serviceResponse.responseCode == ResponseCode.Success) {

			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
			serviceResponse.data = serviceData;

			final List<Entity> entities = (List<Entity>) serviceData.data;
			Aircandi.stopwatch1.segmentTime("Entities for beacons: objects deserialized");

			if (entities.size() > 0) {

				/*
				 * Make sure we don't have duplicates keyed on sourceId because
				 * getPlacesNearLocation could have already completed.
				 */
				synchronized (mEntityModel.mEntityCache) {
					for (Entity entity : entities) {
						if (entity.place != null) {
							if (mEntityModel.mEntityCache.containsKey(entity.place.getProvider().id)) {
								mEntityModel.mEntityCache.remove(entity.place.getProvider().id);
							}
						}
					}
				}

				/* Merge entities into data model */
				mEntityModel.upsertEntities(entities);
				manageEntityVisibility();
			}
			
			mEntityModel.setLastBeaconRefreshDate(serviceData.date.longValue());

			/* All cached place entities that qualify based on current distance pref setting */
			final List<Entity> entitiesForEvent = ProxiManager.getInstance().getEntityModel().getAllPlaces(false);
			Aircandi.stopwatch1.segmentTime("Entities for beacons: objects processed");

			BusProvider.getInstance().post(new EntitiesForBeaconsFinishedEvent());
			BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent));
		}

		return serviceResponse;
	}

	public synchronized ServiceResponse getPlacesNearLocation(Observation observation) {

		ServiceResponse serviceResponse = new ServiceResponse();
		final Bundle parameters = new Bundle();
		/*
		 * We find all aircandi place entities in the cache (via proximity or location) that are active based
		 * on the current search parameters (beacons and search radius) and could be supplied by the place provider. We
		 * create an array of the provider place id's and pass them so they can be excluded from the places
		 * that get returned.
		 */
		final List<String> excludePlaceIds = new ArrayList<String>();
		for (Entity entity : mEntityModel.getAircandiPlaces()) {
			excludePlaceIds.add(entity.id);
			if (!entity.place.getProvider().type.equals("user")) {
				excludePlaceIds.add(entity.place.getProvider().id);
			}
		}

		if (excludePlaceIds.size() > 0) {
			parameters.putStringArrayList("excludePlaceIds", (ArrayList<String>) excludePlaceIds);
		}

		final String placeProvider = Aircandi.settings.getString(CandiConstants.PREF_TESTING_PLACE_PROVIDER, "foursquare");

		parameters.putString("provider", placeProvider);
		parameters.putFloat("latitude", observation.latitude.floatValue());
		parameters.putFloat("longitude", observation.longitude.floatValue());
		parameters.putInt("limit", ProxiConstants.RADAR_PLACES_LIMIT);

		final Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(CandiConstants.PREF_SEARCH_RADIUS,
				CandiConstants.PREF_SEARCH_RADIUS_DEFAULT));
		parameters.putInt("radius", searchRangeMeters);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getPlacesNearLocation")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		serviceResponse = dispatch(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.Success) {

			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
			serviceResponse.data = serviceData;

			/* FIXME: Checks to help find null pointer bug */
			if (serviceData == null) {
				throw new RuntimeException("ServiceData object is null");
			}
			else if (serviceData.data == null) {
				throw new RuntimeException("ServiceData.data property is null");
			}

			Float maxDistance = 0f;

			/* Do a bit of fixup */
			final List<Entity> entities = (List<Entity>) serviceData.data;
			for (Entity entity : entities) {
				/* No id means it's a synthetic */
				if (entity.id == null) {
					entity.id = entity.place.getProvider().id;
					entity.modifiedDate = DateUtils.nowDate().getTime();
					entity.synthetic = true;
				}
				else {
					entity.synthetic = false;
				}
			}

			/* Places locked in by proximity trump places locked in by location */
			final List<Entity> proximityPlaces = mEntityModel.getProximityPlaces();

			Iterator<Entity> iterProximityPlaces = proximityPlaces.iterator();
			Iterator<Entity> iterLocationPlaces = entities.iterator();

			while (iterLocationPlaces.hasNext()) {
				Entity locPlace = iterLocationPlaces.next();

				while (iterProximityPlaces.hasNext()) {
					Entity proxPlace = iterProximityPlaces.next();

					if (proxPlace.id.equals(locPlace.id)) {
						iterLocationPlaces.remove();
					}
					else if (!proxPlace.place.getProvider().type.equals("user")) {
						if (proxPlace.place.getProvider().id.equals(locPlace.id)) {
							iterLocationPlaces.remove();
						}
					}
				}
			}

			/* Find the place with the maximum distance */
			for (Entity entity : entities) {
				float distance = entity.getDistance(); // In meters
				if (distance > maxDistance) {
					maxDistance = distance;
				}
			}

			mEntityModel.removeSyntheticEntities();
			mEntityModel.upsertEntities(entities);

			final List<Entity> entitiesForEvent = ProxiManager.getInstance().getEntityModel().getAllPlaces(false);

			BusProvider.getInstance().post(new PlacesNearLocationFinishedEvent());
			BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent));
		}
		return serviceResponse;
	}

	ServiceResponse dispatch(ServiceRequest serviceRequest) {
		/*
		 * We use this as a choke point for all calls to the aircandi service.
		 */
		final ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		return serviceResponse;
	}

	public ModelResult upsizeSynthetic(Entity synthetic, List<Beacon> beacons, Beacon primaryBeacon) {
		final Entity entity = Entity.upsizeFromSynthetic(synthetic);
		ModelResult result = ProxiManager.getInstance().getEntityModel().insertEntity(entity
				, beacons
				, primaryBeacon
				, entity.getPhoto().getBitmap()
				, false
				, null);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			/*
			 * Success so remove the synthetic entity.
			 */
			ProxiManager.getInstance().getEntityModel().removeEntity(synthetic.id);
			/*
			 * Cached beacons come from the beacon scan process so tuning won't add them
			 * so we need to do it here.
			 */
			if (primaryBeacon != null) {
				if (mEntityModel.getBeacon(primaryBeacon.id) == null) {
					/*
					 * Insert beacon in service. Could fail because it
					 * is already in the beacons collection.
					 */
					result = mEntityModel.insertBeacon(primaryBeacon);
					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						mEntityModel.upsertBeacon((Beacon) result.data);
					}
				}
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------------------------
	// Entity management
	// --------------------------------------------------------------------------------------------

	private void manageEntityVisibility() {

		/* Visibility status effects all entities regardless of whether this is a full or partial update. */
		Logger.v(this, "Managing entity visibility");

		for (Beacon beacon : mEntityModel.getBeacons()) {
			synchronized (beacon) {
				for (Entity entity : beacon.getEntities()) {
					setEntityVisibility(entity, beacon);

					/* Push hidden down to children */
					for (Entity childEntity : entity.getChildren()) {
						childEntity.hidden = entity.hidden;

						/* If child is going to inherit visibility then perform its own personal visibility check. */
						if (!childEntity.hidden) {
							setEntityVisibility(childEntity, beacon);
						}
					}
				}
			}
		}
	}

	private void setEntityVisibility(Entity entity, Beacon beacon) {
		boolean oldIsHidden = entity.hidden;
		entity.hidden = false;
		/*
		 * Make it harder to fade out than it is to fade in. Entities are only New for the first scan that discovers
		 * them.
		 */
		float signalThresholdFluid = entity.signalFence.floatValue();
		if (!oldIsHidden && entity.getActiveBeaconPrimary(LinkType.proximity.name()) != null) {
			signalThresholdFluid = entity.signalFence.floatValue() - 5;
		}

		/* Hide entities that are not within entity declared virtual range */
		if (Aircandi.settings.getBoolean(CandiConstants.PREF_ENABLE_DEV, CandiConstants.PREF_ENABLE_DEV_DEFAULT)
				&& Aircandi.settings.getBoolean(CandiConstants.PREF_ENTITY_FENCING, CandiConstants.PREF_ENTITY_FENCING_DEFAULT)
				&& beacon.level.intValue() < signalThresholdFluid) {
			entity.hidden = true;
			return;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	public Boolean refreshNeeded(Location activeLocation) {
		if (mEntityModel.getLastBeaconRefreshDate() != null) {
			final Long interval = DateUtils.nowDate().getTime() - mEntityModel.getLastBeaconRefreshDate().longValue();
			if (interval > CandiConstants.INTERVAL_REFRESH) {
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
		for (Beacon beacon : mEntityModel.getBeacons()) {
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

	Entity loadEntityFromResources(Integer entityResId) {
		InputStream inputStream = null;
		BufferedReader reader = null;
		try {
			inputStream = mContext.getResources().openRawResource(entityResId);
			reader = new BufferedReader(new InputStreamReader(inputStream));
			final StringBuilder text = new StringBuilder(10000);
			String line;
			while ((line = reader.readLine()) != null) {
				text.append(line);
			}
			final String jsonEntity = text.toString();
			final Entity entity = (Entity) HttpService.convertJsonToObjectInternalSmart(jsonEntity, ServiceDataType.Entity);
			return entity;
		}
		catch (IOException exception) {
			return null;
		}
		finally {
			try {
				inputStream.close();
				reader.close();
			}
			catch (IOException e) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace();
				}
			}
		}
	}

	ServiceResponse storeImageAtS3(Entity entity, User user, Bitmap bitmap) {
		/*
		 * TODO: We are going with a garbage collection scheme for orphaned
		 * images. We need to use an extended property on S3 items that is set to a date when collection is ok. This
		 * allows downloaded entities to keep working even if an image for entity has changed.
		 */

		/* Make sure the bitmap is less than or equal to the maximum size we want to persist. */
		bitmap = ImageUtils.ensureBitmapScaleForS3(bitmap);

		/*
		 * Push it to S3. It is always formatted/compressed as a jpeg.
		 */
		try {
			final String stringDate = DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME);
			final String imageKey = String.valueOf((user != null) ? user.id : Aircandi.getInstance().getUser().id) + "_" + stringDate + ".jpg";
			S3.putImage(imageKey, bitmap, CandiConstants.IMAGE_QUALITY_S3);

			/* Update the photo object for the entity or user */
			if (entity != null) {
				entity.photo = new Photo(imageKey, null, bitmap.getWidth(), bitmap.getHeight(), PhotoSource.aircandi);
			}
			else if (user != null) {
				user.photo = new Photo(imageKey, null, bitmap.getWidth(), bitmap.getHeight(), PhotoSource.aircandi);
			}
		}
		catch (HttpServiceException exception) {
			return new ServiceResponse(ResponseCode.Failed, null, exception);
		}

		return new ServiceResponse();
	}

	// --------------------------------------------------------------------------------------------
	// Setters/getters
	// --------------------------------------------------------------------------------------------

	public Context getContext() {
		return mContext;
	}

	public void setContext(Context context) {
		mContext = context;
	}

	public EntityModel getEntityModel() {
		return mEntityModel;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
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