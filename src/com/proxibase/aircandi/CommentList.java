package com.proxibase.aircandi;

import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.utils.DateUtils;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.aircandi.utils.NetworkManager;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.utils.NetworkManager.ResponseCode;
import com.proxibase.aircandi.utils.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.widgets.ActionsWindow;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.Comment;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

@SuppressWarnings("unused")
public class CommentList extends AircandiActivity {

	private ListView		mListViewComments;
	private List<Object>	mListComments;
	private ActionsWindow	mActionsWindow;
	private Runnable		mUserSignedInRunnable;
	private Handler			mHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		configure();
		bindEntity();
		drawEntity();
		GoogleAnalyticsTracker.getInstance().trackPageView("/CommentList");
	}

	protected void bindEntity() {

		mListViewComments = (ListView) findViewById(R.id.list_comments);

		Bundle parameters = new Bundle();
		parameters.putInt("entityId", mEntityProxy.id);

		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE + "GetCommentsForEntity");
		serviceRequest.setRequestType(RequestType.Method);
		serviceRequest.setParameters(parameters);
		serviceRequest.setResponseFormat(ResponseFormat.Json);

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (serviceResponse.responseCode != ResponseCode.Success) {
			setResult(Activity.RESULT_CANCELED);
			finish();
			overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
		}
		else {
			String jsonResponse = (String) serviceResponse.data;
			mListComments = ProxibaseService.convertJsonToObjects(jsonResponse, Comment.class, GsonType.ProxibaseService);
			GoogleAnalyticsTracker.getInstance().dispatch();
		}
		mListViewComments.setAdapter(new ListAdapter(CommentList.this, 0, mListComments));
	}

	protected void drawEntity() {}

	private void configure() {
		mHandler = new Handler();
		mContextButton = (Button) findViewById(R.id.btn_context);
		if (mContextButton != null) {
			mContextButton.setVisibility(View.INVISIBLE);
			showBackButton(true, getString(R.string.form_button_back));
		}
	}

	public void onCommentMoreButtonClick(View view) {

		Comment comment = (Comment) view.getTag();
		if (mActionsWindow == null) {
			mActionsWindow = new ActionsWindow(this);
		}
		else {
			long dismissInterval = System.currentTimeMillis() - mActionsWindow.getActionStripToggleTime();
			if (dismissInterval <= 200) {
				return;
			}
		}

		int[] coordinates = { 0, 0 };

		view.getLocationInWindow(coordinates);
		final Rect rect = new Rect(coordinates[0], coordinates[1], coordinates[0] + view.getWidth(), coordinates[1] + view.getHeight());
		View content = configureActionButtons(comment);

		mActionsWindow.show(rect, content, view, 0, -13, -5);
	}

	private View configureActionButtons(Comment comment) {

		ViewGroup viewGroup = null;

		viewGroup = new LinearLayout(this);

		/* Like */

		Button commandButton = (Button) getLayoutInflater().inflate(R.layout.temp_actionstrip_button, null);
		commandButton.setText("Like");
		Drawable icon = getResources().getDrawable(R.drawable.icon_new2_dark);
		icon.setBounds(0, 0, 30, 30);
		commandButton.setCompoundDrawables(null, icon, null, null);

		Command command = new Command();
		command.verb = "like";
		command.name = "aircandi.like";
		command.type = "action";
		command.handler = "dialog.new";
		commandButton.setTag(command);
		viewGroup.addView(commandButton);

		/* Reply */

		Button commandButtonCandi = (Button) getLayoutInflater().inflate(R.layout.temp_actionstrip_button, null);
		commandButtonCandi.setText("Reply");
		icon = getResources().getDrawable(R.drawable.logo5_dark);
		icon.setBounds(0, 0, 30, 30);
		commandButtonCandi.setCompoundDrawables(null, icon, null, null);

		Command commandCandi = new Command();
		commandCandi.verb = "new";
		commandCandi.name = "aircandi.candi.new";
		commandCandi.type = "action";
		commandCandi.handler = "CommentForm";
		commandButtonCandi.setTag(commandCandi);

		viewGroup.addView(commandButtonCandi);

		return viewGroup;
	}

	public void onCommandButtonClick(View view) {

		if (mActionsWindow != null) {
			mActionsWindow.dismiss();
		}

//		final Command command = (Command) view.getTag();
//		final String commandHandler = "com.proxibase.aircandi." + command.handler;
//		String commandName = command.name.toLowerCase();

		ImageUtils.showToastNotification("Unimplemented...", Toast.LENGTH_SHORT);

//		try {
//
//			String message = getString(R.string.signin_message_new_candi) + " " + command.label;
//			mUserSignedInRunnable = new Runnable() {
//
//				@Override
//				public void run() {
//					try {
//						Class clazz = Class.forName(commandHandler, false, this.getClass().getClassLoader());
//
//						if (command.verb.equals("new")) {
//							Intent intent = buildIntent(null, command.entity.id, command, command.entity.beacon, mUser, clazz);
//							startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
//						}
//						else {
//							Intent intent = buildIntent(null, 0, command, null, mUser, clazz);
//							startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
//						}
//
//					}
//					catch (ClassNotFoundException exception) {
//						exception.printStackTrace();
//					}
//					finally {
//						mUserSignedInRunnable = null;
//					}
//				}
//			};
//
//			if (!command.verb.equals("view")) {
//				if (mUser != null && mUser.anonymous) {
//					Intent intent = buildIntent(null, 0, new Command("edit"), null, null, SignInForm.class);
//					intent.putExtra(getString(R.string.EXTRA_MESSAGE), message);
//					startActivityForResult(intent, CandiConstants.ACTIVITY_SIGNIN);
//				}
//				else {
//					mHandler.post(mUserSignedInRunnable);
//				}
//			}
//			else {
//				mHandler.post(mUserSignedInRunnable);
//			}
//		}
//		catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		}
//		catch (IllegalStateException e) {
//			e.printStackTrace();
//		}
//		catch (SecurityException exception) {
//			exception.printStackTrace();
//		}
	}

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
				holder.itemButtonAction = (View) view.findViewById(R.id.item_button_action);
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
						Date createdDate = DateUtils.wcfToDate(comment.createdDate);
						holder.itemCreatedDate.setText(DateUtils.timeSince(createdDate, DateUtils.nowDate()));
					}
					else {
						holder.itemCreatedDate.setVisibility(View.GONE);
					}
				}

				if (holder.itemAuthorImage != null) {
					if (comment.author.imageUri != null && comment.author.imageUri.length() != 0) {
						ImageRequest imageRequest = new ImageRequest(comment.author.imageUri, ImageShape.Square, "binary", false,
								CandiConstants.IMAGE_WIDTH_SEARCH_MAX, false, true, true, 1, this, null);
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
	protected int getLayoutID() {
		return R.layout.comment_list;
	}
}