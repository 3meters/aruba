package com.aircandi.ui.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.aircandi.ProxiConstants;
import com.aircandi.applications.Comments;
import com.aircandi.beta.R;
import com.aircandi.components.BusyManager;
import com.aircandi.components.EndlessAdapter;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.Maps;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Cursor;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.DefaultType;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.User;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;

public abstract class BaseEntityList extends BaseBrowse {

	protected ListView			mListView;
	protected GridView			mGridView;
	protected OnClickListener	mClickListener;
	protected Integer			mPhotoWidthPixels;
	protected Integer			mPhotoMarginPixels;

	private List<Entity>		mEntities	= new ArrayList<Entity>();
	private Cursor				mCursorSettings;
	private Button				mButtonNewEntity;

	private Boolean				mMore		= false;
	private static final long	LIST_MAX	= 300L;

	private Number				mEntityModelRefreshDate;
	private Number				mEntityModelActivityDate;
	private User				mEntityModelUser;
	private EntityAdapter		mAdapter;

	/* Inputs */
	public String				mEntityId;
	protected String			mListLinkSchema;
	protected String			mListLinkType;
	protected String			mListLinkDirection;
	protected Integer			mListItemResId;
	protected Boolean			mListNewEnabled;

	@Override
	protected void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
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
			mListNewEnabled = extras.getBoolean(Constants.EXTRA_LIST_NEW_ENABLED, false);
			mListItemResId = extras.getInt(Constants.EXTRA_LIST_ITEM_RESID, getListItemResId(mListLinkSchema));
		}
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
				return LinkOptions.getDefault(DefaultType.NoLinks);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
				return LinkOptions.getDefault(DefaultType.LinksForPost);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				return LinkOptions.getDefault(DefaultType.LinksForPlace);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				return LinkOptions.getDefault(DefaultType.LinksForUser);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
				return LinkOptions.getDefault(DefaultType.NoLinks);
			}
		}
		return LinkOptions.getDefault(DefaultType.NoLinks);
	}

	@Override
	protected void initialize(Bundle savedInstanceState) {
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
					Routing.route(BaseEntityList.this, Route.Browse, entity, null, null);
				}
			}
		};

		mBusyManager = new BusyManager(this);
	}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();
		/*
		 * Navigation setup for action bar icon and title
		 */
		final Entity entity = EntityManager.getEntity(mEntityId);
		if (entity != null) {
			setActivityTitle(entity.name);
		}
	}

	@Override
	protected void configureNavigationDrawer() {
		super.configureNavigationDrawer();
		if (mDrawerLayout != null) {
			mDrawerToggle.setDrawerIndicatorEnabled(false);
		}
	}

	@Override
	protected void databind(final Boolean refresh) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy();
				mBusyManager.startBodyBusyIndicator();
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
						Collections.sort(mEntities, new Entity.SortByPositionModifiedDate());
						if (mAdapter == null) {
							mAdapter = new EntityAdapter(mEntities);
							if (mListView != null) {
								mListView.setAdapter(mAdapter); // draw happens in the adapter
							}
							else if (mGridView != null) {
								mGridView.setAdapter(mAdapter); // draw happens in the adapter
							}
						}
						else {
							mAdapter.notifyDataSetChanged();
						}
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

		ModelResult result = EntityManager.getInstance().loadEntitiesForEntity(mEntityId, getLinkOptions(mListLinkSchema), mCursorSettings);

		return result;
	}

	public void doRefresh() {
		/* Called from AircandiCommon */
		databind(true);
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
		 * entity is locked.
		 */
		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mEntityId);
		Routing.route(this, Route.New, null, mListLinkSchema, extras);
	}

	@SuppressWarnings("ucd")
	public void onCommentsClick(View view) {
		final Entity entity = (Entity) view.getTag();
		Comments.viewFor(this, entity.id, Constants.TYPE_LINK_COMMENT, null);
	}

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

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
			if (mListLinkSchema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
				menuItem.setTitle(R.string.menu_add_comment_item);
			}
			else if (mListLinkSchema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
				menuItem.setTitle(R.string.menu_add_picture_item);
			}
			menuItem.setVisible(true);
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
					doRefresh();
				}
			});
		}

		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
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
				databind(true);
			}
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
	// Inner classes/enums
	// --------------------------------------------------------------------------------------------

	private class EntityAdapter extends EndlessAdapter {

		private List<Entity>	moreEntities	= new ArrayList<Entity>();

		private EntityAdapter(List<Entity> list) {
			super(new ListAdapter(list));
		}

		@Override
		protected View getPendingView(ViewGroup parent) {
			if (mEntities.size() == 0) {
				return new View(BaseEntityList.this);

			}
			return LayoutInflater.from(BaseEntityList.this).inflate(R.layout.temp_candi_list_item_placeholder, null);
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
			list.sort(new Entity.SortByModifiedDate());
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
				holder.name = (TextView) view.findViewById(R.id.name);
				holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
				holder.type = (TextView) view.findViewById(R.id.type);
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

				holder.photoView = (AirImageView) view.findViewById(R.id.photo);
				if (mGridView != null) {
					final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mPhotoWidthPixels, mPhotoWidthPixels);
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
					UI.setVisibility(holder.type, View.VISIBLE);
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
					holder.creator.databind(entity.creator, entity.modifiedDate.longValue(), entity.locked);
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

				if (holder.photoView != null) {
					holder.photoView.setTag(entity);
					Photo photo = entity.getPhoto();
					if (entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
						photo = entity.creator.getPhoto();
					}

					UI.drawPhoto(holder.photoView, photo);
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
		public Object		data;			// Object binding to
		public Button		buttonComments;
	}

}