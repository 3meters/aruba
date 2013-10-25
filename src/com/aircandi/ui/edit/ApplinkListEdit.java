package com.aircandi.ui.edit;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.EntityManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.base.BaseEntityListEdit;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;

public class ApplinkListEdit extends BaseEntityListEdit {

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mListItemResId = R.layout.temp_listitem_applink_edit;
		mMessage.setText(R.string.applink_list_edit_empty);
	}

	@Override
	protected ArrayAdapter getAdapter() {
		return new ListAdapter(this, mEntities, mListItemResId, mMessage);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onRefresh() {
		refreshApplinks(mEntities);
	}

	@SuppressWarnings("ucd")
	public void onSearchLinksButtonClick(View view) {
		searchApplinks(mEntities, true, (Place) mParent, true);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ENTITY_INSERT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
					if (jsonEntity != null) {

						final Entity entityNew = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
						if (entityNew != null) {
							searchApplinks(mEntities, true, (Place) mParent, false);
						}
					}
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

	private void searchApplinks(final List<Entity> applinks, final Boolean autoInsert, final Place entity, final Boolean userInitiated) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				showBusy(R.string.progress_applink_search, false);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("ApplinkSearch");
				ModelResult result = EntityManager.getInstance().searchApplinks(applinks, entity, ServiceConstants.TIMEOUT_APPLINK_SEARCH, true);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					final List<Entity> applinks = (List<Entity>) result.data;
					/*
					 * Make sure they have the schema property set
					 */
					for (Entity applink : applinks) {
						if (applink.schema == null) {
							applink.schema = Constants.SCHEMA_ENTITY_APPLINK;
						}
					}

					if (autoInsert) {
						if (applinks.size() > 0) {

							int activeCountOld = mEntities.size();
							int activeCountNew = applinks.size();

							mEntities.clear();
							mEntities.addAll(applinks);

							for (Entity entity : mEntities) {
								entity.checked = false;
							}

							rebuildPositions();
							mAdapter.notifyDataSetChanged();

							if (activeCountNew == activeCountOld) {
								if (userInitiated) {
									UI.showToastNotification(getResources().getString(R.string.toast_applinks_no_links), Toast.LENGTH_SHORT);
								}
							}
							else {
								mDirty = true;
								UI.showToastNotification(getResources().getString((applinks.size() == 1)
										? R.string.toast_applinks_linked
										: R.string.toast_applinks_linked), Toast.LENGTH_SHORT);
							}
						}

					}
				}
				hideBusy();
			}
		}.execute();
	}

	private void refreshApplinks(final List<Entity> applinks) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				showBusy(R.string.progress_applink_refresh, false);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("ApplinkRefresh");
				List<Entity> entities = new ArrayList<Entity>();
				for (Entity applink : applinks) {
					if (applink.id != null) {
						entities.add(applink);
					}
				}
				ModelResult result = EntityManager.getInstance().refreshApplinks(entities, ServiceConstants.TIMEOUT_APPLINK_REFRESH, true);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					final List<Entity> applinks = (List<Entity>) result.data;

					if (applinks.size() > 0) {
						for (Entity entity : mEntities) {
							for (Entity applink : applinks) {
								if (applink.id != null && entity.id != null && applink.id.equals(entity.id)) {
									mDirty = true;
									entity.name = applink.name;
									entity.description = applink.description;
									if (applink.photo != null) {
										entity.photo = applink.photo.clone();
									}
									entity.data = applink.data;
									((Applink) entity).appId = ((Applink) applink).appId;
									((Applink) entity).appUrl = ((Applink) applink).appUrl;
								}
							}
						}
						mAdapter.notifyDataSetChanged();
						UI.showToastNotification(getResources().getString(R.string.toast_applinks_refreshed), Toast.LENGTH_SHORT);
					}
				}
				hideBusy();
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------	

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.applink_list_edit;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	private static class ListAdapter extends EntityListAdapter {

		private TextView	message;

		public ListAdapter(Context context, List<Entity> entities, Integer itemLayoutId, TextView message) {
			super(context, entities, itemLayoutId);
			this.message = message;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final ViewHolder holder;
			final Entity entity = mListItems.get(position);

			if (view == null) {
				view = LayoutInflater.from(mContext).inflate(mItemLayoutId, null);
				holder = new ViewHolder();
				holder.photoView = (AirImageView) view.findViewById(R.id.photo);
				holder.name = (TextView) view.findViewById(R.id.name);
				holder.appId = (TextView) view.findViewById(R.id.app_id);
				holder.appUrl = (TextView) view.findViewById(R.id.app_url);
				holder.type = (TextView) view.findViewById(R.id.type);
				holder.checked = (CheckBox) view.findViewById(R.id.checked);
				if (holder.checked != null) {
					holder.checked.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View view) {
							final CheckBox checkBox = (CheckBox) view;
							final Entity entity = (Entity) checkBox.getTag();
							entity.checked = checkBox.isChecked();
						}
					});
				}
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
			}

			if (entity != null) {
				final Applink applink = (Applink) entity;

				UI.setVisibility(holder.checked, View.GONE);
				if (holder.checked != null && applink.checked != null) {
					holder.checked.setChecked(applink.checked);
					holder.checked.setTag(applink);
					UI.setVisibility(holder.checked, View.VISIBLE);
				}

				UI.setVisibility(holder.type, View.GONE);
				if (holder.type != null && applink.type != null && applink.type.length() > 0) {
					holder.type.setText(applink.type);
					UI.setVisibility(holder.type, View.VISIBLE);
				}

				UI.setVisibility(holder.name, View.GONE);
				if (holder.name != null && applink.name != null && applink.name.length() > 0) {
					holder.name.setText(applink.name);
					UI.setVisibility(holder.name, View.VISIBLE);
				}

				UI.setVisibility(holder.appId, View.GONE);
				if (holder.appId != null) {
					if (applink.appId != null && applink.appId.length() > 0) {
						holder.appId.setText(applink.appId);
						UI.setVisibility(holder.appId, View.VISIBLE);
					}
				}

				UI.setVisibility(holder.appUrl, View.GONE);
				if (holder.appUrl != null) {
					if (applink.appUrl != null && applink.appUrl.length() > 0) {
						holder.appUrl.setText(applink.appUrl);
						UI.setVisibility(holder.appUrl, View.VISIBLE);
					}
				}

				if (holder.photoView != null) {
					holder.photoView.setTag(applink);
					UI.drawPhoto(holder.photoView, applink.getPhoto());
				}
			}
			return view;
		}

		@Override
		public void notifyDataSetChanged() {
			message.setVisibility(getCount() == 0 ? View.VISIBLE : View.GONE);
			super.notifyDataSetChanged();
		}

		private static class ViewHolder {
			private AirImageView	photoView;
			private TextView		name;
			private TextView		appId;
			private TextView		appUrl;
			private TextView		type;
			private CheckBox		checked;
		}
	}
}