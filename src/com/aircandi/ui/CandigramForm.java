package com.aircandi.ui;

import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.applications.Applinks;
import com.aircandi.applications.Places;
import com.aircandi.components.EntityManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.events.MessageEvent;
import com.aircandi.service.objects.Candigram;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.service.objects.User;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.DateTime.IntervalContext;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

public class CandigramForm extends BaseEntityForm {

	private Runnable	mTimer;
	private TextView	mActionInfo;
	private int			mCandigramExitSoundId;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mLinkProfile = LinkProfile.LINKS_FOR_CANDIGRAM;
		mActionInfo = (TextView) findViewById(R.id.action_info);
		mCandigramExitSoundId = Aircandi.soundPool.load(this, R.raw.candigram_exit, 1);
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
		/*
		 * Candigram has moved/changed and we want to show something. We
		 * don't get this message if current user triggered the move.
		 */
		else if (mEntityId.equals(event.notification.entity.id)) {
			if (event.notification.action.equals("move")) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						EntityManager.getInstance().deleteEntity(event.notification.entity.id, true);
						kickAlert(event.notification.toEntity, event.notification.user);
					}
				});
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onKickButtonClick(View view) {

		//		if (!EntityManager.canUserKick((Candigram) mEntity)) {
		//			LINK link = mEntity.getParentLink(Constants.TYPE_LINK_CANDIGRAM);
		//			kickUnavailable(link.shortcut.name, link.shortcut.photo, Aircandi.getInstance().getUser());
		//			return;
		//		}

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("KickEntity");
				Tracker.sendEvent("ui_action", "kick_entity", null, 0, Aircandi.getInstance().getUser());
				/*
				 * Call service routine to get a move candidate.
				 */
				final ModelResult result = EntityManager.getInstance().moveCandigram(mEntity, true, null);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;
				setSupportProgressBarIndeterminateVisibility(false);
				hideBusy();
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					if (result.data != null) {
						List<Entity> entities = (List<Entity>) result.data;
						Entity place = entities.get(0);
						kickCandidate(place, Aircandi.getInstance().getUser());
					}
				}
				else {
					Errors.handleError(CandigramForm.this, result.serviceResponse);
				}
			}
		}.execute();

	}

	public void kickUnavailable(String placeName, Photo placePhoto, final User user) {

		ViewGroup customView = customPlaceView(null, placeName, placePhoto);
		final TextView message = (TextView) customView.findViewById(R.id.message);

		message.setText("To kick this candigram, you need to be at: ");

		String dialogTitle = getResources().getString(R.string.alert_kicked_candigram);
		if (mEntity.name != null && !mEntity.name.equals("")) {
			dialogTitle = mEntity.name;
		}

		final AlertDialog dialog = Dialogs.alertDialog(null
				, dialogTitle
				, null
				, customView
				, this
				, android.R.string.ok
				, null
				, null
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	public void kickCandidate(final Entity place, final User user) {

		ViewGroup customView = customPlaceView((Place) place, place.name, place.getPhoto());
		final TextView message = (TextView) customView.findViewById(R.id.message);

		message.setText("This candigram will be kicked to: ");

		String dialogTitle = getResources().getString(R.string.alert_kicked_candigram);
		if (mEntity.name != null && !mEntity.name.equals("")) {
			dialogTitle = mEntity.name;
		}

		final AlertDialog dialog = Dialogs.alertDialog(null
				, dialogTitle
				, null
				, customView
				, this
				, R.string.alert_kicked
				, android.R.string.cancel
				, R.string.alert_kicked_follow
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							kickWrapup(place, user, false);
						}
						else if (which == Dialog.BUTTON_NEUTRAL) {
							kickWrapup(place, user, true);
						}
						else if (which == Dialog.BUTTON_NEGATIVE) {
							dialog.dismiss();
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	public void kickWrapup(final Entity entity, User user, final Boolean follow) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("KickEntity");
				Tracker.sendEvent("ui_action", "kick_entity", null, 0, Aircandi.getInstance().getUser());
				final ModelResult result = EntityManager.getInstance().moveCandigram(mEntity, false, entity.id);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;
				setSupportProgressBarIndeterminateVisibility(false);
				hideBusy();
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					if (follow) {
						Places.view(CandigramForm.this, entity.id, mEntity.id);
						Aircandi.soundPool.play(mCandigramExitSoundId, 1.5f, 1.5f, 0, 0, 1f);
						finish();
						Animate.doOverridePendingTransition(CandigramForm.this, TransitionType.CANDIGRAM_OUT);
					}
					else {
						Aircandi.soundPool.play(mCandigramExitSoundId, 1.5f, 1.5f, 0, 0, 1f);
						finish();
						Animate.doOverridePendingTransition(CandigramForm.this, TransitionType.CANDIGRAM_OUT);
					}
				}
				else {
					Errors.handleError(CandigramForm.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	public void kickAlert(Entity entity, User user) {

		final Place place = (Place) entity;
		ViewGroup customView = customPlaceView((Place) place, place.name, place.getPhoto());
		final TextView message = (TextView) customView.findViewById(R.id.message);

		if (user.id.equals(Aircandi.getInstance().getUser().id)) {
			message.setText("You kicked this candigram to: ");
		}
		else {
			message.setText(user.name + " kicked this candigram to: ");
		}

		String dialogTitle = getResources().getString(R.string.alert_kicked_candigram);
		if (mEntity.name != null && !mEntity.name.equals("")) {
			dialogTitle = mEntity.name;
		}

		final AlertDialog dialog = Dialogs.alertDialog(null
				, dialogTitle
				, null
				, customView
				, this
				, android.R.string.ok
				, R.string.alert_kicked_follow
				, null
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							Aircandi.soundPool.play(mCandigramExitSoundId, 1.5f, 1.5f, 0, 0, 1f);
							finish();
							Animate.doOverridePendingTransition(CandigramForm.this, TransitionType.CANDIGRAM_OUT);
						}
						else if (which == Dialog.BUTTON_NEGATIVE) {
							Places.view(CandigramForm.this, place.id, mEntity.id);
							Aircandi.soundPool.play(mCandigramExitSoundId, 1.5f, 1.5f, 0, 0, 1f);
							finish();
							Animate.doOverridePendingTransition(CandigramForm.this, TransitionType.CANDIGRAM_OUT);
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	public ViewGroup customPlaceView(Place place, String placeName, Photo placePhoto) {

		final LayoutInflater inflater = LayoutInflater.from(this);
		final ViewGroup customView = (ViewGroup) inflater.inflate(R.layout.temp_kicked_candigram, null);
		final TextView name = (TextView) customView.findViewById(R.id.name);
		final TextView address = (TextView) customView.findViewById(R.id.address);
		final AirImageView photoView = (AirImageView) customView.findViewById(R.id.photo);

		UI.setVisibility(name, View.GONE);
		UI.setVisibility(address, View.GONE);
		if (placeName != null) {
			name.setText(placeName);
			UI.setVisibility(name, View.VISIBLE);
		}

		if (place != null) {
			String addressBlock = "";
			if (place.city != null && place.region != null && !place.city.equals("") && !place.region.equals("")) {
				addressBlock += place.city + ", " + place.region;
			}
			else if (place.city != null && !place.city.equals("")) {
				addressBlock += place.city;
			}
			else if (place.region != null && !place.region.equals("")) {
				addressBlock += place.region;
			}
			if (!addressBlock.equals("")) {
				address.setText(addressBlock);
				UI.setVisibility(address, View.VISIBLE);
			}
		}

		photoView.setTag(place);
		UI.drawPhoto(photoView, placePhoto);
		return customView;
	}

	@Override
	public void onAdd() {
		if (EntityManager.canUserAdd(mEntity)) {
			Routing.route(this, Route.NEW_FOR, mEntity);
			return;
		}

		if (mEntity.locked) {
			Dialogs.locked(this, mEntity);
		}
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
		Routing.route(this, Route.HELP, null, null, extras);
	}

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

	@Override
	public void draw() {
		/*
		 * For now, we assume that the candi form isn't recycled.
		 * 
		 * We leave most of the views visible by default so they are visible in the layout editor.
		 * 
		 * - WebImageView primary image is visible by default
		 * - WebImageView child views are gone by default
		 * - Header views are visible by default
		 */

		if (mEntity.type.equals("tour") && ((Candigram) mEntity).hopEnabled && mTimer == null) {
			mTimer = new Runnable() {

				@Override
				public void run() {
					setActionText();
					mHandler.postDelayed(mTimer, 1000);
				}
			};
		}
		else {
			setActionText();
		}

		setActivityTitle(mEntity.name);

		final CandiView candiView = (CandiView) findViewById(R.id.candi_view);
		final AirImageView photoView = (AirImageView) findViewById(R.id.photo);
		final TextView name = (TextView) findViewById(R.id.name);
		final TextView subtitle = (TextView) findViewById(R.id.subtitle);
		final AirImageView placePhotoView = (AirImageView) findViewById(R.id.place_photo);
		final TextView placeName = (TextView) findViewById(R.id.place_name);

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
			UI.setVisibility(mActionInfo, View.VISIBLE);
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

		/* Place context */
		View placeHolder = findViewById(R.id.place_holder);
		UI.setVisibility(placeHolder, View.GONE);
		Link link = candigram.getParentLink(Constants.TYPE_LINK_CANDIGRAM);
		if (link != null && link.shortcut != null) {
			if (placePhotoView != null) {
				Photo photo = link.shortcut.getPhoto();
				UI.drawPhoto(placePhotoView, photo);
				UI.setVisibility(placePhotoView, View.VISIBLE);
			}
			UI.setVisibility(placeName, View.GONE);
			if (link.shortcut.name != null && !link.shortcut.name.equals("")) {
				placeName.setText(Html.fromHtml(link.shortcut.name));
				UI.setVisibility(placeName, View.VISIBLE);
			}
			placeHolder.setTag(link.shortcut);
			UI.setVisibility(placeHolder, View.VISIBLE);
		}

		/* Stats */

		drawStats();

		/* Shortcuts */

		/* Clear shortcut holder */
		((ViewGroup) findViewById(R.id.shortcut_holder)).removeAllViews();

		/* Synthetic applink shortcuts */
		ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_APPLINK, Constants.SCHEMA_ENTITY_APPLINK, Direction.in, true, true);
		settings.appClass = Applinks.class;
		List<Shortcut> shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, null);
		if (shortcuts.size() > 0) {
			Collections.sort(shortcuts, new Shortcut.SortByPosition());
			Boolean canAdd = EntityManager.canUserAdd(mEntity);

			drawShortcuts(shortcuts
					, settings
					, canAdd ? R.string.section_candigram_shortcuts_applinks_can_add : R.string.section_candigram_shortcuts_applinks
					, R.string.section_links_more
					, mResources.getInteger(R.integer.shortcuts_flow_limit)
					, R.id.shortcut_holder
					, R.layout.temp_place_switchboard_item);
		}

		/* service applink shortcuts */
		settings = new ShortcutSettings(Constants.TYPE_LINK_APPLINK, Constants.SCHEMA_ENTITY_APPLINK, Direction.in, false, true);
		settings.appClass = Applinks.class;
		shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, null);
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
		shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new Link.SortByModifiedDate());
		if (shortcuts.size() > 0) {
			drawShortcuts(shortcuts
					, settings
					, R.string.section_candigram_shortcuts_places
					, R.string.section_places_more
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
				&& !mEntity.creator.id.equals(ServiceConstants.ADMIN_USER_ID)) {

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

		if (user != null && mEntity.modifier != null && !mEntity.modifier.id.equals(ServiceConstants.ADMIN_USER_ID)) {
			if (mEntity.createdDate.longValue() != mEntity.modifiedDate.longValue()) {
				user.setLabel(getString(R.string.candi_label_user_edited_by));
				user.databind(mEntity.modifier, mEntity.modifiedDate.longValue(), null);
				UI.setVisibility(user, View.VISIBLE);
			}
		}

		/* Buttons */
		drawButtons();

		/* Visibility */
		if (mScrollView != null) {
			mScrollView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void drawStats() {

		Count count = mEntity.getCount(Constants.TYPE_LINK_LIKE, Direction.in);
		if (count == null) count = new Count(Constants.TYPE_LINK_LIKE, 0);
		String label = this.getString(count.count.intValue() == 1 ? R.string.stats_label_likes : R.string.stats_label_likes_plural);
		((TextView) findViewById(R.id.like_stats)).setText(String.valueOf(count.count) + " " + label);

		count = mEntity.getCount(Constants.TYPE_LINK_WATCH, Direction.in);
		if (count == null) count = new Count(Constants.TYPE_LINK_WATCH, 0);
		label = this.getString(count.count.intValue() == 1 ? R.string.stats_label_watching : R.string.stats_label_watching_plural);
		((TextView) findViewById(R.id.watching_stats)).setText(String.valueOf(count.count) + " " + label);
	}

	@Override
	protected void drawButtons() {
		super.drawButtons();

		Candigram candigram = (Candigram) mEntity;

		UI.setVisibility(findViewById(R.id.button_kick), View.GONE);
		//		if (EntityManager.canUserAdd(mEntity)) {
		if (candigram.type.equals(Constants.TYPE_APP_BOUNCE)) {
			UI.setVisibility(findViewById(R.id.button_kick), View.VISIBLE);
		}
		//		}
	}

	private void setActionText() {
		Candigram candigram = (Candigram) mEntity;
		String action = "ready to leave";

		if (!candigram.hopEnabled) {
			action = "parked";
		}

		if (candigram.hopCount.intValue() >= candigram.hopsMax.intValue()) {
			action = "finished traveling and back with sender";
		}
		else {
			if (candigram.hopNextDate != null) {
				Long now = DateTime.nowDate().getTime();
				Long next = candigram.hopNextDate.longValue();
				String timeTill = DateTime.interval(now, next, IntervalContext.FUTURE);
				if (!timeTill.equals("now")) {
					action = "leaving in" + "\n" + timeTill;
				}
				else {
					action = "waiting to leave";
				}
			}
		}
		mActionInfo.setText(action);
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onPause() {
		mHandler.removeCallbacks(mTimer);
		super.onPause();
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.candigram_form;
	}

}