package com.aircandi.ui.edit;

import java.util.List;

import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.aircandi.Aircandi;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.BeaconsLockedEvent;
import com.aircandi.components.BusProvider;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.ProximityManager.ScanReason;
import com.aircandi.components.QueryWifiScanReceivedEvent;
import com.aircandi.components.Tracker;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Place;
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

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mButtonTune = (Button) findViewById(R.id.button_tune);
		mButtonUntune = (Button) findViewById(R.id.button_untune);
	}

	@Override
	protected void draw() {

		if (mEntity != null) {

			/* Color */

			((ImageView) findViewById(R.id.image_star_banner)).setColorFilter(getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_ATOP);
			((ImageView) findViewById(R.id.image_star_tune)).setColorFilter(getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_ATOP);

			final Entity entity = mEntity;

			/* Tuning buttons */
			final Boolean hasActiveProximityLink = entity.hasActiveProximityLink();
			if (hasActiveProximityLink) {
				mButtonUntune.setVisibility(View.VISIBLE);
			}

			drawPhoto();
		}
	}

	@Override
	protected void drawPhoto() {
		super.drawPhoto();
		/*
		 * Special color layering if we are using the category photo.
		 */
		Place place = (Place) mEntity;
		if (place.photo == null && place.category != null) {

			final int color = Place.getCategoryColor((place.category != null)
					? place.category.name
					: null, true, mMuteColor, false);

			mPhoto.getImageView().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
			Integer colorResId = Place.getCategoryColorResId(place.category != null ? place.category.name : null,
					true, mMuteColor, false);

			if (findViewById(R.id.color_layer) != null) {
				(findViewById(R.id.color_layer)).setBackgroundResource(colorResId);
				(findViewById(R.id.color_layer)).setVisibility(View.VISIBLE);
				(findViewById(R.id.reverse_layer)).setVisibility(View.VISIBLE);
			}
			else {
				mPhoto.getImageView().setBackgroundResource(colorResId);
			}
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
				enableEvents();
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
				enableEvents();
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

	private void toggleStarOn(final Integer starId) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (mThemeTone.equals("dark")) {
					((ImageView) findViewById(starId)).setImageResource(R.drawable.ic_action_star_10_dark);
				}
				else {
					((ImageView) findViewById(starId)).setImageResource(R.drawable.ic_action_star_10_light);
				}

			}
		});
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
						disableEvents();
						setSupportProgressBarIndeterminateVisibility(false);
						mBusyManager.hideBusy();
						if (mUntuning) {
							mButtonUntune.setText(R.string.form_button_tuning_tuned);
							mUntuned = true;
						}
						else {
							mButtonTune.setText(R.string.form_button_tuning_tuned);
							mTuned = true;
						}
						toggleStarOn(R.id.image_star_tune);
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
					disableEvents();
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
				mBusyManager.hideBusy();
				if (mUntuning) {
					mButtonUntune.setText(R.string.form_button_tuning_tuned);
					mUntuned = true;
				}
				else {
					mButtonTune.setText(R.string.form_button_tuning_tuned);
					mTuned = true;
					if (mButtonUntune.getVisibility() != View.VISIBLE) {
						mButtonUntune.setVisibility(View.VISIBLE);
					}
				}
				toggleStarOn(R.id.image_star_tune);
				mTuningInProcess = false;
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onStop() {
		disableEvents();
		super.onStop();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	private void enableEvents() {
		BusProvider.getInstance().register(this);
	}

	private void disableEvents() {
		try {
			BusProvider.getInstance().unregister(this);
		}
		catch (Exception e) {} // $codepro.audit.disable emptyCatchClause
	}

	@Override
	protected int getLayoutId() {
		return R.layout.tuning_edit;
	}
}