package com.aircandi.ui;

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
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.ProxiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.BeaconsLockedEvent;
import com.aircandi.components.BusProvider;
import com.aircandi.components.CommandType;
import com.aircandi.components.FontManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.ProxiManager.ScanReason;
import com.aircandi.components.QueryWifiScanReceivedEvent;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.GeoLocation;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Source;
import com.aircandi.ui.base.CandiActivity;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;
import com.squareup.otto.Subscribe;

public class CandiForm extends CandiActivity {

	private ScrollView		mScrollView;
	private ViewGroup		mContentView;
	private ViewGroup		mCandiForm;
	private CandiView		mCandiView;
	private Entity			mEntity;
	private Number			mEntityModelRefreshDate;
	private Number			mEntityModelActivityDate;
	private Boolean			mUpsize;
	private Boolean			mTracked			= false;
	private PackageReceiver	mPackageReceiver	= new PackageReceiver();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			initialize();
			bind(false);
		}
	}

	private void initialize() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mContentView = (ViewGroup) findViewById(R.id.candi_body);

		Integer candiFormResId = R.layout.candi_form_base;
		if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_POST)) {
			candiFormResId = R.layout.candi_form_post;
		}
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_PLACE)) {
			candiFormResId = R.layout.candi_form_place;
		}

		ViewGroup body = (ViewGroup) inflater.inflate(candiFormResId, null);
		if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_PLACE)) {
			((ViewStub) body.findViewById(R.id.stub_switchboard)).inflate();
		}

		mCandiForm = (ViewGroup) body.findViewById(R.id.candi_form);
		mScrollView = (ScrollView) body.findViewById(R.id.scroll_view);
		mCandiView = (CandiView) body.findViewById(R.id.candi_view);

		mContentView.addView(body, 0);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mUpsize = extras.getBoolean(CandiConstants.EXTRA_UPSIZE_SYNTHETIC);
		}
	}

	private void bind(Boolean refresh) {
		doBind(refresh);
	}

	private void doBind(final Boolean refresh) {
		/*
		 * Navigation setup for action bar icon and title
		 */
		Logger.d(this, "Binding candi form");
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("GetEntity");
				ModelResult result = ProxiManager.getInstance().getEntityModel().getEntity(mCommon.mEntityId, refresh, null, null);
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				ModelResult result = (ModelResult) modelResult;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					if (result.data != null) {
						mEntity = (Entity) result.data;
						mEntityModelRefreshDate = ProxiManager.getInstance().getEntityModel().getLastBeaconRefreshDate();
						mEntityModelActivityDate = ProxiManager.getInstance().getEntityModel().getLastActivityDate();
						mCommon.mActionBar.setTitle(mEntity.name);

						draw();
						if (mUpsize) {
							mUpsize = false;
							mTracked = true;
							upsize();
						}

						if (!mTracked) {
							mTracked = true;
							if (mEntity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
								track();
							}
						}
					}
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiForm);
				}
				mCommon.hideBusy(false);
			}

		}.execute();
	}

	private void track() {

		final List<Beacon> beacons = ProxiManager.getInstance().getStrongestBeacons(5);
		final Beacon primaryBeacon = beacons.size() > 0 ? beacons.get(0) : null;

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("TrackEntityBrowse");
				ModelResult result = ProxiManager.getInstance().getEntityModel().trackEntity(mEntity, beacons, primaryBeacon, "browse");
				return result;
			}

			@Override
			protected void onPostExecute(Object result) {
				Integer placeRankScore = mEntity.getPlaceRankScore();
				mCandiView.getPlaceRankScore().setText(String.valueOf(placeRankScore));
			}

		}.execute();
	}

	private void upsize() {

		final List<Beacon> beacons = ProxiManager.getInstance().getStrongestBeacons(5);
		final Beacon primaryBeacon = beacons.size() > 0 ? beacons.get(0) : null;

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy(R.string.progress_expanding);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("UpsizeSynthetic");
				ModelResult result = ProxiManager.getInstance().upsizeSynthetic(mEntity, beacons, primaryBeacon);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;
				mCommon.hideBusy(false);
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					Entity upsizedEntity = (Entity) result.data;
					mCommon.mEntityId = upsizedEntity.id;
					doBind(false);
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
		CandiForm.buildCandiForm(this, mEntity, mContentView, mCommon.mMenu, null, false);
		mCandiForm.setVisibility(View.VISIBLE);
	}

	// --------------------------------------------------------------------------------------------
	// Event bus routines
	// --------------------------------------------------------------------------------------------

	@Subscribe
	@SuppressWarnings("ucd")
	public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Logger.d(CandiForm.this, "Query wifi scan received event: locking beacons");
				if (event.wifiList != null && event.wifiList.size() > 0) {
					ProxiManager.getInstance().lockBeacons();
				}
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onBeaconsLocked(BeaconsLockedEvent event) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Logger.d(CandiForm.this, "Beacons locked event: tune entity");
				tuneProximity();
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onBrowseCommentsButtonClick(View view) {
		if (mEntity.commentCount != null && mEntity.commentCount > 0) {
			Tracker.sendEvent("ui_action", "browse_comments", null, 0);
			IntentBuilder intentBuilder = new IntentBuilder(this, CommentList.class);
			intentBuilder.setCommandType(CommandType.View)
					.setEntityId(mEntity.id)
					.setParentEntityId(mEntity.parentId)
					.setCollectionId(mEntity.id);
			Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.PageToPage);
		}
		else {
			onNewCommentButtonClick(view);
		}
	}

	@SuppressWarnings("ucd")
	public void onMapButtonClick(View view) {
		Tracker.sendEvent("ui_action", "map_place", null, 0);
		GeoLocation location = mEntity.getLocation();
		AndroidManager.getInstance().callMapActivity(this, String.valueOf(location.latitude.doubleValue())
				, String.valueOf(location.longitude.doubleValue())
				, mEntity.name);
	}

	@SuppressWarnings("ucd")
	public void onTuneButtonClick(View view) {
		Tracker.sendEvent("ui_action", "tune_place", null, 0);
		mCommon.showBusy(R.string.progress_tuning);
		if (NetworkManager.getInstance().isWifiEnabled()) {
			ProxiManager.getInstance().scanForWifi(ScanReason.query);
		}
		else {
			tuneProximity();
		}
	}

	@SuppressWarnings("ucd")
	public void onCallButtonClick(View view) {
		Tracker.sendEvent("ui_action", "call_place", null, 0);
		AndroidManager.getInstance().callDialerActivity(this, mEntity.place.contact.phone);
	}

	@SuppressWarnings("ucd")
	public void onListItemClick(View view) {}

	@SuppressWarnings("ucd")
	public void onCandiClick(View view) {
		Entity entity = (Entity) view.getTag();
		doCandiClick(entity);
	}

	private void doCandiClick(Entity entity) {
		if (entity.type.equals(CandiConstants.TYPE_CANDI_SOURCE)) {
			Source meta = ProxiManager.getInstance().getEntityModel().getSourceMeta().get(entity.source.source);
			if (meta != null && !meta.installDeclined
					&& meta.intentSupport
					&& entity.source.appExists()
					&& !entity.source.appInstalled()) {
				showInstallDialog(entity);
			}
			else {
				Tracker.sendEvent("ui_action", "browse_source", entity.source.source, 0);
				if (entity.source.source.equals("twitter")) {
					AndroidManager.getInstance().callTwitterActivity(this, entity.source.id);
				}
				else if (entity.source.source.equals("foursquare")) {
					AndroidManager.getInstance().callFoursquareActivity(this, entity.source.id);
				}
				else if (entity.source.source.equals("facebook")) {
					AndroidManager.getInstance().callFacebookActivity(this, entity.source.id);
				}
				else if (entity.source.source.equals("yelp")) {
					AndroidManager.getInstance().callYelpActivity(this, entity.source.id, entity.source.url);
				}
				else if (entity.source.source.equals("opentable")) {
					AndroidManager.getInstance().callOpentableActivity(this, entity.source.id, entity.source.url);
				}
				else if (entity.source.source.equals("website")) {
					AndroidManager.getInstance().callBrowserActivity(this, entity.source.id);
				}
				else if (entity.source.source.equals("email")) {
					AndroidManager.getInstance().callSendToActivity(this, entity.source.name, entity.source.id, null, null);
				}
				else if (entity.source.source.equals("comments")) {
					IntentBuilder intentBuilder = new IntentBuilder(this, CommentList.class);
					intentBuilder.setCommandType(CommandType.View)
							.setEntityId(mEntity.id)
							.setParentEntityId(mEntity.parentId)
							.setCollectionId(mEntity.id);
					Intent intent = intentBuilder.create();
					startActivity(intent);
					AnimUtils.doOverridePendingTransition(this, TransitionType.PageToPage);
				}
				else {
					AndroidManager.getInstance().callGenericActivity(this, entity.source.url != null ? entity.source.url : entity.source.id);
				}
			}
		}
		else {
			mCommon.showCandiFormForEntity(entity, CandiForm.class);
		}
	}

	@SuppressWarnings("ucd")
	public void onImageClick(View view) {
		Intent intent = null;
		Photo photo = mEntity.photo;
		photo.setCreatedAt(mEntity.modifiedDate);
		photo.setTitle(mEntity.name);
		photo.setUser(mEntity.creator);
		ProxiManager.getInstance().getEntityModel().getPhotos().clear();
		ProxiManager.getInstance().getEntityModel().getPhotos().add(photo);
		intent = new Intent(this, PictureDetail.class);
		intent.putExtra(CandiConstants.EXTRA_URI, mEntity.photo.getUri());
		intent.putExtra(CandiConstants.EXTRA_PAGING_ENABLED, false);

		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToPage);
	}

	@SuppressWarnings("ucd")
	public void onAddCandiButtonClick(View view) {
		Tracker.sendEvent("ui_action", "add_candi", null, 0);
		if (!mEntity.locked || mEntity.ownerId.equals(Aircandi.getInstance().getUser().id)) {
			mCommon.showTemplatePicker(false);
		}
		else {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.alert_entity_locked)
					, null
					, this, android.R.string.ok, null, null, null);
		}
	}

	@SuppressWarnings("ucd")
	public void onEditCandiButtonClick(View view) {
		Tracker.sendEvent("ui_action", "edit_entity", null, 0);
		mCommon.doEditCandiClick();
	}

	@SuppressWarnings("ucd")
	public void onNewCommentButtonClick(View view) {
		if (!mEntity.locked || mEntity.ownerId.equals(Aircandi.getInstance().getUser().id)) {
			IntentBuilder intentBuilder = new IntentBuilder(this, CommentForm.class);
			intentBuilder.setEntityId(null);
			intentBuilder.setParentEntityId(mEntity.id);
			Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
		}
		else {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.alert_entity_locked)
					, null
					, this, android.R.string.ok, null, null, null);
		}
	}

	@SuppressWarnings("ucd")
	public void onInstallButtonClick(View view) {
		Entity entity = (Entity) view.getTag();
		showInstallDialog(entity);
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
			if (requestCode == CandiConstants.ACTIVITY_ENTITY_EDIT) {
				if (resultCode == CandiConstants.RESULT_ENTITY_DELETED) {
					finish();
					AnimUtils.doOverridePendingTransition(this, TransitionType.PageToRadarAfterDelete);
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_TEMPLATE_PICK) {
				if (intent != null && intent.getExtras() != null) {

					Bundle extras = intent.getExtras();
					final String entityType = extras.getString(CandiConstants.EXTRA_ENTITY_TYPE);
					if (entityType != null && !entityType.equals("")) {

						IntentBuilder intentBuilder = new IntentBuilder(this, EntityForm.class)
								.setCommandType(CommandType.New)
								.setEntityId(null)
								.setParentEntityId(mCommon.mEntityId)
								.setEntityType(entityType);

						Intent redirectIntent = intentBuilder.create();
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
		if (position != null)
			mScrollView.post(new Runnable() {
				@Override
				public void run() {
					mScrollView.scrollTo(position[0], position[1]);
				}
			});
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void tuneProximity() {

		final List<Beacon> beacons = ProxiManager.getInstance().getStrongestBeacons(5);
		final Beacon primaryBeacon = beacons.size() > 0 ? beacons.get(0) : null;

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("TrackEntityProximity");
				ModelResult result = ProxiManager.getInstance().getEntityModel().trackEntity(mEntity, beacons, primaryBeacon, "proximity");
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				/*
				 * Rebind so distance info gets updated.
				 * 
				 * TODO: We could check to see if proximity is new and only
				 * refresh if true.
				 */
				if (mCandiView != null) {
					mCandiView.showDistance(mEntity);
					Integer placeRankScore = mEntity.getPlaceRankScore();
					mCandiView.getPlaceRankScore().setText(String.valueOf(placeRankScore));
				}
				//doBind(true);
				setSupportProgressBarIndeterminateVisibility(false);
				mCommon.hideBusy(false);
				ImageUtils.showToastNotification(getString(R.string.toast_tuned), Toast.LENGTH_SHORT);
			}
		}.execute();
	}

	static private ViewGroup buildCandiForm(Context context, final Entity entity, final ViewGroup layout, Menu menu, GeoLocation mLocation, boolean refresh) {
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
		final UserView creator = (UserView) layout.findViewById(R.id.created_by);
		final UserView editor = (UserView) layout.findViewById(R.id.edited_by);

		if (candiView != null) {
			/*
			 * This is a place entity with a fancy image widget
			 */
			candiView.bindToEntity(entity);
		}
		else {
			if (image != null) {
				String imageUri = entity.getEntityPhotoUri();
				if (imageUri != null) {

					BitmapRequestBuilder builder = new BitmapRequestBuilder(image).setImageUri(imageUri);
					BitmapRequest imageRequest = builder.create();

					image.setBitmapRequest(imageRequest);
					image.setClickable(false);

					if (entity.type.equals(CandiConstants.TYPE_CANDI_PICTURE)) {
						image.setClickable(true);
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

		/* Switchboard - source entities */
		setVisibility(layout.findViewById(R.id.section_switchboard), View.GONE);
		List<Entity> entities = entity.getSourceEntities();
		if (entities.size() > 0) {

			SectionLayout section = (SectionLayout) layout.findViewById(R.id.section_layout_switchboard);
			if (section != null) {
				section.getTextViewHeader().setText(context.getString(R.string.candi_section_switchboard));
				FlowLayout flow = (FlowLayout) layout.findViewById(R.id.flow_switchboard);
				drawCandi(context, flow, entities, R.layout.temp_place_switchboard_item);
				setVisibility(layout.findViewById(R.id.section_switchboard), View.VISIBLE);
			}
		}

		/* All non-source children */
		entities = entity.getChildren();
		if (entities.size() > 0) {
			ViewStub stub = (ViewStub) layout.findViewById(R.id.stub_candi);
			if (stub != null) {
				((ViewStub) layout.findViewById(R.id.stub_candi)).inflate();
			}
		}

		setVisibility(layout.findViewById(R.id.section_candi), View.GONE);
		if (entities.size() > 0) {

			SectionLayout section = (SectionLayout) layout.findViewById(R.id.section_layout_candi);
			if (section != null) {
				section.getTextViewHeader().setText(context.getString(R.string.candi_section_candi));
				FlowLayout flow = (FlowLayout) layout.findViewById(R.id.flow_candi);
				drawCandi(context, flow, entities, R.layout.temp_place_candi_item);
				setVisibility(layout.findViewById(R.id.section_candi), View.VISIBLE);
			}
		}

		/* Place specific info */
		if (entity.place != null) {
			final Place place = entity.place;

			setVisibility(address, View.GONE);
			if (address != null && place.location != null) {
				String addressBlock = place.location.getAddressBlock();

				if (place.contact != null && place.contact.formattedPhone != null) {
					addressBlock += "<br/>" + place.contact.formattedPhone;
				}

				address.setText(Html.fromHtml(addressBlock));
				FontManager.getInstance().setTypefaceDefault(address);
				setVisibility(address, View.VISIBLE);
			}
		}

		/* Creator block */

		setVisibility(creator, View.GONE);
		if (creator != null && entity.creator != null) {
			if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
				if (entity.place.source.equals("user")) {
					creator.setLabel(context.getString(R.string.candi_label_user_created_by));
				}
				else {
					creator.setLabel(context.getString(R.string.candi_label_user_discovered_by));
				}
			}
			else if (entity.type.equals(CandiConstants.TYPE_CANDI_PICTURE)) {
				creator.setLabel(context.getString(R.string.candi_label_user_added_by));
			}
			else if (entity.type.equals(CandiConstants.TYPE_CANDI_POST)) {
				creator.setLabel(context.getString(R.string.candi_label_user_posted_by));
			}
			else {
				creator.setLabel(context.getString(R.string.candi_label_user_created_by));
			}
			creator.bindToAuthor(entity.creator, entity.createdDate.longValue(), entity.locked);
			setVisibility(creator, View.VISIBLE);
		}

		/* Editor block */

		setVisibility(editor, View.GONE);
		if (editor != null && entity.modifier != null) {
			editor.setLabel(context.getString(R.string.candi_label_user_edited_by));
			editor.bindToAuthor(entity.modifier, entity.modifiedDate.longValue(), null);
			setVisibility(editor, View.VISIBLE);
		}

		/* Buttons */
		buildCandiButtons(context, entity, layout, menu, mLocation);

		return layout;
	}

	static private void drawCandi(Context context, FlowLayout layout, List<Entity> entities, Integer viewResId) {

		layout.removeAllViews();
		final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		View parentView = (View) layout.getParent();
		Integer layoutWidthPixels = metrics.widthPixels
				- (parentView.getPaddingLeft() + parentView.getPaddingRight() + layout.getPaddingLeft() + layout.getPaddingRight());
		Integer bonusPadding = ImageUtils.getRawPixels(context, 20);
		layoutWidthPixels -= bonusPadding;

		Integer spacing = 3;
		Integer spacingHorizontalPixels = ImageUtils.getRawPixels(context, spacing);
		Integer spacingVerticalPixels = ImageUtils.getRawPixels(context, spacing);

		Integer desiredWidthPixels = (int) (metrics.xdpi * 0.45f);
		if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			desiredWidthPixels = (int) (metrics.ydpi * 0.45f);
		}

		Integer candiCount = (int) Math.ceil(layoutWidthPixels / desiredWidthPixels);
		Integer candiWidthPixels = (int) (layoutWidthPixels - (spacingHorizontalPixels * (candiCount - 1))) / candiCount;

		Integer candiHeightPixels = (int) (candiWidthPixels * 1);

		layout.setSpacingHorizontal(spacingHorizontalPixels);
		layout.setSpacingVertical(spacingVerticalPixels);

		/*
		 * Insert views for entities that we don't already have a view for
		 */
		for (Entity entity : entities) {

			View view = inflater.inflate(viewResId, null);
			WebImageView webImageView = (WebImageView) view.findViewById(R.id.image);

			TextView title = (TextView) view.findViewById(R.id.title);
			TextView badge = (TextView) view.findViewById(R.id.badge);

			FontManager.getInstance().setTypefaceDefault(title);
			FontManager.getInstance().setTypefaceDefault(badge);

			if (entity.type.equals(CandiConstants.TYPE_CANDI_SOURCE)) {
				if (entity.source.name != null && entity.source.name.equals("comments")) {
					if (entity.commentCount != null && entity.commentCount > 0) {
						badge.setText(String.valueOf(entity.commentCount));
						badge.setVisibility(View.VISIBLE);
					}
					else {
						badge.setVisibility(View.GONE);
					}
				}
				title.setText(entity.name);
				title.setVisibility(View.VISIBLE);
			}
			else {
				if (entity.name != null && !entity.name.equals("")) {
					title.setText(entity.name);
					title.setVisibility(View.VISIBLE);
				}
			}

			String imageUri = entity.getEntityPhotoUri();
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
		}
	}

	static private void buildCandiButtons(Context context, final Entity entity, final ViewGroup layout, Menu menu, GeoLocation mLocation) {

		setVisibility(layout.findViewById(R.id.button_map), View.GONE);
		setVisibility(layout.findViewById(R.id.button_call), View.GONE);
		setVisibility(layout.findViewById(R.id.button_tune), View.GONE);
		setVisibility(layout.findViewById(R.id.button_comment), View.GONE);
		setVisibility(layout.findViewById(R.id.button_new), View.GONE);
		setVisibility(layout.findViewById(R.id.button_edit), View.GONE);
		setVisibility(layout.findViewById(R.id.button_comments_browse), View.GONE);

		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_map));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_call));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_tune));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_comments_browse));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_comment));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_new));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_edit));

		if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {

			/* Tune, map, dial */
			GeoLocation location = entity.getLocation();
			if (location != null) {
				setVisibility(layout.findViewById(R.id.button_map), View.VISIBLE);
			}

			if (entity.place != null) {

				Place place = entity.place;
				setVisibility(layout.findViewById(R.id.button_tune), View.VISIBLE);

				if (place.contact != null) {
					if (place.contact.phone != null) {
						setVisibility(layout.findViewById(R.id.button_call), View.VISIBLE);
					}
				}
			}

			if (entity.synthetic) {
				setVisibility(layout.findViewById(R.id.form_footer), View.GONE);
				return;
			}
			else {
				setVisibility(layout.findViewById(R.id.form_footer), View.VISIBLE);
			}
		}

		/* Browse comments */
		setVisibility(layout.findViewById(R.id.button_comments_browse), View.VISIBLE);
		if (entity.commentCount != null && entity.commentCount > 0) {
			Button button = (Button) layout.findViewById(R.id.button_comments_browse);
			if (button != null) {
				button.setText(String.valueOf(entity.commentCount) + (entity.commentCount == 1 ? " Comment" : " Comments"));
			}
		}

		/* Add candi */
		if (entity.isCollection != null && entity.isCollection) {
			setVisibility(layout.findViewById(R.id.button_new), View.VISIBLE);
		}

		/* Add comment */
		setVisibility(layout.findViewById(R.id.button_comment), View.VISIBLE);

		/* Edit */
		if (entity.ownerId != null) {
			if (entity.ownerId.equals(Aircandi.getInstance().getUser().id)) {
				setVisibility(layout.findViewById(R.id.button_edit), View.VISIBLE);
			}
			else if (entity.ownerId.equals(ProxiConstants.ADMIN_USER_ID)) {
				setVisibility(layout.findViewById(R.id.button_edit), View.VISIBLE);
			}
			else if (!entity.ownerId.equals(ProxiConstants.ADMIN_USER_ID) && !entity.locked) {
				setVisibility(layout.findViewById(R.id.button_edit), View.VISIBLE);
			}
		}
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
			if (ProxiManager.getInstance().getEntityModel().getLastBeaconRefreshDate().longValue() > mEntityModelRefreshDate.longValue()
					|| ProxiManager.getInstance().getEntityModel().getLastActivityDate().longValue() > mEntityModelActivityDate.longValue()) {
				invalidateOptionsMenu();
				bind(true);
			}
			/* Package receiver */
			IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
			filter.addDataScheme("package");
			registerReceiver(mPackageReceiver, filter);
		}
	}

	@Override
	protected void onPause() {
		try {
			unregisterReceiver(mPackageReceiver);
		}
		catch (Exception e) {}
		super.onPause();
	}

	@Override
	protected void onStart() {
		super.onStart();
		enableEvents();
	}

	@Override
	protected void onStop() {
		disableEvents();
		super.onStop();
	}

	// --------------------------------------------------------------------------------------------
	// Dialogs
	// --------------------------------------------------------------------------------------------

	private void showInstallDialog(final Entity entity) {

		AlertDialog installDialog = AircandiCommon.showAlertDialog(null
				, getString(R.string.dialog_install_title)
				, getString(R.string.dialog_install_message)
				, null
				, this
				, R.string.dialog_install_ok
				, R.string.dialog_install_cancel
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							try {
								Tracker.sendEvent("ui_action", "install_source", entity.source.packageName, 0);
								Logger.d(this, "Install: navigating to market install page");
								Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=" + entity.source.packageName
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
								Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://play.google.com/store/apps/details?id="
										+ entity.source.packageName + "&referrer=utm_source%3Dcom.aircandi"));
								intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
								startActivityForResult(intent, CandiConstants.ACTIVITY_MARKET);
							}
							dialog.dismiss();
							AnimUtils.doOverridePendingTransition(CandiForm.this, TransitionType.PageToForm);
						}
						else if (which == Dialog.BUTTON_NEGATIVE) {
							Source meta = ProxiManager.getInstance().getEntityModel().getSourceMeta().get(entity.source.source);
							meta.installDeclined = true;
							doCandiClick(entity);
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

	private void enableEvents() {
		BusProvider.getInstance().register(this);
	}

	private void disableEvents() {
		try {
			BusProvider.getInstance().unregister(this);
		}
		catch (Exception e) {}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.candi_form;
	}

	public Entity getEntity() {
		return mEntity;
	}

	private class PackageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(final Context context, Intent intent) {

			/* This is on the main UI thread */
			if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
				String publicName = AndroidManager.getInstance().getPublicName(intent.getData().getEncodedSchemeSpecificPart());
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