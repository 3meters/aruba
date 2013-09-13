// $codepro.audit.disable fileComment
package com.aircandi;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

import com.aircandi.components.Logger;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ScanReason;

public class ScanService extends Service {

	private ScanTask	mScanTask;

	/**
	 * Simply return null, since our SERVICE will not be communicating with
	 * any other components. It just does its work silently.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * This is called on 2.0+ (API level 5 or higher). Returning
	 * START_NOT_STICKY tells the system to not restart the SERVICE if it is
	 * killed because of poor resource (memory/cpu) conditions.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mScanTask = new ScanTask();
		handleIntent(intent);
		return START_NOT_STICKY;
	}

	private void handleIntent(Intent intent) {
		if (mScanTask.getStatus().equals(AsyncTask.Status.FINISHED)
				|| mScanTask.getStatus().equals(AsyncTask.Status.PENDING)) {
			mScanTask.execute();
		}
	}

	private class ScanTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			Thread.currentThread().setName("WifiScanService");				 //$NON-NLS-1$
			/*
			 * Kicks off the scan but we won't know if it was completed. Will exit
			 * early if a scan is already active.
			 */
			Logger.v(ScanService.this, "Wifi scan SERVICE: requesting wifi scan"); //$NON-NLS-1$
			ProximityManager.getInstance().scanForWifi(ScanReason.MONITORING);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			stopSelf();
		}
	}
}
