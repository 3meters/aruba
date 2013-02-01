package com.aircandi.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.graphics.PorterDuff;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.TextViewEllipsizing;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.ImageUtils;

public class CandiListAdapter extends ArrayAdapter<Entity> implements Filterable {

	private final Object	mLock			= new Object();
	private LayoutInflater	mInflater;
	private Integer			mItemLayoutId	= R.layout.temp_listitem_candi;
	private List<Entity>	mListItems;
	private CandiFilter		mCandiFilter;
	private int				mScrollState	= CandiScrollManager.SCROLL_STATE_IDLE;

	public CandiListAdapter(Context context, List<Entity> entities, Integer itemLayoutId) {
		super(context, 0, entities);
		mListItems = entities;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		if (itemLayoutId != null) {
			mItemLayoutId = itemLayoutId;
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		final CandiListViewHolder holder;
		Entity itemData = (Entity) mListItems.get(position);

		if (view == null) {
			view = mInflater.inflate(mItemLayoutId, null);
			holder = new CandiListViewHolder();
			holder.image = (WebImageView) view.findViewById(R.id.image);
			holder.candiView = (CandiView) view.findViewById(R.id.candi_view);
			holder.title = (TextView) view.findViewById(R.id.title);

			holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
			holder.description = (TextViewEllipsizing) view.findViewById(R.id.description);
			holder.user = (UserView) view.findViewById(R.id.user);
			holder.comments = (Button) view.findViewById(R.id.button_comments_browse);
			//holder.check = (CheckBox) view.findViewById(R.id.check);
			if (holder.check != null) {
				holder.check.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View view) {
						CheckBox checkBox = (CheckBox) view;
						Entity entity = (Entity) checkBox.getTag();
						entity.checked = checkBox.isChecked();
					}
				});
			}
			view.setTag(holder);

			FontManager.getInstance().setTypefaceDefault(holder.title);
			FontManager.getInstance().setTypefaceDefault(holder.subtitle);
			FontManager.getInstance().setTypefaceDefault(holder.description);
			FontManager.getInstance().setTypefaceDefault(holder.comments);
		}
		else {
			holder = (CandiListViewHolder) view.getTag();
		}

		if (itemData != null) {
			Entity entity = itemData;
			Logger.d(this, "Adapter getView: " + itemData.name);
			holder.data = itemData;
			holder.position = position;

			setVisibility(holder.check, View.GONE);
			if (holder.check != null && entity.checked != null) {
				holder.check.setChecked(entity.checked);
				holder.check.setTag(entity);
				setVisibility(holder.check, View.VISIBLE);
			}

			setVisibility(holder.title, View.GONE);
			if (holder.title != null && entity.name != null && entity.name.length() > 0) {
				holder.title.setText(entity.name);
				setVisibility(holder.title, View.VISIBLE);
			}

			setVisibility(holder.subtitle, View.GONE);
			if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
				if (holder.subtitle != null) {
					if (entity.subtitle != null) {
						holder.subtitle.setText(entity.subtitle);
						setVisibility(holder.subtitle, View.VISIBLE);
					}
					else {
						if (entity.place.category != null) {
							holder.subtitle.setText(Html.fromHtml(entity.place.category.name));
							setVisibility(holder.subtitle, View.VISIBLE);
						}
					}
				}
			}
			else {
				if (holder.subtitle != null && entity.subtitle != null && !entity.subtitle.equals("")) {
					holder.subtitle.setText(entity.subtitle);
					setVisibility(holder.subtitle, View.VISIBLE);
				}
			}

			setVisibility(holder.description, View.GONE);
			if (holder.description != null && entity.description != null && entity.description.length() > 0) {
				holder.description.setMaxLines(5);
				holder.description.setText(entity.description);
				setVisibility(holder.description, View.VISIBLE);
			}

			/* Comments */
			setVisibility(holder.comments, View.GONE);
			if (holder.comments != null && entity.commentCount != null && entity.commentCount > 0) {
				holder.comments.setText(String.valueOf(entity.commentCount) + (entity.commentCount == 1 ? " Comment" : " Comments"));
				holder.comments.setTag(entity);
				setVisibility(holder.comments, View.VISIBLE);
			}

			setVisibility(holder.user, View.GONE);
			if (holder.user != null && entity.creator != null) {
				holder.user.bindToAuthor(entity.creator, entity.modifiedDate.longValue(), entity.locked);
				setVisibility(holder.user, View.VISIBLE);
			}

			if (holder.image != null) {
				holder.image.setTag(entity);
				/*
				 * The WebImageView sets the current bitmap ref being held
				 * by the internal image view to null before doing the work
				 * to satisfy the new request.
				 */
				if (entity.getPhoto().getBitmap() != null) {
					ImageUtils.showImageInImageView(entity.getPhoto().getBitmap(), holder.image.getImageView(), true, AnimUtils.fadeInMedium());
				}
				else {
					final String imageUri = entity.getEntityPhotoUri();

					/* Don't do anything if the image is already set to the one we want */
					if (holder.image.getImageUri() == null || !holder.image.getImageUri().equals(imageUri)) {

						BitmapRequestBuilder builder = new BitmapRequestBuilder(holder.image)
								.setImageUri(imageUri);

						final BitmapRequest imageRequest = builder.create();

						holder.imageUri = imageUri;
						if (entity.synthetic) {
							int color = Place.getCategoryColor(entity.place.category != null ? entity.place.category.name : null, true, true, false);
							holder.image.getImageView().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
						}
						else {
							holder.image.getImageView().clearColorFilter();
						}

						holder.image.setBitmapRequest(imageRequest);
					}
				}
			}
		}
		return view;
	}

	@Override
	public Entity getItem(int position) {
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

	@Override
	public Filter getFilter() {
		if (mCandiFilter == null) {
			mCandiFilter = new CandiFilter();
		}
		return mCandiFilter;
	}

	public int getScrollState() {
		return mScrollState;
	}

	public void setScrollState(int scrollState) {
		mScrollState = scrollState;
	}

	private static void setVisibility(View view, Integer visibility) {
		if (view != null) {
			view.setVisibility(visibility);
		}
	}

	private class CandiScrollManager implements AbsListView.OnScrollListener {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			mScrollState = scrollState;
			notifyDataSetChanged();
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
	}

	@SuppressWarnings("ucd")
	public static class CandiListViewHolder {

		public int					position;
		public String				imageUri;
		public WebImageView			image;
		public CandiView			candiView;
		public TextView				title;
		public TextView				subtitle;
		public TextViewEllipsizing	description;
		public UserView				user;
		public Button				comments;
		public CheckBox				check;
		public Object				data;
	}

	private class CandiFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence filterType) {

			/* Initiate our results object */
			FilterResults results = new FilterResults();

			/* If the adapter array is empty, check the actual items array and use it */
			if (mListItems == null) {
				synchronized (mLock) { // Notice the declaration above
					mListItems = new ArrayList<Entity>();
				}
			}

			/* No prefix is sent to filter by so we're going to send back the original array */
			if (filterType == null || filterType.length() == 0) {
				synchronized (mLock) {
					results.values = mListItems;
					results.count = mListItems.size();
				}
			}
			else {
				if (filterType.toString().toLowerCase(Locale.US).equals("candipatches")) {
					final ArrayList<Entity> filteredEntities = new ArrayList<Entity>(mListItems.size());
					for (int i = 0; i < mListItems.size(); i++) {
						@SuppressWarnings("unused")
						Entity entity = mListItems.get(i);
					}
					results.values = filteredEntities;
					results.count = filteredEntities.size();
				}
			}
			return results;
		}

		@Override
		protected void publishResults(CharSequence prefix, FilterResults results) {

			mListItems = (ArrayList<Entity>) results.values;
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
