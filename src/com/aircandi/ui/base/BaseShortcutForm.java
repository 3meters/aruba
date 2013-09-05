package com.aircandi.ui.base;

import java.util.Collections;
import java.util.List;

import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.applications.Pictures;
import com.aircandi.applications.Places;
import com.aircandi.applications.Users;
import com.aircandi.components.EntityManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.utilities.Routing;

@SuppressWarnings("ucd")
public abstract class BaseShortcutForm extends BaseEntityForm {

	protected String		mShortcutType;
	protected LinkProfile	mLinkProfiles;

	@Override
	public void databind() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy();
				mBusyManager.startBodyBusyIndicator();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("GetUser");

				Entity entity = EntityManager.getEntity(mEntityId);
				Boolean refresh = mRefreshFromService;
				if (entity == null || !entity.shortcuts) {
					refresh = true;
				}
				mRefreshFromService = false;

				LinkOptions options =LinkOptions.getDefault(mLinkProfiles); 
				final ModelResult result = EntityManager.getInstance().getEntity(mEntityId, refresh, options);

				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					if (result.data != null) {
						mEntity = (Entity) result.data;
						synchronize();
						setActivityTitle(mEntity.name);
						draw();
					}
				}
				else {
					Routing.serviceError(BaseShortcutForm.this, result.serviceResponse);
				}
				hideBusy();
				mBusyManager.stopBodyBusyIndicator();
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void draw() {

		/* Clear shortcut holder */
		((ViewGroup) findViewById(R.id.shortcut_holder)).removeAllViews();

		if (mShortcutType.equals(Constants.TYPE_LINK_CREATE)) {

			/* Shortcuts for place entities created by user */
			ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PLACE, Direction.out, false, false);
			settings.appClass = Places.class;
			List<Shortcut> shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, null);
			if (shortcuts.size() > 0) {
				Collections.sort(shortcuts, new Shortcut.SortByModifiedDate());
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_places_created
						, R.string.section_places_more
						, mResources.getInteger(R.integer.shortcuts_flow_limit)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}

			/* Shortcuts for post entities created by user */
			settings = new ShortcutSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PICTURE, Direction.out, false, false);
			settings.appClass = Pictures.class;
			shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, null);
			if (shortcuts.size() > 0) {
				Collections.sort(shortcuts, new Shortcut.SortByModifiedDate());
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_pictures_created
						, R.string.section_pictures_more
						, mResources.getInteger(R.integer.shortcuts_flow_limit)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}
		}

		else if (mShortcutType.equals(Constants.TYPE_LINK_WATCH)) {

			/* Watching places */
			ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, Direction.out, false, false);
			settings.appClass = Places.class;
			List<Shortcut> shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, null);
			if (shortcuts.size() > 0) {
				Collections.sort(shortcuts, new Shortcut.SortByModifiedDate());
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_places_watching
						, R.string.section_places_more
						, mResources.getInteger(R.integer.shortcuts_flow_limit)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}

			/* Watching posts */
			settings = new ShortcutSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PICTURE, Direction.out, false, false);
			settings.appClass = Pictures.class;
			shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, null);
			if (shortcuts.size() > 0) {
				Collections.sort(shortcuts, new Shortcut.SortByModifiedDate());
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_pictures_watching
						, R.string.section_pictures_more
						, mResources.getInteger(R.integer.shortcuts_flow_limit)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}

			/* Watching users */
			settings = new ShortcutSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, Direction.out, false, false);
			settings.appClass = Users.class;
			shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, null);
			if (shortcuts.size() > 0) {
				Collections.sort(shortcuts, new Shortcut.SortByModifiedDate());
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_users_watching
						, R.string.section_users_more
						, mResources.getInteger(R.integer.shortcuts_flow_limit)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}
		}

		drawButtons();

		if (mScrollView != null) {
			mScrollView.setVisibility(View.VISIBLE);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

}