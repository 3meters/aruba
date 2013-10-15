package com.aircandi.ui.helpers;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.BitmapResponse;
import com.aircandi.service.RequestListener;
import com.aircandi.service.ServiceResponse;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.ui.base.BasePicker;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.BounceListView;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;

public class ShortcutPicker extends BasePicker {

	private BounceListView			mList;
	private final List<Shortcut>	mShortcuts	= new ArrayList<Shortcut>();

	@Override
	public void unpackIntent() {
		super.unpackIntent();
		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			final List<String> jsonShortcuts = extras.getStringArrayList(Constants.EXTRA_SHORTCUTS);
			if (jsonShortcuts != null) {
				for (String jsonShortcut : jsonShortcuts) {
					Shortcut shortcut = (Shortcut) Json.jsonToObject(jsonShortcut, Json.ObjectType.SHORTCUT);
					mShortcuts.add(shortcut);
				}
			}
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mList = (BounceListView) findViewById(R.id.list);
	}

	@Override
	public void bind(BindingMode mode) {

		if (mShortcuts != null && mShortcuts.size() > 0) {
			Shortcut shortcut = mShortcuts.get(0);
			setActivityTitle(shortcut.app);

			/* Show default photo based on the type of the shortcut set */
			
			Photo photo = Entity.getDefaultPhoto(shortcut.schema, shortcut.app);
			final BitmapRequest bitmapRequest = new BitmapRequest()
					.setBitmapUri(photo.getUri())
					.setBitmapRequestor(shortcut)
					.setRequestListener(new RequestListener() {

						@Override
						public void onComplete(Object response) {

							final ServiceResponse serviceResponse = (ServiceResponse) response;
							if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										final BitmapResponse bitmapResponse = (BitmapResponse) serviceResponse.data;
										mActionBar.setIcon(new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmapResponse.bitmap));
									}
								});
							}
						}
					});
			BitmapManager.getInstance().masterFetch(bitmapRequest);
		}

		final ShortcutListAdapter adapter = new ShortcutListAdapter(this, mShortcuts, R.layout.temp_listitem_shortcut_picker);
		mList.setAdapter(adapter);
		hideBusy(); // Visible by default
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {
		View label = (View) view.findViewById(R.id.photo);
		Shortcut shortcut = (Shortcut) label.getTag();
		Routing.shortcut(this, shortcut, mEntity, null);
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.link_picker;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	static public class ShortcutListAdapter extends ArrayAdapter<Shortcut>
			implements Filterable {

		private final LayoutInflater	mInflater;
		private Integer					mItemLayoutId;
		private final List<Shortcut>	mListItems;

		public ShortcutListAdapter(Context context, List<Shortcut> shortcuts, Integer itemLayoutId) {
			super(context, 0, shortcuts);

			mListItems = shortcuts;
			mInflater = LayoutInflater.from(context);

			if (itemLayoutId != null) {
				mItemLayoutId = itemLayoutId;
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final ViewHolder holder;
			final Shortcut itemData = mListItems.get(position);

			if (view == null) {
				view = mInflater.inflate(mItemLayoutId, null);
				holder = new ViewHolder();
				holder.photoView = (AirImageView) view.findViewById(R.id.photo);
				holder.name = (TextView) view.findViewById(R.id.name);
				holder.appId = (TextView) view.findViewById(R.id.app_id);
				holder.appUrl = (TextView) view.findViewById(R.id.app_url);
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
			}

			if (itemData != null) {
				final Shortcut shortcut = itemData;

				UI.setVisibility(holder.name, View.GONE);
				if (holder.name != null && shortcut.name != null && shortcut.name.length() > 0) {
					holder.name.setText(shortcut.name);
					setVisibility(holder.name, View.VISIBLE);
				}

				UI.setVisibility(holder.appId, View.GONE);
				if (holder.appId != null) {
					if (shortcut.appId != null && shortcut.appId.length() > 0) {
						holder.appId.setText(shortcut.appId);
						UI.setVisibility(holder.appId, View.VISIBLE);
					}
				}

				UI.setVisibility(holder.appUrl, View.GONE);
				if (holder.appUrl != null) {
					if (shortcut.appUrl != null && shortcut.appUrl.length() > 0) {
						holder.appUrl.setText(shortcut.appUrl);
						UI.setVisibility(holder.appUrl, View.VISIBLE);
					}
				}

				if (holder.photoView != null) {
					holder.photoView.setTag(shortcut);
					Photo photo = shortcut.getPhoto();
					if (holder.photoView.getPhoto() == null || !photo.getUri().equals(holder.photoView.getPhoto().getUri())) {
						UI.drawPhoto(holder.photoView, photo);
					}
				}
			}
			return view;
		}

		@Override
		public Shortcut getItem(int position) {
			return mListItems.get(position);
		}

		@Override
		public int getCount() {
			return mListItems.size();
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		private static void setVisibility(View view, Integer visibility) {
			if (view != null) {
				view.setVisibility(visibility);
			}
		}

		private static class ViewHolder {
			private AirImageView	photoView;
			private TextView		name;
			private TextView		appId;
			private TextView		appUrl;
		}
	}

}