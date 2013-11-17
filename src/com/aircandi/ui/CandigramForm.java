package com.aircandi.ui;

import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.applications.Applinks;
import com.aircandi.applications.Places;
import com.aircandi.components.EntityManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.events.MessageEvent;
import com.aircandi.service.objects.Action.EventCategory;
import com.aircandi.service.objects.Candigram;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.ServiceBase;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.service.objects.User;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.CandiView.IndicatorOptions;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.DateTime.IntervalContext;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Media;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.Type;
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
		mFirstDraw = false;
		if (mEntity.type.equals(Constants.TYPE_APP_TOUR) && !((Candigram) mEntity).stopped && mTimer == null) {
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
			candiView.databind((Place) mEntity, new IndicatorOptions());
		}
		else {
			UI.setVisibility(photoView, View.GONE);
			if (photoView != null) {

				int screenWidthDp = (int) UI.getScreenWidthDisplayPixels(this);
				int widgetWidthDp = 122;
				if (screenWidthDp - widgetWidthDp <= 264) {
					int photoViewWidth = UI.getRawPixelsForDisplayPixels(this, screenWidthDp - widgetWidthDp);
					LinearLayout.LayoutParams paramsImage = new LinearLayout.LayoutParams(photoViewWidth, photoViewWidth);
					photoView.setLayoutParams(paramsImage);
				}

				if (!UI.photosEqual(photoView.getPhoto(), mEntity.getPhoto())) {
					Photo photo = mEntity.getPhoto();
					UI.drawPhoto(photoView, photo);
					if (Type.isFalse(photo.usingDefault)) {
						photoView.setClickable(true);
					}
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
		UI.setVisibility(mActionInfo, View.GONE);
		if (candigram.type.equals(Constants.TYPE_APP_TOUR)) {
			/* Start updating the countdown in action info */
			mHandler.post(mTimer);
			((TextView) findViewById(R.id.header)).setText(getString(R.string.candigram_type_tour_verbose) + " " + getString(R.string.form_title_candigram));
			UI.setVisibility(mActionInfo, View.VISIBLE);
		}
		else if (candigram.type.equals(Constants.TYPE_APP_BOUNCE)) {
			((TextView) findViewById(R.id.header)).setText(getString(R.string.candigram_type_bounce_verbose) + " " + getString(R.string.form_title_candigram));
			if (candigram.stopped) {
				UI.setVisibility(mActionInfo, View.VISIBLE);
			}
		}
		else if (candigram.type.equals(Constants.TYPE_APP_EXPAND)) {
			((TextView) findViewById(R.id.header)).setText(getString(R.string.candigram_type_expand_verbose) + " " + getString(R.string.form_title_candigram));
			if (candigram.stopped) {
				UI.setVisibility(mActionInfo, View.VISIBLE);
			}
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
		if (!candigram.type.equals(Constants.TYPE_APP_EXPAND)) {
			Link link = candigram.getParentLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE);
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
		}

		/* Stats */

		drawStats();

		/* Shortcuts */

		/* Clear shortcut holder */
		((ViewGroup) findViewById(R.id.shortcut_holder)).removeAllViews();

		/* Synthetic applink shortcuts */
		ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, Direction.in, null, true, true);
		settings.appClass = Applinks.class;
		List<Shortcut> shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, null, new Shortcut.SortByPositionSortDate());
		if (shortcuts.size() > 0) {
			Collections.sort(shortcuts, new Shortcut.SortByPositionSortDate());
			Boolean canAdd = EntityManager.canUserAdd(mEntity);
			drawShortcuts(shortcuts
					, settings
					, canAdd ? R.string.section_candigram_shortcuts_applinks_can_add : R.string.section_candigram_shortcuts_applinks
					, R.string.section_links_more
					, mResources.getInteger(R.integer.limit_shortcuts_flow)
					, R.id.shortcut_holder
					, R.layout.temp_place_switchboard_item);
		}

		/* service applink shortcuts */
		settings = new ShortcutSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, Direction.in, null, false, true);
		settings.appClass = Applinks.class;
		shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, null, new Shortcut.SortByPositionSortDate());
		if (shortcuts.size() > 0) {
			Collections.sort(shortcuts, new Shortcut.SortByPositionSortDate());
			drawShortcuts(shortcuts
					, settings
					, null
					, R.string.section_links_more
					, mResources.getInteger(R.integer.limit_shortcuts_flow)
					, R.id.shortcut_holder
					, R.layout.temp_place_switchboard_item);
		}

		/* Shortcuts for places linked to this candigram */
		settings = new ShortcutSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, Direction.out, null, false, false);
		settings.appClass = Places.class;
		shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new ServiceBase.SortByPositionSortDate(), new Shortcut.SortByPositionSortDate());
		if (shortcuts.size() > 0) {
			Collections.sort(shortcuts, new Shortcut.SortByPositionSortDate());
			Integer headingResId = mEntity.type.equals(Constants.TYPE_APP_EXPAND)
					? R.string.section_candigram_shortcuts_places_expand
					: R.string.section_candigram_shortcuts_places;
			drawShortcuts(shortcuts
					, settings
					, headingResId
					, R.string.section_places_more
					, mResources.getInteger(R.integer.limit_shortcuts_flow)
					, R.id.shortcut_holder
					, R.layout.temp_place_switchboard_item);
		}

		/* Creator block */

		UI.setVisibility(user_one, View.GONE);
		UI.setVisibility(user_two, View.GONE);
		UserView user = user_one;

		if (user != null
				&& mEntity.creator != null
				&& !mEntity.creator.id.equals(ServiceConstants.ADMIN_USER_ID)
				&& !mEntity.creator.id.equals(ServiceConstants.ANONYMOUS_USER_ID)) {

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

		if (user != null && mEntity.modifier != null
				&& !mEntity.modifier.id.equals(ServiceConstants.ADMIN_USER_ID)
				&& !mEntity.modifier.id.equals(ServiceConstants.ANONYMOUS_USER_ID)) {
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

		Count count = mEntity.getCount(Constants.TYPE_LINK_LIKE, null, false, Direction.in);
		if (count == null) count = new Count(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_CANDIGRAM, 0);
		String label = this.getString(count.count.intValue() == 1 ? R.string.stats_label_likes : R.string.stats_label_likes_plural);
		((TextView) findViewById(R.id.like_stats)).setText(String.valueOf(count.count) + " " + label);

		count = mEntity.getCount(Constants.TYPE_LINK_WATCH, null, false, Direction.in);
		if (count == null) count = new Count(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_CANDIGRAM, 0);
		label = this.getString(count.count.intValue() == 1 ? R.string.stats_label_watching : R.string.stats_label_watching_plural);
		((TextView) findViewById(R.id.watching_stats)).setText(String.valueOf(count.count) + " " + label);

		UI.setVisibility(findViewById(R.id.places_stats), View.GONE);
		if (mEntity.type.equals(Constants.TYPE_APP_EXPAND)) {
			Count active = mEntity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, false, Direction.out);
			Count inactive = mEntity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, true, Direction.out);
			Count summed = new Count(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, 0);
			if (active != null) {
				summed.count = summed.count.intValue() + active.count.intValue();
			}
			if (inactive != null) {
				summed.count = summed.count.intValue() + inactive.count.intValue();
			}

			label = this.getString(summed.count.intValue() == 1 ? R.string.stats_label_places : R.string.stats_label_places_plural);
			((TextView) findViewById(R.id.places_stats)).setText(String.valueOf(summed.count) + " " + label);
			UI.setVisibility(findViewById(R.id.places_stats), View.VISIBLE);
		}

	}

	@Override
	protected void drawButtons() {
		super.drawButtons();

		Candigram candigram = (Candigram) mEntity;

		UI.setVisibility(findViewById(R.id.button_bounce), View.GONE);
		UI.setVisibility(findViewById(R.id.button_expand), View.GONE);

		if (!candigram.stopped) {
			if (candigram.type.equals(Constants.TYPE_APP_BOUNCE)) {
				UI.setVisibility(findViewById(R.id.button_bounce), View.VISIBLE);
			}
			else if (candigram.type.equals(Constants.TYPE_APP_EXPAND)) {
				UI.setVisibility(findViewById(R.id.button_expand), View.VISIBLE);
			}
		}
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
		if (event.message.action.getEventCategory().equals(EventCategory.REFRESH)) {
			if (event.message.action.entity != null && mEntityId.equals(event.message.action.entity.id)) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						onRefresh();
					}
				});
			}
		}
		else if (event.message.action.toEntity != null && mEntityId.equals(event.message.action.toEntity.id)) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onRefresh();
				}
			});
		}
		else if (event.message.action.entity != null && mEntityId.equals(event.message.action.entity.id)) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					/*
					 * Candigram has moved or repeated and we want to show something.
					 * 
					 * - We don't get this message if current user triggered the move.
					 * - Refresh will update stats, show new place.
					 */
					String message = null;
					if (event.message.action.entity.type.equals(Constants.TYPE_APP_BOUNCE)) {
						message = getString(R.string.alert_candigram_bounced);
					}
					else if (event.message.action.entity.type.equals(Constants.TYPE_APP_TOUR)) {
						message = getString(R.string.alert_candigram_toured);
					}
					else if (event.message.action.entity.type.equals(Constants.TYPE_APP_EXPAND)) {
						message = getString(R.string.alert_candigram_expanded);
					}
					if (!TextUtils.isEmpty(event.message.action.toEntity.name)) {
						message += ": " + event.message.action.toEntity.name;
					}
					UI.showToastNotification(message, Toast.LENGTH_SHORT);
					onRefresh();
				}
			});
		}
	}

	@SuppressWarnings("ucd")
	public void onExpandButtonClick(View view) {
		
		if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
			Dialogs.signin(this, R.string.alert_signin_message_candigram_expand);
			return;
		}		

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("PromoteEntity");
				/*
				 * Call service routine to get a move candidate.
				 */
				final ModelResult result = EntityManager.getInstance().moveCandigram(mEntity, true, true, null);
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
						repeatCandidate(place, Aircandi.getInstance().getCurrentUser());
					}
				}
				else {
					Errors.handleError(CandigramForm.this, result.serviceResponse);
				}
			}
		}.execute();

	}

	@SuppressWarnings("ucd")
	public void onBounceButtonClick(View view) {
		
		if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
			Dialogs.signin(this, R.string.alert_signin_message_candigram_bounce);
			return;
		}		

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("KickEntity");
				/*
				 * Call service routine to get a move candidate.
				 */
				final ModelResult result = EntityManager.getInstance().moveCandigram(mEntity, false, true, null);
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
						kickCandidate(place, Aircandi.getInstance().getCurrentUser());
					}
				}
				else {
					Errors.handleError(CandigramForm.this, result.serviceResponse);
				}
			}
		}.execute();

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
		else if (mEntity.type.equals(Constants.TYPE_APP_EXPAND)) {
			helpResId = R.layout.candigram_touring_help;
		}
		extras.putInt(Constants.EXTRA_HELP_ID, helpResId);
		Routing.route(this, Route.HELP, null, null, extras);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public void kickCandidate(final Entity place, final User user) {

		ViewGroup customView = buildPlaceView((Place) place, place.name, place.getPhoto());
		final TextView message = (TextView) customView.findViewById(R.id.message);

		message.setText(getResources().getString(R.string.alert_message_candigram_bounce));

		String dialogTitle = getResources().getString(R.string.alert_candigram_bounce);
		if (mEntity.name != null && !mEntity.name.equals("")) {
			dialogTitle = mEntity.name;
		}

		final AlertDialog dialog = Dialogs.alertDialog(null
				, dialogTitle
				, null
				, customView
				, this
				, R.string.alert_button_bounce
				, android.R.string.cancel
				, R.string.alert_button_bounce_follow
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
				final ModelResult result = EntityManager.getInstance().moveCandigram(mEntity, false, false, entity.id);
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
						Media.playSound(mCandigramExitSoundId, 1.0f);
						finish();
						Animate.doOverridePendingTransition(CandigramForm.this, TransitionType.CANDIGRAM_OUT);
					}
					else {
						Media.playSound(mCandigramExitSoundId, 1.0f);
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

	public void repeatCandidate(final Entity place, final User user) {

		ViewGroup customView = buildPlaceView((Place) place, place.name, place.getPhoto());
		final TextView message = (TextView) customView.findViewById(R.id.message);

		message.setText(getResources().getString(R.string.alert_message_candigram_expand));

		String dialogTitle = getResources().getString(R.string.alert_promoted_candigram);
		if (mEntity.name != null && !mEntity.name.equals("")) {
			dialogTitle = mEntity.name;
		}

		final AlertDialog dialog = Dialogs.alertDialog(null
				, dialogTitle
				, null
				, customView
				, this
				, R.string.alert_button_expand
				, android.R.string.cancel
				, R.string.alert_button_expand_follow
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							repeatWrapup(place, user, false);
						}
						else if (which == Dialog.BUTTON_NEUTRAL) {
							repeatWrapup(place, user, true);
						}
						else if (which == Dialog.BUTTON_NEGATIVE) {
							dialog.dismiss();
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	public void repeatWrapup(final Entity entity, User user, final Boolean follow) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("PromoteEntity");
				final ModelResult result = EntityManager.getInstance().moveCandigram(mEntity, true, false, entity.id);
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
						Media.playSound(mCandigramExitSoundId, 1.0f);
						finish();
						Animate.doOverridePendingTransition(CandigramForm.this, TransitionType.CANDIGRAM_OUT);
					}
					else {
						Media.playSound(mCandigramExitSoundId, 1.0f);
						onRefresh();
					}
				}
				else {
					Errors.handleError(CandigramForm.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	public ViewGroup buildPlaceView(Place place, String placeName, Photo placePhoto) {

		final LayoutInflater inflater = LayoutInflater.from(this);
		final ViewGroup customView = (ViewGroup) inflater.inflate(R.layout.dialog_candigram, null);
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

	private void setActionText() {
		Candigram candigram = (Candigram) mEntity;
		String action = "ready to leave";

		if (candigram.stopped) {
			if (candigram.type.equals(Constants.TYPE_APP_EXPAND)) {
				action = "finished";
			}
			else {
				action = "parked";
				if (candigram.hopCount.intValue() >= candigram.hopsMax.intValue()) {
					action = "finished traveling and back with sender";
				}
			}
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