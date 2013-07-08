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

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.EntityManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ExcludeNulls;
import com.aircandi.service.HttpService.ObjectType;
import com.aircandi.service.HttpService.UseAnnotations;
import com.aircandi.service.objects.Entity;
import com.aircandi.ui.widgets.BounceListView;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;

public abstract class BaseEntityListEdit extends BaseEntityEdit {

	protected BounceListView	mList;
	protected TextView			mMessage;
	protected List<Entity>		mEntities	= new ArrayList<Entity>();
	protected String			mParentId;
	protected Entity			mParent;
	protected Entity			mEntityEditing;
	protected List<String>		mJsonEntitiesOriginal;

	@Override
	protected void initialize() {

		/* We use this to access the source suggestions */
		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			final List<String> jsonEntities = extras.getStringArrayList(Constants.EXTRA_ENTITIES);
			if (jsonEntities != null) {
				mJsonEntitiesOriginal = jsonEntities;
				for (String jsonEntity : jsonEntities) {
					Entity entity = (Entity) HttpService.jsonToObject(jsonEntity, ObjectType.Entity);
					mEntities.add(entity);
				}
			}
			mParentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			if (mParentId != null) {
				mParent = EntityManager.getEntity(mParentId);
			}
		}

		mMessage = (TextView) findViewById(R.id.message);
		mList = (BounceListView) findViewById(R.id.list);
	}

	@Override
	protected void bind() {
		/*
		 * Before entities are customized, they have no position and are
		 * sorted by the modified date on the link. Once entity customization
		 * is saved to the service, the position field has been set on the set of
		 * entities.
		 */
		Collections.sort(mEntities, new Entity.SortEntitiesByPosition());

		if (mEntities.size() == 0) {
			mCommon.hideBusy(true); // visible by default
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
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

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
				confirmEntityDelete();
				return;
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onAddButtonClick(View view) {
		final IntentBuilder intentBuilder = new IntentBuilder(this, BaseEntityEdit.editFormBySchema(mEntityEditing.schema))
				.setEntitySchema(mCommon.mEntitySchema)
				.setEntityParentId(mParentId);

		Intent intent = intentBuilder.create();
		intent.putExtra(Constants.EXTRA_SKIP_SAVE, true);
		startActivityForResult(intent, Constants.ACTIVITY_ENTITY_INSERT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@SuppressWarnings("ucd")
	public void onMoveUpButtonClick(View view) {
		for (int i = mEntities.size() - 1; i >= 0; i--) {
			if (mEntities.get(i).checked) {
				mEntities.get(i).position = mEntities.get(i).position.intValue() - 2;
			}
		}
		Collections.sort(mEntities, new Entity.SortEntitiesByPosition());
		Integer position = 0;
		for (Entity entity : mEntities) {
			entity.position = position;
			position++;
		}
		mList.invalidateViews();
	}

	@SuppressWarnings("ucd")
	public void onMoveDownButtonClick(View view) {
		for (int i = mEntities.size() - 1; i >= 0; i--) {
			if (mEntities.get(i).checked) {
				mEntities.get(i).position = mEntities.get(i).position.intValue() + 2;
			}
		}
		Collections.sort(mEntities, new Entity.SortEntitiesByPosition());
		Integer position = 0;
		for (Entity entity : mEntities) {
			entity.position = position;
			position++;
		}
		mList.invalidateViews();
	}

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {
		final CheckBox check = (CheckBox) ((View) view.getParent()).findViewById(R.id.checked);
		mEntityEditing = (Entity) check.getTag();

		final IntentBuilder intentBuilder = new IntentBuilder(this, BaseEntityEdit.editFormBySchema(mEntityEditing.schema)).setEntity(mEntityEditing);
		Intent intent = intentBuilder.create();
		intent.putExtra(Constants.EXTRA_SKIP_SAVE, true);

		startActivityForResult(intent, Constants.ACTIVITY_ENTITY_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
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

	private void confirmEntityDelete() {

		/* How many are we deleting? */
		Integer deleteCount = 0;
		for (int i = mEntities.size() - 1; i >= 0; i--) {
			if (mEntities.get(i).checked) {
				deleteCount++;
			}
		}

		final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final ViewGroup customView = (ViewGroup) inflater.inflate(R.layout.temp_delete_sources, null);
		final TextView message = (TextView) customView.findViewById(R.id.message);
		final LinearLayout list = (LinearLayout) customView.findViewById(R.id.list);
		for (Entity entity : mEntities) {
			if (entity.checked) {
				View sourceView = inflater.inflate(R.layout.temp_listitem_delete_sources, null);
				WebImageView image = (WebImageView) sourceView.findViewById(R.id.photo);
				TextView name = (TextView) sourceView.findViewById(R.id.name);
				if (entity.name != null) {
					name.setText(entity.name);
				}
				image.setTag(entity);
				final String photoUri = entity.getPhotoUri();
				BitmapRequest bitmapRequest = new BitmapRequest(photoUri, image.getImageView());
				bitmapRequest.setImageSize(image.getSizeHint());
				bitmapRequest.setImageRequestor(image.getImageView());

				BitmapManager.getInstance().masterFetch(bitmapRequest);
				list.addView(sourceView);
			}
		}

		message.setText((deleteCount == 1)
				? R.string.alert_source_delete_message_single
				: R.string.alert_source_delete_message_multiple);

		final AlertDialog dialog = AircandiCommon.showAlertDialog(null
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
							mList.invalidateViews();
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	private void confirmDirtyExit() {
		final AlertDialog dialog = AircandiCommon.showAlertDialog(null
				, getResources().getString(R.string.alert_sources_dirty_exit_title)
				, getResources().getString(R.string.alert_sources_dirty_exit_message)
				, null
				, this
				, R.string.alert_dirty_save
				, android.R.string.cancel
				, R.string.alert_dirty_discard
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							gatherAndExit();
						}
						else if (which == Dialog.BUTTON_NEUTRAL) {
							setResult(Activity.RESULT_CANCELED);
							finish();
							AnimUtils.doOverridePendingTransition(BaseEntityListEdit.this, TransitionType.FormToPage);
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	private void gatherAndExit() {
		final Intent intent = new Intent();
		final List<String> jsonEntities = new ArrayList<String>();

		for (Entity entity : mEntities) {
			jsonEntities.add(HttpService.objectToJson(entity, UseAnnotations.False, ExcludeNulls.True));
		}

		intent.putStringArrayListExtra(Constants.EXTRA_ENTITIES, (ArrayList<String>) jsonEntities);
		setResult(Activity.RESULT_OK, intent);
		finish();
		AnimUtils.doOverridePendingTransition(this, TransitionType.FormToPage);
	}

	@Override
	protected Boolean isDirty() {

		/* Gather */

		final List<String> jsonEntities = new ArrayList<String>();
		for (Entity entity : mEntities) {
			jsonEntities.add(HttpService.objectToJson(entity, UseAnnotations.False, ExcludeNulls.True));
		}

		if (mJsonEntitiesOriginal == null) {
			if (jsonEntities.size() > 0) {
				return true;
			}
		}
		else {

			if (jsonEntities.size() != mJsonEntitiesOriginal.size()) {
				return true;
			}

			int position = 0;
			for (String jsonEntity : jsonEntities) {
				if (!jsonEntity.equals(mJsonEntitiesOriginal.get(position))) {
					return true;
				}
				position++;
			}
		}
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	private void scrollToBottom() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mList.setSelection(mList.getAdapter().getCount() - 1);
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.accept) {
			gatherAndExit();
			return true;
		}
		else if (item.getItemId() == R.id.cancel) {
			if (isDirty()) {
				confirmDirtyExit();
			}
			else {
				setResult(Activity.RESULT_CANCELED);
				finish();
				AnimUtils.doOverridePendingTransition(this, TransitionType.FormToPage);
			}
			return true;
		}

		/* In case we add general menu items later */
		mCommon.doOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.entity_list_edit;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	protected abstract static class EntityListAdapter extends ArrayAdapter<Entity> implements Filterable {

		protected final LayoutInflater	mInflater;
		protected Integer				mItemLayoutId;
		protected final List<Entity>	mListItems;

		public EntityListAdapter(Context context, List<Entity> entities, Integer itemLayoutId) {
			super(context, 0, entities);

			mListItems = entities;
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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
				view = mInflater.inflate(mItemLayoutId, null);
				holder = new ViewHolder();
				holder.photo = (WebImageView) view.findViewById(R.id.photo);
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

				if (holder.photo != null) {
					holder.photo.setTag(entity);
					/*
					 * The WebImageView sets the current bitmap ref being held
					 * by the internal image view to null before doing the work
					 * to satisfy the new request.
					 */
					if (entity.photo != null && entity.photo.hasBitmap()) {
						ImageUtils.showImageInImageView(entity.photo.getBitmap(), holder.photo.getImageView(), true, AnimUtils.fadeInMedium());
					}
					else {
						final String photoUri = entity.getPhotoUri();

						/* Don't do anything if the image is already set to the one we want */
						if (holder.photo.getImageUri() == null || !holder.photo.getImageUri().equals(photoUri)) {

							final BitmapRequestBuilder builder = new BitmapRequestBuilder(holder.photo).setImageUri(photoUri);
							final BitmapRequest imageRequest = builder.create();
							holder.photo.setBitmapRequest(imageRequest);
						}
					}

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
			private WebImageView	photo;
			private TextView		name;
			private TextView		subtitle;
			private TextView		description;
			private CheckBox		checked;
		}
	}
}