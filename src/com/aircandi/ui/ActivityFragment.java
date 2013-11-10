package com.aircandi.ui;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.applications.Comments;
import com.aircandi.components.EntityManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Maps;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.service.objects.Activity;
import com.aircandi.service.objects.Cursor;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.DateTime.IntervalContext;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;

public class ActivityFragment extends BaseFragment {

	protected ListView			mListView;
	protected TextView			mMessage;

	protected Integer			mLastViewedPosition;
	protected Integer			mTopOffset;
	protected Integer			mSavedScrollPositionX;
	protected Integer			mSavedScrollPositionY;

	private static final int	PAGE_SIZE_DEFAULT	= 20;

	protected OnClickListener	mClickListener;
	private List<Activity>		mActivities			= new ArrayList<Activity>();
	private Cursor				mCursor;
	private ListAdapter			mAdapter;
	private Boolean				mMore				= false;
	protected View				mLoading;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Activity activity = (Activity) ((ViewHolder) v.getTag()).data;

				/* Build intent that can be used in association with the notification */
				if (activity.action.entity != null) {
					if (activity.action.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
						activity.intent = Comments.viewForGetIntent(getSherlockActivity(), activity.action.toEntity.id, Constants.TYPE_LINK_CONTENT, null,
								null);
					}
					else {
						Class<?> clazz = BaseEntityForm.viewFormBySchema(activity.action.entity.schema);
						IntentBuilder intentBuilder = new IntentBuilder(getSherlockActivity(), clazz)
								.setEntityId(activity.action.entity.id)
								.setEntitySchema(activity.action.entity.schema)
								.setForceRefresh(true);
						activity.intent = intentBuilder.create();
					}
				}

				Routing.intent(getSherlockActivity(), activity.intent);
			}
		};
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		/*
		 * The listview scroll position seems to be preserved between destroy view
		 * and create view. Probably falls into the buck of view properties that are
		 * auto restored by android.
		 */
		if (view == null) return view;

		mListView = (ListView) view.findViewById(R.id.list);
		mMessage = (TextView) view.findViewById(R.id.message);
		mLoading = LayoutInflater.from(getSherlockActivity()).inflate(R.layout.temp_list_item_loading, null);

		/*
		 * Triggers data fetch because endless wrapper calls cacheInBackground()
		 * when first created.
		 */
		mAdapter = new ListAdapter(mActivities);
		/*
		 * Bind adapter to UI triggers view generation but we might not
		 * have any data yet. When a new chuck of data is added to mEntities,
		 * notifyDataSetChanged is called on the adapter when then lets the
		 * UI know to repaint.
		 */
		if (mListView != null) {
			mListView.setAdapter(mAdapter);
		}

		return view;
	}

	@Override
	public void databind(final BindingMode mode) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mMessage.setVisibility(View.GONE);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("LoadActivities");
				ModelResult result = new ModelResult();

				showBusy();
				if (mActivities.size() == 0 || mode == BindingMode.MANUAL) {
					showBusy("Loading activity...", false);
					result = loadActivities(0);
				}

				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
					hideBusy();
					Errors.handleError(getSherlockActivity(), result.serviceResponse);
				}
				else {
					if (result.data != null) {
						ServiceData serviceData = (ServiceData) result.serviceResponse.data;
						mMore = serviceData.more;

						mActivities.clear();
						mAdapter.setNotifyOnChange(false);
						mAdapter.clear();
						mAdapter.addAll((List<Activity>) result.data);
						mAdapter.sort(new Activity.SortBySortDate());
						mAdapter.notifyDataSetChanged();
					}
					hideBusy();
				}
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onRefresh() {
		saveListPosition();
		super.onRefresh();
	}

	public void onMoreButtonClick(View view) {
		lazyLoad();
	}

	@Override
	public void onScollToTop() {
		scrollToTop(mListView);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	private ModelResult loadActivities(Integer skip) {
		/*
		 * Called on a background thread.
		 * 
		 * Sorting is applied to links not the entities on the service side.
		 */
		List<String> schemas = new ArrayList<String>();
		schemas.add(Constants.SCHEMA_ENTITY_PLACE);
		schemas.add(Constants.SCHEMA_ENTITY_CANDIGRAM);
		schemas.add(Constants.SCHEMA_ENTITY_PICTURE);
		schemas.add(Constants.SCHEMA_ENTITY_USER);

		List<String> linkTypes = new ArrayList<String>();
		linkTypes.add(Constants.TYPE_LINK_CREATE);
		linkTypes.add(Constants.TYPE_LINK_WATCH);

		mCursor = new Cursor()
				.setLimit(PAGE_SIZE_DEFAULT)
				.setSort(Maps.asMap("modifiedDate", -1))
				.setSkip(skip)
				.setSchemas(schemas)
				.setLinkTypes(linkTypes)
				.setWhere(Maps.asMap("type", Maps.asMap("$regex", "^insert|^move|^expand|^restart")));

		ModelResult result = EntityManager.getInstance().loadActivities(Aircandi.getInstance().getCurrentUser().id, mCursor);

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
				ModelResult result = loadActivities(mActivities.size());
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
					Errors.handleError(getSherlockActivity(), result.serviceResponse);
				}
				else {
					if (result.data != null) {
						ServiceData serviceData = (ServiceData) result.serviceResponse.data;
						mMore = serviceData.more;
						mAdapter.addAll((List<Activity>) result.data);
						mAdapter.sort(new Activity.SortBySortDate());
						mAdapter.notifyDataSetChanged();
					}
				}
				switcher.setDisplayedChild(0);
			}
		}.execute();
	}

	private void saveListPosition() {
		mLastViewedPosition = mListView.getFirstVisiblePosition();
		View view = mListView.getChildAt(0);
		mTopOffset = (view == null) ? 0 : view.getTop();
	}

	@SuppressWarnings("unused")
	private void setListPosition() {
		if (mLastViewedPosition != null) {
			mListView.setSelectionFromTop(mLastViewedPosition, mTopOffset);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	public void onPause() {
		super.onPause();
		saveListPosition();
	}

	@Override
	public void onResume() {
		super.onResume();
		databind(BindingMode.AUTO);
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.activity_list_fragment;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public class ListAdapter extends ArrayAdapter<Activity> {

		private ListAdapter(List<Activity> items) {
			super(getSherlockActivity(), 0, items);
		}

		@Override
		public int getCount() {
			return mActivities.size() + (mMore ? 1 : 0);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (position == mActivities.size()) {
				return mLoading;
			}

			View view = convertView;
			final ViewHolder holder;
			final Activity activity = mActivities.get(position);

			if (view == null || view.findViewById(R.id.animator_more) != null) {
				view = LayoutInflater.from(getSherlockActivity()).inflate(R.layout.temp_listitem_activity, null);
				holder = new ViewHolder();

				holder.name = (TextView) view.findViewById(R.id.name);
				holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
				holder.type = (TextView) view.findViewById(R.id.type);
				holder.description = (TextView) view.findViewById(R.id.description);
				holder.date = (TextView) view.findViewById(R.id.timesince);
				holder.byPhotoView = (AirImageView) view.findViewById(R.id.photo_user);

				holder.shortcutOne = view.findViewById(R.id.shortcut_one);
				if (holder.shortcutOne != null) {
					holder.photoViewOne = (AirImageView) holder.shortcutOne.findViewById(R.id.photo_one);
					holder.nameOne = (TextView) holder.shortcutOne.findViewById(R.id.name_one);
				}

				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
			}

			if (activity != null) {
				holder.data = activity;

				UI.setVisibility(holder.byPhotoView, View.INVISIBLE);
				if (holder.byPhotoView != null) {
					if (activity.photoBy != null) {
						if (holder.byPhotoView.getPhoto() == null || !activity.photoBy.getUri().equals(holder.byPhotoView.getPhoto().getUri())) {
							UI.drawPhoto(holder.byPhotoView, activity.photoBy);
						}
						UI.setVisibility(holder.byPhotoView, View.VISIBLE);
					}
				}

				UI.setVisibility(holder.name, View.GONE);
				if (holder.name != null && activity.title != null && activity.title.length() > 0) {
					holder.name.setText(activity.title);
					UI.setVisibility(holder.name, View.VISIBLE);
				}

				UI.setVisibility(holder.subtitle, View.GONE);
				if (holder.subtitle != null && activity.subtitle != null && !activity.subtitle.equals("")) {
					holder.subtitle.setText(activity.subtitle);
					UI.setVisibility(holder.subtitle, View.VISIBLE);
				}

				/* Shortcuts */

				UI.setVisibility(holder.shortcutOne, View.GONE);
				if (holder.shortcutOne != null) {
					if (activity.photoOne != null) {
						if (holder.photoViewOne.getPhoto() == null || !activity.photoOne.getUri().equals(holder.photoViewOne.getPhoto().getUri())) {
							UI.drawPhoto(holder.photoViewOne, activity.photoOne);
						}
						holder.nameOne.setText(activity.photoOne.name);
						holder.photoViewOne.setTag(activity.photoOne.shortcut);
						UI.setVisibility(holder.shortcutOne, View.VISIBLE);
					}
				}

				UI.setVisibility(holder.description, View.GONE);
				if (holder.description != null && !TextUtils.isEmpty(activity.description)) {
					holder.description.setMaxLines(5);
					holder.description.setText(activity.description);
					UI.setVisibility(holder.description, View.VISIBLE);
				}

				UI.setVisibility(holder.date, View.GONE);
				if (holder.date != null && activity.activityDate != null) {
					holder.date.setText(DateTime.interval(activity.activityDate.longValue(), DateTime.nowDate().getTime(), IntervalContext.PAST));
					UI.setVisibility(holder.date, View.VISIBLE);
				}

				view.setClickable(true);
				view.setOnClickListener(mClickListener);
			}
			return view;
		}

		@Override
		public Activity getItem(int position) {
			return mActivities.get(position);
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

	public static class ViewHolder {

		public TextView		name;
		public AirImageView	byPhotoView;

		public TextView		subtitle;
		public TextView		description;
		public TextView		type;
		public TextView		date;

		@SuppressWarnings("ucd")
		public String		photoUri;		// Used for verification after fetching image
		public Object		data;			// object binding to

		public View			shortcutOne;
		public AirImageView	photoViewOne;
		public TextView		nameOne;

	}

}