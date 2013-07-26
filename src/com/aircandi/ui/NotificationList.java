package com.aircandi.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
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
import com.aircandi.applications.Comments;
import com.aircandi.beta.R;
import com.aircandi.components.DatabaseHelper;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.MessageEvent;
import com.aircandi.components.NotificationManager;
import com.aircandi.components.NotificationTable;
import com.aircandi.components.NotificationsContentProvider;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ObjectType;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.ui.base.BaseBrowse;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

public class NotificationList extends BaseBrowse implements LoaderManager.LoaderCallbacks<Cursor> {

	protected ListView			mListView;
	protected OnClickListener	mClickListener;
	private ListAdapter			mAdapter;

	protected Integer getListItemResId(String notificationType) {
		Integer itemResId = R.layout.temp_listitem_news;
		return itemResId;
	}

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mListView = (ListView) findViewById(R.id.list);

		mClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				final AirNotification notification = (AirNotification) ((ViewHolder) v.getTag()).data;
				
				/* Build intent that can be used in association with the notification */
				if (notification.entity != null) {
					if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
						notification.intent = Comments.viewForGetIntent(NotificationList.this, notification.entity.toId, Constants.TYPE_LINK_COMMENT, null);
					}
					else {
						Class<?> clazz = BaseEntityForm.viewFormBySchema(notification.entity.schema);
						IntentBuilder intentBuilder = new IntentBuilder(NotificationList.this, clazz)
								.setEntityId(notification.entity.id)
								.setEntitySchema(notification.entity.schema)
								.setForceRefresh(true);
						notification.intent = intentBuilder.create();
					}
				}
				
				Routing.intent(NotificationList.this, notification.intent);
			}
		};
	}

	@Override
	protected void databind(final Boolean refresh) {
		NotificationManager.getInstance().setNewCount(0);
		//		if (mAdapter == null) {
		getSupportLoaderManager().initLoader(0, null, this);
		mAdapter = new ListAdapter(this, null, false);
		mListView.setAdapter(mAdapter);
		//		}
		mBusyManager.hideBusy();
	}

	@SuppressWarnings("ucd")
	public void doRefresh() {
		/* Called from AircandiCommon */
		databind(true);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = { NotificationTable.COLUMN_ID, NotificationTable.COLUMN_SENT_DATE, NotificationTable.COLUMN_OBJECT };
		CursorLoader cursorLoader = new CursorLoader(this, NotificationsContentProvider.CONTENT_URI, projection, null, null, NotificationTable.COLUMN_SENT_DATE + " desc");
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (mAdapter != null && cursor != null) {
			if (cursor.getCount() == 0) {
				findViewById(R.id.message).setVisibility(View.VISIBLE);
			}
			else {
				findViewById(R.id.message).setVisibility(View.GONE);
			}
			mAdapter.swapCursor(cursor);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		if (mAdapter != null) {
			mAdapter.swapCursor(null);
		}

	}

	@Override
	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
		super.onMessage(event);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				databind(true);
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public void clearNotifications() {

		final AlertDialog dialog = Dialogs.alertDialog(null
				, getResources().getString(R.string.alert_delete_title)
				, getResources().getString(R.string.alert_notifications_delete_message)
				, null
				, this
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
							databind(true);
							findViewById(R.id.message).setVisibility(View.VISIBLE);
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);

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
	protected void onResume() {
		super.onResume();
		if (!isFinishing()) {
			//databind(true);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.notification_list;
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
			View view = LayoutInflater.from(NotificationList.this).inflate(getListItemResId(null), null);
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
				holder.photoView = (AirImageView) view.findViewById(R.id.photo);
				holder.photoUserView = (AirImageView) view.findViewById(R.id.photo_user);
				view.setTag(holder);
			}

			int sentDateIndex = cursor.getColumnIndexOrThrow(NotificationTable.COLUMN_SENT_DATE);
			int objectIndex = cursor.getColumnIndexOrThrow(NotificationTable.COLUMN_OBJECT);

			@SuppressWarnings("unused")
			Long sentDate = cursor.getLong(sentDateIndex);
			String jsonObject = cursor.getString(objectIndex);
			AirNotification notification = (AirNotification) HttpService.jsonToObject(jsonObject, ObjectType.AirNotification);
			
			/* Decorate again in case the logic has changed since the notification was stored */
			NotificationManager.getInstance().decorateNotification(notification);

			if (notification != null) {
				Logger.d(this, "Adapter getView: " + notification.title);
				holder.data = notification;

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

				if (holder.photoView != null) {
					holder.photoView.setTag(notification);
					if (notification.entity.photo == null && !notification.entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
						holder.photoView.setVisibility(View.GONE);
					}
					else {
						UI.drawPhoto(holder.photoView, notification.entity.getPhoto());
					}
				}

				UI.setVisibility(holder.description, View.GONE);
				if (holder.description != null && notification.entity.description != null && notification.entity.description.length() > 0) {
					holder.description.setMaxLines(5);
					holder.description.setText(notification.entity.description);
					UI.setVisibility(holder.description, View.VISIBLE);
				}

				UI.setVisibility(holder.date, View.GONE);
				if (holder.date != null && notification.sentDate != null) {
					holder.date.setText(DateTime.timeSince(notification.sentDate.longValue(), DateTime.nowDate().getTime()));
					UI.setVisibility(holder.date, View.VISIBLE);
				}

				if (holder.photoUserView != null) {
					holder.photoUserView.setTag(notification);
					UI.drawPhoto(holder.photoUserView, notification.user.getPhoto());
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
		public AirImageView	photoView;
		public AirImageView	photoUserView;
		public TextView		subtitle;
		public TextView		description;
		public TextView		type;
		public TextView		date;

		@SuppressWarnings("ucd")
		public String		photoUri;		// Used for verification after fetching image
		public Object		data;			// Object binding to
	}

}