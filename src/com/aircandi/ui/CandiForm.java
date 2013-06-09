package com.aircandi.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import android.view.ViewStub;
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
import com.aircandi.components.CommandType;
import com.aircandi.components.FontManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.ProxiManager.ArrayListType;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.GeoLocation;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.User;
import com.aircandi.ui.base.CandiActivity;
import com.aircandi.ui.builders.LinkPicker;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.ComboButton;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;
import static java.util.Arrays.asList;

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
		if (mCommon.mEntityType == null) {
			mCommon.mEntityType = Constants.SCHEMA_ENTITY_PLACE;
		}
		if (mCommon.mEntityType.equals(Constants.SCHEMA_ENTITY_PLACE)) {
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

				Entity entity = ProxiManager.getInstance().getEntityModel().getCacheEntity(mCommon.mEntityId);
				Boolean refresh = refreshProposed;
				/*
				 * We always force refresh if we are missing children or comments. Won't have an effect
				 * if this is still a synthetic place.
				 */
				List<String> types = asList(Constants.TYPE_LINK_POST, Constants.TYPE_LINK_APPLINK);
				Integer childCount = entity.getInCount(types);
				Integer loadedCount = entity.getChildrenByLinkType(types).size();

				if (entity == null || loadedCount < childCount) {
					refresh = true;
				}

				final ModelResult result = ProxiManager.getInstance().getEntityModel().getEntity(mCommon.mEntityId, refresh, null, null);
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					if (result.data != null) {
						mEntity = (Entity) result.data;
						mEntityModelRefreshDate = ProxiManager.getInstance().getEntityModel().getLastBeaconRefreshDate();
						mEntityModelActivityDate = ProxiManager.getInstance().getEntityModel().getLastActivityDate();
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
					if (mCommon.mEntityType.equals(Constants.SCHEMA_ENTITY_PLACE)) {
						final Boolean runOnceHelp = Aircandi.settings.getBoolean(Constants.SETTING_RUN_ONCE_HELP_CANDI_PLACE, false);
						if (!runOnceHelp) {
							mCommon.doHelpClick();
							return;
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
				final ModelResult result = ProxiManager.getInstance().upsizeSynthetic((Place) mEntity, null, null);
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
					result = ProxiManager.getInstance().getEntityModel().verbSomething(Aircandi.getInstance().getUser().id, mEntity.id, "like", "like");
				}
				else {
					Tracker.sendEvent("ui_action", "unlike_entity", null, 0, Aircandi.getInstance().getUser());
					result = ProxiManager.getInstance().getEntityModel().unverbSomething(Aircandi.getInstance().getUser().id, mEntity.id, "like", "unlike");
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
					result = ProxiManager.getInstance().getEntityModel().verbSomething(Aircandi.getInstance().getUser().id, mEntity.id, "watch", "watch");
				}
				else {
					Tracker.sendEvent("ui_action", "unwatch_entity", null, 0, Aircandi.getInstance().getUser());
					result = ProxiManager.getInstance().getEntityModel().unverbSomething(Aircandi.getInstance().getUser().id, mEntity.id, "watch", "unwatch");
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
			IntentBuilder intentBuilder = new IntentBuilder(this, CandiList.class);
			intentBuilder.setCommandType(CommandType.View)
					.setArrayListType(ArrayListType.InCollection)
					.setCollectionId(mEntity.id);

			Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.PageToPage);
		}
	}

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {}

	@SuppressWarnings("ucd")
	public void onCandiClick(View view) {
		List<Entity> entities = null;
		final Entity entity = (Entity) view.getTag();
		TextView badgeUpper = (TextView) ((View) view.getParent()).findViewById(R.id.badge_upper);
		if (badgeUpper != null) {
			entities = (List<Entity>) badgeUpper.getTag();
		}
		doCandiClick(entity, entities);
	}

	private void doCandiClick(Entity entity, List<Entity> entities) {

		if (entities != null && entities.size() > 1) {
			Entity first = entities.get(0);
			if (first.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
				Applink applink = (Applink) first;
				final Applink meta = ProxiManager.getInstance().getEntityModel().getApplinkMeta().get(applink.type);
				if (meta != null && !meta.installDeclined
						&& meta.intentSupport
						&& applink.appExists()
						&& !applink.appInstalled()) {
					showInstallDialog(applink);
				}
			}
			else {
				final Intent intent = new Intent(this, LinkPicker.class);
				final List<String> entityStrings = new ArrayList<String>();
				for (Entity sourceEntity : entities) {
					entityStrings.add(HttpService.convertObjectToJsonSmart(sourceEntity, false, true));
				}
				intent.putStringArrayListExtra(Constants.EXTRA_ENTITIES, (ArrayList<String>) entityStrings);
				startActivity(intent);
				AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
			}
		}
		else {
			if (entity.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
				Applink applink = (Applink) entity;
				final Applink meta = ProxiManager.getInstance().getEntityModel().getApplinkMeta().get(applink.type);
				if (meta != null && !meta.installDeclined
						&& meta.intentSupport
						&& applink.appExists()
						&& !applink.appInstalled()) {
					showInstallDialog(applink);
				}
				else {
					mCommon.routeApplink(applink, mEntity);
				}
			}
			else {
				mCommon.showCandiFormForEntity(entity, CandiForm.class);
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onImageClick(View view) {
		Intent intent = null;
		final Photo photo = mEntity.photo;
		photo.setCreatedAt(mEntity.modifiedDate.longValue());
		photo.setTitle(mEntity.name);
		photo.setUser(mEntity.creator);
		ProxiManager.getInstance().getEntityModel().getPhotos().clear();
		ProxiManager.getInstance().getEntityModel().getPhotos().add(photo);
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
			final IntentBuilder intentBuilder = new IntentBuilder(this, EntityForm.class)
					.setCommandType(CommandType.New)
					.setEntityId(null)
					.setParentEntityId(mCommon.mEntityId)
					.setEntityType(Constants.SCHEMA_ENTITY_POST);

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
		final Entity entity = (Entity) view.getTag();
		showInstallDialog((Applink) entity);
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
					final String entityType = extras.getString(Constants.EXTRA_ENTITY_TYPE);
					if (entityType != null && !entityType.equals("")) {

						final IntentBuilder intentBuilder = new IntentBuilder(this, EntityForm.class)
								.setCommandType(CommandType.New)
								.setEntityId(null)
								.setParentEntityId(mCommon.mEntityId)
								.setEntityType(entityType);

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

	private ViewGroup buildCandiForm(final Entity entity, final ViewGroup layout, Menu menu, GeoLocation mLocation, boolean refresh) { // $codepro.audit.disable largeNumberOfParameters
		/*
		 * For now, we assume that the candi form isn't recycled.
		 * 
		 * We leave most of the views visible by default so they are visible in the layout editor.
		 * 
		 * - WebImageView primary image is visible by default
		 * - WebImageView child views are gone by default
		 * - Header views are visible by default
		 */
		final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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

						if (entity.type.equals(Constants.SCHEMA_ENTITY_POST)) {
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

		/* Switchboard - applink entities */
		setVisibility(layout.findViewById(R.id.section_layout_switchboard), View.GONE);
		List<Entity> entities = (List<Entity>) entity.getChildrenByLinkType(Constants.TYPE_LINK_APPLINK);
		if (entities.size() > 0) {

			final SectionLayout section = (SectionLayout) layout.findViewById(R.id.section_layout_switchboard);
			if (section != null) {
				final FlowLayout flow = (FlowLayout) layout.findViewById(R.id.flow_switchboard);
				drawCandi(flow, entities, R.layout.temp_place_switchboard_item);
				setVisibility(layout.findViewById(R.id.section_layout_switchboard), View.VISIBLE);
			}
		}

		/* Candigram button */
		FontManager.getInstance().setTypefaceRegular(addCandigram);

		/* All non-source children */
		entities = entity.getChildren();
		if (entities.size() > 0) {
			final ViewStub stub = (ViewStub) layout.findViewById(R.id.stub_candi);
			if (stub != null) {
				((ViewStub) layout.findViewById(R.id.stub_candi)).inflate();
			}
		}

		setVisibility(layout.findViewById(R.id.section_candi), View.GONE);
		if (entities.size() > 0) {

			final SectionLayout section = (SectionLayout) layout.findViewById(R.id.section_layout_candi);
			if (section != null) {
				section.getTextViewHeader().setText(getString(R.string.candi_section_candi));

				if (entities.size() > getResources().getInteger(R.integer.candi_flow_limit)) {
					View footer = inflater.inflate(R.layout.temp_section_footer, null);
					Button button = (Button) footer.findViewById(R.id.button_more);
					FontManager.getInstance().setTypefaceDefault(button);
					button.setText(R.string.candi_section_candigrams_more);
					button.setTag("candi");
					section.setFooter(footer); // Replaces if there already is one.
				}

				final FlowLayout flow = (FlowLayout) layout.findViewById(R.id.flow_candi);
				drawCandi(flow, entities.size() > mResources.getInteger(R.integer.candi_flow_limit)
						? entities.subList(0, mResources.getInteger(R.integer.candi_flow_limit))
						: entities, R.layout.temp_place_candi_item);

				setVisibility(layout.findViewById(R.id.section_candi), View.VISIBLE);
			}
		}

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
				if (entity.type.equals(Constants.SCHEMA_ENTITY_POST)) {
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

	private void drawCandi(FlowLayout layout, List<Entity> entities, Integer viewResId) {

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

		/*
		 * Insert views for entities that we don't already have a view for
		 */
		final Map<String, List<Entity>> applinkLists = new HashMap<String, List<Entity>>();
		for (Entity entity : entities) {
			if (entity.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
				if (applinkLists.containsKey(entity.type)) {
					applinkLists.get(entity.type).add(entity);
				}
				else {
					List<Entity> sources = new ArrayList<Entity>();
					sources.add(entity);
					applinkLists.put(entity.type, sources);
				}
			}
		}

		final Map<String, Object> sources = new HashMap<String, Object>();
		for (Entity entity : entities) {

			if (entity.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
				if (sources.containsKey(entity.type)) {
					continue;
				}
				if (entity.type.equals("likes") && (mEntity.getInCount(Constants.TYPE_LINK_LIKE) == 0)) {
					continue;
				}
				if (entity.type.equals("watchers") && (mEntity.getInCount(Constants.TYPE_LINK_WATCH) == 0)) {
					continue;
				}
			}

			View view = inflater.inflate(viewResId, null);
			WebImageView webImageView = (WebImageView) view.findViewById(R.id.image);

			TextView title = (TextView) view.findViewById(R.id.title);
			TextView badgeUpper = (TextView) view.findViewById(R.id.badge_upper);
			TextView badgeLower = (TextView) view.findViewById(R.id.badge_lower);
			ImageView indicator = (ImageView) view.findViewById(R.id.indicator);

			FontManager.getInstance().setTypefaceRegular(title);
			FontManager.getInstance().setTypefaceDefault(badgeUpper);

			if (entity.type.equals(Constants.SCHEMA_ENTITY_APPLINK)) {

				Applink applink = (Applink) entity;
				indicator.setVisibility(View.GONE);
				badgeUpper.setVisibility(View.GONE);
				badgeLower.setVisibility(View.GONE);

				if (applink.type.equals(Constants.TYPE_APPLINK_COMMENT)) {
					Integer count = mEntity.getInCount(Constants.TYPE_LINK_COMMENT);
					if (count > 0) {
						badgeUpper.setText(String.valueOf(count));
						badgeUpper.setVisibility(View.VISIBLE);
					}
				}
				else if (entity.type.equals(Constants.TYPE_APPLINK_LIKE)) {
					Integer count = mEntity.getInCount(Constants.TYPE_LINK_COMMENT);
					if (count > 0) {
						badgeUpper.setText(String.valueOf(count));
						badgeUpper.setVisibility(View.VISIBLE);
					}
				}
				else if (entity.type.equals(Constants.TYPE_APPLINK_WATCH)) {
					Integer count = mEntity.getInCount(Constants.TYPE_LINK_COMMENT);
					if (count > 0) {
						badgeUpper.setText(String.valueOf(count));
						badgeUpper.setVisibility(View.VISIBLE);
					}
				}
				else {
					/* Show badge if there are multiples of the same type */
					if (applinkLists.containsKey(applink.type)) {
						Integer sourceCount = applinkLists.get(applink.type).size();
						if (sourceCount > 1) {
							badgeUpper.setTag(applinkLists.get(applink.type));
							badgeUpper.setText(String.valueOf(sourceCount));
							badgeUpper.setVisibility(View.VISIBLE);
						}
					}

					/* Show hint if source has app that hasn't been installed */
					final Applink meta = ProxiManager.getInstance().getEntityModel().getApplinkMeta().get(applink.type);
					if (meta != null && !meta.installDeclined
							&& meta.intentSupport
							&& applink.appExists()
							&& !applink.appInstalled()) {
						/* Show hint */
						indicator.setVisibility(View.VISIBLE);
					}

					/* Show hint if generic icon has been replaced with a custom one */
					if (applink.photo != null) {
						if (applink.type.equals(Constants.TYPE_APPLINK_FACEBOOK)) {
							badgeLower.setBackgroundResource(R.drawable.ic_action_facebook_dark);
							if (mCommon.mThemeTone.equals("light")) {
								badgeLower.setBackgroundResource(R.drawable.ic_action_facebook_light);
							}
						}
						else if (applink.type.equals(Constants.TYPE_APPLINK_TWITTER)) {
							badgeLower.setBackgroundResource(R.drawable.ic_action_twitter_dark);
							if (mCommon.mThemeTone.equals("light")) {
								badgeLower.setBackgroundResource(R.drawable.ic_action_twitter_light);
							}
						}
						else if (applink.type.equals(Constants.TYPE_APPLINK_WEBSITE)) {
							badgeLower.setBackgroundResource(R.drawable.ic_action_website_dark);
							if (mCommon.mThemeTone.equals("light")) {
								badgeLower.setBackgroundResource(R.drawable.ic_action_website_light);
							}
						}
						else if (applink.type.equals(Constants.TYPE_APPLINK_FOURSQUARE)) {
							badgeLower.setBackgroundResource(R.drawable.ic_action_foursquare_dark);
							if (mCommon.mThemeTone.equals("light")) {
								badgeLower.setBackgroundResource(R.drawable.ic_action_foursquare_dark);
							}
						}
					}
					badgeLower.setVisibility(View.VISIBLE);
				}
				title.setText(applink.type);
				title.setVisibility(View.VISIBLE);
			}
			else {
				if (entity.name != null && !entity.name.equals("")) {
					title.setText(entity.name);
				}
				else {
					title.setVisibility(View.GONE);
				}
			}

			String imageUri = entity.getPhotoUri();
			if (imageUri != null) {
				BitmapRequestBuilder builder = new BitmapRequestBuilder(webImageView).setImageUri(imageUri);
				BitmapRequest imageRequest = builder.create();
				webImageView.setSizeHint(candiWidthPixels);
				webImageView.setBitmapRequest(imageRequest);
				webImageView.setTag(entity);
			}

			FlowLayout.LayoutParams params = new FlowLayout.LayoutParams(candiWidthPixels, LayoutParams.WRAP_CONTENT);
			params.setCenterHorizontal(false);
			view.setLayoutParams(params);

			RelativeLayout.LayoutParams paramsImage = new RelativeLayout.LayoutParams(candiWidthPixels, candiHeightPixels);
			webImageView.setLayoutParams(paramsImage);

			layout.addView(view);

			if (entity.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
				sources.put(entity.type, entity);
			}
		}
	}

	private void buildCandiButtons(final Entity entity, final ViewGroup layout, Menu menu, GeoLocation mLocation) {

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
			if (ProxiManager.getInstance().getEntityModel().getLastBeaconRefreshDate() != null
					&& ProxiManager.getInstance().getEntityModel().getLastBeaconRefreshDate().longValue() > mEntityModelRefreshDate.longValue()
					|| ProxiManager.getInstance().getEntityModel().getLastActivityDate() != null
					&& ProxiManager.getInstance().getEntityModel().getLastActivityDate().longValue() > mEntityModelActivityDate.longValue()) {
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

	private void showInstallDialog(final Applink entity) {

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
								Tracker.sendEvent("ui_action", "install_source", entity.getPackageName(), 0, Aircandi.getInstance().getUser());
								Logger.d(this, "Install: navigating to market install page");
								final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=" + entity.getPackageName()
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
										+ entity.getPackageName() + "&referrer=utm_source%3Dcom.aircandi"));
								intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
								startActivityForResult(intent, Constants.ACTIVITY_MARKET);
							}
							dialog.dismiss();
							AnimUtils.doOverridePendingTransition(CandiForm.this, TransitionType.PageToForm);
						}
						else if (which == Dialog.BUTTON_NEGATIVE) {
							final Applink meta = ProxiManager.getInstance().getEntityModel().getApplinkMeta().get(entity.type);
							meta.installDeclined = true;
							doCandiClick(entity, null);
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