package com.aircandi.ui;

import java.util.Collections;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.R;
import com.aircandi.applications.Applinks;
import com.aircandi.applications.Places;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.events.MessageEvent;
import com.aircandi.service.objects.Candigram;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.DefaultType;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

public class CandigramForm extends BaseEntityForm {

	Handler		mHandler	= new Handler();
	Runnable	mTimer;
	TextView	mActionInfo;

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mActionInfo = (TextView) findViewById(R.id.action_info);

		mTimer = new Runnable() {

			@Override
			public void run() {
				setActionText();
				mHandler.postDelayed(mTimer, 1000);
			}
		};
	}

	@Override
	public void onDatabind(final Boolean refreshProposed) {
		/*
		 * Navigation setup for action bar icon and title
		 */
		Logger.d(this, "Binding candi form");

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy();
				mBusyManager.startBodyBusyIndicator();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("GetEntity");

				Entity entity = EntityManager.getEntity(mEntityId);
				Boolean refresh = refreshProposed;
				if (entity == null || !entity.shortcuts) {
					refresh = true;
				}

				final ModelResult result = EntityManager.getInstance().getEntity(mEntityId
						, refresh
						, LinkOptions.getDefault(DefaultType.LinksForCandigram));
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
						setActivityTitle(mEntity.name);
						if (mMenuItemEdit != null) {
							mMenuItemEdit.setVisible(canUserEdit());
						}
						draw();
					}
					else {
						UI.showToastNotification("This item has been deleted", Toast.LENGTH_SHORT);
						finish();
					}
				}
				else {
					Routing.serviceError(CandigramForm.this, result.serviceResponse);
				}
				mBusyManager.hideBusy();
				mBusyManager.stopBodyBusyIndicator();
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
		/*
		 * Refresh the form because something new has been added to it
		 * like a comment or post.
		 */
		if (mEntityId.equals(event.notification.entity.toId)) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onRefresh();
				}
			});
		}
	}

	@SuppressWarnings("ucd")
	public void onNudgeButtonClick(View view) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("NudgeEntity");
				Tracker.sendEvent("ui_action", "nudge_entity", null, 0, Aircandi.getInstance().getUser());
				/*
				 * Call service routine to trigger a nudge
				 */
				return new ModelResult();
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;
				setSupportProgressBarIndeterminateVisibility(false);
				if (result.serviceResponse.responseCode == ResponseCode.Success) {}
				else {
					Routing.serviceError(CandigramForm.this, result.serviceResponse);
				}
			}
		}.execute();

	}

	@SuppressWarnings("ucd")
	public void onCaptureButtonClick(View view) {
		StringBuilder preamble = new StringBuilder(getString(R.string.alert_candigram_capture));
		Dialogs.alertDialogSimple(this, null, preamble.toString());
	}

	@Override
	public void onHelp() {
		Bundle extras = new Bundle();
		Integer helpResId = null;
		if (mEntity.type.equals(Constants.TYPE_APP_BOUNCE)) {
			helpResId = R.layout.candigram_bouncing_help;
		}
		else if (mEntity.type.equals(Constants.TYPE_APP_TOUR)) {
			helpResId = R.layout.candigram_touring_help;
		}
		extras.putInt(Constants.EXTRA_HELP_ID, helpResId);
		Routing.route(this, Route.Help, null, null, extras);
	}

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

	@Override
	protected void draw() {
		/*
		 * For now, we assume that the candi form isn't recycled.
		 * 
		 * We leave most of the views visible by default so they are visible in the layout editor.
		 * 
		 * - WebImageView primary image is visible by default
		 * - WebImageView child views are gone by default
		 * - Header views are visible by default
		 */
		final CandiView candiView = (CandiView) findViewById(R.id.candi_view);
		final AirImageView photoView = (AirImageView) findViewById(R.id.photo);
		final TextView name = (TextView) findViewById(R.id.name);
		final TextView subtitle = (TextView) findViewById(R.id.subtitle);

		final TextView description = (TextView) findViewById(R.id.description);
		final UserView user_one = (UserView) findViewById(R.id.user_one);
		final UserView user_two = (UserView) findViewById(R.id.user_two);

		if (candiView != null) {
			/*
			 * This is a place entity with a fancy image widget
			 */
			candiView.databind((Place) mEntity);
		}
		else {
			UI.setVisibility(photoView, View.GONE);
			if (photoView != null) {
				Photo photo = mEntity.getPhoto();
				UI.drawPhoto(photoView, photo);
				if (photo.usingDefault == null || !photo.usingDefault) {
					photoView.setClickable(true);
				}
				UI.setVisibility(photoView, View.VISIBLE);
			}

			name.setText(null);
			subtitle.setText(null);

			UI.setVisibility(name, View.GONE);
			if (name != null && mEntity.name != null && !mEntity.name.equals("")) {
				name.setText(Html.fromHtml(mEntity.name));
				UI.setVisibility(name, View.VISIBLE);
			}

			UI.setVisibility(subtitle, View.GONE);
			if (subtitle != null && mEntity.subtitle != null && !mEntity.subtitle.equals("")) {
				subtitle.setText(Html.fromHtml(mEntity.subtitle));
				UI.setVisibility(subtitle, View.VISIBLE);
			}
		}

		/* Set header */
		Candigram candigram = (Candigram) mEntity;
		if (candigram.type.equals(Constants.TYPE_APP_TOUR)) {
			/* Start updating the countdown in action info */
			mHandler.post(mTimer);
			((TextView) findViewById(R.id.header)).setText("touring " + getString(R.string.form_title_candigram));
		}
		else if (candigram.type.equals(Constants.TYPE_APP_BOUNCE)) {
			((TextView) findViewById(R.id.header)).setText("bouncing " + getString(R.string.form_title_candigram));
			UI.setVisibility(mActionInfo, View.GONE);
		}

		/* Primary candi image */

		description.setText(null);

		UI.setVisibility(findViewById(R.id.section_description), View.GONE);
		if (description != null && mEntity.description != null && !mEntity.description.equals("")) {
			description.setText(Html.fromHtml(mEntity.description));
			UI.setVisibility(findViewById(R.id.section_description), View.VISIBLE);
		}

		/* Stats */

		Count count = mEntity.getCount(Constants.TYPE_LINK_LIKE, Direction.in);
		if (count == null) count = new Count(Constants.TYPE_LINK_LIKE, 0);
		String label = this.getString(count.count.intValue() == 1 ? R.string.stats_label_likes : R.string.stats_label_likes_plural);
		((TextView) findViewById(R.id.like_stats)).setText(String.valueOf(count.count) + " " + label);

		count = mEntity.getCount(Constants.TYPE_LINK_WATCH, Direction.in);
		if (count == null) count = new Count(Constants.TYPE_LINK_WATCH, 0);
		label = this.getString(count.count.intValue() == 1 ? R.string.stats_label_watching : R.string.stats_label_watching_plural);
		((TextView) findViewById(R.id.watching_stats)).setText(String.valueOf(count.count) + " " + label);

		/* Shortcuts */

		/* Clear shortcut holder */
		((ViewGroup) findViewById(R.id.shortcut_holder)).removeAllViews();

		/* Synthetic applink shortcuts */
		ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_APPLINK, Constants.SCHEMA_ENTITY_APPLINK, Direction.in, true, true);
		settings.appClass = Applinks.class;
		List<Shortcut> shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings);
		if (shortcuts.size() > 0) {
			Collections.sort(shortcuts, new Shortcut.SortByPosition());
			drawShortcuts(shortcuts
					, settings
					, R.string.section_candigram_shortcuts_applinks
					, R.string.section_links_more
					, mResources.getInteger(R.integer.shortcuts_flow_limit)
					, R.id.shortcut_holder
					, R.layout.temp_place_switchboard_item);
		}

		/* Service applink shortcuts */
		settings = new ShortcutSettings(Constants.TYPE_LINK_APPLINK, Constants.SCHEMA_ENTITY_APPLINK, Direction.in, false, true);
		settings.appClass = Applinks.class;
		shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings);
		if (shortcuts.size() > 0) {
			Collections.sort(shortcuts, new Shortcut.SortByPosition());
			drawShortcuts(shortcuts
					, settings
					, null
					, R.string.section_links_more
					, mResources.getInteger(R.integer.shortcuts_flow_limit)
					, R.id.shortcut_holder
					, R.layout.temp_place_switchboard_item);
		}

		/* Shortcuts for places linked to this candigram */
		settings = new ShortcutSettings(Constants.TYPE_LINK_CANDIGRAM, Constants.SCHEMA_ENTITY_PLACE, Direction.out, false, false);
		settings.appClass = Places.class;
		shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings);
		if (shortcuts.size() > 0) {
			Collections.sort(shortcuts, new Shortcut.SortByModifiedDate());
			drawShortcuts(shortcuts
					, settings
					, R.string.section_candigram_shortcuts_places
					, R.string.section_pictures_more
					, mResources.getInteger(R.integer.shortcuts_flow_limit)
					, R.id.shortcut_holder
					, R.layout.temp_place_switchboard_item);
		}

		/* Creator block */

		UI.setVisibility(user_one, View.GONE);
		UI.setVisibility(user_two, View.GONE);
		UserView user = user_one;

		if (user != null
				&& mEntity.creator != null
				&& !mEntity.creator.id.equals(ProxiConstants.ADMIN_USER_ID)) {

			if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				if (((Place) mEntity).getProvider().type.equals("aircandi")) {
					user.setLabel(getString(R.string.candi_label_user_created_by));
					user.databind(mEntity.creator, mEntity.createdDate.longValue(), mEntity.locked);
					UI.setVisibility(user, View.VISIBLE);
					user = user_two;
				}
			}
			else {
				if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
					user.setLabel(getString(R.string.candi_label_user_added_by));
				}
				else {
					user.setLabel(getString(R.string.candi_label_user_created_by));
				}
				user.databind(mEntity.creator, mEntity.createdDate.longValue(), mEntity.locked);
				UI.setVisibility(user_one, View.VISIBLE);
				user = user_two;
			}
		}

		/* Editor block */

		if (user != null && mEntity.modifier != null && !mEntity.modifier.id.equals(ProxiConstants.ADMIN_USER_ID)) {
			if (mEntity.createdDate.longValue() != mEntity.modifiedDate.longValue()) {
				user.setLabel(getString(R.string.candi_label_user_edited_by));
				user.databind(mEntity.modifier, mEntity.modifiedDate.longValue(), null);
				UI.setVisibility(user, View.VISIBLE);
			}
		}

		/* Buttons */
		drawButtons();

		/* Visibility */
		if (mFooterHolder != null) {
			mFooterHolder.setVisibility(View.VISIBLE);
		}

		if (mScrollView != null) {
			mScrollView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void drawButtons() {
		super.drawButtons();

		Candigram candigram = (Candigram) mEntity;

		UI.setVisibility(findViewById(R.id.button_nudge), View.GONE);
		UI.setVisibility(findViewById(R.id.button_capture), View.GONE);

		if (candigram.capture == null || !candigram.capture) {
			UI.setVisibility(findViewById(R.id.button_capture), View.VISIBLE);
		}

		if (candigram.type.equals(Constants.TYPE_APP_BOUNCE)) {
			UI.setVisibility(findViewById(R.id.button_nudge), View.VISIBLE);
		}
	}

	private void setActionText() {
		Candigram candigram = (Candigram) mEntity;
		String action = "ready to leave";

		if (candigram.hopNextDate != null) {
			Long now = DateTime.nowDate().getTime();
			Long next = candigram.hopNextDate.longValue();
			String timeTill = DateTime.timeTill(now, next);
			if (!timeTill.equals("now")) {
				mActionInfo.setText("leaving in" + "\n" + timeTill);
			}
		}
		else if (candigram.hopLastDate != null) {
			Long now = DateTime.nowDate().getTime();
			Long next = candigram.hopLastDate.longValue() + candigram.duration.longValue();
			String timeTill = DateTime.timeTill(now, next);
			if (!timeTill.equals("now")) {
				mActionInfo.setText("leaving in" + "\n" + timeTill);
			}
		}
		mActionInfo.setText(action);
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.candigram_form;
	}

}