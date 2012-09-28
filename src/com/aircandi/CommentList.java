package com.aircandi;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.DateUtils;
import com.aircandi.components.EndlessAdapter;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.objects.Comment;
import com.aircandi.service.objects.Entity;
import com.aircandi.widgets.WebImageView;

public class CommentList extends CandiActivity {

	private ListView		mListView;
	private List<Comment>	mComments		= new ArrayList<Comment>();

	protected int			mLastResultCode	= Activity.RESULT_OK;
	private LayoutInflater	mInflater;
	private Boolean			mMore = false;
	private static long		LIST_MAX		= 300L;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			initialize();
			configureActionBar();
			bind(true);
		}
	}

	protected void initialize() {
		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mListView = (ListView) findViewById(R.id.list_comments);
	}

	private void configureActionBar() {
		/*
		 * Navigation setup for action bar icon and title
		 */

		if (mCommon.mCollectionId.equals(ProxiConstants.ROOT_COLLECTION_ID)) {
			if (mCommon.mEntityTree == ProxiExplorer.EntityTree.Radar) {
				mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
			}
			else if (mCommon.mEntityTree == ProxiExplorer.EntityTree.User) {
				mCommon.mActionBar.setDisplayHomeAsUpEnabled(false);
				mCommon.mActionBar.setHomeButtonEnabled(false);
			}
		}
		else {
			Entity collection = ProxiExplorer.getInstance().getEntityModel().getEntity(mCommon.mCollectionId);
			mCommon.mActionBar.setTitle(collection.title);
			mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
		}

	}

	protected void bind(final Boolean refresh) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, getString(R.string.progress_loading));
			}

			@Override
			protected Object doInBackground(Object... params) {
				/*
				 * Just get the comments without updating the entity in the cache
				 */
				String jsonEagerLoad = "{\"children\":false,\"parents\":false,\"comments\":true}";
				ModelResult result = ProxiExplorer.getInstance().getEntityModel().getEntity(mCommon.mEntityId, refresh, false, jsonEagerLoad, null);
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				ModelResult result = (ModelResult) modelResult;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					if (result.data != null) {
						Entity entity = (Entity) result.data;

						if (entity.comments != null) {
							mComments = entity.comments;
							mMore = entity.commentsMore;
						}

						if (mComments != null) {
							mListView.setAdapter(new EndlessCommentAdapter(mComments));
						}
					}
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CommentBrowse);
				}
				mCommon.showProgressDialog(false, null);
			}

		}.execute();
	}

	public ModelResult loadComments() {
		
		String jsonEagerLoad = "{\"children\":false,\"parents\":false,\"comments\":true}";
		String jsonOptions = "{\"limit\":"
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

		ModelResult result = ProxiExplorer.getInstance().getEntityModel().getEntity(mCommon.mEntityId, true, false, jsonEagerLoad, jsonOptions);
		return result;
	}

	public void doRefresh() {
		/* Called from AircandiCommon */
		mComments.clear();
		bind(true);
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes/enums
	// --------------------------------------------------------------------------------------------

	class EndlessCommentAdapter extends EndlessAdapter {

		List<Comment>	moreComments	= new ArrayList<Comment>();

		EndlessCommentAdapter(List<Comment> list) {
			super(new ListAdapter(list));
		}

		@Override
		protected View getPendingView(ViewGroup parent) {
			View view = mInflater.inflate(R.layout.temp_candi_list_item_placeholder, null);
			return (view);
		}

		@Override
		protected boolean cacheInBackground() {
			moreComments.clear();
			if (mMore) {
				ModelResult result = loadComments();
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					
					if (result.data != null) {
						Entity entity = (Entity) result.data;

						if (entity.comments != null) {
							moreComments = entity.comments;
							mMore = entity.commentsMore;
						}

						if (mMore) {
							return ((getWrappedAdapter().getCount() + moreComments.size()) < LIST_MAX);
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
			ArrayAdapter<Comment> list = (ArrayAdapter<Comment>) getWrappedAdapter();
			for (Comment comment : moreComments) {
				list.add(comment);
			}
		}
	}

	public class ListAdapter extends ArrayAdapter<Comment> {

		public ListAdapter(List<Comment> items) {
			super(CommentList.this, 0, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final ViewHolder holder;
			Comment itemData = (Comment) mComments.get(position);

			if (view == null) {
				view = getLayoutInflater().inflate(R.layout.temp_listitem_comment, null);
				holder = new ViewHolder();
				holder.itemAuthorImage = (WebImageView) view.findViewById(R.id.item_author_image);
				holder.itemAuthorName = (TextView) view.findViewById(R.id.item_author_name);
				holder.itemAuthorLocation = (TextView) view.findViewById(R.id.item_author_location);
				holder.itemAuthorLocationSeparator = (View) view.findViewById(R.id.item_author_location_separator);
				holder.itemComment = (TextView) view.findViewById(R.id.item_comment);
				holder.itemCreatedDate = (TextView) view.findViewById(R.id.item_created_date);
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
			}

			if (itemData != null) {
				Comment comment = itemData;
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

					String imageUri = comment.imageUri;
					if (holder.itemAuthorImage.getImageUri() == null || !imageUri.equals((String) holder.itemAuthorImage.getImageUri())) {
						/*
						 * We are aggresive about recycling bitmaps when we can.
						 */
						holder.itemAuthorImage.recycleBitmap();
						if (comment.imageUri != null && comment.imageUri.length() != 0) {
							ImageRequestBuilder builder = new ImageRequestBuilder(holder.itemAuthorImage);
							builder.setFromUris(comment.imageUri, null);
							ImageRequest imageRequest = builder.create();
							holder.itemAuthorImage.setImageRequest(imageRequest);
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

		public boolean areAllItemsEnabled() {
			return false;
		}

		public boolean isEnabled(int position) {
			return false;
		}

	}

	private class ViewHolder {

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