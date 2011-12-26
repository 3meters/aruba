package com.proxibase.aircandi.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;

import com.proxibase.aircandi.utils.NetworkManager.ResponseCode;
import com.proxibase.aircandi.utils.NetworkManager.ResultCodeDetail;
import com.proxibase.aircandi.utils.NetworkManager.ServiceResponse;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.consumer.Beacon.BeaconState;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy.EntityState;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.Logger;
import com.proxibase.sdk.android.util.ProxiConstants;

public class ProxiExplorer {

	private static ProxiExplorer	singletonObject;
	private static String			sModName				= "ProxiExplorer";
	private Context					mContext;
	private User					mUser;

	private boolean					mPrefEntityFencing		= true;
	private boolean					mPrefDemoMode			= false;
	private boolean					mPrefGlobalBeacons		= true;

	private List<Beacon>			mBeacons				= new ArrayList<Beacon>();
	private List<EntityProxy>		mEntityProxies			= new ArrayList<EntityProxy>();

	private Boolean					mScanRequestActive		= false;
	private Boolean					mScanRequestProcessing	= false;
	private Options					mOptions;
	private RequestListener			mRequestListener;

	private List<WifiScanResult>	mWifiList				= new ArrayList<WifiScanResult>();
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

	/**
	 * Designed as a singleton. The private Constructor prevents any other class from instantiating.
	 */
	private ProxiExplorer() {}

	/**
	 * Returns a {@link List} collection of {@link EntityProxy} objects currently associated with
	 * wifi beacons visible to the host device.
	 * The bssid's for any discovered beacons are sent to the proxibase service to lookup
	 * any entity proxies that are currently associated with the beacon keyed on the bssid.
	 * <p>
	 * For data traffic efficiency, beacons are tracked once they are discovered and only new beacons are processed
	 * during subsequent calls. The fullUpdate param is used to clear the tracked beacons forcing a complete refresh.
	 * Using fullUpdate is recommended to ensure that beacons with new or updated EntityProxy objects are current.
	 * <p>
	 * To force a beacon to be refreshed without forcing a full update, use the setInvalid method on the beacon in the
	 * Beacons collection.
	 * 
	 * @param fullUpdate if true then the existing beacon collection is cleared.
	 * @param listener the callback used to return the EntityProxy collection.
	 */
	public void scanForBeacons(Options options, RequestListener listener) {

		if (listener == null) {
			throw new IllegalArgumentException("Listener is required when calling scanForBeacons");
		}

		mRequestListener = listener;
		mOptions = options;
		mWifiList.clear();

		if (!mUsingEmulator) {
			if (mContext == null) {
				throw new IllegalStateException("Context must be set before calling scanForBeaconsAsync");
			}

			mScanRequestActive = true;
			mContext.registerReceiver(new BroadcastReceiver() {

				@Override
				public void onReceive(Context context, Intent intent) {

					/* We only process scan results we have requested */
					if (mScanRequestActive && !mScanRequestProcessing) {

						mScanRequestActive = false;
						mScanRequestProcessing = true;

						/* Get the latest scan results */
						mWifiList.clear();
						boolean demoBeaconFound = false;
						for (ScanResult scanResult : mWifiManager.getScanResults()) {
							mWifiList.add(new WifiScanResult(scanResult));
							if (scanResult.BSSID.equals(demoBssid))
								demoBeaconFound = true;
						}

						/* Insert wifi results if in demo mode */
						if (mPrefDemoMode && !demoBeaconFound) {
							mWifiList.add(new WifiScanResult(demoBssid, demoSsid, demoLevel));
						}

						if (mPrefGlobalBeacons) {
							mWifiList.add(new WifiScanResult(globalBssid, globalSsid, globalLevel));
						}

						Logger.d(ProxiConstants.APP_NAME, sModName, "Received wifi scan results");

						/* Start asych task to process the results of the just completed wifi scan. */
						Logger.d(ProxiConstants.APP_NAME, sModName, "Starting AsyncTask to rebuild entities");

						/* We are on the background thread */
						ServiceResponse serviceResponse = processBeaconsFromScan(mWifiList, mOptions.refreshAllBeacons);

						if (serviceResponse.responseCode != ResponseCode.Success) {
							mRequestListener.onComplete(serviceResponse);
						}

						if (mOptions.refreshDirty) {
							serviceResponse = refreshDirtyEntities();
							if (serviceResponse.responseCode != ResponseCode.Success) {
								mRequestListener.onComplete(serviceResponse);
							}
							mEntityProxies = (List<EntityProxy>) serviceResponse.data;
						}

						mScanRequestProcessing = false;

						Logger.d(ProxiConstants.APP_NAME, sModName, "Passing updated proxi beacon collection back to listeners");
						serviceResponse.data = mEntityProxies;
						mRequestListener.onComplete(serviceResponse);
					}
				}
			}, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

			wifiLockAcquire(WifiManager.WIFI_MODE_FULL);

			for (ScanResult scanResult : mWifiManager.getScanResults()) {
				mWifiList.add(new WifiScanResult(scanResult));
			}

			Logger.d(ProxiConstants.APP_NAME, sModName, "Requesting system wifi scan");

			/* Beacon and entity processing start when we recieve the scan results */
			mWifiManager.startScan();
		}
		else {
			Logger.d(ProxiConstants.APP_NAME, sModName, "Emulator enabled so using dummy scan results");

			mWifiList.add(new WifiScanResult(demoBssid, demoSsid, demoLevel));
			mScanRequestProcessing = true;

			/* We are on the background thread */
			ServiceResponse serviceResponse = processBeaconsFromScan(mWifiList, mOptions.refreshAllBeacons);

			if (serviceResponse.responseCode != ResponseCode.Success) {
				listener.onComplete(serviceResponse);
				return;
			}

			if (mOptions.refreshDirty) {
				serviceResponse = refreshDirtyEntities();
				if (serviceResponse.responseCode != ResponseCode.Success) {
					listener.onComplete(serviceResponse);
					return;
				}
				mEntityProxies = (List<EntityProxy>) serviceResponse.data;
			}

			mScanRequestProcessing = false;

			Logger.d(ProxiConstants.APP_NAME, sModName, "Passing updated proxi beacon collection back to listeners");
			serviceResponse.data = mEntityProxies;
			listener.onComplete(serviceResponse);
		}
	}

	/**
	 * Refreshes only beacons and entities that have been flagged as dirty.
	 * 
	 * @return Updated master entity list which includes all entities refreshed or not.
	 * @throws ProxibaseException
	 */
	public ServiceResponse refreshDirtyEntities() {
		/*
		 * For beacons:
		 * - first entity or addtional entities could be added.
		 * - existing entities could be modified or gone.
		 */
		ServiceResponse serviceResponse = new ServiceResponse(ResponseCode.Success, ResultCodeDetail.Success, null, null);;
		for (Beacon beacon : mBeacons) {

			if (Thread.interrupted()) {
				return null;
			}

			if (beacon.isDirty) {
				beacon.isDirty = false;
				beacon.entityProxies.clear();

				Bundle parameters = new Bundle();
				parameters.putString("beaconBssid", beacon.id);
				parameters.putInt("userId", Integer.parseInt(mUser.id));

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE + "GetEntitiesForBeacon");
				serviceRequest.setRequestType(RequestType.Method);
				serviceRequest.setParameters(parameters);
				serviceRequest.setResponseFormat(ResponseFormat.Json);

				serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				if (serviceResponse.responseCode != ResponseCode.Success) {
					return serviceResponse;
				}

				String jsonResponse = (String) serviceResponse.data;
				List<Object> freshEntities = ProxibaseService.convertJsonToObjects(jsonResponse, EntityProxy.class, GsonType.ProxibaseService);

				if (freshEntities != null && freshEntities.size() > 0) {

					for (Object obj : freshEntities) {
						EntityProxy freshEntityProxy = (EntityProxy) obj;

						freshEntityProxy.state = EntityState.Refreshed;
						freshEntityProxy.beacon = beacon;
						setEntityVisibility(freshEntityProxy, beacon);

						for (EntityProxy childEntityProxy : freshEntityProxy.children) {
							childEntityProxy.beacon = beacon;
							childEntityProxy.state = EntityState.Refreshed;
							setEntityVisibility(childEntityProxy, beacon);
						}
						beacon.entityProxies.add(freshEntityProxy);
					}
				}
			}
			else {
				for (int i = beacon.entityProxies.size() - 1; i >= 0; i--) {
					EntityProxy entityProxy = beacon.entityProxies.get(i);
					if (entityProxy.isDirty) {
						/*
						 * Includes call to rebuildEntityProxyList()
						 */
						doRefreshEntity(entityProxy.id);
					}
					else {
						for (int j = entityProxy.children.size() - 1; j >= 0; j--) {
							EntityProxy childEntityProxy = entityProxy.children.get(j);
							if (childEntityProxy.isDirty) {
								doRefreshEntity(childEntityProxy.id);
							}
						}
					}
				}
			}
		}

		rebuildEntityProxyList();
		serviceResponse.data = mEntityProxies;

		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Entities
	// --------------------------------------------------------------------------------------------

	public void initialize() {
		if (!mUsingEmulator) {
			mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		}
	}

	@SuppressWarnings("unused")
	private boolean scanResultsContain(String bsid) {
		for (WifiScanResult wifiScanResult : mWifiList) {
			if (wifiScanResult.BSSID.equals(bsid)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unused")
	private void updateOrInsertDemoBeacon(Beacon beacon, String bssid, String label) {

		/* Keep method until we are sure we won't be using it. */
		if (beacon == null) {
			Beacon beaconNew = new Beacon(bssid, label, label, demoLevel, nowDate());
			beaconNew.detectedLastPass = true;
			beaconNew.state = BeaconState.New;
			beaconNew.addScanPass(demoLevel);
			mBeacons.add(beaconNew);
		}
		else {
			beacon.detectedLastPass = true;
			beacon.state = BeaconState.Normal;
			beacon.addScanPass(demoLevel);
		}
	}

	private ServiceResponse processBeaconsFromScan(List<WifiScanResult> scanList, boolean refreshAllBeacons) {
		/*
		 * Full update: Queries service for entities for every discovered beacon.
		 * Partial update: Queries service for entities for beacons that were not discovered on previous scans.
		 * Entities from previous scans will still be updated for local changes in visibility.
		 */
		Logger.d(ProxiConstants.APP_NAME, sModName, "Processing beacons from scan");
		ServiceResponse serviceResponse = new ServiceResponse(ResponseCode.Success, ResultCodeDetail.Success, null, null);;

		/* Clear beacon collection for a complete rebuild */
		if (refreshAllBeacons)
			mBeacons.clear();

		/* Reset detection flag */
		for (Beacon beacon : mBeacons)
			beacon.detectedLastPass = false;

		/* Walk all the latest wifi scan hits */
		for (int i = 0; i < scanList.size(); i++) {

			final WifiScanResult scanResult = scanList.get(i);

			/* See if we are already tracking the beacon */
			Beacon beaconMatch = findBeaconById(scanResult.BSSID);

			/* Add it if we aren't */
			if (beaconMatch == null) {
				Beacon beaconNew = new Beacon(scanResult.BSSID, scanResult.SSID, scanResult.SSID, scanResult.level, nowDate());
				beaconNew.detectedLastPass = true;
				beaconNew.state = BeaconState.New;
				beaconNew.addScanPass(scanResult.level);
				mBeacons.add(beaconNew);
			}
			else {
				beaconMatch.detectedLastPass = true;
				beaconMatch.state = BeaconState.Normal;
				beaconMatch.addScanPass(scanResult.level);
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
			for (EntityProxy entity : beacon.entityProxies) {
				entity.state = EntityState.Normal;
				for (EntityProxy childEntityProxy : entity.children) {
					childEntityProxy.state = EntityState.Normal;
				}
			}
		}

		/* Construct string array of the beacon ids */
		ArrayList<String> refreshBeaconIds = new ArrayList<String>();
		for (Beacon beacon : mBeacons) {
			if (beacon.state == BeaconState.New) {
				beacon.state = BeaconState.Normal;
				refreshBeaconIds.add(beacon.id);
			}
		}

		if (Thread.interrupted()) {
			return null;
		}

		if (refreshBeaconIds.size() > 0) {

			Bundle parameters = new Bundle();
			parameters.putStringArrayList("beaconBssids", refreshBeaconIds);
			parameters.putInt("userId", Integer.parseInt(mUser.id));

			ServiceRequest serviceRequest = new ServiceRequest();
			serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE + "GetEntitiesForBeacons");
			serviceRequest.setRequestType(RequestType.Method);
			serviceRequest.setParameters(parameters);
			serviceRequest.setResponseFormat(ResponseFormat.Json);

			serviceResponse = NetworkManager.getInstance().request(serviceRequest);

			if (Thread.interrupted()) {
				return null;
			}

			if (serviceResponse.responseCode != ResponseCode.Success) {
				return serviceResponse;
			}

			String jsonResponse = (String) serviceResponse.data;
			List<Object> freshEntities = ProxibaseService.convertJsonToObjects(jsonResponse, EntityProxy.class, GsonType.ProxibaseService);

			/* Match returned entities back to beacons */
			if (freshEntities != null && freshEntities.size() > 0) {
				for (Object obj : freshEntities) {
					EntityProxy freshEntity = (EntityProxy) obj;

					for (Beacon beacon : mBeacons) {
						if (beacon.id.equals(freshEntity.beaconId)) {

							beacon.entityProxies.add(freshEntity);
							freshEntity.state = EntityState.New;
							freshEntity.beacon = beacon;
							for (EntityProxy childEntity : freshEntity.children) {
								childEntity.beacon = beacon;
								childEntity.state = EntityState.New;
							}
						}
					}
				}
			}
		}

		/* Rebuild the top level entity list */
		mEntityProxies.clear();
		for (Beacon beacon : mBeacons) {
			for (EntityProxy entityProxy : beacon.entityProxies) {
				mEntityProxies.add(entityProxy);
			}
		}

		manageEntityVisibility();
		return serviceResponse;
	}

	private Beacon findBeaconById(String beaconId) {
		for (Beacon beacon : mBeacons) {
			if (beacon.id.equals(beaconId)) {
				return beacon;
			}
		}
		return null;
	}

	private ServiceResponse doRefreshEntity(int entityId) {

		ServiceResponse serviceResponse = new ServiceResponse(ResponseCode.Success, ResultCodeDetail.Success, null, null);
		EntityProxy targetEntityProxy = getEntityById(entityId);

		for (Beacon beacon : mBeacons) {
			if (beacon.entityProxies.contains(targetEntityProxy)) {

				/* We are doing a replace not a merge */
				EntityProxy entityProxy = beacon.entityProxies.get(beacon.entityProxies.indexOf(targetEntityProxy));
				Bundle parameters = new Bundle();
				parameters.putInt("entityId", entityProxy.id);
				parameters.putBoolean("includeChildren", true);

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE + "GetEntity");
				serviceRequest.setRequestType(RequestType.Method);
				serviceRequest.setParameters(parameters);
				serviceRequest.setResponseFormat(ResponseFormat.Json);

				serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				if (serviceResponse.responseCode != ResponseCode.Success) {
					return serviceResponse;
				}

				String jsonResponse = (String) serviceResponse.data;
				List<Object> freshEntities = ProxibaseService.convertJsonToObjects(jsonResponse, EntityProxy.class, GsonType.ProxibaseService);

				if (freshEntities != null && freshEntities.size() > 0) {

					for (Object obj : freshEntities) {
						EntityProxy freshEntityProxy = (EntityProxy) obj;

						freshEntityProxy.beacon = beacon;
						freshEntityProxy.state = EntityState.Refreshed;
						setEntityVisibility(freshEntityProxy, beacon);
						for (EntityProxy childEntityProxy : freshEntityProxy.children) {
							childEntityProxy.beacon = beacon;
							childEntityProxy.state = EntityState.Refreshed;
							setEntityVisibility(childEntityProxy, beacon);
						}
						beacon.entityProxies.set(beacon.entityProxies.indexOf(entityProxy), freshEntityProxy);

						return serviceResponse;
					}
				}
				else {
					beacon.entityProxies.remove(beacon.entityProxies.indexOf(entityProxy));
					break;
				}
			}
			else {
				for (EntityProxy entityProxy : beacon.entityProxies) {
					if (entityProxy.children.contains(targetEntityProxy)) {

						EntityProxy childEntityProxy = entityProxy.children.get(entityProxy.children.indexOf(targetEntityProxy));
						Bundle parameters = new Bundle();
						parameters.putInt("entityId", childEntityProxy.id);
						parameters.putBoolean("includeChildren", false);

						ServiceRequest serviceRequest = new ServiceRequest();
						serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE + "GetEntity");
						serviceRequest.setRequestType(RequestType.Method);
						serviceRequest.setParameters(parameters);
						serviceRequest.setResponseFormat(ResponseFormat.Json);

						serviceResponse = NetworkManager.getInstance().request(serviceRequest);

						if (serviceResponse.responseCode != ResponseCode.Success) {
							return serviceResponse;
						}

						String jsonResponse = (String) serviceResponse.data;
						List<Object> freshEntities = ProxibaseService.convertJsonToObjects(jsonResponse, EntityProxy.class,
									GsonType.ProxibaseService);

						if (freshEntities != null && freshEntities.size() > 0) {

							for (Object obj : freshEntities) {
								EntityProxy freshEntityProxy = (EntityProxy) obj;
								freshEntityProxy.beacon = beacon;
								freshEntityProxy.state = EntityState.Refreshed;
								setEntityVisibility(freshEntityProxy, beacon);
								entityProxy.children.set(entityProxy.children.indexOf(childEntityProxy), freshEntityProxy);

								return serviceResponse;
							}
						}
						else {
							entityProxy.children.remove(entityProxy.children.indexOf(childEntityProxy));
							break;
						}
					}
				}
			}
		}
		return serviceResponse;
	}

	private void rebuildEntityProxyList() {

		/* Rebuild the top level entity list when there could have been a top level change */
		mEntityProxies.clear();
		for (Beacon beaconTemp : mBeacons) {
			for (EntityProxy entityProxyTemp : beaconTemp.entityProxies) {
				mEntityProxies.add(entityProxyTemp);
			}
		}
	}

	private void manageEntityVisibility() {

		/* Visibility status effects all entities regardless of whether this is a full or partial update. */
		Logger.d(ProxiConstants.APP_NAME, sModName, "Managing entity visibility");

		for (Beacon beacon : mBeacons) {
			for (EntityProxy entity : beacon.entityProxies) {
				setEntityVisibility(entity, beacon);
			}
		}

		/* Push hidden setting down to children */
		for (Beacon beacon : mBeacons) {
			for (EntityProxy entity : beacon.entityProxies) {
				for (EntityProxy childEntity : entity.children) {
					childEntity.isHidden = entity.isHidden;

					/* If child is going to inherit visibility then perform its own personal visibility check. */
					if (!childEntity.isHidden) {
						setEntityVisibility(childEntity, beacon);
					}
				}
			}
		}
	}

	private void setEntityVisibility(EntityProxy entity, Beacon beacon) {
		boolean oldIsHidden = entity.isHidden;
		entity.isHidden = false;
		/*
		 * Make it harder to fade out than it is to fade in.
		 * Entities are only New for the first scan that discovers them.
		 */
		float signalThresholdFluid = entity.signalFence;
		if (oldIsHidden == false && entity.beacon.state != BeaconState.New)
			signalThresholdFluid = entity.signalFence - 5;

		/* Hide entities that are not within entity declared virtual range */
		if (mPrefEntityFencing && beacon.getAvgBeaconLevel() < signalThresholdFluid) {
			entity.isHidden = true;
			return;
		}
	}

	// --------------------------------------------------------------------------------------------
	// WIFI routines
	// --------------------------------------------------------------------------------------------

	private void wifiLockAcquire(int lockType) {
		if (mWifiLock == null) {
			mWifiLock = mWifiManager.createWifiLock(lockType, "Proxi");
			mWifiLock.setReferenceCounted(false);
		}
		if (!mWifiLock.isHeld())
			mWifiLock.acquire();
	}

	private void wifiReleaseLock() {
		if (mWifiLock.isHeld())
			mWifiLock.release();
	}

	// --------------------------------------------------------------------------------------------
	// MISC routines
	// --------------------------------------------------------------------------------------------

	/**
	 * Call this method when an activity holding a reference to ProxiExplorer is being
	 * paused by the system. This ensures that any ongoing tag scan is cleaned
	 * up properly.
	 */
	public void onPause() {
		try {
			/* Start blocking the processing of any scan messages even if we have an active request */
			mScanRequestActive = false;
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

	/**
	 * Call this method when an activity holding a reference to ProxiExplorer is being
	 * resumed by the system. This ensures that the component is re-initialized properly.
	 */
	public void onResume() {
	/*
	 * This is a placeholder for any future work that should be done
	 * when a parent activity is resumed.
	 */
	}

	private static Date nowDate() {
		Calendar cal = Calendar.getInstance();
		return cal.getTime();
	}

	public void setPrefEntityFencing(boolean prefEntityFencing) {
		this.mPrefEntityFencing = prefEntityFencing;
	}

	public boolean isPrefEntityFencing() {
		return mPrefEntityFencing;
	}

	public Context getContext() {
		return this.mContext;
	}

	public void setContext(Context context) {
		this.mContext = context;
	}

	public List<Beacon> getBeacons() {
		return this.mBeacons;
	}

	public List<EntityProxy> getEntityProxiesFlat() {
		List<EntityProxy> entityProxiesFlat = new ArrayList<EntityProxy>();
		for (Beacon proxiBeacon : mBeacons)
			for (EntityProxy entityProxy : proxiBeacon.entityProxies) {
				entityProxiesFlat.add(entityProxy);
				for (EntityProxy childEntityProxy : entityProxy.children)
					entityProxiesFlat.add(childEntityProxy);
			}
		return entityProxiesFlat;
	}

	public Beacon getStrongestBeacon() {
		int strongestLevelDb = -1000;
		Beacon strongestBeacon = null;
		Beacon demoBeacon = null;
		for (Beacon beacon : mBeacons) {
			if (beacon.id.equals(demoBssid)) {
				demoBeacon = beacon;
			}
			else {
				if (!beacon.id.equals(globalBssid) && beacon.levelDb > strongestLevelDb) {
					strongestBeacon = beacon;
					strongestLevelDb = beacon.levelDb;
				}
			}
		}
		if (strongestBeacon == null && demoBeacon != null) {
			strongestBeacon = demoBeacon;
		}

		return strongestBeacon;
	}

	public EntityProxy getEntityById(int entityId) {
		for (Beacon beacon : mBeacons) {
			for (EntityProxy entity : beacon.entityProxies) {
				if (entity.id.equals(entityId)) {
					return entity;
				}
				for (EntityProxy childEntity : entity.children) {
					if (childEntity.id.equals(entityId)) {
						return childEntity;
					}
				}
			}
		}
		return null;
	}

	public void setPrefDemoMode(boolean prefDemoMode) {
		mPrefDemoMode = prefDemoMode;

		/* Make sure we clear any demo beacons */
		if (!mPrefDemoMode) {
			for (int i = mBeacons.size() - 1; i >= 0; i--) {
				if (mBeacons.get(i).label.equals("demo_dolly")) {
					mBeacons.remove(i);
				}
			}
		}
	}

	public boolean isPrefDemoMode() {
		return mPrefDemoMode;
	}

	public void setPrefGlobalBeacons(boolean prefGlobalBeacons) {
		mPrefGlobalBeacons = prefGlobalBeacons;

		/* Make sure we clear any global beacons */
		if (!mPrefGlobalBeacons) {
			for (int i = mBeacons.size() - 1; i >= 0; i--) {
				if (mBeacons.get(i).label.equals("candi_feed")) {
					mBeacons.remove(i);
				}
			}
		}
	}

	public boolean isPrefGlobalBeacons() {
		return mPrefGlobalBeacons;
	}

	public void setUsingEmulator(boolean usingEmulator) {
		mUsingEmulator = usingEmulator;
	}

	public boolean isUsingEmulator() {
		return mUsingEmulator;
	}

	public User getUser() {
		return mUser;
	}

	public void setUser(User user) {
		mUser = user;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public static class Options {

		public boolean	refreshDirty		= false;
		public boolean	refreshAllBeacons	= true;

		public Options(boolean refreshAllBeacons, boolean refreshDirty) {
			this.refreshDirty = refreshDirty;
			this.refreshAllBeacons = refreshAllBeacons;
		}
	}

	public class WifiScanResult {

		public String	BSSID;
		public String	SSID;
		public int		level	= 0;

		public WifiScanResult(String bssid, String ssid, int level) {
			this.BSSID = bssid;
			this.SSID = ssid;
			this.level = level;
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
		public void onComplete(List<EntityProxy> proxiEntities);

		/**
		 * Called when the server-side Proxibase method fails.
		 * Executed by a background thread: do not update the UI in this method.
		 */
		public void onProxibaseException(ProxibaseException exception);
	}
}