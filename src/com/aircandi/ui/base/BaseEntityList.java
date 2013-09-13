package com.aircandi.ui.base;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.applications.Candigrams;
import com.aircandi.applications.Comments;
import com.aircandi.applications.Pictures;
import com.aircandi.applications.Places;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.Maps;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.service.objects.CacheStamp;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Cursor;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.DateTime.IntervalContext;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;
import com.commonsware.cwac.endless.EndlessAdapter;

public abstract class BaseEntityList extends BaseBrowse implements IList {

	protected ListView			mListView;
	protected GridView			mGridView;
	protected OnClickListener	mClickListener;
	protected Integer			mPhotoWidthPixels;

	private List<Entity>		mEntities	= new ArrayList<Entity>();
	private Cursor				mCursorSettings;
	private Button				mButtonNewEntity;

	private long				mOffset		= 0;
	private static final long	LIST_MAX	= 300L;
	private static final long	PAGE_SIZE	= 10L;

	private EntityAdapter		mAdapter;

	/* Inputs */
	public String				mForEntityId;
	public Entity				mForEntity;
	protected String			mListLinkSchema;
	protected String			mListLinkType;
	protected String			mListLinkDirection;
	protected Integer			mListItemResId;
	protected Boolean			mListNewEnabled;
	protected String			mListTitle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Aircandi.stopwatch3.start(this.getClass().getSimpleName() + " create");
		super.onCreate(savedInstanceState);
	}

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mForEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			/*
			 * Could be any link TYPE: place, post, comment, applink, create, like, WATCH
			 */
			mListLinkSchema = extras.getString(Constants.EXTRA_LIST_LINK_SCHEMA);
			mListLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			if (mListLinkType == null) {
				mListLinkType = mListLinkSchema;
			}
			mListLinkDirection = extras.getString(Constants.EXTRA_LIST_LINK_DIRECTION);
			if (mListLinkDirection == null) {
				mListLinkDirection = "IN";
			}
			mListNewEnabled = extras.getBoolean(Constants.EXTRA_LIST_NEW_ENABLED, false);
			mListItemResId = extras.getInt(Constants.EXTRA_LIST_ITEM_RESID, getListItemResId(mListLinkSchema));
			mListTitle = extras.getString(Constants.EXTRA_LIST_TITLE);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mListView = (ListView) findViewById(R.id.list);
		mGridView = (GridView) findViewById(R.id.grid);
		mButtonNewEntity = (Button) findViewById(R.id.button_new_entity);
		mButtonNewEntity.setText(getString(R.string.entity_button_entity_first) + " " + mListLinkSchema);

		if (mListLinkSchema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
			mButtonNewEntity.setText(R.string.entity_button_comment_first);
		}
		else if (mListLinkSchema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
			mButtonNewEntity.setText(R.string.entity_button_picture_first);
		}
		else if (mListLinkSchema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			mButtonNewEntity.setText(R.string.entity_button_applink_first);
		}

		mClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Logger.v(this, "List item clicked");

				final Entity entity = (Entity) ((ViewHolder) v.getTag()).data;
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
					Routing.shortcut(BaseEntityList.this, entity.getShortcut(), null, null);
				}
				else {
					Routing.route(BaseEntityList.this, Route.BROWSE, entity, null, null);
				}
			}
		};
		Aircandi.stopwatch3.segmentTime(this.getClass().getSimpleName() + " initialized");
	}

	@Override
	public void bind(final BindingMode mode) {

		if (mAdapter == null) {
			mButtonNewEntity.setVisibility(View.GONE);
			showBusy("Loading " + getActivityTitle() + "...", false);
			mForEntity = EntityManager.getEntity(mForEntityId);
			mCacheStamp = mForEntity.getCacheStamp();
			setAdapter();
		}
		else {

			final AtomicBoolean refreshNeeded = new AtomicBoolean(false);

			new AsyncTask() {

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("ActivityStaleCheck");
					ModelResult result = new ModelResult();

					showBusy();
					CacheStamp cacheStamp = EntityManager.getInstance().loadCacheStamp(mForEntity.id, mCacheStamp);
					/*
					 * For now, we refresh for BOTH modified and activity to keep it simple. We do
					 * not update the ForEntity because that should be handled by code that is dealing with
					 * it directly. The cache stamp should keep us from doing extra refreshes even though
					 * the ForEntity hasn't changed.
					 */
					if (!cacheStamp.equals(mCacheStamp)) {
						refreshNeeded.set(true);
						mCacheStamp = cacheStamp;
					}
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					if (refreshNeeded.get()) {
						/*
						 * Using brute force to rebuild the data because I've
						 * beat my head against the wall trying to get other refresh
						 * mechanisms to work.
						 */
						setAdapter();
					}
					else if (mode == BindingMode.SERVICE) {
						showBusyTimed(Constants.INTERVAL_FAKE_BUSY, false);
					}
					else {
						hideBusy();
					}
				}
			}.execute();
		}
	}

	protected void setAdapter() {
		mOffset = 0;
		mEntities.clear();
		mAdapter = new EntityAdapter(mEntities);
		if (mListView != null) {
			mListView.setAdapter(mAdapter); // draw happens IN the adapter
		}
		else if (mGridView != null) {
			mGridView.setAdapter(mAdapter); // draw happens IN the adapter
		}
	}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();
		/*
		 * Navigation setup for action bar icon and title
		 */
		if (mListLinkSchema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
			Drawable icon = getResources().getDrawable(R.drawable.img_picture_temp);
			icon.setColorFilter(Aircandi.getInstance().getResources().getColor(Pictures.ICON_COLOR), PorterDuff.Mode.SRC_ATOP);
			mActionBar.setIcon(icon);
			setActivityTitle(mListTitle != null ? mListTitle : "pictures");
		}
		else if (mListLinkSchema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
			Drawable icon = getResources().getDrawable(R.drawable.img_comment_temp);
			icon.setColorFilter(Aircandi.getInstance().getResources().getColor(Comments.ICON_COLOR), PorterDuff.Mode.SRC_ATOP);
			mActionBar.setIcon(icon);
			setActivityTitle(mListTitle != null ? mListTitle : "comments");
		}
		else if (mListLinkSchema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
			Drawable icon = getResources().getDrawable(R.drawable.img_candigram_temp);
			icon.setColorFilter(Aircandi.getInstance().getResources().getColor(Candigrams.ICON_COLOR), PorterDuff.Mode.SRC_ATOP);
			mActionBar.setIcon(icon);
			setActivityTitle(mListTitle != null ? mListTitle : "candigrams");
		}
		else if (mListLinkSchema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			Drawable icon = getResources().getDrawable(R.drawable.img_place_temp);
			icon.setColorFilter(Aircandi.getInstance().getResources().getColor(Places.ICON_COLOR), PorterDuff.Mode.SRC_ATOP);
			mActionBar.setIcon(icon);
			setActivityTitle(mListTitle != null ? mListTitle : "places");
		}
		Aircandi.stopwatch3.segmentTime(this.getClass().getSimpleName() + " action bar configured");
	}

	private ModelResult loadEntities(Boolean refresh) {
		/*
		 * Called on a background thread.
		 */

		mCursorSettings = new Cursor()
				.setLimit(PAGE_SIZE)
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(refresh ? 0 : mEntities.size());

		if (mListLinkSchema != null) {
			List<String> schemas = new ArrayList<String>();
			schemas.add(mListLinkSchema);
			mCursorSettings.setSchemas(schemas);
		}

		if (mListLinkType != null) {
			List<String> linkTypes = new ArrayList<String>();
			linkTypes.add(mListLinkType);
			mCursorSettings.setLinkTypes(linkTypes);
		}

		if (mListLinkDirection != null) {
			mCursorSettings.setDirection(mListLinkDirection);
		}

		LinkOptions linkOptions = getLinkOptions(mListLinkSchema);
		if (mForEntity != null && mForEntity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
			if (mListLinkSchema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				linkOptions.setIgnoreInactive(true);
			}
		}
		ModelResult result = EntityManager.getInstance().loadEntitiesForEntity(mForEntityId, linkOptions, mCursorSettings, Aircandi.stopwatch3);

		return result;
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onNewEntityButtonClick(View view) {
		onAdd();
	}

	@Override
	public void onAdd() {
		/*
		 * We assume the new entity button wouldn't be visible if the
		 * entity is LOCKED.
		 */
		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mForEntityId);
		Routing.route(this, Route.NEW, null, mListLinkSchema, extras);
	}

	@SuppressWarnings("ucd")
	public void onCommentsClick(View view) {
		final Entity entity = (Entity) view.getTag();
		Comments.viewFor(this, entity.id, Constants.TYPE_LINK_COMMENT, null);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	protected Integer getListItemResId(String schema) {
		Integer itemResId = R.layout.temp_listitem_entity;
		if (schema != null && schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
			itemResId = R.layout.temp_listitem_comment;
		}
		return itemResId;
	}

	private LinkOptions getLinkOptions(String schema) {
		if (schema != null) {
			if (schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
				return LinkOptions.getDefault(LinkProfile.NO_LINKS);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
				return LinkOptions.getDefault(LinkProfile.LINKS_FOR_PICTURE);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
				return LinkOptions.getDefault(LinkProfile.LINKS_FOR_CANDIGRAM);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				return LinkOptions.getDefault(LinkProfile.LINKS_FOR_PLACE);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				return LinkOptions.getDefault(LinkProfile.LINKS_FOR_USER);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
				return LinkOptions.getDefault(LinkProfile.NO_LINKS);
			}
		}
		return LinkOptions.getDefault(LinkProfile.NO_LINKS);
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		/*
		 * Setup menu items that are common to entity list browsing.
		 */
		mMenuItemAdd = menu.findItem(R.id.add);
		if (mMenuItemAdd != null) {
			mMenuItemAdd.setVisible(canUserAdd());
		}

		MenuItem refresh = menu.findItem(R.id.refresh);
		if (refresh != null) {
			if (mBusyManager != null) {
				mBusyManager.setRefreshImage(refresh.getActionView().findViewById(R.id.refresh_image));
				mBusyManager.setRefreshProgress(refresh.getActionView().findViewById(R.id.refresh_progress));
			}

			refresh.getActionView().findViewById(R.id.refresh_frame).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					onRefresh();
				}
			});
		}

		return true;
	}

	protected Boolean canUserAdd() {
		if (mListNewEnabled) {
			if (Aircandi.currentPlace != null && !Aircandi.currentPlace.hasActiveProximity()) {
				if (mForEntity != null ? !mForEntity.isOwnedByCurrentUser() : true) {
					return false;
				}
			}
			mMenuItemAdd.setTitle(R.string.menu_add_entity_item);
			if (mListLinkSchema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
				mMenuItemAdd.setTitle(R.string.menu_add_comment_item);
			}
			else if (mListLinkSchema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
				mMenuItemAdd.setTitle(R.string.menu_add_picture_item);
			}
			mMenuItemAdd.setVisible(true);
			return true;
		}
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		if (!isFinishing()) {
			invalidateOptionsMenu();
			bind(BindingMode.AUTO); // Setting this here because it doesn't mean a SERVICE call
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.entity_list;
	}

	// --------------------------------------------------------------------------------------------
	// Classes/enums
	// --------------------------------------------------------------------------------------------

	private class EntityAdapter extends EndlessAdapter {

		private List<Entity>	mMoreEntities	= new ArrayList<Entity>();

		private EntityAdapter(List<Entity> list) {
			super(new ListAdapter(list));
		}

		@Override
		protected boolean cacheInBackground() {
			/*
			 * Triggered:
			 * 
			 * - first time the adapter runs.
			 * - when this function reported more available and the special pending view
			 * is being rendered by getView.
			 * 
			 * Returning true means we think there are more items available to QUERY for.
			 * 
			 * This is called on background thread from an AsyncTask started by EndlessAdapter.
			 */
			mMoreEntities.clear();
			final ModelResult result = loadEntities(false);

			if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
				hideBusy();
				Routing.serviceError(BaseEntityList.this, result.serviceResponse);
				Aircandi.stopwatch3.stop(this.getClass().getSimpleName() + " databind failed");
				return false;
			}
			else {
				if (result.data != null) {
					mMoreEntities = (List<Entity>) result.data;

					if (mMoreEntities.size() == 0) {
						if (mOffset == 0) {
							if (mListNewEnabled) {
								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										mButtonNewEntity.setVisibility(View.VISIBLE);
									}
								});
							}
						}
					}
					else {
						if (mMoreEntities.size() >= PAGE_SIZE) {
							mOffset += PAGE_SIZE;
							hideBusy();
							Aircandi.stopwatch3.stop(this.getClass().getSimpleName() + " databind - more data available");
							return (getWrappedAdapter().getCount() + mMoreEntities.size()) < LIST_MAX;
						}
						if (mListNewEnabled) {
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									mButtonNewEntity.setVisibility(View.GONE);
								}
							});
						}
					}
				}
			}

			hideBusy();
			Aircandi.stopwatch3.stop(this.getClass().getSimpleName() + " databind complete");
			return false;
		}

		@Override
		protected View getPendingView(ViewGroup parent) {
			if (mEntities.size() == 0) {
				return new View(BaseEntityList.this);
			}
			return LayoutInflater.from(BaseEntityList.this).inflate(R.layout.temp_candi_list_item_placeholder, null);
		}

		@Override
		protected void appendCachedData() {
			final ArrayAdapter<Entity> list = (ArrayAdapter<Entity>) getWrappedAdapter();
			for (Entity entity : mMoreEntities) {
				list.add(entity);
			}
			if (mListLinkType.equals(Constants.TYPE_LINK_CANDIGRAM)
					|| mListLinkType.equals(Constants.TYPE_LINK_WATCH)
					|| mListLinkType.equals(Constants.TYPE_LINK_CREATE)
					|| mListLinkType.equals(Constants.TYPE_LINK_COMMENT)) {
				list.sort(new Entity.SortByLinkModifiedDate());
			}
			else {
				list.sort(new Entity.SortByLinkModifiedDate());
			}
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
				view = LayoutInflater.from(BaseEntityList.this).inflate(mListItemResId, null);
				holder = new ViewHolder();

				holder.photoView = (AirImageView) view.findViewById(R.id.photo);
				holder.name = (TextView) view.findViewById(R.id.name);
				holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
				holder.type = (TextView) view.findViewById(R.id.type);
				holder.description = (TextView) view.findViewById(R.id.description);
				holder.creator = (UserView) view.findViewById(R.id.creator);
				holder.area = (TextView) view.findViewById(R.id.area);
				holder.createdDate = (TextView) view.findViewById(R.id.created_date);
				holder.comments = (TextView) view.findViewById(R.id.comments);
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

				holder.placePhotoView = (AirImageView) view.findViewById(R.id.place_photo);
				holder.placeName = (TextView) view.findViewById(R.id.place_name);
				holder.userName = (TextView) view.findViewById(R.id.user_name);

				if (mGridView != null) {
					Integer nudge = mResources.getDimensionPixelSize(R.dimen.grid_item_height_kick);
					final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mPhotoWidthPixels, mPhotoWidthPixels - nudge);
					holder.photoView.getImageView().setLayoutParams(params);
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

				UI.setVisibility(holder.type, View.GONE);
				if (holder.type != null && entity.type != null && entity.type.length() > 0) {
					holder.type.setText(entity.type);
					if (entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
						String typeVerbose = entity.type;
						if (entity.type.equals(Constants.TYPE_APP_TOUR)) {
							typeVerbose = mResources.getString(R.string.candigram_type_tour_verbose);
						}
						if (entity.type.equals(Constants.TYPE_APP_BOUNCE)) {
							typeVerbose = mResources.getString(R.string.candigram_type_bounce_verbose);
						}
						holder.type.setText(typeVerbose);
					}
					UI.setVisibility(holder.type, View.VISIBLE);
				}

				UI.setVisibility(holder.description, View.GONE);
				if (holder.description != null && entity.description != null && entity.description.length() > 0) {
					holder.description.setMaxLines(5);
					holder.description.setText(entity.description);
					UI.setVisibility(holder.description, View.VISIBLE);
				}

				/* Comments */

				UI.setVisibility(holder.comments, View.GONE);
				if (holder.comments != null) {
					Count count = entity.getCount(Constants.TYPE_LINK_COMMENT, Direction.IN);
					Integer commentCount = count != null ? count.count.intValue() : 0;
					if (commentCount != null && commentCount > 0) {
						holder.comments.setText(String.valueOf(commentCount) + ((commentCount == 1) ? " COMMENT" : " Comments"));
						holder.comments.setTag(entity);
						UI.setVisibility(holder.comments, View.VISIBLE);
					}
				}

				/* Creator */

				UI.setVisibility(holder.creator, View.GONE);
				if (holder.creator != null && entity.creator != null && !entity.creator.id.equals(ServiceConstants.ADMIN_USER_ID)) {
					holder.creator.databind(entity.creator, entity.modifiedDate.longValue(), entity.locked);
					UI.setVisibility(holder.creator, View.VISIBLE);
				}

				UI.setVisibility(holder.userName, View.GONE);
				if (holder.userName != null && entity.creator != null && entity.creator.name != null && entity.creator.name.length() > 0) {
					holder.userName.setText(entity.creator.name);
					UI.setVisibility(holder.userName, View.VISIBLE);
				}

				UI.setVisibility(holder.area, View.GONE);
				if (holder.area != null && entity.creator != null && entity.creator.area != null && entity.creator.area.length() > 0) {
					holder.area.setText(entity.creator.area);
					UI.setVisibility(holder.area, View.VISIBLE);
				}

				UI.setVisibility(holder.createdDate, View.GONE);
				if (holder.createdDate != null && entity.createdDate != null) {
					holder.createdDate.setText(DateTime.interval(entity.createdDate.longValue(), DateTime.nowDate().getTime(), IntervalContext.PAST));
					UI.setVisibility(holder.createdDate, View.VISIBLE);
				}

				/* PLACE context */
				UI.setVisibility(view.findViewById(R.id.place_holder), View.GONE);
				if (entity.place != null) {
					if (holder.placePhotoView != null) {
						Photo photo = entity.place.getPhoto();
						if (holder.placePhotoView.getPhoto() == null || !holder.placePhotoView.getPhoto().getUri().equals(photo.getUri())) {
							UI.drawPhoto(holder.placePhotoView, photo);
						}
						if (photo.usingDefault == null || !photo.usingDefault) {
							holder.placePhotoView.setClickable(true);
						}
						UI.setVisibility(holder.placePhotoView, View.VISIBLE);
					}
					UI.setVisibility(holder.placeName, View.GONE);
					if (holder.placeName != null && entity.place.name != null && !entity.place.name.equals("")) {
						holder.placeName.setText(Html.fromHtml(entity.place.name));
						UI.setVisibility(holder.placeName, View.VISIBLE);
					}
					UI.setVisibility(view.findViewById(R.id.place_holder), View.VISIBLE);
				}

				/* PHOTO */

				if (holder.photoView != null) {
					holder.photoView.setTag(entity);
					Photo photo = entity.getPhoto();
					if (entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
						photo = entity.creator.getPhoto();
					}
					if (holder.photoView.getPhoto() == null || !holder.photoView.getPhoto().getUri().equals(photo.getUri())) {
						holder.photoView.getImageView().setImageDrawable(null);
						UI.drawPhoto(holder.photoView, photo);
					}
				}

				view.setClickable(true);
				view.setOnClickListener(mClickListener);
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
		public AirImageView	photoView;
		public AirImageView	placePhotoView;
		public TextView		placeName;
		public TextView		userName;
		public TextView		subtitle;
		public TextView		description;
		public TextView		type;
		public TextView		area;
		public TextView		createdDate;
		public UserView		creator;
		public CheckBox		checked;
		@SuppressWarnings("ucd")
		public int			position;

		@SuppressWarnings("ucd")
		public String		photoUri;		// Used for verification after fetching image
		public Object		data;			// OBJECT binding to
		public TextView		comments;
	}

}