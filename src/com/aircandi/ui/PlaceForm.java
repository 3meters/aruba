package com.aircandi.ui;

import java.util.Collections;
import java.util.List;

import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.applications.Applinks;
import com.aircandi.components.EntityManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.BitmapResponse;
import com.aircandi.events.MessageEvent;
import com.aircandi.service.RequestListener;
import com.aircandi.service.ServiceResponse;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

public class PlaceForm extends BaseEntityForm {

	private Boolean	mUpsize;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mUpsize = extras.getBoolean(Constants.EXTRA_UPSIZE_SYNTHETIC);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mLinkProfile = LinkProfile.LINKS_FOR_PLACE;
	}

	@Override
	public void afterDatabind() {
		super.afterDatabind();

		if (mUpsize && mEntity != null) {
			mUpsize = false;
			upsize();
		}
	}

	private void upsize() {
		/*
		 * Upsized places do not automatically link to nearby beacons because
		 * the browsing action isn't enough of an indicator of proximity.
		 */
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.stopBodyBusyIndicator();
				showBusy(R.string.progress_upsizing, false);
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
				hideBusy();
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					final Entity upsizedEntity = (Entity) result.data;
					mEntityId = upsizedEntity.id;
					mEntity = null;
					mCacheStamp = null;
					databind(BindingMode.AUTO);
				}
				else {
					Errors.handleError(PlaceForm.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

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
		extras.putInt(Constants.EXTRA_HELP_ID, R.layout.place_help);
		Routing.route(this, Route.HELP, null, null, extras);
	}

	@SuppressWarnings("ucd")
	public void onTuneButtonClick(View view) {
		if (EntityManager.canUserAdd(mEntity)) {
			Routing.route(this, Route.TUNE, mEntity);
			return;
		}

		if (mEntity.locked) {
			Dialogs.locked(this, mEntity);
		}
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
		/*
		 * Refresh the form because something new has been added to it
		 * like a comment or post. Or something has moved like a candigram.
		 */
		if (mEntityId.equals(event.notification.toEntity.id)) {
			
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onRefresh();
				}
			});
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
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

		setActivityTitle(mEntity.name);

		/* Action bar icon */
		if (((Place) mEntity).category != null) {
			Place place = (Place) mEntity;
			final BitmapRequest bitmapRequest = new BitmapRequest()
					.setBitmapUri(place.category.photo.getUri())
					.setBitmapRequestor(place.category)
					.setRequestListener(new RequestListener() {

						@Override
						public void onComplete(Object response) {

							final ServiceResponse serviceResponse = (ServiceResponse) response;
							if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										final BitmapResponse bitmapResponse = (BitmapResponse) serviceResponse.data;
										final int color = mResources.getColor(getThemeTone().equals("dark") ? R.color.gray_00_pcnt
												: R.color.gray_90_pcnt);
										ColorFilter colorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
										BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(),
												bitmapResponse.bitmap);
										bitmapDrawable.setColorFilter(colorFilter);
										bitmapDrawable.setAlpha(getThemeTone().equals("dark") ? 204 : 153);
										mActionBar.setIcon(bitmapDrawable);
									}
								});
							}
						}
					});
			BitmapManager.getInstance().masterFetch(bitmapRequest);
		}

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

		drawStats();

		/* Shortcuts */

		/* Clear shortcut holder */
		((ViewGroup) findViewById(R.id.shortcut_holder)).removeAllViews();

		/* Synthetic applink shortcuts */
		ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, Direction.in, false, true, true);
		settings.appClass = Applinks.class;
		List<Shortcut> shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, null, null);
		if (shortcuts.size() > 0) {
			Collections.sort(shortcuts, new Shortcut.SortByPositionSortDate());
			drawShortcuts(shortcuts
					, settings
					, R.string.section_place_shortcuts_applinks
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
				&& !mEntity.creator.id.equals(ServiceConstants.ADMIN_USER_ID)
				&& !mEntity.creator.id.equals(ServiceConstants.ANONYMOUS_USER_ID)) {

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

		if (userView != null && mEntity.modifier != null
				&& !mEntity.modifier.id.equals(ServiceConstants.ADMIN_USER_ID)
				&& !mEntity.modifier.id.equals(ServiceConstants.ANONYMOUS_USER_ID)) {
			if (mEntity.createdDate.longValue() != mEntity.modifiedDate.longValue()) {
				userView.setLabel(getString(R.string.candi_label_user_edited_by));
				userView.databind(mEntity.modifier, mEntity.modifiedDate.longValue(), null);
				UI.setVisibility(userView, View.VISIBLE);
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

		Count count = mEntity.getCount(Constants.TYPE_LINK_LIKE, null, Direction.in);
		if (count == null) count = new Count(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_PLACE, 0);
		String label = this.getString(count.count.intValue() == 1 ? R.string.stats_label_likes : R.string.stats_label_likes_plural);
		((TextView) findViewById(R.id.like_stats)).setText(String.valueOf(count.count) + " " + label);

		count = mEntity.getCount(Constants.TYPE_LINK_WATCH, null, Direction.in);
		if (count == null) count = new Count(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, 0);
		label = this.getString(count.count.intValue() == 1 ? R.string.stats_label_watching : R.string.stats_label_watching_plural);
		((TextView) findViewById(R.id.watching_stats)).setText(String.valueOf(count.count) + " " + label);
	}

	@Override
	protected void drawButtons() {
		super.drawButtons();

		Place place = (Place) mEntity;

		/* TUNE */
		UI.setVisibility(findViewById(R.id.button_tune), View.GONE);
		if (!place.synthetic) {
			UI.setVisibility(findViewById(R.id.button_tune), View.VISIBLE);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.place_form;
	}
}