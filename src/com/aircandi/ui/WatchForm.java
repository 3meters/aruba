package com.aircandi.ui;

import java.util.Collections;
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.DefaultType;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.base.BaseEntityList.ListMode;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Routing;

@SuppressWarnings("ucd")
public class WatchForm extends BaseEntityForm {

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mActionBar.setIcon(R.drawable.img_watch);
	}

	@Override
	protected void bind(final Boolean refreshProposed) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("GetWatching");

				Entity entity = EntityManager.getEntity(mEntityId);
				Boolean refresh = refreshProposed;
				if (entity == null || !entity.shortcuts) {
					refresh = true;
				}

				ModelResult result = EntityManager.getInstance().getEntity(mEntityId
						, refresh
						, LinkOptions.getDefault(DefaultType.LinksUserWatching));

				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					if (result.data != null) {
						mEntity = (Entity) result.data;
						mEntityModelRefreshDate = ProximityManager.getInstance().getLastBeaconLoadDate();
						mEntityModelActivityDate = EntityManager.getEntityCache().getLastActivityDate();
						mActionBar.setTitle(mEntity.name);
						drawForm();
					}
				}
				else {
					Routing.serviceError(WatchForm.this, result.serviceResponse);
				}
				mBusyManager.hideBusy();
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------
	
	@Override
	public void onMoreButtonClick(View view) {

		ShortcutSettings settings = (ShortcutSettings) view.getTag();
		IntentBuilder intentBuilder = null;

		intentBuilder = new IntentBuilder(this, EntityList.class)
				.setListMode(ListMode.EntitiesWatchedByUser)
				.setEntitySchema(settings.targetSchema)
				.setEntityId(mEntityId);

		Intent intent = intentBuilder.create();
		startActivity(intent);
		Animate.doOverridePendingTransition(this, TransitionType.PageToPage);
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------
	@Override
	protected void draw() {
		drawForm();
	}

	private void drawForm() {

		/* Clear shortcut holder */
		((ViewGroup) findViewById(R.id.shortcut_holder)).removeAllViews();

		/* Watching places */
		ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, Direction.out, false);
		List<Shortcut> shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, false);
		if (shortcuts.size() > 0) {
			Collections.sort(shortcuts, new Shortcut.SortByPosition());
			drawShortcuts(shortcuts
					, settings
					, R.string.candi_section_shortcuts_place
					, R.string.candi_section_links_more
					, mResources.getInteger(R.integer.candi_flow_limit)
					, R.layout.temp_place_switchboard_item);
		}

		/* Watching users */
		settings = new ShortcutSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, Direction.out, false);
		shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, false);
		if (shortcuts.size() > 0) {
			Collections.sort(shortcuts, new Shortcut.SortByPosition());
			drawShortcuts(shortcuts
					, settings
					, null
					, null
					, mResources.getInteger(R.integer.candi_flow_limit)
					, R.layout.temp_place_switchboard_item);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.watch_form;
	}
}