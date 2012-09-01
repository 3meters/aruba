package com.proxibase.aircandi.components;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.aircandi.service.objects.Entity;
import com.proxibase.aircandi.R;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.aircandi.widgets.TextViewEllipsizing;
import com.proxibase.aircandi.widgets.WebImageView;

public class CandiListAdapter extends ArrayAdapter<Entity> implements Filterable {

	private final Object	mLock			= new Object();
	private LayoutInflater	mInflater;
	private Integer			mItemLayoutId	= R.layout.temp_listitem_candi;
	private List<Entity>	mEntities;
	private CandiFilter		mCandiFilter;

	public CandiListAdapter(Context context, List<Entity> entities, Integer itemLayoutId) {
		super(context, 0, entities);
		mEntities = entities;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (itemLayoutId != null) {
			mItemLayoutId = itemLayoutId;
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		final CandiListViewHolder holder;
		Entity itemData = (Entity) mEntities.get(position);

		if (view == null) {
			view = mInflater.inflate(mItemLayoutId, null);
			holder = new CandiListViewHolder();
			holder.itemImage = (WebImageView) view.findViewById(R.id.item_image);
			holder.itemImageCollection = (ImageView) view.findViewById(R.id.item_image_collection);
			holder.itemTitle = (TextView) view.findViewById(R.id.item_title);
			holder.itemSubtitle = (TextView) view.findViewById(R.id.item_subtitle);
			holder.itemDescription = (TextViewEllipsizing) view.findViewById(R.id.item_description);
			holder.itemAuthor = (AuthorBlock) view.findViewById(R.id.item_block_author);
			holder.itemComments = (Button) view.findViewById(R.id.item_comments);
			view.setTag(holder);
		}
		else {
			holder = (CandiListViewHolder) view.getTag();
		}

		if (itemData != null) {
			Entity entity = itemData;
			holder.data = itemData;
			if (holder.itemImageCollection != null) {
				if (entity.type.equals(CandiConstants.TYPE_CANDI_COLLECTION)) {
					if (entity.getMasterImageUri() != null && !entity.getMasterImageUri().toLowerCase().startsWith("resource:")) {
						holder.itemImageCollection.setVisibility(View.VISIBLE);
					}
					else {
						holder.itemImageCollection.setVisibility(View.GONE);
					}
				}
				else {
					holder.itemImageCollection.setVisibility(View.GONE);
				}
			}
			if (holder.itemTitle != null) {
				if (entity.title != null && entity.title.length() > 0) {
					holder.itemTitle.setText(entity.title);
					holder.itemTitle.setVisibility(View.VISIBLE);
				}
				else {
					holder.itemTitle.setVisibility(View.GONE);
				}
			}

			if (holder.itemSubtitle != null) {
				if (entity.subtitle != null && entity.subtitle.length() > 0) {
					holder.itemSubtitle.setText(entity.subtitle);
					holder.itemSubtitle.setVisibility(View.VISIBLE);
				}
				else {
					holder.itemSubtitle.setVisibility(View.GONE);
				}
			}

			if (holder.itemDescription != null) {
				holder.itemDescription.setMaxLines(5);
				if (entity.description != null && entity.description.length() > 0) {
					holder.itemDescription.setText(entity.description);
					holder.itemDescription.setVisibility(View.VISIBLE);
				}
				else {
					holder.itemDescription.setVisibility(View.GONE);
				}
			}

			/* Comments */
			if (holder.itemComments != null) {
				if (entity.commentCount != null && entity.commentCount > 0) {
					holder.itemComments.setText(String.valueOf(entity.commentCount) + (entity.commentCount == 1 ? " Comment" : " Comments"));
					holder.itemComments.setTag(entity);
					holder.itemComments.setVisibility(View.VISIBLE);
				}
				else {
					holder.itemComments.setVisibility(View.GONE);
				}
			}

			if (holder.itemAuthor != null) {
				if (entity.creator != null) {
					holder.itemAuthor.bindToAuthor(entity.creator, entity.modifiedDate.longValue(), entity.locked);
					holder.itemAuthor.setVisibility(View.VISIBLE);
				}
				else {
					holder.itemAuthor.setVisibility(View.GONE);
				}
			}

			if (holder.itemImage != null) {
				/*
				 * The WebImageView sets the current bitmap ref being held
				 * by the internal image view to null before doing the work
				 * to satisfy the new request.
				 */
				String imageUri = entity.getMasterImageUri();
				if (holder.itemImage.getImageView().getTag() == null || !imageUri.equals((String) holder.itemImage.getImageView().getTag())) {

					BitmapDrawable bitmapDrawable = (BitmapDrawable) holder.itemImage.getImageView().getDrawable();
					if (bitmapDrawable != null && bitmapDrawable.getBitmap() != null && !bitmapDrawable.getBitmap().isRecycled()) {
						bitmapDrawable.getBitmap().recycle();
					}
					ImageRequestBuilder builder = new ImageRequestBuilder(holder.itemImage);
					builder.setImageUri(imageUri);
					builder.setImageFormat(entity.getMasterImageFormat());
					builder.setLinkZoom(entity.linkZoom);
					builder.setLinkJavascriptEnabled(entity.linkJavascriptEnabled);
					ImageRequest imageRequest = builder.create();
					holder.itemImage.setImageRequest(imageRequest);

				}
			}
		}
		return view;
	}

	@Override
	public Entity getItem(int position) {
		return mEntities.get(position);
	}

	@Override
	public int getCount() {
		return mEntities.size();
	}

	public boolean areAllItemsEnabled() {
		return false;
	}

	public boolean isEnabled(int position) {
		return false;
	}

	@Override
	public Filter getFilter() {
		if (mCandiFilter == null) {
			mCandiFilter = new CandiFilter();
		}
		return mCandiFilter;
	}

	public static class CandiListViewHolder {

		public WebImageView			itemImage;
		public ImageView			itemImageCollection;
		public TextView				itemTitle;
		public TextView				itemSubtitle;
		public TextViewEllipsizing	itemDescription;
		public AuthorBlock			itemAuthor;
		public Button				itemComments;
		public View					itemActionButton;
		public Object				data;
	}

	private class CandiFilter extends Filter {

		protected FilterResults performFiltering(CharSequence filterType) {

			/* Initiate our results object */
			FilterResults results = new FilterResults();

			/* If the adapter array is empty, check the actual items array and use it */
			if (mEntities == null) {
				synchronized (mLock) { // Notice the declaration above
					mEntities = new ArrayList<Entity>();
				}
			}

			/* No prefix is sent to filter by so we're going to send back the original array */
			if (filterType == null || filterType.length() == 0) {
				synchronized (mLock) {
					results.values = mEntities;
					results.count = mEntities.size();
				}
			}
			else {
				if (filterType.toString().toLowerCase().equals("candipatches")) {
					final ArrayList<Entity> filteredEntities = new ArrayList<Entity>(mEntities.size());
					for (int i = 0; i < mEntities.size(); i++) {
						Entity entity = mEntities.get(i);
						if (entity.type.equals(CandiConstants.TYPE_CANDI_COLLECTION) && !entity.locked) {
							filteredEntities.add(entity);
						}
					}
					results.values = filteredEntities;
					results.count = filteredEntities.size();
				}
			}
			return results;
		}

		protected void publishResults(CharSequence prefix, FilterResults results) {

			mEntities = (ArrayList<Entity>) results.values;
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
