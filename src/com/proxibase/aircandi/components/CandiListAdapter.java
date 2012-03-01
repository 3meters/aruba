package com.proxibase.aircandi.components;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.aircandi.widgets.TextViewEllipsizing;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.User;

public class CandiListAdapter extends ArrayAdapter<Entity> {

	@Override
	public Entity getItem(int position) {
		return mItems.get(position);
	}

	private List<Entity>	mItems;
	private Context			mContext;
	@SuppressWarnings("unused")
	private User			mUser;
	private LayoutInflater	mInflater;

	public CandiListAdapter(Context context, User user, List<Entity> items) {
		super(context, 0, items);
		this.mItems = items;
		this.mContext = context;
		this.mUser = user;
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		final CandiListViewHolder holder;
		Entity itemData = (Entity) mItems.get(position);

		if (view == null) {
			view = mInflater.inflate(R.layout.temp_listitem_candi, null);
			holder = new CandiListViewHolder();
			holder.itemImage = (WebImageView) view.findViewById(R.id.item_image);
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
				if (entity.commentsCount > 0) {
					holder.itemComments.setText(String.valueOf(entity.commentsCount) + (entity.commentsCount == 1 ? " Comment" : " Comments"));
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
				BitmapDrawable bitmapDrawable = (BitmapDrawable) holder.itemImage.getImageView().getDrawable();
				if (bitmapDrawable != null && bitmapDrawable.getBitmap() != null) {
					bitmapDrawable.getBitmap().recycle();
				}
				ImageRequestBuilder builder = new ImageRequestBuilder(holder.itemImage);
				builder.setImageUri(entity.getMasterImageUri());
				builder.setImageFormat(entity.getMasterImageFormat());
				builder.setLinkZoom(entity.linkZoom);
				builder.setLinkJavascriptEnabled(entity.linkJavascriptEnabled);
				ImageRequest imageRequest = builder.create();
				holder.itemImage.setImageRequest(imageRequest);
			}
		}
		return view;
	}

	public boolean areAllItemsEnabled() {
		return false;
	}

	public boolean isEnabled(int position) {
		return false;
	}

	public static class CandiListViewHolder {

		public WebImageView			itemImage;
		public TextView				itemTitle;
		public TextView				itemSubtitle;
		public TextViewEllipsizing	itemDescription;
		public AuthorBlock			itemAuthor;
		public Button				itemComments;
		public View					itemActionButton;
		public Object				data;
	}
}
