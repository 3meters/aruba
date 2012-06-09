package com.proxibase.aircandi;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.proxibase.aircandi.CandiList.MethodType;
import com.proxibase.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.proxibase.aircandi.components.EndlessCandiListAdapter;
import com.proxibase.aircandi.components.EntityProvider;
import com.proxibase.service.objects.Entity;

public class CandiPicker extends FormActivity implements OnItemClickListener {

	private ListView		mListViewUserCandi;
	private ListView		mListViewRadarCandi;
	private EntityProvider	mEntityProviderUserCandi;
	private EntityProvider	mEntityProviderRadarCandi;
	private ViewFlipper		mViewFlipper;
	private MethodType		mMethodType	= MethodType.CandiByRadar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
	}

	private void initialize() {
		mCommon.track();

		mListViewRadarCandi = (ListView) findViewById(R.id.list_radar_candi);
		mListViewRadarCandi.setOnItemClickListener(this);
		mListViewRadarCandi.setDivider(null);
		mEntityProviderRadarCandi = new EntityProvider(MethodType.CandiByRadar
				, Aircandi.getInstance().getUser().id
				, mCommon.mEntity != null ? mCommon.mEntity.id : null);

		mListViewUserCandi = (ListView) findViewById(R.id.list_user_candi);
		mListViewUserCandi.setOnItemClickListener(this);
		mListViewUserCandi.setDivider(null);
		mEntityProviderUserCandi = new EntityProvider(MethodType.CandiByUser
				, Aircandi.getInstance().getUser().id
				, mCommon.mEntity != null ? mCommon.mEntity.id: null);
		
		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		
		if (mViewFlipper != null) {
			mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(0));
			mMethodType = MethodType.CandiByRadar;
			bind();
		}
	}

	public void bind() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, "Loading...");
			}

			@Override
			protected Object doInBackground(Object... params) {
				List<Entity> entities = null;
				if (mMethodType == MethodType.CandiByUser) {
					entities = mEntityProviderUserCandi.loadEntities();
				}
				else if (mMethodType == MethodType.CandiByRadar) {
					entities = mEntityProviderRadarCandi.loadEntities();
				}
				return entities;
			}

			@Override
			protected void onPostExecute(Object entities) {
				if (entities != null) {
					if (mMethodType == MethodType.CandiByRadar) {
						mListViewRadarCandi.setAdapter(new EndlessCandiListAdapter(CandiPicker.this, (List<Entity>) entities, mEntityProviderRadarCandi,
								R.layout.temp_listitem_candi));
					}
					else if (mMethodType == MethodType.CandiByUser) {
						mListViewUserCandi.setAdapter(new EndlessCandiListAdapter(CandiPicker.this, (List<Entity>) entities, mEntityProviderUserCandi,
								R.layout.temp_listitem_candi));
					}
				}
				mCommon.showProgressDialog(false, null);
			}
		}.execute();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Entity entity = (Entity) ((CandiListViewHolder) view.getTag()).data;
		Intent intent = new Intent();
		intent.putExtra(getString(R.string.EXTRA_ENTITY_ID), entity.id);
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onTabClick(View view) {
		mCommon.setActiveTab(view);
		if (view.getTag().equals("radar")) {
			mViewFlipper.setDisplayedChild(0);
			mMethodType = MethodType.CandiByRadar;
			if (mListViewRadarCandi.getAdapter() == null) {
				bind();
			}
		}
		else if (view.getTag().equals("mycandi")) {
			mViewFlipper.setDisplayedChild(1);
			mMethodType = MethodType.CandiByUser;
			if (mListViewUserCandi.getAdapter() == null) {
				bind();
			}
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected int getLayoutID() {
		return R.layout.candi_picker;
	}
}