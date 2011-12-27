package com.proxibase.aircandi.components;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.components.ImageManager.ImageRequest;
import com.proxibase.aircandi.components.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.aircandi.widgets.TextViewEllipsizing;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.consumer.User;

public class CandiListAdapter extends ArrayAdapter<EntityProxy> {

	@Override
	public EntityProxy getItem(int position) {
		return mItems.get(position);
	}

	private List<EntityProxy>	mItems;
	private Context				mContext;
	@SuppressWarnings("unused")
	private User				mUser;
	private LayoutInflater		mInflater;

	public CandiListAdapter(Context context, User user, List<EntityProxy> items) {
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
		EntityProxy itemData = (EntityProxy) mItems.get(position);

		if (view == null) {
			view = mInflater.inflate(R.layout.temp_listitem_candi, null);
			holder = new CandiListViewHolder();
			holder.itemImage = (WebImageView) view.findViewById(R.id.item_image);
			holder.itemTitle = (TextView) view.findViewById(R.id.item_title);
			holder.itemSubtitle = (TextView) view.findViewById(R.id.item_subtitle);
			holder.itemDescription = (TextViewEllipsizing) view.findViewById(R.id.item_description);
			holder.itemAuthor = (AuthorBlock) view.findViewById(R.id.item_block_author);
			holder.itemActionButton = (View) view.findViewById(R.id.item_button_action);
			view.setTag(holder);
		}
		else {
			holder = (CandiListViewHolder) view.getTag();
		}

		if (itemData != null) {
			EntityProxy entityProxy = itemData;
			holder.data = itemData;
			if (holder.itemTitle != null) {
				if (entityProxy.title != null && entityProxy.title.length() > 0) {
					holder.itemTitle.setText(entityProxy.title);
					holder.itemTitle.setVisibility(View.VISIBLE);
				}
				else {
					holder.itemTitle.setVisibility(View.GONE);
				}
			}

			if (holder.itemSubtitle != null) {
				if (entityProxy.subtitle != null && entityProxy.subtitle.length() > 0) {
					holder.itemSubtitle.setText(entityProxy.subtitle);
					holder.itemSubtitle.setVisibility(View.VISIBLE);
				}
				else {
					holder.itemSubtitle.setVisibility(View.GONE);
				}
			}

			if (holder.itemDescription != null) {
				holder.itemDescription.setMaxLines(5);
				if (entityProxy.description != null && entityProxy.description.length() > 0) {
					holder.itemDescription.setText(entityProxy.description);
					holder.itemDescription.setVisibility(View.VISIBLE);
				}
				else {
					holder.itemDescription.setVisibility(View.GONE);
				}
			}

			if (holder.itemAuthor != null) {
				if (entityProxy.author != null) {
					holder.itemAuthor.bindToAuthor(entityProxy.author, DateUtils.wcfToDate(entityProxy.createdDate));
					holder.itemAuthor.setVisibility(View.VISIBLE);
				}
				else {
					holder.itemAuthor.setVisibility(View.GONE);
				}
			}

			if (holder.itemImage != null) {
				if (entityProxy.imageUri != null && entityProxy.imageUri.length() != 0) {
					ImageRequest imageRequest = new ImageRequest(entityProxy.imageUri, ImageShape.Square, entityProxy.imageFormat,
							entityProxy.javascriptEnabled,
							CandiConstants.IMAGE_WIDTH_SEARCH_MAX, false, true, true, 1, this, null);
					holder.itemImage.setImageRequest(imageRequest, null);
				}
			}

			/* Loop the streams */
			if (holder.itemActionButton != null) {
				holder.itemActionButton.setVisibility(View.GONE);
			}
//			if (entityProxy.commands != null && holder.itemActionButton != null) {
//				boolean activeCommand = false;
//				for (Command command : entityProxy.commands) {
//					if (command.name.toLowerCase().contains("edit")) {
//						if (entityProxy.createdById != null && entityProxy.createdById.toString().equals(mUser.id)) {
//							activeCommand = true;
//							command.entity = entityProxy;
//						}
//					}
//					else {
//						activeCommand = true;
//						command.entity = entityProxy;
//					}
//				}
//				if (!activeCommand) {
//					holder.itemActionButton.setVisibility(View.GONE);
//				}
//				else {
//					holder.itemActionButton.setVisibility(View.VISIBLE);
//					holder.itemActionButton.setTag(itemData);
//				}
//			}
//			else {
//				holder.itemActionButton.setVisibility(View.GONE);
//			}
		}
		return view;
	}

	public boolean areAllItemsEnabled() {
		return false;
	}

	public boolean isEnabled(int position) {
		return false;
	}

	public class CandiListViewHolder {

		public WebImageView			itemImage;
		public TextView				itemTitle;
		public TextView				itemSubtitle;
		public TextViewEllipsizing	itemDescription;
		public AuthorBlock			itemAuthor;
		public View					itemActionButton;
		public Object				data;
	}
}
