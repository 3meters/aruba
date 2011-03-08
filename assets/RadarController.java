package com.georain.ripple.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.http.client.ClientProtocolException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.widget.Toast;
import com.georain.ripple.model.BaseModifyListener;
import com.georain.ripple.model.Entity;
import com.georain.ripple.model.Query;
import com.georain.ripple.model.RippleService;

public class RadarController
{
	/**
	 * Takes the results of the latest wifi scan and update the pointList. We return true if the operation succeeded and
	 * false if it fails. The most likely cause of failure is no network connection at the time we tried to access it.
	 * 
	 * @param pointList
	 * @param scanList
	 */
	private List<ScanResult>	mWifiList;
	private ArrayList<Entity>	mPointList						= new ArrayList<Entity>();
	public WifiManager			mWifiManager;
	public boolean				spotCountChanged;
	private WifiLock			mWifiLock;
	private Context				mContext;
	public WifiReceiver			mWifiReceiver;
	public BaseModifyListener	mListener;
	public static final String	RADAR_RESULTS_AVAILABLE_ACTION	= "update";

	public RadarController(Context context, Radar radar) {
		this.mContext = context;
		this.mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	}

	// Any checks for whether wifi is enable or not is done by the caller
	public void wifiScan(BaseModifyListener listener)
	{
		this.mListener = listener;
		aquireWifiLock(WifiManager.WIFI_MODE_FULL);
		mWifiReceiver = new WifiReceiver();
		mContext.registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		mWifiList = mWifiManager.getScanResults();
		mWifiManager.startScan();
	}

	/**
	 * This is called whenever we get the results of a new wifi scan. We need to merge any changes together with our
	 * existing collection of points to reflect the current state of affairs.
	 */
	public void rebuildRadar(Context context, BaseModifyListener listener)
	{
		// Do the work to update the collection of points. A network issue will be flagged in the service and we
		// still get back a raw collection of wifi points.
		int pointListPrevCount = mPointList.size();
		ArrayList<Entity> pointListNew = RadarController.refreshActivePointsClient(mPointList, mWifiList);
		if (pointListNew == null)
			return;

		mPointList.clear();
		for (Entity point : pointListNew)
			mPointList.add(point);

		// Do we have a point that is currently selected? It's only one now but might be multiple in the future
		List<Entity> pointSelectedList = RadarController.getPointsSelected(mPointList);
		if (pointSelectedList.size() != 0)
			((Ripple) context.getApplicationContext()).currentEntityX = pointSelectedList.get(0);
		else
			((Ripple) context.getApplicationContext()).currentEntityX = null;

		// For UI routines, we flag whether the number of points has changed
		if (mPointList.size() != pointListPrevCount)
			spotCountChanged = true;
		else
			spotCountChanged = false;

		// Sort the point collection by point type and strength of signal
		Collections.sort(mPointList, new byZoneLevel());
		
		// Call back
		listener.onComplete();
	}

	public class WifiReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(final Context context, Intent intent)
		{
			// Unregister the receiver so we don't get called again. If we want to get notified
			// of scans triggered in the system by any application then we should stay registered.
			context.unregisterReceiver(mWifiReceiver);

			// Get the latest scan results
			mWifiList = mWifiManager.getScanResults();

			// Kick off the process of integrating the new scan on a separate thread
			new Thread() {
				@Override
				public void run()
				{
					rebuildRadar(context, new RebuildRadarListener());
				}
			}.start();

		}
	}

	public class RebuildRadarListener extends BaseModifyListener
	{
		public void onComplete()
		{
			// Let the radar ui know that there are fresh results to process
			try
			{
				mListener.onComplete();
			}
			catch (Exception e)
			{
				RippleUI.showToastNotification(mContext, "Radar callback failed.", Toast.LENGTH_SHORT);
			}
		}
	}

	public void onPause()
	{
		try
		{
			mContext.unregisterReceiver(mWifiReceiver);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void onResume()
	{
		try
		{
			// mContext.registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void onDestroy()
	{
		// We are aggressive about hold our wifi lock so we need to be sure
		// it gets released when we are destroyed.
		try
		{
			releaseWifiLock();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void releaseWifiLock()
	{
		if (mWifiLock.isHeld())
			mWifiLock.release();
	}

	public static ArrayList<Entity> refreshActivePointsClient(List<Entity> pointList, List<ScanResult> scanList)
	{
		ArrayList<Entity> pointListNew = new ArrayList<Entity>();

		// Filter out the ripple spots
		for (int i = 0; i < scanList.size(); i++)
		{
			ScanResult mScanResult = scanList.get(i);
			String bssid = mScanResult.BSSID;
			String label = mScanResult.SSID;
			String ssid = mScanResult.SSID;
			int levelDb = mScanResult.level;

			// Check to see if we already have this point
			Entity pointExisting = getPointByBssidClient(pointList, bssid);

			// Point might already exist but have been updated in the service.
			// Check with service to see if this point has been ripplized.
			// If call comes back null then there was a network or service problem.
			// The user got a toast notification from the service and we will still display
			// the point as raw wifi only.
			Query query = new Query("Points").filter("Bssid eq '" + bssid + "'");

			RippleService ripple = new RippleService();
			String response;
			try
			{
				response = ripple.select(query, Entity.class);
				ArrayList<Object> points = RippleService.convertJsonToObjects(response, Entity.class);

				Entity point = null;
				if (points != null && points.size() != 0)
					point = (Entity) points.get(0);

				if (point != null)
				{
					point.isTagged = true;
					point.isServiceVerified = true;
					point.levelDb = levelDb;
				}
				else
				{
					point = new Entity(label, bssid, ssid, levelDb);
					if (points != null)
						point.isServiceVerified = true;
					else
						point.isServiceVerified = false;
				}

				// If we have already accumulated scan stats, transfer them.
				if (pointExisting != null)
				{
					point.scanPasses = pointExisting.scanPasses;
					point.isSelected = pointExisting.isSelected;
				}
				point.addScanPass(levelDb);
				pointListNew.add(point);
			}
			catch (ClientProtocolException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// We need to move over any points in the previous pointList that were not in the current scanList
		for (Entity point : pointList)
			if (getPointByBssidClient(pointListNew, point.tagId) == null)
			{
				point.scanMisses++;
				if (point.scanMisses < 3)
					pointListNew.add(point);
			}

		// Replace the core point list
		return pointListNew;
	}

	public class byZoneLevel implements Comparator<Entity>
	{
		@Override
		public int compare(Entity object1, Entity object2)
		{
			if (object1.isTagged && !object2.isTagged)
				return -1;
			else if (object2.isTagged && !object1.isTagged)
				return 1;
			else
				return object2.getAvgPointLevel() - object1.getAvgPointLevel();
		}
	}

	public static Entity getPointByBssidClient(List<Entity> pointList, String bssid)
	{
		int pointListCount = pointList.size();
		for (int i = 0; i < pointListCount; i++)
			if (pointList.get(i).tagId.equals(bssid.toString()))
				return pointList.get(i);
		return null;
	}

	public static List<Entity> getPointsSelected(List<Entity> pointList)
	{
		List<Entity> pointListSelected = new ArrayList<Entity>();
		for (int i = 0; i < pointList.size(); i++)
		{
			if (pointList.get(i).isSelected)
				pointListSelected.add(pointList.get(i));
		}
		return pointListSelected;
	}

	public void aquireWifiLock(int lockType)
	{
		if (mWifiLock == null)
		{
			mWifiLock = mWifiManager.createWifiLock(lockType, "Ripple");
			mWifiLock.setReferenceCounted(false);
		}
		if (!mWifiLock.isHeld())
			mWifiLock.acquire();
	}

	public ArrayList<Entity> getRadarList()
	{
		return mPointList;
	}

	public enum WifiAction
	{
		Enabled, Enabling, Disabled, EnableAndRetry
	}

}
