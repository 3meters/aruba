package com.proxibase.aircandi;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.EndlessAdapter;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.Comment;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.ServiceData;

public class CommentList extends CandiActivity {

	private ListView		mListView;
	private List<Comment>	mComments		= new ArrayList<Comment>();
	private Entity			mEntity;

	protected int			mLastResultCode	= Activity.RESULT_OK;
	private LayoutInflater	mInflater;
	private Boolean			mMore;
	private static long		LIST_MAX		= 300L;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			initialize();
			bind();
		}
	}

	protected void initialize() {
		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mListView = (ListView) findViewById(R.id.list_comments);
	}

	protected void bind() {

		/*
		 * Navigation setup for action bar icon and title
		 */
		if (mCommon.mCollectionId.equals(ProxiConstants.ROOT_COLLECTION_ID)) {
			if (mCommon.mCollectionType == ProxiExplorer.CollectionType.CandiByRadar) {
				mCommon.setActionBarTitleAndIcon(null, R.string.navigation_radar, true);
			}
			else if (mCommon.mCollectionType == ProxiExplorer.CollectionType.CandiByUser) {
				mCommon.mActionBar.setHomeButtonEnabled(false);
			}
		}
		else {
			Entity collectionEntity = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mCollectionId, mCommon.mCollectionType);
			mCommon.setActionBarTitleAndIcon(collectionEntity, true);
		}
		
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, getString(R.string.progress_loading));
			}

			@Override
			protected Object doInBackground(Object... params) {
				ServiceResponse serviceResponse = loadComments();
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				if (serviceResponse.responseCode == ResponseCode.Success) {

					ServiceData serviceData = (ServiceData) serviceResponse.data;
					List<Entity> entities = (List<Entity>) serviceData.data;
					if (entities != null && entities.get(0).comments != null) {
						mEntity = entities.get(0);
						mComments = mEntity.comments;
						mMore = mEntity.commentsMore;
					}

					if (mComments != null) {
						mListView.setAdapter(new EndlessCommentAdapter(mComments));
					}
				}
				mCommon.showProgressDialog(false, null);
				mCommon.stopTitlebarProgress();
			}

		}.execute();
	}

	public ServiceResponse loadComments() {

		final Bundle parameters = new Bundle();
		ArrayList<String> entityIds = new ArrayList<String>();
		entityIds.add(mCommon.mEntityId);
		parameters.putStringArrayList("entityIds", entityIds);
		parameters.putString("eagerLoad", "object:{\"children\":false,\"comments\":true}");
		parameters.putString("fields", "object:{\"entities\":{},\"comments\":{},\"children\":{}}");
		parameters.putString("options", "object:{\"limit\":"
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
				+ "}}");

		final ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntities")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.Success) {
			String jsonResponse = (String) serviceResponse.data;
			ServiceData serviceData = ProxibaseService.convertJsonToObjects(jsonResponse, Entity.class, GsonType.ProxibaseService);
			serviceResponse.data = serviceData;
		}

		return serviceResponse;
	}

	public void doRefresh() {
		/* Called from AircandiCommon */
		mCommon.startTitlebarProgress();
		mComments.clear();
		bind();
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
				ServiceResponse serviceResponse = loadComments();
				if (serviceResponse.responseCode == ResponseCode.Success) {
					ServiceData serviceData = (ServiceData) serviceResponse.data;
					List<Entity> entities = (List<Entity>) serviceData.data;

					mEntity = entities.get(0);
					moreComments = mEntity.comments;
					mMore = mEntity.commentsMore;

					if (mMore) {
						return ((getWrappedAdapter().getCount() + moreComments.size()) < LIST_MAX);
					}
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
					if (holder.itemAuthorImage.getImageView().getTag() == null || !imageUri.equals((String) holder.itemAuthorImage.getImageView().getTag())) {
						/*
						 * We are aggresive about recycling bitmaps when we can.
						 */
						BitmapDrawable bitmapDrawable = (BitmapDrawable) holder.itemAuthorImage.getImageView().getDrawable();
						if (bitmapDrawable != null && bitmapDrawable.getBitmap() != null) {
							bitmapDrawable.getBitmap().recycle();
						}
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