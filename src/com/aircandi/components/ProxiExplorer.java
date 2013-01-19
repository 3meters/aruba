package com.aircandi.components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.ProxiConstants;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.ProxibaseServiceException;
import com.aircandi.service.Query;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Beacon.BeaconType;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Comment;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Link.LinkType;
import com.aircandi.service.objects.Observation;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.ServiceEntry;
import com.aircandi.service.objects.Source;
import com.aircandi.service.objects.User;
import com.aircandi.ui.CandiRadar;
import com.aircandi.ui.Preferences;
import com.aircandi.utilities.DateUtils;
import com.aircandi.utilities.ImageUtils;

public class ProxiExplorer {

	private static ProxiExplorer	singletonObject;
	private Context					mContext;
	private EntityModel				mEntityModel			= new EntityModel();

	private AtomicBoolean			mScanRequestActive		= new AtomicBoolean(false);

	public List<WifiScanResult>		mWifiList				= new ArrayList<WifiScanResult>();
	public Date						mLastWifiUpdate;
	private WifiManager				mWifiManager;
	private boolean					mUsingEmulator			= false;

	private static WifiScanResult	mWifiMassenaUpper		= new WifiScanResult("00:1c:b3:ae:bf:f0", "test_massena_upper", -50, true);
	private static WifiScanResult	mWifiMassenaLower		= new WifiScanResult("00:1c:b3:ae:bb:57", "test_massena_lower", -50, true);
	private static WifiScanResult	mWifiMassenaLowerStrong	= new WifiScanResult("00:1c:b3:ae:bb:57", "test_massena_lower_strong", -20, true);
	private static WifiScanResult	mWifiMassenaLowerWeak	= new WifiScanResult("00:1c:b3:ae:bb:57", "test_massena_lower_weak", -100, true);
	private static WifiScanResult	mWifiEmpty				= new WifiScanResult("aa:aa:bb:bb:cc:cc", "test_empty", -50, true);

	public static synchronized ProxiExplorer getInstance() {
		if (singletonObject == null) {
			singletonObject = new ProxiExplorer();
		}
		return singletonObject;
	}

	private ProxiExplorer() {}

	public void initialize() {
		if (!mUsingEmulator) {
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

		Aircandi.stopwatch2.start("Wifi scan");
		synchronized (mWifiList) {

			if (!mUsingEmulator) {

				mContext.registerReceiver(new BroadcastReceiver() {

					@Override
					public void onReceive(Context context, Intent intent) {

							Aircandi.stopwatch2.segmentTime("Wifi scan: results received");
						Logger.v(ProxiExplorer.this, "Received wifi scan results for " + reason.name().toString());
						mContext.unregisterReceiver(this);

						/* Get the latest scan results */
						mWifiList.clear();

						for (ScanResult scanResult : mWifiManager.getScanResults()) {
							mWifiList.add(new WifiScanResult(scanResult));
						}

						String testingBeacons = Aircandi.settings.getString(Preferences.PREF_TESTING_BEACONS, "natural");

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
						Aircandi.stopwatch2.segmentTime("Wifi scan: objects created");
						Collections.sort(mWifiList, new WifiScanResult.SortWifiBySignalLevel());
						Aircandi.stopwatch2.segmentTime("Wifi scan: objects sorted");

						mLastWifiUpdate = DateUtils.nowDate();
						if (reason == ScanReason.monitoring) {
							BusProvider.getInstance().post(new MonitoringWifiScanReceivedEvent(mWifiList));
						}
						else if (reason == ScanReason.query) {
							BusProvider.getInstance().post(new QueryWifiScanReceivedEvent(mWifiList));
						}
						Aircandi.stopwatch2.segmentTime("Wifi scan: bus events sent");
						mScanRequestActive.set(false);
						Aircandi.stopwatch2.stop("Wifi scan: routine finished");

					}
				}, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

				Aircandi.stopwatch2.segmentTime("Wifi scan: requesting");
				mWifiManager.startScan();
			}
			else {
				mWifiList.clear();
				Logger.d(ProxiExplorer.this, "Emulator enabled so using dummy scan results");
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
		mEntityModel.updateBeacons();
	}

	// --------------------------------------------------------------------------------------------
	// Public entry points for service calls
	// --------------------------------------------------------------------------------------------

	public void getEntitiesForBeacons() {
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
		Aircandi.stopwatch3.start("Entities for beacons");
		Logger.d(this, "Processing beacons from scan");

		/*
		 * Call the proxi service to see if the new beacons have been tagged with any entities. If call comes back
		 * null then there was a network or service problem. The user got a toast notification from the service. We
		 * are making synchronous calls inside an asynchronous thread.
		 */

		/* Construct string array of the beacon ids */
		ArrayList<String> beaconIds = new ArrayList<String>();
		for (Beacon beacon : mEntityModel.getBeacons()) {
			beaconIds.add(beacon.id);
		}

		ServiceResponse serviceResponse = new ServiceResponse();

		if (beaconIds.size() == 0) {
			mEntityModel.removeBeaconEntities();
			mEntityModel.setLastRefreshDate(DateUtils.nowDate().getTime());
			List<Entity> entitiesForEvent = ProxiExplorer.getInstance().getEntityModel().getPlaces();
			BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent));
			return;
		}

		/* Set method parameters */
		Bundle parameters = new Bundle();
		if (beaconIds.size() > 0) {
			parameters.putStringArrayList("beaconIdsNew", beaconIds);
			ArrayList<Integer> levels = new ArrayList<Integer>();
			for (String beaconId : beaconIds) {
				Beacon beacon = mEntityModel.getBeacon(beaconId);
				levels.add(beacon.level.intValue());
			}
			parameters.putIntegerArrayList("beaconLevels", levels);
		}

		/*
		 * The observation is used two ways:
		 * 1) To include entities that have loc info but are not linked to a beacon
		 * 2) To update the location info for the new beacons if it is better than
		 * what is already stored.
		 */
		Observation observation = LocationManager.getInstance().getObservation();
		if (observation != null) {
			parameters.putString("observation"
					, "object:" + ProxibaseService.convertObjectToJsonSmart(observation, true, true));
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

		ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForLocation")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		Aircandi.stopwatch3.segmentTime("Entities for beacons: service call started");
		serviceResponse = dispatch(serviceRequest, false);

		Aircandi.stopwatch3.segmentTime("Entities for beacons: service call complete");
		if (serviceResponse.responseCode == ResponseCode.Success) {

			String jsonResponse = (String) serviceResponse.data;
			ServiceData serviceData = (ServiceData) ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
			serviceResponse.data = serviceData;

			List<Entity> entities = (List<Entity>) serviceData.data;
			Aircandi.stopwatch3.segmentTime("Entities for beacons: objects deserialized");

			/* Merge entities into data model */
			mEntityModel.removeBeaconEntities();
			mEntityModel.upsertEntities(entities);

			mEntityModel.setLastRefreshDate(serviceData.date.longValue());
			manageEntityVisibility();

			List<Entity> entitiesForEvent = ProxiExplorer.getInstance().getEntityModel().getPlaces();
			Aircandi.stopwatch3.segmentTime("Entities for beacons: objects processed");

			BusProvider.getInstance().post(new EntitiesForBeaconsFinishedEvent());
			BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent));
		}

		Aircandi.stopwatch3.stop("Entities for beacons: finished");

		return;
	}

	public ServiceResponse getEntitiesForLocation() {

		ServiceResponse serviceResponse = new ServiceResponse();

		/* Set method parameters */
		Bundle parameters = new Bundle();

		Observation observation = LocationManager.getInstance().getObservation();
		if (observation != null) {
			parameters.putString("observation", "object:" + ProxibaseService.convertObjectToJsonSmart(observation, true, true));
			Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(Preferences.PREF_SEARCH_RADIUS,
					Aircandi.applicationContext.getString(R.string.search_radius_default)));
			parameters.putFloat("radius", LocationManager.getRadiusForMeters((float) searchRangeMeters));
		}

		/* We don't want to fetch entities we already have via links to local beacons */
		ArrayList<String> excludeEntityIds = new ArrayList<String>();
		for (Entity entity : mEntityModel.getRadarPlaces()) {
			if (!entity.synthetic
					&& entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)
					&& entity.links != null
					&& entity.links.size() > 0) {
				excludeEntityIds.add(entity.id);
			}
		}

		if (excludeEntityIds.size() > 0) {
			parameters.putStringArrayList("excludeEntityIds", excludeEntityIds);
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

		ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForLocation")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		serviceResponse = dispatch(serviceRequest, false);

		if (serviceResponse.responseCode == ResponseCode.Success) {

			String jsonResponse = (String) serviceResponse.data;
			ServiceData serviceData = (ServiceData) ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
			serviceResponse.data = serviceData;

			List<Entity> entities = (List<Entity>) serviceData.data;
			/*
			 * These were found purely using location but they can still come with
			 * links to beacons not currently visible. To keep things clean, we
			 * make sure the links without beacons are stripped.
			 */
			for (Entity entity : entities) {
				if (entity.links != null) {
					for (int i = entity.links.size() - 1; i >= 0; i--) {
						Beacon beacon = mEntityModel.getBeacon(entity.links.get(i).toId);
						if (beacon == null) {
							entity.links.remove(i);
						}
					}
				}
			}

			/* Merge entities into data model */
			mEntityModel.removeLocationEntities();
			mEntityModel.upsertEntities(entities);

			List<Entity> entitiesForEvent = ProxiExplorer.getInstance().getEntityModel().getPlaces();

			BusProvider.getInstance().post(new EntitiesForLocationFinishedEvent());
			BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent));
		}
		return serviceResponse;
	}

	public void getPlacesNearLocation(Observation observation) {
		ServiceResponse serviceResponse = new ServiceResponse();
		/*
		 * Make a list of places that should be excluded because
		 * we already have entities for them.
		 */
		ArrayList<String> excludePlaceIds = new ArrayList<String>();
		for (Entity entity : mEntityModel.getRadarPlaces()) {
			if (!entity.synthetic && entity.type.equals(CandiConstants.TYPE_CANDI_PLACE) && !entity.place.source.equals("user")) {
				excludePlaceIds.add(entity.place.sourceId);
			}
		}

		Bundle parameters = new Bundle();
		if (excludePlaceIds.size() > 0) {
			parameters.putStringArrayList("excludePlaceIds", excludePlaceIds);
		}
		parameters.putString("source", PlaceSources.foursquare.name());
		parameters.putFloat("latitude", observation.latitude.floatValue());
		parameters.putFloat("longitude", observation.longitude.floatValue());
		parameters.putInt("limit", ProxiConstants.RADAR_PLACES_LIMIT);
		Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(Preferences.PREF_SEARCH_RADIUS,
				Aircandi.applicationContext.getString(R.string.search_radius_default)));
		parameters.putInt("radius", searchRangeMeters);

		ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getPlacesNearLocation")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		serviceResponse = dispatch(serviceRequest, false);

		if (serviceResponse.responseCode == ResponseCode.Success) {
			String jsonResponse = (String) serviceResponse.data;
			ServiceData serviceData = (ServiceData) ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
			serviceResponse.data = serviceData;

			/* Do a bit of fixup */
			List<Entity> entities = (List<Entity>) serviceData.data;
			for (Entity entity : entities) {
				entity.modifiedDate = DateUtils.nowDate().getTime();
				entity.synthetic = true;
			}
			mEntityModel.removeSyntheticEntities();
			mEntityModel.upsertEntities(entities);

			List<Entity> entitiesForEvent = ProxiExplorer.getInstance().getEntityModel().getPlaces();

			BusProvider.getInstance().post(new PlacesNearLocationFinishedEvent());
			BusProvider.getInstance().post(new EntitiesChangedEvent(entitiesForEvent));
		}
		return;
	}

	private ServiceResponse dispatch(ServiceRequest serviceRequest, Boolean skipUpdateCheck) {
		/*
		 * We use this as a choke point for all calls to the aircandi service.
		 */
		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		return serviceResponse;
	}

	public ModelResult upsizeSynthetic(Entity synthetic, List<Beacon> beacons, Beacon primaryBeacon) {
		Entity entity = Entity.upsizeFromSynthetic(synthetic);
		ModelResult result = ProxiExplorer.getInstance().getEntityModel().insertEntity(entity
				, beacons
				, primaryBeacon
				, entity.getPhoto().getBitmap()
				, false);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			/*
			 * Success so remove the synthetic entity.
			 */
			ProxiExplorer.getInstance().getEntityModel().removeEntity(synthetic.id);
			/*
			 * Cached beacons come from the beacon scan process so tuning won't add them
			 * so we need to do it here.
			 */
			if (primaryBeacon != null) {
				if (mEntityModel.getBeacon(primaryBeacon.id) == null) {
					result = mEntityModel.insertBeacon(primaryBeacon);
					mEntityModel.upsertBeacon((Beacon) result.data);
				}
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------------------------
	// Entity management
	// --------------------------------------------------------------------------------------------

	public void manageEntityVisibility() {

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
		if (oldIsHidden == false && entity.getActiveBeacon(LinkType.proximity.name()) != null) {
			signalThresholdFluid = entity.signalFence.floatValue() - 5;
		}

		/* Hide entities that are not within entity declared virtual range */
		if (Aircandi.settings.getBoolean(Preferences.PREF_ENABLE_DEV, true)
				&& Aircandi.settings.getBoolean(Preferences.PREF_ENTITY_FENCING, true)
				&& beacon.level.intValue() < signalThresholdFluid) {
			entity.hidden = true;
			return;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	public Boolean updateCheckNeeded() {
		Boolean doUpdateCheck = false;
		if (Aircandi.lastApplicationUpdateCheckDate == null) {
			doUpdateCheck = true;
			Logger.v(this, "Update check needed: first check");
		}
		else if ((DateUtils.nowDate().getTime() - Aircandi.lastApplicationUpdateCheckDate.longValue()) > CandiConstants.INTERVAL_UPDATE_CHECK) {
			doUpdateCheck = true;
			Logger.v(this, "Update check needed: past internal");
		}
		else {
			String interval = DateUtils.timeSince(Aircandi.lastApplicationUpdateCheckDate.longValue(), DateUtils.nowDate().getTime());
			Logger.v(this, "No update check needed: Last check " + interval);
		}
		return doUpdateCheck;
	}

	public ModelResult checkForUpdate() {

		Logger.v(this, "Checking for update");
		ModelResult result = new ModelResult();

		Aircandi.applicationUpdateNeeded = false;
		Aircandi.applicationUpdateRequired = false;
		Query query = new Query("documents").filter("{\"type\":\"version\",\"name\":\"aircandi\"}");

		ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST)
				.setRequestType(RequestType.Get)
				.setQuery(query)
				.setSuppressUI(true)
				.setResponseFormat(ResponseFormat.Json);

		/*
		 * This causes the user session expiration window to get bumped
		 * if we are within a week of expiration.
		 */
		if (Aircandi.getInstance().getUser() != null) {
			serviceRequest.setSession(Aircandi.getInstance().getUser().session);
		}

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {

			String jsonResponse = (String) result.serviceResponse.data;
			final ServiceEntry serviceEntry = (ServiceEntry) ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.ServiceEntry).data;
			HashMap<String, Object> map = serviceEntry.data;
			Boolean enabled = (Boolean) map.get("enabled");
			String versionName = (String) map.get("versionName");

			String currentVersionName = Aircandi.getVersionName(mContext, CandiRadar.class);

			if (enabled && !currentVersionName.equals(versionName)) {
				Logger.i(ProxiExplorer.this, "Update check: update needed");
				Aircandi.applicationUpdateNeeded = true;

				String updateUri = (String) map.get("updateUri");
				Boolean updateRequired = (Boolean) map.get("updateRequired");

				Aircandi.applicationUpdateUri = updateUri != null ? updateUri : CandiConstants.URL_AIRCANDI_UPGRADE;
				if (updateRequired) {
					Aircandi.applicationUpdateRequired = true;
					Logger.i(ProxiExplorer.this, "Update check: update required");
				}
			}
			Aircandi.lastApplicationUpdateCheckDate = DateUtils.nowDate().getTime();
		}
		return result;
	}

	public List<Beacon> getStrongestBeacons(int max) {

		List<Beacon> beaconStrongest = new ArrayList<Beacon>();
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
		List<Beacon> beacons = getStrongestBeacons(1);
		if (beacons.size() > 1) {
			return beacons.get(0);
		}
		return null;
	}

	public Entity loadEntityFromResources(Integer entityResId) {
		try {
			InputStream inputStream = mContext.getResources().openRawResource(entityResId);
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			StringBuilder text = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				text.append(line);
			}
			String jsonEntity = text.toString();
			Entity entity = (Entity) ProxibaseService.convertJsonToObjectInternalSmart(jsonEntity, ServiceDataType.Entity);
			return entity;
		}
		catch (IOException exception) {
			return null;
		}
	}

	private ServiceResponse storeImageAtS3(Entity entity, User user, Bitmap bitmap) {
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
			String stringDate = DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME);
			String imageKey = String.valueOf(user != null ? user.id : Aircandi.getInstance().getUser().id) + "_" + stringDate + ".jpg";
			S3.putImage(imageKey, bitmap, CandiConstants.IMAGE_QUALITY_S3);

			/* Update the photo object for the entity or user */
			if (entity != null) {
				entity.getPhotoForSet().setImageUri(imageKey, null, bitmap.getWidth(), bitmap.getHeight());
			}
			else if (user != null) {
				user.getPhotoForSet().setImageUri(imageKey, null, bitmap.getWidth(), bitmap.getHeight());
			}
		}
		catch (ProxibaseServiceException exception) {
			return new ServiceResponse(ResponseCode.Failed, null, exception);
		}

		return new ServiceResponse();
	}

	// --------------------------------------------------------------------------------------------
	// Setters/getters
	// --------------------------------------------------------------------------------------------

	public Context getContext() {
		return this.mContext;
	}

	public void setContext(Context context) {
		this.mContext = context;
	}

	public void setUsingEmulator(boolean usingEmulator) {
		mUsingEmulator = usingEmulator;
	}

	public boolean isUsingEmulator() {
		return mUsingEmulator;
	}

	public EntityModel getEntityModel() {
		return mEntityModel;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public class EntityModel {

		private HashMap<String, Entity>	mEntityCache		= new HashMap<String, Entity>();
		private List<Beacon>			mBeacons			= new ArrayList<Beacon>();
		private List<Photo>				mPhotos				= new ArrayList<Photo>();
		private List<Category>			mCategories			= new ArrayList<Category>();

		private Number					mLastRefreshDate;
		private Number					mLastActivityDate	= DateUtils.nowDate().getTime();

		public EntityModel() {}

		// --------------------------------------------------------------------------------------------
		// Combo service/cache queries
		// --------------------------------------------------------------------------------------------

		public ModelResult getUserEntities(String userId, Boolean refresh, Integer limit) {
			EntityList<Entity> entities = new EntityList<Entity>();
			ModelResult result = new ModelResult();

			if (refresh) {

				Bundle parameters = new Bundle();
				parameters.putString("userId", userId);
				parameters.putString("eagerLoad", "object:{\"children\":true,\"parents\":false,\"comments\":false}");
				parameters.putString("fields", "object:{\"entities\":{},\"comments\":{},\"children\":{},\"parents\":{}}");
				parameters.putString("options", "object:{\"limit\":"
						+ String.valueOf(limit)
						+ ",\"skip\":0"
						+ ",\"sort\":{\"modifiedDate\":-1} "
						+ ",\"children\":{\"limit\":"
						+ String.valueOf(ProxiConstants.RADAR_CHILDENTITY_LIMIT)
						+ ",\"skip\":0"
						+ ",\"sort\":{\"modifiedDate\":-1}}"
						+ "}");

				ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForUser")
						.setRequestType(RequestType.Method)
						.setParameters(parameters)
						.setResponseFormat(ResponseFormat.Json);

				result.serviceResponse = dispatch(serviceRequest, false);

				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					String jsonResponse = (String) result.serviceResponse.data;
					ServiceData serviceData = ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
					List<Entity> entityList = (List<Entity>) serviceData.data;
					entities.addAll(entityList);

					upsertEntities(entities);
					result.data = getUserEntities(userId);
				}
			}
			else {
				result.data = getUserEntities(userId);
			}
			return result;
		}

		/**
		 * Retrieves entity from cache if available otherwise downloads the entity from the service. If refresh is true
		 * then bypasses the cache and downloads from the service.
		 * 
		 * @param entityId
		 * @param refresh
		 * @param jsonEagerLoad
		 * @param jsonOptions
		 * @return
		 */
		public ModelResult getEntity(String entityId, Boolean refresh, String jsonEagerLoad, String jsonOptions) {
			List<String> entityIds = new ArrayList<String>();
			entityIds.add(entityId);
			ModelResult result = getEntities(entityIds, refresh, jsonEagerLoad, jsonOptions);
			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				List<Entity> entities = (List<Entity>) result.data;
				result.data = entities.get(0);
			}
			return result;
		}

		public ModelResult getEntities(List<String> entityIds, Boolean refresh, String jsonEagerLoad, String jsonOptions) {
			ModelResult result = new ModelResult();

			ArrayList<String> getEntityIds = new ArrayList<String>();

			for (String entityId : entityIds) {
				Entity entity = getCacheEntity(entityId);
				if (refresh || entity == null) {
					getEntityIds.add(entityId);
				}
			}

			if (getEntityIds.size() > 0) {
				if (jsonEagerLoad == null) {
					jsonEagerLoad = "{\"children\":true,\"parents\":false,\"comments\":false}";
				}

				if (jsonOptions == null) {
					jsonOptions = "{\"limit\":"
							+ String.valueOf(ProxiConstants.RADAR_ENTITY_LIMIT)
							+ ",\"skip\":0"
							+ ",\"sort\":{\"modifiedDate\":-1} "
							+ ",\"children\":{\"limit\":"
							+ String.valueOf(ProxiConstants.RADAR_CHILDENTITY_LIMIT)
							+ ",\"skip\":0"
							+ ",\"sort\":{\"modifiedDate\":-1}}"
							+ "}";
				}

				final Bundle parameters = new Bundle();
				parameters.putStringArrayList("entityIds", getEntityIds);
				parameters.putString("eagerLoad", "object:" + jsonEagerLoad);
				parameters.putString("options", "object:" + jsonOptions);

				final ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntities")
						.setRequestType(RequestType.Method)
						.setParameters(parameters)
						.setResponseFormat(ResponseFormat.Json);

				result.serviceResponse = dispatch(serviceRequest, false);

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) result.serviceResponse.data;
					ServiceData serviceData = ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
					List<Entity> fetchedEntities = (List<Entity>) serviceData.data;

					/* Remove any links to beacons not currently visible */
					for (Entity entity : fetchedEntities) {
						if (entity.links != null) {
							for (int i = entity.links.size() - 1; i >= 0; i--) {
								Beacon beacon = getBeacon(entity.links.get(i).toId);
								if (beacon == null) {
									entity.links.remove(i);
								}
							}
						}
					}

					if (fetchedEntities != null && fetchedEntities.size() > 0) {
						upsertEntities(fetchedEntities);
					}
				}
			}

			List<Entity> entities = new ArrayList<Entity>();
			for (String entityId : entityIds) {
				Entity entity = getCacheEntity(entityId);
				entities.add(entity);
			}

			result.data = entities;
			return result;
		}

		public ModelResult getUser(String userId) {
			ModelResult result = new ModelResult();

			final Bundle parameters = new Bundle();
			parameters.putString("userId", userId);

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getUser")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setResponseFormat(ResponseFormat.Json)
					.setSession(Aircandi.getInstance().getUser().session);

			result.serviceResponse = dispatch(serviceRequest, false);

			return result;
		}

		public ModelResult loadCategories() {

			ModelResult result = new ModelResult();

			Bundle parameters = new Bundle();
			parameters.putString("source", "foursquare");

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getPlaceCategories")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = dispatch(serviceRequest, false);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				String jsonResponse = (String) result.serviceResponse.data;
				ServiceData serviceData = (ServiceData) ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Category);
				mCategories = (List<Category>) serviceData.data;
				result.serviceResponse.data = mCategories;
			}
			return result;
		}

		public ModelResult getSourceSuggestions(List<Source> sources) {

			if (sources == null || sources.size() == 0) {
				throw new IllegalArgumentException("sources parameter for getSourceSuggestions is null or size = 0");
			}

			ModelResult result = new ModelResult();

			Bundle parameters = new Bundle();
			parameters.putInt("suggestTimeout", ProxiConstants.SOURCE_SUGGESTIONS_TIMEOUT);
			List<String> sourceStrings = new ArrayList<String>();
			for (Source source : sources) {
				sourceStrings.add("object:" + ProxibaseService.convertObjectToJsonSmart(source, true, true));
			}

			parameters.putStringArrayList("sources", (ArrayList<String>) sourceStrings);

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "suggestSources")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = dispatch(serviceRequest, false);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				String jsonResponse = (String) result.serviceResponse.data;
				ServiceData serviceData = (ServiceData) ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Source);
				result.serviceResponse.data = (List<Category>) serviceData.data;
			}
			return result;
		}

		public ModelResult getPlacePhotos(String source, String sourceId, long count, long offset) {
			ModelResult result = new ModelResult();

			Bundle parameters = new Bundle();
			parameters.putString("source", source);
			parameters.putString("sourceId", sourceId);
			parameters.putLong("limit", count);
			parameters.putLong("skip", offset);

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getPlacePhotos")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = dispatch(serviceRequest, false);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				String jsonResponse = (String) result.serviceResponse.data;
				ServiceData serviceData = (ServiceData) ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Photo);
				List<Photo> photos = (List<Photo>) serviceData.data;
				result.serviceResponse.data = photos;
			}
			return result;

		}

		// --------------------------------------------------------------------------------------------
		// Combo service/cache updates
		// --------------------------------------------------------------------------------------------

		public ModelResult moveEntity(String entityId, String newParentId, Boolean toBeacon, Boolean cacheOnly) {

			ModelResult result = new ModelResult();

			if (!cacheOnly) {
				Entity entity = getCacheEntity(entityId);

				Link link = new Link();
				Bundle parameters = new Bundle();

				link.toId = newParentId;
				link.fromId = entity.id;
				parameters.putString("link", "object:" + ProxibaseService.convertObjectToJsonSmart(link, true, true));
				parameters.putString("originalToId", entity.parentId != null ? entity.parentId : entity.getBeaconId());

				if (link.toId == null || link.fromId == null || parameters.getString("originalToId") == null) {
					throw new IllegalArgumentException("moveEntity: missing id for link update");
				}

				ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "updateLink")
						.setRequestType(RequestType.Method)
						.setResponseFormat(ResponseFormat.Json)
						.setParameters(parameters)
						.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
						.setRetry(false)
						.setSession(Aircandi.getInstance().getUser().session);

				result.serviceResponse = dispatch(serviceRequest, false);
			}

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				moveEntity(entityId, !toBeacon ? newParentId : null, toBeacon ? newParentId : null);
			}

			return result;
		}

		public ModelResult deleteEntity(String entityId, Boolean cacheOnly) {
			ModelResult result = new ModelResult();

			if (!cacheOnly) {
				Entity entity = getCacheEntity(entityId);
				/*
				 * If there is an image stored with S3 then delete it.
				 * TODO: Flag image for garbage collection but don't
				 * delete it because because it might be needed while aircandi users have current sessions.
				 */
				if (entity.getPhoto().getUri() != null
						&& !BitmapManager.isLocalImage(entity.getPhoto().getUri())) {
					try {
						// String imageKey =
						// mCommon.mEntity.imagePreviewUri.substring(mCommon.mEntity.imagePreviewUri.lastIndexOf("/") +
						// 1);
						// S3.deleteImage(imageKey);
						/*
						 * Associated images are removed from the local image cache when the candi model is finally
						 * removed and the cand view is killed or recycled
						 */
					}
					catch (ProxibaseServiceException exception) {
						result.serviceResponse = new ServiceResponse(ResponseCode.Failed, null, exception);
						return result;
					}
				}

				/*
				 * Delete the entity and all links and observations it is associated with. We attempt to continue even
				 * if the call to delete the image failed.
				 */
				Logger.i(this, "Deleting entity: " + entity.name);

				Bundle parameters = new Bundle();
				parameters.putString("entityId", entity.id);
				parameters.putBoolean("deleteChildren", true);

				ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "deleteEntity")
						.setRequestType(RequestType.Method)
						.setParameters(parameters)
						.setSession(Aircandi.getInstance().getUser().session)
						.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
						.setRetry(false)
						.setResponseFormat(ResponseFormat.Json);

				result.serviceResponse = dispatch(serviceRequest, false);
			}

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				removeEntity(entityId);
			}
			return result;
		}

		/**
		 * Inserts the entity in the entity service collection and Links are created to all the included beacons. The
		 * inserted entity is retrieved from the service and pushed into the local cache. The cached entity is returned
		 * in the data property of the result object.
		 * 
		 * @param entity
		 * @param beacons
		 * @param primaryBeacon
		 * @param bitmap
		 * @param cacheOnly
		 * 
		 * @return a ModelResult object. The data property includes the just inserted entity.
		 */

		public ModelResult insertEntity(Entity entity, List<Beacon> beacons, Beacon primaryBeacon, Bitmap bitmap, Boolean cacheOnly) {
			ModelResult result = new ModelResult();
			/*
			 * This is the only place we use the children property
			 * set when deserializing from the service. After this
			 * all references to the children are dynamically assembled
			 * in the getChildren method on entities.
			 */

			if (!cacheOnly) {
				Logger.i(this, "Inserting entity: " + entity.name);
				/*
				 * Upload image to S3 as needed
				 */
				if (bitmap != null) {
					result.serviceResponse = storeImageAtS3(entity, null, bitmap);
				}

				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					/* Construct entity, link, and observation */
					Bundle parameters = new Bundle();

					/* Primary beacon id */
					if (primaryBeacon != null) {
						parameters.putString("primaryBeaconId", primaryBeacon.id);
					}

					if (beacons != null && beacons.size() > 0) {
						/*
						 * Linking to beacons
						 */
						List<String> beaconStrings = new ArrayList<String>();

						for (Beacon beacon : beacons) {
							Observation observation = LocationManager.getInstance().getObservation();
							if (observation != null) {

								beacon.latitude = observation.latitude;
								beacon.longitude = observation.longitude;

								if (observation.altitude != null) {
									beacon.altitude = observation.altitude;
								}
								if (observation.accuracy != null) {
									beacon.accuracy = observation.accuracy;
								}
								if (observation.bearing != null) {
									beacon.bearing = observation.bearing;
								}
								if (observation.speed != null) {
									beacon.speed = observation.speed;
								}
							}

							beacon.beaconType = BeaconType.Fixed.name().toLowerCase(Locale.US);
							beacon.locked = false;
							beaconStrings.add("object:" + ProxibaseService.convertObjectToJsonSmart(beacon, true, true));
						}
						parameters.putStringArrayList("beacons", (ArrayList<String>) beaconStrings);
					}
					else if (entity.parentId != null) {
						/*
						 * Linking to another entity
						 */
						parameters.putString("parentId", entity.parentId);
					}

					/* Observation: only used as data package for the action that gets logged. */
					Observation observation = LocationManager.getInstance().getObservation();
					if (observation != null) {
						parameters.putString("observation",
								"object:" + ProxibaseService.convertObjectToJsonSmart(observation, true, true));
					}

					/* Sources configuration */
					if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
						parameters.putBoolean("suggestSources", true);
						parameters.putInt("suggestTimeout", 10000);
					}

					/* Entity */
					parameters.putString("entity", "object:" + ProxibaseService.convertObjectToJsonSmart(entity, true, true));

					ServiceRequest serviceRequest = new ServiceRequest()
							.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "insertEntity")
							.setRequestType(RequestType.Method)
							.setParameters(parameters)
							.setSession(Aircandi.getInstance().getUser().session)
							.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
							.setRetry(false)
							.setResponseFormat(ResponseFormat.Json);

					result.serviceResponse = dispatch(serviceRequest, false);
				}

				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					Tracker.trackEvent("Entity", "New", entity.type, 0);

					String jsonResponse = (String) result.serviceResponse.data;
					ServiceData serviceData = ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.Entity);

					Entity insertedEntity = (Entity) serviceData.data;
					upsertEntity(insertedEntity);
					result.data = insertedEntity;
				}
			}
			else {
				upsertEntity(entity);
			}

			return result;
		}

		public ModelResult trackEntity(Entity entity, List<Beacon> beacons, Beacon primaryBeacon, String actionType) {

			ModelResult result = new ModelResult();
			Logger.i(this, "Tracking entity");

			/* Construct entity, link, and observation */
			Bundle parameters = new Bundle();

			/* Beacons */
			if (primaryBeacon != null) {
				parameters.putString("primaryBeaconId", primaryBeacon.id);
			}

			if (beacons != null && beacons.size() > 0) {
				List<String> beaconStrings = new ArrayList<String>();
				for (Beacon beacon : beacons) {
					if (beacon.id.equals(primaryBeacon.id)) {
						Observation observation = LocationManager.getInstance().getObservation();
						if (observation != null) {

							beacon.latitude = observation.latitude;
							beacon.longitude = observation.longitude;

							if (observation.altitude != null) {
								beacon.altitude = observation.altitude;
							}
							if (observation.accuracy != null) {
								beacon.accuracy = observation.accuracy;
							}
							if (observation.bearing != null) {
								beacon.bearing = observation.bearing;
							}
							if (observation.speed != null) {
								beacon.speed = observation.speed;
							}
						}
					}

					beacon.beaconType = BeaconType.Fixed.name().toLowerCase(Locale.US);
					beacon.locked = false;
					beaconStrings.add("object:" + ProxibaseService.convertObjectToJsonSmart(beacon, true, true));
				}

				parameters.putStringArrayList("beacons", (ArrayList<String>) beaconStrings);
			}

			/* Observation */
			Observation observation = LocationManager.getInstance().getObservation();
			if (observation != null) {
				parameters.putString("observation",
						"object:" + ProxibaseService.convertObjectToJsonSmart(observation, true, true));
			}

			/* Entity */
			parameters.putString("entityId", entity.id);

			/* Action type */
			parameters.putString("actionType", actionType);

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "trackEntity")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setSession(Aircandi.getInstance().getUser().session)
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = dispatch(serviceRequest, false);

			/* Reproduce the service call effect locally */
			if (result.serviceResponse.responseCode == ResponseCode.Success) {

				entity.activityDate = DateUtils.nowDate().getTime(); // So place rank score gets updated
				setLastActivityDate(DateUtils.nowDate().getTime());	 // So collections get resorted

				if (beacons != null) {
					for (Beacon beacon : beacons) {
						Boolean primary = (primaryBeacon != null && primaryBeacon.id.equals(beacon.id));
						Link link = entity.getLink(beacon, actionType);
						if (link != null) {
							if (primary) {
								if (link.tuneCount != null) {
									link.tuneCount = link.tuneCount.intValue() + 1;
								}
								else {
									link.tuneCount = 1;
								}
								if (!link.primary) {
									link.primary = true;
								}
							}
						}
						else {
							link = new Link(beacon.id, entity.id);
							link.type = actionType;
							link.signal = beacon.level;
							if (primary) {
								if (link.tuneCount != null) {
									link.tuneCount = link.tuneCount.intValue() + 1;
								}
								else {
									link.tuneCount = 1;
								}
								link.primary = true;
							}
							entity.links.add(link);
						}
					}
				}
			}

			return result;
		}

		public ModelResult updateEntity(Entity entity, Bitmap bitmap) {
			ModelResult result = new ModelResult();

			/* Upload new images to S3 as needed. */
			if (bitmap != null) {
				result.serviceResponse = storeImageAtS3(entity, null, bitmap);
			}

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				Logger.i(this, "Updating entity: " + entity.name);

				/*
				 * Construct entity, link, and observation
				 * 
				 * Note: A property will be removed from the document if it is set to null. The routine
				 * to convert objects to json takes a parameter to ignore or serialize props set to null.
				 * For now, I have special case code to ensure that photo is seriallized as null even
				 * if ignoreNulls = true.
				 */
				Bundle parameters = new Bundle();
				parameters.putBoolean("skipActivityDate", false);
				parameters.putString("entity", "object:" + ProxibaseService.convertObjectToJsonSmart(entity, true, true));

				ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "updateEntity")
						.setRequestType(RequestType.Method)
						.setParameters(parameters)
						.setSession(Aircandi.getInstance().getUser().session)
						.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
						.setRetry(false)
						.setResponseFormat(ResponseFormat.Json);

				result.serviceResponse = dispatch(serviceRequest, false);
			}

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				Tracker.trackEvent("Entity", "Update", entity.type, 0);
			}

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				entity.activityDate = DateUtils.nowDate().getTime();
				upsertEntity(entity);
			}

			return result;
		}

		public ModelResult insertComment(String entityId, Comment comment, Boolean cacheOnly) {
			ModelResult result = new ModelResult();

			if (!cacheOnly) {
				Bundle parameters = new Bundle();
				parameters.putString("entityId", entityId);
				parameters.putString("comment", "object:" + ProxibaseService.convertObjectToJsonSmart(comment, true, true));

				ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "insertComment")
						.setRequestType(RequestType.Method)
						.setParameters(parameters)
						.setSession(Aircandi.getInstance().getUser().session)
						.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
						.setRetry(false)
						.setResponseFormat(ResponseFormat.Json);

				result.serviceResponse = dispatch(serviceRequest, false);
			}

			/*
			 * We want to update the entity wherever it might be in the entity model while
			 * keeping the same instance.
			 */
			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				insertComment(entityId, comment);
			}

			return result;
		}

		public ModelResult signin(String email, String password) {
			ModelResult result = new ModelResult();

			Bundle parameters = new Bundle();
			parameters.putString("user", "object:{"
					+ "\"email\":\"" + email + "\","
					+ "\"password\":\"" + password + "\""
					+ "}");

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_AUTH + "signin")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = dispatch(serviceRequest, true);
			return result;

		}

		public ModelResult signout() {
			ModelResult result = new ModelResult();

			User user = Aircandi.getInstance().getUser();
			if (user.session != null) {
				/*
				 * We use a short timeout with no retry because failure doesn't
				 * really hurt anything.
				 */
				ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_AUTH + "signout")
						.setRequestType(RequestType.Get)
						.setSession(user.session)
						.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_QUERIES)
						.setRetry(false)
						.setResponseFormat(ResponseFormat.Json);

				result.serviceResponse = dispatch(serviceRequest, true);
			}

			return result;
		}

		public ModelResult updatePassword(String userId, String passwordOld, String passwordNew) {
			ModelResult result = new ModelResult();

			Bundle parameters = new Bundle();
			parameters.putString("user", "object:{"
					+ "\"_id\":\"" + userId + "\","
					+ "\"oldPassword\":\"" + passwordOld + "\","
					+ "\"newPassword\":\"" + passwordNew + "\""
					+ "}");

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_USER + "changepw")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setSession(Aircandi.getInstance().getUser().session)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = dispatch(serviceRequest, true);

			return result;
		}

		public ModelResult insertLink(Link link) {
			ModelResult result = new ModelResult();

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST + link.getCollection())
					.setRequestType(RequestType.Insert)
					.setRequestBody(ProxibaseService.convertObjectToJsonSmart(link, true, true))
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = dispatch(serviceRequest, false);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				String jsonResponse = (String) result.serviceResponse.data;
				ServiceData serviceData = ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.Link);
				result.data = serviceData.data;
			}

			return result;
		}

		public ModelResult insertBeacon(Beacon beacon) {
			ModelResult result = new ModelResult();

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST + beacon.getCollection())
					.setRequestType(RequestType.Insert)
					.setRequestBody(ProxibaseService.convertObjectToJsonSmart(beacon, true, true))
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = dispatch(serviceRequest, false);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				String jsonResponse = (String) result.serviceResponse.data;
				ServiceData serviceData = ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.Beacon);
				result.data = serviceData.data;
			}

			return result;
		}

		/**
		 * Inserts a new user into the service.
		 * 
		 * @param user
		 * @param bitmap
		 * @return a ModelResult object. The data property includes the just inserted user.
		 */
		public ModelResult insertUser(User user, Bitmap bitmap) {
			ModelResult result = new ModelResult();

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_USER + "create")
					.setRequestType(RequestType.Insert)
					.setRequestBody(ProxibaseService.convertObjectToJsonSmart(user, true, true))
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setUseSecret(true)
					.setResponseFormat(ResponseFormat.Json);

			/*
			 * Insert user.
			 */
			result.serviceResponse = dispatch(serviceRequest, true);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {

				String jsonResponse = (String) result.serviceResponse.data;
				ServiceData serviceData = ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.None);
				user = serviceData.user;
				user.session = serviceData.session;
				/*
				 * Put image to S3 if we have one. Handles setting up the photo
				 * object on user
				 */
				if (bitmap != null && !bitmap.isRecycled()) {
					result.serviceResponse = storeImageAtS3(null, user, bitmap);
				}

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					/*
					 * Update user to capture the uri for the image we saved.
					 */
					serviceRequest = new ServiceRequest()
							.setUri(user.getEntryUri())
							.setRequestType(RequestType.Update)
							.setRequestBody(ProxibaseService.convertObjectToJsonSmart(user, true, true))
							.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
							.setRetry(false)
							.setSession(user.session)
							.setResponseFormat(ResponseFormat.Json);

					/* Doing an update so we don't need anything back */
					result.serviceResponse = dispatch(serviceRequest, false);

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						jsonResponse = (String) result.serviceResponse.data;
						serviceData = ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.User);
						User insertedUser = (User) serviceData.data;
						insertedUser.session = user.session;
						result.data = insertedUser;
					}
				}
			}
			return result;
		}

		public ModelResult updateUser(User user, Bitmap bitmap, Boolean cacheOnly) {
			ModelResult result = new ModelResult();

			if (!cacheOnly) {
				/*
				 * TODO: We are going with a garbage collection scheme for orphaned images. We
				 * need to use an extended property on S3 items that is set to a date when
				 * collection is ok. This allows downloaded entities to keep working even if
				 * an image for entity has changed.
				 */

				/*
				 * Put image to S3 if we have a new one. Handles updating the photo
				 * object on user
				 */
				if (bitmap != null && !bitmap.isRecycled()) {
					result.serviceResponse = storeImageAtS3(null, user, bitmap);
				}

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					/*
					 * Service handles modifiedId and modifiedDate based
					 * on the session info passed with request.
					 */
					ServiceRequest serviceRequest = new ServiceRequest()
							.setUri(user.getEntryUri())
							.setRequestType(RequestType.Update)
							.setRequestBody(ProxibaseService.convertObjectToJsonSmart(user, true, false))
							.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
							.setRetry(false)
							.setSession(Aircandi.getInstance().getUser().session)
							.setResponseFormat(ResponseFormat.Json);

					result.serviceResponse = dispatch(serviceRequest, false);
					updateUser(user);
				}
			}
			else {
				/*
				 * entity.creator is what we show for entity authors. To make the entity model consistent
				 * with this update to the profile we walk all the entities and update where creator.id
				 * equals the signed in user.
				 */
				updateUser(user);
			}
			return result;
		}

		public ModelResult logAction(String targetId, String targetSource, String actionType) {
			ModelResult result = new ModelResult();
			Bundle parameters = new Bundle();

			parameters.putString("targetId", targetId);
			parameters.putString("targetSource", targetSource);
			parameters.putString("actionType", actionType);

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "logAction")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setSession(Aircandi.getInstance().getUser().session)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = dispatch(serviceRequest, false);
			return result;
		}

		// --------------------------------------------------------------------------------------------
		// Beacon routines
		// --------------------------------------------------------------------------------------------

		public void updateBeacons() {
			/*
			 * Makes sure that the beacon collection is an accurate representation
			 * of the latest wifi scan.
			 */

			synchronized (mEntityModel.mBeacons) {
				mEntityModel.mBeacons.clear();
			}
			/*
			 * Insert beacons for the latest scan results.
			 */
			synchronized (mWifiList) {
				for (int i = 0; i < mWifiList.size(); i++) {
					final WifiScanResult scanResult = mWifiList.get(i);
					Beacon beacon = new Beacon(scanResult.BSSID
							, scanResult.SSID
							, scanResult.SSID
							, scanResult.level
							, DateUtils.nowDate()
							, scanResult.test);

					mEntityModel.upsertBeacon(beacon);
				}
			}

			/* Sort beacons by signal strength */
			synchronized (mEntityModel.mBeacons) {
				Collections.sort(mEntityModel.mBeacons, new Beacon.SortBeaconsBySignalLevel());
			}

			BusProvider.getInstance().post(new BeaconsLockedEvent());
		}

		public Collection<Beacon> getBeacons() {
			return mBeacons;
		}

		public Beacon getBeacon(String beaconId) {
			for (Beacon beacon : mBeacons) {
				if (beacon.id.equals(beaconId)) {
					return beacon;
				}
			}
			return null;
		}

		// --------------------------------------------------------------------------------------------
		// Entity cache fetch routines
		// --------------------------------------------------------------------------------------------

		public ModelResult getEntitiesByListType(EntityListType entityListType, Boolean refresh, String collectionId, String userId, Integer limit) {
			ModelResult result = new ModelResult();
			if (entityListType == EntityListType.TunedPlaces) {
				result.data = getRadarPlaces();
			}
			else if (entityListType == EntityListType.SyntheticPlaces) {
				result.data = getRadarSynthetics();
			}
			else if (entityListType == EntityListType.CreatedByUser) {
				result = getUserEntities(userId, refresh, limit);
			}
			else if (entityListType == EntityListType.Collections) {
				result.data = getCollectionEntities();
			}
			else if (entityListType == EntityListType.InCollection) {
				result.data = getChildEntities(collectionId);
			}
			return result;
		}

		/**
		 * Returns top level place entities of all types that should be visible in radar. Entities that are hidden
		 * have already been screened out. Entities are pre-sorted by place rank score and distance.
		 * 
		 * @return
		 */

		public EntityList<Entity> getPlaces() {
			/*
			 * This is the one case where refresh scenarios have been
			 * handled outside of this method.
			 */
			EntityList<Entity> entities = new EntityList<Entity>();
			Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(Preferences.PREF_SEARCH_RADIUS,
					Aircandi.applicationContext.getString(R.string.search_radius_default)));

			synchronized (mEntityCache) {
				for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
					if (entry.getValue().type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
						Entity entity = entry.getValue();
						if (!entity.hidden) {
							Beacon beacon = entity.getActiveBeacon(LinkType.proximity.name());
							/* Must do this to cache the distance before sorting */
							Float distance = entity.getDistance();
							if (beacon != null) {
								entities.add(entity);
							}
							else {
								beacon = entity.getActiveBeacon(LinkType.browse.name());
								/*
								 * Entities that were first found by beacon hang around and could
								 * later be visible via location if we continue with this approach.
								 * 
								 * One thought is that if it qualifies then so be it. Sorting should
								 * put it in the right priority order and place with beacons should
								 * sort higher.
								 */
								/* No beacon for this entity so check using location */
								if (beacon != null || (distance != null && distance != -1 && distance < searchRangeMeters)) {
									entities.add(entity);
								}
							}
						}
					}
				}
			}
			Collections.sort(entities, new Entity.SortEntitiesByPlaceRankScoreDistance());
			return entities;
		}

		/**
		 * Returns all the entities that should be visible in radar. Entities that are hidden
		 * have already been screened out. Only includes top level entities which currently are
		 * only places or global candi (like the update notice). Entities are pre-sorted by
		 * tuning score.
		 * 
		 * @return
		 */

		public EntityList<Entity> getRadarPlaces() {
			/*
			 * This is the one case where refresh scenarios have been
			 * handled outside of this method.
			 */
			EntityList<Entity> entities = new EntityList<Entity>();
			Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(Preferences.PREF_SEARCH_RADIUS,
					Aircandi.applicationContext.getString(R.string.search_radius_default)));

			synchronized (mEntityCache) {
				for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
					if (entry.getValue().type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
						Entity entity = entry.getValue();
						if (!entity.hidden && !entity.synthetic) {
							Beacon beacon = entity.getActiveBeacon(LinkType.proximity.name());
							/* Must do this to cache the distance before sorting */
							Float distance = entity.getDistance();
							if (beacon != null) {
								entities.add(entity);
							}
							else {
								beacon = entity.getActiveBeacon(LinkType.browse.name());
								/*
								 * Entities that were first found by beacon hang around and could
								 * later be visible via location if we continue with this approach.
								 * 
								 * One thought is that if it qualifies then so be it. Sorting should
								 * put it in the right priority order and place with beacons should
								 * sort higher.
								 */
								/* No beacon for this entity so check using location */
								if (beacon != null || (distance != null && distance != -1 && distance < searchRangeMeters)) {
									entities.add(entity);
								}
							}
						}
					}
				}
			}
			Collections.sort(entities, new Entity.SortEntitiesByPlaceRankScoreDistance());
			return entities;
		}

		public EntityList<Entity> getCollectionEntities() {
			/*
			 * This is the one case where refresh scenarios have been
			 * handled outside of this method.
			 */
			EntityList<Entity> entities = new EntityList<Entity>();
			Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(Preferences.PREF_SEARCH_RADIUS,
					Aircandi.applicationContext.getString(R.string.search_radius_default)));
			synchronized (mEntityCache) {
				for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
					if (entry.getValue().isCollection) {
						Entity entity = entry.getValue();
						if (!entity.hidden && !entity.synthetic) {
							Beacon beacon = entity.getActiveBeacon(LinkType.proximity.name());
							/* Must do this to cache the distance before sorting */
							Float distance = entity.getDistance();
							if (beacon != null) {
								entities.add(entity);
							}
							else {
								/*
								 * Entities that were first found by beacon hang around and could
								 * later be visible via location if we continue with this approach.
								 * 
								 * One thought is that if it qualifies then so be it. Sorting should
								 * put it in the right priority order and place with beacons should
								 * sort higher.
								 */
								/* No beacon for this entity so check using location */
								if (distance != null && distance < searchRangeMeters) {
									entities.add(entity);
								}
							}
						}
					}
				}
			}
			Collections.sort(entities, new Entity.SortEntitiesByPlaceRankScoreDistance());
			return entities;
		}

		public EntityList<Entity> getRadarSynthetics() {
			/*
			 * This is the one case where refresh scenarios have been
			 * handled outside of this method.
			 */
			EntityList<Entity> entities = new EntityList<Entity>();
			Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(Preferences.PREF_SEARCH_RADIUS,
					Aircandi.applicationContext.getString(R.string.search_radius_default)));
			synchronized (mEntityCache) {
				for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
					if (entry.getValue().type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
						Entity entity = entry.getValue();
						if (!entity.hidden && entity.synthetic) {
							Float distance = entity.getDistance();
							Beacon beacon = entity.getActiveBeacon(LinkType.proximity.name());
							if (beacon != null) {
								entities.add(entity);
							}
							else {
								/* No beacon for this entity so check using location */
								if (distance != null && distance < searchRangeMeters) {
									entities.add(entity);
								}
							}
						}
					}
				}
			}
			Collections.sort(entities, new Entity.SortEntitiesByPlaceRankScoreDistance());
			return entities;
		}

		public EntityList<Entity> getUserEntities(String userId) {
			EntityList<Entity> entities = new EntityList<Entity>();
			synchronized (mEntityCache) {
				for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
					if (entry.getValue().creatorId != null && entry.getValue().creatorId.equals(userId)) {
						entities.add(entry.getValue());
					}
				}
			}
			Collections.sort(entities, new Entity.SortEntitiesByModifiedDate());
			return entities;
		}

		public EntityList<Entity> getBeaconEntities(String beaconId) {
			EntityList<Entity> entities = new EntityList<Entity>();
			synchronized (mEntityCache) {
				for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
					if (entry.getValue().getBeaconId() != null && entry.getValue().getBeaconId().equals(beaconId)) {
						entities.add(entry.getValue());
					}
				}
			}
			Collections.sort((List<Entity>) entities, new Entity.SortEntitiesByModifiedDate());
			return entities;
		}

		public Entity getCacheEntity(String entityId) {
			synchronized (mEntityCache) {
				return mEntityCache.get(entityId);
			}
		}

		public List<Entity> getCacheEntities(List<String> entityIds) {
			List<Entity> entities = new ArrayList<Entity>();
			synchronized (mEntityCache) {
				for (String entityId : entityIds) {
					Entity entity = mEntityCache.get(entityId);
					if (entity != null) {
						entities.add(entity);
					}
				}
			}
			return entities;
		}

		public EntityList<Entity> getChildEntities(String entityId) {
			EntityList<Entity> entities = new EntityList<Entity>();
			synchronized (mEntityCache) {
				for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
					if (!entry.getValue().type.equals(CandiConstants.TYPE_CANDI_SOURCE)
							&& entry.getValue().parentId != null
							&& entry.getValue().parentId.equals(entityId)) {
						entities.add(entry.getValue());
					}
				}
			}
			Collections.sort(entities, new Entity.SortEntitiesByModifiedDate());
			return entities;
		}

		public EntityList<Entity> getSourceEntities(String entityId) {
			EntityList<Entity> entities = new EntityList<Entity>();
			synchronized (mEntityCache) {
				for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
					if (entry.getValue().type.equals(CandiConstants.TYPE_CANDI_SOURCE)
							&& entry.getValue().parentId != null
							&& entry.getValue().parentId.equals(entityId)) {
						entities.add(entry.getValue());
					}
				}
			}
			Collections.sort(entities, new Entity.SortEntitiesBySourcePosition());
			return entities;
		}

		// --------------------------------------------------------------------------------------------
		// Other fetch routines
		// --------------------------------------------------------------------------------------------

		public Photo getPhoto(String imageUri) {
			for (Photo photo : mPhotos) {
				if (photo.getUri() != null) {
					if (photo.getUri().equals(imageUri)) {
						return photo;
					}
				}
				else {
					if (photo.getUri().equals(imageUri)) {
						return photo;
					}
				}
			}
			return null;
		}

		public List<String> getCategoriesAsStrings(List<Category> categories) {
			List<String> categoryStrings = new ArrayList<String>();
			for (Category category : categories) {
				categoryStrings.add(category.name);
			}
			return categoryStrings;
		}

		// --------------------------------------------------------------------------------------------
		// Entity cache modification routines
		// --------------------------------------------------------------------------------------------

		public void insertComment(String entityId, Comment comment) {
			Entity entity = getCacheEntity(entityId);
			if (entity != null) {
				if (entity.comments == null) {
					entity.comments = new ArrayList<Comment>();
				}
				entity.comments.add(0, comment);
				if (entity.commentCount == null) {
					entity.commentCount = 0;
				}
				entity.commentCount++;
				setLastActivityDate(DateUtils.nowDate().getTime());
			}
		}

		public void upsertBeacons(List<Beacon> beacons) {
			/*
			 * We could be over writing data that has been set locally
			 * and would be lost if we replace beacons that already are
			 * cached.
			 */
			for (Beacon beacon : beacons) {
				upsertBeacon(beacon);
			}
		}

		public void upsertBeacon(Beacon beacon) {
			synchronized (mBeacons) {

				Beacon beaconOriginal = getBeacon(beacon.id);
				if (beaconOriginal != null) {
					Beacon.copyProperties(beacon, beaconOriginal);
				}
				else {
					mBeacons.add(beacon);
				}
			}
			setLastActivityDate(DateUtils.nowDate().getTime());
		}

		public void upsertEntities(List<Entity> entities) {
			for (Entity entity : entities) {
				upsertEntity(entity);
			}
		}

		public Entity upsertEntity(Entity entity) {
			/*
			 * This is the only place we use the children property
			 * set when deserializing from the service. After this
			 * all references to the children are dynamically assembled
			 * in the getChildren method on entities.
			 */
			Entity original = entity;
			synchronized (mEntityCache) {
				Entity entityOriginal = mEntityModel.mEntityCache.get(entity.id);
				if (entityOriginal != null) {
					original = entityOriginal;
					Entity.copyProperties(entity, entityOriginal);
					/*
					 * We only do children work if the new entity has them.
					 */
					if (entity.children != null) {
						/* Removes all children except source entities */
						HashMap<String, Entity> removedChildren = removeChildren(entityOriginal.id);
						for (Entity childEntity : entity.children) {
							Entity removedChild = removedChildren.get(childEntity.id);
							if (removedChild != null) {
								childEntity.hidden = removedChild.hidden;
							}
							mEntityCache.put(childEntity.id, childEntity);
						}
					}
				}
				else {
					mEntityCache.put(entity.id, entity);
					if (entity.children != null) {
						for (Entity childEntity : entity.children) {
							mEntityCache.put(childEntity.id, childEntity);
						}
					}
				}
			}

			/* Create virtual candi for sources */
			if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {

				/* First remove any old stuff */
				removeSourceEntities(entity.id);

				/* Add the current stuff */
				Integer position = 0;
				if (entity.sources != null) {
					for (Source source : entity.sources) {

						/* We are getting duplicates which will have to be addressed on the service side */
						Entity sourceEntity = loadEntityFromResources(R.raw.source_entity);
						if (sourceEntity != null) {
							/* Transfers from source item */
							sourceEntity.id = entity.id + "." + source.id;
							sourceEntity.name = source.name;
							sourceEntity.getPhotoForSet().setImageUri(source.icon, null, null, null);
							source.position = position;
							sourceEntity.source = source;
							sourceEntity.parentId = entity.id;
							upsertEntity(sourceEntity);
							position++;
						}
					}
				}
				Entity sourceEntity = loadEntityFromResources(R.raw.source_entity);
				sourceEntity.id = entity.id + ".comments";
				sourceEntity.name = "comments";
				Source source = new Source();
				source.name = "comments";
				source.icon = "resource:post";
				source.position = position;
				sourceEntity.source = source;

				sourceEntity.commentCount = entity.commentCount;
				sourceEntity.parentId = entity.id;
				upsertEntity(sourceEntity);
			}

			setLastActivityDate(DateUtils.nowDate().getTime());

			//			if (original.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
			//				Logger.v(this, "Proxi pushing entity: " + original.name);
			//				BusProvider.getInstance().post(new EntityChangedEvent(original));
			//			}

			return original;
		}

		public void updateUser(User user) {
			synchronized (mEntityCache) {
				for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
					if (entry.getValue().creatorId != null && entry.getValue().creatorId.equals(user.id)) {
						if (entry.getValue().creator != null) {
							entry.getValue().creator.getPhotoForSet().setImageUri(user.photo.getUri());
							entry.getValue().creator.location = user.location;
							entry.getValue().creator.name = user.name;
						}
					}
				}
			}
			setLastActivityDate(DateUtils.nowDate().getTime());
		}

		public void moveEntity(String moveEntityId, String parentId, String beaconId) {
			Entity entity = getCacheEntity(moveEntityId);
			entity.parentId = parentId;
			setLastActivityDate(DateUtils.nowDate().getTime());
		}

		public void removeEntitiesForUser(String userId) {
			/*
			 * We clean out user entities and their children when the entity
			 * is associated with a beacon that isn't a radar hit.
			 */
			synchronized (mEntityCache) {
				final Iterator iter = mEntityModel.mEntityCache.keySet().iterator();
				while (iter.hasNext()) {
					Entity entity = mEntityModel.mEntityCache.get(iter.next());
					if (entity.parentId == null && entity.creatorId.equals(userId)) {
						iter.remove();
					}
				}
			}
			setLastActivityDate(DateUtils.nowDate().getTime());
		}

		public void removeSyntheticEntities() {
			/*
			 * We clean out user entities and their children when the entity
			 * is associated with a beacon that isn't a radar hit.
			 */
			synchronized (mEntityCache) {
				final Iterator iter = mEntityModel.mEntityCache.keySet().iterator();
				while (iter.hasNext()) {
					Entity entity = mEntityModel.mEntityCache.get(iter.next());
					if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)
							&& entity.synthetic) {
						iter.remove();
					}
				}
			}
			setLastActivityDate(DateUtils.nowDate().getTime());
		}

		public void removeLocationEntities() {
			/*
			 * We clean out user entities and their children when the entity
			 * is associated with a beacon that isn't a radar hit.
			 */
			synchronized (mEntityCache) {
				final Iterator iter = mEntityModel.mEntityCache.keySet().iterator();
				while (iter.hasNext()) {
					Entity entity = mEntityModel.mEntityCache.get(iter.next());
					if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)
							&& !entity.synthetic
							&& (entity.links == null || entity.links.size() == 0)) {
						iter.remove();
					}
				}
			}
			setLastActivityDate(DateUtils.nowDate().getTime());
		}

		public void removeBeaconEntities() {
			/*
			 * We clean out user entities and their children when the entity
			 * is associated with a beacon that isn't a radar hit.
			 */
			synchronized (mEntityCache) {
				final Iterator iter = mEntityModel.mEntityCache.keySet().iterator();
				while (iter.hasNext()) {
					Entity entity = mEntityModel.mEntityCache.get(iter.next());
					if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)
							&& !entity.synthetic
							&& (entity.links != null && entity.links.size() > 0)) {
						iter.remove();
					}
				}
			}
			setLastActivityDate(DateUtils.nowDate().getTime());
		}

		public void removeEntitiesForBeacon(String beaconId) {
			/*
			 * We clean out entities and their children when the top
			 * level entity is associated with the beacon.
			 */
			synchronized (mEntityCache) {
				final Iterator iter = mEntityModel.mEntityCache.keySet().iterator();
				while (iter.hasNext()) {
					Entity entity = mEntityModel.mEntityCache.get(iter.next());
					if (entity.parentId == null
							&& entity.getBeaconId() != null
							&& entity.getBeaconId().equals(beaconId)) {
						iter.remove();
					}
				}
			}
			setLastActivityDate(DateUtils.nowDate().getTime());
		}

		public Entity removeEntity(String entityId) {
			/*
			 * Clean out every entity related to entityId
			 */
			Entity entity = null;
			synchronized (mEntityCache) {
				entity = mEntityCache.remove(entityId);
			}
			removeChildren(entityId);
			removeSourceEntities(entityId);
			setLastActivityDate(DateUtils.nowDate().getTime());
			return entity;
		}

		public void removeAllEntities() {
			synchronized (mEntityCache) {
				mEntityCache.clear();
			}
		}

		private HashMap<String, Entity> removeChildren(String entityId) {
			/*
			 * This will clean all entities that have entityId as parentId. This
			 * will take care synthesized source entities as well.
			 */
			HashMap<String, Entity> entities = new HashMap<String, Entity>();
			synchronized (mEntityCache) {
				final Iterator iter = mEntityModel.mEntityCache.keySet().iterator();
				while (iter.hasNext()) {
					Entity entity = mEntityModel.mEntityCache.get(iter.next());
					if (!entity.type.equals(CandiConstants.TYPE_CANDI_SOURCE)
							&& entity.parentId != null
							&& entity.parentId.equals(entityId)) {
						entities.put(entity.id, entity);
						iter.remove();
					}
				}
			}
			return entities;
		}

		public HashMap<String, Entity> removeSourceEntities(String entityId) {
			/*
			 * This will clean all entities that have entityId as parentId. This
			 * will take care synthesized source entities as well.
			 */
			HashMap<String, Entity> entities = new HashMap<String, Entity>();
			synchronized (mEntityCache) {
				final Iterator iter = mEntityModel.mEntityCache.keySet().iterator();
				while (iter.hasNext()) {
					Entity entity = mEntityModel.mEntityCache.get(iter.next());
					if (entity.type.equals(CandiConstants.TYPE_CANDI_SOURCE)
							&& entity.parentId != null
							&& entity.parentId.equals(entityId)) {
						entities.put(entity.id, entity);
						iter.remove();
					}
				}
			}
			return entities;
		}

		// --------------------------------------------------------------------------------------------
		// Set/Get routines
		// --------------------------------------------------------------------------------------------

		public Number getLastRefreshDate() {
			return mLastRefreshDate;
		}

		public void setLastRefreshDate(Number lastRefreshDate) {
			mLastRefreshDate = lastRefreshDate;
		}

		public Number getLastActivityDate() {
			return mLastActivityDate;
		}

		public void setLastActivityDate(Number lastActivityDate) {
			mLastActivityDate = lastActivityDate;
		}

		public List<Photo> getPhotos() {
			return mPhotos;
		}

		public void setPhotos(List<Photo> photos) {
			mPhotos = photos;
		}

		public List<Category> getCategories() {
			return mCategories;
		}

		public void setCategories(List<Category> categories) {
			mCategories = categories;
		}
	}

	public static class ModelResult {
		public Object			data;
		public ServiceResponse	serviceResponse	= new ServiceResponse();
	}

	public static class WifiScanResult {

		public String	BSSID;
		public String	SSID;
		public int		level	= 0;
		public Boolean	test	= false;

		public WifiScanResult(String bssid, String ssid, int level, Boolean test) {
			this.BSSID = bssid;
			this.SSID = ssid;
			this.level = level;
			this.test = test;
		}

		public WifiScanResult(ScanResult scanResult) {
			this.BSSID = scanResult.BSSID;
			this.SSID = scanResult.SSID;
			this.level = scanResult.level;
		}

		public static class SortWifiBySignalLevel implements Comparator<WifiScanResult> {

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

	public static enum EntityListType {
		TunedPlaces, SyntheticPlaces, CreatedByUser, Collections, InCollection
	}

	public static enum PlaceSources {
		foursquare,
		factual
	}

	public static enum ScanReason {
		query,
		monitoring
	}

	public static interface IEntityProcessListener {
		/**
		 * Callback interface for ProxiExplorer beacon scan requests.
		 */

		/**
		 * Called when a request completes with the given response. Executed by a background thread: do not update the
		 * UI in this method.
		 */
		public void onComplete(List<Entity> proxiEntities);

		/**
		 * Called when the server-side Proxibase method fails. Executed by a background thread: do not update the UI in
		 * this method.
		 */
		public void onProxibaseServiceException(ProxibaseServiceException exception);
	}
}