package com.proxibase.aircandi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ViewFlipper;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.proxibase.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.proxibase.aircandi.components.EndlessCandiListAdapter;
import com.proxibase.aircandi.components.EntityList;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.ProxiExplorer.CollectionType;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.objects.Entity;

public class CandiPicker extends FormActivity implements OnItemClickListener, ActionBar.TabListener {

	private ListView			mListViewUserCandi;
	private ListView			mListViewRadarCandi;
	private EntityList<Entity>	mEntitiesUserCandi;
	private EntityList<Entity>	mEntitiesRadarCandi;
	private ViewFlipper			mViewFlipper;
	private CollectionType		mMethodType	= CollectionType.CandiByRadar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
	}

	private void initialize() {
		mCommon.track();

		/* Action bar */
		mCommon.mActionBar.setTitle(R.string.form_title_candi);
		
		mListViewRadarCandi = (ListView) findViewById(R.id.list_radar_candi);
		mListViewRadarCandi.setOnItemClickListener(this);
		mListViewRadarCandi.setDivider(null);
		mEntitiesRadarCandi = ProxiExplorer.getInstance().getEntityModel().getCollectionById(ProxiConstants.ROOT_COLLECTION_ID, CollectionType.CandiByRadar);

		mListViewUserCandi = (ListView) findViewById(R.id.list_user_candi);
		mListViewUserCandi.setOnItemClickListener(this);
		mListViewUserCandi.setDivider(null);
		mEntitiesUserCandi = ProxiExplorer.getInstance().getEntityModel().getCollectionById(ProxiConstants.ROOT_COLLECTION_ID, CollectionType.CandiByUser);

		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);

		if (mViewFlipper != null) {
			mCommon.setActiveTab(0);
			mMethodType = ProxiExplorer.CollectionType.CandiByRadar;
			bind();
		}
	}

	public void bind() {

		if (mMethodType == ProxiExplorer.CollectionType.CandiByRadar) {
			mListViewRadarCandi.setAdapter(new EndlessCandiListAdapter(CandiPicker.this, mEntitiesRadarCandi, R.layout.temp_listitem_candi));
		}
		else if (mMethodType == ProxiExplorer.CollectionType.CandiByUser) {
			mListViewUserCandi.setAdapter(new EndlessCandiListAdapter(CandiPicker.this, mEntitiesUserCandi, R.layout.temp_listitem_candi));
		}
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

	public void onTabClick(View view) {}

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

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		if (tab.getTag().equals("radar")) {
			mViewFlipper.setDisplayedChild(0);
			mMethodType = ProxiExplorer.CollectionType.CandiByRadar;
			if (mListViewRadarCandi.getAdapter() == null) {
				bind();
			}
		}
		else if (tab.getTag().equals("mycandi")) {
			mViewFlipper.setDisplayedChild(1);
			mMethodType = ProxiExplorer.CollectionType.CandiByUser;
			if (mListViewUserCandi.getAdapter() == null) {
				bind();
			}
		}
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub

	}
}