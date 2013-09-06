package com.aircandi.ui.edit;

import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.aircandi.Aircandi;
import com.aircandi.ProxiConstants;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.ProximityManager.ScanReason;
import com.aircandi.components.Tracker;
import com.aircandi.events.BeaconsLockedEvent;
import com.aircandi.events.QueryWifiScanReceivedEvent;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.squareup.otto.Subscribe;

public class TuningEdit extends BaseEntityEdit {

	private Button	mButtonTune;
	private Button	mButtonUntune;

	private Boolean	mTuned				= false;
	private Boolean	mUntuned			= false;
	private Boolean	mTuningInProcess	= false;
	private Boolean	mUntuning			= false;
	private Boolean	mFirstTune 			= true;

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mButtonTune = (Button) findViewById(R.id.button_tune);
		mButtonUntune = (Button) findViewById(R.id.button_untune);
}

	@Override
	public void draw() {

		if (mEntity != null) {

			/* Color */

			final Entity entity = mEntity;

			/* Tuning buttons */
			final Boolean hasActiveProximityLink = entity.hasActiveProximity();
			if (hasActiveProximityLink) {
				mFirstTune = false;
				mButtonUntune.setVisibility(View.VISIBLE);
			}

			drawPhoto();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onEditButtonClick(View view) {
		Routing.route(this, Route.Edit, mEntity);
	}

	@SuppressWarnings("ucd")
	public void onTuneButtonClick(View view) {
		if (!mTuned) {
			Tracker.sendEvent("ui_action", "tune_place", null, 0, Aircandi.getInstance().getUser());
			mUntuning = false;
			mBusyManager.showBusy(R.string.progress_tuning);
			if (NetworkManager.getInstance().isWifiEnabled()) {
				mTuningInProcess = true;
				ProximityManager.getInstance().scanForWifi(ScanReason.query);
			}
			else {
				tuneProximity();
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onUntuneButtonClick(View view) {
		if (!mUntuned) {
			Tracker.sendEvent("ui_action", "untune_place", null, 0, Aircandi.getInstance().getUser());
			mUntuning = true;
			mBusyManager.showBusy(R.string.progress_tuning);
			if (NetworkManager.getInstance().isWifiEnabled()) {
				mTuningInProcess = true;
				ProximityManager.getInstance().scanForWifi(ScanReason.query);
			}
			else {
				tuneProximity();
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	protected String getLinkType() {
		return null;
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Subscribe
	@SuppressWarnings("ucd")
	public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

		if (mTuningInProcess) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Logger.d(TuningEdit.this, "Query wifi scan received event: locking beacons");
					if (event.wifiList != null) {
						ProximityManager.getInstance().lockBeacons();
					}
					else {
						/*
						 * We fake that the tuning happened because it is simpler than enabling/disabling ui
						 */
						setSupportProgressBarIndeterminateVisibility(false);
						hideBusy();
						if (mUntuning) {
							mButtonUntune.setText(R.string.form_button_tuning_tuned);
							mUntuned = true;
						}
						else {
							mButtonTune.setText(R.string.form_button_tuning_tuned);
							mTuned = true;
						}
						mTuningInProcess = false;
					}
				}
			});
		}
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onBeaconsLocked(BeaconsLockedEvent event) {

		if (mTuningInProcess) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Logger.d(TuningEdit.this, "Beacons locked event: tune entity");
					tuneProximity();
				}
			});
		}
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

	private void tuneProximity() {
		/*
		 * If there are beacons:
		 * 
		 * - links to beacons created.
		 * - link_proximity action logged.
		 * 
		 * If no beacons:
		 * 
		 * - no links are created.
		 * - entity_proximity action logged.
		 */
		final List<Beacon> beacons = ProximityManager.getInstance().getStrongestBeacons(ProxiConstants.PROXIMITY_BEACON_COVERAGE);
		final Beacon primaryBeacon = (beacons.size() > 0) ? beacons.get(0) : null;

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy();
				mBusyManager.startBodyBusyIndicator();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("TrackEntityProximity");

				final ModelResult result = EntityManager.getInstance().trackEntity(mEntity
						, beacons
						, primaryBeacon
						, mUntuning);

				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				setSupportProgressBarIndeterminateVisibility(false);
				hideBusy();
				mBusyManager.stopBodyBusyIndicator();
				
				if (mTuned || mUntuned) {
					/* Undoing a tuning */
					mButtonTune.setText(R.string.form_button_tuning_tune);
					mButtonUntune.setText(R.string.form_button_tuning_untune);
					mUntuned = false;
					mTuned = false;
					if (!mFirstTune) {
						mButtonUntune.setVisibility(View.VISIBLE);
					}
					else {
						mButtonUntune.setVisibility(View.GONE);
					}
				}
				else {
					/* Tuning or untuning */
					if (mUntuning) {
						mButtonUntune.setText(R.string.form_button_tuning_tuned);
						mButtonTune.setText(R.string.form_button_tuning_undo);
						mUntuned = true;
					}
					else {
						mButtonTune.setText(R.string.form_button_tuning_tuned);
						mButtonUntune.setText(R.string.form_button_tuning_undo);
						mTuned = true;
						if (mButtonUntune.getVisibility() != View.VISIBLE) {
							mButtonUntune.setVisibility(View.VISIBLE);
						}
					}
				}
				mTuningInProcess = false;
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.tuning_edit;
	}
}