package com.aircandi.ui;

import java.util.Collections;
import java.util.List;

import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.applications.Applinks;
import com.aircandi.beta.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.MessageEvent;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.service.HttpService.RequestListener;
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
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

public class PlaceForm extends BaseEntityForm {

	private Boolean	mUpsize;

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mUpsize = extras.getBoolean(Constants.EXTRA_UPSIZE_SYNTHETIC);
		}
	}

	@Override
	protected void databind(final Boolean refreshProposed) {

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
				if (entity == null) {
					refresh = true;
				}
				else if (!entity.shortcuts && !entity.synthetic) {
					refresh = true;
				}

				final ModelResult result = EntityManager.getInstance().getEntity(mEntityId
						, refresh
						, LinkOptions.getDefault(DefaultType.LinksForPlace));

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
							mMenuItemEdit.setVisible(canEdit());
						}

						/* Action bar icon */
						if (((Place) mEntity).category != null) {
							final BitmapRequest bitmapRequest = new BitmapRequest();
							bitmapRequest.setImageUri(((Place) mEntity).category.photo.getUri());
							bitmapRequest.setImageRequestor(this);
							bitmapRequest.setRequestListener(new RequestListener() {

								@Override
								public void onComplete(Object response) {

									final ServiceResponse serviceResponse = (ServiceResponse) response;
									if (serviceResponse.responseCode == ResponseCode.Success) {
										runOnUiThread(new Runnable() {

											@Override
											public void run() {
												final ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
												mActionBar.setIcon(new BitmapDrawable(Aircandi.applicationContext.getResources(), imageResponse.bitmap));
											}
										});
									}
								}
							});
							BitmapManager.getInstance().masterFetch(bitmapRequest);
						}

						draw();
						if (mUpsize) {
							mUpsize = false;
							upsize();
						}
					}
				}
				else {
					Routing.serviceError(PlaceForm.this, result.serviceResponse);
				}
				mBusyManager.hideBusy();
				mBusyManager.stopBodyBusyIndicator();
			}

		}.execute();
	}

	private void upsize() {
		/*
		 * Upsized places do not automatically link to nearby beacons because
		 * the browsing action isn't enough of an indicator of proximity.
		 */
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy();
				mBusyManager.startBodyBusyIndicator();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("UpsizeSynthetic");
				final ModelResult result = EntityManager.getInstance().upsizeSynthetic((Place) mEntity);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				mBusyManager.hideBusy();
				mBusyManager.stopBodyBusyIndicator();
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					final Entity upsizedEntity = (Entity) result.data;
					mEntityId = upsizedEntity.id;
					databind(false);
				}
				else {
					Routing.serviceError(PlaceForm.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onCallButtonClick(View view) {
		Tracker.sendEvent("ui_action", "call_place", null, 0, Aircandi.getInstance().getUser());
		AndroidManager.getInstance().callDialerActivity(this, ((Place) mEntity).phone);
	}

	@Override
	public void onAdd() {
		if (!mEntity.locked || mEntity.ownerId.equals(Aircandi.getInstance().getUser().id)) {
			Routing.route(this, Route.NewFor, mEntity);
		}
		else {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.alert_entity_locked)
					, null
					, this, android.R.string.ok, null, null, null, null);
		}
	}

	@Override
	public void onHelp() {
		Bundle extras = new Bundle();
		extras.putInt(Constants.EXTRA_HELP_ID, R.layout.place_help);
		Routing.route(this, Route.Help, null, null, extras);
	}

	@SuppressWarnings("ucd")
	public void onTuneButtonClick(View view) {
		if (!mEntity.locked || mEntity.ownerId.equals(Aircandi.getInstance().getUser().id)) {
			Routing.route(this, Route.Tune, mEntity);
		}
		else {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, mResources.getString(R.string.alert_entity_locked)
					, null
					, this, android.R.string.ok, null, null, null, null);
		}
	}

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

	// --------------------------------------------------------------------------------------------
	// UI routines
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

		final TextView description = (TextView) findViewById(R.id.candi_form_description);
		final TextView address = (TextView) findViewById(R.id.candi_form_address);
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
			Collections.sort(shortcuts, new Shortcut.SortByPositionModifiedDate());
			drawShortcuts(shortcuts
					, settings
					, R.string.section_place_shortcuts_applinks
					, R.string.section_links_more
					, mResources.getInteger(R.integer.place_shortcuts_flow_limit)
					, R.id.shortcut_holder
					, R.layout.temp_place_switchboard_item);
		}

		/* Service applink shortcuts */
		settings = new ShortcutSettings(Constants.TYPE_LINK_APPLINK, Constants.SCHEMA_ENTITY_APPLINK, Direction.in, false, true);
		settings.appClass = Applinks.class;
		shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings);
		if (shortcuts.size() > 0) {
			Collections.sort(shortcuts, new Shortcut.SortByPositionModifiedDate());
			drawShortcuts(shortcuts
					, settings
					, null
					, R.string.section_links_more
					, mResources.getInteger(R.integer.place_shortcuts_flow_limit)
					, R.id.shortcut_holder
					, R.layout.temp_place_switchboard_item);
		}

		/* Place specific info */
		if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			final Place place = (Place) mEntity;

			UI.setVisibility(address, View.GONE);
			if (address != null) {
				String addressBlock = place.getAddressBlock();

				if (place.phone != null) {
					addressBlock += "<br/>" + place.getFormattedPhone();
				}

				if (!"".equals(addressBlock)) {
					address.setText(Html.fromHtml(addressBlock));
					UI.setVisibility(address, View.VISIBLE);
				}
			}
		}

		/* Creator block */

		UI.setVisibility(user_one, View.GONE);
		UI.setVisibility(user_two, View.GONE);
		UserView userView = user_one;

		if (userView != null
				&& mEntity.creator != null
				&& !mEntity.creator.id.equals(ProxiConstants.ADMIN_USER_ID)) {

			if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				if (((Place) mEntity).getProvider().type.equals("aircandi")) {
					userView.setLabel(getString(R.string.candi_label_user_created_by));
					userView.databind(mEntity.creator, mEntity.createdDate.longValue(), mEntity.locked);
					UI.setVisibility(userView, View.VISIBLE);
					userView = user_two;
				}
			}
			else {
				if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
					userView.setLabel(getString(R.string.candi_label_user_added_by));
				}
				else {
					userView.setLabel(getString(R.string.candi_label_user_created_by));
				}
				userView.databind(mEntity.creator, mEntity.createdDate.longValue(), mEntity.locked);
				UI.setVisibility(user_one, View.VISIBLE);
				userView = user_two;
			}
		}

		/* Editor block */

		if (userView != null && mEntity.modifier != null && !mEntity.modifier.id.equals(ProxiConstants.ADMIN_USER_ID)) {
			if (mEntity.createdDate.longValue() != mEntity.modifiedDate.longValue()) {
				userView.setLabel(getString(R.string.candi_label_user_edited_by));
				userView.databind(mEntity.modifier, mEntity.modifiedDate.longValue(), null);
				UI.setVisibility(userView, View.VISIBLE);
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

		Place place = (Place) mEntity;

		/* Tune */
		UI.setVisibility(findViewById(R.id.button_tune), View.GONE);
		if (!place.synthetic) {
			UI.setVisibility(findViewById(R.id.button_tune), View.VISIBLE);
		}

		/* Footer */
		if (place.synthetic) {
			UI.setVisibility(findViewById(R.id.form_footer), View.GONE);
			return;
		}
		else {
			UI.setVisibility(findViewById(R.id.form_footer), View.VISIBLE);
		}

		/* Add post button in footer */
		UI.setVisibility(findViewById(R.id.button_tune), canAdd() ? View.VISIBLE : View.GONE);
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	protected Boolean canEdit() {
		if (mEntity != null && mEntity.ownerId != null) {
			if (mEntity.ownerId.equals(Aircandi.getInstance().getUser().id)) {
				return true;
			}
			else if (mEntity.ownerId.equals(ProxiConstants.ADMIN_USER_ID)) {
				return true;
			}
			else if (!mEntity.ownerId.equals(ProxiConstants.ADMIN_USER_ID) && !mEntity.locked) {
				return true;
			}
		}
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.place_form;
	}
}