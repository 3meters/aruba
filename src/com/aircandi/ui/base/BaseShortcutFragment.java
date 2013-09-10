package com.aircandi.ui.base;

import java.util.List;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.applications.Applinks;
import com.aircandi.applications.Candigrams;
import com.aircandi.applications.Comments;
import com.aircandi.applications.Pictures;
import com.aircandi.applications.Places;
import com.aircandi.applications.Users;
import com.aircandi.components.EntityManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutMeta;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public abstract class BaseShortcutFragment extends BaseFragment {

	protected String		mShortcutType;
	protected LinkProfile	mLinkProfiles;
	protected ScrollView	mScrollView;

	protected String		mEntityId;
	protected Entity		mEntity;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		mScrollView = (ScrollView) view.findViewById(R.id.scroll_view);
		return view;
	}

	@Override
	public void databind(final BindingMode mode) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {

				Entity entity = EntityManager.getEntity(mEntityId);
				Boolean refresh = false;
				if (entity == null || mode == BindingMode.service || !entity.shortcuts) {
					refresh = true;
				}

				LinkOptions options = LinkOptions.getDefault(mLinkProfiles);
				final ModelResult result = EntityManager.getInstance().getEntity(mEntityId, refresh, options);

				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;

				if (isAdded()) {
					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						if (result.data != null) {
							mEntity = (Entity) result.data;
							draw();
						}
					}
					else {
						Routing.serviceError(getSherlockActivity(), result.serviceResponse);
					}
					hideBusy();
				}
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
		if (getView() == null) return;

		((ViewGroup) getView().findViewById(R.id.shortcut_holder)).removeAllViews();

		if (mShortcutType.equals(Constants.TYPE_LINK_CREATE)) {

			/* Shortcuts for place entities created by user */
			ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PLACE, Direction.out, false, false);
			settings.appClass = Places.class;
			List<Shortcut> shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new Link.SortByModifiedDate());
			if (shortcuts.size() > 0) {
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_places_created
						, R.string.section_places_more
						, mResources.getInteger(R.integer.shortcuts_flow_limit)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}

			/* Shortcuts for place entities created by user */
			settings = new ShortcutSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_CANDIGRAM, Direction.out, false, false);
			settings.appClass = Candigrams.class;
			shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new Link.SortByModifiedDate());
			if (shortcuts.size() > 0) {
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_candigrams_created
						, R.string.section_places_more
						, mResources.getInteger(R.integer.shortcuts_flow_limit)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}

			/* Shortcuts for post entities created by user */
			settings = new ShortcutSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PICTURE, Direction.out, false, false);
			settings.appClass = Pictures.class;
			shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new Link.SortByModifiedDate());
			if (shortcuts.size() > 0) {
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
			List<Shortcut> shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new Link.SortByModifiedDate());
			if (shortcuts.size() > 0) {
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_places_watching
						, R.string.section_places_more
						, mResources.getInteger(R.integer.shortcuts_flow_limit)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}

			/* Watching candigrams */
			settings = new ShortcutSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_CANDIGRAM, Direction.out, false, false);
			settings.appClass = Candigrams.class;
			shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new Link.SortByModifiedDate());
			if (shortcuts.size() > 0) {
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_candigrams_watching
						, R.string.section_pictures_more
						, mResources.getInteger(R.integer.shortcuts_flow_limit)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}

			/* Watching pictures */
			settings = new ShortcutSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PICTURE, Direction.out, false, false);
			settings.appClass = Pictures.class;
			shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new Link.SortByModifiedDate());
			if (shortcuts.size() > 0) {
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
			shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new Link.SortByModifiedDate());
			if (shortcuts.size() > 0) {
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_users_watching
						, R.string.section_users_more
						, mResources.getInteger(R.integer.shortcuts_flow_limit)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}
		}

		if (mScrollView != null) {
			mScrollView.setVisibility(View.VISIBLE);
		}
	}

	protected void drawShortcuts(List<Shortcut> shortcuts
			, ShortcutSettings settings
			, Integer titleResId
			, Integer moreResId
			, Integer flowLimit
			, Integer holderId
			, Integer flowItemResId) {

		View holder = LayoutInflater.from(getSherlockActivity()).inflate(R.layout.section_shortcuts, null);
		SectionLayout section = (SectionLayout) holder.findViewById(R.id.section_layout_shortcuts);
		if (titleResId != null) {
			section.setHeaderTitle(getString(titleResId));
		}
		else {
			if (section.getHeader() != null) {
				section.getHeader().setVisibility(View.GONE);
			}
		}

		if (shortcuts.size() > flowLimit) {

			Intent intent = null;
			if (settings.appClass.equals(Applinks.class)) {
				intent = Applinks.viewForGetIntent(getSherlockActivity(), mEntityId, settings.linkType, settings.direction);
			}
			if (settings.appClass.equals(Comments.class)) {
				intent = Comments.viewForGetIntent(getSherlockActivity(), mEntityId, settings.linkType, settings.direction);
			}
			if (settings.appClass.equals(Places.class)) {
				intent = Places.viewForGetIntent(getSherlockActivity(), mEntityId, settings.linkType, settings.direction);
			}
			if (settings.appClass.equals(Pictures.class)) {
				intent = Pictures.viewForGetIntent(getSherlockActivity(), mEntityId, settings.linkType, settings.direction);
			}
			if (settings.appClass.equals(Users.class)) {
				intent = Users.viewForGetIntent(getSherlockActivity(), mEntityId, settings.linkType, settings.direction);
			}

			/*
			 * Make button shortcut
			 */
			Shortcut shortcut = Shortcut.builder(mEntity
					, Constants.SCHEMA_INTENT
					, Constants.TYPE_APP_INTENT
					, null
					, getString(moreResId)
					, "resource:img_more"
					, 10
					, false
					, true);
			shortcut.intent = intent;
			shortcuts = shortcuts.subList(0, flowLimit - 1);
			shortcuts.add(shortcut);
		}

		final FlowLayout flow = (FlowLayout) section.findViewById(R.id.flow_shortcuts);
		flowShortcuts(flow, shortcuts.size() > flowLimit
				? shortcuts.subList(0, flowLimit)
				: shortcuts, flowItemResId);

		if (getView() != null) {
			((ViewGroup) getView().findViewById(holderId)).addView(holder);
		}

	}

	private void flowShortcuts(FlowLayout layout, List<Shortcut> shortcuts, Integer viewResId) {

		layout.removeAllViews();

		final DisplayMetrics metrics = getResources().getDisplayMetrics();
		final View parentView = (View) layout.getParent();
		Integer layoutWidthPixels = metrics.widthPixels
				- (parentView.getPaddingLeft() + parentView.getPaddingRight() + layout.getPaddingLeft() + layout.getPaddingRight());
		final Integer bonusPadding = UI.getRawPixelsForDisplayPixels(getSherlockActivity(), 20);
		layoutWidthPixels -= bonusPadding;

		final Integer spacing = 3;
		final Integer spacingHorizontalPixels = UI.getRawPixelsForDisplayPixels(getSherlockActivity(), spacing);
		final Integer spacingVerticalPixels = UI.getRawPixelsForDisplayPixels(getSherlockActivity(), spacing);

		Integer desiredWidthPixels = (int) (metrics.density * 75);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			desiredWidthPixels = (int) (metrics.density * 75);
		}

		final Integer candiCount = (int) Math.ceil(layoutWidthPixels / desiredWidthPixels);
		final Integer candiWidthPixels = (layoutWidthPixels - (spacingHorizontalPixels * (candiCount - 1))) / candiCount;

		final Integer candiHeightPixels = (candiWidthPixels * 1);

		layout.setSpacingHorizontal(spacingHorizontalPixels);
		layout.setSpacingVertical(spacingVerticalPixels);

		for (Shortcut shortcut : shortcuts) {

			if (!shortcut.isActive(mEntity)) {
				continue;
			}

			View view = LayoutInflater.from(getSherlockActivity()).inflate(viewResId, null);
			AirImageView photoView = (AirImageView) view.findViewById(R.id.photo);

			TextView name = (TextView) view.findViewById(R.id.name);
			TextView badgeUpper = (TextView) view.findViewById(R.id.badge_upper);
			TextView badgeLower = (TextView) view.findViewById(R.id.badge_lower);
			ImageView indicator = (ImageView) view.findViewById(R.id.indicator);
			if (indicator != null) indicator.setVisibility(View.GONE);
			if (badgeUpper != null) badgeUpper.setVisibility(View.GONE);
			if (badgeLower != null) badgeLower.setVisibility(View.GONE);
			name.setVisibility(View.GONE);

			view.setTag(shortcut);

			if (shortcut.group != null && shortcut.group.size() > 1) {
				badgeUpper.setText(String.valueOf(shortcut.group.size()));
				badgeUpper.setVisibility(View.VISIBLE);

				if (shortcut.app.equals(Constants.TYPE_APP_FACEBOOK)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_facebook_dark);
					if (((BaseActivity) getSherlockActivity()).getThemeTone().equals("light")) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_facebook_light);
					}
				}
				else if (shortcut.app.equals(Constants.TYPE_APP_TWITTER)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_twitter_dark);
					if (((BaseActivity) getSherlockActivity()).getThemeTone().equals("light")) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_twitter_light);
					}
				}
				else if (shortcut.app.equals(Constants.TYPE_APP_WEBSITE)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_website_dark);
					if (((BaseActivity) getSherlockActivity()).getThemeTone().equals("light")) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_website_light);
					}
				}
				else if (shortcut.app.equals(Constants.TYPE_APP_FOURSQUARE)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_foursquare_dark);
					if (((BaseActivity) getSherlockActivity()).getThemeTone().equals("light")) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_foursquare_dark);
					}
				}
				badgeLower.setVisibility(View.VISIBLE);
			}
			else if (shortcut.count != null && shortcut.count > 0) {
				badgeUpper.setTag(shortcut);
				badgeUpper.setText(String.valueOf(shortcut.count));
				badgeUpper.setVisibility(View.VISIBLE);
			}

			/* Show hint if source has app that hasn't been installed */
			final ShortcutMeta meta = Shortcut.shortcutMeta.get(shortcut.app);
			if (meta != null && !meta.installDeclined
					&& shortcut.getIntentSupport()
					&& shortcut.appExists()
					&& !shortcut.appInstalled()) {

				/* Show install indicator */
				if (indicator != null) {
					indicator.setVisibility(View.VISIBLE);
				}
			}

			if (shortcut.name != null && !shortcut.name.equals("")) {
				name.setText(shortcut.name);
				name.setVisibility(View.VISIBLE);
			}

			photoView.setTag(shortcut);
			photoView.setSizeHint(candiWidthPixels);
			photoView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					final Shortcut shortcut = (Shortcut) view.getTag();
					Routing.route(getSherlockActivity(), Route.Shortcut, mEntity, shortcut, null, null);
				}
			});

			UI.drawPhoto(photoView, shortcut.getPhoto());

			FlowLayout.LayoutParams params = new FlowLayout.LayoutParams(candiWidthPixels, LayoutParams.WRAP_CONTENT);
			params.setCenterHorizontal(false);
			view.setLayoutParams(params);

			RelativeLayout.LayoutParams paramsImage = new RelativeLayout.LayoutParams(candiWidthPixels, candiHeightPixels);
			photoView.setLayoutParams(paramsImage);

			layout.addView(view);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	public void onResume() {
		super.onResume();
		databind(BindingMode.auto);
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

}