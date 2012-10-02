package com.aircandi.components;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.aircandi.R;
import com.aircandi.components.SearchManager.SearchItem;
import com.aircandi.components.SearchManager.SearchItemType;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.objects.Entity.ImageFormat;
import com.aircandi.widgets.WebImageView;

public class SearchAdapter extends ArrayAdapter<SearchItem> implements Filterable {

	private final Object		mLock			= new Object();
	private List<SearchItem>	mListItems;
	private LayoutInflater		mInflater;
	private Integer				mItemLayoutId	= R.layout.temp_listitem_search;
	@SuppressWarnings("unused")
	private SuggestionFilter	mSuggestionFilter;

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
		SearchItem itemData = (SearchItem) mListItems.get(position);

		if (view == null) {
			view = mInflater.inflate(mItemLayoutId, null);
			holder = new SearchListViewHolder();
			holder.itemName = (TextView) view.findViewById(R.id.item_name);
			holder.itemUri = (TextView) view.findViewById(R.id.item_uri);
			holder.itemCategoryName = (TextView) view.findViewById(R.id.item_category_name);
			holder.itemImage = (WebImageView) view.findViewById(R.id.item_image);
			view.setTag(holder);
		}
		else {
			holder = (SearchListViewHolder) view.getTag();
		}

		if (itemData != null) {
			SearchItem suggestion = itemData;
			holder.data = itemData;
			holder.position = position;

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
				if (suggestion.type == SearchItemType.Suggestions) {
					((TextView) view.findViewById(R.id.item_section_title)).setText("NEARBY");
				}
				else {
					((TextView) view.findViewById(R.id.item_section_title)).setText(suggestion.type.name().toUpperCase());
				}
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
					final ImageRequestBuilder builder = new ImageRequestBuilder(holder.itemImage)
							.setImageUri(imageUri)
							.setImageFormat(ImageFormat.Binary)
							.setSearchCache(false)
							.setUpdateCache(false)
							.setScaleToWidth(CandiConstants.IMAGE_WIDTH_ORIGINAL);

					ImageRequest imageRequest = builder.create();

					holder.itemImageUri = imageUri;
					holder.itemImage.setImageRequest(imageRequest);
				}
				else {
					holder.itemImage.getImageView().setImageResource(R.drawable.app_web_browser_sm);
				}
			}
		}
		return view;
	}

	public String getTitle(int position) {
		return mListItems.get(position).name;
	}

	public String getUrl(int position) {
		return mListItems.get(position).uri;
	}

	@Override
	public SearchItem getItem(int position) {
		return mListItems.get(position);
	}

	@Override
	public int getCount() {
		return mListItems.size();
	}

	public boolean areAllItemsEnabled() {
		return false;
	}

	public boolean isEnabled(int position) {
		return false;
	}

	public static class SearchListViewHolder {

		public int			position;
		public TextView		itemName;
		public TextView		itemUri;
		public TextView		itemCategoryName;
		public WebImageView	itemImage;
		public String		itemImageUri;
		public Object		data;
	}

	private class SuggestionFilter extends Filter {

		protected FilterResults performFiltering(CharSequence filterType) {

			/* Initiate our results object */
			FilterResults results = new FilterResults();

			/* If the adapter array is empty, check the actual items array and use it */
			if (mListItems == null) {
				synchronized (mLock) { // Notice the declaration above
					mListItems = new ArrayList<SearchItem>();
				}
			}

			/* No prefix is sent to filter by so we're going to send back the original array */
			if (filterType == null || filterType.length() == 0) {
				synchronized (mLock) {
					results.values = mListItems;
					results.count = mListItems.size();
				}
			}
			else {}
			return results;
		}

		protected void publishResults(CharSequence prefix, FilterResults results) {

			mListItems = (ArrayList<SearchItem>) results.values;
			/* Let the adapter know about the updated list */
			if (results.count > 0) {
				notifyDataSetChanged();
			}
			else {
				notifyDataSetInvalidated();
			}
		}
	}

}