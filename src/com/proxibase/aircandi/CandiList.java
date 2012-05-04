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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.proxibase.aircandi.Aircandi.CandiTask;
import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.Command.CommandType;
import com.proxibase.aircandi.components.EndlessAdapter;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.aircandi.widgets.TextViewEllipsizing;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.ServiceData;
import com.proxibase.service.objects.User;

public class CandiList extends CandiActivity {

	public static enum MethodType {
		CandiByUser, CandiForParent
	}

	private ListView		mListView;
	protected List<Entity>	mEntities	= new ArrayList<Entity>();
	private Entity			mParentEntity;
	private MethodType		mMethodType;
	private LayoutInflater	mInflater;
	private Boolean			mMore;
	private static long		LIST_MAX	= 300L;

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

	private void initialize() {
		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public void bind() {
		super.bind();

		mListView = (ListView) findViewById(R.id.list_candi);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, "Loading...");
			}

			@Override
			protected Object doInBackground(Object... params) {
				ServiceResponse serviceResponse = loadEntities();
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				if (serviceResponse.responseCode == ResponseCode.Success) {

					ServiceData serviceData = (ServiceData) serviceResponse.data;
					mEntities = (List<Entity>) serviceData.data;
					mMore = serviceData.more;

					if (mMethodType == MethodType.CandiForParent) {
						mParentEntity = mEntities.get(0);
						mEntities = mParentEntity.children;
						mMore = mParentEntity.childrenMore;
					}

					if (mEntities != null) {
						mListView.setAdapter(new EndlessEntityAdapter(Aircandi.getInstance().getUser(), mEntities));
					}
				}
				mCommon.showProgressDialog(false, null);
				mCommon.stopTitlebarProgress();
			}
		}.execute();
	}

	public ServiceResponse loadEntities() {

		Bundle parameters = new Bundle();
		ServiceRequest serviceRequest = new ServiceRequest();
		if (mCommon.mEntity != null) {

			mMethodType = MethodType.CandiForParent;
			ArrayList<String> entityIds = new ArrayList<String>();
			entityIds.add(mCommon.mEntity.id);
			parameters.putStringArrayList("entityIds", entityIds);
			parameters.putString("eagerLoad", "object:{\"children\":true,\"comments\":false}");
			parameters.putString("options", "object:{\"limit\":"
					+ String.valueOf(CandiConstants.RADAR_ENTITY_LIMIT)
					+ ",\"skip\":0"
					+ ",\"sort\":{\"modifiedDate\":-1} "
					+ ",\"children\":{\"limit\":"
					+ String.valueOf(CandiConstants.RADAR_CHILDENTITY_LIMIT)
					+ ",\"skip\":" + String.valueOf(mEntities.size())
					+ ",\"sort\":{\"modifiedDate\":-1}}"
					+ "}");

			serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntities").setRequestType(RequestType.Method)
					.setParameters(parameters).setResponseFormat(ResponseFormat.Json);
			mCommon.track("/CandiList");
		}
		else if (mCommon.mEntity == null) {
			mMethodType = MethodType.CandiByUser;
			parameters.putString("userId", Aircandi.getInstance().getUser().id);
			parameters.putString("eagerLoad", "object:{\"children\":false,\"comments\":false}");
			parameters.putString("options", "object:{\"limit\":"
					+ String.valueOf(CandiConstants.RADAR_ENTITY_LIMIT)
					+ ",\"skip\":" + String.valueOf(mEntities.size())
					+ ",\"sort\":{\"modifiedDate\":-1} "
					+ ",\"children\":{\"limit\":"
					+ String.valueOf(CandiConstants.RADAR_CHILDENTITY_LIMIT)
					+ ",\"skip\":0"
					+ ",\"sort\":{\"modifiedDate\":-1}}"
					+ "}");

			serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForUser").setRequestType(RequestType.Method)
					.setParameters(parameters).setResponseFormat(ResponseFormat.Json);
			mCommon.track("/MyCandiList");
		}

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

	public void onListItemClick(View view) {
		Entity entity = (Entity) ((CandiListViewHolder) view.getTag()).data;
		if (entity.type == CandiConstants.TYPE_CANDI_COMMAND) {

		}
		else {

			IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class);
			intentBuilder.setCommand(new Command(CommandType.View));
			intentBuilder.setEntity(entity);
			intentBuilder.setEntityType(entity.type);
			if (!entity.root) {
				intentBuilder.setEntityLocation(mParentEntity.location);
			}
			else {
				intentBuilder.setBeaconId(entity.beaconId);
			}
			Intent intent = intentBuilder.create();

			startActivityForResult(intent, CandiConstants.ACTIVITY_CANDI_INFO);
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		}

	}

	public void onCommentsClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity.commentsCount > 0) {

			IntentBuilder intentBuilder = new IntentBuilder(this, CommentList.class);
			intentBuilder.setCommand(new Command(CommandType.View));
			intentBuilder.setEntity(entity);
			Intent intent = intentBuilder.create();
			startActivityForResult(intent, 0);
		}
	}

	public void onBackPressed() {
		if (mMethodType == MethodType.CandiByUser) {
			Aircandi.getInstance().setCandiTask(CandiTask.RadarCandi);
			Intent intent = new Intent(this, CandiRadar.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			overridePendingTransition(R.anim.fade_in_short, R.anim.fade_out_short);
		}
		else {
			setResult(mLastResultCode);
			super.onBackPressed();
		}
	}

	public void onRefreshClick(View view) {
		mCommon.startTitlebarProgress();
		mMore = false;
		mEntities.clear();
		bind();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		mLastResultCode = resultCode;
		if (resultCode == CandiConstants.RESULT_ENTITY_UPDATED || resultCode == CandiConstants.RESULT_ENTITY_DELETED
				|| resultCode == CandiConstants.RESULT_ENTITY_INSERTED || resultCode == CandiConstants.RESULT_COMMENT_INSERTED) {
			mMore = false;
			mEntities.clear();
			bind();
			if (resultCode == CandiConstants.RESULT_ENTITY_DELETED) {
				mLastResultCode = CandiConstants.RESULT_ENTITY_CHILD_DELETED;
			}
		}
		else if (resultCode == CandiConstants.RESULT_PROFILE_UPDATED) {
			mCommon.updateUserPicture();
			mMore = false;
			mEntities.clear();
			bind();
		}
		else if (resultCode == CandiConstants.RESULT_USER_SIGNED_IN) {
			mCommon.updateUserPicture();

			/* Need to rebind if showing my candi */
			if (mMethodType == MethodType.CandiByUser) {
				mCommon.startTitlebarProgress();
				mMore = false;
				mEntities.clear();
				bind();
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean rebind = mCommon.doOptionsItemSelected(item);
		if (rebind) {
			mMore = false;
			mEntities.clear();
			bind();
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	class EndlessEntityAdapter extends EndlessAdapter {

		List<Entity>	moreEntities	= new ArrayList<Entity>();

		EndlessEntityAdapter(User user, List<Entity> list) {
			super(new ListAdapter(user, list));
		}

		@Override
		protected View getPendingView(ViewGroup parent) {
			View view = mInflater.inflate(R.layout.temp_candi_list_item_placeholder, null);
			return (view);
		}

		@Override
		protected boolean cacheInBackground() {
			moreEntities.clear();
			if (mMore) {
				ServiceResponse serviceResponse = loadEntities();
				if (serviceResponse.responseCode == ResponseCode.Success) {
					ServiceData serviceData = (ServiceData) serviceResponse.data;
					List<Entity> entities = (List<Entity>) serviceData.data;

					if (mMethodType == MethodType.CandiForParent) {
						mParentEntity = entities.get(0);
						moreEntities = mParentEntity.children;
						mMore = mParentEntity.childrenMore;
					}
					else {
						mMore = serviceData.more;
						moreEntities = entities;
					}
					if (mMore) {
						return ((getWrappedAdapter().getCount() + moreEntities.size()) < LIST_MAX);
					}
				}
			}
			return false;
		}

		@Override
		protected void appendCachedData() {
			ArrayAdapter<Entity> list = (ArrayAdapter<Entity>) getWrappedAdapter();
			for (Entity entity : moreEntities) {
				list.add(entity);
			}
		}
	}

	private class ListAdapter extends ArrayAdapter<Entity> {

		public ListAdapter(User user, List<Entity> items) {
			super(CandiList.this, 0, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			Logger.v(this, "getView: position: " + String.valueOf(position));
			final CandiListViewHolder holder;
			Entity itemData = (Entity) mEntities.get(position);

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
					if (entity.commentsCount != null && entity.commentsCount > 0) {
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

		public boolean areAllItemsEnabled() {
			return false;
		}

		public boolean isEnabled(int position) {
			return false;
		}

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

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.candi_list;
	}

}