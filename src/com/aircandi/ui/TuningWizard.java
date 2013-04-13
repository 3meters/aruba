package com.aircandi.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.BeaconsLockedEvent;
import com.aircandi.components.BusProvider;
import com.aircandi.components.CommandType;
import com.aircandi.components.FontManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.ProxiManager.ScanReason;
import com.aircandi.components.QueryWifiScanReceivedEvent;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Entity.Visibility;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Provider;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.FlowLayout;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;
import com.squareup.otto.Subscribe;

public class TuningWizard extends FormActivity {

	private WebImageView	mImageViewPicture;
	private Button			mButtonTune;
	private Button			mButtonUntune;
	private Bitmap			mEntityBitmap;
	private Boolean			mEntityBitmapLocalOnly	= false;
	private Entity			mEntityForForm;
	private Boolean			mMuteColor;
	private Integer			mColorResId;
	private Boolean			mTuned					= false;
	private Boolean			mUntuned				= false;
	private Boolean			mTuningInProcess		= false;
	private Boolean			mUntuning				= false;
	private List<Entity>	mAddedCandi				= new ArrayList<Entity>();
	private Boolean			mDirty					= false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			initialize();
			bind();
			draw();
		}
	}

	private void initialize() {
		mCommon.mActionBar.setTitle(R.string.form_title_tune);
		mImageViewPicture = (WebImageView) findViewById(R.id.image_picture);
		mButtonTune = (Button) findViewById(R.id.button_tune);
		mButtonUntune = (Button) findViewById(R.id.button_untune);

		/* Color */

		mMuteColor = android.os.Build.MODEL.toLowerCase(Locale.US).equals("nexus s"); // nexus 4, nexus 7 are others		
		((ImageView) findViewById(R.id.image_star_banner)).setColorFilter(getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_ATOP);
		((ImageView) findViewById(R.id.image_star_tune)).setColorFilter(getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_ATOP);
		((ImageView) findViewById(R.id.image_star_add_candi)).setColorFilter(getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_ATOP);

		/* Fonts */

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_change_image));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_tune));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_untune));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_add_candi));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_edit));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.section_title_banner));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.section_title_tune));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.section_title_add_candi));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.section_title_edit));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.label_banner));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.label_tune));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.label_add_candi));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.label_edit));
	}

	private void bind() {
		/*
		 * Fill in the system and default properties for the base entity properties. The activities that subclass this
		 * will set any additional properties beyond the base ones.
		 */
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
		if (mEntityForForm == null && mCommon.mEntityId != null) {
			/*
			 * Entity is coming from entity model. We want to create a clone so
			 * that any changes only show up in the entity model if the changes make it
			 * to the service.
			 */
			final Entity entityForModel = ProxiManager.getInstance().getEntityModel().getCacheEntity(mCommon.mEntityId);
			if (entityForModel != null) {
				mEntityForForm = entityForModel.clone();
			}
			mCommon.mActionBar.setTitle(mEntityForForm.name);
		}
	}

	private void draw() {

		if (mEntityForForm != null) {

			final Entity entity = mEntityForForm;
			final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			/* Tuning buttons */
			final Boolean hasActiveProximityLink = entity.hasActiveProximityLink();
			if (hasActiveProximityLink) {
				mButtonUntune.setVisibility(View.VISIBLE);
			}

			/* Content */
			if (mAddedCandi.size() > 0) {
				final ViewStub stub = (ViewStub) findViewById(R.id.stub_candi);
				if (stub != null) {
					((ViewStub) findViewById(R.id.stub_candi)).inflate();
				}
			}

			setVisibility(findViewById(R.id.section_candi), View.GONE);
			if (mAddedCandi.size() > 0) {

				final SectionLayout section = (SectionLayout) findViewById(R.id.section_layout_candi);
				if (section != null) {

					if (mAddedCandi.size() > getResources().getInteger(R.integer.candi_flow_limit)) {
						View footer = inflater.inflate(R.layout.temp_section_footer, null);
						Button button = (Button) footer.findViewById(R.id.button_more);
						FontManager.getInstance().setTypefaceDefault(button);
						button.setText(R.string.candi_section_candigrams_more);
						button.setTag("candi");
						section.setFooter(footer); // Replaces if there already is one.
					}

					final FlowLayout flow = (FlowLayout) findViewById(R.id.flow_candi);
					drawCandi(this, flow, mAddedCandi.size() > getResources().getInteger(R.integer.candi_flow_limit)
							? mAddedCandi.subList(0, getResources().getInteger(R.integer.candi_flow_limit))
							: mAddedCandi, R.layout.temp_tuning_candi_item);

					setVisibility(findViewById(R.id.section_candi), View.VISIBLE);
				}
			}

			drawImage(entity);

		}
	}

	private static void drawCandi(Context context, FlowLayout layout, List<Entity> entities, Integer viewResId) {

		layout.removeAllViews();
		final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		final View parentView = (View) layout.getParent();
		Integer layoutWidthPixels = metrics.widthPixels
				- (parentView.getPaddingLeft() + parentView.getPaddingRight() + layout.getPaddingLeft() + layout.getPaddingRight());
		final Integer bonusPadding = ImageUtils.getRawPixels(context, 20);
		layoutWidthPixels -= bonusPadding;

		final Integer spacing = 3;
		final Integer spacingHorizontalPixels = ImageUtils.getRawPixels(context, spacing);
		final Integer spacingVerticalPixels = ImageUtils.getRawPixels(context, spacing);

		Integer desiredWidthPixels = (int) (metrics.density * 75);
		if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
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
		for (Entity entity : entities) {

			View view = inflater.inflate(viewResId, null);
			WebImageView webImageView = (WebImageView) view.findViewById(R.id.image);

			TextView title = (TextView) view.findViewById(R.id.title);
			TextView badgeUpper = (TextView) view.findViewById(R.id.badge_upper);
			@SuppressWarnings("unused")
			TextView badgeLower = (TextView) view.findViewById(R.id.badge_lower);

			FontManager.getInstance().setTypefaceDefault(title);
			FontManager.getInstance().setTypefaceDefault(badgeUpper);

			if (entity.name != null && !entity.name.equals("")) {
				title.setText(entity.name);
			}
			else {
				title.setVisibility(View.GONE);
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

	private void drawImage(Entity entity) {
		if (mImageViewPicture != null) {

			mImageViewPicture.getImageView().clearColorFilter();
			mImageViewPicture.getImageView().setBackgroundResource(0);
			if (findViewById(R.id.color_layer) != null) {
				(findViewById(R.id.color_layer)).setBackgroundResource(0);
				(findViewById(R.id.color_layer)).setVisibility(View.GONE);
				(findViewById(R.id.reverse_layer)).setVisibility(View.GONE);
			}

			if (mEntityBitmap != null) {
				mImageViewPicture.hideLoading();
				ImageUtils.showImageInImageView(mEntityBitmap, mImageViewPicture.getImageView(), true, AnimUtils.fadeInMedium());
				mImageViewPicture.setVisibility(View.VISIBLE);
			}
			else {
				if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
					if (entity.photo == null && entity.place != null && entity.place.category != null) {

						final int color = Place.getCategoryColor((entity.place.category != null)
								? entity.place.category.name
								: null, true, mMuteColor, false);

						mImageViewPicture.getImageView().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
						mColorResId = Place.getCategoryColorResId((entity.place != null && entity.place.category != null) ? entity.place.category.name : null,
								true, mMuteColor, false);

						if (findViewById(R.id.color_layer) != null) {
							(findViewById(R.id.color_layer)).setBackgroundResource(mColorResId);
							(findViewById(R.id.color_layer)).setVisibility(View.VISIBLE);
							(findViewById(R.id.reverse_layer)).setVisibility(View.VISIBLE);
						}
						else {
							mImageViewPicture.getImageView().setBackgroundResource(mColorResId);
						}
					}
				}

				final String imageUri = entity.getEntityPhotoUri();
				final BitmapRequest bitmapRequest = new BitmapRequest(imageUri, mImageViewPicture.getImageView());
				bitmapRequest.setImageSize(mImageViewPicture.getSizeHint());
				bitmapRequest.setImageRequestor(mImageViewPicture.getImageView());
				mImageViewPicture.getImageView().setTag(imageUri);
				BitmapManager.getInstance().masterFetch(bitmapRequest);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event bus routines
	// --------------------------------------------------------------------------------------------

	@Subscribe
	@SuppressWarnings("ucd")
	public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

		if (mTuningInProcess) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Logger.d(TuningWizard.this, "Query wifi scan received event: locking beacons");
					if (event.wifiList != null) {
						ProxiManager.getInstance().lockBeacons();
					}
					else {
						/*
						 * We fake that the tuning happened because it is simpler than enabling/disabling ui
						 */
						disableEvents();
						setSupportProgressBarIndeterminateVisibility(false);
						mCommon.hideBusy(true);
						if (mUntuning) {
							mButtonUntune.setText(R.string.form_button_tuning_tuned);
							mUntuned = true;
						}
						else {
							mButtonTune.setText(R.string.form_button_tuning_tuned);
							mTuned = true;
						}
						toggleStarOn(R.id.image_star_tune);
						mTuningInProcess = false;
					}
				}
			});
		}
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onBeaconsLocked(BeaconsLockedEvent event) {

		if (mTuningInProcess) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Logger.d(TuningWizard.this, "Beacons locked event: tune entity");
					disableEvents();
					tuneProximity();
				}
			});
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		if (isDirty()) {
			confirmDirtyExit();
		}
		else {
			setResult(Activity.RESULT_CANCELED);
			finish();
			AnimUtils.doOverridePendingTransition(TuningWizard.this, TransitionType.FormToPage);
		}
	}

	@SuppressWarnings("ucd")
	public void onChangePictureButtonClick(View view) {
		mCommon.showPictureSourcePicker(mEntityForForm.id, null);
		mImageRequestWebImageView = mImageViewPicture;
		mImageRequestListener = new RequestListener() {

			@Override
			public void onComplete(Object response
					, Photo photo
					, String imageUri
					, Bitmap imageBitmap
					, String title
					, String description
					, Boolean bitmapLocalOnly) {

				final ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {

					mDirty = true;
					mEntityBitmapLocalOnly = bitmapLocalOnly;
					/* Could get set to null if we are using the default */
					mEntityBitmap = imageBitmap;
					if (photo != null) {
						mEntityForForm.photo = photo;
					}
					else if (imageUri != null) {
						mEntityForForm.photo = new Photo(imageUri, null, null, null, PhotoSource.aircandi);
					}
					drawImage(mEntityForForm);
				}
			}
		};
	}

	@SuppressWarnings("ucd")
	public void onEditButtonClick(View view) {
		Tracker.sendEvent("ui_action", "edit_entity", null, 0, Aircandi.getInstance().getUser());
		final IntentBuilder intentBuilder = new IntentBuilder(this, EntityForm.class)
				.setCommandType(CommandType.Edit)
				.setEntityId(mEntityForForm.id)
				.setParentEntityId(mEntityForForm.parentId)
				.setEntityType(mEntityForForm.type);
		final Intent intent = intentBuilder.create();
		startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@SuppressWarnings("ucd")
	public void onTuneButtonClick(View view) {
		if (!mTuned) {
			Tracker.sendEvent("ui_action", "tune_place", null, 0, Aircandi.getInstance().getUser());
			mUntuning = false;
			mCommon.showBusy(R.string.progress_tuning, true);
			if (NetworkManager.getInstance().isWifiEnabled()) {
				mTuningInProcess = true;
				enableEvents();
				ProxiManager.getInstance().scanForWifi(ScanReason.query);
			}
			else {
				tuneProximity();
			}
		}
	}

	@SuppressWarnings("ucd")
	public void onUntuneButtonClick(View view) {
		if (!mUntuned) {
			Tracker.sendEvent("ui_action", "untune_place", null, 0, Aircandi.getInstance().getUser());
			mUntuning = true;
			mCommon.showBusy(R.string.progress_tuning, true);
			if (NetworkManager.getInstance().isWifiEnabled()) {
				mTuningInProcess = true;
				enableEvents();
				ProxiManager.getInstance().scanForWifi(ScanReason.query);
			}
			else {
				tuneProximity();
			}
		}
	}

	public void onCandiClick(View view) {
		final Entity entity = (Entity) view.getTag();

		final IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class)
				.setCommandType(CommandType.View)
				.setEntityId(entity.id)
				.setParentEntityId(entity.parentId)
				.setEntityType(entity.type);

		if (entity.parentId != null) {
			intentBuilder.setCollectionId(entity.getParent().id);
		}

		final Intent intent = intentBuilder.create();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToPage);
	}

	@SuppressWarnings("ucd")
	public void onAddCandiButtonClick(View view) {
		Tracker.sendEvent("ui_action", "add_candi", null, 0, Aircandi.getInstance().getUser());
		if (!mEntityForForm.locked || mEntityForForm.ownerId.equals(Aircandi.getInstance().getUser().id)) {
			mCommon.showTemplatePicker(false);
		}
		else {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.alert_entity_locked)
					, null
					, this, android.R.string.ok, null, null, null, null);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode != Activity.RESULT_CANCELED) {
			if (resultCode == CandiConstants.RESULT_ENTITY_INSERTED) {
				if (requestCode == CandiConstants.ACTIVITY_ENTITY_INSERT) {
					if (intent != null && intent.getExtras() != null) {

						final Bundle extras = intent.getExtras();
						final String entityId = extras.getString(CandiConstants.EXTRA_ENTITY_ID);
						final Entity entity = ProxiManager.getInstance().getEntityModel().getCacheEntity(entityId);
						mAddedCandi.add(entity);
						draw();
						toggleStarOn(R.id.image_star_add_candi);
					}
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_PICTURE_SOURCE_PICK) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String pictureSource = extras.getString(CandiConstants.EXTRA_PICTURE_SOURCE);
					if (pictureSource != null && !pictureSource.equals("")) {
						if (pictureSource.equals("search")) {
							pictureSearch();
						}
						else if (pictureSource.equals("gallery")) {
							pictureFromGallery();
						}
						else if (pictureSource.equals("camera")) {
							pictureFromCamera();
						}
						else if (pictureSource.equals("place")) {
							pictureFromPlace(mEntityForForm.id);
						}
						else if (pictureSource.equals("default")) {
							usePictureDefault(mEntityForForm);
						}
					}
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_TEMPLATE_PICK) {
				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String entityType = extras.getString(CandiConstants.EXTRA_ENTITY_TYPE);
					if (entityType != null && !entityType.equals("")) {

						final IntentBuilder intentBuilder = new IntentBuilder(this, EntityForm.class)
								.setCommandType(CommandType.New)
								.setEntityId(null)
								.setParentEntityId(mCommon.mEntityId)
								.setEntityType(entityType);

						final Intent redirectIntent = intentBuilder.create();
						startActivityForResult(redirectIntent, CandiConstants.ACTIVITY_ENTITY_INSERT);
						AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
					}
				}
			}
			else {
				super.onActivityResult(requestCode, resultCode, intent);
			}
		}
	}

	private void usePictureDefault(Entity entity) {
		/*
		 * Setting the photo to null will trigger correct default handling.
		 */
		if (entity.photo != null) {
			entity.photo.setBitmap(null);
			entity.photo = null;
		}
		mEntityBitmap = null;
		mEntityBitmapLocalOnly = false;
		drawImage(entity);
		doSave();
		Tracker.sendEvent("ui_action", "set_entity_picture_to_default", null, 0, Aircandi.getInstance().getUser());
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void doSave() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("InsertUpdateEntity");
				ModelResult result = new ModelResult();

				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						mCommon.showBusy(R.string.progress_saving, true);
					}
				});

				/*
				 * Pull all the control values back into the entity object being used to
				 * update the service. Because the entity reference comes from an entity model
				 * collection, that entity gets updated.
				 */
				result = updateEntityAtService();

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					toggleStarOn(R.id.image_star_banner);
					ImageUtils.showToastNotification(getString(R.string.alert_updated), Toast.LENGTH_SHORT);
					setResult(CandiConstants.RESULT_ENTITY_UPDATED);
				}
				return result.serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ServiceResponse serviceResponse = (ServiceResponse) response;
				mCommon.hideBusy(true);
				if (serviceResponse.responseCode == ResponseCode.Success) {
					setResult(Activity.RESULT_OK);
					finish();
					AnimUtils.doOverridePendingTransition(TuningWizard.this, TransitionType.FormToPage);
				}
				else {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.CandiSave, TuningWizard.this);
				}
			}

		}.execute();
	}

	private Boolean isDirty() {
		return mDirty;
	}

	private void confirmDirtyExit() {
		final AlertDialog dialog = AircandiCommon.showAlertDialog(null
				, getResources().getString(R.string.alert_tuning_dirty_exit_title)
				, getResources().getString(R.string.alert_tuning_dirty_exit_message)
				, null
				, TuningWizard.this
				, R.string.alert_dirty_save
				, android.R.string.cancel
				, R.string.alert_dirty_discard
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							doSave();
						}
						else if (which == Dialog.BUTTON_NEUTRAL) {
							setResult(Activity.RESULT_CANCELED);
							finish();
							AnimUtils.doOverridePendingTransition(TuningWizard.this, TransitionType.FormToPage);
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	private void tuneProximity() {
		/*
		 * If there are beacons:
		 * 
		 * - links to beacons created.
		 * - link_proximity action logged.
		 * 
		 * If no beacons:
		 * 
		 * - no links are created.
		 * - entity_proximity action logged.
		 */
		final List<Beacon> beacons = ProxiManager.getInstance().getStrongestBeacons(5);
		final Beacon primaryBeacon = (beacons.size() > 0) ? beacons.get(0) : null;

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("TrackEntityProximity");

				String actionType = "proximity";
				final Boolean hasActiveProximityLink = mEntityForForm.hasActiveProximityLink();
				if (!hasActiveProximityLink) {
					actionType = "proximity_first";
				}

				final ModelResult result = ProxiManager.getInstance().getEntityModel()
						.trackEntity(mEntityForForm, beacons, primaryBeacon, actionType, mUntuning);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				setSupportProgressBarIndeterminateVisibility(false);
				mCommon.hideBusy(true);
				if (mUntuning) {
					mButtonUntune.setText(R.string.form_button_tuning_tuned);
					mUntuned = true;
				}
				else {
					mButtonTune.setText(R.string.form_button_tuning_tuned);
					mTuned = true;
					if (mButtonUntune.getVisibility() != View.VISIBLE) {
						mButtonUntune.setVisibility(View.VISIBLE);
					}
				}
				toggleStarOn(R.id.image_star_tune);
				mTuningInProcess = false;
			}
		}.execute();
	}

	private ModelResult updateEntityAtService() {
		Tracker.sendEvent("ui_action", "entity_update", mEntityForForm.type, 0, Aircandi.getInstance().getUser());
		Bitmap bitmap = mEntityBitmap;
		if (mEntityBitmapLocalOnly) {
			bitmap = null;
		}

		List<Entity> entities = mEntityForForm.getChildren();

		/* Something in the call caused us to lose the most recent picture. */
		ModelResult result = ProxiManager.getInstance().getEntityModel().updateEntity(mEntityForForm, bitmap);

		if (mEntityForForm.type.equals(CandiConstants.TYPE_CANDI_PLACE) && mEntityForForm.photo != null) {

			entities = mEntityForForm.getChildren();
			Boolean candiMatch = false;
			for (Entity entity : entities) {
				if (entity.type.equals(CandiConstants.TYPE_CANDI_PICTURE)) {
					if (entity.getPhoto().getUri().equals(mEntityForForm.getPhoto().getUri())) {
						candiMatch = true;
						break;
					}
				}
			}

			if (!candiMatch) {
				Entity pictureEntity = makeEntity(CandiConstants.TYPE_CANDI_PICTURE);
				pictureEntity.photo = mEntityForForm.photo.clone();
				pictureEntity.parentId = mEntityForForm.id;
				result = ProxiManager.getInstance().getEntityModel().insertEntity(pictureEntity, null, null, null, false, true);
			}
		}

		return result;
	}

	private Entity makeEntity(String type) {
		if (type == null) {
			throw new IllegalArgumentException("TuningWizard.makeEntity(): type parameter is null");
		}
		final Entity entity = new Entity();
		entity.signalFence = -100.0f;
		entity.locked = false;
		entity.isCollection = (type.equals(CandiConstants.TYPE_CANDI_PLACE));
		entity.visibility = Visibility.Public.toString().toLowerCase(Locale.US);
		entity.type = type;
		if (type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
			entity.getPlace().setProvider(new Provider(Aircandi.getInstance().getUser().id, "user"));
		}
		return entity;
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.accept) {
			if (isDirty()) {
				doSave();
			}
			else {
				setResult(Activity.RESULT_OK);
				finish();
				AnimUtils.doOverridePendingTransition(this, TransitionType.FormToPage);
			}
			return true;
		}

		/* In case we add general menu items later */
		mCommon.doOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onStop() {
		disableEvents();
		super.onStop();
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
		catch (Exception e) {} // $codepro.audit.disable emptyCatchClause
	}

	private void toggleStarOn(final Integer starId) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (mCommon.mThemeTone.equals("dark")) {
					((ImageView) findViewById(starId)).setImageResource(R.drawable.ic_action_star_10_dark);
				}
				else {
					((ImageView) findViewById(starId)).setImageResource(R.drawable.ic_action_star_10_light);
				}

			}
		});
	}

	@Override
	protected int getLayoutID() {
		return R.layout.tuning_wizard;
	}
}