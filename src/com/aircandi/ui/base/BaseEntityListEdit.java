package com.aircandi.ui.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ExcludeNulls;
import com.aircandi.service.HttpService.ObjectType;
import com.aircandi.service.HttpService.UseAnnotations;
import com.aircandi.service.objects.Entity;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.BounceListView;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;

public abstract class BaseEntityListEdit extends BaseEdit implements IList {

	protected BounceListView	mList;
	protected TextView			mMessage;
	protected Integer			mListItemResId;

	protected Entity			mParent;
	protected Entity			mEntityEditing;

	/* Inputs */
	protected String			mEntityId;
	protected List<Entity>		mEntities	= new ArrayList<Entity>();
	protected String			mListSchema;
	protected ArrayAdapter		mAdapter;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			final List<String> jsonEntities = extras.getStringArrayList(Constants.EXTRA_ENTITIES);
			if (jsonEntities != null) {
				for (String jsonEntity : jsonEntities) {
					Entity entity = (Entity) HttpService.jsonToObject(jsonEntity, ObjectType.Entity);
					mEntities.add(entity);
				}
			}
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mListSchema = extras.getString(Constants.EXTRA_LIST_LINK_SCHEMA);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mMessage = (TextView) findViewById(R.id.message);
		mList = (BounceListView) findViewById(R.id.list);
	}

	@Override
	public void bind(BindingMode mode) {
		/*
		 * Before entities are customized, they have no position and are
		 * sorted by the modified date on the link. Once entity customization
		 * is saved to the service, the position field has been set on the set of
		 * entities.
		 */
		if (mAdapter == null) {
			Collections.sort(mEntities, new Entity.SortByPositionModifiedDate());

			if (mEntities.size() == 0) {
				mMessage.setText(R.string.sources_builder_empty);
				mMessage.setVisibility(View.VISIBLE);
			}
			else {
				mMessage.setVisibility(View.GONE);
				Integer position = 0;
				for (Entity entity : mEntities) {
					entity.checked = false;
					entity.position = position;
					position++;
				}
			}

			if (mEntityId != null) {
				mParent = EntityManager.getEntity(mEntityId);
			}
			mAdapter = getAdapter();
			mList.setAdapter(mAdapter);  // triggers draw

			hideBusy(); // visible by default
			mBusyManager.stopBodyBusyIndicator();
		}
	}

	protected ArrayAdapter getAdapter() {
		return null;
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onAccept() {
		if (isDirty()) {
			if (validate()) {

				/* Pull all the control values back into the entity object */
				final Intent intent = new Intent();
				final List<String> jsonEntities = new ArrayList<String>();

				for (Entity entity : mEntities) {
					jsonEntities.add(HttpService.objectToJson(entity, UseAnnotations.False, ExcludeNulls.True));
				}

				intent.putStringArrayListExtra(Constants.EXTRA_ENTITIES, (ArrayList<String>) jsonEntities);
				setResult(Activity.RESULT_OK, intent);
				finish();
				Animate.doOverridePendingTransition(this, TransitionType.FormToPage);
			}
		}
		else {
			finish();
			Animate.doOverridePendingTransition(BaseEntityListEdit.this, TransitionType.FormToPage);
		}
	}

	@SuppressWarnings("ucd")
	public void onCheckedClick(View view) {
		CheckBox check = (CheckBox) view.findViewById(R.id.checked);
		check.setChecked(!check.isChecked());
		final Entity entity = (Entity) check.getTag();
		entity.checked = check.isChecked();
	}

	@SuppressWarnings("ucd")
	public void onDeleteButtonClick(View view) {
		for (int i = mEntities.size() - 1; i >= 0; i--) {
			if (mEntities.get(i).checked) {
				confirmDelete();
				return;
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onAddButtonClick(View view) {
		onAdd();
	}

	@Override
	public void onAdd() {
		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_ENTITY_PARENT_ID, mEntityId);
		extras.putBoolean(Constants.EXTRA_SKIP_SAVE, true);
		Routing.route(this, Route.New, null, mListSchema, extras);
	}

	@SuppressWarnings("ucd")
	public void onMoveUpButtonClick(View view) {
		for (int i = mEntities.size() - 1; i >= 0; i--) {
			if (mEntities.get(i).checked) {
				mEntities.get(i).position = mEntities.get(i).position.intValue() - 2;
			}
		}
		Collections.sort(mEntities, new Entity.SortByPosition());
		Integer position = 0;
		for (Entity entity : mEntities) {
			entity.position = position;
			position++;
		}
		mDirty = true;
		mList.invalidateViews();
	}

	@SuppressWarnings("ucd")
	public void onMoveDownButtonClick(View view) {
		for (int i = mEntities.size() - 1; i >= 0; i--) {
			if (mEntities.get(i).checked) {
				mEntities.get(i).position = mEntities.get(i).position.intValue() + 2;
			}
		}
		Collections.sort(mEntities, new Entity.SortByPosition());
		Integer position = 0;
		for (Entity entity : mEntities) {
			entity.position = position;
			position++;
		}
		mDirty = true;
		mList.invalidateViews();
	}

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {
		final CheckBox check = (CheckBox) ((View) view.getParent()).findViewById(R.id.checked);
		mEntityEditing = (Entity) check.getTag();
		Bundle extras = new Bundle();
		extras.putBoolean(Constants.EXTRA_SKIP_SAVE, true);
		Routing.route(this, Route.Edit, mEntityEditing, null, null, extras);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ENTITY_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
					if (jsonEntity != null) {
						final Entity entityUpdated = (Entity) HttpService.jsonToObject(jsonEntity, ObjectType.Entity);
						if (entityUpdated != null) {
							for (Entity entity : mEntities) {
								if (entity.id.equals(entityUpdated.id)) {
									mEntities.set(mEntities.indexOf(entity), entityUpdated);
									break;
								}
							}
							mDirty = true;
							mList.invalidateViews();
						}
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_ENTITY_INSERT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
					if (jsonEntity != null) {
						final Entity entityNew = (Entity) HttpService.jsonToObject(jsonEntity, ObjectType.Entity);
						if (entityNew != null) {
							entityNew.checked = false;
							mEntities.add(entityNew);

							/* Rebuild the position numbering */
							Integer position = 0;
							for (Entity entity : mEntities) {
								entity.position = position;
								position++;
							}
							mDirty = true;
							mList.invalidateViews();
							scrollToBottom();
						}
					}
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	public void confirmDelete() {

		/* How many are we deleting? */
		Integer deleteCount = 0;
		for (int i = mEntities.size() - 1; i >= 0; i--) {
			if (mEntities.get(i).checked) {
				deleteCount++;
			}
		}

		final LayoutInflater inflater = LayoutInflater.from(this);
		final ViewGroup customView = (ViewGroup) inflater.inflate(R.layout.temp_delete_sources, null);
		final TextView message = (TextView) customView.findViewById(R.id.message);
		final LinearLayout list = (LinearLayout) customView.findViewById(R.id.list);
		for (Entity entity : mEntities) {
			if (entity.checked) {
				View sourceView = inflater.inflate(R.layout.temp_listitem_applink_delete, null);

				TextView name = (TextView) sourceView.findViewById(R.id.name);
				AirImageView photoView = (AirImageView) sourceView.findViewById(R.id.photo);

				if (entity.name != null) {
					name.setText(entity.name);
				}
				photoView.setTag(entity);
				UI.drawPhoto(photoView, entity.getPhoto());

				list.addView(sourceView);
			}
		}

		message.setText((deleteCount == 1)
				? R.string.alert_source_delete_message_single
				: R.string.alert_source_delete_message_multiple);

		final AlertDialog dialog = Dialogs.alertDialog(null
				, getResources().getString(R.string.alert_source_delete_title)
				, null
				, customView
				, this
				, android.R.string.ok
				, android.R.string.cancel
				, null
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							for (int i = mEntities.size() - 1; i >= 0; i--) {
								if (mEntities.get(i).checked) {
									mEntities.remove(i);
								}
							}
							mDirty = true;
							mList.invalidateViews();
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	private void scrollToBottom() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mList.setSelection(mList.getAdapter().getCount() - 1);
			}
		});
	}

	@Override
	protected void insert() {}

	@Override
	protected void update() {}

	@Override
	protected void delete() {}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		bind(BindingMode.auto);
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

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

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.entity_list_edit;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	protected abstract static class EntityListAdapter extends ArrayAdapter<Entity> implements Filterable {

		protected Integer				mItemLayoutId;
		protected final List<Entity>	mListItems;
		protected Context				mContext;

		public EntityListAdapter(Context context, List<Entity> entities, Integer itemLayoutId) {
			super(context, 0, entities);

			mListItems = entities;
			mContext = context;

			if (itemLayoutId != null) {
				mItemLayoutId = itemLayoutId;
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			final ViewHolder holder;
			final Entity entity = mListItems.get(position);

			if (view == null) {
				view = LayoutInflater.from(mContext).inflate(mItemLayoutId, null);
				holder = new ViewHolder();
				holder.photoView = (AirImageView) view.findViewById(R.id.photo);
				holder.name = (TextView) view.findViewById(R.id.name);
				holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
				holder.description = (TextView) view.findViewById(R.id.description);
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

				setVisibility(holder.checked, View.GONE);
				if (holder.checked != null && entity.checked != null) {
					holder.checked.setChecked(entity.checked);
					holder.checked.setTag(entity);
					setVisibility(holder.checked, View.VISIBLE);
				}

				setVisibility(holder.name, View.GONE);
				if (holder.name != null && entity.name != null && entity.name.length() > 0) {
					holder.name.setText(entity.name);
					setVisibility(holder.name, View.VISIBLE);
				}

				setVisibility(holder.subtitle, View.GONE);
				if (holder.subtitle != null) {
					if (entity.subtitle != null && entity.subtitle.length() > 0) {
						holder.subtitle.setText(entity.subtitle);
						setVisibility(holder.subtitle, View.VISIBLE);
					}
				}

				setVisibility(holder.description, View.GONE);
				if (holder.description != null) {
					if (entity.description != null && entity.description.length() > 0) {
						holder.description.setText(entity.description);
						setVisibility(holder.description, View.VISIBLE);
					}
				}

				if (holder.photoView != null) {
					holder.photoView.setTag(entity);
					holder.photoView.getImageView().setImageDrawable(null);
					UI.drawPhoto(holder.photoView, entity.getPhoto());
				}
			}
			return view;
		}

		@Override
		public Entity getItem(int position) {
			return mListItems.get(position);
		}

		@Override
		public int getCount() {
			return mListItems.size();
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		private static void setVisibility(View view, Integer visibility) {
			if (view != null) {
				view.setVisibility(visibility);
			}
		}

		private static class ViewHolder {
			private AirImageView	photoView;
			private TextView		name;
			private TextView		subtitle;
			private TextView		description;
			private CheckBox		checked;
		}
	}
}