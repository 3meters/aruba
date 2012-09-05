package com.aircandi.components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.Preferences;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.GsonType;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ProxibaseServiceException;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Beacon.BeaconState;
import com.aircandi.service.objects.Comment;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Entity.EntityState;
import com.aircandi.service.objects.Observation;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.User;

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
	private final static int		demoLevel				= -20;
	private final static String		demoBssid				= "48:5b:39:e6:d3:55";
	private final static String		demoSsid				= "demo_dolly";
	private final static int		globalLevel				= -20;
	public final static String		globalBssid				= "00:00:00:00:00:00";
	private final static String		globalSsid				= "candi_feed";

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
							boolean demoBeaconFound = false;
							for (ScanResult scanResult : mWifiManager.getScanResults()) {
								mWifiList.add(new WifiScanResult(scanResult));
								if ((scanResult.BSSID).equals(demoBssid)) {
									demoBeaconFound = true;
								}
							}

							/* Insert wifi results if in demo mode */
							if (Aircandi.settings.getBoolean(Preferences.PREF_DEMO_MODE, false) && !demoBeaconFound) {
								mWifiList.add(new WifiScanResult(demoBssid, demoSsid, demoLevel));
							}

							if (Aircandi.settings.getBoolean(Preferences.PREF_GLOBAL_BEACONS, true)) {
								mWifiList.add(new WifiScanResult(globalBssid, globalSsid, globalLevel));
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
					mWifiList.add(new WifiScanResult(demoBssid, demoSsid, demoLevel));
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
			}

			/* Walk all the latest wifi scan hits */
			synchronized (mWifiList) {
				for (int i = 0; i < mWifiList.size(); i++) {

					final WifiScanResult scanResult = mWifiList.get(i);

					/* See if we are already a beacon for the wifi hit */
					Beacon beaconMatch = mEntityModel.getBeaconById("0003:" + scanResult.BSSID);

					/* Add it if we aren't */
					if (beaconMatch == null) {
						Beacon beaconNew = new Beacon(scanResult.BSSID, scanResult.SSID, scanResult.SSID, scanResult.level, DateUtils.nowDate());
						beaconNew.detectedLastPass = true;
						beaconNew.radarHit = true;
						beaconNew.state = BeaconState.New;
						if (observation != null) {
							beaconNew.latitude = observation.latitude;
							beaconNew.longitude = observation.longitude;
						}
						beaconNew.addScanPass(scanResult.level);
						mEntityModel.getBeacons().add(beaconNew);
					}
					else {
						/*
						 * Should we update the beacons levelDb
						 */
						beaconMatch.detectedLastPass = true;
						beaconMatch.state = BeaconState.Normal;
						beaconMatch.addScanPass(scanResult.level);
					}
				}
			}

			/* Remove beacons that have too many scan misses */
			for (int i = mEntityModel.getBeacons().size() - 1; i >= 0; i--) {
				Beacon beacon = mEntityModel.getBeacons().get(i);
				if (!beacon.detectedLastPass) {
					beacon.scanMisses++;
					if (beacon.scanMisses >= 3) {
						mEntityModel.getBeacons().remove(i);
					}
				}
			}

			/*
			 * Call the proxi service to see if the new beacons have been tagged with any entities. If call comes back
			 * null then there was a network or service problem. The user got a toast notification from the service. We
			 * are making synchronous calls inside an asynchronous thread.
			 */

			/* Set state of all pre-existing entities to Normal */
			for (Beacon beacon : mEntityModel.getBeacons()) {
				synchronized (beacon.entities) {
					for (Entity entity : beacon.entities) {
						entity.state = EntityState.Normal;
						if (entity.children != null) {
							for (Entity childEntity : entity.children) {
								childEntity.state = EntityState.Normal;
							}
						}
					}
				}
			}

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

				serviceResponse = getEntitiesForBeacons(beaconIdsNew, beaconIdsRefresh, mEntityModel.getLastRefreshDate());
				if (serviceResponse.responseCode == ResponseCode.Success) {
					ServiceData serviceData = (ServiceData) serviceResponse.data;
					mEntityModel.setLastRefreshDate(serviceData.date.longValue());
				}
			}
			/*
			 * Rebuild the top level entity list and manage visibility
			 */
			mEntityModel.rebuildEntityList();
			manageEntityVisibility();
			Events.EventBus.onEntitiesLoaded(serviceResponse);
			mScanRequestProcessing.set(false);
		}
		return;
	}

	public ServiceResponse getEntitiesForBeacons(ArrayList<String> beaconIdsNew, ArrayList<String> beaconIdsRefresh, Number lastRefreshDate) {
		/*
		 * For all refresh types, calling this will reset entity collections.
		 */
		if (Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false)) {
			wifiLockAcquire(WifiManager.WIFI_MODE_FULL);
		}

		Bundle parameters = new Bundle();
		ServiceRequest serviceRequest = new ServiceRequest();
		ServiceResponse serviceResponse = new ServiceResponse();

		/* Set method parameters */
		if (beaconIdsNew.size() > 0) {
			parameters.putStringArrayList("beaconIdsNew", beaconIdsNew);
		}

		if (beaconIdsRefresh.size() > 0 && lastRefreshDate != null) {
			parameters.putStringArrayList("beaconIdsRefresh", beaconIdsRefresh);
			parameters.putLong("refreshDate", lastRefreshDate.longValue());
		}

		mObservation = GeoLocationManager.getInstance().getObservation();
		if (mObservation != null) {
			parameters.putString("observation",
					"object:" + ProxibaseService.convertObjectToJson(mObservation, GsonType.ProxibaseService));
		}

		parameters.putString("userId", Aircandi.getInstance().getUser().id);
		parameters.putString("eagerLoad", "object:{\"children\":true,\"comments\":false}");
		parameters.putString("options", "object:{\"limit\":"
				+ String.valueOf(ProxiConstants.RADAR_ENTITY_LIMIT)
				+ ",\"skip\":0"
				+ ",\"sort\":{\"modifiedDate\":-1} "
				+ ",\"children\":{\"limit\":"
				+ String.valueOf(ProxiConstants.RADAR_CHILDENTITY_LIMIT)
				+ ",\"skip\":0"
				+ ",\"sort\":{\"modifiedDate\":-1}}"
				+ "}");

		serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForBeacons")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		wifiReleaseLock();

		if (serviceResponse.responseCode == ResponseCode.Success) {

			String jsonResponse = (String) serviceResponse.data;
			ServiceData serviceData = ProxibaseService.convertJsonToObjects(jsonResponse, Entity.class, GsonType.ProxibaseService);
			serviceResponse.data = serviceData;
			List<Object> entities = (List<Object>) serviceData.data;

			/*
			 * Temporary to force empty case in the UI
			 * entities.clear();
			 */

			/* Do some fixup migrating settings to the children collection */
			for (Object obj : entities) {
				Entity rawEntity = (Entity) obj;
				if (rawEntity.children != null) {
					rawEntity.children.setCollectionType(EntityTree.Radar);
				}
				if (rawEntity.beaconId.equals("0003:" + globalBssid)) {
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
			mEntityModel.mergeEntities(entities, beaconIdsNew, beaconIdsRefresh, false);
			mEntityModel.getEntities().setCollectionType(EntityTree.Radar);
		}
		return serviceResponse;
	}

	public ServiceResponse getEntity(String entityId, String jsonEagerLoad, String jsonFields, String jsonOptions) {
		ArrayList<String> entityIds = new ArrayList<String>();
		entityIds.add(entityId);
		ServiceResponse serviceResponse = getEntities(entityIds, jsonEagerLoad, jsonFields, jsonOptions);
		if (serviceResponse.responseCode == ResponseCode.Success) {
			List<Object> entities = (List<Object>) ((ServiceData) serviceResponse.data).data;
			if (entities != null && entities.size() > 0) {
				Entity entity = (Entity) entities.get(0);
				/*
				 * For now we only expect a single parent to grab the first one.
				 */
				if (entity.parents != null && entity.parents.size() > 0) {
					entity.parent = entity.parents.get(0);
					entity.parentId = entity.parent.id;
				}
				((ServiceData) serviceResponse.data).data = entity;
			}
		}
		return serviceResponse;
	}

	public ServiceResponse getEntities(ArrayList<String> entityIds, String jsonEagerLoad, String jsonFields, String jsonOptions) {

		if (Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false)) {
			wifiLockAcquire(WifiManager.WIFI_MODE_FULL);
		}

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

		if (jsonFields == null) {
			jsonFields = "{\"entities\":{},\"children\":{},\"parents\":{},\"comments\":{}}";
		}

		final Bundle parameters = new Bundle();
		parameters.putStringArrayList("entityIds", entityIds);
		parameters.putString("eagerLoad", "object:" + jsonEagerLoad);
		parameters.putString("fields", "object:" + jsonFields);
		parameters.putString("options", "object:" + jsonOptions);

		final ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntities");
		serviceRequest.setRequestType(RequestType.Method);
		serviceRequest.setParameters(parameters);
		serviceRequest.setResponseFormat(ResponseFormat.Json);

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		wifiReleaseLock();

		if (serviceResponse.responseCode == ResponseCode.Success) {
			String jsonResponse = (String) serviceResponse.data;
			ServiceData serviceData = ProxibaseService.convertJsonToObjects(jsonResponse, Entity.class, GsonType.ProxibaseService);
			serviceResponse.data = serviceData;
			List<Object> entities = (List<Object>) serviceData.data;

			if (entities != null && entities.size() > 0) {
				for (Object objEntity : entities) {
					Entity entity = (Entity) objEntity;

					/* Attach the beacon */
					for (Beacon beacon : mEntityModel.getBeacons()) {
						if (beacon.id.equals(entity.beaconId)) {
							entity.beacon = beacon;
							if (entity.children != null) {
								for (Entity childEntity : entity.children) {
									childEntity.beacon = beacon;
									childEntity.beaconId = beacon.id;
									childEntity.parent = entity;
									childEntity.parentId = entity.id;
								}
							}
						}
					}
				}
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
			synchronized (beacon.entities) {
				for (Entity entity : beacon.entities) {
					setEntityVisibility(entity, beacon);
				}
			}
		}

		/* Push hidden setting down to children */
		for (Beacon beacon : mEntityModel.getBeacons()) {
			synchronized (beacon.entities) {
				for (Entity entity : beacon.entities) {
					for (Entity childEntity : entity.children) {
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
		if (oldIsHidden == false && entity.beacon != null && entity.beacon.state != BeaconState.New) {
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

		/* Hide demo entities if specified */
		if (!Aircandi.settings.getBoolean(Preferences.PREF_DEMO_MODE, false)) {
			if (entity.beacon.bssid.equals(demoBssid)) {
				entity.hidden = true;
				return;
			}
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
				if (wifi.global) {
					continue;
				}
				if (wifi.demo) {
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
			beaconStrongest = mEntityModel.getBeaconById("0003:" + wifiStrongest.BSSID);
			if (beaconStrongest == null) {
				beaconStrongest = new Beacon(wifiStrongest.BSSID, wifiStrongest.SSID, wifiStrongest.SSID, wifiStrongest.level, DateUtils.nowDate());
			}
		}

		return beaconStrongest;
	}

	public void setPrefDemoMode(boolean prefDemoMode) {

		/* Make sure we clear any demo beacons */
		if (!Aircandi.settings.getBoolean(Preferences.PREF_DEMO_MODE, false)) {
			for (int i = mEntityModel.getBeacons().size() - 1; i >= 0; i--) {
				if (mEntityModel.getBeacons().get(i).label.equals("demo_dolly")) {
					mEntityModel.getBeacons().remove(i);
				}
			}
		}
	}

	public void setPrefGlobalBeacons(boolean prefGlobalBeacons) {

		/* Make sure we clear any global beacons */
		if (!Aircandi.settings.getBoolean(Preferences.PREF_GLOBAL_BEACONS, true)) {
			for (int i = mEntityModel.getBeacons().size() - 1; i >= 0; i--) {
				if (mEntityModel.getBeacons().get(i).label.equals("candi_feed")) {
					mEntityModel.getBeacons().remove(i);
				}
			}
		}
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
			Entity entity = ProxibaseService.getGson(GsonType.Internal).fromJson(jsonEntity, Entity.class);
			return entity;
		}
		catch (IOException exception) {
			return null;
		}
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

	public static class EntityModel {

		private EntityList<Entity>	mEntities			= new EntityList<Entity>(EntityTree.Radar);
		private EntityList<Entity>	mUserEntities		= new EntityList<Entity>(EntityTree.User);
		private List<Object>		mMapEntities		= new ArrayList<Object>();

		private List<Beacon>		mBeacons			= new ArrayList<Beacon>();
		private List<Beacon>		mMapBeacons			= new ArrayList<Beacon>();

		private Number				mLastRefreshDate;
		private Number				mLastActivityDate	= DateUtils.nowDate().getTime();
		private Boolean				mRookieHit			= false;

		public EntityModel() {}

		public EntityModel clone() {

			/*
			 * Shallow copy so entities are by value but any entity object properties like beacon are by ref from the
			 * original.
			 */
			EntityModel entityModel = new EntityModel();

			entityModel.mBeacons = new ArrayList(mBeacons);
			entityModel.mEntities = mEntities.clone();
			entityModel.mUserEntities = mUserEntities.clone();
			entityModel.mMapEntities = new ArrayList(mMapEntities);
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

			entityModel.mBeacons = new ArrayList(mBeacons); /* refs to same beacons */
			entityModel.mEntities = mEntities.deepCopy(); /* new entities */
			entityModel.mUserEntities = mUserEntities.deepCopy(); /* new entities */
			entityModel.mMapEntities = new ArrayList(mMapEntities); /* refs to map entities */
			entityModel.mLastRefreshDate = mLastRefreshDate;
			entityModel.mLastActivityDate = mLastActivityDate;
			entityModel.mRookieHit = mRookieHit;

			return entityModel;
		}

		// --------------------------------------------------------------------------------------------
		// Entity routines
		// --------------------------------------------------------------------------------------------

		public boolean replaceEntity(Entity entityNew)
		{
			/*
			 * Need to replace the children too
			 */
			if (entityNew != null) {
				for (Beacon beacon : mBeacons) {
					synchronized (beacon.entities) {
						for (int i = 0; i < beacon.entities.size(); i++) {
							Entity entityOld = beacon.entities.get(i);
							if (entityOld.id.equals(entityNew.id)) {
								beacon.entities.set(i, entityNew);
								return true;
							}
							if (entityOld.children != null) {
								for (int j = 0; j < entityOld.children.size(); j++) {
									Entity childEntityOld = entityOld.children.get(j);
									if (childEntityOld.id.equals(entityNew.id)) {
										entityOld.children.set(j, entityNew);
										return true;
									}
								}
							}
						}
					}
				}
			}
			return false;
		}

		public void insertEntity(Entity entity, Beacon beacon, Entity parentEntity, Boolean atTop, EntityTree collectionType) {
			if (collectionType == EntityTree.Radar) {
				/*
				 * Radar candi are associated with beacons currently in radar proximity.
				 * 
				 * Linkages: beacon, parent
				 */
				entity.state = EntityState.New;
				if (parentEntity == null) {
					entity.beacon = beacon;
					synchronized (beacon.entities) {
						if (atTop) {
							beacon.entities.add(0, entity);
						}
						else {
							beacon.entities.add(entity);
						}
					}
					if (entity.children != null) {
						for (Entity childEntity : entity.children) {
							childEntity.beacon = beacon;
							childEntity.beaconId = beacon.id;
							childEntity.parent = entity;
							childEntity.parentId = entity.id;
							childEntity.state = EntityState.New;
						}
					}
				}
				else {
					entity.beacon = parentEntity.beacon;
					entity.beaconId = parentEntity.beacon.id;
					entity.parent = parentEntity;
					entity.parentId = parentEntity.id;
					if (parentEntity.children == null) {
						parentEntity.children = new EntityList<Entity>();
					}
					synchronized (parentEntity.children) {
						if (atTop) {
							parentEntity.children.add(0, entity);
						}
						else {
							parentEntity.children.add(entity);
						}
					}
				}
			}
			else if (collectionType == EntityTree.User) {
				/*
				 * User candi are beacon independent for our purposes for now. Long term
				 * they could be linked to multiple beacons and that info could be part
				 * of the entity
				 * 
				 * Linkages: parent
				 */
				List<Entity> userEntities = mUserEntities;
				entity.state = EntityState.New;
				if (parentEntity == null) {
					/*
					 * This is a root entity
					 */
					synchronized (userEntities) {
						if (atTop) {
							userEntities.add(0, entity);
						}
						else {
							userEntities.add(entity);
						}
					}
					if (entity.children != null) {
						for (Entity childEntity : entity.children) {
							childEntity.state = EntityState.New;
							childEntity.parent = entity;
							childEntity.parentId = entity.id;
						}
					}
				}
				else {
					/*
					 * This is a child entity
					 */
					entity.parent = parentEntity;
					entity.parentId = parentEntity.id;
					if (parentEntity.children == null) {
						parentEntity.children = new EntityList<Entity>();
					}
					synchronized (parentEntity.children) {
						if (atTop) {
							parentEntity.children.add(0, entity);
						}
						else {
							parentEntity.children.add(entity);
						}
					}
				}
			}
		}

		public void updateEntityEverywhere(Entity entityWithUpdates) {
			/*
			 * We want to update the entity wherever it might be in the entity model while
			 * keeping the same instance.
			 */

			/* Radar entities */
			for (Beacon beacon : mBeacons) {
				synchronized (beacon.entities) {
					for (Entity entity : beacon.entities) {
						if (entity.id.equals(entityWithUpdates.id)) {
							Entity.copyEntityProperties(entityWithUpdates, entity);
						}
						else {
							if (entity.children != null) {
								for (Entity childEntity : entity.children) {
									if (childEntity.id.equals(entityWithUpdates.id)) {
										Entity.copyEntityProperties(entityWithUpdates, childEntity);
									}
								}
							}
						}
					}
				}
			}

			/* User entities */
			synchronized (mUserEntities) {
				for (Entity entity : mUserEntities) {
					if (entity.id.equals(entityWithUpdates.id)) {
						Entity.copyEntityProperties(entityWithUpdates, entity);
					}
					else {
						if (entity.children != null) {
							for (Entity childEntity : entity.children) {
								if (childEntity.id.equals(entityWithUpdates.id)) {
									Entity.copyEntityProperties(entityWithUpdates, childEntity);
								}
							}
						}
					}
				}
			}
		}

		public void insertCommentEverywhere(Comment comment, String entityId) {
			/*
			 * We want to update the entity wherever it might be in the entity model while
			 * keeping the same instance.
			 */

			/* Radar entities */
			for (Beacon beacon : mBeacons) {
				synchronized (beacon.entities) {
					for (Entity entity : beacon.entities) {
						if (entity.id.equals(entityId)) {
							if (entity.comments == null) {
								entity.comments = new ArrayList<Comment>();
							}
							entity.comments.add(0, comment);
							if (entity.commentCount == null) {
								entity.commentCount = 0;
							}
							entity.commentCount++;
						}
						else {
							if (entity.children != null) {
								for (Entity childEntity : entity.children) {
									if (childEntity.id.equals(entityId)) {
										if (childEntity.comments == null) {
											childEntity.comments = new ArrayList<Comment>();
										}
										childEntity.comments.add(0, comment);
										if (childEntity.commentCount == null) {
											childEntity.commentCount = 0;
										}
										childEntity.commentCount++;
									}
								}
							}
						}
					}
				}
			}

			/* User entities */
			synchronized (mUserEntities) {
				for (Entity entity : mUserEntities) {
					if (entity.id.equals(entityId)) {
						if (entity.comments == null) {
							entity.comments = new ArrayList<Comment>();
						}
						entity.comments.add(0, comment);
						entity.commentCount++;
					}
					else {
						if (entity.children != null) {
							for (Entity childEntity : entity.children) {
								if (childEntity.id.equals(entityId)) {
									if (childEntity.comments == null) {
										childEntity.comments = new ArrayList<Comment>();
									}
									childEntity.comments.add(0, comment);
									childEntity.commentCount++;
								}
							}
						}
					}
				}
			}
		}

		public void updateEntityByBeacon(Entity entityUpdated, Beacon beacon) {
			/*
			 * This only gets call as part of processing the results of getEntitiesForBeacons.
			 */
			entityUpdated.beacon = beacon;
			entityUpdated.state = EntityState.Refreshed;
			for (Entity entity : beacon.entities) {
				if (entity.id.equals(entityUpdated.id)) {
					/*
					 * Replace existing entity and do fixups on the children
					 */
					beacon.entities.set(beacon.entities.indexOf(entity), entityUpdated);
					if (entityUpdated.children != null) {
						for (Entity childEntity : entityUpdated.children) {
							childEntity.beacon = beacon;
							childEntity.beaconId = beacon.id;
							childEntity.parent = entityUpdated;
							childEntity.parentId = entityUpdated.id;
							childEntity.state = EntityState.Refreshed;
						}
					}
					break;
				}
			}
		}

		public void deleteEntity(Entity deleteEntity, EntityTree collectionType) {
			/*
			 * This presumes the entity can only appear once per collection type.
			 * This needs to change when entities can have multiple parents.
			 */
			if (collectionType == EntityTree.Radar) {
				for (Beacon beacon : mBeacons) {
					synchronized (beacon.entities) {
						for (int i = beacon.entities.size() - 1; i >= 0; i--) {
							Entity entity = beacon.entities.get(i);
							if (entity.id.equals(deleteEntity.id)) {
								beacon.entities.remove(i);
								return;
							}
							if (entity.children != null) {
								for (int j = entity.children.size() - 1; j >= 0; j--) {
									Entity childEntity = entity.children.get(j);
									if (childEntity.id.equals(deleteEntity.id)) {
										entity.children.remove(j);
										return;
									}
								}
							}
						}
					}
				}
			}
			else if (collectionType == EntityTree.User) {
				/*
				 * An entity can appear both at the top level and as a
				 * child of a collection.
				 */
				synchronized (mUserEntities) {
					for (int i = mUserEntities.size() - 1; i >= 0; i--) {
						Entity entity = mUserEntities.get(i);
						if (entity.id.equals(deleteEntity.id)) {
							mUserEntities.remove(i);
							continue;
						}
						if (entity.children != null) {
							for (int j = entity.children.size() - 1; j >= 0; j--) {
								Entity childEntity = entity.children.get(j);
								if (childEntity.id.equals(deleteEntity.id)) {
									entity.children.remove(j);
								}
							}
						}
					}
				}
			}
		}

		private void mergeEntities(List<Object> entities, ArrayList<String> beaconIds, ArrayList<String> refreshIds, Boolean chunking) {
			/*
			 * The passed entities collection is a top level collection and not a child collection.
			 */

			/* Match returned entities back to beacons */
			List<Beacon> refreshedBeacons = new ArrayList<Beacon>();
			if (entities != null && entities.size() > 0) {

				/* First find out which beacons got updated */

				for (Object obj : entities) {
					Entity rawEntity = (Entity) obj;
					if (beaconIds != null && beaconIds.contains(rawEntity.beaconId)) {
						/*
						 * This is a new entity for a new beacon.
						 */
						for (Beacon beacon : mBeacons) {
							if (beacon.id.equals(rawEntity.beaconId)) {
								insertEntity(rawEntity, beacon, null, false, EntityTree.Radar);
							}
						}
					}
					else if (refreshIds.contains(rawEntity.beaconId)) {
						/*
						 * This is either a new or updated entity for an existing beacon.
						 */
						for (Beacon beacon : mBeacons) {
							if (beacon.id.equals(rawEntity.beaconId)) {

								if (!refreshedBeacons.contains(beacon)) {
									refreshedBeacons.add(beacon);
								}

								boolean updateHit = false;
								synchronized (beacon.entities) {
									for (Entity entity : beacon.entities) {
										if (entity.id.equals(rawEntity.id)) {
											/*
											 * This is an updated entity
											 */
											updateHit = true;
											updateEntityByBeacon(rawEntity, beacon);
											break;
										}
									}
								}
								if (!updateHit) {
									/*
									 * This is new entity
									 */
									insertEntity(rawEntity, beacon, null, false, EntityTree.Radar);
								}
								break;
							}
						}
					}
				}
			}

			/*
			 * Now we need to remove any entities for refreshed beacons that didn't come back in the results.
			 * 
			 * TODO: How do we know the different between an entity that was deleted and an entity that didn't fit the
			 * limit. If we remove it, then we should still get it back when paging to it.
			 */
			if (!chunking) {
				for (Beacon beacon : refreshedBeacons) {

					synchronized (beacon.entities) {
						for (int i = beacon.entities.size() - 1; i >= 0; i--) {
							Entity entity = beacon.entities.get(i);
							boolean hit = false;
							for (Object obj : entities) {
								Entity rawEntity = (Entity) obj;
								if (rawEntity.id.equals(entity.id)) {
									hit = true;
									break;
								}
							}
							if (!hit) {
								/* Remove it */
								beacon.entities.remove(i);
							}
						}
					}
				}
			}
		}

		public void rebuildEntityList() {

			/* Rebuild the top level entity list when there could have been a top level change */
			mEntities.clear();
			for (Beacon beacon : mBeacons) {
				synchronized (beacon.entities) {
					for (Entity entityTemp : beacon.entities) {
						mEntities.add(entityTemp);
						if (entityTemp.children != null) {
							for (int i = entityTemp.children.size() - 1; i >= 0; i--) {
								if (entityTemp.children.get(i).beacon == null) {
									entityTemp.children.remove(i);
								}
							}
						}
					}
				}
			}
		}

		public void updateUser(User user) {

			/* Radar entities */
			for (Beacon beacon : mBeacons) {
				synchronized (beacon.entities) {
					for (Entity entity : beacon.entities) {
						if (entity.creatorId.equals(user.id)) {
							entity.creator.imageUri = user.imageUri;
							entity.creator.location = user.location;
							entity.creator.name = user.name;
						}
						if (entity.children != null) {
							for (Entity childEntity : entity.children) {
								if (childEntity.creatorId.equals(user.id)) {
									childEntity.creator.imageUri = user.imageUri;
									childEntity.creator.location = user.location;
									childEntity.creator.name = user.name;
								}
							}
						}
					}
				}
			}

			/* My candi entities */
			synchronized (mUserEntities) {
				for (Entity entity : mUserEntities) {
					if (entity.creatorId.equals(user.id)) {
						entity.creator.imageUri = user.imageUri;
						entity.creator.location = user.location;
						entity.creator.name = user.name;
					}
					if (entity.children != null) {
						for (Entity childEntity : entity.children) {
							if (childEntity.creatorId.equals(user.id)) {
								childEntity.creator.imageUri = user.imageUri;
								childEntity.creator.location = user.location;
								childEntity.creator.name = user.name;
							}
						}
					}
				}
			}
		}

		// --------------------------------------------------------------------------------------------
		// Lookup routines
		// --------------------------------------------------------------------------------------------

		public Beacon getBeaconById(String beaconId) {
			for (Beacon beacon : mBeacons) {
				if (beacon.id.equals(beaconId)) {
					return beacon;
				}
			}
			return null;
		}

		public Beacon getMapBeaconById(String beaconId) {
			for (Beacon beacon : mMapBeacons) {
				if (beacon.id.equals(beaconId)) {
					return beacon;
				}
			}
			return null;
		}

		public Entity getEntityById(String entityId, String parentId, EntityTree entityTree) {
			if (entityId != null) {
				if (entityId.equals(ProxiConstants.ROOT_COLLECTION_ID)) {
					Entity entity = new Entity();
					entity.id = entityId;
					entity.superRoot = true;
					return entity;
				}
				else {
					if (entityTree == EntityTree.Radar) {
						for (Beacon beacon : mBeacons) {
							synchronized (beacon.entities) {
								for (Entity entity : beacon.entities) {
									if (entity.id.equals(entityId)) {
										return entity;
									}
									if (entity.children != null) {
										for (Entity childEntity : entity.children) {
											if (childEntity.id.equals(entityId)) {
												return childEntity;
											}
										}
									}
								}
							}
						}
					}
					else if (entityTree == EntityTree.User) {
						synchronized (mUserEntities) {
							if (parentId == null) {
								for (Entity entity : mUserEntities) {
									if (entity.id.equals(entityId)) {
										return entity;
									}
								}
							}
							else {
								for (Entity entity : mUserEntities) {
									if (entity.id.equals(parentId)) {
										if (entity.children != null) {
											for (Entity childEntity : entity.children) {
												if (childEntity.id.equals(entityId)) {
													return childEntity;
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			return null;
		}

		public EntityList<Entity> getCollectionById(String collectionId, EntityTree entityTree) {
			/*
			 * Returns the children of the entity where id == collectionId Returns null if no match is found.
			 */
			EntityList<Entity> entities = null;
			if (entityTree == EntityTree.Radar) {
				entities = mEntities;
			}
			else if (entityTree == EntityTree.User) {
				entities = mUserEntities;
			}

			/* No need to go further if we are looking for a root collection */
			if (collectionId.equals(ProxiConstants.ROOT_COLLECTION_ID)) {
				return entities;
			}

			return doCollectionById(entities, collectionId);
		}

		private EntityList<Entity> doCollectionById(EntityList<Entity> entities, String collectionId) {
			synchronized (entities) {
				for (Entity entity : entities) {
					if (entity.id.equals(collectionId)) {
						return entity.children;
					}
					else {
						if (entity.children != null && entity.children.size() > 0) {
							EntityList<Entity> childEntities = doCollectionById(entity.children, collectionId);
							if (childEntities != null) {
								return childEntities;
							}
						}
					}
				}
			}
			return null;
		}

		public Beacon getStrongestBeacon() {

			Beacon beaconStrongest = null;
			Beacon beaconDemo = null;

			for (Beacon beacon : mBeacons) {
				if (beacon.id.equals("0003:" + globalBssid)) {
					continue;
				}
				if (beacon.id.equals("0003:" + demoBssid)) {
					beaconDemo = beacon;
				}
				else {
					if (beaconStrongest == null) {
						beaconStrongest = beacon;
					}
					else if (beacon.getAvgBeaconLevel() > beaconStrongest.getAvgBeaconLevel()) {
						beaconStrongest = beacon;
					}
				}
			}
			if (beaconStrongest == null && beaconDemo != null) {
				beaconStrongest = beaconDemo;
			}

			return beaconStrongest;
		}

		public Beacon getGlobalBeacon() {
			for (Beacon beacon : mBeacons) {
				if (beacon.id.equals("0003:" + globalBssid)) {
					return beacon;
				}
			}
			return null;
		}

		// --------------------------------------------------------------------------------------------
		// Set/Get routines
		// --------------------------------------------------------------------------------------------

		public List<Entity> getEntitiesFlat() {
			List<Entity> entitiesFlat = new ArrayList<Entity>();
			for (Beacon beacon : mBeacons)
				synchronized (beacon.entities) {
					for (Entity entity : beacon.entities) {
						entitiesFlat.add(entity);
						if (entity.children != null) {
							for (Entity childEntity : entity.children) {
								entitiesFlat.add(childEntity);
							}
						}
					}
				}
			return entitiesFlat;
		}

		public List<Beacon> getBeacons() {
			return mBeacons;
		}

		public void setBeacons(List<Beacon> beacons) {
			mBeacons = beacons;
		}

		public EntityList<Entity> getEntities() {
			return mEntities;
		}

		public void setEntities(EntityList<Entity> entities) {
			mEntities = entities;
		}

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

		public EntityList<Entity> getUserEntities() {
			return mUserEntities;
		}

		public void setUserEntities(EntityList<Entity> userEntities) {
			mUserEntities = userEntities;
		}

		public List<Object> getMapEntities() {
			return mMapEntities;
		}

		public void setMapEntities(List<Object> mapEntities) {
			mMapEntities = mapEntities;
		}

		public List<Beacon> getMapBeacons() {
			return mMapBeacons;
		}

		public void setMapBeacons(List<Beacon> mapBeacons) {
			mMapBeacons = mapBeacons;
		}

		public Number getLastActivityDate() {
			return mLastActivityDate;
		}

		public void setLastActivityDate(Number lastActivityDate) {
			mLastActivityDate = lastActivityDate;
		}

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
		public Boolean	demo	= false;
		public Boolean	global	= false;

		public WifiScanResult(String bssid, String ssid, int level) {
			this.BSSID = bssid;
			this.SSID = ssid;
			this.level = level;

			if (bssid.equals(globalBssid)) {
				this.global = true;
			}
			if (bssid.equals(demoBssid)) {
				this.demo = true;
			}
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
		User, Radar
	}
}