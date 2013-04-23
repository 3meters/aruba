package com.aircandi.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.aircandi.CandiConstants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.EndlessAdapter;
import com.aircandi.components.FontManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.Comment;
import com.aircandi.service.objects.Entity;
import com.aircandi.ui.base.CandiActivity;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.DateUtils;

public class CommentList extends CandiActivity {

	private ListView		mListView;
	private List<Comment>	mComments		= new ArrayList<Comment>();
	private Button			mButtonNewComment;

	
	private LayoutInflater	mInflater;
	private Boolean			mMore			= false;
	private static final long		LIST_MAX		= 300L;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			initialize();
			configureActionBar();
			bind(true);
		}
	}

	private void initialize() {
		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mListView = (ListView) findViewById(R.id.list_comments);
		mButtonNewComment = (Button) findViewById(R.id.button_new_comment);
		
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_new_comment));
	}

	private void configureActionBar() {
		/*
		 * Navigation setup for action bar icon and title
		 */
		final Entity entity = ProxiManager.getInstance().getEntityModel().getCacheEntity(mCommon.mEntityId);
		mCommon.mActionBar.setTitle(entity.name);
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
	}

	private void bind(final Boolean refresh) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy(true);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("GetComments");
				/*
				 * Just get the comments without updating the entity in the cache
				 */
				final String jsonEagerLoad = "{\"children\":false,\"parents\":false,\"comments\":true}";
				final ModelResult result = ProxiManager.getInstance().getEntityModel().getEntity(mCommon.mEntityId, refresh, jsonEagerLoad, null);
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					if (result.data != null) {
						mCommon.mEntity = (Entity) result.data;

						if (mCommon.mEntity.comments != null && mCommon.mEntity.comments.size() > 0) {
							mButtonNewComment.setVisibility(View.GONE);
							mComments = mCommon.mEntity.comments;
							mMore = mCommon.mEntity.commentsMore;
							Collections.sort(mComments, new Comment.SortCommentsByDate());
							mListView.setAdapter(new EndlessCommentAdapter(mComments));
						}
						else {
							mButtonNewComment.setVisibility(View.VISIBLE);
						}
					}
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CommentBrowse);
				}
				mCommon.hideBusy(true);
			}

		}.execute();
	}

	private ModelResult loadComments() {

		final String jsonEagerLoad = "{\"children\":false,\"parents\":false,\"comments\":true}";
		final String jsonOptions = "{\"limit\":"
				+ String.valueOf(ProxiConstants.RADAR_ENTITY_LIMIT)
				+ ",\"skip\":0"
				+ ",\"sort\":{\"modifiedDate\":-1} "
				+ ",\"children\":{\"limit\":"
				+ String.valueOf(ProxiConstants.RADAR_CHILDENTITY_LIMIT)
				+ ",\"skip\":0"
				+ ",\"sort\":{\"modifiedDate\":-1}}"
				+ ",\"comments\":{\"limit\":"
				+ String.valueOf(ProxiConstants.RADAR_COMMENT_LIMIT)
				+ ",\"skip\":" + String.valueOf(mComments.size())
				+ "}}";

		final ModelResult result = ProxiManager.getInstance().getEntityModel().getEntity(mCommon.mEntityId, true, jsonEagerLoad, jsonOptions);
		return result;
	}

	public void doRefresh() {
		/* Called from AircandiCommon */
		mComments.clear();
		bind(true);
	}

	@SuppressWarnings("ucd")
	public void onNewCommentButtonClick(View view) {
		/*
		 * We assume the new comment button wouldn't be visible if the
		 * entity is locked.
		 */
		final IntentBuilder intentBuilder = new IntentBuilder(this, CommentForm.class);
		intentBuilder.setEntityId(null);
		intentBuilder.setParentEntityId(mCommon.mEntityId);
		final Intent intent = intentBuilder.create();
		startActivityForResult(intent, CandiConstants.ACTIVITY_COMMENT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == CandiConstants.RESULT_COMMENT_INSERTED) {
			bind(true);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes/enums
	// --------------------------------------------------------------------------------------------

	private class EndlessCommentAdapter extends EndlessAdapter {

		private List<Comment>	moreComments	= new ArrayList<Comment>();

		private EndlessCommentAdapter(List<Comment> list) {
			super(new ListAdapter(list));
		}

		@Override
		protected View getPendingView(ViewGroup parent) {
			if (mComments.size() == 0) {
				return new View(CommentList.this);

			}
			return mInflater.inflate(R.layout.temp_candi_list_item_placeholder, null);
		}

		@Override
		protected boolean cacheInBackground() {
			moreComments.clear();
			if (mMore) {
				final ModelResult result = loadComments();
				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					if (result.data != null) {
						final Entity entity = (Entity) result.data;

						if (entity.comments != null) {
							moreComments = entity.comments;
							mMore = entity.commentsMore;
						}

						if (mMore) {
							return (getWrappedAdapter().getCount() + moreComments.size()) < LIST_MAX;
						}
					}
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CommentBrowse);
				}
			}
			return false;
		}

		@Override
		protected void appendCachedData() {
			final ArrayAdapter<Comment> list = (ArrayAdapter<Comment>) getWrappedAdapter();
			for (Comment comment : moreComments) {
				list.add(comment);
			}
			list.sort(new Comment.SortCommentsByDate());			
			notifyDataSetChanged();
		}
	}

	private class ListAdapter extends ArrayAdapter<Comment> {

		private ListAdapter(List<Comment> items) {
			super(CommentList.this, 0, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final ViewHolder holder;
			final Comment itemData = mComments.get(position);

			if (view == null) {
				view = getLayoutInflater().inflate(R.layout.temp_listitem_comment, null);
				holder = new ViewHolder();
				holder.itemAuthorImage = (WebImageView) view.findViewById(R.id.item_author_image);
				holder.itemAuthorName = (TextView) view.findViewById(R.id.item_author_name);
				holder.itemAuthorLocation = (TextView) view.findViewById(R.id.item_author_location);
				holder.itemAuthorLocationSeparator = view.findViewById(R.id.item_author_location_separator);
				holder.itemComment = (TextView) view.findViewById(R.id.item_comment);
				holder.itemCreatedDate = (TextView) view.findViewById(R.id.item_created_date);

				FontManager.getInstance().setTypefaceBoldDefault(holder.itemAuthorName);
				FontManager.getInstance().setTypefaceDefault(holder.itemAuthorLocation);
				FontManager.getInstance().setTypefaceDefault(holder.itemCreatedDate);
				FontManager.getInstance().setTypefaceDefault(holder.itemComment);

				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
			}

			if (itemData != null) {
				final Comment comment = itemData;
				if (holder.itemAuthorName != null) {
					holder.itemAuthorName.setText(comment.name);
				}

				if (holder.itemAuthorLocation != null) {
					if (comment.location != null && comment.location.length() > 0) {
						holder.itemAuthorLocation.setText(comment.location);
					}
					else {
						holder.itemAuthorLocation.setVisibility(View.GONE);
						holder.itemAuthorLocationSeparator.setVisibility(View.GONE);
					}
				}

				if (holder.itemComment != null) {
					if (comment.description != null && comment.description.length() > 0) {
						holder.itemComment.setText(comment.description);
					}
					else {
						holder.itemComment.setVisibility(View.GONE);
					}
				}

				if (holder.itemCreatedDate != null) {
					if (comment.createdDate != null) {
						holder.itemCreatedDate.setText(DateUtils.timeSince(comment.createdDate.longValue(), DateUtils.nowDate().getTime()));
					}
					else {
						holder.itemCreatedDate.setVisibility(View.GONE);
					}
				}

				if (holder.itemAuthorImage != null) {

					final String imageUri = comment.imageUri;
					if (holder.itemAuthorImage.getImageUri() == null || !imageUri.equals(holder.itemAuthorImage.getImageUri())) {
						/*
						 * We are aggresive about recycling bitmaps when we can.
						 */
						if (comment.imageUri != null && comment.imageUri.length() != 0) {
							final BitmapRequestBuilder builder = new BitmapRequestBuilder(holder.itemAuthorImage).setFromUri(comment.imageUri);
							final BitmapRequest imageRequest = builder.create();
							holder.itemAuthorImage.setBitmapRequest(imageRequest);
						}
					}
				}

				if (holder.itemButtonAction != null) {
					holder.itemButtonAction.setTag(comment);
				}
			}
			return view;
		}

		@Override
		public Comment getItem(int position) {
			return mComments.get(position);
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

	}

	private static class ViewHolder {

		public WebImageView	itemAuthorImage;
		public TextView		itemAuthorName;
		public TextView		itemAuthorLocation;
		public View			itemAuthorLocationSeparator;
		public TextView		itemComment;
		public TextView		itemCreatedDate;
		public View			itemButtonAction;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.comment_list;
	}
}