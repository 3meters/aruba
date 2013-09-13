package com.aircandi.ui.edit;

import java.util.List;

import android.content.Context;
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
import com.aircandi.components.EntityManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.base.BaseEntityListEdit;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.UI;

public class ApplinkListEdit extends BaseEntityListEdit {

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mListItemResId = R.layout.temp_listitem_applink_edit;
	}

	@Override
	protected ArrayAdapter getAdapter() {
		return new ListAdapter(this, mEntities, mListItemResId);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onRefresh() {
		refreshApplinks(mEntities);
	}

	@SuppressWarnings("ucd")
	public void onSuggestLinksButtonClick(View view) {
		suggestApplinks(mEntities, true, (Place) mParent);
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

	private void suggestApplinks(final List<Entity> applinks, final Boolean autoInsert, final Place entity) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("LoadApplinkSuggestions");
				ModelResult result = EntityManager.getInstance().suggestApplinks(applinks, entity);
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
					for (Entity applink: applinks) {
						if (applink.schema == null) {
							applink.schema = Constants.SCHEMA_ENTITY_APPLINK;
						}
					}

					if (autoInsert) {
						if (applinks.size() > 0) {

							int activeCountOld = mEntities.size();
							int activeCountNew = applinks.size();
							mEntities = applinks;
							
							Integer position = 0;
							for (Entity entity : mEntities) {
								entity.checked = false;
								entity.position = position;
								position++;
							}
							
							mAdapter.clear();
							for (Entity entity: mEntities) {
								mAdapter.add(entity);
							}
							mAdapter.notifyDataSetChanged();
							if (activeCountNew == activeCountOld) {
								UI.showToastNotification(getResources().getString(R.string.toast_applinks_no_links), Toast.LENGTH_SHORT);
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
				showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("LoadSourceSuggestions");
				ModelResult result = EntityManager.getInstance().refreshApplinks(applinks);
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
								if (applink.id.equals(entity.id)) {
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
						mEntities = applinks;
						mAdapter.clear();
						for (Entity entity: mEntities) {
							mAdapter.add(entity);
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

		public ListAdapter(Context context, List<Entity> entities, Integer itemLayoutId) {
			super(context, entities, itemLayoutId);
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
					holder.photoView.getImageView().setImageDrawable(null);
					UI.drawPhoto(holder.photoView, applink.getPhoto());
				}
			}
			return view;
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