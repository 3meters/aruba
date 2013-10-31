package com.aircandi.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.applications.Comments;
import com.aircandi.components.DatabaseHelper;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.MessagingManager;
import com.aircandi.components.MessagingManager.Tag;
import com.aircandi.components.NotificationTable;
import com.aircandi.components.NotificationsContentProvider;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.DateTime.IntervalContext;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Notifications;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;

public class NewsFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	protected ListView			mListView;
	protected TextView			mMessage;
	protected Integer			mLastViewedPosition;
	protected Integer			mTopOffset;
	protected Integer			mSavedScrollPositionX;
	protected Integer			mSavedScrollPositionY;
	protected OnClickListener	mClickListener;
	private ListAdapter			mAdapter;
	private Handler				mHandler	= new Handler();
	private static final int	LOADER_ID	= 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				final AirNotification notification = (AirNotification) ((ViewHolder) v.getTag()).data;

				/* Build intent that can be used in association with the notification */
				if (notification.entity != null) {
					if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
						notification.intent = Comments.viewForGetIntent(getSherlockActivity(), notification.toEntity.id, Constants.TYPE_LINK_CONTENT, null,
								null);
					}
					else {
						Class<?> clazz = BaseEntityForm.viewFormBySchema(notification.entity.schema);
						IntentBuilder intentBuilder = new IntentBuilder(getSherlockActivity(), clazz)
								.setEntityId(notification.entity.id)
								.setEntitySchema(notification.entity.schema)
								.setForceRefresh(true);
						notification.intent = intentBuilder.create();
					}
				}

				Routing.intent(getSherlockActivity(), notification.intent);
			}
		};
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view == null) return view;
		
		mListView = (ListView) view.findViewById(R.id.list);
		mMessage = (TextView) view.findViewById(R.id.message);
		return view;
	}

	@Override
	public void databind(BindingMode mode) {
		/*
		 * This databinding is always against the local sqlite database.
		 */
		Logger.d(this, "databinding...");

		showBusy();

		/* Usability Hack: Slight delay so the busy indicator has a chance to run */
		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				MessagingManager.getInstance().setNewCount(0);
				if (mAdapter == null) {
					mAdapter = new ListAdapter(getSherlockActivity(), null, false);
					getSherlockActivity().getSupportLoaderManager().initLoader(LOADER_ID, null, NewsFragment.this);
					mListView.setAdapter(mAdapter);
				}
				else {
					getSherlockActivity().getSupportLoaderManager().restartLoader(LOADER_ID, null, NewsFragment.this);
				}
				setListPosition();
			}
		}, 200);

	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onRefresh() {
		saveListPosition();
		super.onRefresh();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = { NotificationTable.COLUMN_ID, NotificationTable.COLUMN_SENT_DATE, NotificationTable.COLUMN_OBJECT };
		CursorLoader cursorLoader = new CursorLoader(getSherlockActivity()
				, NotificationsContentProvider.CONTENT_URI
				, projection
				, null
				, null
				, NotificationTable.COLUMN_SENT_DATE + " desc");
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		/*
		 * This gets called if the content provider has an update.
		 */
		if (mAdapter != null && cursor != null) {
			showMessage(cursor.getCount() == 0);
			mAdapter.swapCursor(cursor);
		}

		if (mSavedScrollPositionY != null) {
			mListView.post(new Runnable() {
				@Override
				public void run() {
					mListView.scrollTo(mSavedScrollPositionX, mSavedScrollPositionY);
				}
			});
		}

		/* Clear notifications from status bar because user is viewing them */

		MessagingManager.getInstance().cancelNotification(Tag.INSERT);
		MessagingManager.getInstance().cancelNotification(Tag.UPDATE);

		hideBusy();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		if (mAdapter != null) {
			mAdapter.swapCursor(null);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	private void showMessage(Boolean visible) {
		if (mMessage != null) {
			mMessage.setText(R.string.list_notifications_empty);
			mMessage.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	public void clearNotifications() {

		final AlertDialog dialog = Dialogs.alertDialog(null
				, getResources().getString(R.string.alert_notifications_delete_title)
				, getResources().getString(R.string.alert_notifications_delete_message)
				, null
				, getSherlockActivity()
				, android.R.string.ok
				, android.R.string.cancel
				, null
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							SQLiteDatabase database = DatabaseHelper.getInstance().getWritableDatabase();
							Integer deleteCount = database.delete(NotificationTable.TABLE_NOTIFICATIONS, "1", null);
							UI.showToastNotification("Items deleted: " + String.valueOf(deleteCount), Toast.LENGTH_SHORT);
							getSherlockActivity().getSupportLoaderManager().restartLoader(LOADER_ID, null, NewsFragment.this);
							getView().findViewById(R.id.message).setVisibility(View.VISIBLE);
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);

	}

	protected Integer getListItemResId(String notificationType) {
		Integer itemResId = R.layout.temp_listitem_news;
		return itemResId;
	}

	private void saveListPosition() {
		mLastViewedPosition = mListView.getFirstVisiblePosition();
		View view = mListView.getChildAt(0);
		mTopOffset = (view == null) ? 0 : view.getTop();
	}

	private void setListPosition() {
		if (mLastViewedPosition != null) {
			mListView.setSelectionFromTop(mLastViewedPosition, mTopOffset);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*
		 * The action bar home/up action should open or close the drawer.
		 * ActionBarDrawerToggle will take care of this.
		 */
		if (item.getItemId() == R.id.delete_notifications) {
			clearNotifications();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

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
		return R.layout.notification_list_fragment;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public class ListAdapter extends CursorAdapter {

		public ListAdapter(Context context, Cursor cursor, boolean autoRequery) {
			super(context, cursor, autoRequery);
		}

		private int	mScrollState	= ScrollManager.SCROLL_STATE_IDLE;

		@Override
		public View newView(final Context context, Cursor cursor, ViewGroup parent) {
			View view = LayoutInflater.from(getSherlockActivity()).inflate(getListItemResId(null), null);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			ViewHolder holder = (ViewHolder) view.getTag();

			if (holder == null) {
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

			int sentDateIndex = cursor.getColumnIndexOrThrow(NotificationTable.COLUMN_SENT_DATE);
			int objectIndex = cursor.getColumnIndexOrThrow(NotificationTable.COLUMN_OBJECT);

			@SuppressWarnings("unused")
			Long sentDate = cursor.getLong(sentDateIndex);
			String jsonObject = cursor.getString(objectIndex);
			AirNotification notification = (AirNotification) Json.jsonToObject(jsonObject, Json.ObjectType.AIR_NOTIFICATION);

			/* Decorate again in case the logic has changed since the notification was stored */
			Notifications.decorate(notification);

			if (notification != null) {
				holder.data = notification;

				UI.setVisibility(holder.byPhotoView, View.INVISIBLE);
				if (holder.byPhotoView != null) {
					if (notification.photoBy != null) {
						if (holder.byPhotoView.getPhoto() == null || !notification.photoBy.getUri().equals(holder.byPhotoView.getPhoto().getUri())) {
							UI.drawPhoto(holder.byPhotoView, notification.photoBy);
						}
						UI.setVisibility(holder.byPhotoView, View.VISIBLE);
					}
				}

				UI.setVisibility(holder.name, View.GONE);
				if (holder.name != null && notification.title != null && notification.title.length() > 0) {
					holder.name.setText(notification.title);
					UI.setVisibility(holder.name, View.VISIBLE);
				}

				UI.setVisibility(holder.subtitle, View.GONE);
				if (holder.subtitle != null && notification.subtitle != null && !notification.subtitle.equals("")) {
					holder.subtitle.setText(notification.subtitle);
					UI.setVisibility(holder.subtitle, View.VISIBLE);
				}

				UI.setVisibility(holder.type, View.GONE);
				if (holder.type != null && notification.type != null && notification.type.length() > 0) {
					holder.type.setText(notification.type);
					UI.setVisibility(holder.type, View.VISIBLE);
				}

				/* Shortcuts */

				UI.setVisibility(holder.shortcutOne, View.GONE);
				if (holder.shortcutOne != null) {
					if (notification.photoOne != null) {
						if (holder.photoViewOne.getPhoto() == null || !notification.photoOne.getUri().equals(holder.photoViewOne.getPhoto().getUri())) {
							UI.drawPhoto(holder.photoViewOne, notification.photoOne);
						}
						holder.nameOne.setText(notification.photoOne.name);
						holder.photoViewOne.setTag(notification.photoOne.shortcut);
						UI.setVisibility(holder.shortcutOne, View.VISIBLE);
					}
				}

				UI.setVisibility(holder.description, View.GONE);
				if (holder.description != null && !TextUtils.isEmpty(notification.description)) {
					holder.description.setMaxLines(5);
					holder.description.setText(notification.description);
					UI.setVisibility(holder.description, View.VISIBLE);
				}

				UI.setVisibility(holder.date, View.GONE);
				if (holder.date != null && notification.sentDate != null) {
					holder.date.setText(DateTime.interval(notification.sentDate.longValue(), DateTime.nowDate().getTime(), IntervalContext.PAST));
					UI.setVisibility(holder.date, View.VISIBLE);
				}

				view.setClickable(true);
				view.setOnClickListener(mClickListener);
			}
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