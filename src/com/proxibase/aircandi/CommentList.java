package com.proxibase.aircandi;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.AircandiCommon.ActionButtonSet;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.Comment;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

public class CommentList extends CandiActivity {

	private ListView		mListViewComments;
	private List<Object>	mListComments;
	protected int			mLastResultCode	= Activity.RESULT_OK;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bind();
		Tracker.trackPageView("/CommentList");
	}

	protected void bind() {

		mListViewComments = (ListView) findViewById(R.id.list_comments);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, "Loading...");
			}

			@Override
			protected Object doInBackground(Object... params) {

				Bundle parameters = new Bundle();
				parameters.putInt("entityId", mCommon.mEntity.id);

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE + "GetCommentsForEntity");
				serviceRequest.setRequestType(RequestType.Method);
				serviceRequest.setParameters(parameters);
				serviceRequest.setResponseFormat(ResponseFormat.Json);

				ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				if (serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) serviceResponse.data;
					mListComments = ProxibaseService.convertJsonToObjects(jsonResponse, Comment.class, GsonType.ProxibaseService);
					Tracker.dispatch();
				}
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					mListViewComments.setAdapter(new ListAdapter(CommentList.this, 0, mListComments));
				}
				mCommon.showProgressDialog(false, null);
				mCommon.stopTitlebarProgress();
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onActionsClick(View view) {
		if (mCommon.mEntity == null || !mCommon.mEntity.locked) {
			mCommon.doActionsClick(view, mCommon.mEntity != null, ActionButtonSet.CommentList);
		}
	}

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
			mCommon.updateUserPicture();
		}
		else if (resultCode == CandiConstants.RESULT_USER_SIGNED_IN) {
			mCommon.updateUserPicture();
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

	public class ListAdapter extends ArrayAdapter<Object> {

		private List<Object>	items;

		public ListAdapter(Context context, int textViewResourceId, List<Object> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public Object getItem(int position) {
			return items.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final ViewHolder holder;
			Comment itemData = (Comment) items.get(position);

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
					holder.itemAuthorName.setText(comment.author.fullname);
				}

				if (holder.itemAuthorLocation != null) {
					if (comment.author.location != null && comment.author.location.length() > 0) {
						holder.itemAuthorLocation.setText(comment.author.location);
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
						holder.itemCreatedDate.setText(DateUtils.timeSince(comment.createdDate, (int) (DateUtils.nowDate().getTime() / 1000L)));
					}
					else {
						holder.itemCreatedDate.setVisibility(View.GONE);
					}
				}

				if (holder.itemAuthorImage != null) {
					if (comment.author.imageUri != null && comment.author.imageUri.length() != 0) {
						ImageRequestBuilder builder = new ImageRequestBuilder(holder.itemAuthorImage);
						builder.setFromUris(comment.author.imageUri, null);
						ImageRequest imageRequest = builder.create();
						holder.itemAuthorImage.setImageRequest(imageRequest, null);
					}
				}

				if (holder.itemButtonAction != null) {
					holder.itemButtonAction.setTag(comment);
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