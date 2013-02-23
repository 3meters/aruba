package com.aircandi.components;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.TextView;

import com.aircandi.R;
import com.aircandi.components.SearchManager.SearchItem;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.ImageUtils;

public class SearchAdapter extends ArrayAdapter<SearchItem> implements Filterable {

	private final List<SearchItem>	mListItems;
	private final LayoutInflater		mInflater;
	private Integer				mItemLayoutId	= R.layout.temp_listitem_search;

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

			holder.itemName = (TextView) view.findViewById(R.id.item_name);
			holder.itemCategoryName = (TextView) view.findViewById(R.id.item_category_name);
			holder.itemUri = (TextView) view.findViewById(R.id.item_uri);
			holder.itemImage = (WebImageView) view.findViewById(R.id.item_image);

			FontManager.getInstance().setTypefaceDefault((TextView) view.findViewById(R.id.item_section_title));
			FontManager.getInstance().setTypefaceDefault((TextView) view.findViewById(R.id.item_name));
			FontManager.getInstance().setTypefaceDefault((TextView) view.findViewById(R.id.item_category_name));
			FontManager.getInstance().setTypefaceDefault((TextView) view.findViewById(R.id.item_uri));

			view.setTag(holder);
		}
		else {
			holder = (SearchListViewHolder) view.getTag();
		}

		if (itemData != null) {
			final SearchItem suggestion = itemData;
			holder.data = itemData;
			Boolean needSeparator = false;
			if (position == 0) {
				needSeparator = true;
			}
			else if (getItem(position - 1).type != suggestion.type) {
				needSeparator = true;
			}

			if (!needSeparator) {
				((TextView) view.findViewById(R.id.item_section_title)).setVisibility(View.GONE);
			}
			else {
				((TextView) view.findViewById(R.id.item_section_title)).setVisibility(View.VISIBLE);
				((TextView) view.findViewById(R.id.item_section_title)).setText(suggestion.type.name());
			}

			if (holder.itemName != null) {
				if (suggestion.name != null && suggestion.name.length() > 0) {
					holder.itemName.setText(suggestion.name);
					holder.itemName.setVisibility(View.VISIBLE);
				}
				else {
					holder.itemName.setVisibility(View.GONE);
				}
			}

			if (holder.itemCategoryName != null) {
				if (suggestion.categoryName != null && suggestion.categoryName.length() > 0) {
					holder.itemCategoryName.setText(suggestion.categoryName);
					holder.itemCategoryName.setVisibility(View.VISIBLE);
				}
				else {
					holder.itemCategoryName.setVisibility(View.GONE);
				}
			}

			if (holder.itemUri != null) {
				if (suggestion.uri != null && suggestion.uri.length() > 0) {
					holder.itemUri.setText(suggestion.uri);
					holder.itemUri.setVisibility(View.VISIBLE);
				}
				else {
					holder.itemUri.setVisibility(View.GONE);
				}
			}

			if (holder.itemImage != null) {
				/*
				 * The WebImageView sets the current bitmap ref being held
				 * by the internal image view to null before doing the work
				 * to satisfy the new request.
				 */
				if (suggestion.icon != null) {
					ImageUtils.showImageInImageView(suggestion.icon, holder.itemImage.getImageView(), true, AnimUtils.fadeInMedium());
				}
				else if (suggestion.categoryIconUri != null) {

					final String imageUri = suggestion.categoryIconUri;
					final BitmapRequestBuilder builder = new BitmapRequestBuilder(holder.itemImage).setImageUri(imageUri);
					final BitmapRequest imageRequest = builder.create();

					holder.itemImageUri = imageUri;
					holder.itemImage.setBitmapRequest(imageRequest);
				}
				else {
					holder.itemImage.getImageView().setImageResource(R.drawable.img_globe);
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

		private TextView		itemName;
		private TextView		itemUri;
		private TextView		itemCategoryName;
		private WebImageView	itemImage;
		public String			itemImageUri;
		public Object			data;
	}
}