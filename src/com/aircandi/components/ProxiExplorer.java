package com.aircandi.components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.CandiRadar;
import com.aircandi.Preferences;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.Query;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.ProxibaseServiceException;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Beacon.BeaconState;
import com.aircandi.service.objects.Beacon.BeaconType;
import com.aircandi.service.objects.Comment;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Observation;
import com.aircandi.service.objects.Result;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.User;
import com.aircandi.service.objects.VersionInfo;

public class ProxiExplorer {

	private static ProxiExplorer	singletonObject;
	private Context					mContext;
	private EntityModel				mEntityModel			= new EntityModel();
	private Observation				mObservation;

	private AtomicBoolean			mScanRequestActive		= new AtomicBoolean(false);
	private AtomicBoolean			mScanRequestProcessing	= new AtomicBoolean(false);

	public List<WifiScanResult>		mWifiList				= new ArrayList<WifiScanResult>();
	private WifiManager				mWifiManager;
	private WifiLock				mWifiLock;
	private boolean					mUsingEmulator			= false;
	public static WifiScanResult	mWifiGlobal				= new WifiScanResult("00:00:00:00:00:00", "candi_feed", -100, false);
	private static WifiScanResult	mWifiDemo				= new WifiScanResult("48:5b:39:e6:d3:55", "test_demo", -50, false);
	private static WifiScanResult	mWifiMassenaUpper		= new WifiScanResult("00:1c:b3:ae:bf:f0", "test_massena_upper", -50, true);
	private static WifiScanResult	mWifiMassenaLower		= new WifiScanResult("00:1c:b3:ae:bb:57", "test_massena_lower", -50, true);
	private static WifiScanResult	mWifiMassenaLowerStrong	= new WifiScanResult("00:1c:b3:ae:bb:57", "test_massena_lower_strong", -20, true);
	private static WifiScanResult	mWifiMassenaLowerWeak	= new WifiScanResult("00:1c:b3:ae:bb:57", "test_massena_lower_weak", -100, true);
	private static WifiScanResult	mWifiEmpty				= new WifiScanResult("aa:aa:bb:bb:cc:cc", "test_empty", -50, true);
	private static double			RADIUS_EARTH			= 6378000;																			// meters
	private static double			SEARCH_RANGE			= 1000000;																			// meters

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

	public void scanForWifi(final RequestListener requestListener) {
		/*
		 * If context is null then we probably crashed and the scan service is still calling.
		 */
		if (mContext == null) {
			return;
		}

		if (!mScanRequestActive.get()) {
			mScanRequestActive.set(true);

			synchronized (mWifiList) {

				if (!mUsingEmulator) {

					mContext.registerReceiver(new BroadcastReceiver() {

						@Override
						public void onReceive(Context context, Intent intent) {

							Logger.v(ProxiExplorer.this, "Received wifi scan results");
							mContext.unregisterReceiver(this);
							wifiReleaseLock();

							/* Get the latest scan results */
							mWifiList.clear();
							for (ScanResult scanResult : mWifiManager.getScanResults()) {
								mWifiList.add(new WifiScanResult(scanResult));
							}

							if (Aircandi.settings.getBoolean(Preferences.PREF_GLOBAL_BEACONS, true)) {
								mWifiGlobal.global = true;
								mWifiList.add(mWifiGlobal);
							}

							String testingBeacons = Aircandi.settings.getString(Preferences.PREF_TESTING_BEACONS, "natural");
							if (!ListPreferenceMultiSelect.contains("natural", testingBeacons, null)) {
								mWifiList.clear();
							}

							if (ListPreferenceMultiSelect.contains("demo", testingBeacons, null)) {
								boolean demoBeaconFound = false;
								for (ScanResult scanResult : mWifiManager.getScanResults()) {
									if ((scanResult.BSSID).equals(mWifiDemo.BSSID)) {
										demoBeaconFound = true;
										break;
									}
								}
								if (!demoBeaconFound) {
									mWifiList.add(mWifiDemo);
								}
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

							if (ListPreferenceMultiSelect.contains("global", testingBeacons, null)) {
								mWifiList.add(mWifiGlobal);
							}

							if (ListPreferenceMultiSelect.contains("empty", testingBeacons, null)) {
								mWifiList.add(mWifiEmpty);
							}

							Events.EventBus.onWifiScanReceived(mWifiList);
							if (requestListener != null) {
								requestListener.onComplete(new ServiceResponse());
							}
							mScanRequestActive.set(false);
						}
					}, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

					/*
					 * WIFI_MODE_FULL: Keeps the wifi radio active and behaving normally per user settings.
					 * WIFI_MODE_SCAN_ONLY: Wi-Fi will be kept active, but the only operation that will be supported is
					 * initiation of scans, and the subsequent reporting of scan results. This would work fine but
					 * auto-refresh will not keep the device awake and when it sleeps, any auto-refresh that results in
					 * new beacons and service requests will find the wifi connection dead. Acquiring a wifilock
					 * requires WakeLock permission.
					 */
					if (Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false)) {
						wifiLockAcquire(WifiManager.WIFI_MODE_FULL);
					}

					mWifiManager.startScan();
				}
				else {
					mWifiList.clear();
					Logger.d(ProxiExplorer.this, "Emulator enabled so using dummy scan results");
					mWifiList.add(mWifiDemo);
					Events.EventBus.onWifiScanReceived(mWifiList);
					if (requestListener != null) {
						requestListener.onComplete(new ServiceResponse());
					}
					mScanRequestActive.set(false);
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Public entry points for service calls
	// --------------------------------------------------------------------------------------------

	public void processBeaconsFromScan() {

		if (!mScanRequestProcessing.get()) {
			mScanRequestProcessing.set(true);
			Aircandi.stopwatch.start();
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

			/* Stash latest observation */
			Observation observation = GeoLocationManager.getInstance().getObservation();

			/* Reset detection flag */
			for (Beacon beacon : mEntityModel.getBeacons()) {
				beacon.detectedLastPass = false;
				beacon.radarHit = false;
			}

			/* Walk all the latest wifi scan hits */
			synchronized (mWifiList) {
				for (int i = 0; i < mWifiList.size(); i++) {

					final WifiScanResult scanResult = mWifiList.get(i);

					/* See if we are already a beacon for the wifi hit */
					Beacon beaconMatch = mEntityModel.getBeacon("0003:" + scanResult.BSSID);

					/* Add it if we aren't */
					if (beaconMatch == null) {
						Beacon beaconNew = new Beacon(scanResult.BSSID, scanResult.SSID, scanResult.SSID, scanResult.level, DateUtils.nowDate(),
								scanResult.test);
						beaconNew.detectedLastPass = true;
						beaconNew.radarHit = true;
						beaconNew.state = BeaconState.New;
						beaconNew.global = scanResult.global;
						if (observation != null) {
							beaconNew.latitude = observation.latitude;
							beaconNew.longitude = observation.longitude;
						}
						beaconNew.addScanPass(scanResult.level);
						mEntityModel.upsertBeacon(beaconNew);
					}
					else {
						/*
						 * Should we update the beacons levelDb
						 */
						beaconMatch.radarHit = true;
						beaconMatch.detectedLastPass = true;
						beaconMatch.state = BeaconState.Normal;
						beaconMatch.addScanPass(scanResult.level);
					}
				}
			}

			/*
			 * Remove beacons that have too many scan misses. We use
			 * an iterator instead of a list so we can remove items if
			 * needed.
			 */
			final Iterator beaconIter = mEntityModel.mBeaconCache.keySet().iterator();
			while (beaconIter.hasNext()) {
				Beacon beacon = mEntityModel.mBeaconCache.get(beaconIter.next());
				if (!beacon.detectedLastPass) {
					beacon.scanMisses++;
					if (beacon.scanMisses >= CandiConstants.RADAR_SCAN_MISS_MAX) {
						beaconIter.remove();
					}
				}
			}

			/*
			 * Call the proxi service to see if the new beacons have been tagged with any entities. If call comes back
			 * null then there was a network or service problem. The user got a toast notification from the service. We
			 * are making synchronous calls inside an asynchronous thread.
			 */

			/* Construct string array of the beacon ids */
			ArrayList<String> beaconIdsNew = new ArrayList<String>();
			ArrayList<String> beaconIdsRefresh = new ArrayList<String>();
			for (Beacon beacon : mEntityModel.getBeacons()) {
				if (beacon.state == BeaconState.New) {
					beacon.state = BeaconState.Normal;
					beaconIdsNew.add(beacon.id);
				}
				else if (beacon.state == BeaconState.Normal) {
					beaconIdsRefresh.add(beacon.id);
				}
			}

			ServiceResponse serviceResponse = new ServiceResponse();
			if (beaconIdsNew.size() > 0 || beaconIdsRefresh.size() > 0) {
				Aircandi.stopwatch.segmentTime("Finished pre-processing beacons");
				serviceResponse = getEntitiesForBeacons(beaconIdsNew, beaconIdsRefresh, mEntityModel.getLastRefreshDate(), beaconIdsNew.size() > 0 ? true
						: false, true);
				if (serviceResponse.responseCode == ResponseCode.Success) {
					ServiceData serviceData = (ServiceData) serviceResponse.data;
					mEntityModel.setLastRefreshDate(serviceData.date.longValue());
				}
			}
			/*
			 * Rebuild the top level entity list and manage visibility
			 */
			manageEntityVisibility();
			Events.EventBus.onEntitiesLoaded(serviceResponse);
			mScanRequestProcessing.set(false);
		}
		return;
	}

	public ServiceResponse getEntitiesForBeacons(ArrayList<String> beaconIdsNew, ArrayList<String> beaconIdsRefresh, Number lastRefreshDate,
			Boolean includeObservation, Boolean merge) {
		/*
		 * For all refresh types, calling this will reset entity collections.
		 */
		if (Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false)) {
			wifiLockAcquire(WifiManager.WIFI_MODE_FULL);
		}

		ServiceResponse serviceResponse = new ServiceResponse();

		/* Set method parameters */
		Bundle parameters = new Bundle();
		if (beaconIdsNew.size() > 0) {
			parameters.putStringArrayList("beaconIdsNew", beaconIdsNew);
			if (includeObservation) {
				ArrayList<Integer> levels = new ArrayList<Integer>();
				for (String beaconId : beaconIdsNew) {
					Beacon beacon = mEntityModel.getBeacon(beaconId);
					levels.add(beacon.global ? -20 : beacon.signalLevel);
				}
				parameters.putIntegerArrayList("beaconLevels", levels);
			}
		}

		if (beaconIdsRefresh != null && beaconIdsRefresh.size() > 0 && lastRefreshDate != null) {
			parameters.putStringArrayList("beaconIdsRefresh", beaconIdsRefresh);
			parameters.putLong("refreshDate", lastRefreshDate.longValue());
		}

		if (includeObservation) {
			mObservation = GeoLocationManager.getInstance().getObservation();
			if (mObservation != null) {
				parameters.putString("observation",
						"object:" + ProxibaseService.convertObjectToJsonSmart(mObservation, true));
			}
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
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForBeacons")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		Aircandi.stopwatch.segmentTime("Finished service query");

		wifiReleaseLock();

		if (serviceResponse.responseCode == ResponseCode.Success) {

			String jsonResponse = (String) serviceResponse.data;
			ServiceData serviceData = (ServiceData) ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
			serviceResponse.data = serviceData;
			Aircandi.stopwatch.segmentTime("Finished parsing json objects");

			if (merge) {
				List<Entity> entities = (List<Entity>) serviceData.data;

				/* Do some fixup migrating settings to the children collection */
				for (Entity rawEntity : entities) {
					if (rawEntity.beaconId.equals("0003:" + mWifiGlobal.BSSID)) {
						rawEntity.global = true;
					}
				}

				/* Add any local globals */
				if (Aircandi.applicationUpdateNeeded) {
					Entity entity = loadEntityFromResources(R.raw.aircandi_install);
					entity.global = true;
					if (entity != null) {
						entities.add(entity);
					}
				}

				/* Merge entities into data model */
				mEntityModel.reloadEntities(entities, beaconIdsNew, beaconIdsRefresh);
				Aircandi.stopwatch.segmentTime("Finished merging into entity model");
			}
		}
		return serviceResponse;
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
				}
			}
		}

		/* Push hidden setting down to children */
		for (Beacon beacon : mEntityModel.getBeacons()) {
			synchronized (beacon) {
				for (Entity entity : beacon.getEntities()) {
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
		if (oldIsHidden == false && entity.getBeacon() != null && entity.getBeacon().state != BeaconState.New) {
			signalThresholdFluid = entity.signalFence.floatValue() - 5;
		}

		/* Hide entities that are not within entity declared virtual range */
		if (Aircandi.settings.getBoolean(Preferences.PREF_ENTITY_FENCING, true) && beacon.getAvgBeaconLevel() < signalThresholdFluid) {
			entity.hidden = true;
			return;
		}

		/* Hide global entities if specified */
		if (!Aircandi.settings.getBoolean(Preferences.PREF_GLOBAL_BEACONS, true) && entity.global) {
			entity.hidden = true;
			return;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Wifi routines
	// --------------------------------------------------------------------------------------------

	private void wifiLockAcquire(int lockType) {
		if (mWifiLock != null && mWifiLock.isHeld()) {
			mWifiLock.release();
		}
		mWifiLock = mWifiManager.createWifiLock(lockType, "Proxi");
		mWifiLock.setReferenceCounted(false);
		if (!mWifiLock.isHeld()) {
			mWifiLock.acquire();
		}
	}

	private void wifiReleaseLock() {
		if (mWifiLock != null && mWifiLock.isHeld()) {
			mWifiLock.release();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	public void onPause() {
		/*
		 * Call this method when an activity holding a reference to ProxiExplorer is being paused by the system. This
		 * ensures that any ongoing tag scan is cleaned up properly.
		 */
		try {
			/*
			 * TODO: Do we need to do anything to clean up processes that might be ongoing? What happens to flags for
			 * scanning and processing?
			 */
		}
		catch (Exception exception) {
			/*
			 * Jayma: For some insane reason, unregisterReceiver always throws an exception so we catch it and move on.
			 */
		}
	}

	public void onDestroy() {
		/*
		 * Call this method when an activity holding a reference to ProxiExplorer is being destroyed by the system. This
		 * ensures that any ongoing tag scan is cleaned up properly and locks are released.
		 */
		try {
			/*
			 * We are aggressive about hold our wifi lock so we need to be sure it gets released when we are destroyed.
			 */
			wifiReleaseLock();
		}
		catch (Exception exception) {}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	public Beacon getStrongestWifiAsBeacon() {

		WifiScanResult wifiStrongest = null;
		WifiScanResult demoWifi = null;

		synchronized (mWifiList) {
			for (WifiScanResult wifi : mWifiList) {
				if (wifi.global || wifi.test) {
					continue;
				}
				if (wifi.BSSID.equals(mWifiDemo.BSSID)) {
					demoWifi = wifi;
				}
				else {
					if (wifiStrongest == null) {
						wifiStrongest = wifi;
					}
					else if (wifi.level > wifiStrongest.level) {
						wifiStrongest = wifi;
					}
				}
			}
		}

		if (wifiStrongest == null && demoWifi != null) {
			wifiStrongest = demoWifi;
		}

		Beacon beaconStrongest = null;
		if (wifiStrongest != null) {
			beaconStrongest = mEntityModel.getBeacon("0003:" + wifiStrongest.BSSID);
			if (beaconStrongest == null) {
				beaconStrongest = new Beacon(wifiStrongest.BSSID, wifiStrongest.SSID, wifiStrongest.SSID, wifiStrongest.level, DateUtils.nowDate(),
						wifiStrongest.test);
			}
		}

		return beaconStrongest;
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

	private ServiceResponse storeImageAtS3(Entity entity, Bitmap bitmap) {

		/*
		 * Delete image from S3 if it has been orphaned TODO: We are going with a garbage collection scheme for orphaned
		 * images. We need to use an extended property on S3 items that is set to a date when collection is ok. This
		 * allows downloaded entities to keep working even if an image for entity has changed.
		 */
		ServiceResponse serviceResponse = new ServiceResponse();

		/* Upload image to S3 if we have a new one. */
		if (bitmap != null) {
			try {
				String imageKey = String.valueOf(Aircandi.getInstance().getUser().id) + "_"
						+ String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME))
						+ ".jpg";
				S3.putImage(imageKey, bitmap);
				entity.imagePreviewUri = imageKey;
				if (entity.imageUri == null || entity.imageUri.equals("")) {
					entity.imageUri = entity.imagePreviewUri;
				}
			}
			catch (ProxibaseServiceException exception) {
				return new ServiceResponse(ResponseCode.Failed, null, exception);
			}
		}
		return serviceResponse;
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
		private HashMap<String, Beacon>	mBeaconCache		= new HashMap<String, Beacon>();

		private Number					mLastRefreshDate;
		private Number					mLastActivityDate	= DateUtils.nowDate().getTime();
		private Boolean					mRookieHit			= false;

		private Boolean					mMapBeaconsLoaded	= false;
		private String					mEntitiesUserId;

		public EntityModel() {}

		public EntityModel clone() {

			/*
			 * Shallow copy so entities are by value but any entity object properties like beacon are by ref from the
			 * original.
			 */
			EntityModel entityModel = new EntityModel();

			entityModel.mLastRefreshDate = mLastRefreshDate;
			entityModel.mLastActivityDate = mLastActivityDate;
			entityModel.mRookieHit = mRookieHit;

			return entityModel;
		}

		public EntityModel copy() {

			/*
			 * Shallow copy so entities are by value but any entity object properties like beacon are by ref from the
			 * original.
			 */
			EntityModel entityModel = new EntityModel();

			entityModel.mLastRefreshDate = mLastRefreshDate;
			entityModel.mLastActivityDate = mLastActivityDate;
			entityModel.mRookieHit = mRookieHit;

			return entityModel;
		}

		// --------------------------------------------------------------------------------------------
		// Combo service/cache queries
		// --------------------------------------------------------------------------------------------

		public ModelResult getUserEntities(Boolean refresh) {
			EntityList<Entity> entities = new EntityList<Entity>();
			ModelResult result = new ModelResult();

			if (refresh || mEntitiesUserId == null || !mEntitiesUserId.equals(Aircandi.getInstance().getUser().id)) {

				Bundle parameters = new Bundle();
				parameters.putString("userId", Aircandi.getInstance().getUser().id);
				parameters.putString("eagerLoad", "object:{\"children\":true,\"parents\":false,\"comments\":false}");
				parameters.putString("fields", "object:{\"entities\":{},\"comments\":{},\"children\":{},\"parents\":{}}");
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
						.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForUser")
						.setRequestType(RequestType.Method)
						.setParameters(parameters)
						.setResponseFormat(ResponseFormat.Json);

				result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					String jsonResponse = (String) result.serviceResponse.data;
					ServiceData serviceData = ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
					List<Entity> entityList = (List<Entity>) serviceData.data;
					entities.addAll(entityList);

					mEntitiesUserId = Aircandi.getInstance().getUser().id;
					upsertEntities(entities);
					result.data = getUserEntities();
				}
			}
			else {
				result.data = getUserEntities();
			}
			return result;
		}

		public ModelResult getBeaconEntities(String beaconId, Boolean refresh) {
			ModelResult result = new ModelResult();

			if (refresh) {
				ArrayList<String> beaconIdsNew = new ArrayList<String>();
				beaconIdsNew.add(beaconId);
				result.serviceResponse = ProxiExplorer.getInstance().getEntitiesForBeacons(beaconIdsNew, null, null, false, false);
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					ServiceData serviceData = (ServiceData) result.serviceResponse.data;
					List<Entity> entityList = (List<Entity>) serviceData.data;
					EntityList<Entity> entities = new EntityList<Entity>();
					entities.addAll(entityList);

					upsertEntities(entities);
					result.data = getBeaconEntities(beaconId);
				}
			}
			else {
				result.data = getBeaconEntities(beaconId);
			}

			return result;
		}

		public ModelResult getEntity(String entityId, Boolean refresh, Boolean updateCache, String jsonEagerLoad, String jsonOptions) {
			ModelResult result = new ModelResult();

			if (refresh) {
				ArrayList<String> entityIds = new ArrayList<String>();
				entityIds.add(entityId);

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
				parameters.putStringArrayList("entityIds", entityIds);
				parameters.putString("eagerLoad", "object:" + jsonEagerLoad);
				parameters.putString("options", "object:" + jsonOptions);

				final ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntities")
						.setRequestType(RequestType.Method)
						.setParameters(parameters)
						.setResponseFormat(ResponseFormat.Json);

				result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) result.serviceResponse.data;
					ServiceData serviceData = ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
					List<Entity> entities = (List<Entity>) serviceData.data;

					if (entities != null && entities.size() > 0) {
						if (updateCache) {
							upsertEntity(entities.get(0));
							result.data = getEntity(entityId);
						}
						else {
							result.data = entities.get(0);
						}
					}
				}
			}
			else {
				result.data = getEntity(entityId);
			}
			return result;
		}

		public ModelResult getMapBeacons(Location location, Boolean refresh) {
			ModelResult result = new ModelResult();

			if (refresh || !mMapBeaconsLoaded) {
				Bundle parameters = new Bundle();
				parameters.putDouble("latitude", location.getLatitude());
				parameters.putDouble("longitude", location.getLongitude());
				parameters.putDouble("radius", SEARCH_RANGE / RADIUS_EARTH); // to radians
				parameters.putString("userId", Aircandi.getInstance().getUser().id);

				ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getBeaconsNearLocation")
						.setRequestType(RequestType.Method)
						.setParameters(parameters)
						.setResponseFormat(ResponseFormat.Json);

				result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) result.serviceResponse.data;
					ServiceData serviceData = (ServiceData) ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Beacon);
					List<Beacon> beacons = (List<Beacon>) serviceData.data;
					mMapBeaconsLoaded = true;
					for (Beacon beacon : beacons) {
						upsertBeacon(beacon);
					}
					result.data = getBeacons();
				}
			}
			else {
				result.data = getBeacons();
			}

			return result;
		}

		public ModelResult getUser(String userId) {
			ModelResult result = new ModelResult();

			Query query = new Query("users");
			query.filter("{\"_id\":\"" + String.valueOf(userId) + "\"}");

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST)
					.setRequestType(RequestType.Get)
					.setQuery(query)
					.setResponseFormat(ResponseFormat.Json);

			if (!Aircandi.getInstance().getUser().isAnonymous()) {
				serviceRequest.setSession(Aircandi.getInstance().getUser().session);
			}

			result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

			return result;
		}

		// --------------------------------------------------------------------------------------------
		// Combo service/cache updates
		// --------------------------------------------------------------------------------------------

		public ModelResult moveEntity(String entityId, String newParentId, Boolean toBeacon, Boolean cacheOnly) {

			ModelResult result = new ModelResult();

			if (!cacheOnly) {
				Entity entity = getEntity(entityId);

				Link link = new Link();
				Bundle parameters = new Bundle();

				link.toId = newParentId;
				link.fromId = entity.id;
				parameters.putString("link", "object:" + ProxibaseService.convertObjectToJsonSmart(link, true));
				parameters.putString("originalToId", entity.parentId != null ? entity.parentId : entity.beaconId);

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

				result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
			}

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				moveEntity(entityId, !toBeacon ? newParentId : null, toBeacon ? newParentId : null);
			}

			return result;
		}

		public ModelResult deleteEntity(String entityId, Boolean cacheOnly) {
			ModelResult result = new ModelResult();

			if (!cacheOnly) {
				Entity entity = getEntity(entityId);
				/*
				 * If there is an image stored with S3 then delete it.
				 * TODO: Flag image for garbage collection but don't
				 * delete it because because it might be needed while aircandi users have current sessions.
				 */
				if (entity.imagePreviewUri != null
						&& !entity.imagePreviewUri.equals("")
						&& !ImageManager.isLocalImage(entity.imagePreviewUri)) {
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
				Logger.i(this, "Deleting entity: " + entity.title);

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

				result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
			}

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				removeEntity(entityId);
			}
			return result;
		}

		public ModelResult insertEntity(Entity entity, Beacon beacon, Bitmap bitmap, Boolean cacheOnly) {
			ModelResult result = new ModelResult();
			/*
			 * This is the only place we use the children property
			 * set when deserializing from the service. After this
			 * all references to the children are dynamically assembled
			 * in the getChildren method on entities.
			 */

			if (!cacheOnly) {
				Logger.i(this, "Inserting entity: " + entity.title);

				result.serviceResponse = storeImageAtS3(entity, bitmap); /* Upload images to S3 as needed. */

				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					/* Construct entity, link, and observation */
					Bundle parameters = new Bundle();

					/* User */
					parameters.putString("userId", Aircandi.getInstance().getUser().id);

					/*
					 * We record an observation if we found a nearby beacon. Beacon might not be registered with
					 * proxibase but
					 * will be after the call.
					 */
					if (beacon != null) {
						mObservation = GeoLocationManager.getInstance().getObservation();
						if (mObservation != null) {
							mObservation.beaconId = beacon.id;
							parameters.putString("observation",
									"object:" + ProxibaseService.convertObjectToJsonSmart(mObservation, true));
						}
					}

					/* Beacon */
					if (beacon != null) {
						if (GeoLocationManager.getInstance().getLocation() != null) {
							Location currentLocation = GeoLocationManager.getInstance().getLocation();

							beacon.latitude = currentLocation.getLatitude();
							beacon.longitude = currentLocation.getLongitude();
							if (currentLocation.hasAltitude()) {
								beacon.altitude = currentLocation.getAltitude();
							}
							if (currentLocation.hasAccuracy()) {
								beacon.accuracy = currentLocation.getAccuracy();
							}
							if (currentLocation.hasBearing()) {
								beacon.bearing = currentLocation.getBearing();
							}
							if (currentLocation.hasSpeed()) {
								beacon.speed = currentLocation.getSpeed();
							}
						}

						beacon.beaconType = BeaconType.Fixed.name().toLowerCase();
						/*
						 * Owner, creator, and modifier are managed by the service using
						 * the session info sent with the request.
						 */
						beacon.locked = false;

						parameters.putString("beacon",
								"object:" + ProxibaseService.convertObjectToJsonSmart(beacon, true));
					}

					/* Link */
					Link link = new Link();
					link.toId = entity.parentId == null ? entity.beaconId : entity.parentId;
					parameters.putString("link",
							"object:" + ProxibaseService.convertObjectToJsonSmart(link, true));

					/* Entity */
					parameters.putString("entity",
							"object:" + ProxibaseService.convertObjectToJsonSmart(entity, true));

					ServiceRequest serviceRequest = new ServiceRequest()
							.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "insertEntity")
							.setRequestType(RequestType.Method)
							.setParameters(parameters)
							.setSession(Aircandi.getInstance().getUser().session)
							.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
							.setRetry(false)
							.setResponseFormat(ResponseFormat.Json);

					result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
				}

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					Tracker.trackEvent("Entity", "New", entity.type, 0);

					String jsonResponse = (String) result.serviceResponse.data;
					ServiceData serviceData = ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.Result);
					Result insertResult = (Result) serviceData.data;

					/* Call to get the inserted entity and push it to the cache */
					result = getEntity(insertResult.id, true, true, null, null);

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						Entity insertedEntity = (Entity) result.data;
						insertedEntity.rookie = true;
						return result;
					}
				}
			}
			else {
				upsertEntity(entity);
			}

			return result;
		}

		public ModelResult updateEntity(Entity entity, Bitmap bitmap, Boolean cacheOnly) {
			ModelResult result = new ModelResult();

			if (!cacheOnly) {

				/* Upload new images to S3 as needed. */
				result.serviceResponse = storeImageAtS3(entity, bitmap);

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					Logger.i(this, "Updating entity: " + entity.title);

					/* Construct entity, link, and observation */
					Bundle parameters = new Bundle();
					parameters.putBoolean("skipActivityDate", false);
					parameters.putString("entity", "object:" + ProxibaseService.convertObjectToJsonSmart(entity, true));

					ServiceRequest serviceRequest = new ServiceRequest()
							.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "updateEntity")
							.setRequestType(RequestType.Method)
							.setParameters(parameters)
							.setSession(Aircandi.getInstance().getUser().session)
							.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
							.setRetry(false)
							.setResponseFormat(ResponseFormat.Json);

					result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
				}

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					Tracker.trackEvent("Entity", "Update", entity.type, 0);
				}
			}

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				upsertEntity(entity);
			}

			return result;
		}

		public ModelResult insertComment(String entityId, Comment comment, Boolean cacheOnly) {
			ModelResult result = new ModelResult();

			if (!cacheOnly) {
				Bundle parameters = new Bundle();
				parameters.putString("entityId", entityId);
				parameters.putString("comment", "object:" + ProxibaseService.convertObjectToJsonSmart(comment, true));

				ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "insertComment")
						.setRequestType(RequestType.Method)
						.setParameters(parameters)
						.setSession(Aircandi.getInstance().getUser().session)
						.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
						.setRetry(false)
						.setResponseFormat(ResponseFormat.Json);

				result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
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

			result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
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

				result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
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

			result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

			return result;
		}

		public ModelResult insertUser(User user, Bitmap bitmap) {
			ModelResult result = new ModelResult();

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_USER + "create")
					.setRequestType(RequestType.Insert)
					.setRequestBody(ProxibaseService.convertObjectToJsonSmart(user, true))
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setUseSecret(true)
					.setResponseFormat(ResponseFormat.Json);

			/*
			 * Insert user.
			 */
			ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

			if (serviceResponse.responseCode == ResponseCode.Success) {

				String jsonResponse = (String) serviceResponse.data;
				ServiceData serviceData = ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.None);
				user = serviceData.user;
				user.session = serviceData.session;

				/*
				 * Upload images to S3 as needed.
				 */
				if (user.imageUri != null && !user.imageUri.contains("resource:") && bitmap != null) {
					String imageKey = String.valueOf(user.id) + "_"
							+ String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME))
							+ ".jpg";
					try {
						S3.putImage(imageKey, bitmap);
					}
					catch (ProxibaseServiceException exception) {
						serviceResponse = new ServiceResponse(ResponseCode.Failed, null, exception);
					}

					if (serviceResponse.responseCode == ResponseCode.Success) {
						/*
						 * Update user.
						 * 
						 * Need to update the user to capture the uri for the image we saved.
						 */
						user.imageUri = imageKey;
						serviceRequest = new ServiceRequest()
								.setUri(user.getEntryUri())
								.setRequestType(RequestType.Update)
								.setRequestBody(ProxibaseService.convertObjectToJsonSmart(user, true))
								.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
								.setRetry(false)
								.setSession(user.session)
								.setResponseFormat(ResponseFormat.Json);

						/* Doing an update so we don't need anything back */
						serviceResponse = NetworkManager.getInstance().request(serviceRequest);
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

				/* Put image to S3 if we have a new one. */
				if (bitmap != null && !bitmap.isRecycled()) {
					try {
						String imageKey = String.valueOf(((User) user).id) + "_"
								+ String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME))
								+ ".jpg";
						S3.putImage(imageKey, bitmap);
						user.imageUri = imageKey;
					}
					catch (ProxibaseServiceException exception) {
						result.serviceResponse = new ServiceResponse(ResponseCode.Failed, null, exception);
					}
				}

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					/*
					 * Service handles modifiedId and modifiedDate based
					 * on the session info passed with request.
					 */

					ServiceRequest serviceRequest = new ServiceRequest()
							.setUri(user.getEntryUri())
							.setRequestType(RequestType.Update)
							.setRequestBody(ProxibaseService.convertObjectToJsonSmart(user, true))
							.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
							.setRetry(false)
							.setSession(Aircandi.getInstance().getUser().session)
							.setResponseFormat(ResponseFormat.Json);

					result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);
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

		public ModelResult checkForUpdate()
		{
			ModelResult result = new ModelResult();

			Aircandi.applicationUpdateNeeded = false;
			Aircandi.applicationUpdateRequired = false;
			Query query = new Query("documents").filter("{\"type\":\"version\",\"target\":\"aircandi\"}");

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST)
					.setRequestType(RequestType.Get)
					.setQuery(query)
					.setSuppressUI(true)
					.setResponseFormat(ResponseFormat.Json);

			if (!Aircandi.getInstance().getUser().isAnonymous()) {
				/*
				 * This causes the user session expiration window to get bumped
				 * if we are within a week of expiration.
				 */
				serviceRequest.setSession(Aircandi.getInstance().getUser().session);
			}

			result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {

				String jsonResponse = (String) result.serviceResponse.data;
				final VersionInfo versionInfo = (VersionInfo) ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.VersionInfo).data;
				String currentVersionName = Aircandi.getVersionName(mContext, CandiRadar.class);

				if (versionInfo.enabled && !currentVersionName.equals(versionInfo.versionName)) {
					Logger.i(ProxiExplorer.this, "Update check: update needed");
					Aircandi.applicationUpdateNeeded = true;
					Aircandi.applicationUpdateUri = versionInfo.updateUri != null ? versionInfo.updateUri : CandiConstants.URL_AIRCANDI_UPGRADE;
					if (versionInfo.updateRequired) {
						Aircandi.applicationUpdateRequired = true;
						Logger.i(ProxiExplorer.this, "Update check: update required");
					}
				}
				Aircandi.lastApplicationUpdateCheckDate = DateUtils.nowDate().getTime();
			}
			return result;

		}

		// --------------------------------------------------------------------------------------------
		// Entity cache fetch routines
		// --------------------------------------------------------------------------------------------

		public EntityList<Entity> getRadarEntities() {
			/*
			 * This is the one case where refresh scenarios have been
			 * handled outside of this method.
			 */
			EntityList<Entity> entities = new EntityList<Entity>();
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().parentId == null && entry.getValue().beaconId != null) {
					Entity entity = entry.getValue();
					Beacon beacon = mBeaconCache.get(entity.beaconId);
					if (beacon != null && beacon.radarHit && !entity.hidden) {
						entities.add(entity);
					}
				}
			}
			Collections.sort(entities, new EntityList.SortEntitiesBySignalLevelDiscoveryTimeModifiedDate());
			return entities;
		}

		public EntityList<Entity> getUserEntities() {
			EntityList<Entity> entities = new EntityList<Entity>();
			String userId = Aircandi.getInstance().getUser().id;
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().creatorId.equals(userId)) {
					entities.add(entry.getValue());
				}
			}
			Collections.sort(entities, new EntityList.SortEntitiesByModifiedDate());
			return entities;
		}

		public EntityList<Entity> getBeaconEntities(String beaconId) {
			EntityList<Entity> entities = new EntityList<Entity>();
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().beaconId != null && entry.getValue().beaconId.equals(beaconId)) {
					entities.add(entry.getValue());
				}
			}
			Collections.sort((List<Entity>) entities, new EntityList.SortEntitiesByModifiedDate());
			return entities;
		}

		public Entity getEntity(String entityId) {
			return mEntityCache.get(entityId);
		}

		public Collection<Beacon> getBeacons() {
			return mBeaconCache.values();
		}

		public Beacon getBeacon(String beaconId) {
			return mBeaconCache.get(beaconId);
		}

		public EntityList<Entity> getChildren(String entityId) {
			EntityList<Entity> entities = new EntityList<Entity>();
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().parentId != null && entry.getValue().parentId.equals(entityId)) {
					entities.add(entry.getValue());
				}
			}
			Collections.sort(entities, new EntityList.SortEntitiesByModifiedDate());
			return entities;
		}

		public Beacon getGlobalBeacon() {
			return mBeaconCache.get("0003:" + mWifiGlobal.BSSID);
		}

		// --------------------------------------------------------------------------------------------
		// Entity cache modification routines
		// --------------------------------------------------------------------------------------------

		public void insertComment(String entityId, Comment comment) {
			Entity entity = getEntity(entityId);
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

		public void upsertEntities(List<Entity> entities) {
			for (Entity entity : entities) {
				upsertEntity(entity);
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
			Beacon beaconOriginal = mBeaconCache.get(beacon.id);
			if (beaconOriginal != null) {
				Beacon.copyProperties(beacon, beaconOriginal);
			}
			else {
				mBeaconCache.put(beacon.id, beacon);
			}
			setLastActivityDate(DateUtils.nowDate().getTime());
		}

		public void upsertEntity(Entity entity) {
			/*
			 * This is the only place we use the children property
			 * set when deserializing from the service. After this
			 * all references to the children are dynamically assembled
			 * in the getChildren method on entities.
			 */
			Entity entityOriginal = mEntityModel.mEntityCache.get(entity.id);
			if (entityOriginal != null) {
				Entity.copyProperties(entity, entityOriginal);
				/*
				 * We only do children work if the new entity has them.
				 */
				if (entity.children != null) {
					HashMap<String, Entity> removedChildren = removeChildren(entityOriginal.id);
					for (Entity childEntity : entity.children) {
						Entity removedChild = removedChildren.get(childEntity.id);
						if (removedChild != null) {
							childEntity.rookie = removedChild.rookie;
							childEntity.hidden = removedChild.hidden;
							childEntity.global = removedChild.global;
							childEntity.discoveryTime = removedChild.discoveryTime;
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
			setLastActivityDate(DateUtils.nowDate().getTime());
		}

		public void updateUser(User user) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().creatorId.equals(user.id)) {
					entry.getValue().creator.imageUri = user.imageUri;
					entry.getValue().creator.location = user.location;
					entry.getValue().creator.name = user.name;
				}
			}
			setLastActivityDate(DateUtils.nowDate().getTime());
		}

		public void moveEntity(String moveEntityId, String parentId, String beaconId) {
			Entity entity = getEntity(moveEntityId);
			entity.parentId = parentId;
			entity.beaconId = beaconId;
			setLastActivityDate(DateUtils.nowDate().getTime());
		}

		public void removeEntitiesForUser(String userId) {
			/*
			 * We clean out user entities and their children when the entity
			 * is associated with a beacon that isn't a radar hit.
			 */
			final Iterator iter = mEntityModel.mEntityCache.keySet().iterator();
			while (iter.hasNext()) {
				Entity entity = mEntityModel.mEntityCache.get(iter.next());
				if (entity.parentId == null && entity.creatorId.equals(userId)) {
					iter.remove();
				}
			}
			setLastActivityDate(DateUtils.nowDate().getTime());
		}

		public void removeBeaconsForRadar() {
			/*
			 * We clean out user entities and their children when the entity
			 * is associated with a beacon that isn't a radar hit.
			 */
			final Iterator iter = mEntityModel.mBeaconCache.keySet().iterator();
			while (iter.hasNext()) {
				Beacon beacon = mEntityModel.mBeaconCache.get(iter.next());
				if (beacon.radarHit) {
					iter.remove();
				}
			}
		}

		public void removeEntitiesForBeacon(String beaconId) {
			/*
			 * We clean out entities and their children when the top
			 * level entity is associated with the beacon.
			 */
			final Iterator iter = mEntityModel.mEntityCache.keySet().iterator();
			while (iter.hasNext()) {
				Entity entity = mEntityModel.mEntityCache.get(iter.next());
				if (entity.parentId == null
						&& entity.beaconId != null
						&& entity.beaconId.equals(beaconId)) {
					iter.remove();
				}
			}
			setLastActivityDate(DateUtils.nowDate().getTime());
		}

		public void removeEntity(String entityId) {
			/*
			 * Clean out every entity related to entityId
			 */
			mEntityCache.remove(entityId);
			removeChildren(entityId);
			setLastActivityDate(DateUtils.nowDate().getTime());
		}

		private HashMap<String, Entity> removeChildren(String entityId) {

			HashMap<String, Entity> entities = new HashMap<String, Entity>();
			final Iterator iter = mEntityModel.mEntityCache.keySet().iterator();
			while (iter.hasNext()) {
				Entity entity = mEntityModel.mEntityCache.get(iter.next());
				if (entity.parentId != null && entity.parentId.equals(entityId)) {
					entities.put(entity.id, entity);
					iter.remove();
				}
			}
			return entities;
		}

		private void reloadEntities(List<Entity> entities, ArrayList<String> beaconIdsNew, ArrayList<String> beaconIdsRefreshed) {
			/*
			 * The passed entities collection is a top level collection and not a child collection.
			 * 
			 * The main trick is to clear out all the entities that are associated with beacons that got
			 * updated since the update will contain fresh entities to use.
			 */
			List<String> updatedBeacons = new ArrayList<String>();
			for (Entity entity : entities) {
				if (!updatedBeacons.contains(entity.beaconId)) {
					updatedBeacons.add(entity.beaconId);
				}
			}

			for (String beaconId : updatedBeacons) {
				removeEntitiesForBeacon(beaconId);
			}

			/*
			 * Now we should be able to just blast in all the new entities
			 */
			upsertEntities(entities);
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

		public Boolean getRookieHit() {
			return mRookieHit;
		}

		public void setRookieHit(Boolean rookieHit) {
			mRookieHit = rookieHit;
		}

		public Number getLastActivityDate() {
			return mLastActivityDate;
		}

		public void setLastActivityDate(Number lastActivityDate) {
			mLastActivityDate = lastActivityDate;
		}

	}

	public static class ModelResult {
		public Object			data;
		public ServiceResponse	serviceResponse	= new ServiceResponse();
	}

	public static class ScanOptions {

		public boolean	showProgress	= true;
		public Integer	progressMessageResId;
		public boolean	fullBuild		= false;

		public ScanOptions(boolean fullBuild, boolean showProgress, Integer progressMessageResId) {
			this.fullBuild = fullBuild;
			this.showProgress = showProgress;
			this.progressMessageResId = progressMessageResId;
		}
	}

	public static class WifiScanResult {

		public String	BSSID;
		public String	SSID;
		public int		level	= 0;
		public Boolean	global	= false;
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

	public static enum EntityTree {
		User, Radar, Map
	}
}