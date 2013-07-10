package com.aircandi.ui.base;

import java.util.List;

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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutMeta;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.ui.PlaceForm;
import com.aircandi.ui.PostForm;
import com.aircandi.ui.user.UserForm;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;

public abstract class BaseEntityForm extends BaseBrowse {

	protected ScrollView			mScrollView;
	protected ViewGroup				mBodyHolder;
	protected ViewGroup				mFormHolder;
	protected ViewGroup				mFooterHolder;
	protected Entity				mEntity;
	protected Number				mEntityModelRefreshDate;
	protected Number				mEntityModelActivityDate;

	/* Inputs */
	protected Boolean				mForceRefresh		= false;
	public String					mParentId;
	public String					mEntityId;
	public String					mEntitySchema;
	public String					mMessage;

	protected final PackageReceiver	mPackageReceiver	= new PackageReceiver();

	@Override
	protected void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mParentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			mEntitySchema = extras.getString(Constants.EXTRA_ENTITY_SCHEMA);
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mMessage = extras.getString(Constants.EXTRA_MESSAGE);
		}
	}

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		
		mFormHolder = (ViewGroup) findViewById(R.id.form_holder);
		mBodyHolder = (ViewGroup) findViewById(R.id.body_holder);
		mFooterHolder = (ViewGroup) findViewById(R.id.footer_holder);
		mScrollView = (ScrollView) findViewById(R.id.scroll_view);
	}

	@Override
	protected void configureActionBar() {}
	
	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------
	
	@Override
	public void onAdd() {}

	@SuppressWarnings("ucd")
	public void onLikeButtonClick(View view) {
		Tracker.sendEvent("ui_action", "like_" + mEntity.schema, null, 0, Aircandi.getInstance().getUser());

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("LikeEntity");
				ModelResult result = new ModelResult();
				if (!mEntity.byAppUser(Constants.TYPE_LINK_LIKE)) {
					Tracker.sendEvent("ui_action", "like_entity", null, 0, Aircandi.getInstance().getUser());
					Shortcut shortcut = Aircandi.getInstance().getUser().getShortcut();
					result = EntityManager.getInstance().insertLink(Aircandi.getInstance().getUser().id
							, mEntity.id
							, Constants.TYPE_LINK_LIKE
							, false
							, shortcut
							, Constants.TYPE_LINK_LIKE);
				}
				else {
					Tracker.sendEvent("ui_action", "unlike_" + mEntity.schema, null, 0, Aircandi.getInstance().getUser());
					result = EntityManager.getInstance().deleteLink(Aircandi.getInstance().getUser().id
							, mEntity.id
							, Constants.TYPE_LINK_LIKE
							, "unlike");
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;
				setSupportProgressBarIndeterminateVisibility(false);
				mBusyManager.hideBusy();
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					bind(false);
				}
				else {
					if (result.serviceResponse.exception.getStatusCode() == ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						UI.showToastNotification(getString(R.string.toast_like_duplicate), Toast.LENGTH_SHORT);
					}
					else {
						Routing.serviceError(BaseEntityForm.this, result.serviceResponse);
					}
				}
			}
		}.execute();

	}

	@SuppressWarnings("ucd")
	public void onWatchButtonClick(View view) {

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("WatchEntity");
				ModelResult result = new ModelResult();
				if (!mEntity.byAppUser(Constants.TYPE_LINK_WATCH)) {
					Tracker.sendEvent("ui_action", "watch_" + mEntity.schema, null, 0, Aircandi.getInstance().getUser());
					Shortcut shortcut = Aircandi.getInstance().getUser().getShortcut();
					result = EntityManager.getInstance().insertLink(
							Aircandi.getInstance().getUser().id
							, mEntity.id
							, Constants.TYPE_LINK_WATCH
							, false
							, shortcut
							, Constants.TYPE_LINK_WATCH);
				}
				else {
					Tracker.sendEvent("ui_action", "unwatch_" + mEntity.schema, null, 0, Aircandi.getInstance().getUser());
					result = EntityManager.getInstance().deleteLink(Aircandi.getInstance().getUser().id
							, mEntity.id
							, Constants.TYPE_LINK_WATCH
							, "unwatch");
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;
				setSupportProgressBarIndeterminateVisibility(false);
				mBusyManager.hideBusy();
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					bind(false);
				}
				else {
					if (result.serviceResponse.exception.getStatusCode() != ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						Routing.serviceError(BaseEntityForm.this, result.serviceResponse);
					}
				}
			}
		}.execute();

	}

	public void onMoreButtonClick(View view) {
		ShortcutSettings settings = (ShortcutSettings) view.getTag();
		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_LIST_SCHEMA, settings.targetSchema);
		Routing.route(this, Route.EntityList, mEntity, null, null, extras);
	}

	@SuppressWarnings("ucd")
	public void onShortcutClick(View view) {
		final Shortcut shortcut = (Shortcut) view.getTag();
		Routing.route(this, Route.Shortcut, mEntity, shortcut);
	}

	public void onUserClick(View view) {
		Entity entity = (Entity) view.getTag();
		Routing.route(this, Route.Profile, entity);
	}

	@SuppressWarnings("ucd")
	public void onImageClick(View view) {
		Routing.route(this, Route.Photo, mEntity);
	}

	@SuppressWarnings("ucd")
	public void onNewCommentButtonClick(View view) {
		if (!mEntity.locked || mEntity.ownerId.equals(Aircandi.getInstance().getUser().id)) {
			Routing.route(this, Route.CommentNew, mEntity);
		}
		else {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, mResources.getString(R.string.alert_entity_locked)
					, null
					, this, android.R.string.ok, null, null, null, null);
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
				if (resultCode == Constants.RESULT_ENTITY_DELETED) {
					finish();
					Animate.doOverridePendingTransition(this, TransitionType.PageToRadarAfterDelete);
				}
			}
			else if (requestCode == Constants.ACTIVITY_TEMPLATE_PICK) {
				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String entitySchema = extras.getString(Constants.EXTRA_ENTITY_SCHEMA);
					if (entitySchema != null && !entitySchema.equals("")) {

						final IntentBuilder intentBuilder = new IntentBuilder(this, BaseEntityEdit.editFormBySchema(entitySchema))
								.setEntitySchema(entitySchema)
								.setEntityParentId(mEntityId);

						startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_INSERT);
						Animate.doOverridePendingTransition(this, TransitionType.PageToForm);
					}
				}
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putIntArray("ARTICLE_SCROLL_POSITION", new int[] { mScrollView.getScrollX(), mScrollView.getScrollY() });
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		final int[] position = savedInstanceState.getIntArray("ARTICLE_SCROLL_POSITION");
		if (position != null) {
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

	@Override
	protected abstract void draw();

	protected void drawButtons() {

		if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE) && ((Place) mEntity).synthetic) {
			UI.setVisibility(findViewById(R.id.button_like), View.GONE);
			UI.setVisibility(findViewById(R.id.button_watch), View.GONE);
		}
		else {
			UI.setVisibility(findViewById(R.id.button_like), View.VISIBLE);
			UI.setVisibility(findViewById(R.id.button_watch), View.VISIBLE);
		}

		ComboButton watched = (ComboButton) findViewById(R.id.button_watch);
		if (watched != null) {
			if (mEntity.byAppUser(Constants.TYPE_LINK_WATCH)) {
				final int color = Aircandi.getInstance().getResources().getColor(R.color.brand_pink_lighter);
				watched.setLabel(getString(R.string.candi_button_unwatch));
				watched.setDrawableId(R.drawable.ic_action_show_dark);
				watched.setAlpha(1);
				watched.getImageIcon().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
			}
			else {
				watched.setLabel(getString(R.string.candi_button_watch));
				watched.getImageIcon().setColorFilter(null);
				if (mThemeTone.equals("dark")) {
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
				liked.setLabel(getString(R.string.candi_button_unlike));
				liked.setDrawableId(R.drawable.ic_action_heart_dark);
				liked.getImageIcon().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
			}
			else {
				liked.setLabel(getString(R.string.candi_button_like));
				liked.getImageIcon().setColorFilter(null);
				if (mThemeTone.equals("dark")) {
					liked.setDrawableId(R.drawable.ic_action_heart_dark);
				}
				else {
					liked.setDrawableId(R.drawable.ic_action_heart_light);
				}
			}
		}
	}

	protected void drawShortcuts(List<Shortcut> shortcuts, ShortcutSettings settings, Integer titleResId, Integer moreResId, Integer flowLimit,
			Integer flowItemResId) {

		View holder = mInflater.inflate(R.layout.section_shortcuts, null);
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
			View footer = mInflater.inflate(R.layout.temp_section_footer, null);
			Button button = (Button) footer.findViewById(R.id.button_more);
			button.setText(moreResId);
			button.setTag(settings);
			section.setFooter(footer); // Replaces if there already is one.
		}

		final FlowLayout flow = (FlowLayout) section.findViewById(R.id.flow_shortcuts);
		flowShortcuts(flow, shortcuts.size() > flowLimit
				? shortcuts.subList(0, flowLimit)
				: shortcuts, flowItemResId);

		((ViewGroup) findViewById(R.id.shortcut_holder)).addView(holder);

	}

	private void flowShortcuts(FlowLayout layout, List<Shortcut> shortcuts, Integer viewResId) {

		layout.removeAllViews();
		final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final DisplayMetrics metrics = getResources().getDisplayMetrics();
		final View parentView = (View) layout.getParent();
		Integer layoutWidthPixels = metrics.widthPixels
				- (parentView.getPaddingLeft() + parentView.getPaddingRight() + layout.getPaddingLeft() + layout.getPaddingRight());
		final Integer bonusPadding = UI.getRawPixels(this, 20);
		layoutWidthPixels -= bonusPadding;

		final Integer spacing = 3;
		final Integer spacingHorizontalPixels = UI.getRawPixels(this, spacing);
		final Integer spacingVerticalPixels = UI.getRawPixels(this, spacing);

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

			View view = inflater.inflate(viewResId, null);
			WebImageView webImageView = (WebImageView) view.findViewById(R.id.photo);

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

				if (shortcut.app.equals(Constants.TYPE_APPLINK_FACEBOOK)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_facebook_dark);
					if (mThemeTone.equals("light")) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_facebook_light);
					}
				}
				else if (shortcut.app.equals(Constants.TYPE_APPLINK_TWITTER)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_twitter_dark);
					if (mThemeTone.equals("light")) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_twitter_light);
					}
				}
				else if (shortcut.app.equals(Constants.TYPE_APPLINK_WEBSITE)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_website_dark);
					if (mThemeTone.equals("light")) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_website_light);
					}
				}
				else if (shortcut.app.equals(Constants.TYPE_APPLINK_FOURSQUARE)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_foursquare_dark);
					if (mThemeTone.equals("light")) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_foursquare_dark);
					}
				}
				badgeLower.setVisibility(View.VISIBLE);
			}
			else if (shortcut.count > 0) {
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

			String photoUri = shortcut.photo.getUri();
			if (photoUri != null) {
				BitmapRequestBuilder builder = new BitmapRequestBuilder(webImageView).setImageUri(photoUri);
				BitmapRequest imageRequest = builder.create();
				webImageView.setSizeHint(candiWidthPixels);
				webImageView.setBitmapRequest(imageRequest);
				webImageView.setTag(shortcut);
			}

			FlowLayout.LayoutParams params = new FlowLayout.LayoutParams(candiWidthPixels, LayoutParams.WRAP_CONTENT);
			params.setCenterHorizontal(false);
			view.setLayoutParams(params);

			RelativeLayout.LayoutParams paramsImage = new RelativeLayout.LayoutParams(candiWidthPixels, candiHeightPixels);
			webImageView.setLayoutParams(paramsImage);

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
		else if (schema.equals(Constants.SCHEMA_ENTITY_POST)) {
			return PostForm.class;
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
		mMenuItemEdit = menu.findItem(R.id.edit);
		if (mMenuItemEdit != null) {
			mMenuItemEdit.setVisible(canEdit());
		}

		MenuItem refresh = menu.findItem(R.id.refresh);
		if (refresh != null) {
			if (mBusyManager != null) {
				mBusyManager.setAccuracyIndicator(refresh.getActionView().findViewById(R.id.accuracy_indicator));
				mBusyManager.setRefreshImage(refresh.getActionView().findViewById(R.id.refresh_image));
				mBusyManager.setRefreshProgress(refresh.getActionView().findViewById(R.id.refresh_progress));
				mBusyManager.updateAccuracyIndicator();
			}
			
			refresh.getActionView().findViewById(R.id.refresh_frame).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					onRefresh();
				}
			});
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		return Routing.route(this, Routing.routeForMenu(menuItem), mEntity);
	}

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

	protected Boolean canAdd() {
		if (mEntity != null && mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			return true;
		}
		return false;
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
			Animate.doOverridePendingTransition(this, TransitionType.PageBack);
			if (mEntityModelRefreshDate != null
					&& ProximityManager.getInstance().getLastBeaconLoadDate() != null
					&& ProximityManager.getInstance().getLastBeaconLoadDate().longValue() > mEntityModelRefreshDate.longValue()) {
				invalidateOptionsMenu();
				bind(true);
			}
			else if (mEntityModelActivityDate != null
					&& EntityManager.getEntityCache().getLastActivityDate() != null
					&& EntityManager.getEntityCache().getLastActivityDate().longValue() > mEntityModelActivityDate.longValue()) {
				invalidateOptionsMenu();
				bind(true);
			}

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
	// Misc routines
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
							bind(false);
						}
					}, 1500);
				}
			}
		}
	}

}