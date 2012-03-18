package com.proxibase.aircandi.components;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseServiceException;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestListener;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.objects.Beacon;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.Beacon.BeaconState;
import com.proxibase.service.objects.Entity.EntityState;

public class ProxiExplorer {

	private static ProxiExplorer	singletonObject;
	private Context					mContext;

	private List<Beacon>			mBeacons				= new ArrayList<Beacon>();
	public List<Entity>				mEntities				= new ArrayList<Entity>();
	public HashMap					mEntitiesUpdated		= new HashMap();
	public HashMap					mEntitiesInserted		= new HashMap();
	public HashMap					mEntitiesDeleted		= new HashMap();

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
		 * If context is null then we probably crashed and the scan
		 * service is still calling.
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

							Logger.d(ProxiExplorer.this, "Received wifi scan results");
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
					 * 
					 * WIFI_MODE_SCAN_ONLY: Wi-Fi will be kept active, but the only operation that will be supported is
					 * initiation of scans, and the subsequent reporting of scan results. This would work fine
					 * but auto-refresh will not keep the device awake and when it sleeps, any auto-refresh
					 * that results in new beacons and service requests will find the wifi connection dead.
					 * 
					 * Acquiring a wifilock requires WakeLock permission.
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

	public void processBeaconsFromScan(boolean refreshAllBeacons) {

		if (!mScanRequestProcessing.get()) {
			mScanRequestProcessing.set(true);
			/*
			 * Full update: Queries service for entities for every discovered beacon.
			 * Partial update: Queries service for entities for beacons that were not discovered on previous scans.
			 * Entities from previous scans will still be updated for local changes in visibility.
			 */
			Logger.d(this, "Processing beacons from scan");
			ServiceResponse serviceResponse = new ServiceResponse();

			/* Clear beacon collection for a complete rebuild */
			if (refreshAllBeacons) {
				mBeacons.clear();
			}

			/* Reset detection flag */
			for (Beacon beacon : mBeacons) {
				beacon.detectedLastPass = false;
			}

			/* Walk all the latest wifi scan hits */
			synchronized (mWifiList) {
				for (int i = 0; i < mWifiList.size(); i++) {

					final WifiScanResult scanResult = mWifiList.get(i);

					/* See if we are already a beacon for the wifi hit */
					Beacon beaconMatch = getBeaconById("0003:" + scanResult.BSSID);

					/* Add it if we aren't */
					if (beaconMatch == null) {
						Beacon beaconNew = new Beacon(scanResult.BSSID, scanResult.SSID, scanResult.SSID, scanResult.level, DateUtils.nowDate());
						beaconNew.detectedLastPass = true;
						beaconNew.state = BeaconState.New;
						beaconNew.addScanPass(scanResult.level);
						mBeacons.add(beaconNew);
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
			for (int i = mBeacons.size() - 1; i >= 0; i--) {
				Beacon beacon = mBeacons.get(i);
				if (!beacon.detectedLastPass) {
					beacon.scanMisses++;
					if (beacon.scanMisses >= 3) {
						mBeacons.remove(i);
					}
				}
			}

			/*
			 * Call the proxi service to see if the new beacons have been tagged with any entities.
			 * If call comes back null then there was a network or service problem.
			 * The user got a toast notification from the service. We are making synchronous calls inside
			 * an asynchronous thread.
			 */

			/* Set state of all pre-existing entities to Normal */
			for (Beacon beacon : mBeacons) {
				for (Entity entity : beacon.entities) {
					entity.state = EntityState.Normal;
					for (Entity childEntity : entity.children) {
						childEntity.state = EntityState.Normal;
					}
				}
			}

			/* Construct string array of the beacon ids */
			ArrayList<String> beaconIds = new ArrayList<String>();
			for (Beacon beacon : mBeacons) {
				if (beacon.state == BeaconState.New) {
					beacon.state = BeaconState.Normal;
					beaconIds.add(beacon.id);
				}
			}

			if (beaconIds.size() > 0) {

				if (Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false)) {
					wifiLockAcquire(WifiManager.WIFI_MODE_FULL);
				}

				Bundle parameters = new Bundle();
				parameters.putStringArrayList("beaconIds", beaconIds);
				parameters.putString("userId", Aircandi.getInstance().getUser().id);
				parameters.putString("eagerLoad", "object:{\"children\":true,\"comments\":false}");

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForBeacons");
				serviceRequest.setRequestType(RequestType.Method);
				serviceRequest.setParameters(parameters);
				serviceRequest.setResponseFormat(ResponseFormat.Json);

				serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				wifiReleaseLock();

				if (serviceResponse.responseCode == ResponseCode.Success) {

					String jsonResponse = (String) serviceResponse.data;
					List<Object> entities = ProxibaseService.convertJsonToObjects(jsonResponse, Entity.class, GsonType.ProxibaseService);

					/* Match returned entities back to beacons */
					if (entities != null && entities.size() > 0) {
						for (Object obj : entities) {
							Entity entity = (Entity) obj;

							/* Attach beacon */
							for (Beacon beacon : mBeacons) {
								if (beacon.id.equals(entity.beaconId)) {
									entity.beacon = beacon;
									entity.state = EntityState.New;
									beacon.registered = true;
									beacon.entities.add(entity);
									for (Entity childEntity : entity.children) {
										childEntity.beacon = beacon;
										childEntity.beaconId = beacon.id;
										childEntity.parent = entity;
										childEntity.state = EntityState.New;
									}
								}
							}
						}
					}
					/* Any beacon that didn't get entities could be unregistered */
				}
			}

			/* Rebuild the top level entity list */
			mEntities.clear();
			for (Beacon beacon : mBeacons) {
				for (Entity entity : beacon.entities) {
					mEntities.add(entity);
				}
			}

			manageEntityVisibility();
			Events.EventBus.onEntitiesLoaded(serviceResponse);
			mScanRequestProcessing.set(false);
		}
		return;
	}

	public ServiceResponse refreshDirtyEntities() {
		/*
		 * Refreshes only beacons and entities that have been flagged as dirty.
		 * 
		 * For beacons:
		 * - first entity or addtional entities could be added.
		 * - existing entities could be modified or gone.
		 */
		ServiceResponse serviceResponse = new ServiceResponse();
		for (Beacon beacon : mBeacons) {

			if (beacon.dirty) {
				beacon.dirty = false;
				beacon.entities.clear();

				if (Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false)) {
					wifiLockAcquire(WifiManager.WIFI_MODE_FULL);
				}

				Bundle parameters = new Bundle();
				ArrayList<String> beacons = new ArrayList<String>();
				beacons.add(beacon.id);
				parameters.putStringArrayList("beaconIds", beacons);
				parameters.putString("userId", Aircandi.getInstance().getUser().id);
				parameters.putString("eagerLoad", "object:{\"children\":true,\"comments\":false}");

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForBeacons");
				serviceRequest.setRequestType(RequestType.Method);
				serviceRequest.setParameters(parameters);
				serviceRequest.setResponseFormat(ResponseFormat.Json);

				serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				wifiReleaseLock();

				if (serviceResponse.responseCode == ResponseCode.Success) {

					String jsonResponse = (String) serviceResponse.data;
					List<Object> entities = ProxibaseService.convertJsonToObjects(jsonResponse, Entity.class, GsonType.ProxibaseService);

					if (entities != null && entities.size() > 0) {

						for (Object obj : entities) {
							Entity entity = (Entity) obj;

							entity.beacon = beacon;
							entity.state = EntityState.Refreshed;
							setEntityVisibility(entity, beacon);

							for (Entity childEntity : entity.children) {
								childEntity.beacon = beacon;
								childEntity.beaconId = beacon.id;
								childEntity.parent = entity;
								childEntity.state = EntityState.Refreshed;
								setEntityVisibility(childEntity, beacon);
							}
							beacon.entities.add(entity);
						}
					}
				}
			}
			else {
				for (int i = beacon.entities.size() - 1; i >= 0; i--) {

					if (serviceResponse.responseCode != ResponseCode.Success) break;
					Entity entity = beacon.entities.get(i);
					if (entity.dirty) {
						/*
						 * Includes call to rebuildEntityList()
						 */
						serviceResponse = doRefreshEntity(entity.id);
						if (serviceResponse.responseCode != ResponseCode.Success) break;
					}
					else {
						for (int j = entity.children.size() - 1; j >= 0; j--) {
							Entity childEntity = entity.children.get(j);
							if (childEntity.dirty) {
								serviceResponse = doRefreshEntity(childEntity.id);
								if (serviceResponse.responseCode != ResponseCode.Success) break;
							}
						}
					}
				}
			}
		}

		if (serviceResponse.responseCode == ResponseCode.Success) {
			rebuildEntityList();
			serviceResponse.data = mEntities;
		}

		return serviceResponse;

	}

	public ServiceResponse getEntityFromService(String entityId, Boolean getChildren) {

		if (Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false)) {
			wifiLockAcquire(WifiManager.WIFI_MODE_FULL);
		}

		final Bundle parameters = new Bundle();
		ArrayList<String> entityIds = new ArrayList<String>();
		entityIds.add(entityId);
		parameters.putStringArrayList("entityIds", entityIds);
		parameters.putString("eagerLoad", "object:{\"children\":" + getChildren.toString() + ",\"comments\":false}");

		final ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntities");
		serviceRequest.setRequestType(RequestType.Method);
		serviceRequest.setParameters(parameters);
		serviceRequest.setResponseFormat(ResponseFormat.Json);

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		wifiReleaseLock();

		if (serviceResponse.responseCode == ResponseCode.Success) {
			String jsonResponse = (String) serviceResponse.data;
			List<Object> entities = ProxibaseService.convertJsonToObjects(jsonResponse, Entity.class, GsonType.ProxibaseService);

			if (entities != null && entities.size() > 0) {
				Entity entity = (Entity) entities.get(0);

				/* Attach the beacon */
				for (Beacon beacon : mBeacons) {
					if (beacon.id.equals(entity.beaconId)) {
						entity.beacon = beacon;
						beacon.registered = true;
						if (entity.children != null) {
							for (Entity childEntity : entity.children) {
								childEntity.beacon = beacon;
								childEntity.parent = entity;
							}
						}
					}
				}

				serviceResponse.data = entity;
			}
		}
		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Private service calls
	// --------------------------------------------------------------------------------------------	

	private ServiceResponse doRefreshEntity(String entityId) {

		ServiceResponse serviceResponse = new ServiceResponse();
		Entity targetEntity = getEntityById(entityId);

		for (Beacon beacon : mBeacons) {
			if (beacon.entities.contains(targetEntity)) {

				/* We are doing a replace not a merge */
				Entity entityToRefresh = beacon.entities.get(beacon.entities.indexOf(targetEntity));

				if (Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false)) {
					wifiLockAcquire(WifiManager.WIFI_MODE_FULL);
				}

				Bundle parameters = new Bundle();
				ArrayList<String> entityIds = new ArrayList<String>();
				entityIds.add(entityToRefresh.id);
				parameters.putStringArrayList("entityIds", entityIds);
				parameters.putString("eagerLoad", "object:{\"children\":true,\"comments\":false}");

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntities");
				serviceRequest.setRequestType(RequestType.Method);
				serviceRequest.setParameters(parameters);
				serviceRequest.setResponseFormat(ResponseFormat.Json);

				serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				wifiReleaseLock();

				if (serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) serviceResponse.data;
					List<Object> entities = ProxibaseService.convertJsonToObjects(jsonResponse, Entity.class, GsonType.ProxibaseService);

					if (entities != null && entities.size() > 0) {

						for (Object obj : entities) {
							Entity entity = (Entity) obj;

							entity.beacon = beacon;
							entity.beacon.registered = true;
							entity.state = EntityState.Refreshed;
							setEntityVisibility(entity, beacon);
							for (Entity childEntity : entity.children) {
								childEntity.beacon = beacon;
								childEntity.parent = entity;
								childEntity.state = EntityState.Refreshed;
								setEntityVisibility(childEntity, beacon);
							}
							beacon.entities.set(beacon.entities.indexOf(entityToRefresh), entity);
							break;
						}
					}
					else {
						beacon.entities.remove(beacon.entities.indexOf(entityToRefresh));
						break;
					}
				}
				return serviceResponse;
			}
			else {
				for (Entity entity : beacon.entities) {
					if (entity.children.contains(targetEntity)) {

						Entity childEntityToRefresh = entity.children.get(entity.children.indexOf(targetEntity));

						if (Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false)) {
							wifiLockAcquire(WifiManager.WIFI_MODE_FULL);
						}

						Bundle parameters = new Bundle();
						parameters.putString("entityId", childEntityToRefresh.id);
						parameters.putString("eagerLoad", "object:{\"children\":false,\"comments\":false}");

						ServiceRequest serviceRequest = new ServiceRequest();
						serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "GetEntities");
						serviceRequest.setRequestType(RequestType.Method);
						serviceRequest.setParameters(parameters);
						serviceRequest.setResponseFormat(ResponseFormat.Json);

						serviceResponse = NetworkManager.getInstance().request(serviceRequest);

						wifiReleaseLock();

						if (serviceResponse.responseCode == ResponseCode.Success) {

							String jsonResponse = (String) serviceResponse.data;
							List<Object> childEntities = ProxibaseService.convertJsonToObjects(jsonResponse, Entity.class, GsonType.ProxibaseService);

							if (childEntities != null && childEntities.size() > 0) {

								for (Object obj : childEntities) {
									Entity childEntity = (Entity) obj;
									childEntity.beacon = beacon;
									childEntity.parent = entity;
									childEntity.beacon.registered = true;
									childEntity.state = EntityState.Refreshed;
									setEntityVisibility(childEntity, beacon);
									entity.children.set(entity.children.indexOf(childEntityToRefresh), childEntity);
									break;
								}
							}
							else {
								entity.children.remove(entity.children.indexOf(childEntityToRefresh));
								break;
							}
						}
						return serviceResponse;
					}
				}
			}
		}
		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Entity routines
	// --------------------------------------------------------------------------------------------	

	private void rebuildEntityList() {

		/* Rebuild the top level entity list when there could have been a top level change */
		mEntities.clear();
		for (Beacon beaconTemp : mBeacons) {
			for (Entity entityTemp : beaconTemp.entities) {
				mEntities.add(entityTemp);
			}
		}
	}

	private void manageEntityVisibility() {

		/* Visibility status effects all entities regardless of whether this is a full or partial update. */
		Logger.d(this, "Managing entity visibility");

		for (Beacon beacon : mBeacons) {
			for (Entity entity : beacon.entities) {
				setEntityVisibility(entity, beacon);
			}
		}

		/* Push hidden setting down to children */
		for (Beacon beacon : mBeacons) {
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

	private void setEntityVisibility(Entity entity, Beacon beacon) {
		boolean oldIsHidden = entity.hidden;
		entity.hidden = false;
		/*
		 * Make it harder to fade out than it is to fade in.
		 * Entities are only New for the first scan that discovers them.
		 */
		float signalThresholdFluid = entity.signalFence.floatValue();
		if (oldIsHidden == false && entity.beacon.state != BeaconState.New) {
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

	/**
	 * Call this method when an activity holding a reference to ProxiExplorer is being
	 * paused by the system. This ensures that any ongoing tag scan is cleaned
	 * up properly.
	 */
	public void onPause() {
		try {
			/*
			 * TODO: Do we need to do anything to clean up processes that might
			 * be ongoing? What happens to flags for scanning and processing?
			 */
		}
		catch (Exception exception) {
			/*
			 * Jayma: For some insane reason, unregisterReceiver always throws an exception
			 * so we catch it and move on.
			 */
		}
	}

	/**
	 * Call this method when an activity holding a reference to ProxiExplorer is being
	 * destroyed by the system. This ensures that any ongoing tag scan is cleaned
	 * up properly and locks are released.
	 */
	public void onDestroy() {
		try {
			/*
			 * We are aggressive about hold our wifi lock so we need to be sure it gets
			 * released when we are destroyed.
			 */
			wifiReleaseLock();
		}
		catch (Exception exception) {
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	public Context getContext() {
		return this.mContext;
	}

	public void setContext(Context context) {
		this.mContext = context;
	}

	public List<Beacon> getBeacons() {
		return this.mBeacons;
	}

	public List<Entity> getEntitiesFlat() {
		List<Entity> entitiesFlat = new ArrayList<Entity>();
		for (Beacon proxiBeacon : mBeacons)
			for (Entity entity : proxiBeacon.entities) {
				entitiesFlat.add(entity);
				for (Entity childEntity : entity.children) {
					entitiesFlat.add(childEntity);
				}
			}
		return entitiesFlat;
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
			beaconStrongest = getBeaconById("0003:" + wifiStrongest.BSSID);
			if (beaconStrongest == null) {
				beaconStrongest = new Beacon(wifiStrongest.BSSID, wifiStrongest.SSID, wifiStrongest.SSID, wifiStrongest.level, DateUtils.nowDate());
			}
		}

		return beaconStrongest;
	}

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
		return null;
	}

	public void setPrefDemoMode(boolean prefDemoMode) {

		/* Make sure we clear any demo beacons */
		if (!Aircandi.settings.getBoolean(Preferences.PREF_DEMO_MODE, false)) {
			for (int i = mBeacons.size() - 1; i >= 0; i--) {
				if (mBeacons.get(i).label.equals("demo_dolly")) {
					mBeacons.remove(i);
				}
			}
		}
	}

	public void setPrefGlobalBeacons(boolean prefGlobalBeacons) {

		/* Make sure we clear any global beacons */
		if (!Aircandi.settings.getBoolean(Preferences.PREF_GLOBAL_BEACONS, true)) {
			for (int i = mBeacons.size() - 1; i >= 0; i--) {
				if (mBeacons.get(i).label.equals("candi_feed")) {
					mBeacons.remove(i);
				}
			}
		}
	}

	public void setUsingEmulator(boolean usingEmulator) {
		mUsingEmulator = usingEmulator;
	}

	public boolean isUsingEmulator() {
		return mUsingEmulator;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public static class Options {

		public boolean	refreshDirty		= false;
		public boolean	refreshAllBeacons	= true;
		public boolean	showProgress		= true;

		public Options(boolean refreshAllBeacons, boolean refreshDirty, boolean showProgress) {
			this.refreshDirty = refreshDirty;
			this.refreshAllBeacons = refreshAllBeacons;
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

	/**
	 * Callback interface for ProxiExplorer beacon scan requests.
	 */
	public static interface IEntityProcessListener {

		/**
		 * Called when a request completes with the given response.
		 * Executed by a background thread: do not update the UI in this method.
		 */
		public void onComplete(List<Entity> proxiEntities);

		/**
		 * Called when the server-side Proxibase method fails.
		 * Executed by a background thread: do not update the UI in this method.
		 */
		public void onProxibaseServiceException(ProxibaseServiceException exception);
	}
}