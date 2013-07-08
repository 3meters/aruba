package com.aircandi.components;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.TextView;

import com.aircandi.beta.R;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.ui.widgets.WebImageView;

public class ShortcutListAdapter extends ArrayAdapter<Shortcut> implements Filterable {

	private final LayoutInflater	mInflater;
	private Integer					mItemLayoutId	= R.layout.temp_listitem_applink_list_edit;
	private final List<Shortcut>	mListItems;

	public ShortcutListAdapter(Context context, List<Shortcut> shortcuts, Integer itemLayoutId) {
		super(context, 0, shortcuts);

		mListItems = shortcuts;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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
			holder.photo = (WebImageView) view.findViewById(R.id.photo);
			holder.name = (TextView) view.findViewById(R.id.name);
			view.setTag(holder);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}

		if (itemData != null) {
			final Shortcut shortcut = itemData;

			setVisibility(holder.name, View.GONE);
			if (holder.name != null && shortcut.name != null && shortcut.name.length() > 0) {
				holder.name.setText(shortcut.name);
				setVisibility(holder.name, View.VISIBLE);
			}

			if (holder.photo != null) {
				holder.photo.setTag(shortcut);
				/*
				 * The WebImageView sets the current bitmap ref being held
				 * by the internal image view to null before doing the work
				 * to satisfy the new request.
				 */
				final String photoUri = shortcut.photo.getUri();

				/* Don't do anything if the image is already set to the one we want */
				if (holder.photo.getImageUri() == null || !holder.photo.getImageUri().equals(photoUri)) {

					final BitmapRequestBuilder builder = new BitmapRequestBuilder(holder.photo).setImageUri(photoUri);
					final BitmapRequest imageRequest = builder.create();
					holder.photo.setBitmapRequest(imageRequest);
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
		private WebImageView	photo;
		private TextView		name;
	}
}
