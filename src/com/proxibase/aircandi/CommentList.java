package com.proxibase.aircandi;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.EndlessAdapter;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
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
	private Entity			mParentEntity;

	protected int			mLastResultCode	= Activity.RESULT_OK;
	private LayoutInflater	mInflater;
	private Boolean			mMore;
	private static long		LIST_MAX		= 300L;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!Aircandi.getInstance().getLaunchedFromRadar()) {
			/*
			 * Try to detect case where this is being created after
			 * a crash and bail out.
			 */
			setResult(Activity.RESULT_CANCELED);
			finish();
		}
		else {
			initialize();
			bind();
		}
	}

	protected void initialize() {
		mCommon.track();
		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	protected void bind() {

		mListView = (ListView) findViewById(R.id.list_comments);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, "Loading...");
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
						mParentEntity = entities.get(0);
						mComments = mParentEntity.comments;
						mMore = mParentEntity.commentsMore;
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
		entityIds.add(mCommon.mEntity.id);
		parameters.putStringArrayList("entityIds", entityIds);
		parameters.putString("eagerLoad", "object:{\"children\":false,\"comments\":true}");
		parameters.putString("fields", "object:{\"entities\":{},\"comments\":{},\"children\":{}}");
		parameters.putString("options", "object:{\"limit\":"
				+ String.valueOf(CandiConstants.RADAR_ENTITY_LIMIT)
				+ ",\"skip\":0"
				+ ",\"sort\":{\"modifiedDate\":-1} "
				+ ",\"children\":{\"limit\":"
				+ String.valueOf(CandiConstants.RADAR_CHILDENTITY_LIMIT)
				+ ",\"skip\":0"
				+ ",\"sort\":{\"modifiedDate\":-1}}"
				+ ",\"comments\":{\"limit\":"
				+ String.valueOf(CandiConstants.RADAR_COMMENT_LIMIT)
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

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onCommandButtonClick(View view) {
		if (mCommon.mActionsWindow != null) {
			mCommon.mActionsWindow.dismiss();
		}
		Command command = (Command) view.getTag();
		mCommon.doCommand(command);
	}

	public void onRefreshClick(View view) {
		mCommon.startTitlebarProgress();
		bind();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		mLastResultCode = resultCode;
		if (resultCode == CandiConstants.RESULT_COMMENT_INSERTED) {
			bind();
		}
		else if (resultCode == CandiConstants.RESULT_PROFILE_UPDATED) {
			invalidateOptionsMenu();
		}
		else if (resultCode == CandiConstants.RESULT_USER_SIGNED_IN) {
			invalidateOptionsMenu();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	public boolean onCreateOptionsMenu(Menu menu) {
		mCommon.doCreateOptionsMenu(menu);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		mCommon.doPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mCommon.doOptionsItemSelected(item);
		return true;
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

					mParentEntity = entities.get(0);
					moreComments = mParentEntity.comments;
					mMore = mParentEntity.commentsMore;

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