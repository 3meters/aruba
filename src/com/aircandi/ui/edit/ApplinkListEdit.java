package com.aircandi.ui.edit;

import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.beta.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.base.BaseEntityListEdit;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.UI;

public class ApplinkListEdit extends BaseEntityListEdit {

	@Override
	protected void bind() {
		super.bind();
		final ListAdapter adapter = new ListAdapter(this, mEntities, R.layout.temp_listitem_applink_list_edit);
		mList.setAdapter(adapter);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onSuggestLinksButtonClick(View view) {
		/* Go get applink suggestions again */
		loadApplinkSuggestions(mEntities, true, (Place) mParent);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	private void loadApplinkSuggestions(final List<Entity> applinks, final Boolean autoInsert, final Place entity) {
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy();
				mBusyManager.startBodyBusyIndicator();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("LoadSourceSuggestions");
				ModelResult result = EntityManager.getInstance().getApplinkSuggestions(applinks, entity);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					final List<Entity> applinksProcessed = (List<Entity>) result.data;
					if (autoInsert) {
						if (applinksProcessed.size() > 0) {

							/* First make sure they have default captions */
							for (Entity applink : applinksProcessed) {
								if (applink.name == null) {
									applink.name = applink.type;
								}
							}
							int activeCountOld = mEntities.size();
							int activeCountNew = applinksProcessed.size();
							mEntities = applinksProcessed;
							if (activeCountNew == activeCountOld) {
								UI.showToastNotification(getResources().getString(R.string.toast_source_no_links), Toast.LENGTH_SHORT);
							}
							else {
								UI.showToastNotification(getResources().getString((applinksProcessed.size() == 1)
										? R.string.toast_source_linked
										: R.string.toast_sources_linked), Toast.LENGTH_SHORT);
							}
						}

					}
				}
				bind();
				mBusyManager.hideBusy();
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.applink_list_edit;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
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
				view = mInflater.inflate(mItemLayoutId, null);
				holder = new ViewHolder();
				holder.photo = (WebImageView) view.findViewById(R.id.photo);
				holder.name = (TextView) view.findViewById(R.id.name);
				holder.appId = (TextView) view.findViewById(R.id.app_id);
				holder.appUrl = (TextView) view.findViewById(R.id.app_url);
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

				if (holder.photo != null) {
					holder.photo.setTag(applink);
					/*
					 * The WebImageView sets the current bitmap ref being held
					 * by the internal image view to null before doing the work
					 * to satisfy the new request.
					 */
					if (applink.photo != null && applink.photo.hasBitmap()) {
						UI.showImageInImageView(applink.photo.getBitmap(), holder.photo.getImageView(), true, Animate.fadeInMedium());
					}
					else {
						final String photoUri = applink.getPhotoUri();

						/* Don't do anything if the image is already set to the one we want */
						if (holder.photo.getImageUri() == null || !holder.photo.getImageUri().equals(photoUri)) {

							final BitmapRequestBuilder builder = new BitmapRequestBuilder(holder.photo).setImageUri(photoUri);
							final BitmapRequest imageRequest = builder.create();
							holder.photo.setBitmapRequest(imageRequest);
						}
					}

				}
			}
			return view;
		}

		private static class ViewHolder {
			private WebImageView	photo;
			private TextView		name;
			private TextView		appId;
			private TextView		appUrl;
			private CheckBox		checked;
		}
	}
}