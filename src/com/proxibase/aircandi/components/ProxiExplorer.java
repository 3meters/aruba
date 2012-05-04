package com.proxibase.aircandi.components;

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

import com.proxibase.aircandi.Aircandi;
import com.proxibase.aircandi.Preferences;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestListener;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ProxibaseServiceException;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.Beacon;
import com.proxibase.service.objects.Beacon.BeaconState;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.Entity.EntityState;
import com.proxibase.service.objects.ServiceData;

public class ProxiExplorer {

	private static ProxiExplorer	singletonObject;
	private Context					mContext;
	private EntityModel				mEntityModel			= new EntityModel();

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
	private final static String		globalBssid				= "00:00:00:00:00:00";
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
			 * All current beacons ids are sent to the service. Previously discovered beacons are
			 * included in separate array along with a their freshness date.
			 * 
			 * To force a full rebuild of all entities for all beacons, clear the beacon collection.
			 * 
			 * The service returns all entities for new beacons and entities that have had activity since
			 * the freshness date for old beacons. Unchanged entities from previous scans will
			 * still be updated for local changes in visibility.
			 */
			Logger.d(this, "Processing beacons from scan");

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
						beaconNew.state = BeaconState.New;
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
						for (Entity childEntity : entity.children) {
							childEntity.state = EntityState.Normal;
						}
					}
				}
			}

			/* Construct string array of the beacon ids */
			ArrayList<String> beaconIds = new ArrayList<String>();
			ArrayList<String> refreshIds = new ArrayList<String>();
			for (Beacon beacon : mEntityModel.getBeacons()) {
				if (beacon.state == BeaconState.New) {
					beacon.state = BeaconState.Normal;
					beaconIds.add(beacon.id);
				}
				else if (beacon.state == BeaconState.Normal) {
					refreshIds.add(beacon.id);
				}
			}

			ServiceResponse serviceResponse = new ServiceResponse();
			if (beaconIds.size() > 0 || refreshIds.size() > 0) {

				serviceResponse = getEntitiesForBeacons(beaconIds, refreshIds, mEntityModel.getLastRefreshDate(), false);
				if (serviceResponse.responseCode == ResponseCode.Success) {
					ServiceData serviceData = (ServiceData) serviceResponse.data;
					/*
					 * Chunking for entities
					 * 
					 * - serviceData.more reflects the total set of entities even if this was a smart refresh.
					 * - Stash the beaconIds so we can reproduce the same call for chunking.
					 */
					mEntityModel.setLastRefreshDate(serviceData.date.longValue());
					if (serviceData.more != null && serviceData.more) {
						mEntityModel.setMore(serviceData.more);
						mEntityModel.setBeaconIdsLast(beaconIds);
						mEntityModel.setRefreshIdsLast(refreshIds);
						mEntityModel.setLastRefreshDateLast(mEntityModel.getLastRefreshDate());
					}
					else {
						mEntityModel.setMore(false);
						mEntityModel.setBeaconIdsLast(null);
						mEntityModel.setRefreshIdsLast(null);
						mEntityModel.setLastRefreshDateLast(null);
					}
				}
			}
			/*
			 * Rebuild the top level entity list and manage visibility
			 */
			rebuildEntityList();
			manageEntityVisibility();
			Events.EventBus.onEntitiesLoaded(serviceResponse);
			mScanRequestProcessing.set(false);
		}
		return;
	}

	public ServiceResponse getEntitiesForBeacons(ArrayList<String> beaconIds, ArrayList<String> refreshIds, Number lastRefreshDate, Boolean chunkMore) {

		if (Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false)) {
			wifiLockAcquire(WifiManager.WIFI_MODE_FULL);
		}

		Bundle parameters = new Bundle();

		/*
		 * If skipping then we need to send all as beaconIds to force
		 * retrieve more entities even if they aren't stale.
		 */
		if (chunkMore) {
			ArrayList<String> beaconIdsAll = (ArrayList<String>) beaconIds.clone();
			beaconIdsAll.addAll(refreshIds);
			if (beaconIdsAll.size() > 0) {
				parameters.putStringArrayList("beaconIds", beaconIdsAll);
			}
		}
		else {
			if (beaconIds.size() > 0) {
				parameters.putStringArrayList("beaconIds", beaconIds);
			}

			if (refreshIds.size() > 0) {
				parameters.putStringArrayList("refreshIds", refreshIds);
				parameters.putLong("refreshDate", lastRefreshDate.longValue());
			}
		}

		parameters.putString("userId", Aircandi.getInstance().getUser().id);
		parameters.putString("eagerLoad", "object:{\"children\":true,\"comments\":false}");
		parameters.putString("options", "object:{\"limit\":"
				+ String.valueOf(CandiConstants.RADAR_ENTITY_LIMIT)
				+ ",\"skip\":" + (chunkMore ? String.valueOf(mEntityModel.getEntities().size()) : "0")
				+ ",\"sort\":{\"modifiedDate\":-1} "
				+ ",\"children\":{\"limit\":"
				+ String.valueOf(CandiConstants.RADAR_CHILDENTITY_LIMIT)
				+ ",\"skip\":0"
				+ ",\"sort\":{\"modifiedDate\":-1}}"
				+ "}");

		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForBeacons");
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

			/* Merge entities into data model */
			mergeEntities(entities, beaconIds, refreshIds, mEntityModel.isMore());
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
			jsonEagerLoad = "{\"children\":true,\"comments\":false}";
		}

		if (jsonOptions == null) {
			jsonOptions = "{\"limit\":"
					+ String.valueOf(CandiConstants.RADAR_ENTITY_LIMIT)
					+ ",\"skip\":0"
					+ ",\"sort\":{\"modifiedDate\":-1} "
					+ ",\"children\":{\"limit\":"
					+ String.valueOf(CandiConstants.RADAR_CHILDENTITY_LIMIT)
					+ ",\"skip\":0"
					+ ",\"sort\":{\"modifiedDate\":-1}}"
					+ "}";
		}

		if (jsonFields == null) {
			jsonFields = "{\"entities\":{},\"children\":{},\"comments\":{}}";
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
									childEntity.parent = entity;
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
	// Chunking
	// --------------------------------------------------------------------------------------------

	public ServiceResponse chunkEntities() {
		/*
		 * If this gets called it means either:
		 * 
		 * mSkip > 0 which means that there are more top level entities to be chunked or
		 * parentEntityId.skip > 0 which means there are more child entities to be chunked.
		 */
		Logger.v(this, "Chunking entities");
		ServiceResponse serviceResponse = new ServiceResponse();
		if (mEntityModel.getBeaconIdsLast().size() > 0 || mEntityModel.getRefreshIdsLast().size() > 0) {

			serviceResponse = getEntitiesForBeacons(mEntityModel.getBeaconIdsLast(), mEntityModel.getRefreshIdsLast(),
					mEntityModel.getLastRefreshDateLast(), mEntityModel.isMore());
			if (serviceResponse.responseCode == ResponseCode.Success) {
				ServiceData serviceData = (ServiceData) serviceResponse.data;

				/* Keep track of whether there are still more entities to chunk */
				if (serviceData.more != null && serviceData.more) {
					mEntityModel.setMore(serviceData.more);
				}
				else {
					mEntityModel.setMore(false);
					mEntityModel.setBeaconIdsLast(null);
					mEntityModel.setRefreshIdsLast(null);
					mEntityModel.setLastRefreshDateLast(null);
				}
			}
		}
		/*
		 * Rebuild the top level entity list. Causes any entities not
		 * associated with beacons to be cleared out.
		 */
		rebuildEntityList();
		/*
		 * Shouldn't cause any changes in previously fetched entities
		 * because we didn't update signal levels.
		 */
		manageEntityVisibility();
		return serviceResponse;
	}

	public ServiceResponse chunkChildEntities(String parentEntityId) {
		/*
		 * Chunk more children for an entity
		 */
		Logger.v(this, "Chunking child entities");
		ServiceResponse serviceResponse = new ServiceResponse();
		Entity entity = mEntityModel.getEntityById(parentEntityId);
		String jsonEagerLoad = "{\"children\":true,\"comments\":false}";
		String jsonOptions = "{\"limit\":"
				+ String.valueOf(CandiConstants.RADAR_ENTITY_LIMIT)
				+ ",\"skip\":0"
				+ ",\"sort\":{\"modifiedDate\":-1} "
				+ ",\"children\":{\"limit\":"
				+ String.valueOf(CandiConstants.RADAR_CHILDENTITY_LIMIT)
				+ ",\"skip\":" + String.valueOf(entity.children.size())
				+ ",\"sort\":{\"modifiedDate\":-1}}"
				+ "}";

		serviceResponse = ProxiExplorer.getInstance().getEntity(entity.id, jsonEagerLoad, null, jsonOptions);

		if (serviceResponse.responseCode == ResponseCode.Success) {
			Entity rawEntity = (Entity) ((ServiceData) serviceResponse.data).data;
			ArrayList<Object> entities = new ArrayList<Object>();
			entities.add(rawEntity);
			ArrayList<String> refreshIds = new ArrayList<String>();
			refreshIds.add(rawEntity.beaconId);
			mergeEntities(entities, null, refreshIds, true);
		}
		/*
		 * Need to handle the visibility status of newly chunked entities.
		 * This shouldn't cause any changes in previously fetched entities
		 * because we didn't update signal levels.
		 */
		manageEntityVisibility();
		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Entity routines
	// --------------------------------------------------------------------------------------------

	private void insertEntity(Entity entity, Beacon beacon) {
		entity.beacon = beacon;
		entity.state = EntityState.New;
		synchronized (beacon.entities) {
			beacon.entities.add(entity);
		}
		for (Entity childEntity : entity.children) {
			childEntity.beacon = beacon;
			childEntity.beaconId = beacon.id;
			childEntity.parent = entity;
			childEntity.state = EntityState.New;
		}
	}

	private void updateEntity(Entity entity, Beacon beacon, boolean chunked) {
		entity.beacon = beacon;
		entity.state = EntityState.Refreshed;
		for (Entity entityTemp : beacon.entities) {
			if (entityTemp.id.equals(entity.id)) {
				if (!chunked) {
					/*
					 * Replace existing entity and do fixups.
					 */
					beacon.entities.set(beacon.entities.indexOf(entityTemp), entity);
					for (Entity childEntity : entity.children) {
						childEntity.beacon = beacon;
						childEntity.beaconId = beacon.id;
						childEntity.parent = entity;
						childEntity.state = EntityState.Refreshed;
					}
				}
				else {
					/*
					 * Append new children to existing entity and do fixups.
					 */
					entityTemp.childrenMore = entity.childrenMore;
					entityTemp.childrenCount = entity.childrenCount;
					for (Entity childEntity : entity.children) {
						childEntity.beacon = beacon;
						childEntity.beaconId = beacon.id;
						childEntity.parent = entityTemp;
						childEntity.state = EntityState.New;
						entityTemp.children.add(childEntity);
					}
				}
			}
		}
	}

	private void mergeEntities(List<Object> entities, ArrayList<String> beaconIds, ArrayList<String> refreshIds, Boolean chunking) {

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
					for (Beacon beacon : mEntityModel.getBeacons()) {
						if (beacon.id.equals(rawEntity.beaconId)) {
							insertEntity(rawEntity, beacon);
						}
					}
				}
				else if (refreshIds.contains(rawEntity.beaconId)) {
					/*
					 * This is either a new or updated entity for an existing beacon.
					 */
					for (Beacon beacon : mEntityModel.getBeacons()) {
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
										updateEntity(rawEntity, beacon, chunking);
										break;
									}
								}
							}
							if (!updateHit) {
								/*
								 * This is new entity
								 */
								insertEntity(rawEntity, beacon);
							}
							break;
						}
					}
				}
			}
		}

		/*
		 * Now we need to remove any entities for refreshed beacons that didn't
		 * come back in the results.
		 * 
		 * TODO: How do we know the different between an entity that was deleted
		 * and an entity that didn't fit the limit. If we remove it, then we should
		 * still get it back when paging to it.
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

	private void rebuildEntityList() {

		/* Rebuild the top level entity list when there could have been a top level change */
		mEntityModel.getEntities().clear();
		for (Beacon beacon : mEntityModel.getBeacons()) {
			synchronized (beacon.entities) {
				for (Entity entityTemp : beacon.entities) {
					mEntityModel.getEntities().add(entityTemp);
					for (int i = entityTemp.children.size() - 1; i >= 0; i--) {
						if (entityTemp.children.get(i).beacon == null) {
							entityTemp.children.remove(i);
						}
					}
				}
			}
		}
	}

	private void manageEntityVisibility() {

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

		private List<Beacon>		mBeacons	= new ArrayList<Beacon>();
		private List<Entity>		mEntities	= new ArrayList<Entity>();
		private Number				mLastRefreshDate;

		private Boolean				mMore		= false;
		private ArrayList<String>	mBeaconIdsLast;
		private ArrayList<String>	mRefreshIdsLast;
		private Number				mLastRefreshDateLast;
		private Boolean				mRookieHit	= false;

		public EntityModel() {}

		public Beacon getBeaconById(String beaconId) {
			for (Beacon beacon : mBeacons) {
				if (beacon.id.equals(beaconId)) {
					return beacon;
				}
			}
			return null;
		}

		public Entity getEntityById(String entityId) {
			if (entityId != null) {
				for (Beacon beacon : mBeacons) {
					synchronized (beacon.entities) {
						for (Entity entity : beacon.entities) {
							if (entity.id.equals(entityId)) {
								return entity;
							}
							for (Entity childEntity : entity.children) {
								if (childEntity.id.equals(entityId)) {
									return childEntity;
								}
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

		public EntityModel clone() {

			/*
			 * Shallow copy so entities are by value but any entity object properties
			 * like beacon are by ref from the original.
			 */
			EntityModel entityModel = new EntityModel();
			entityModel.mEntities = new ArrayList();
			for (Entity entity : mEntities) {
				entityModel.mEntities.add(entity.clone());
			}
			entityModel.mBeacons = new ArrayList(mBeacons);
			entityModel.mLastRefreshDate = mLastRefreshDate;
			entityModel.mMore = mMore;
			if (mBeaconIdsLast != null) {
				entityModel.mBeaconIdsLast = (ArrayList<String>) mBeaconIdsLast.clone();
			}
			if (mRefreshIdsLast != null) {
				entityModel.mRefreshIdsLast = (ArrayList<String>) mRefreshIdsLast.clone();
			}
			entityModel.mLastRefreshDateLast = mLastRefreshDateLast;
			entityModel.mRookieHit = mRookieHit;

			return entityModel;
		}

		public List<Entity> getEntitiesFlat() {
			List<Entity> entitiesFlat = new ArrayList<Entity>();
			for (Beacon beacon : mBeacons)
				synchronized (beacon.entities) {
					for (Entity entity : beacon.entities) {
						entitiesFlat.add(entity);
						for (Entity childEntity : entity.children) {
							entitiesFlat.add(childEntity);
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

		public List<Entity> getEntities() {
			return mEntities;
		}

		public void setEntities(List<Entity> entities) {
			mEntities = entities;
		}

		public Boolean isMore() {
			return mMore;
		}

		public void setMore(Boolean more) {
			mMore = more;
		}

		public ArrayList<String> getBeaconIdsLast() {
			return mBeaconIdsLast;
		}

		public void setBeaconIdsLast(ArrayList<String> beaconIdsLast) {
			mBeaconIdsLast = beaconIdsLast;
		}

		public ArrayList<String> getRefreshIdsLast() {
			return mRefreshIdsLast;
		}

		public void setRefreshIdsLast(ArrayList<String> refreshIdsLast) {
			mRefreshIdsLast = refreshIdsLast;
		}

		public Number getLastRefreshDateLast() {
			return mLastRefreshDateLast;
		}

		public void setLastRefreshDateLast(Number lastRefreshDateLast) {
			mLastRefreshDateLast = lastRefreshDateLast;
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
	}

	public static class ScanOptions {

		public boolean	showProgress	= true;
		public boolean	fullBuild		= false;

		public ScanOptions(boolean fullBuild, boolean showProgress) {
			this.fullBuild = fullBuild;
			this.showProgress = showProgress;
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
}