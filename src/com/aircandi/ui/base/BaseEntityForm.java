package com.aircandi.ui.base;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.BusyManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ExcludeNulls;
import com.aircandi.service.HttpService.UseAnnotations;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutMeta;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.ui.HelpForm;
import com.aircandi.ui.PictureForm;
import com.aircandi.ui.PlaceForm;
import com.aircandi.ui.PostForm;
import com.aircandi.ui.WatchForm;
import com.aircandi.ui.base.BaseEntityList.ListMode;
import com.aircandi.ui.edit.CommentEdit;
import com.aircandi.ui.helpers.ShortcutPicker;
import com.aircandi.ui.user.UserForm;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;

public abstract class BaseEntityForm extends BaseActivity {

	protected ScrollView			mScrollView;
	protected ViewGroup				mBodyHolder;
	protected ViewGroup				mFormHolder;
	protected ViewGroup				mFooterHolder;
	protected Entity				mEntity;
	protected Number				mEntityModelRefreshDate;
	protected Number				mEntityModelActivityDate;

	public String					mParentId;
	public String					mEntityId;
	public String					mEntitySchema;
	public String					mMessage;

	protected Boolean				mForceRefresh		= false;
	protected final PackageReceiver	mPackageReceiver	= new PackageReceiver();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			initialize(savedInstanceState);
			bind(mForceRefresh);
		}
	}

	protected void initialize(Bundle savedInstanceState) {

		mActionBar.setDisplayHomeAsUpEnabled(true);
		mFormHolder = (ViewGroup) findViewById(R.id.form_holder);
		mBodyHolder = (ViewGroup) findViewById(R.id.body_holder);
		mFooterHolder = (ViewGroup) findViewById(R.id.footer_holder);
		mScrollView = (ScrollView) findViewById(R.id.scroll_view);
		mBusyManager = new BusyManager(this);
	}

	@Override
	protected void unpackIntent() {

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {

			mForceRefresh = extras.getBoolean(Constants.EXTRA_REFRESH_FORCE);
			mParentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			mEntitySchema = extras.getString(Constants.EXTRA_ENTITY_SCHEMA);
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mMessage = extras.getString(Constants.EXTRA_MESSAGE);
		}
	}

	protected abstract void bind(final Boolean refreshProposed);

	public void doRefresh() {
		bind(true); // Called from AircandiCommon
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

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
		@SuppressWarnings("unused")
		ShortcutSettings settings = (ShortcutSettings) view.getTag();
		IntentBuilder intentBuilder = null;

		intentBuilder = new IntentBuilder(this, BaseEntityList.class)
				.setListMode(ListMode.EntitiesForEntity)
				.setEntityId(mEntityId);

		Intent intent = intentBuilder.create();
		startActivity(intent);
		Animate.doOverridePendingTransition(this, TransitionType.PageToPage);
	}

	@SuppressWarnings("ucd")
	public void onShortcutClick(View view) {
		final Shortcut shortcut = (Shortcut) view.getTag();
		doShortcutClick(shortcut, mEntity);
	}

	protected void doShortcutClick(Shortcut shortcut, Entity entity) {

		final ShortcutMeta meta = Shortcut.shortcutMeta.get(shortcut.app);
		if (meta != null && !meta.installDeclined
				&& shortcut.getIntentSupport()
				&& shortcut.appExists()
				&& !shortcut.appInstalled()) {
			showInstallDialog(this, shortcut, entity);
		}

		if (shortcut.group != null && shortcut.group.size() > 1) {
			IntentBuilder intentBuilder = new IntentBuilder(this, ShortcutPicker.class).setEntity(entity);
			final Intent intent = intentBuilder.create();
			final List<String> shortcutStrings = new ArrayList<String>();
			for (Shortcut item : shortcut.group) {
				Shortcut clone = item.clone();
				clone.group = null;
				shortcutStrings.add(HttpService.objectToJson(clone, UseAnnotations.False, ExcludeNulls.True));
			}
			intent.putStringArrayListExtra(Constants.EXTRA_SHORTCUTS, (ArrayList<String>) shortcutStrings);
			startActivity(intent);
			Animate.doOverridePendingTransition(this, TransitionType.PageToForm);
		}
		else {
			Routing.shortcut(this, shortcut, entity);
		}
	}

	public void onUserClick(View view) {
		Entity entity = (Entity) view.getTag();
		doUserClick(entity);
	}

	public void doUserClick(Entity entity) {
		if (entity != null) {
			Intent intent = new Intent(this, UserForm.class);
			intent.putExtra(Constants.EXTRA_ENTITY_ID, entity.id);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			Animate.doOverridePendingTransition(this, TransitionType.RadarToPage);
		}
	}

	@SuppressWarnings("ucd")
	public void onImageClick(View view) {
		Intent intent = null;
		final Photo photo = mEntity.photo;
		photo.setCreatedAt(mEntity.modifiedDate.longValue());
		photo.setName(mEntity.name);
		photo.setUser(mEntity.creator);
		EntityManager.getInstance().getPhotos().clear();
		EntityManager.getInstance().getPhotos().add(photo);
		intent = new Intent(this, PictureForm.class);
		intent.putExtra(Constants.EXTRA_URI, mEntity.photo.getUri());
		intent.putExtra(Constants.EXTRA_PAGING_ENABLED, false);

		startActivity(intent);
		Animate.doOverridePendingTransition(this, TransitionType.PageToPage);
	}

	public void doWatchingClick() {
		Intent intent = new Intent(this, WatchForm.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(intent);
		Animate.doOverridePendingTransition(this, TransitionType.RadarToPage);
	}

	public void doEditClick() {
		IntentBuilder intentBuilder = new IntentBuilder(this, BaseEntityEdit.editFormBySchema(mEntity.schema)).setEntity(mEntity);
		startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_EDIT);
		Animate.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	public void doHelpClick() {
		Intent intent = new Intent(this, HelpForm.class);
		if (mPageName.equals("RadarForm")) {
			intent.putExtra(Constants.EXTRA_HELP_ID, R.layout.radar_help);
		}
		else if (mPageName.equals("PlaceForm") && mEntitySchema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			intent.putExtra(Constants.EXTRA_HELP_ID, R.layout.place_help);
		}
		startActivity(intent);
		Animate.doOverridePendingTransition(this, TransitionType.PageToHelp);
	}

	@SuppressWarnings("ucd")
	public void onNewCommentButtonClick(View view) {
		if (!mEntity.locked || mEntity.ownerId.equals(Aircandi.getInstance().getUser().id)) {
			final IntentBuilder intentBuilder = new IntentBuilder(this, CommentEdit.class);
			intentBuilder.setEntityId(null);
			intentBuilder.setEntityParentId(mEntity.id);
			final Intent intent = intentBuilder.create();
			startActivity(intent);
			Animate.doOverridePendingTransition(this, TransitionType.PageToForm);
		}
		else {
			Dialogs.showAlertDialog(android.R.drawable.ic_dialog_alert
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
	// UI routines
	// --------------------------------------------------------------------------------------------

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

	private void showInstallDialog(final Activity activity, final Shortcut shortcut, final Entity entity) {

		final AlertDialog installDialog = Dialogs.showAlertDialog(null
				, activity.getString(R.string.dialog_install_title)
				, activity.getString(R.string.dialog_install_message)
				, null
				, activity
				, R.string.dialog_install_ok
				, R.string.dialog_install_cancel
				, null
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							try {
								Tracker.sendEvent("ui_action", "install_source", shortcut.getPackageName(), 0, Aircandi.getInstance().getUser());
								Logger.d(this, "Install: navigating to market install page");
								final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=" + shortcut.getPackageName()
										+ "&referrer=utm_source%3Dcom.aircandi"));
								intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
								activity.startActivity(intent);
							}
							catch (Exception e) {
								/*
								 * In case the market app isn't installed on the phone
								 */
								Logger.d(this, "Install: navigating to play website install page");
								final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://play.google.com/store/apps/details?id="
										+ shortcut.getPackageName() + "&referrer=utm_source%3Dcom.aircandi"));
								intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
								activity.startActivityForResult(intent, Constants.ACTIVITY_MARKET);
							}
							dialog.dismiss();
							Animate.doOverridePendingTransition(activity, TransitionType.PageToForm);
						}
						else if (which == Dialog.BUTTON_NEGATIVE) {
							final ShortcutMeta meta = Shortcut.shortcutMeta.get(shortcut.app);
							meta.installDeclined = true;
							doShortcutClick(shortcut, entity);
							dialog.dismiss();
						}
					}
				}
				, null);
		installDialog.setCanceledOnTouchOutside(false);
		installDialog.show();
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
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/*
		 * Android 2.3 or lower: called when user hits the menu button for the first time.
		 * Android 3.0 or higher: called when activity is first started.
		 * 
		 * Behavior might be modified because we are using ABS.
		 */
		final SherlockActivity activity = (SherlockActivity) this;
		if (mPageName.equals("RadarForm")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_radar, menu);
		}
		else if (mPageName.equals("PlaceForm") || mPageName.equals("PostForm")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_candi, menu);
			activity.getSupportMenuInflater().inflate(R.menu.menu_base, menu);
		}
		else if (mPageName.equals("EntityList")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_entity_list, menu);
			activity.getSupportMenuInflater().inflate(R.menu.menu_base, menu);
		}
		else if (mPageName.equals("UserForm")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_user, menu);
		}
		else if (mPageName.equals("HelpForm")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_help, menu);
		}

		/* Cache edit and delete menus because we need to toggle it later */
		mMenuItemEdit = menu.findItem(R.id.edit);
		mMenuItemEdit.setVisible(canEdit());

		/* Cache refresh menu item for later ui updates */
		mMenuItemRefresh = menu.findItem(R.id.refresh);
		if (mMenuItemRefresh != null) {
			if (mBusyManager != null) {
				mBusyManager.setRefreshImage(mMenuItemRefresh.getActionView().findViewById(R.id.refresh_image));
				mBusyManager.setRefreshProgress(mMenuItemRefresh.getActionView().findViewById(R.id.refresh_progress));
			}
			mMenuItemRefresh.getActionView().findViewById(R.id.refresh_frame).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					doRefresh();
				}
			});
		}

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		/*
		 * Android 2.3 or lower: called every time the user hits the menu button.
		 * Android 3.0 or higher: called when invalidateOptionsMenu is called.
		 * 
		 * Behavior might be modified because we are using ABS.
		 */
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.edit) {
			Tracker.sendEvent("ui_action", "edit_" + mEntity.schema, null, 0, Aircandi.getInstance().getUser());
			doEditClick();
			return true;
		}
		else if (item.getItemId() == R.id.help) {
			doHelpClick();
			return true;
		}
		else if (item.getItemId() == R.id.profile) {
			doUserClick(Aircandi.getInstance().getUser());
			return true;
		}
		else if (item.getItemId() == R.id.watching) {
			doWatchingClick();
			return true;
		}

		/* In case we add general menu items later */
		super.onOptionsItemSelected(item);
		return true;
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