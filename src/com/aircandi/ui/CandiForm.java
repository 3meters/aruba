package com.aircandi.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.ProxiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.CommandType;
import com.aircandi.components.FontManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.EntityListType;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.GeoLocation;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.base.CandiActivity;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;

public class CandiForm extends CandiActivity {

	protected List<Entity>	mEntitiesForPaging	= new ArrayList<Entity>();
	protected ScrollView	mScrollView;
	protected ViewGroup		mContentView;
	protected ViewGroup		mCandiForm;
	protected CandiView		mCandiView;
	private Entity			mEntity;
	protected Number		mEntityModelRefreshDate;
	protected Number		mEntityModelActivityDate;
	protected Boolean		mUpsize;
	protected Boolean		mTracked			= false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			initialize();
			bind(false);
		}
	}

	public void initialize() {
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

		/* Font for button bar */
		FontManager.getInstance().setTypefaceDefault((TextView) mContentView.findViewById(R.id.button_comment));
		FontManager.getInstance().setTypefaceDefault((TextView) mContentView.findViewById(R.id.button_new_text));

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mUpsize = extras.getBoolean(CandiConstants.EXTRA_UPSIZE_SYNTHETIC);
		}
	}

	public void bind(Boolean refresh) {
		doBind(refresh);
	}

	public void doBind(final Boolean refresh) {
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
				ModelResult result = ProxiExplorer.getInstance().getEntityModel().getEntity(mCommon.mEntityId, refresh, null, null);
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				ModelResult result = (ModelResult) modelResult;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					if (result.data != null) {
						mEntity = (Entity) result.data;
						mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
						mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
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

	public void track() {

		final List<Beacon> beacons = ProxiExplorer.getInstance().getStrongestBeacons(5);
		final Beacon primaryBeacon = beacons.size() > 0 ? beacons.get(0) : null;

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("TrackEntityBrowse");
				ModelResult result = ProxiExplorer.getInstance().getEntityModel().trackEntity(mEntity, beacons, primaryBeacon, "browse");
				return result;
			}

			@Override
			protected void onPostExecute(Object result) {
				Integer placeRankScore = mEntity.getPlaceRankScore();
				mCandiView.getPlaceRankScore().setText(String.valueOf(placeRankScore));
			}

		}.execute();
	}

	public void upsize() {

		final List<Beacon> beacons = ProxiExplorer.getInstance().getStrongestBeacons(5);
		final Beacon primaryBeacon = beacons.size() > 0 ? beacons.get(0) : null;

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy(R.string.progress_expanding);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("UpsizeSynthetic");
				ModelResult result = ProxiExplorer.getInstance().upsizeSynthetic(mEntity, beacons, primaryBeacon);
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

	public void draw() {
		CandiForm.buildCandiForm(this, mEntity, mContentView, mCommon.mMenu, null, false);
		mCandiForm.setVisibility(View.VISIBLE);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onChildrenButtonClick(View v) {
		showChildrenForEntity();
	}

	public void showChildrenForEntity() {
		/*
		 * mCommon.mEntityId is the original entity the user navigated to but
		 * they could have swiped using the viewpager to a different entity so
		 * we need to use mEntity to get the right entity context.
		 */
		IntentBuilder intentBuilder = new IntentBuilder(this, CandiList.class);
		intentBuilder.setCommandType(CommandType.View)
				.setEntityListType(EntityListType.InCollection)
				.setCollectionId(mEntity.id);

		Intent intent = intentBuilder.create();
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiFormToCandiList);
	}

	public void onBrowseCommentsButtonClick(View view) {
		if (mEntity.commentCount != null && mEntity.commentCount > 0) {
			IntentBuilder intentBuilder = new IntentBuilder(this, CommentList.class);
			intentBuilder.setCommandType(CommandType.View)
					.setEntityId(mEntity.id)
					.setParentEntityId(mEntity.parentId)
					.setCollectionId(mEntity.id);
			Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
		}
		else {
			onNewCommentButtonClick(view);
		}
	}

	public void onMapButtonClick(View view) {
		GeoLocation location = mEntity.getLocation();
		AndroidManager.getInstance().callMapActivity(this, String.valueOf(location.latitude.doubleValue())
				, String.valueOf(location.longitude.doubleValue())
				, mEntity.name);
	}

	public void onTuneButtonClick(View view) {
		tuneProximity();
	}

	public void onCallButtonClick(View view) {
		AndroidManager.getInstance().callDialerActivity(this, mEntity.place.contact.phone);
	}

	public void onMoreButtonClick(View view) {
		String target = (String) view.getTag();
		if (target.equals("candi")) {
			IntentBuilder intentBuilder = new IntentBuilder(this, CandiList.class);
			intentBuilder.setCommandType(CommandType.View)
					.setEntityListType(EntityListType.InCollection)
					.setCollectionId(mEntity.id);

			Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.CandiListToCandiForm);
		}
	}

	public void onListItemClick(View view) {}

	public void onWebsiteButtonClick(View view) {
		AndroidManager.getInstance().callBrowserActivity(this, mEntity.place.website);
	}

	public void onShareButtonClick(View view) {
		AndroidManager.getInstance().callSendActivity(this, null, mEntity.place.sourceUriShort);
	}

	public void onMenuButtonClick(View view) {
		AndroidManager.getInstance().callBrowserActivity(this, mEntity.place.menu.mobileUri != null ? mEntity.place.menu.mobileUri : mEntity.place.menu.uri);
	}

	public void onPhotoClick(View view) {
		List<Photo> photos = mEntity.place.photos;
		ProxiExplorer.getInstance().getEntityModel().setPhotos(photos);
		Photo photo = (Photo) view.getTag();
		Intent intent = new Intent(this, PictureDetail.class);
		intent.putExtra(CandiConstants.EXTRA_URI, photo.getUri());
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	public void onCandiClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity.type.equals(CandiConstants.TYPE_CANDI_SOURCE)) {
			if (entity.source.name.equals("twitter")) {
				AndroidManager.getInstance().callTwitterActivity(this, entity.source.id);
			}
			else if (entity.source.name.equals("foursquare")) {
				AndroidManager.getInstance().callFoursquareActivity(this, entity.source.id);
			}
			else if (entity.source.name.equals("facebook")) {
				AndroidManager.getInstance().callFacebookActivity(this, entity.source.id);
			}
			else if (entity.source.name.equals("yelp")) {
				AndroidManager.getInstance().callYelpActivity(this, entity.source.id, entity.source.url);
			}
			else if (entity.source.name.equals("opentable")) {
				AndroidManager.getInstance().callGenericActivity(this, entity.source.url != null ? entity.source.url : entity.source.id);
			}
			else if (entity.source.name.equals("website")) {
				AndroidManager.getInstance().callBrowserActivity(this, entity.source.id);
			}
			else if (entity.source.name.equals("comments")) {
				IntentBuilder intentBuilder = new IntentBuilder(this, CommentList.class);
				intentBuilder.setCommandType(CommandType.View)
						.setEntityId(mEntity.id)
						.setParentEntityId(mEntity.parentId)
						.setCollectionId(mEntity.id);
				Intent intent = intentBuilder.create();
				startActivity(intent);
				AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
			}
			else {
				AndroidManager.getInstance().callGenericActivity(this, entity.source.url != null ? entity.source.url : entity.source.id);
			}
		}
		else {
			mCommon.showCandiFormForEntity(entity, CandiForm.class);
		}
	}

	public void onImageClick(View view) {
		Intent intent = null;
		Photo photo = mEntity.photo;
		photo.setCreatedAt(mEntity.modifiedDate);
		photo.setTitle(mEntity.name);
		photo.setUser(mEntity.creator);
		ProxiExplorer.getInstance().getEntityModel().getPhotos().clear();
		ProxiExplorer.getInstance().getEntityModel().getPhotos().add(photo);
		intent = new Intent(this, PictureDetail.class);
		intent.putExtra(CandiConstants.EXTRA_URI, mEntity.photo.getUri());
		intent.putExtra(CandiConstants.EXTRA_PAGING_ENABLED, false);

		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	public void onAddCandiButtonClick(View view) {
		if (!mEntity.locked) {
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

	public void onEditCandiButtonClick(View view) {
		mCommon.doEditCandiClick();
	}

	public void onNewCommentButtonClick(View view) {
		if (!mEntity.locked) {
			IntentBuilder intentBuilder = new IntentBuilder(this, CommentForm.class);
			intentBuilder.setEntityId(null);
			intentBuilder.setParentEntityId(mEntity.id);
			Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
		}
		else {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.alert_entity_locked)
					, null
					, this, android.R.string.ok, null, null, null);
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
			if (requestCode == CandiConstants.ACTIVITY_ENTITY_EDIT) {
				if (resultCode == CandiConstants.RESULT_ENTITY_DELETED) {
					finish();
					AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToCandiMap);
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
						AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
					}
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_CANDI_PICK) {
				if (intent != null) {
					String parentEntityId = null;
					Bundle extras = intent.getExtras();
					if (extras != null) {
						parentEntityId = extras.getString(CandiConstants.EXTRA_ENTITY_ID);
					}
					/*
					 * If parentEntityId is null then the candi is being moved to the top on its own.
					 * 
					 * Special case: user could have a top level candi and choose to move it to
					 * top so it's a no-op.
					 */
					if (parentEntityId == null && mEntity.getParent() == null) {
						return;
					}
					moveCandi(mEntity, parentEntityId);
				}
			}
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putIntArray("ARTICLE_SCROLL_POSITION", new int[] { mScrollView.getScrollX(), mScrollView.getScrollY() });
	}

	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		final int[] position = savedInstanceState.getIntArray("ARTICLE_SCROLL_POSITION");
		if (position != null)
			mScrollView.post(new Runnable() {
				public void run() {
					mScrollView.scrollTo(position[0], position[1]);
				}
			});
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	public void tuneProximity() {

		final List<Beacon> beacons = ProxiExplorer.getInstance().getStrongestBeacons(5);
		final Beacon primaryBeacon = beacons.size() > 0 ? beacons.get(0) : null;

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy(R.string.progress_tuning);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("TrackEntityProximity");
				ModelResult result = ProxiExplorer.getInstance().getEntityModel().trackEntity(mEntity, beacons, primaryBeacon, "proximity");
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
				doBind(true);
				setSupportProgressBarIndeterminateVisibility(false);
				mCommon.hideBusy(false);
				ImageUtils.showToastNotification(getString(R.string.toast_tuned), Toast.LENGTH_SHORT);
			}
		}.execute();
	}

	static public ViewGroup buildCandiForm(Context context, final Entity entity, final ViewGroup layout, Menu menu, GeoLocation mLocation, boolean refresh) {
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
		final UserView author = (UserView) layout.findViewById(R.id.author);

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

					if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
						image.getImageBadge().setVisibility(View.GONE);
						image.getImageZoom().setVisibility(View.GONE);
						image.setClickable(false);
					}
					else if (entity.type.equals(CandiConstants.TYPE_CANDI_POST)) {
						image.getImageBadge().setVisibility(View.GONE);
						image.getImageZoom().setVisibility(View.GONE);
						image.setClickable(false);
					}
					else {
						image.getImageBadge().setVisibility(View.GONE);
						image.getImageZoom().setVisibility(View.VISIBLE);
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

		/* Author block */

		setVisibility(layout.findViewById(R.id.author), View.GONE);
		if (author != null && entity.creator != null) {
			author.setLabel(context.getString(R.string.candi_label_user_creator));
			author.bindToAuthor(entity.creator, entity.modifiedDate.longValue(), entity.locked);
			setVisibility(layout.findViewById(R.id.author), View.VISIBLE);
		}

		/* Buttons */
		buildCandiButtons(context, entity, layout, menu, mLocation);

		return layout;
	}

	static public void drawCandi(Context context, FlowLayout layout, List<Entity> entities, Integer viewResId) {

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
		Integer candiCountPortrait = context.getResources().getInteger(R.integer.candi_per_row_portrait_form);
		Integer candiCountLandscape = context.getResources().getInteger(R.integer.candi_per_row_landscape_form);

		Integer candiWidthPixels = (int) (layoutWidthPixels - (spacingHorizontalPixels * (candiCountPortrait - 1))) / candiCountPortrait;

		/* We need to cap the dimensions so we don't look crazy in landscape orientation */
		if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			candiWidthPixels = (int) (layoutWidthPixels - (spacingHorizontalPixels * (candiCountLandscape - 1))) / candiCountLandscape;
		}

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
				if (entity.source.name.equals("comments")) {
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
			BitmapRequestBuilder builder = new BitmapRequestBuilder(webImageView).setImageUri(imageUri);
			BitmapRequest imageRequest = builder.create();
			webImageView.setSizeHint(candiWidthPixels);
			webImageView.setBitmapRequest(imageRequest);
			webImageView.setTag(entity);

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
		setVisibility(layout.findViewById(R.id.button_comments_browse), View.GONE);

		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_map));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_call));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_tune));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_comment));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_new_text));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_comments_browse));

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
		if (entity.locked != null && !entity.locked) {
			if (entity.isCollection != null && entity.isCollection) {
				setVisibility(layout.findViewById(R.id.button_new), View.VISIBLE);
			}
		}

		/* Add comment */
		if (entity.locked != null && !entity.locked) {
			setVisibility(layout.findViewById(R.id.button_comment), View.VISIBLE);
		}

		/* Edit */
		if (menu != null) {
			MenuItem menuItem = menu.findItem(R.id.edit_candi);
			if (menuItem != null) {
				if (entity.ownerId != null
						&& (entity.ownerId.equals(Aircandi.getInstance().getUser().id)
						|| entity.ownerId.equals(ProxiConstants.ADMIN_USER_ID))) {
					menuItem.setVisible(true);
				}
				else {
					menuItem.setVisible(false);
				}
			}
		}
	}

	private void moveCandi(final Entity entityToMove, final String collectionEntityId) {
		/*
		 * We only move within radar tree or within user tree. A candi can still be
		 * currently shown in both trees so we still need to fixup across both.
		 */

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy(R.string.progress_moving);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("MoveEntity");
				String newParentId = collectionEntityId != null ? collectionEntityId : entityToMove.getParent().getBeaconId();
				ModelResult result = ProxiExplorer.getInstance().getEntityModel()
						.moveEntity(entityToMove.id, newParentId, collectionEntityId == null ? true : false, false);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {

				ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode != ResponseCode.Success) {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiMove, CandiForm.this);
				}
				else {
					ImageUtils.showToastNotification(getString(R.string.alert_moved), Toast.LENGTH_SHORT);
					bind(false);
				}
			}

		}.execute();

	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
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
			if (ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate().longValue() > mEntityModelRefreshDate.longValue()
					|| ProxiExplorer.getInstance().getEntityModel().getLastActivityDate().longValue() > mEntityModelActivityDate.longValue()) {
				invalidateOptionsMenu();
				bind(true);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	protected int getLayoutId() {
		return R.layout.candi_form;
	}

	public Entity getEntity() {
		return mEntity;
	}

	public void setEntity(Entity entity) {
		mEntity = entity;
	}
}