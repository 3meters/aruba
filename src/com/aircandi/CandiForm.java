package com.aircandi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.builders.CandiPicker;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.CandiPagerAdapter;
import com.aircandi.components.CommandType;
import com.aircandi.components.DrawableManager.ViewHolder;
import com.aircandi.components.FontManager;
import com.aircandi.components.ImageManager;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.EntityListType;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Entity.ImageFormat;
import com.aircandi.service.objects.GeoLocation;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Phrase;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Tip;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;
import com.aircandi.widgets.CandiView;
import com.aircandi.widgets.HorizontalScrollLayout;
import com.aircandi.widgets.ListViewExpanded;
import com.aircandi.widgets.SectionLayout;
import com.aircandi.widgets.TextViewEllipsizing;
import com.aircandi.widgets.UserView;
import com.aircandi.widgets.WebImageView;

public class CandiForm extends CandiActivity {

	protected List<Entity>	mEntitiesForPaging	= new ArrayList<Entity>();
	protected ViewPager		mViewPager;
	protected Entity		mEntity;
	protected Number		mEntityModelRefreshDate;
	protected Number		mEntityModelActivityDate;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			initialize();
			bind(false);
		}
	}

	public void bind(Boolean refresh) {
		doBind(refresh, false);
	}

	public void initialize() {

		/* Font for button bar */
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_comment));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_edit));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_move));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_new_text));

	}

	public void doBind(final Boolean refresh, final Boolean pagingEnabled) {
		/*
		 * Navigation setup for action bar icon and title
		 */
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				ModelResult result = new ModelResult();
				Entity entity = ProxiExplorer.getInstance().getEntityModel().getCacheEntity(mCommon.mEntityId);
				if (entity == null || !entity.synthetic) {
					result = ProxiExplorer.getInstance().getEntityModel().getEntity(mCommon.mEntityId, refresh, null, null);
				}
				else {
					result.data = entity;
				}
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

						/* Sort the children if there are any */
						if (mEntity.getChildren().size() > 1) {
							Collections.sort(mEntity.getChildren(), new Entity.SortEntitiesByModifiedDate());
						}

						/*
						 * The set of entities to page are built up by the pager. We only pass the entities
						 * if paging is disabled and we only want to show the current entity.
						 */
						List<Entity> entities = null;
						if (!pagingEnabled) {
							entities = new ArrayList<Entity>();
							entities.add(mEntity);
						}
						updateViewPager(entities);
					}
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiForm);
				}
				mCommon.hideBusy();
			}

		}.execute();
	}

	public void doRefresh() {
		/*
		 * Called from AircandiCommon
		 */
		bind(true);
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

	public void onMoveCandiButtonClick(View view) {
		showCandiPicker();
	}

	public void onMapButtonClick(View view) {
		GeoLocation location = mEntity.getLocation();
		AndroidManager.getInstance().callMapActivity(this, String.valueOf(location.latitude.doubleValue())
				, String.valueOf(location.longitude.doubleValue())
				, mEntity.name);
	}

	public void onTuneButtonClick(View view) {
		tune();
	}

	public void onCallButtonClick(View view) {
		AndroidManager.getInstance().callDialerActivity(this, mEntity.place.contact.phone);
	}

	public void onMoreButtonClick(View view) {
		String target = (String) view.getTag();
		if (target.equals("photos")) {
			IntentBuilder intentBuilder = new IntentBuilder(this, PictureBrowse.class);
			intentBuilder.setCommandType(CommandType.View)
					.setEntityId(mEntity.id);

			Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.CandiListToCandiForm);
		}
		else if (target.equals("tips")) {
			IntentBuilder intentBuilder = new IntentBuilder(this, TipList.class);
			intentBuilder.setCommandType(CommandType.View)
					.setEntityId(mEntity.id)
					.setCollectionId(mEntity.id);

			Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.CandiListToCandiForm);
		}
		else if (target.equals("candi")) {
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

	public void onTwitterButtonClick(View view) {
		AndroidManager.getInstance().callTwitterActivity(this, mEntity.place.contact.twitter);
	}

	public void onFacebookButtonClick(View view) {
		AndroidManager.getInstance().callFacebookActivity(this, mEntity.place.facebook);
	}

	public void onMenuButtonClick(View view) {
		AndroidManager.getInstance().callBrowserActivity(this, mEntity.place.menu.mobileUri != null ? mEntity.place.menu.mobileUri : mEntity.place.menu.uri);
	}

	public void onPhotoClick(View view) {
		List<Photo> photos = mEntity.place.photos;
		ProxiExplorer.getInstance().getEntityModel().setPhotos(photos);
		Photo photo = (Photo) view.getTag();
		Intent intent = new Intent(this, PictureDetail.class);
		intent.putExtra(CandiConstants.EXTRA_URI, photo.getImageUri());
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	public void onCandiClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity.type.equals(CandiConstants.TYPE_CANDI_SOURCE)) {
			if (entity.source.equals("twitter")) {
				AndroidManager.getInstance().callTwitterActivity(this, entity.sourceId);
			}
			else if (entity.source.equals("facebook")) {
				AndroidManager.getInstance().callFacebookActivity(this, entity.sourceId);
			}
			else if (entity.source.equals("website")) {
				AndroidManager.getInstance().callBrowserActivity(this, entity.sourceId);
			}
		}
		else {
			mCommon.showCandiFormForEntity(entity, CandiForm.class);
		}
	}

	public void onImageClick(View view) {
		Intent intent = null;
		Photo photo = mEntity.photo.hasDetail() ? mEntity.photo.getDetail() : mEntity.photo;
		if (photo.getImageFormat() == ImageFormat.Binary) {
			photo.setCreatedAt(mEntity.modifiedDate);
			photo.setTitle(mEntity.name);
			photo.setUser(mEntity.creator);
			ProxiExplorer.getInstance().getEntityModel().getPhotos().clear();
			ProxiExplorer.getInstance().getEntityModel().getPhotos().add(photo);
			intent = new Intent(this, PictureDetail.class);
			intent.putExtra(CandiConstants.EXTRA_URI, mEntity.photo.getImageUri());

			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
		}
		else {
			AndroidManager.getInstance().callBrowserActivity(this, photo.getImageUri());
		}
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
		IntentBuilder intentBuilder = new IntentBuilder(this, EntityForm.class)
				.setCommandType(CommandType.Edit)
				.setEntityId(mEntity.id)
				.setParentEntityId(mEntity.parentId)
				.setEntityType(mEntity.type);
		Intent intent = intentBuilder.create();
		startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
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

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	public void tune() {

		final List<Beacon> beacons = ProxiExplorer.getInstance().getStrongestWifiAsBeacons(5);
		final Beacon primaryBeacon = beacons.size() > 0 ? beacons.get(0) : null;

		new AsyncTask<Object, Object, Object>() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy(R.string.progress_tuning);
			}

			@Override
			protected Object doInBackground(Object... params) {
				List<Entity> entities = new ArrayList<Entity>();
				entities.add(mEntity);
				ServiceResponse serviceResponse = ProxiExplorer.getInstance().tune(beacons, primaryBeacon, entities);
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				ServiceResponse serviceResponse = (ServiceResponse) response;
				setSupportProgressBarIndeterminateVisibility(false);
				mCommon.hideBusy();
				if (serviceResponse.responseCode != ResponseCode.Success) {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.Tuning);
				}
				else {
					if (mEntity.synthetic) {
						if (serviceResponse.data != null) {
							/*
							 * Has map to go from original id to new id
							 */
							HashMap map = (HashMap) serviceResponse.data;
							String newId = (String) map.get(mCommon.mEntityId);
							mCommon.mEntityId = newId;
						}
						bind(true);
					}
				}
			}

		}.execute();
	}

	static public ViewGroup buildCandiForm(Context context, final Entity entity, final ViewGroup layout, GeoLocation mLocation, boolean refresh) {
		/*
		 * For now, we assume that the candi form isn't recycled.
		 * 
		 * We leave most of the views visible by default so they are visible in the layout editor.
		 * 
		 * - WebImageView primary image is visible by default
		 * - WebImageView child views are gone by default
		 * - Header views are visible by default
		 */
		final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final CandiView candiView = (CandiView) layout.findViewById(R.id.candi_view);
		final WebImageView image = (WebImageView) layout.findViewById(R.id.candi_form_image);
		final TextView title = (TextView) layout.findViewById(R.id.candi_form_title);
		final TextView subtitle = (TextView) layout.findViewById(R.id.candi_form_subtitle);

		final TextView description = (TextView) layout.findViewById(R.id.candi_form_description);
		final TextView address = (TextView) layout.findViewById(R.id.candi_form_address);
		final UserView author = (UserView) layout.findViewById(R.id.author);

		if (candiView != null) {
			candiView.setBadgeColorFilter(null, null, null, "#ffffff");
			candiView.bindToEntity(entity);
		}
		else {

			if (image != null) {
				String imageUri = entity.getImageUri();
				if (imageUri != null) {

					ImageFormat imageFormat = entity.getImageFormat();
					ImageRequestBuilder builder = new ImageRequestBuilder(image)
							.setImageUri(imageUri)
							.setImageFormat(imageFormat)
							.setLinkZoom(CandiConstants.LINK_ZOOM)
							.setLinkJavascriptEnabled(CandiConstants.LINK_JAVASCRIPT_ENABLED);

					ImageRequest imageRequest = builder.create();

					image.setImageRequest(imageRequest);

					if (entity.type.equals(CandiConstants.TYPE_CANDI_FOLDER)) {
						if (entity.getImageUri() == null
								|| !entity.getImageUri().toLowerCase().startsWith("resource:")) {
							image.getImageBadge().setImageResource(R.drawable.ic_collection_250);
							image.getImageBadge().setVisibility(View.VISIBLE);
							image.getImageZoom().setVisibility(View.VISIBLE);
							image.setClickable(true);
						}
						else {
							image.getImageBadge().setVisibility(View.GONE);
							image.getImageZoom().setVisibility(View.GONE);
							image.setClickable(false);
						}
					}
					else if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
						//					if (entity.place.categories != null && entity.place.categories.size() > 0) {
						//						ViewHolder holder = new ViewHolder();
						//						holder.itemImage = image.getImageBadge();
						//						holder.itemImage.setTag(entity.place.categories.get(0).iconUri());
						//						ImageManager.getInstance().getDrawableManager().fetchDrawableOnThread(entity.place.categories.get(0).iconUri(), holder, null);
						//						image.getImageBadge().setVisibility(View.VISIBLE);
						//						image.getImageZoom().setVisibility(View.GONE);
						//						image.setClickable(false);
						//					}
						//					else {
						image.getImageBadge().setVisibility(View.GONE);
						image.getImageZoom().setVisibility(View.GONE);
						image.setClickable(false);
						//					}
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

		/* Candi */

		setVisibility(layout.findViewById(R.id.section_candi), View.GONE);
		int visibleChildrenCount = entity.getChildren().size();
		if (visibleChildrenCount > 0) {

			SectionLayout section = (SectionLayout) layout.findViewById(R.id.section_candi);
			if (section != null) {
				section.getTextViewHeader().setText(String.valueOf(visibleChildrenCount) + " " + context.getString(R.string.candi_section_candi));
				HorizontalScrollLayout list = (HorizontalScrollLayout) layout.findViewById(R.id.list_candi);

				for (Entity childEntity : entity.getChildren()) {
					View view = inflater.inflate(R.layout.temp_place_candi_item, null);
					WebImageView webImageView = (WebImageView) view.findViewById(R.id.image);

					String imageUri = childEntity.getImageUri();
					ImageRequestBuilder builder = new ImageRequestBuilder(webImageView)
							.setImageUri(imageUri)
							.setImageFormat(childEntity.getImageFormat())
							.setLinkZoom(CandiConstants.LINK_ZOOM)
							.setLinkJavascriptEnabled(CandiConstants.LINK_JAVASCRIPT_ENABLED);

					ImageRequest imageRequest = builder.create();
					webImageView.setImageRequest(imageRequest);
					webImageView.setTag(childEntity);

					list.addView(view);
				}

				if (visibleChildrenCount > 3) {
					View footer = inflater.inflate(R.layout.temp_section_footer, null);
					Button button = (Button) footer.findViewById(R.id.button_more);
					FontManager.getInstance().setTypefaceDefault(button);
					button.setText(R.string.candi_section_candi_more);
					button.setTag("candi");
					section.addView(footer);
				}

				setVisibility(layout.findViewById(R.id.section_candi), View.VISIBLE);
			}
		}

		/* Hidden unless it's a place */
		setVisibility(layout.findViewById(R.id.label_tuning_score), View.GONE);
		setVisibility(layout.findViewById(R.id.tuning_score), View.GONE);

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

			/* Tuning score */
			setVisibility(layout.findViewById(R.id.label_tuning_score), View.GONE);
			setVisibility(layout.findViewById(R.id.tuning_score), View.GONE);
			if (!entity.synthetic) {
				FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.label_tuning_score));
				FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.tuning_score));
				setVisibility(layout.findViewById(R.id.label_tuning_score), View.VISIBLE);
				setVisibility(layout.findViewById(R.id.tuning_score), View.VISIBLE);
				TextView text = (TextView) layout.findViewById(R.id.tuning_score);
				text.setText(String.valueOf(entity.getTuningScore()));
			}

			/* Photos */
			setVisibility(layout.findViewById(R.id.section_photos), View.GONE);
			if (place.photos != null && place.photos.size() > 0) {
				SectionLayout section = (SectionLayout) layout.findViewById(R.id.section_photos);
				if (section != null) {
					section.getTextViewHeader().setText(String.valueOf(place.photoCount) + " " + context.getString(R.string.candi_section_photos));
					HorizontalScrollLayout list = (HorizontalScrollLayout) layout.findViewById(R.id.list_photos);

					for (Photo photo : place.photos) {
						ViewHolder holder = new ViewHolder();
						View view = inflater.inflate(R.layout.temp_place_photo_item, null);
						WebImageView webImageView = (WebImageView) view.findViewById(R.id.image);
						webImageView.setTag(photo);
						holder.itemImage = webImageView.getImageView();
						holder.itemImage.setTag(photo.getImageSizedUri(100, 100));
						holder.itemImage.setImageBitmap(null);
						ImageManager.getInstance().getDrawableManager().fetchDrawableOnThread(photo.getImageSizedUri(100, 100), holder, null);
						list.addView(view);
					}

					if (place.photoCount > place.photos.size()) {
						TextView button = section.getButtonMore();
						button.setText(R.string.candi_section_photos_more);
						button.setTag("photos");
						setVisibility(button, View.VISIBLE);
					}

					setVisibility(layout.findViewById(R.id.section_photos), View.VISIBLE);
				}
			}

			/* Phrases */
			setVisibility(layout.findViewById(R.id.section_phrases), View.GONE);
			if (place.phrases != null && place.phrases.size() > 0) {
				SectionLayout section = (SectionLayout) layout.findViewById(R.id.section_phrases);
				if (section != null) {
					ListViewExpanded list = (ListViewExpanded) layout.findViewById(R.id.list_phrases);
					list.setAdapter(new BaseAdapter() {

						@Override
						public int getCount() {
							return place.phrases.size();
						}

						@Override
						public Object getItem(int position) {
							return null;
						}

						@Override
						public long getItemId(int position) {
							return 0;
						}

						@Override
						public View getView(int position, View convertView, ViewGroup parent) {
							Phrase phrase = place.phrases.get(position);
							View view = inflater.inflate(R.layout.temp_listitem_phrase, null);

							TextViewEllipsizing phraseText = (TextViewEllipsizing) view.findViewById(R.id.phrase);
							FontManager.getInstance().setTypefaceDefault(phraseText);
							String sampleText = new String(phrase.sampleText);
							sampleText = sampleText.replace(phrase.phrase, "<font color='#ffaa00'>" + phrase.phrase + "</font>");
							sampleText += " <font color='#aaaaaa'>(" + String.valueOf(phrase.sampleCount) + " tips)</font>";
							phraseText.setText(Html.fromHtml(sampleText));

							return view;
						}
					});
					setVisibility(layout.findViewById(R.id.section_phrases), View.VISIBLE);
				}
			}

			/* Tips */
			setVisibility(layout.findViewById(R.id.section_tips), View.GONE);
			if (place.tips != null && place.tips.size() > 0) {
				SectionLayout section = (SectionLayout) layout.findViewById(R.id.section_tips);
				if (section != null) {
					section.getTextViewHeader().setText(String.valueOf(place.tipCount) + " " + context.getString(R.string.candi_section_tips));
					ListViewExpanded list = (ListViewExpanded) layout.findViewById(R.id.list_tips);
					list.setAdapter(new BaseAdapter() {

						@Override
						public int getCount() {
							return place.tips.size();
						}

						@Override
						public Object getItem(int position) {
							return null;
						}

						@Override
						public long getItemId(int position) {
							return 0;
						}

						@Override
						public View getView(int position, View convertView, ViewGroup parent) {
							Tip tip = place.tips.get(position);
							View view = inflater.inflate(R.layout.temp_listitem_tip, null);

							WebImageView webImageView = (WebImageView) view.findViewById(R.id.image);
							TextViewEllipsizing description = (TextViewEllipsizing) view.findViewById(R.id.description);
							UserView user = (UserView) view.findViewById(R.id.author);

							/* Tip text */
							FontManager.getInstance().setTypefaceDefault(description);
							description.setText(tip.text);

							/* Author block */
							if (user != null) {
								setVisibility(user, View.GONE);
								if (tip.user != null) {
									user.bindToAuthor(tip.user, tip.createdAt.longValue(), false);
									setVisibility(user, View.VISIBLE);
								}
							}

							ViewHolder holder = new ViewHolder();
							holder.itemImage = webImageView.getImageView();
							holder.itemImage.setTag(tip.user.photo.getImageSizedUri(100, 100));
							holder.itemImage.setImageBitmap(null);
							ImageManager.getInstance().getDrawableManager().fetchDrawableOnThread(tip.user.photo.getImageSizedUri(100, 100), holder, null);

							return view;
						}
					});

					if (place.tipCount > list.getItemMaxCount()) {
						TextView button = section.getButtonMore();
						button.setText(R.string.candi_section_tips_more);
						button.setTag("tips");
						setVisibility(button, View.VISIBLE);
					}

					setVisibility(layout.findViewById(R.id.section_tips), View.VISIBLE);
				}
			}
		}

		/* Author block */

		setVisibility(author, View.GONE);
		if (author != null && entity.creator != null) {
			author.bindToAuthor(entity.creator, entity.modifiedDate.longValue(), entity.locked);
			setVisibility(author, View.VISIBLE);
		}

		/* Buttons */
		buildCandiButtons(context, entity, layout, mLocation);

		return layout;
	}

	static private void buildCandiButtons(Context context, final Entity entity, final ViewGroup layout, GeoLocation mLocation) {

		setVisibility(layout.findViewById(R.id.button_map), View.GONE);
		setVisibility(layout.findViewById(R.id.button_call), View.GONE);
		setVisibility(layout.findViewById(R.id.button_tune), View.GONE);

		setVisibility(layout.findViewById(R.id.button_comment), View.GONE);
		setVisibility(layout.findViewById(R.id.button_new), View.GONE);
		setVisibility(layout.findViewById(R.id.button_edit), View.GONE);
		setVisibility(layout.findViewById(R.id.button_move), View.GONE);

		setVisibility(layout.findViewById(R.id.button_menu), View.GONE);
		setVisibility(layout.findViewById(R.id.form_button_link), View.GONE);
		setVisibility(layout.findViewById(R.id.button_twitter), View.GONE);
		setVisibility(layout.findViewById(R.id.button_website), View.GONE);
		setVisibility(layout.findViewById(R.id.button_facebook), View.GONE);

		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_map));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_call));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_tune));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_comment));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_new_text));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_edit));
		FontManager.getInstance().setTypefaceDefault((TextView) layout.findViewById(R.id.button_move));

		if (entity.locked != null && !entity.locked) {
			if (entity.isCollection != null && entity.isCollection) {
				setVisibility(layout.findViewById(R.id.button_new), View.VISIBLE);
			}
		}

		if (entity.locked != null && !entity.locked) {
			setVisibility(layout.findViewById(R.id.button_comment), View.VISIBLE);
		}

		if (entity.creatorId != null && entity.creatorId.equals(Aircandi.getInstance().getUser().id)) {
			setVisibility(layout.findViewById(R.id.button_edit), View.VISIBLE);
			if (entity.isCollection == null || !entity.isCollection) {
				setVisibility(layout.findViewById(R.id.button_move), View.VISIBLE);
			}
		}

		if (!entity.synthetic) {
			setVisibility(layout.findViewById(R.id.form_button_link), View.VISIBLE);
			if (entity.commentCount != null && entity.commentCount > 0) {
				Button button = (Button) layout.findViewById(R.id.form_button_link);
				if (button != null) {
					button.setText(String.valueOf(entity.commentCount) + (entity.commentCount == 1 ? " Comment" : " Comments"));
				}
			}
		}

		/* Map */

		GeoLocation location = entity.getLocation();
		if (location != null) {
			setVisibility(layout.findViewById(R.id.button_map), View.VISIBLE);
		}

		/* Dial, twitter, website, menu */

		if (entity.place != null) {
			Place place = entity.place;

			setVisibility(layout.findViewById(R.id.button_tune), View.VISIBLE);

			if (!entity.place.source.equals("user")) {
				setVisibility(layout.findViewById(R.id.button_edit), View.GONE);
			}

			if (!entity.synthetic && Aircandi.getInstance().getUser().isDeveloper) {
				setVisibility(layout.findViewById(R.id.button_edit), View.VISIBLE);
			}

			if (place.contact != null) {
				if (place.contact.phone != null) {
					setVisibility(layout.findViewById(R.id.button_call), View.VISIBLE);
				}

				if (place.contact.twitter != null && !place.contact.twitter.equals("")) {
					Button button = (Button) layout.findViewById(R.id.button_twitter);
					if (button != null) {
						button.setText("@" + place.contact.twitter);
						setVisibility(button, View.VISIBLE);
					}
				}
			}

			if (place.website != null && !place.website.equals("")) {
				setVisibility(layout.findViewById(R.id.button_website), View.VISIBLE);
			}

			if (place.facebook != null && !place.facebook.equals("")) {
				Button button = (Button) layout.findViewById(R.id.button_facebook);
				if (button != null) {
					button.setText(place.facebook);
					setVisibility(layout.findViewById(R.id.button_facebook), View.VISIBLE);
				}
			}

			if (place.menu != null) {
				if (place.menu.mobileUri != null || place.menu.uri != null) {
					setVisibility(layout.findViewById(R.id.button_menu), View.VISIBLE);
				}
			}
		}
	}

	protected void updateViewPager(List<Entity> entitiesForPaging)
	{
		if (mViewPager == null) {

			mViewPager = (ViewPager) findViewById(R.id.view_pager);

			/*
			 * We clone the collection so our updates don't impact the entity model that
			 * radar relies on. When radar resumes, it should pickup any changes made so it
			 * stays consistent with what the user sees in candi form.
			 */
			for (Entity entity : entitiesForPaging) {
				mEntitiesForPaging.add(entity);
			}

			mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

				@Override
				public void onPageSelected(int position) {
					mEntity = mEntitiesForPaging.get(position);
				}
			});

			mViewPager.setAdapter(new CandiPagerAdapter(this, mViewPager, mEntitiesForPaging));

			synchronized (mEntitiesForPaging) {
				for (int i = 0; i < mEntitiesForPaging.size(); i++) {
					if (mEntitiesForPaging.get(i).id.equals(mEntity.id)) {
						mViewPager.setCurrentItem(i, false);
						break;
					}
				}
			}

		}
		else {

			/* Replace the entity in our local collection */
			for (int i = 0; i < mEntitiesForPaging.size(); i++) {
				Entity entityOld = mEntitiesForPaging.get(i);
				if (entityOld.id.equals(mEntity.id) || entityOld.id.equals(mEntity.place.sourceId)) {
					mEntitiesForPaging.set(i, mEntity);
					break;
				}
			}

			mViewPager.getAdapter().notifyDataSetChanged();
		}
	}

	private void showCandiPicker() {
		IntentBuilder intentBuilder = new IntentBuilder(this, CandiPicker.class);
		Intent intent = intentBuilder.create();
		startActivityForResult(intent, CandiConstants.ACTIVITY_CANDI_PICK);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
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

}