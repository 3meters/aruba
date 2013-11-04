package com.aircandi.ui.base;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

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
import com.aircandi.components.TrackerBase.TrackerCategory;
import com.aircandi.service.objects.CacheStamp;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Cursor;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.DateTime.IntervalContext;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;

public abstract class BaseEntityList extends BaseBrowse implements ListDelegate {

	protected AbsListView		mListView;
	protected OnClickListener	mClickListener;
	protected Integer			mPhotoWidthPixels;

	private List<Entity>		mEntities			= new ArrayList<Entity>();
	private Cursor				mCursorSettings;
	private Button				mButtonNewEntity;

	private static final long	LIST_MAX			= 300L;
	private static final int	PAGE_SIZE_DEFAULT	= 20;
	private static final int	LAZY_LOAD_THRESHOLD	= 10;

	private ListAdapter			mAdapter;
	private Boolean				mMore				= false;
	protected View				mLoading;

	/* Inputs */
	public String				mForEntityId;
	public Entity				mForEntity;
	protected String			mListLinkSchema;
	protected String			mListLinkType;
	protected String			mListLinkDirection;
	protected String			mListLinkInactive;
	protected Integer			mListItemResId;
	protected Boolean			mListNewEnabled;
	protected String			mListTitle;
	protected Integer			mListPageSize;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mForEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			/*
			 * Could be any link type: place, post, comment, applink, create, like, watch
			 */
			mListLinkSchema = extras.getString(Constants.EXTRA_LIST_LINK_SCHEMA);
			mListLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			if (mListLinkType == null) {
				mListLinkType = mListLinkSchema;
			}
			mListLinkDirection = extras.getString(Constants.EXTRA_LIST_LINK_DIRECTION);
			if (mListLinkDirection == null) {
				mListLinkDirection = "in";
			}
			mListLinkInactive = extras.getString(Constants.EXTRA_LIST_LINK_INACTIVE);
			mListNewEnabled = extras.getBoolean(Constants.EXTRA_LIST_NEW_ENABLED, false);
			mListPageSize = extras.getInt(Constants.EXTRA_LIST_PAGE_SIZE, PAGE_SIZE_DEFAULT);

			mListItemResId = extras.getInt(Constants.EXTRA_LIST_ITEM_RESID, getListItemResId(mListLinkSchema));
			mListTitle = extras.getString(Constants.EXTRA_LIST_TITLE);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mListView = (ListView) findViewById(R.id.list);
		if (mListView == null) {
			mListView = (GridView) findViewById(R.id.grid);
		}
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
				final Shortcut shortcut = entity.getShortcut();
				shortcut.linkType = mListLinkType;
				Routing.shortcut(BaseEntityList.this, shortcut, mForEntity, null);
			}
		};

		/* Hookup adapter */
		mForEntity = EntityManager.getEntity(mForEntityId);
		invalidateOptionsMenu();
		if (mForEntity != null) {
			mCacheStamp = mForEntity.getCacheStamp();
		}
		/*
		 * Triggers data fetch because endless wrapper calls cacheInBackground()
		 * when first created.
		 */
		mAdapter = new ListAdapter(mEntities);
		/*
		 * Bind adapter to UI triggers view generation but we might not
		 * have any data yet. When a new chuck of data is added to mEntities,
		 * notifyDataSetChanged is called on the adapter when then lets the
		 * UI know to repaint.
		 */
		if (mListView != null) {
			mListView.setAdapter(mAdapter);
		}
	}

	@Override
	public void bind(final BindingMode mode) {
		/*
		 * Might not have entities because of a network error so force
		 * a refresh if mode = manual.
		 */
		final AtomicBoolean refreshNeeded = new AtomicBoolean(mEntities.size() == 0);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mButtonNewEntity.setVisibility(View.GONE);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("ActivityStaleCheck");
				ModelResult result = new ModelResult();

				if (!refreshNeeded.get()) {
					showBusy();
					CacheStamp cacheStamp = EntityManager.getInstance().loadCacheStamp(mForEntity.id, mCacheStamp);
					/*
					 * For now, we refresh for both modified and activity to keep it simple. We do
					 * not update the ForEntity because that should be handled by code that is dealing with
					 * it directly. The cache stamp should keep us from doing extra refreshes even though
					 * the ForEntity hasn't changed.
					 */
					if (cacheStamp != null && !cacheStamp.equals(mCacheStamp)) {
						refreshNeeded.set(true);
						mCacheStamp = cacheStamp;
					}
				}

				if (refreshNeeded.get()) {
					showBusy();
					if (mEntities.size() == 0) {
						showBusy("Loading " + getActivityTitle() + "...", false);
					}
					result = loadEntities(0);
				}

				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
					hideBusy();
					Errors.handleError(BaseEntityList.this, result.serviceResponse);
				}
				else {
					if (refreshNeeded.get()) {
						if (result.data != null) {
							ServiceData serviceData = (ServiceData) result.serviceResponse.data;
							mMore = serviceData.more;

							mEntities.clear();
							mAdapter.setNotifyOnChange(false);
							mAdapter.clear();
							mAdapter.addAll((List<Entity>) result.data);
							mAdapter.sort(new Entity.SortByPositionSortDate());
							mAdapter.notifyDataSetChanged();
						}
						hideBusy();
					}
					else if (mode == BindingMode.MANUAL) {
						showBusyTimed(Constants.INTERVAL_FAKE_BUSY, false);
					}
					else {
						hideBusy();
					}
					if (mListNewEnabled && mEntities.size() == 0 && mEntities.size() == 0) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								mButtonNewEntity.setVisibility(View.VISIBLE);
							}
						});
					}
				}
			}
		}.execute();
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
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onOverflowButtonClick(View view) {

		final Entity entity = (Entity) view.getTag();

		if (Constants.SUPPORTS_HONEYCOMB) {

			PopupMenu popupMenu = new PopupMenu(this, view);
			onCreatePopupMenu(popupMenu.getMenu());
			popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

				@Override
				public boolean onMenuItemClick(android.view.MenuItem item) {
					switch (item.getItemId()) {
						case R.id.report:
							Routing.route(BaseEntityList.this, Route.REPORT, entity, entity.getSchemaMapped(), null);
							return true;
						default:
							return false;
					}
				}
			});
			popupMenu.show();
		}
		else {
			gingerbreadPopupMenu(entity);
		}
	}

	@SuppressWarnings("ucd")
	public void onNewEntityButtonClick(View view) {
		onAdd();
	}

	public void onMoreButtonClick(View view) {
		lazyLoad();
	}

	@Override
	public void onAdd() {
		/*
		 * The new entity button is visible even if the entity is locked. Now we do
		 * the actual hard core check.
		 */
		if (EntityManager.canUserAdd(mForEntity)) {
			Bundle extras = new Bundle();
			extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mForEntityId);
			Routing.route(this, Route.NEW, null, mListLinkSchema, extras);
			return;
		}

		if (mForEntity.locked) {
			Dialogs.locked(this, mForEntity);
		}
	}

	@SuppressWarnings("ucd")
	public void onCommentsClick(View view) {
		final Entity entity = (Entity) view.getTag();
		Comments.viewFor(this, entity.id, Constants.TYPE_LINK_CONTENT, null);
	}

	@Override
	public void onRefresh() {
		bind(BindingMode.MANUAL); // Called from Routing
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	private ModelResult loadEntities(Integer skip) {
		/*
		 * Called on a background thread.
		 * 
		 * Sorting is applied to links not the entities on the service side.
		 */
		mCursorSettings = new Cursor()
				.setLimit(mListPageSize)
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(skip);

		if (mListLinkInactive != null) {
			mCursorSettings.setWhere(Maps.asMap("inactive", Boolean.parseBoolean(mListLinkInactive)));
		}

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

		ModelResult result = EntityManager.getInstance().loadEntitiesForEntity(mForEntityId
				, getLinkOptions(mListLinkSchema)
				, mCursorSettings
				, null);

		return result;
	}

	private void lazyLoad() {

		final ViewSwitcher switcher = (ViewSwitcher) mLoading.findViewById(R.id.animator_more);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				switcher.setDisplayedChild(1);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("LazyLoadList");
				ModelResult result = loadEntities(mEntities.size());
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
					Errors.handleError(BaseEntityList.this, result.serviceResponse);
				}
				else {
					if (result.data != null) {
						ServiceData serviceData = (ServiceData) result.serviceResponse.data;
						mMore = serviceData.more;
						mAdapter.addAll((List<Entity>) result.data);
						mAdapter.sort(new Entity.SortByPositionSortDate());
						mAdapter.notifyDataSetChanged();
					}
				}
				switcher.setDisplayedChild(0);
			}
		}.execute();
	}

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
				return LinkOptions.getDefault(LinkProfile.LINKS_FOR_COMMENT);
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
				return LinkOptions.getDefault(LinkProfile.LINKS_FOR_USER_CURRENT);
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

	public void gingerbreadPopupMenu(final Entity entity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setItems(R.array.more_options_entity, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int item) {
						if (item == 0) {
							Routing.route(BaseEntityList.this, Route.REPORT, entity, entity.getSchemaMapped(), null);
						}
					}
				});
		AlertDialog alert = builder.create();

		/* Prevent dimming the background */
		if (Constants.SUPPORTS_ICE_CREAM_SANDWICH) {
			alert.getWindow().setDimAmount(Constants.POPUP_DIM_AMOUNT);
		}

		alert.show();
	}

	public boolean onCreatePopupMenu(android.view.Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_popup_entity, menu);
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		/*
		 * Setup menu items that are common to entity list browsing.
		 */
		mMenuItemAdd = menu.findItem(R.id.add);
		if (mMenuItemAdd != null) {
			mMenuItemAdd.setVisible(UI.showAction(Route.ADD, mForEntity));
			mMenuItemAdd.setTitle(R.string.menu_add_entity_item);
			if (mListLinkSchema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
				mMenuItemAdd.setTitle(R.string.menu_add_comment_item);
			}
			else if (mListLinkSchema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
				mMenuItemAdd.setTitle(R.string.menu_add_picture_item);
			}
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
					Aircandi.tracker.sendEvent(TrackerCategory.UX, "list_refresh_by_user", mListLinkSchema + "_" + mListLinkType, 0);
					onRefresh();
				}
			});
		}

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (mMenuItemAdd != null) {
			mMenuItemAdd.setVisible(UI.showAction(Route.ADD, mForEntity));
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		if (!isFinishing()) {
			bind(BindingMode.AUTO); // Setting this here because it doesn't mean a service call
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
	// Classes
	// --------------------------------------------------------------------------------------------

	public class ListAdapter extends ArrayAdapter<Entity> {

		@Override
		public int getCount() {
			return mEntities.size() + (mMore ? 1 : 0);
		}

		private ListAdapter(List<Entity> items) {
			super(BaseEntityList.this, 0, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (position == mEntities.size()) {
				return mLoading;
			}

			View view = convertView;
			final ViewHolder holder;
			final Entity entity = mEntities.get(position);

			if (view == null || view.findViewById(R.id.animator_more) != null) {
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
				holder.overflow = (ComboButton) view.findViewById(R.id.button_overflow);

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

				if (mListView instanceof GridView) {
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
				holder.data = entity;
				holder.position = position;

				UI.setVisibility(holder.checked, View.GONE);
				if (holder.checked != null && entity.checked != null) {
					holder.checked.setChecked(entity.checked);
					holder.checked.setTag(entity);
					UI.setVisibility(holder.checked, View.VISIBLE);
				}

				UI.setVisibility(holder.overflow, View.GONE);
				if (holder.overflow != null) {
					holder.overflow.setTag(entity);
					UI.setVisibility(holder.overflow, View.VISIBLE);
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
							if (place.category != null && !TextUtils.isEmpty(place.category.name)) {
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
						else if (entity.type.equals(Constants.TYPE_APP_BOUNCE)) {
							typeVerbose = mResources.getString(R.string.candigram_type_bounce_verbose);
						}
						else if (entity.type.equals(Constants.TYPE_APP_EXPAND)) {
							typeVerbose = mResources.getString(R.string.candigram_type_expand_verbose);
						}
						holder.type.setText(typeVerbose);
					}
					UI.setVisibility(holder.type, View.VISIBLE);
				}

				UI.setVisibility(holder.description, View.GONE);
				if (holder.description != null && entity.description != null && entity.description.length() > 0) {
					holder.description.setText(entity.description);
					UI.setVisibility(holder.description, View.VISIBLE);
				}

				/* Comments */

				UI.setVisibility(holder.comments, View.GONE);
				if (holder.comments != null) {
					Count count = entity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_COMMENT, false, Direction.in);
					Integer commentCount = count != null ? count.count.intValue() : 0;
					if (commentCount != null && commentCount > 0) {
						holder.comments.setText(String.valueOf(commentCount) + ((commentCount == 1) ? " Comment" : " Comments"));
						holder.comments.setTag(entity);
						UI.setVisibility(holder.comments, View.VISIBLE);
					}
				}

				/* Creator */

				UI.setVisibility(holder.creator, View.GONE);
				if (holder.creator != null && entity.creator != null
						&& !entity.creator.id.equals(ServiceConstants.ADMIN_USER_ID)
						&& !entity.creator.id.equals(ServiceConstants.ANONYMOUS_USER_ID)) {
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

				/* Place context */
				UI.setVisibility(view.findViewById(R.id.place_holder), View.GONE);
				if (entity.place != null) {
					if (holder.placePhotoView != null) {
						Photo photo = entity.place.getPhoto();
						if (holder.placePhotoView.getPhoto() == null || !holder.placePhotoView.getPhoto().getUri().equals(photo.getUri())) {
							UI.drawPhoto(holder.placePhotoView, photo);
						}
						if (Type.isFalse(photo.usingDefault)) {
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

				/* Photo */

				if (holder.photoView != null) {
					holder.photoView.setTag(entity);
					Photo photo = entity.getPhoto();
					if (entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
						photo = entity.creator.getPhoto();
					}
					if (holder.photoView.getPhoto() == null || !photo.getUri().equals(holder.photoView.getPhoto().getUri())) {
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

	}

	@SuppressWarnings("unused")
	private class ScrollManager implements AbsListView.OnScrollListener {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			if (scrollState == SCROLL_STATE_IDLE) {
				if (mMore
						&& mEntities.size() < LIST_MAX
						&& mListView.getLastVisiblePosition() >= (mListView.getCount() - LAZY_LOAD_THRESHOLD)) {
					lazyLoad();
				}
			}
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}

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
		public ComboButton	overflow;
		public CheckBox		checked;
		@SuppressWarnings("ucd")
		public int			position;

		@SuppressWarnings("ucd")
		public String		photoUri;		// Used for verification after fetching image
		public Object		data;			// object binding to
		public TextView		comments;
	}

}