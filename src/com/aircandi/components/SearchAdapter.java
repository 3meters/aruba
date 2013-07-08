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
import com.aircandi.components.SearchManager.SearchItem;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.ImageUtils;

public class SearchAdapter extends ArrayAdapter<SearchItem> implements Filterable {

	private final List<SearchItem>	mListItems;
	private final LayoutInflater	mInflater;
	private Integer					mItemLayoutId	= R.layout.temp_listitem_search;

	public SearchAdapter(Context context, List<SearchItem> searchItems, Integer itemLayoutId) {
		super(context, 0, searchItems);
		mListItems = searchItems;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (itemLayoutId != null) {
			mItemLayoutId = itemLayoutId;
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		final SearchListViewHolder holder;
		final SearchItem itemData = mListItems.get(position);

		if (view == null) {
			view = mInflater.inflate(mItemLayoutId, null);
			holder = new SearchListViewHolder();

			holder.name = (TextView) view.findViewById(R.id.name);
			holder.categoryName = (TextView) view.findViewById(R.id.category_name);
			holder.uri = (TextView) view.findViewById(R.id.uri);
			holder.photo = (WebImageView) view.findViewById(R.id.photo);

			view.setTag(holder);
		}
		else {
			holder = (SearchListViewHolder) view.getTag();
		}

		if (itemData != null) {
			final SearchItem searchItem = itemData;
			holder.data = searchItem;
			Boolean needSeparator = false;
			if (position == 0) {
				needSeparator = true;
			}
			else if (getItem(position - 1).type != searchItem.type) {
				needSeparator = true;
			}

			if (!needSeparator) {
				((TextView) view.findViewById(R.id.type)).setVisibility(View.GONE);
			}
			else {
				((TextView) view.findViewById(R.id.type)).setVisibility(View.VISIBLE);
				((TextView) view.findViewById(R.id.type)).setText(searchItem.type.name());
			}

			if (holder.name != null) {
				if (searchItem.name != null && searchItem.name.length() > 0) {
					holder.name.setText(searchItem.name);
					holder.name.setVisibility(View.VISIBLE);
				}
				else {
					holder.name.setVisibility(View.GONE);
				}
			}

			if (holder.categoryName != null) {
				if (searchItem.categoryName != null && searchItem.categoryName.length() > 0) {
					holder.categoryName.setText(searchItem.categoryName);
					holder.categoryName.setVisibility(View.VISIBLE);
				}
				else {
					holder.categoryName.setVisibility(View.GONE);
				}
			}

			if (holder.uri != null) {
				if (searchItem.uri != null && searchItem.uri.length() > 0) {
					holder.uri.setText(searchItem.uri);
					holder.uri.setVisibility(View.VISIBLE);
				}
				else {
					holder.uri.setVisibility(View.GONE);
				}
			}

			if (holder.photo != null) {
				/*
				 * The WebImageView sets the current bitmap ref being held
				 * by the internal image view to null before doing the work
				 * to satisfy the new request.
				 */
				if (searchItem.icon != null) {
					ImageUtils.showImageInImageView(searchItem.icon, holder.photo.getImageView(), true, AnimUtils.fadeInMedium());
				}
				else if (searchItem.categoryIconUri != null) {

					final String photoUri = searchItem.categoryIconUri;
					final BitmapRequestBuilder builder = new BitmapRequestBuilder(holder.photo).setImageUri(photoUri);
					final BitmapRequest imageRequest = builder.create();

					holder.categoryIconUri = photoUri;
					holder.photo.setBitmapRequest(imageRequest);
				}
				else {
					holder.photo.getImageView().setImageResource(R.drawable.img_globe);
				}
			}
		}
		return view;
	}

	@Override
	public SearchItem getItem(int position) {
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

	@SuppressWarnings("ucd")
	public static class SearchListViewHolder {

		private TextView		name;
		private TextView		uri;
		private TextView		categoryName;
		private WebImageView	photo;
		public String			categoryIconUri;
		public Object			data;
	}
}