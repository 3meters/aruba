package com.aircandi.ui;

import java.util.ArrayList;
import java.util.Collections;
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
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
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

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.FontManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutMeta;
import com.aircandi.service.objects.User;
import com.aircandi.ui.EntityList.ListMode;
import com.aircandi.ui.base.CandiActivity;
import com.aircandi.ui.builders.ShortcutPicker;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;

public class CandiForm extends CandiActivity {

	private ScrollView				mScrollView;
	private ViewGroup				mContentView;
	private ViewGroup				mCandiForm;
	private Entity					mEntity;
	private Number					mEntityModelRefreshDate;
	private Number					mEntityModelActivityDate;
	private Boolean					mUpsize;
	private Boolean					mForceRefresh		= false;
	private static Resources		mResources;
	private final PackageReceiver	mPackageReceiver	= new PackageReceiver();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			initialize();
			bind(mForceRefresh);
		}
	}

	private void initialize() {
		final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mContentView = (ViewGroup) findViewById(R.id.candi_body);

		Integer candiFormResId = R.layout.candi_form_base;
		if (mCommon.mEntitySchema == null) {
			mCommon.mEntitySchema = Constants.SCHEMA_ENTITY_PLACE;
		}
		if (mCommon.mEntitySchema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			candiFormResId = R.layout.candi_form_place;
		}

		final ViewGroup body = (ViewGroup) inflater.inflate(candiFormResId, null);

		mCandiForm = (ViewGroup) body.findViewById(R.id.candi_form);
		mScrollView = (ScrollView) body.findViewById(R.id.scroll_view);
		mResources = getResources();

		mContentView.addView(body, 0);

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mUpsize = extras.getBoolean(Constants.EXTRA_UPSIZE_SYNTHETIC);
			mForceRefresh = extras.getBoolean(Constants.EXTRA_REFRESH_FORCE);
		}

	}

	private void bind(final Boolean refreshProposed) {
		/*
		 * Navigation setup for action bar icon and title
		 */
		Logger.d(this, "Binding candi form");
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy(true);
				mCommon.startBodyBusyIndicator();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("GetEntity");

				Entity entity = EntityManager.getEntity(mCommon.mEntityId);
				Boolean refresh = refreshProposed;

				if (entity == null || (!entity.shortcuts && !mUpsize)) {
					refresh = true;
				}

				final ModelResult result = EntityManager.getInstance().getEntity(mCommon.mEntityId, refresh, null);
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
						mCommon.mActionBar.setTitle(mEntity.name);
						if (mCommon.mMenuItemEdit != null) {
							mCommon.mMenuItemEdit.setVisible(canEdit());
						}
						View view = findViewById(R.id.button_add_candigram);
						if (view != null) {
							view.setVisibility(canAdd() ? View.VISIBLE : View.GONE);
						}

						/* Action bar icon */
						if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE) && ((Place) mEntity).category != null) {
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
												mCommon.mActionBar.setIcon(new BitmapDrawable(Aircandi.applicationContext.getResources(), imageResponse.bitmap));
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
					mCommon.hideBusy(false);

					/* Run help if it hasn't been run yet */
					if (mCommon.mEntitySchema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
						final Boolean runOnceHelp = Aircandi.settings.getBoolean(Constants.SETTING_RUN_ONCE_HELP_CANDI_PLACE, false);
						if (!runOnceHelp) {
							//							mCommon.doHelpClick();
							//							return;
						}
					}
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiForm);
					mCommon.hideBusy(true);
				}
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
				mCommon.showBusy(true);
				mCommon.startBodyBusyIndicator();
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
				mCommon.hideBusy(true);
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					final Entity upsizedEntity = (Entity) result.data;
					mCommon.mEntityId = upsizedEntity.id;
					bind(false);
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.Tuning);
				}
			}
		}.execute();
	}

	public void doRefresh() {
		/*
		 * Called from AircandiCommon
		 */
		bind(true);
	}

	private void draw() {
		buildCandiForm(mEntity, mContentView, mCommon.mMenu, null, false);
		mCandiForm.setVisibility(View.VISIBLE);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onCallButtonClick(View view) {
		Tracker.sendEvent("ui_action", "call_place", null, 0, Aircandi.getInstance().getUser());
		AndroidManager.getInstance().callDialerActivity(this, ((Place) mEntity).phone);
	}

	@SuppressWarnings("ucd")
	public void onLikeButtonClick(View view) {
		Tracker.sendEvent("ui_action", "like_place", null, 0, Aircandi.getInstance().getUser());

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
					Tracker.sendEvent("ui_action", "unlike_entity", null, 0, Aircandi.getInstance().getUser());
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
				mCommon.hideBusy(true);
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					bind(false);
				}
				else {
					if (result.serviceResponse.exception.getStatusCode() == ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						ImageUtils.showToastNotification(getString(R.string.toast_like_duplicate), Toast.LENGTH_SHORT);
					}
					else {
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiForm);
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
					Tracker.sendEvent("ui_action", "watch_entity", null, 0, Aircandi.getInstance().getUser());
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
					Tracker.sendEvent("ui_action", "unwatch_entity", null, 0, Aircandi.getInstance().getUser());
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
				mCommon.hideBusy(true);
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					bind(false);
				}
				else {
					if (result.serviceResponse.exception.getStatusCode() != ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiForm);
					}
				}
			}
		}.execute();

	}

	public void onMoreButtonClick(View view) {
		String target = (String) view.getTag();
		if (target.equals("candi")) {
			IntentBuilder intentBuilder = new IntentBuilder(this, EntityList.class)
					.setListMode(ListMode.EntitiesForEntity)
					.setEntityId(mCommon.mEntityId);

			Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.PageToPage);
		}
	}

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {}

	@SuppressWarnings("ucd")
	public void onShortcutClick(View view) {
		final Shortcut shortcut = (Shortcut) view.getTag();
		doShortcutClick(shortcut);
	}

	private void doShortcutClick(Shortcut shortcut) {

		final ShortcutMeta meta = Shortcut.shortcutMeta.get(shortcut.app);
		if (meta != null && !meta.installDeclined
				&& shortcut.getIntentSupport()
				&& shortcut.appExists()
				&& !shortcut.appInstalled()) {
			showInstallDialog(shortcut);
		}

		if (shortcut.group != null && shortcut.group.size() > 1) {
			final Intent intent = new Intent(this, ShortcutPicker.class);
			final List<String> shortcutStrings = new ArrayList<String>();
			for (Shortcut item : shortcut.group) {
				shortcutStrings.add(HttpService.convertObjectToJsonSmart(item, false, true));
			}
			intent.putStringArrayListExtra(Constants.EXTRA_SHORTCUTS, (ArrayList<String>) shortcutStrings);
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
		}
		else {
			mCommon.routeShortcut(shortcut, mEntity);
		}
	}

	@SuppressWarnings("ucd")
	public void onImageClick(View view) {
		Intent intent = null;
		final Photo photo = mEntity.photo;
		photo.setCreatedAt(mEntity.modifiedDate.longValue());
		photo.setTitle(mEntity.name);
		photo.setUser(mEntity.creator);
		EntityManager.getInstance().getPhotos().clear();
		EntityManager.getInstance().getPhotos().add(photo);
		intent = new Intent(this, PictureDetail.class);
		intent.putExtra(Constants.EXTRA_URI, mEntity.photo.getUri());
		intent.putExtra(Constants.EXTRA_PAGING_ENABLED, false);

		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToPage);
	}

	public void onUserClick(View view) {
		User user = (User) view.getTag();
		mCommon.doUserClick(user);
	}

	public void onAddCandigramButtonClick(View view) {
		doAddCandigram();
	}

	public void doAddCandigram() {
		if (!mEntity.locked || mEntity.ownerId.equals(Aircandi.getInstance().getUser().id)) {
			Tracker.sendEvent("ui_action", "add_candigram", null, 0, Aircandi.getInstance().getUser());
			final IntentBuilder intentBuilder = new IntentBuilder(this, EntityEdit.class)
					.setEntityId(null)
					.setParentEntityId(mCommon.mEntityId)
					.setEntitySchema(Constants.SCHEMA_ENTITY_POST);

			final Intent redirectIntent = intentBuilder.create();
			startActivity(redirectIntent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
		}
		else {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.alert_entity_locked)
					, null
					, this, android.R.string.ok, null, null, null, null);
		}
	}

	@SuppressWarnings("ucd")
	public void onNewCommentButtonClick(View view) {
		if (!mEntity.locked || mEntity.ownerId.equals(Aircandi.getInstance().getUser().id)) {
			final IntentBuilder intentBuilder = new IntentBuilder(this, CommentForm.class);
			intentBuilder.setEntityId(null);
			intentBuilder.setParentEntityId(mEntity.id);
			final Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
		}
		else {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, mResources.getString(R.string.alert_entity_locked)
					, null
					, this, android.R.string.ok, null, null, null, null);
		}
	}

	@SuppressWarnings("ucd")
	public void onTuneButtonClick(View view) {
		if (!mEntity.locked || mEntity.ownerId.equals(Aircandi.getInstance().getUser().id)) {
			final IntentBuilder intentBuilder = new IntentBuilder(this, TuningWizard.class);
			intentBuilder.setEntityId(mEntity.id);
			final Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
		}
		else {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, mResources.getString(R.string.alert_entity_locked)
					, null
					, this, android.R.string.ok, null, null, null, null);
		}
	}

	@SuppressWarnings("ucd")
	public void onInstallButtonClick(View view) {
		final Shortcut shortcut = (Shortcut) view.getTag();
		showInstallDialog(shortcut);
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
					AnimUtils.doOverridePendingTransition(this, TransitionType.PageToRadarAfterDelete);
				}
			}
			else if (requestCode == Constants.ACTIVITY_TEMPLATE_PICK) {
				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String entityType = extras.getString(Constants.EXTRA_ENTITY_SCHEMA);
					if (entityType != null && !entityType.equals("")) {

						final IntentBuilder intentBuilder = new IntentBuilder(this, EntityEdit.class)
								.setEntityId(null)
								.setParentEntityId(mCommon.mEntityId)
								.setEntitySchema(entityType);

						final Intent redirectIntent = intentBuilder.create();
						startActivity(redirectIntent);
						AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
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

	private ViewGroup buildCandiForm(final Entity entity, final ViewGroup layout, Menu menu, AirLocation mLocation, boolean refresh) { // $codepro.audit.disable largeNumberOfParameters
		/*
		 * For now, we assume that the candi form isn't recycled.
		 * 
		 * We leave most of the views visible by default so they are visible in the layout editor.
		 * 
		 * - WebImageView primary image is visible by default
		 * - WebImageView child views are gone by default
		 * - Header views are visible by default
		 */
		final CandiView candiView = (CandiView) layout.findViewById(R.id.candi_view);
		final WebImageView image = (WebImageView) layout.findViewById(R.id.candi_form_image);
		final TextView title = (TextView) layout.findViewById(R.id.candi_form_title);
		final TextView subtitle = (TextView) layout.findViewById(R.id.candi_form_subtitle);

		final TextView description = (TextView) layout.findViewById(R.id.candi_form_description);
		final TextView address = (TextView) layout.findViewById(R.id.candi_form_address);
		final Button addCandigram = (Button) layout.findViewById(R.id.button_add_candigram);
		final UserView user_one = (UserView) layout.findViewById(R.id.user_one);
		final UserView user_two = (UserView) layout.findViewById(R.id.user_two);

		if (candiView != null) {
			/*
			 * This is a place entity with a fancy image widget
			 */
			candiView.bindToPlace((Place) entity);
		}
		else {
			setVisibility(image, View.GONE);
			if (image != null) {
				final String imageUri = entity.getPhotoUri();
				if (imageUri != null) {
					if (entity.creator == null || !imageUri.equals(entity.creator.getPhotoUri())) {

						final BitmapRequestBuilder builder = new BitmapRequestBuilder(image).setImageUri(imageUri);
						final BitmapRequest imageRequest = builder.create();

						image.setBitmapRequest(imageRequest);
						image.setClickable(false);

						if (entity.schema.equals(Constants.SCHEMA_ENTITY_POST)) {
							image.setClickable(true);
						}
						setVisibility(image, View.VISIBLE);
					}
				}
			}

			title.setText(null);
			subtitle.setText(null);

			setVisibility(title, View.GONE);
			if (title != null && entity.name != null && !entity.name.equals("")) {
				title.setText(Html.fromHtml(entity.name));
				FontManager.getInstance().setTypefaceDefault(title);
				setVisibility(title, View.VISIBLE);
			}

			setVisibility(subtitle, View.GONE);
			if (subtitle != null && entity.subtitle != null && !entity.subtitle.equals("")) {
				subtitle.setText(Html.fromHtml(entity.subtitle));
				FontManager.getInstance().setTypefaceDefault(subtitle);
				setVisibility(subtitle, View.VISIBLE);
			}
		}

		/* Primary candi image */

		description.setText(null);

		setVisibility(layout.findViewById(R.id.section_description), View.GONE);
		if (description != null && entity.description != null && !entity.description.equals("")) {
			FontManager.getInstance().setTypefaceDefault(description);
			description.setText(Html.fromHtml(entity.description));
			setVisibility(layout.findViewById(R.id.section_description), View.VISIBLE);
		}

		/* Clear shortcut holder */
		((ViewGroup) layout.findViewById(R.id.shortcut_holder)).removeAllViews();

		/* Synthetic applink shortcuts */
		List<Shortcut> shortcuts = (List<Shortcut>) entity.getShortcuts(Constants.TYPE_LINK_APPLINK, Direction.in, true, true);
		if (shortcuts.size() > 0) {
			drawShortcuts(layout
					, shortcuts
					, R.string.candi_section_shortcuts_place
					, R.string.candi_section_links_more
					, mResources.getInteger(R.integer.candi_flow_limit)
					, R.layout.temp_place_switchboard_item);
		}

		/* Service applink shortcuts */
		shortcuts = (List<Shortcut>) entity.getShortcuts(Constants.TYPE_LINK_APPLINK, Direction.in, false, true);
		Collections.sort(shortcuts, new Shortcut.SortByPosition());

		if (shortcuts.size() > 0) {
			drawShortcuts(layout
					, shortcuts
					, null
					, null
					, mResources.getInteger(R.integer.candi_flow_limit)
					, R.layout.temp_place_switchboard_item);
		}

		/* Candigram button */
		FontManager.getInstance().setTypefaceRegular(addCandigram);

		/* Place specific info */
		if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			final Place place = (Place) entity;

			setVisibility(address, View.GONE);
			if (address != null) {
				String addressBlock = place.getAddressBlock();

				if (place.phone != null) {
					addressBlock += "<br/>" + place.getFormattedPhone();
				}

				if (!"".equals(addressBlock)) {
					address.setText(Html.fromHtml(addressBlock));
					FontManager.getInstance().setTypefaceDefault(address);
					setVisibility(address, View.VISIBLE);
				}
			}
		}

		/* Creator block */

		setVisibility(user_one, View.GONE);
		setVisibility(user_two, View.GONE);
		UserView user = user_one;

		if (user != null
				&& entity.creator != null
				&& !entity.creator.id.equals(ProxiConstants.ADMIN_USER_ID)) {

			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				if (((Place) entity).getProvider().type.equals("aircandi")) {
					user.setLabel(getString(R.string.candi_label_user_created_by));
					user.bindToUser(entity.creator, entity.createdDate.longValue(), entity.locked);
					setVisibility(user, View.VISIBLE);
					user = user_two;
				}
			}
			else {
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_POST)) {
					user.setLabel(getString(R.string.candi_label_user_added_by));
				}
				else {
					user.setLabel(getString(R.string.candi_label_user_created_by));
				}
				user.bindToUser(entity.creator, entity.createdDate.longValue(), entity.locked);
				setVisibility(user_one, View.VISIBLE);
				user = user_two;
			}
		}

		/* Editor block */

		if (user != null && entity.modifier != null && !entity.modifier.id.equals(ProxiConstants.ADMIN_USER_ID)) {
			if (entity.createdDate.longValue() != entity.modifiedDate.longValue()) {
				user.setLabel(getString(R.string.candi_label_user_edited_by));
				user.bindToUser(entity.modifier, entity.modifiedDate.longValue(), null);
				setVisibility(user, View.VISIBLE);
			}
		}

		/* Buttons */
		buildCandiButtons(entity, layout, menu, mLocation);

		return layout;
	}

	private void drawShortcuts(ViewGroup layout, List<Shortcut> shortcuts, Integer titleResId, Integer moreResId, Integer flowLimit, Integer flowItemResId) {
		final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View holder = inflater.inflate(R.layout.section_shortcuts, null);
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
			View footer = inflater.inflate(R.layout.temp_section_footer, null);
			Button button = (Button) footer.findViewById(R.id.button_more);
			FontManager.getInstance().setTypefaceDefault(button);
			button.setText(moreResId);
			button.setTag("candi");
			section.setFooter(footer); // Replaces if there already is one.
		}

		final FlowLayout flow = (FlowLayout) section.findViewById(R.id.flow_shortcuts);
		flowShortcuts(flow, shortcuts.size() > flowLimit
				? shortcuts.subList(0, flowLimit)
				: shortcuts, flowItemResId);

		((ViewGroup) layout.findViewById(R.id.shortcut_holder)).addView(holder);

	}

	private void flowShortcuts(FlowLayout layout, List<Shortcut> shortcuts, Integer viewResId) {

		layout.removeAllViews();
		final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final DisplayMetrics metrics = getResources().getDisplayMetrics();
		final View parentView = (View) layout.getParent();
		Integer layoutWidthPixels = metrics.widthPixels
				- (parentView.getPaddingLeft() + parentView.getPaddingRight() + layout.getPaddingLeft() + layout.getPaddingRight());
		final Integer bonusPadding = ImageUtils.getRawPixels(this, 20);
		layoutWidthPixels -= bonusPadding;

		final Integer spacing = 3;
		final Integer spacingHorizontalPixels = ImageUtils.getRawPixels(this, spacing);
		final Integer spacingVerticalPixels = ImageUtils.getRawPixels(this, spacing);

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

			TextView title = (TextView) view.findViewById(R.id.title);
			TextView badgeUpper = (TextView) view.findViewById(R.id.badge_upper);
			TextView badgeLower = (TextView) view.findViewById(R.id.badge_lower);
			ImageView indicator = (ImageView) view.findViewById(R.id.indicator);
			if (indicator != null) indicator.setVisibility(View.GONE);
			if (badgeUpper != null) badgeUpper.setVisibility(View.GONE);
			if (badgeLower != null) badgeLower.setVisibility(View.GONE);
			title.setVisibility(View.GONE);

			view.setTag(shortcut);

			FontManager.getInstance().setTypefaceRegular(title);
			FontManager.getInstance().setTypefaceDefault(badgeUpper);

			if (shortcut.group != null && shortcut.group.size() > 1) {
				badgeUpper.setText(String.valueOf(shortcut.group.size()));
				badgeUpper.setVisibility(View.VISIBLE);

				if (shortcut.app.equals(Constants.TYPE_APPLINK_FACEBOOK)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_facebook_dark);
					if (mCommon.mThemeTone.equals("light")) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_facebook_light);
					}
				}
				else if (shortcut.app.equals(Constants.TYPE_APPLINK_TWITTER)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_twitter_dark);
					if (mCommon.mThemeTone.equals("light")) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_twitter_light);
					}
				}
				else if (shortcut.app.equals(Constants.TYPE_APPLINK_WEBSITE)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_website_dark);
					if (mCommon.mThemeTone.equals("light")) {
						badgeLower.setBackgroundResource(R.drawable.ic_action_website_light);
					}
				}
				else if (shortcut.app.equals(Constants.TYPE_APPLINK_FOURSQUARE)) {
					badgeLower.setBackgroundResource(R.drawable.ic_action_foursquare_dark);
					if (mCommon.mThemeTone.equals("light")) {
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
				title.setText(shortcut.name);
				title.setVisibility(View.VISIBLE);
			}

			String imageUri = shortcut.photo.getUri();
			if (imageUri != null) {
				BitmapRequestBuilder builder = new BitmapRequestBuilder(webImageView).setImageUri(imageUri);
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

	private void buildCandiButtons(final Entity entity, final ViewGroup layout, Menu menu, AirLocation mLocation) {

		setVisibility(layout.findViewById(R.id.button_tune), View.GONE);

		if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE) && ((Place) entity).synthetic) {
			setVisibility(layout.findViewById(R.id.button_like), View.GONE);
			setVisibility(layout.findViewById(R.id.button_watch), View.GONE);
		}
		else {
			setVisibility(layout.findViewById(R.id.button_like), View.VISIBLE);
			setVisibility(layout.findViewById(R.id.button_watch), View.VISIBLE);
		}

		ComboButton watched = (ComboButton) layout.findViewById(R.id.button_watch);
		if (watched != null) {
			if (entity.byAppUser(Constants.TYPE_LINK_WATCH)) {
				final int color = Aircandi.getInstance().getResources().getColor(R.color.brand_pink_lighter);
				watched.setLabel(getString(R.string.candi_button_unwatch));
				watched.setDrawableId(R.drawable.ic_action_show_dark);
				watched.setAlpha(1);
				watched.getImageIcon().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
			}
			else {
				watched.setLabel(getString(R.string.candi_button_watch));
				watched.getImageIcon().setColorFilter(null);
				if (mCommon.mThemeTone.equals("dark")) {
					watched.setDrawableId(R.drawable.ic_action_show_dark);
				}
				else {
					watched.setDrawableId(R.drawable.ic_action_show_light);
				}
			}
		}

		ComboButton liked = (ComboButton) layout.findViewById(R.id.button_like);
		if (liked != null) {
			if (entity.byAppUser(Constants.TYPE_LINK_LIKE)) {
				final int color = Aircandi.getInstance().getResources().getColor(R.color.accent_red);
				liked.setLabel(getString(R.string.candi_button_unlike));
				liked.setDrawableId(R.drawable.ic_action_heart_dark);
				liked.getImageIcon().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
			}
			else {
				liked.setLabel(getString(R.string.candi_button_like));
				liked.getImageIcon().setColorFilter(null);
				if (mCommon.mThemeTone.equals("dark")) {
					liked.setDrawableId(R.drawable.ic_action_heart_dark);
				}
				else {
					liked.setDrawableId(R.drawable.ic_action_heart_light);
				}
			}
		}

		if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			Place place = (Place) entity;

			/* Tune, map, like */
			if (!place.synthetic) {
				setVisibility(layout.findViewById(R.id.button_tune), View.VISIBLE);
			}

			if (place.synthetic) {
				setVisibility(layout.findViewById(R.id.form_footer), View.GONE);
				return;
			}
			else {
				setVisibility(layout.findViewById(R.id.form_footer), View.VISIBLE);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mCommon.doCreateOptionsMenu(menu);
		mCommon.mMenuItemEdit.setVisible(canEdit());
		View view = findViewById(R.id.button_add_candigram);
		if (view != null) {
			findViewById(R.id.button_add_candigram).setVisibility(canAdd() ? View.VISIBLE : View.GONE);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.edit) {
			Tracker.sendEvent("ui_action", "edit_entity", null, 0, Aircandi.getInstance().getUser());
			mCommon.doEditCandiClick();
			return true;
		}

		/* In case we add general menu items later */
		mCommon.doOptionsItemSelected(item);
		return true;
	}

	private Boolean canEdit() {
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

	private Boolean canAdd() {
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
		if (!isFinishing() && mEntity != null) {
			AnimUtils.doOverridePendingTransition(this, TransitionType.PageBack);
			if (ProximityManager.getInstance().getLastBeaconLoadDate() != null
					&& ProximityManager.getInstance().getLastBeaconLoadDate().longValue() > mEntityModelRefreshDate.longValue()
					|| EntityManager.getEntityCache().getLastActivityDate() != null
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
	// Dialogs
	// --------------------------------------------------------------------------------------------

	private void showInstallDialog(final Shortcut shortcut) {

		final AlertDialog installDialog = AircandiCommon.showAlertDialog(null
				, getString(R.string.dialog_install_title)
				, getString(R.string.dialog_install_message)
				, null
				, this
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
								startActivity(intent);
							}
							catch (Exception e) {
								/*
								 * In case the market app isn't installed on the phone
								 */
								Logger.d(this, "Install: navigating to play website install page");
								final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://play.google.com/store/apps/details?id="
										+ shortcut.getPackageName() + "&referrer=utm_source%3Dcom.aircandi"));
								intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
								startActivityForResult(intent, Constants.ACTIVITY_MARKET);
							}
							dialog.dismiss();
							AnimUtils.doOverridePendingTransition(CandiForm.this, TransitionType.PageToForm);
						}
						else if (which == Dialog.BUTTON_NEGATIVE) {
							final ShortcutMeta meta = Shortcut.shortcutMeta.get(shortcut.app);
							meta.installDeclined = true;
							doShortcutClick(shortcut);
							dialog.dismiss();
						}
					}
				}
				, null);
		installDialog.setCanceledOnTouchOutside(false);
		installDialog.show();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.candi_form;
	}

	public Entity getEntity() {
		return mEntity;
	}

	public void setEntity(Entity entity) {
		mEntity = entity;
	}

	private class PackageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(final Context context, Intent intent) {

			/* This is on the main UI thread */
			if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
				final String publicName = AndroidManager.getInstance().getPublicName(intent.getData().getEncodedSchemeSpecificPart());
				if (publicName != null) {
					ImageUtils.showToastNotification(publicName + " " + getText(R.string.dialog_install_toast_package_installed),
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