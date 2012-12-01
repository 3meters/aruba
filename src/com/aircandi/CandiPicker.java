package com.aircandi;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.CandiListAdapter;
import com.aircandi.components.CandiListAdapter.CandiListViewHolder;
import com.aircandi.components.EntityList;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.EntityTree;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.objects.Entity;

public class CandiPicker extends FormActivity implements ActionBar.TabListener {

	private ListView			mListViewCandi;
	private EntityList<Entity>	mEntities	= new EntityList<Entity>();
	private EntityTree			mMethodType	= EntityTree.Radar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
		bind();
	}

	private void initialize() {
		mListViewCandi = (ListView) findViewById(R.id.list_candi);
		mListViewCandi.setDivider(null);
	}

	public void bind() {
		if (mMethodType == EntityTree.Radar) {
			EntityList<Entity> entities = ProxiExplorer.getInstance().getEntityModel().getRadarPlaces();
			if (entities != null) {
				mEntities.clear();
				for (Entity entity : entities) {
					if (entity.type.equals(CandiConstants.TYPE_CANDI_FOLDER) && !entity.locked) {
						mEntities.add(entity);
					}
				}
				CandiListAdapter adapter = new CandiListAdapter(CandiPicker.this, mEntities, R.layout.temp_listitem_candi_picker);
				mListViewCandi.setAdapter(adapter);
			}
		}
		else if (mMethodType == EntityTree.User) {

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showProgressDialog(getString(R.string.progress_loading), true);
				}

				@Override
				protected Object doInBackground(Object... params) {
					ModelResult result = ProxiExplorer.getInstance().getEntityModel()
							.getUserEntities(Aircandi.getInstance().getUser().id, false, ProxiConstants.RADAR_ENTITY_LIMIT);
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {

					ModelResult result = (ModelResult) response;
					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						/*
						 * These only get set on the first data pass, chunking does not change them.
						 */
						if (result.data != null) {
							EntityList<Entity> entitiesUserCandi = (EntityList<Entity>) result.data;
							mEntities.clear();
							for (Entity entity : entitiesUserCandi) {
								if (entity.isCollection && !entity.locked) {
									mEntities.add(entity);
								}
							}
							CandiListAdapter adapter = new CandiListAdapter(CandiPicker.this, mEntities, R.layout.temp_listitem_candi_picker);
							mListViewCandi.setAdapter(adapter);
						}
					}
					else {
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.PickCandi);
					}
					mCommon.hideProgressDialog();
				}

			}.execute();
		}
	}

	public void onListItemClick(View view) {
		Entity entity = (Entity) ((CandiListViewHolder) view.getTag()).data;
		Intent intent = new Intent();
		intent.putExtra(getString(R.string.EXTRA_ENTITY_ID), entity.id);
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	public void onMoveSoloButtonClick(View view) {
		/*
		 * Not returning an entity id is a message that the user has choosen
		 * to go solo with the candi.
		 */
		Intent intent = new Intent();
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

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
		if (((Integer) tab.getTag()) == R.string.candi_picker_tab_radar) {
			mMethodType = ProxiExplorer.EntityTree.Radar;
			bind();
		}
		else if (((Integer) tab.getTag()) == R.string.candi_picker_tab_mycandi) {
			mMethodType = ProxiExplorer.EntityTree.User;
			bind();
		}

	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {}
}