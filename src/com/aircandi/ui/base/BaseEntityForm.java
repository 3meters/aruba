package com.aircandi.ui.base;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.applications.Applinks;
import com.aircandi.applications.Comments;
import com.aircandi.applications.Pictures;
import com.aircandi.applications.Places;
import com.aircandi.applications.Users;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.service.objects.CacheStamp;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutMeta;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.ui.CandigramForm;
import com.aircandi.ui.PictureForm;
import com.aircandi.ui.PlaceForm;
import com.aircandi.ui.user.UserForm;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;

public abstract class BaseEntityForm extends BaseBrowse implements IForm {

	protected ScrollView			mScrollView;
	protected Entity				mEntity;
	protected LinkProfile			mLinkProfile;

	/* Inputs */
	@SuppressWarnings("ucd")
	public String					mParentId;
	public String					mEntityId;
	@SuppressWarnings("ucd")
	public String					mMessage;

	protected final PackageReceiver	mPackageReceiver	= new PackageReceiver();

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mParentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mMessage = extras.getString(Constants.EXTRA_MESSAGE);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mScrollView = (ScrollView) findViewById(R.id.scroll_view);
		mScrollView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
	}

	public void afterDatabind() {}

	public void beforeDatabind() {
		/*
		 * If cache entity is fresher than the one currently bound to or there is
		 * a cache entity available, go ahead and draw before we check against the service.
		 */
		mCacheStamp = null;
		mEntity = EntityManager.getEntity(mEntityId);
		if (mEntity != null) {
			if (mEntity instanceof Place) {
				Aircandi.getInstance().setCurrentPlace(mEntity);
				Logger.v(this, "Setting current place to: " + mEntity.id);
			}
			mCacheStamp = mEntity.getCacheStamp();
			if (mFirstDraw) {
				draw();
			}
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
				Thread.currentThread().setName("GetEntity");
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

				hideBusy();
				if (refreshNeeded.get()) {
					final ModelResult result = (ModelResult) modelResult;
					if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

						if (result.data != null) {
							mEntity = (Entity) result.data;
							if (mEntity instanceof Place) {
								Aircandi.getInstance().setCurrentPlace(mEntity);
								Logger.v(this, "Setting current place to: " + mEntity.id);
							}
							invalidateOptionsMenu();
							mCacheStamp = mEntity.getCacheStamp();
							draw();
						}
						else {
							UI.showToastNotification("This item has been deleted", Toast.LENGTH_SHORT);
							finish();
						}
					}
					else {
						Errors.handleError(BaseEntityForm.this, result.serviceResponse);
						return;
					}
				}
				else if (mode == BindingMode.SERVICE) {
					showBusyTimed(Constants.INTERVAL_FAKE_BUSY, false);
				}
				afterDatabind();
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onRefresh() {
		databind(BindingMode.SERVICE); // Called from Routing
	}

	@SuppressWarnings("ucd")
	public void onLikeButtonClick(View view) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				((ComboButton) findViewById(R.id.button_like)).getViewAnimator().setDisplayedChild(1);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("LikeEntity");
				ModelResult result = new ModelResult();
				if (!mEntity.byAppUser(Constants.TYPE_LINK_LIKE)) {
					Shortcut fromShortcut = Aircandi.getInstance().getCurrentUser().getShortcut();
					Shortcut toShortcut = mEntity.getShortcut();
					Aircandi.getInstance().getCurrentUser().activityDate = DateTime.nowDate().getTime();
					result = EntityManager.getInstance().insertLink(Aircandi.getInstance().getCurrentUser().id
							, mEntity.id
							, Constants.TYPE_LINK_LIKE
							, false
							, fromShortcut
							, toShortcut
							, Constants.TYPE_LINK_LIKE);
				}
				else {
					Aircandi.getInstance().getCurrentUser().activityDate = DateTime.nowDate().getTime();
					result = EntityManager.getInstance().deleteLink(Aircandi.getInstance().getCurrentUser().id
							, mEntity.id
							, Constants.TYPE_LINK_LIKE
							, mEntity.schema
							, "unlike");
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;
				((ComboButton) findViewById(R.id.button_like)).getViewAnimator().setDisplayedChild(0);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					drawButtons();
					drawStats();
				}
				else {
					if (result.serviceResponse.statusCodeService != null
							&& result.serviceResponse.statusCodeService != ServiceConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						Errors.handleError(BaseEntityForm.this, result.serviceResponse);
					}
				}
			}
		}.execute();

	}

	@SuppressWarnings("ucd")
	public void onWatchButtonClick(View view) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				((ComboButton) findViewById(R.id.button_watch)).getViewAnimator().setDisplayedChild(1);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("WatchEntity");
				ModelResult result = new ModelResult();
				if (!mEntity.byAppUser(Constants.TYPE_LINK_WATCH)) {
					Shortcut fromShortcut = Aircandi.getInstance().getCurrentUser().getShortcut();
					Shortcut toShortcut = mEntity.getShortcut();
					Aircandi.getInstance().getCurrentUser().activityDate = DateTime.nowDate().getTime();
					result = EntityManager.getInstance().insertLink(Aircandi.getInstance().getCurrentUser().id
							, mEntity.id
							, Constants.TYPE_LINK_WATCH
							, false
							, fromShortcut
							, toShortcut
							, Constants.TYPE_LINK_WATCH);
				}
				else {
					Aircandi.getInstance().getCurrentUser().activityDate = DateTime.nowDate().getTime();
					result = EntityManager.getInstance().deleteLink(Aircandi.getInstance().getCurrentUser().id
							, mEntity.id
							, Constants.TYPE_LINK_WATCH
							, mEntity.schema
							, "unwatch");
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;
				((ComboButton) findViewById(R.id.button_watch)).getViewAnimator().setDisplayedChild(0);
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					drawButtons();
					drawStats();
				}
				else {
					if (result.serviceResponse.statusCodeService != null
							&& result.serviceResponse.statusCodeService != ServiceConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						Errors.handleError(BaseEntityForm.this, result.serviceResponse);
					}
				}
			}
		}.execute();

	}

	@SuppressWarnings("ucd")
	public void onShortcutClick(View view) {
		final Shortcut shortcut = (Shortcut) view.getTag();
		if (!shortcut.app.equals(Constants.TYPE_APP_MAP)) {
			if (shortcut.count != null && shortcut.count == 0 && !EntityManager.canUserAdd(mEntity)) return;
		}
		Routing.route(this, Route.SHORTCUT, mEntity, shortcut, null, null);
	}

	@SuppressWarnings("ucd")
	public void onUserClick(View view) {
		Entity entity = (Entity) view.getTag();
		Routing.route(this, Route.PROFILE, entity);
	}

	@SuppressWarnings("ucd")
	public void onImageClick(View view) {
		if (mEntity.photo != null) {
			Routing.route(this, Route.PHOTO, mEntity);
		}
	}

	@SuppressWarnings("ucd")
	public void onNewCommentButtonClick(View view) {
		if (EntityManager.canUserAdd(mEntity)) {
			Routing.route(this, Route.COMMENT_NEW, mEntity);
			return;
		}

		if (mEntity.locked) {
			Dialogs.locked(this, mEntity);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		/*
		 * Cases that use activity result
		 * 
		 * - Candi picker returns entity id for a move
		 * - Template picker returns type of candi to add as a child
		 */
		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ENTITY_EDIT) {
				EntityManager.getInstance().getCacheStampOverrides().put(mParentId, mParentId);
				if (resultCode == Constants.RESULT_ENTITY_DELETED) {
					finish();
					Animate.doOverridePendingTransition(this, TransitionType.PAGE_TO_RADAR_AFTER_DELETE);
				}
			}
			else if (requestCode == Constants.ACTIVITY_APPLICATION_PICK) {

				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String entitySchema = extras.getString(Constants.EXTRA_ENTITY_SCHEMA);
					if (entitySchema != null && !entitySchema.equals("")) {

						final IntentBuilder intentBuilder = new IntentBuilder(this, BaseEntityEdit.insertFormBySchema(entitySchema))
								.setEntitySchema(entitySchema)
								.setEntityParentId(mEntityId);

						startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_INSERT);
						Animate.doOverridePendingTransition(this, TransitionType.PAGE_TO_FORM);
					}
				}
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mScrollView != null) {
			outState.putIntArray("ARTICLE_SCROLL_POSITION", new int[] { mScrollView.getScrollX(), mScrollView.getScrollY() });
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		final int[] position = savedInstanceState.getIntArray("ARTICLE_SCROLL_POSITION");
		if (position != null && mScrollView != null) {
			mScrollView.post(new Runnable() {
				@Override
				public void run() {
					mScrollView.scrollTo(position[0], position[1]);
				}
			});
		}
	}

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

	protected void drawStats() {}

	protected void drawButtons() {

		if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE) && ((Place) mEntity).synthetic) {
			UI.setVisibility(findViewById(R.id.button_holder), View.GONE);
		}
		else if (mEntity.id.equals(Aircandi.getInstance().getCurrentUser().id)) {
			UI.setVisibility(findViewById(R.id.button_holder), View.GONE);
		}
		else {
			UI.setVisibility(findViewById(R.id.button_holder), View.VISIBLE);

			ComboButton watched = (ComboButton) findViewById(R.id.button_watch);
			if (watched != null) {
				if (mEntity.byAppUser(Constants.TYPE_LINK_WATCH)) {
					final int color = Aircandi.getInstance().getResources().getColor(R.color.brand_pink_lighter);
					watched.setDrawableId(R.drawable.ic_action_show_dark);
					watched.getImageIcon().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
				}
				else {
					watched.getImageIcon().setColorFilter(null);
					if (getThemeTone().equals("dark")) {
						watched.setDrawableId(R.drawable.ic_action_show_dark);
					}
					else {
						watched.setDrawableId(R.drawable.ic_action_show_light);
					}
				}
			}

			ComboButton liked = (ComboButton) findViewById(R.id.button_like);
			if (liked != null) {
				if (mEntity.byAppUser(Constants.TYPE_LINK_LIKE)) {
					final int color = Aircandi.getInstance().getResources().getColor(R.color.accent_red);
					liked.setDrawableId(R.drawable.ic_action_heart_dark);
					liked.getImageIcon().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
				}
				else {
					liked.getImageIcon().setColorFilter(null);
					if (getThemeTone().equals("dark")) {
						liked.setDrawableId(R.drawable.ic_action_heart_dark);
					}
					else {
						liked.setDrawableId(R.drawable.ic_action_heart_light);
					}
				}
			}
		}
	}

	protected void drawShortcuts(List<Shortcut> shortcuts
			, ShortcutSettings settings
			, Integer titleResId
			, Integer moreResId
			, Integer flowLimit
			, Integer holderId
			, Integer flowItemResId) {

		View holder = LayoutInflater.from(this).inflate(R.layout.section_shortcuts, null);
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
				intent = Applinks.viewForGetIntent(this, mEntityId, settings.linkType, settings.direction, titleResId != null ? getString(titleResId) : null);
			}
			if (settings.appClass.equals(Comments.class)) {
				intent = Comments.viewForGetIntent(this, mEntityId, settings.linkType, settings.direction, titleResId != null ? getString(titleResId) : null);
			}
			if (settings.appClass.equals(Places.class)) {
				intent = Places.viewForGetIntent(this, mEntityId, settings.linkType, settings.direction, settings.linkInactive,
						titleResId != null ? getString(titleResId) : null);
			}
			if (settings.appClass.equals(Pictures.class)) {
				intent = Pictures.viewForGetIntent(this, mEntityId, settings.linkType, settings.direction, titleResId != null ? getString(titleResId) : null);
			}
			if (settings.appClass.equals(Users.class)) {
				intent = Users.viewForGetIntent(this, mEntityId, settings.linkType, settings.direction, titleResId != null ? getString(titleResId) : null);
			}

			/*
			 * Make 'more' button shortcut
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
		flowShortcuts(flow, shortcuts, flowItemResId);
		((ViewGroup) findViewById(holderId)).addView(holder);

	}

	private void flowShortcuts(FlowLayout layout, List<Shortcut> shortcuts, Integer viewResId) {

		layout.removeAllViews();

		final DisplayMetrics metrics = getResources().getDisplayMetrics();
		final View parentView = (View) layout.getParent();
		Integer layoutWidthPixels = metrics.widthPixels
				- (parentView.getPaddingLeft() + parentView.getPaddingRight() + layout.getPaddingLeft() + layout.getPaddingRight());
		final Integer bonusPadding = UI.getRawPixelsForDisplayPixels(this, 20);
		layoutWidthPixels -= bonusPadding;

		final Integer spacing = 3;
		final Integer spacingHorizontalPixels = UI.getRawPixelsForDisplayPixels(this, spacing);
		final Integer spacingVerticalPixels = UI.getRawPixelsForDisplayPixels(this, spacing);

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

			View view = LayoutInflater.from(this).inflate(viewResId, null);
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
					if (getThemeTone().equals("light")) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_facebook_light);
					}
				}
				else if (shortcut.app.equals(Constants.TYPE_APP_TWITTER)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_twitter_dark);
					if (getThemeTone().equals("light")) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_twitter_light);
					}
				}
				else if (shortcut.app.equals(Constants.TYPE_APP_WEBSITE)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_website_dark);
					if (getThemeTone().equals("light")) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_website_light);
					}
				}
				else if (shortcut.app.equals(Constants.TYPE_APP_FOURSQUARE)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_foursquare_dark);
					if (getThemeTone().equals("light")) {
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
	// Methods
	// --------------------------------------------------------------------------------------------

	public static Class<?> viewFormBySchema(String schema) {
		if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			return PlaceForm.class;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
			return CandigramForm.class;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
			return PictureForm.class;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
			return UserForm.class;
		}
		return null;
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		/*
		 * Setup menu items that are common for entity forms.
		 */
		mMenuItemAdd = menu.findItem(R.id.add);
		mMenuItemEdit = menu.findItem(R.id.edit);
		if (mMenuItemEdit != null) {
			mMenuItemEdit.setVisible(UI.showAction(Route.EDIT, mEntity));
		}
		if (mMenuItemAdd != null) {
			mMenuItemAdd.setVisible(UI.showAction(Route.ADD, mEntity));
		}

		MenuItem refresh = menu.findItem(R.id.refresh);
		if (refresh != null) {
			if (mBusyManager != null) {
				mBusyManager.setRefreshImage(refresh.getActionView().findViewById(R.id.refresh_image));
				mBusyManager.setRefreshProgress(refresh.getActionView().findViewById(R.id.refresh_progress));
			}

			refresh.getActionView().findViewById(R.id.refresh_frame).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Tracker.sendEvent("ui_action", "form_refresh_by_user", mEntity.schema, 0, Aircandi.getInstance().getCurrentUser());
					onRefresh();
				}
			});
		}

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (mMenuItemEdit != null) {
			mMenuItemEdit.setVisible(UI.showAction(Route.EDIT, mEntity));
		}
		if (mMenuItemAdd != null) {
			mMenuItemAdd.setVisible(UI.showAction(Route.ADD, mEntity));
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return Routing.route(this, Routing.routeForMenuId(item.getItemId()), mEntity);
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		/*
		 * We have to be pretty aggressive about refreshing the UI because
		 * there are lots of actions that could have happened while this activity
		 * was stopped that change what the user would expect to see.
		 * 
		 * - Entity deleted or modified
		 * - Entity children modified
		 * - New comments
		 * - Change in user which effects which candi and UI should be visible.
		 * - User profile could have been updated and we don't catch that.
		 */
		if (!isFinishing()) {
			if (mEntity instanceof Place) {
				Aircandi.getInstance().setCurrentPlace(mEntity);
				Logger.v(this, "Setting current place to: " + mEntity.id);
			}

			Animate.doOverridePendingTransition(this, TransitionType.PAGE_BACK);
			databind(BindingMode.AUTO);	// check to see if the cache stamp is stale

			/* Package receiver */
			final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
			filter.addDataScheme("package");
			registerReceiver(mPackageReceiver, filter);
		}
	}

	@Override
	protected void onPause() {
		try {
			unregisterReceiver(mPackageReceiver);
		}
		catch (Exception e) {} // $codepro.audit.disable emptyCatchClause
		super.onPause();
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	private class PackageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(final Context context, Intent intent) {

			/* This is on the main UI thread */
			if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
				final String publicName = AndroidManager.getInstance().getPublicName(intent.getData().getEncodedSchemeSpecificPart());
				if (publicName != null) {
					UI.showToastNotification(publicName + " " + getText(R.string.dialog_install_toast_package_installed),
							Toast.LENGTH_SHORT);
					Aircandi.mainThreadHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							databind(BindingMode.AUTO);
						}
					}, 1500);
				}
			}
		}
	}

}