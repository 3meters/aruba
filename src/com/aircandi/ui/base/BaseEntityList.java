package com.aircandi.ui.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.BusyManager;
import com.aircandi.components.EndlessAdapter;
import com.aircandi.components.EntityManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.Maps;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Cursor;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.DefaultType;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.ServiceEntry;
import com.aircandi.service.objects.User;
import com.aircandi.ui.edit.CommentEdit;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;

public abstract class BaseEntityList extends BaseBrowse {

	private ListView				mListView;

	private List<Entity>			mEntities	= new ArrayList<Entity>();
	private Cursor					mCursorSettings;
	private Button					mButtonNewEntity;

	private Boolean					mMore		= false;
	private static final long		LIST_MAX	= 300L;

	private Number					mEntityModelRefreshDate;
	private Number					mEntityModelActivityDate;
	private User					mEntityModelUser;

	/* Inputs */
	public String					mEntityId;
	private ListMode				mListMode;
	private String					mListSchema;
	private Integer					mListItemResId;
	private Boolean					mListNewEnabled;

	protected static LayoutInflater	mInflater;

	@Override
	protected void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mListMode = BaseEntityList.ListMode.valueOf(extras.getString(Constants.EXTRA_LIST_MODE));
			mListSchema = extras.getString(Constants.EXTRA_LIST_SCHEMA);
			mListNewEnabled = extras.getBoolean(Constants.EXTRA_LIST_NEW_ENABLED, false);
			mListItemResId = extras.getInt(Constants.EXTRA_LIST_ITEM_RESID, R.layout.temp_listitem_candi);
		}
	}

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mListView = (ListView) findViewById(R.id.list_entities);
		mButtonNewEntity = (Button) findViewById(R.id.button_new_entity);
		mButtonNewEntity.setText(getString(R.string.entity_button_entity_first) + " " + mListSchema);

		if (mListSchema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
			mButtonNewEntity.setText(R.string.entity_button_comment_first);
		}
		else if (mListSchema.equals(Constants.SCHEMA_ENTITY_POST)) {
			mButtonNewEntity.setText(R.string.entity_button_post_first);
		}
		else if (mListSchema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			mButtonNewEntity.setText(R.string.entity_button_applink_first);
		}

		mBusyManager = new BusyManager(this);
	}

	@Override
	protected void configureActionBar() {
		/*
		 * Navigation setup for action bar icon and title
		 */
		if (mListMode == ListMode.EntitiesForEntity) {
			final Entity entity = EntityManager.getEntity(mEntityId);
			mActionBar.setDisplayHomeAsUpEnabled(true);
			mActionBar.setTitle(entity.name);
		}
		else if (mListMode == ListMode.EntitiesByOwner) {
			mActionBar.setDisplayHomeAsUpEnabled(true);
			mActionBar.setHomeButtonEnabled(true);
			if (mEntityId != null) {
				ModelResult result = EntityManager.getInstance().getEntity(mEntityId, false, LinkOptions.getDefault(DefaultType.LinksUserWatching));
				User user = (User) result.serviceResponse.data;
				mActionBar.setTitle(user.name);
			}
		}
	}

	@Override
	protected void bind(final Boolean refresh) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("BindEntitiesForList");
				ModelResult result = loadEntities(refresh);
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					if (result.data == null || ((ArrayList<Entity>) result.data).size() == 0) {
						if (mListNewEnabled) {
							mButtonNewEntity.setVisibility(View.VISIBLE);
						}
						else {
							mBusyManager.hideBusy();
							onBackPressed();
						}
					}
					else {
						mEntities = (List<Entity>) result.data;
						mButtonNewEntity.setVisibility(View.GONE);
						Collections.sort(mEntities, new Entity.SortEntitiesByModifiedDate());
						mListView.setAdapter(new EndlessEntityAdapter(mEntities)); // draw happens in the adapter
					}

					mEntityModelRefreshDate = ProximityManager.getInstance().getLastBeaconLoadDate();
					mEntityModelActivityDate = EntityManager.getEntityCache().getLastActivityDate();
					mEntityModelUser = Aircandi.getInstance().getUser();
				}
				else {
					Routing.serviceError(BaseEntityList.this, result.serviceResponse);
				}
				mBusyManager.hideBusy();
			}

		}.execute();
	}

	private ModelResult loadEntities(Boolean refresh) {

		Map map = new HashMap<String, Object>();
		map.put("modifiedDate", -1);
		mCursorSettings = new Cursor()
				.setLimit(ProxiConstants.LIMIT_CHILD_ENTITIES)
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(refresh ? 0 : mEntities.size());

		ModelResult result = new ModelResult();

		if (mListMode == ListMode.EntitiesForEntity) {
			result = EntityManager.getInstance().loadEntitiesForEntity(mEntityId
					, mListSchema
					, null
					, mCursorSettings);
		}
		else if (mListMode == ListMode.EntitiesByOwner) {
			List<String> schemas = new ArrayList<String>();
			schemas.add(mListSchema);
			result = EntityManager.getInstance().getEntitiesByOwner(mEntityId
					, true
					, schemas
					, null
					, mCursorSettings);
		}

		return result;
	}

	public void doRefresh() {
		/* Called from AircandiCommon */
		bind(true);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------
	
	public void onNewEntityButtonClick(View view) {
		onAdd();
	}

	@Override
	public void onAdd() {
		/*
		 * We assume the new entity button wouldn't be visible if the
		 * entity is locked.
		 */
		IntentBuilder intentBuilder = new IntentBuilder(this, BaseEntityEdit.editFormBySchema(mListSchema))
				.setEntitySchema(mListSchema)
				.setEntityParentId(mEntityId);

		if (mListSchema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
			intentBuilder.setClass(CommentEdit.class);
		}

		startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_INSERT);
		Animate.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {
		Logger.v(this, "List item clicked");
		final Entity entity = (Entity) ((ViewHolder) view.getTag()).data;
		if (entity.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			Applink applink = (Applink) entity;
			if (applink.type.equals(Constants.TYPE_APPLINK_TWITTER)) {
				AndroidManager.getInstance().callTwitterActivity(this, applink.appId);
			}
			else if (applink.type.equals(Constants.TYPE_APPLINK_FACEBOOK)) {
				AndroidManager.getInstance().callFacebookActivity(this, applink.appId);
			}
			else if (applink.type.equals(Constants.TYPE_APPLINK_WEBSITE)) {
				AndroidManager.getInstance().callBrowserActivity(this, applink.appId);
			}
		}
		else {
			showCandiFormForEntity(entity.id, BaseEntityForm.viewFormBySchema(entity.schema));
		}
	}

	public void showCandiFormForEntity(String entityId, Class<?> clazz) {

		final IntentBuilder intentBuilder = new IntentBuilder(this, clazz)
				.setEntityId(entityId)
				.setEntitySchema(ServiceEntry.getSchemaFromId(entityId));

		Entity entity = EntityManager.getEntity(entityId);
		if (entity != null) {
			intentBuilder.setEntitySchema(entity.schema);
			if (entity.toId != null) {
				intentBuilder.setEntityParentId(entity.getParent().id);
			}
		}

		final Intent intent = intentBuilder.create();

		startActivity(intent);
		Animate.doOverridePendingTransition(this, TransitionType.PageToPage);
	}

	@SuppressWarnings("ucd")
	public void onCommentsClick(View view) {

		final Entity entity = (Entity) view.getTag();
		final IntentBuilder intentBuilder = new IntentBuilder(this, BaseEntityList.class)
				.setListMode(ListMode.EntitiesForEntity)
				.setEntityId(entity.id)
				.setListSchema(Constants.SCHEMA_ENTITY_COMMENT)
				.setListNewEnabled(true)
				.setListItemResId(R.layout.temp_listitem_comment);
		final Intent intent = intentBuilder.create();
		startActivity(intent);
		Animate.doOverridePendingTransition(this, TransitionType.PageToPage);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	protected void draw() {}
	
	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		/*
		 * Setup menu items that are common to entity list browsing.
		 */
		MenuItem menuItem = menu.findItem(R.id.add);
		menuItem.setVisible(false);
		if (mListNewEnabled) {
			menuItem.setTitle(R.string.menu_add_entity_item);
			if (mListSchema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
				menuItem.setTitle(R.string.menu_add_comment_item);
			}
			else if (mListSchema.equals(Constants.SCHEMA_ENTITY_POST)) {
				menuItem.setTitle(R.string.menu_add_post_item);
			}
			menuItem.setVisible(true);
		}

		MenuItem refresh = menu.findItem(R.id.refresh);
		if (refresh != null) {
			if (mBusyManager != null) {
				mBusyManager.setAccuracyIndicator(refresh.getActionView().findViewById(R.id.accuracy_indicator));
				mBusyManager.setRefreshImage(refresh.getActionView().findViewById(R.id.refresh_image));
				mBusyManager.setRefreshProgress(refresh.getActionView().findViewById(R.id.refresh_progress));
				mBusyManager.updateAccuracyIndicator();
			}

			refresh.getActionView().findViewById(R.id.refresh_frame).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					doRefresh();
				}
			});
		}

		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		/*
		 * We have to be pretty aggressive about refreshing the UI because
		 * there are lots of actions that could have happened while this activity
		 * was stopped that change what the user would expect to see.
		 * 
		 * - Entity deleted or modified
		 * - Entity children modified
		 * - New comments
		 * - Change in user which effects which candi and UI should be visible.
		 * - User signed out.
		 * - User profile could have been updated and we don't catch that.
		 */
		if (!isFinishing() && mEntityModelUser != null) {
			if (!Aircandi.getInstance().getUser().id.equals(mEntityModelUser.id)
					|| ProximityManager.getInstance().getLastBeaconLoadDate().longValue() > mEntityModelRefreshDate.longValue()
					|| EntityManager.getEntityCache().getLastActivityDate().longValue() > mEntityModelActivityDate.longValue()) {
				invalidateOptionsMenu();
				bind(true);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.entity_list;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes/enums
	// --------------------------------------------------------------------------------------------

	public static enum ListMode {
		EntitiesForEntity,
		EntitiesByOwner,
		EntitiesWatchedByUser,
	}

	private class EndlessEntityAdapter extends EndlessAdapter {

		private List<Entity>	moreEntities	= new ArrayList<Entity>();

		private EndlessEntityAdapter(List<Entity> list) {
			super(new ListAdapter(list));
		}

		@Override
		protected View getPendingView(ViewGroup parent) {
			if (mEntities.size() == 0) {
				return new View(BaseEntityList.this);

			}
			return mInflater.inflate(R.layout.temp_candi_list_item_placeholder, null);
		}

		@Override
		protected boolean cacheInBackground() {
			moreEntities.clear();
			if (mMore) {
				final ModelResult result = loadEntities(false);
				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					if (result.data != null) {
						final ServiceData serviceData = (ServiceData) result.data;

						if (serviceData != null) {
							moreEntities = (List<Entity>) serviceData.data;
							mMore = serviceData.more;
						}

						if (mMore) {
							return (getWrappedAdapter().getCount() + moreEntities.size()) < LIST_MAX;
						}
					}
				}
				else {
					Routing.serviceError(BaseEntityList.this, result.serviceResponse);
				}
			}
			return false;
		}

		@Override
		protected void appendCachedData() {
			final ArrayAdapter<Entity> list = (ArrayAdapter<Entity>) getWrappedAdapter();
			for (Entity entity : moreEntities) {
				list.add(entity);
			}
			list.sort(new Entity.SortEntitiesByModifiedDate());
			notifyDataSetChanged();
		}
	}

	public class ListAdapter extends ArrayAdapter<Entity> {

		private int	mScrollState	= ScrollManager.SCROLL_STATE_IDLE;

		private ListAdapter(List<Entity> items) {
			super(BaseEntityList.this, 0, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final ViewHolder holder;
			final Entity entity = mEntities.get(position);

			if (view == null) {
				view = mInflater.inflate(mListItemResId, null);
				holder = new ViewHolder();
				holder.name = (TextView) view.findViewById(R.id.name);
				holder.photo = (WebImageView) view.findViewById(R.id.photo);
				holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
				holder.description = (TextView) view.findViewById(R.id.description);
				holder.creator = (UserView) view.findViewById(R.id.creator);
				holder.area = (TextView) view.findViewById(R.id.area);
				holder.createdDate = (TextView) view.findViewById(R.id.created_date);

				holder.buttonComments = (Button) view.findViewById(R.id.button_comments);
				holder.checked = (CheckBox) view.findViewById(R.id.checked);

				if (holder.checked != null) {
					holder.checked.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View view) {
							final CheckBox checkBox = (CheckBox) view;
							final Entity entity = (Entity) checkBox.getTag();
							entity.checked = checkBox.isChecked();
						}
					});
				}
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
			}

			if (entity != null) {
				Logger.d(this, "Adapter getView: " + entity.name);
				holder.data = entity;
				holder.position = position;

				UI.setVisibility(holder.checked, View.GONE);
				if (holder.checked != null && entity.checked != null) {
					holder.checked.setChecked(entity.checked);
					holder.checked.setTag(entity);
					UI.setVisibility(holder.checked, View.VISIBLE);
				}

				UI.setVisibility(holder.name, View.GONE);
				if (holder.name != null && entity.name != null && entity.name.length() > 0) {
					holder.name.setText(entity.name);
					UI.setVisibility(holder.name, View.VISIBLE);
				}

				UI.setVisibility(holder.subtitle, View.GONE);
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					Place place = (Place) entity;
					if (holder.subtitle != null) {
						if (place.subtitle != null) {
							holder.subtitle.setText(place.subtitle);
							UI.setVisibility(holder.subtitle, View.VISIBLE);
						}
						else {
							if (place.category != null) {
								holder.subtitle.setText(Html.fromHtml(place.category.name));
								UI.setVisibility(holder.subtitle, View.VISIBLE);
							}
						}
					}
				}
				else {
					if (holder.subtitle != null && entity.subtitle != null && !entity.subtitle.equals("")) {
						holder.subtitle.setText(entity.subtitle);
						UI.setVisibility(holder.subtitle, View.VISIBLE);
					}
				}

				UI.setVisibility(holder.description, View.GONE);
				if (holder.description != null && entity.description != null && entity.description.length() > 0) {
					holder.description.setMaxLines(5);
					holder.description.setText(entity.description);
					UI.setVisibility(holder.description, View.VISIBLE);
				}

				/* Comments */

				UI.setVisibility(holder.buttonComments, View.GONE);
				if (holder.buttonComments != null) {
					Count count = entity.getCount(Constants.TYPE_LINK_COMMENT, Direction.in);
					Integer commentCount = count != null ? count.count.intValue() : 0;
					if (commentCount != null && commentCount > 0) {
						holder.buttonComments.setText(String.valueOf(commentCount) + ((commentCount == 1) ? " Comment" : " Comments"));
						holder.buttonComments.setTag(entity);
						UI.setVisibility(holder.buttonComments, View.VISIBLE);
					}
				}

				/* Creator */

				UI.setVisibility(holder.creator, View.GONE);
				if (holder.creator != null && entity.creator != null && !entity.creator.id.equals(ProxiConstants.ADMIN_USER_ID)) {
					holder.creator.bindToUser(entity.creator, entity.modifiedDate.longValue(), entity.locked);
					UI.setVisibility(holder.creator, View.VISIBLE);
				}

				UI.setVisibility(holder.area, View.GONE);
				if (holder.area != null && entity.creator != null && entity.creator.area != null && entity.creator.area.length() > 0) {
					holder.area.setText(entity.creator.area);
					UI.setVisibility(holder.area, View.VISIBLE);
				}

				UI.setVisibility(holder.createdDate, View.GONE);
				if (holder.createdDate != null && entity.createdDate != null) {
					holder.createdDate.setText(DateTime.timeSince(entity.createdDate.longValue(), DateTime.nowDate().getTime()));
					UI.setVisibility(holder.createdDate, View.VISIBLE);
				}

				if (holder.photo != null) {
					holder.photo.setTag(entity);
					/*
					 * The WebImageView sets the current bitmap ref being held
					 * by the internal image view to null before doing the work
					 * to satisfy the new request.
					 */
					if (entity.photo != null && entity.photo.getBitmap() != null) {
						UI.showImageInImageView(entity.photo.getBitmap(), holder.photo.getImageView(), true, Animate.fadeInMedium());
					}
					else {
						final String photoUri = entity.getPhotoUri();

						/* Don't do anything if the image is already set to the one we want */
						if (holder.photo.getImageUri() == null || !holder.photo.getImageUri().equals(photoUri)) {

							final BitmapRequestBuilder builder = new BitmapRequestBuilder(holder.photo)
									.setImageUri(photoUri);

							final BitmapRequest imageRequest = builder.create();

							holder.photoUri = photoUri;
							if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
								Place place = (Place) entity;
								if (place.synthetic) {
									final int color = Place.getCategoryColor((place.category != null) ? place.category.name : null, true, true, false);
									holder.photo.getImageView().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
								}
								else {
									holder.photo.getImageView().clearColorFilter();
								}
							}

							holder.photo.setBitmapRequest(imageRequest);
						}
					}
				}
			}
			return view;
		}

		@Override
		public Entity getItem(int position) {
			return mEntities.get(position);
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		public int getScrollState() {
			return mScrollState;
		}

		public void setScrollState(int scrollState) {
			mScrollState = scrollState;
		}

		private class ScrollManager implements AbsListView.OnScrollListener {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				mScrollState = scrollState;
				notifyDataSetChanged();
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
		}
	}

	public static class ViewHolder {

		public TextView		name;
		public WebImageView	photo;
		public TextView		subtitle;
		public TextView		description;
		public TextView		area;
		public TextView		createdDate;
		public UserView		creator;
		public CheckBox		checked;
		public int			position;

		public String		photoUri;		// Used for verification after fetching image
		public Object		data;			// Object binding to
		public Button		buttonComments;
	}

}