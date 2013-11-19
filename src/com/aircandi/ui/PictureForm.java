package com.aircandi.ui;

import java.util.Collections;
import java.util.List;

import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.applications.Applinks;
import com.aircandi.components.EntityManager;
import com.aircandi.events.MessageEvent;
import com.aircandi.service.objects.Action.EventCategory;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.CandiView.IndicatorOptions;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

public class PictureForm extends BaseEntityForm {

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mLinkProfile = LinkProfile.LINKS_FOR_PICTURE;
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
		mFirstDraw = false;
		setActivityTitle(mEntity.name);

		final CandiView candiView = (CandiView) findViewById(R.id.candi_view);
		final TextView name = (TextView) findViewById(R.id.name);
		final TextView placeName = (TextView) findViewById(R.id.place_name);
		final TextView parentName = (TextView) findViewById(R.id.parent_name);
		final TextView parentLabel = (TextView) findViewById(R.id.parent_label);
		final AirImageView photoView = (AirImageView) findViewById(R.id.photo);
		final AirImageView placePhotoView = (AirImageView) findViewById(R.id.place_photo);
		final AirImageView parentPhotoView = (AirImageView) findViewById(R.id.parent_photo);
		final TextView subtitle = (TextView) findViewById(R.id.subtitle);

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
					RelativeLayout.LayoutParams paramsImage = new RelativeLayout.LayoutParams(photoViewWidth, photoViewWidth);
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

		/* Primary candi image */

		description.setText(null);

		UI.setVisibility(findViewById(R.id.section_description), View.GONE);
		if (description != null && mEntity.description != null && !mEntity.description.equals("")) {
			description.setText(Html.fromHtml(mEntity.description));
			UI.setVisibility(findViewById(R.id.section_description), View.VISIBLE);
		}

		/* Parent context */
		View parentHolder = findViewById(R.id.parent_holder);
		UI.setVisibility(parentHolder, View.GONE);
		Link link = mEntity.getLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_CANDIGRAM, null, Direction.out);
		if (link == null) {
			link = mEntity.getLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, null, Direction.out);
		}
		if (link != null && link.shortcut != null) {
			if (parentPhotoView != null) {
				Photo photo = link.shortcut.getPhoto();
				UI.drawPhoto(parentPhotoView, photo);
				UI.setVisibility(parentPhotoView, View.VISIBLE);
			}
			UI.setVisibility(parentName, View.GONE);
			if (parentName != null && !TextUtils.isEmpty(link.shortcut.name)) {
				parentName.setText(Html.fromHtml(link.shortcut.name));
				UI.setVisibility(parentName, View.VISIBLE);
			}
			if (link.shortcut.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
				parentLabel.setText(getString(R.string.picture_label_parent_candigram));
			}
			else if (link.shortcut.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				parentLabel.setText(getString(R.string.picture_label_parent_place));
			}
			parentHolder.setTag(link.shortcut);
			UI.setVisibility(parentHolder, View.VISIBLE);
		}

		/* Place context */
		View placeHolder = findViewById(R.id.place_holder);
		UI.setVisibility(placeHolder, View.GONE);
		if (mEntity.place != null) {
			if (placePhotoView != null) {
				Photo photo = mEntity.place.getPhoto();
				UI.drawPhoto(placePhotoView, photo);
				UI.setVisibility(placePhotoView, View.VISIBLE);
			}
			UI.setVisibility(placeName, View.GONE);
			if (placeName != null && mEntity.place.name != null && !mEntity.place.name.equals("")) {
				placeName.setText(Html.fromHtml(mEntity.place.name));
				UI.setVisibility(placeName, View.VISIBLE);
			}
			placeHolder.setTag(mEntity.place.getShortcut());
			UI.setVisibility(placeHolder, View.VISIBLE);
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
			drawShortcuts(shortcuts
					, settings
					, R.string.section_place_shortcuts_applinks
					, R.string.section_links_more
					, mResources.getInteger(R.integer.limit_shortcuts_flow)
					, R.id.shortcut_holder
					, R.layout.temp_place_switchboard_item);
		}

		/* Service applink shortcuts */
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
	protected void drawButtons() {
		super.drawButtons();
	}

	@Override
	protected void drawStats() {

		TextView likeStats = (TextView) findViewById(R.id.like_stats);
		if (likeStats != null) {
			Count count = mEntity.getCount(Constants.TYPE_LINK_LIKE, null, false, Direction.in);
			if (count == null) count = new Count(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_PICTURE, 0);
			likeStats.setText(String.valueOf(count.count));
		}

		TextView watchingStats = (TextView) findViewById(R.id.watching_stats);
		if (watchingStats != null) {
			Count count = mEntity.getCount(Constants.TYPE_LINK_WATCH, null, false, Direction.in);
			if (count == null) count = new Count(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PICTURE, 0);
			watchingStats.setText(String.valueOf(count.count));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.picture_form;
	}

}