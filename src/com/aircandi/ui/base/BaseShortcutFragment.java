package com.aircandi.ui.base;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.aircandi.service.objects.CacheStamp;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.ServiceBase;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutMeta;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public abstract class BaseShortcutFragment extends BaseFragment {

	protected String		mShortcutType;
	protected TextView		mMessage;
	protected LinkProfile	mLinkProfile;
	protected ScrollView	mScrollView;

	protected String		mEntityId;
	protected Entity		mEntity;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		mScrollView = (ScrollView) view.findViewById(R.id.scroll_view);
		mMessage = (TextView) view.findViewById(R.id.message);
		return view;
	}

	@Override
	public void beforeDatabind() {
		/*
		 * If cache entity is fresher than the one currently bound to or there is
		 * a cache entity available, go ahead and draw before we check against the service.
		 */
		mCacheStamp = null;
		mEntity = EntityManager.getEntity(mEntityId);
		if (mEntity != null) {
			mCacheStamp = mEntity.getCacheStamp();
			draw();
		}
	}

	@Override
	public void databind(final BindingMode mode) {

		final AtomicBoolean refreshNeeded = new AtomicBoolean(false);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				beforeDatabind();
			}

			@Override
			protected Object doInBackground(Object... params) {

				Thread.currentThread().setName("GetEntityForShortcuts");
				ModelResult result = new ModelResult();

				if (mEntity != null && mEntity.synthetic) {
					return result;
				}

				refreshNeeded.set(mCacheStamp == null || mEntity == null);

				if (!refreshNeeded.get()) {
					CacheStamp cacheStamp = EntityManager.getInstance().loadCacheStamp(mEntity.id, mCacheStamp);
					/*
					 * We refresh for both modified and activity because both can change what we
					 * show for an entity including links and link shortcuts.
					 */
					if (cacheStamp != null && !cacheStamp.equals(mCacheStamp)) {
						refreshNeeded.set(true);
					}
				}

				if (refreshNeeded.get()) {
					showBusy();
					LinkOptions options = LinkOptions.getDefault(mLinkProfile);
					result = EntityManager.getInstance().getEntity(mEntityId, true, options);
				}

				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {

				if (isAdded()) {
					hideBusy();
					if (refreshNeeded.get()) {
						final ModelResult result = (ModelResult) modelResult;
						if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
							if (result.data != null) {
								mEntity = (Entity) result.data;
								mCacheStamp = mEntity.getCacheStamp();
								draw();
							}
						}
						else {
							Errors.handleError(getSherlockActivity(), result.serviceResponse);
							return;
						}
					}
					else if (mode == BindingMode.SERVICE) {
						showBusyTimed(Constants.INTERVAL_FAKE_BUSY, false);
					}
					afterDatabind();
				}
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void draw() {

		/* Clear shortcut holder */
		if (getView() == null) return;

		((ViewGroup) getView().findViewById(R.id.shortcut_holder)).removeAllViews();

		if (mShortcutType.equals(Constants.TYPE_LINK_CREATE)) {

			Boolean empty = true;

			/* Shortcuts for place entities created by user */
			ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PLACE, Direction.out, null, false, false);
			settings.appClass = Places.class;
			List<Shortcut> shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new ServiceBase.SortByPositionSortDate(), null);
			if (shortcuts.size() > 0) {
				empty = false;
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_places_created
						, R.string.section_places_more
						, mResources.getInteger(R.integer.limit_shortcuts_flow)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}

			/* Shortcuts for place entities created by user */
			settings = new ShortcutSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_CANDIGRAM, Direction.out, null, false, false);
			settings.appClass = Candigrams.class;
			shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new ServiceBase.SortByPositionSortDate(), null);
			if (shortcuts.size() > 0) {
				empty = false;
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_candigrams_created
						, R.string.section_candigrams_more
						, mResources.getInteger(R.integer.limit_shortcuts_flow)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}

			/* Shortcuts for post entities created by user */
			settings = new ShortcutSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PICTURE, Direction.out, null, false, false);
			settings.appClass = Pictures.class;
			shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new ServiceBase.SortByPositionSortDate(), null);
			if (shortcuts.size() > 0) {
				empty = false;
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_pictures_created
						, R.string.section_pictures_more
						, mResources.getInteger(R.integer.limit_shortcuts_flow)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}

			showMessage(empty);
		}

		else if (mShortcutType.equals(Constants.TYPE_LINK_WATCH)) {

			Boolean empty = true;
			
			/* Watching places */
			ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, Direction.out, null, false, false);
			settings.appClass = Places.class;
			List<Shortcut> shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new ServiceBase.SortByPositionSortDate(), null);
			if (shortcuts.size() > 0) {
				empty = false;
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_places_watching
						, R.string.section_places_more
						, mResources.getInteger(R.integer.limit_shortcuts_flow)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}

			/* Watching candigrams */
			settings = new ShortcutSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_CANDIGRAM, Direction.out, null, false, false);
			settings.appClass = Candigrams.class;
			shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new ServiceBase.SortByPositionSortDate(), null);
			if (shortcuts.size() > 0) {
				empty = false;
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_candigrams_watching
						, R.string.section_pictures_more
						, mResources.getInteger(R.integer.limit_shortcuts_flow)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}

			/* Watching pictures */
			settings = new ShortcutSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PICTURE, Direction.out, null, false, false);
			settings.appClass = Pictures.class;
			shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new ServiceBase.SortByPositionSortDate(), null);
			if (shortcuts.size() > 0) {
				empty = false;
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_pictures_watching
						, R.string.section_pictures_more
						, mResources.getInteger(R.integer.limit_shortcuts_flow)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}

			/* Watching users */
			settings = new ShortcutSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, Direction.out, null, false, false);
			settings.appClass = Users.class;
			shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, new ServiceBase.SortByPositionSortDate(), null);
			if (shortcuts.size() > 0) {
				empty = false;
				drawShortcuts(shortcuts
						, settings
						, R.string.section_user_shortcuts_users_watching
						, R.string.section_users_more
						, mResources.getInteger(R.integer.limit_shortcuts_flow)
						, R.id.shortcut_holder
						, R.layout.temp_place_switchboard_item);
			}
			
			showMessage(empty);
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
				intent = Applinks.viewForGetIntent(getSherlockActivity(), mEntityId, settings.linkType, settings.direction,
						titleResId != null ? getString(titleResId) : null);
			}
			if (settings.appClass.equals(Comments.class)) {
				intent = Comments.viewForGetIntent(getSherlockActivity(), mEntityId, settings.linkType, settings.direction,
						titleResId != null ? getString(titleResId) : null);
			}
			if (settings.appClass.equals(Places.class)) {
				intent = Places.viewForGetIntent(getSherlockActivity(), mEntityId, settings.linkType, settings.direction, settings.linkInactive,
						titleResId != null ? getString(titleResId) : null);
			}
			if (settings.appClass.equals(Pictures.class)) {
				intent = Pictures.viewForGetIntent(getSherlockActivity(), mEntityId, settings.linkType, settings.direction,
						titleResId != null ? getString(titleResId) : null);
			}
			if (settings.appClass.equals(Candigrams.class)) {
				intent = Candigrams.viewForGetIntent(getSherlockActivity(), mEntityId, settings.linkType, settings.direction,
						titleResId != null ? getString(titleResId) : null);
			}
			if (settings.appClass.equals(Users.class)) {
				intent = Users.viewForGetIntent(getSherlockActivity(), mEntityId, settings.linkType, settings.direction,
						titleResId != null ? getString(titleResId) : null);
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
			if ((meta == null || !meta.installDeclined)
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
					Routing.route(getSherlockActivity(), Route.SHORTCUT, mEntity, shortcut, null, null);
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
	// Events
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	protected void showMessage(Boolean visible) {
		if (mMessage != null) {
			mMessage.setVisibility(visible ? View.VISIBLE : View.GONE);
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
		databind(BindingMode.AUTO);
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

}