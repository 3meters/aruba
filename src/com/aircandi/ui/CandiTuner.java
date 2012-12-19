package com.aircandi.ui;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.CandiListAdapter;
import com.aircandi.components.EntityList;
import com.aircandi.components.FontManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

public class CandiTuner extends FormActivity {

	private ListView			mListViewCandi;

	private EntityList<Entity>	mEntities	= new EntityList<Entity>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
		bind();
	}

	private void initialize() {
		mListViewCandi = (ListView) findViewById(R.id.list);
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.message));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.title));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_cancel));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_tune));
	}

	public void bind() {

		EntityList<Entity> entities = ProxiExplorer.getInstance().getEntityModel().getRadarPlaces();
		if (entities != null) {
			entities.addAll(ProxiExplorer.getInstance().getEntityModel().getRadarSynthetics());
		}

		if (entities != null) {
			mEntities.clear();
			for (Entity entity : entities) {
				if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
					entity.checked = false;
					mEntities.add(entity);
				}
			}
		}
		CandiListAdapter adapter = new CandiListAdapter(CandiTuner.this, mEntities, R.layout.temp_listitem_candi_tuner);
		mListViewCandi.setAdapter(adapter);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onTuneButtonClick(View view) {
		tune();
	}

	public void onListItemClick(View view) {
		CheckBox check = (CheckBox) view.findViewById(R.id.check);
		check.setChecked(!check.isChecked());
		Entity entity = (Entity) check.getTag();
		entity.checked = check.isChecked();
	}

	// --------------------------------------------------------------------------------------------
	// Core routines
	// --------------------------------------------------------------------------------------------

	public void tune() {

		final List<Beacon> beacons = ProxiExplorer.getInstance().getStrongestBeacons(5);
		final Beacon primaryBeacon = beacons.size() > 0 ? beacons.get(0) : null;

		new AsyncTask<Object, Object, Object>() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy(R.string.progress_tuning);
			}

			@Override
			protected Object doInBackground(Object... params) {

				List<Entity> entities = new ArrayList<Entity>();
				for (Entity entity : mEntities) {
					if (entity.checked) {
						entities.add(entity);
					}
				}
				ServiceResponse serviceResponse = ProxiExplorer.getInstance().tune(beacons, primaryBeacon, entities);
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				ServiceResponse serviceResponse = (ServiceResponse) response;
				mCommon.hideBusy();
				if (serviceResponse.responseCode != ResponseCode.Success) {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.Tuning);
				}
				else {
					/* Clear checks */
					for (Entity entity : mEntities) {
						entity.checked = false;
					}
					finish();
					AnimUtils.doOverridePendingTransition(CandiTuner.this, TransitionType.FormToCandiPage);
				}
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		mCommon.startScanService(CandiConstants.INTERVAL_SCAN_RADAR);
	}

	@Override
	protected void onPause() {
		mCommon.stopScanService();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		for (Entity entity : mEntities) {
			if (entity.photo != null && entity.photo.getBitmap() != null && !entity.photo.getBitmap().isRecycled()) {
				entity.photo.getBitmap().recycle();
				entity.photo.setBitmap(null);
			}
		}
		System.gc();
	}

	@Override
	protected int getLayoutID() {
		return R.layout.candi_tuner;
	}

}