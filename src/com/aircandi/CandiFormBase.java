package com.aircandi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.candi.models.CandiModel;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AnimUtils;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.CandiPagerAdapter;
import com.aircandi.components.CommandType;
import com.aircandi.components.DateUtils;
import com.aircandi.components.EntityList;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.components.ImageUtils;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.EntityTree;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Entity.ImageFormat;
import com.aircandi.service.objects.GeoLocation;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.User;
import com.aircandi.widgets.AuthorBlock;
import com.aircandi.widgets.WebImageView;

public abstract class CandiFormBase extends CandiActivity {

	protected List<Entity>	mEntitiesForPaging	= new ArrayList<Entity>();
	protected ViewPager		mViewPager;
	protected Entity		mEntity;
	protected Number		mEntityModelRefreshDate;
	protected Number		mEntityModelActivityDate;
	protected User			mEntityModelUser;

	public abstract void bind(Boolean useProxiExplorer);

	public void doBind(final Boolean useEntityModel, final Boolean pagingEnabled, EntityTree entityTree) {
		/*
		 * Navigation setup for action bar icon and title
		 */
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);

		if (useEntityModel) {
			/*
			 * Entity is coming from entity model.
			 */
			mEntity = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mEntityId, mCommon.mParentId, entityTree);
			mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
			mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
			mEntityModelUser = Aircandi.getInstance().getUser();

			/* Was likely deleted from the entity model */
			if (mEntity == null) {
				onBackPressed();
			}
			else {
				mCommon.mActionBar.setTitle(mEntity.title);
				/* Get the view pager configured */
				List<Entity> entities = null;
				if (!pagingEnabled) {
					entities = new ArrayList<Entity>();
					entities.add(mEntity);
				}
				updateViewPager(entities);
			}
		}
		else {
			/*
			 * Entity is coming from service.
			 */
			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showProgressDialog(true, getString(R.string.progress_loading));
				}

				@Override
				protected Object doInBackground(Object... params) {
					String jsonFields = "{\"entities\":{},\"children\":{},\"parents\":{},\"comments\":{}}";
					String jsonEagerLoad = "{\"children\":true,\"parents\":true,\"comments\":false}";
					ServiceResponse serviceResponse = ProxiExplorer.getInstance().getEntity(mCommon.mEntityId, jsonEagerLoad, jsonFields, null);
					return serviceResponse;
				}

				@Override
				protected void onPostExecute(Object result) {
					ServiceResponse serviceResponse = (ServiceResponse) result;

					if (serviceResponse.responseCode == ResponseCode.Success) {
						mEntity = (Entity) ((ServiceData) serviceResponse.data).data;
						mCommon.mActionBar.setTitle(mEntity.title);

						/* Sort the children if there are any */
						if (mEntity.getChildren() != null && mEntity.getChildren().size() > 1) {
							Collections.sort(mEntity.getChildren(), new EntityList.SortEntitiesByModifiedDate());
						}

						/* Get the view pager configured */
						List<Entity> entities = null;
						if (!pagingEnabled) {
							entities = new ArrayList<Entity>();
							entities.add(mEntity);
						}
						updateViewPager(entities);

						mCommon.showProgressDialog(false, null);
					}
					else {
						mCommon.handleServiceError(serviceResponse, ServiceOperation.CandiForm);
					}
				}

			}.execute();
		}
	}

	public void doRefresh() {
		/*
		 * Called from AircandiCommon
		 * 
		 * Refresh causes us to pull the freshest data from the service but
		 * we do not merge it into the entity model and it is only for the
		 * current entity. Other entities in the same collection are not
		 * refreshed.
		 */
		bind(false);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public abstract void onChildrenButtonClick(View v);

	public void showChildrenForEntity(Class<?> clazz) {
		IntentBuilder intentBuilder = new IntentBuilder(this, clazz);

		/*
		 * mCommon.mEntityId is the original entity the user navigated to but
		 * they could have swiped using the viewpager to a different entity so
		 * we need to use mEntity to get the right entity context.
		 */
		intentBuilder.setCommandType(CommandType.View)
				.setEntityId(mEntity.id)
				.setParentEntityId(mEntity.parentId)
				.setCollectionId(mEntity.id)
				.setEntityTree(mCommon.mEntityTree);

		Intent intent = intentBuilder.create();
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiFormToCandiList);
	}

	public void onBrowseCommentsButtonClick(View view) {
		if (mEntity.commentCount != null && mEntity.commentCount > 0) {
			IntentBuilder intentBuilder = new IntentBuilder(this, CommentList.class);
			intentBuilder.setCommandType(CommandType.View);
			intentBuilder.setEntityId(mEntity.id);
			intentBuilder.setParentEntityId(mEntity.parentId);
			intentBuilder.setCollectionId(mEntity.id);
			intentBuilder.setEntityTree(mCommon.mEntityTree);
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
		launchMapApp();
	}

	private void launchMapApp() {
		GeoLocation entityLocation = mEntity.location != null ? mEntity.location : mCommon.mEntityLocation;
		String latitude = String.valueOf(entityLocation.latitude.doubleValue());
		String longitude = String.valueOf(entityLocation.longitude.doubleValue());
		String uri = "geo:" + latitude + "," + longitude + "?q="
				+ latitude
				+ "," + longitude
				+ "(" + mEntity.label + ")";
		Intent searchAddress = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
		startActivity(searchAddress);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	@SuppressWarnings("unused")
	private void launchMapView() {
		IntentBuilder intentBuilder = new IntentBuilder(this, MapBrowse.class);
		intentBuilder.setCommandType(CommandType.View);
		intentBuilder.setEntityId(mEntity.id);
		intentBuilder.setParentEntityId(mEntity.parentId);
		intentBuilder.setEntityLocation(mCommon.mEntityLocation);
		Intent intent = intentBuilder.create();
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	public void onImageClick(View view) {
		Intent intent = null;
		if (mEntity.imageUri != null && !mEntity.imageUri.equals("")) {
			IntentBuilder intentBuilder = new IntentBuilder(this, PictureBrowse.class);
			intentBuilder.setCommandType(CommandType.View);
			intentBuilder.setEntityId(mEntity.id);
			intentBuilder.setParentEntityId(mEntity.parentId);
			intentBuilder.setEntityTree(mCommon.mEntityTree);
			intent = intentBuilder.create();
		}
		else {
			intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setData(Uri.parse(mEntity.linkUri));
		}
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	public void onAddCandiButtonClick(View view) {
		if (!mEntity.locked) {
			mCommon.showTemplatePicker(false);
		}
		else {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert, null,
					getResources().getString(R.string.alert_entity_locked), this, android.R.string.ok, null, null, null);
		}
	}

	public void onEditCandiButtonClick(View view) {
		IntentBuilder intentBuilder = new IntentBuilder(this, EntityForm.class)
				.setCommandType(CommandType.Edit)
				.setEntityId(mEntity.id)
				.setParentEntityId(mEntity.parentId)
				.setEntityType(mEntity.type)
				.setEntityTree(mCommon.mEntityTree);
		Intent intent = intentBuilder.create();
		startActivity(intent);
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
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert, null,
					getResources().getString(R.string.alert_entity_locked), this, android.R.string.ok, null, null, null);
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
			if (requestCode == CandiConstants.ACTIVITY_TEMPLATE_PICK) {
				if (intent != null && intent.getExtras() != null) {

					Bundle extras = intent.getExtras();
					final String entityType = extras.getString(getString(R.string.EXTRA_ENTITY_TYPE));
					if (entityType != null && !entityType.equals("")) {

						String parentId = null;

						if (mCommon.getCandiPatchModel() != null && !mCommon.getCandiPatchModel().getCandiRootCurrent().isSuperRoot()) {
							CandiModel candiModel = (CandiModel) mCommon.getCandiPatchModel().getCandiRootCurrent();
							parentId = candiModel.getEntity().id;
						}
						else if (mCommon.mEntityId != null) {
							parentId = mCommon.mEntityId;
						}

						IntentBuilder intentBuilder = new IntentBuilder(this, EntityForm.class);
						intentBuilder.setCommandType(CommandType.New);
						intentBuilder.setEntityId(null); 	// Because this is a new entity
						intentBuilder.setParentEntityId(parentId);
						intentBuilder.setEntityType(entityType);
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
						parentEntityId = extras.getString(getString(R.string.EXTRA_ENTITY_ID));
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

	static public ViewGroup buildCandiInfo(final Entity entity, final ViewGroup candiInfoView, GeoLocation mLocation, boolean refresh) {

		final TextView title = (TextView) candiInfoView.findViewById(R.id.candi_form_title);
		final TextView subtitle = (TextView) candiInfoView.findViewById(R.id.candi_form_subtitle);
		final WebImageView imageCandi = (WebImageView) candiInfoView.findViewById(R.id.candi_form_image);
		final ImageView imageCollection = (ImageView) candiInfoView.findViewById(R.id.candi_form_image_collection);
		final ImageView imageZoom = (ImageView) candiInfoView.findViewById(R.id.candi_form_image_zoom);
		final ViewGroup imageCandiHolder = (ViewGroup) candiInfoView.findViewById(R.id.candi_form_image_holder);
		final TextView description = (TextView) candiInfoView.findViewById(R.id.candi_form_description);
		final AuthorBlock authorBlock = (AuthorBlock) candiInfoView.findViewById(R.id.block_author);
		final Button comments = (Button) candiInfoView.findViewById(R.id.button_comments);
		final ImageView map = (ImageView) candiInfoView.findViewById(R.id.button_map);
		final Button newComment = (Button) candiInfoView.findViewById(R.id.button_comment);
		final ViewGroup newCandi = (ViewGroup) candiInfoView.findViewById(R.id.button_new);
		final Button moveCandi = (Button) candiInfoView.findViewById(R.id.button_move);
		final Button editCandi = (Button) candiInfoView.findViewById(R.id.button_edit);
		final ViewGroup parentGroup = (ViewGroup) candiInfoView.findViewById(R.id.candi_form_parent_group);
		final TextView parentText = (TextView) candiInfoView.findViewById(R.id.candi_form_parent_text);

		final View holderChildren = (View) candiInfoView.findViewById(R.id.holder_button_children);
		final WebImageView imageChildren = (WebImageView) candiInfoView.findViewById(R.id.button_children_image);
		final TextView textChildren = (TextView) candiInfoView.findViewById(R.id.button_children_text);

		/* Candi image */

		if (imageCollection != null) {
			imageCollection.setVisibility(View.INVISIBLE);
		}

		if (entity.imageUri != null
				|| entity.linkPreviewUri != null
				|| entity.linkUri != null
				|| entity.type.equals(CandiConstants.TYPE_CANDI_POST)) {
			String imageUri = entity.getMasterImageUri();
			ImageFormat imageFormat = entity.getMasterImageFormat();
			if (entity.type.equals(CandiConstants.TYPE_CANDI_POST)) {
				imageUri = "resource:ic_post_v2_250";
				imageFormat = ImageFormat.Binary;
			}
			ImageRequestBuilder builder = new ImageRequestBuilder(imageCandi);
			builder.setImageUri(imageUri);
			builder.setImageFormat(imageFormat);
			builder.setLinkZoom(entity.linkZoom);
			builder.setLinkJavascriptEnabled(entity.linkJavascriptEnabled);
			ImageRequest imageRequest = builder.create();
			imageCandi.setImageRequest(imageRequest);
			imageCandiHolder.setVisibility(View.VISIBLE);
		}

		if (entity.type.equals(CandiConstants.TYPE_CANDI_COLLECTION)) {
			if (entity.getMasterImageUri() == null
					|| !entity.getMasterImageUri().toLowerCase().startsWith("resource:")) {
				imageCollection.setVisibility(View.VISIBLE);
			}
			else {
				imageCandi.setClickable(false);
				imageZoom.setVisibility(View.GONE);
			}
		}
		else if (entity.type.equals(CandiConstants.TYPE_CANDI_POST)) {
			imageCandi.setClickable(false);
			imageZoom.setVisibility(View.GONE);
		}

		/* Author block */

		if (entity.creator != null) {
			authorBlock.bindToAuthor(entity.creator, entity.modifiedDate.longValue(), entity.locked);
			authorBlock.setVisibility(View.VISIBLE);
		}
		else {
			authorBlock.setVisibility(View.GONE);
		}

		/* Parent info */
		if (parentGroup != null && entity.getParent() == null && entity.parentId == null) {
			parentGroup.setVisibility(View.GONE);
		}
		else {
			parentGroup.setVisibility(View.VISIBLE);
			parentText.setText(Aircandi.getInstance().getString(R.string.name_entity_type_collection) + ": " + entity.getParent().label);
		}

		/* Adjust buttons */

		newCandi.setVisibility(View.GONE);
		newComment.setVisibility(View.VISIBLE);
		editCandi.setVisibility(View.GONE);
		if (moveCandi != null) {
			moveCandi.setVisibility(View.GONE);
		}
		holderChildren.setVisibility(View.GONE);
		if (!entity.locked) {
			if (entity.getParent() == null) {
				if (entity.type.equals(CandiConstants.TYPE_CANDI_COLLECTION)) {
					newCandi.setVisibility(View.VISIBLE);
				}
			}
			newComment.setVisibility(View.VISIBLE);
		}

		if (entity.creatorId.equals(Aircandi.getInstance().getUser().id)) {
			editCandi.setVisibility(View.VISIBLE);
			if (entity.type.equals(CandiConstants.TYPE_CANDI_COLLECTION)) {
				newCandi.setVisibility(View.VISIBLE);
			}
			else {
				if (moveCandi != null) {
					moveCandi.setVisibility(View.VISIBLE);
				}
			}
		}

		boolean visibleChildren = (entity.getChildren() != null && entity.hasVisibleChildren());
		if (visibleChildren) {
			Entity childEntity = entity.getChildren().get(0);

			/* image */
			ImageRequestBuilder builder = new ImageRequestBuilder(imageChildren);
			builder.setImageUri(childEntity.getMasterImageUri());
			builder.setImageFormat(childEntity.getMasterImageFormat());
			builder.setLinkZoom(childEntity.linkZoom);
			builder.setLinkJavascriptEnabled(childEntity.linkJavascriptEnabled);
			ImageRequest imageRequest = builder.create();
			imageChildren.setImageRequest(imageRequest);

			/* child count */
			textChildren.setText(String.valueOf(entity.getChildren().size()));

			holderChildren.setVisibility(View.VISIBLE);
		}

		/* Comments */

		if (entity.commentCount != null && entity.commentCount > 0) {
			comments.setText(String.valueOf(entity.commentCount) + (entity.commentCount == 1 ? " Comment" : " Comments"));
			comments.setVisibility(View.VISIBLE);
		}
		else {
			comments.setVisibility(View.VISIBLE);
		}

		/* Map */
		GeoLocation entityLocation = entity.location != null ? entity.location : mLocation;
		if (entityLocation == null || entityLocation.latitude == null || entityLocation.longitude == null) {
			map.setVisibility(View.GONE);
		}
		else {
			map.setVisibility(View.VISIBLE);
		}

		/* Candi text */

		title.setText(null);
		subtitle.setText(null);
		description.setText(null);

		if (entity.title != null && !entity.title.equals("")) {
			title.setText(Html.fromHtml(entity.title));
			title.setVisibility(View.VISIBLE);
		}
		else {
			title.setVisibility(View.GONE);
		}
		if (entity.subtitle != null && !entity.subtitle.equals("")) {
			subtitle.setText(Html.fromHtml(entity.subtitle));
			subtitle.setVisibility(View.VISIBLE);
		}
		else {
			subtitle.setVisibility(View.GONE);
		}
		if (entity.description != null && !entity.description.equals("")) {
			description.setText(Html.fromHtml(entity.description));
			description.setVisibility(View.VISIBLE);
		}
		else {
			description.setVisibility(View.GONE);
		}

		return candiInfoView;
	}

	protected void updateViewPager(List<Entity> entitiesForPaging)
	{
		if (mViewPager == null) {

			mViewPager = (ViewPager) findViewById(R.id.view_pager);

			if (entitiesForPaging == null) {
				entitiesForPaging = ProxiExplorer.getInstance().getEntityModel().getCollectionById(mCommon.mCollectionId, mCommon.mEntityTree);
			}

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
				if (entityOld.id.equals(mEntity.id)) {
					mEntitiesForPaging.set(i, mEntity);
					break;
				}
			}

			mViewPager.getAdapter().notifyDataSetChanged();
		}
	}

	private void showCandiPicker() {
		IntentBuilder intentBuilder = new IntentBuilder(this, CandiPicker.class)
				.setEntityTree(mCommon.mEntityTree);
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
				//mCommon.showProgressDialog(true, getString(R.string.progress_moving));
			}

			@Override
			protected Object doInBackground(Object... params) {

				Link link = new Link();
				Bundle parameters = new Bundle();

				if (collectionEntityId != null) {
					/*
					 * Moving from a (beacon or collection) to a collection
					 */
					link.toId = collectionEntityId;
					link.fromId = entityToMove.id;
					parameters.putString("link", "object:" + ProxibaseService.convertObjectToJsonSmart(link, true));
					parameters.putString("originalToId", entityToMove.parentId != null ? entityToMove.parentId : entityToMove.beaconId);
				}
				else {
					/*
					 * Moving from a collection to a beacon
					 */
					link.toId = entityToMove.beaconId;
					link.fromId = entityToMove.id;
					parameters.putString("link", "object:" + ProxibaseService.convertObjectToJsonSmart(link, true));
					parameters.putString("originalToId", entityToMove.parentId);
				}

				ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "updateLink")
						.setRequestType(RequestType.Method)
						.setResponseFormat(ResponseFormat.Json)
						.setParameters(parameters)
						.setSession(Aircandi.getInstance().getUser().session);

				ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode != ResponseCode.Success) {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.CandiMove, CandiFormBase.this);
				}
				else {
					/*
					 * Fixup the entity model.
					 * 
					 * - Could have moved collection to collection
					 * - Could have moved from collection to beacon
					 * - Could have moved from beacon to collection
					 * 
					 * The entity we have been passed could have come from radar or user collections so we
					 * need to look it up for each.
					 */
					Entity entity = ProxiExplorer.getInstance().getEntityModel().getEntityById(entityToMove.id, entityToMove.parentId, EntityTree.Radar);
					if (entity != null) {
						if (collectionEntityId == null) {
							/*
							 * Moving from collection to beacon. We assume beacon has been set to the beacon used by the
							 * original
							 * parent.
							 */
							Entity collectionEntityOriginal = ProxiExplorer.getInstance().getEntityModel()
									.getEntityById(entity.parentId, null, EntityTree.Radar);
							collectionEntityOriginal.getChildren().remove(entity);

							entity.beacon.entities.add(entity);
							entity.parentId = null;
						}
						else {
							/*
							 * Moving to collection. New collection might have a different beaconId.
							 */
							Entity collectionEntity = ProxiExplorer.getInstance().getEntityModel().getEntityById(collectionEntityId, null, EntityTree.Radar);
							if (collectionEntity != null) {
								if (entity.getParent() == null) {
									/*
									 * Moving from beacon to collection
									 */
									entity.beacon.entities.remove(entity);

									collectionEntity.getChildren().add(entity);
									entity.parentId = collectionEntity.id;
									entity.beaconId = collectionEntity.beaconId;
								}
								else {
									/*
									 * Moving from collection to collection
									 */
									Entity collectionEntityOriginal = ProxiExplorer.getInstance().getEntityModel()
											.getEntityById(entity.parentId, null, EntityTree.Radar);
									collectionEntityOriginal.getChildren().remove(entity);

									collectionEntity.getChildren().add(entity);
									entity.parentId = collectionEntity.id;
									entity.beaconId = collectionEntity.beaconId;
								}

								if (collectionEntity.getChildren().size() > 1) {
									Collections.sort(collectionEntity.getChildren(), new EntityList.SortEntitiesByModifiedDate());
								}
							}
						}
					}

					/* The user candi collection might not be populated yet */
					entity = ProxiExplorer.getInstance().getEntityModel().getEntityById(entityToMove.id, entityToMove.parentId, EntityTree.User);
					if (entity != null) {
						if (collectionEntityId == null) {
							/*
							 * Moving from collection to beacon.
							 */
							Entity collectionEntityOriginal = ProxiExplorer.getInstance().getEntityModel()
									.getEntityById(entity.parentId, null, EntityTree.User);
							collectionEntityOriginal.getChildren().remove(entity);

							entity.parentId = null;
						}
						else {
							/*
							 * Moving to collection.
							 */
							Entity collectionEntity = ProxiExplorer.getInstance()
									.getEntityModel()
									.getEntityById(collectionEntityId, null, EntityTree.User);
							
							if (collectionEntity != null) {
								if (entity.getParent() != null) {
									/*
									 * Moving from beacon to collection
									 */
									collectionEntity.getChildren().add(entity);
									entity.parentId = collectionEntity.id;
									entity.beaconId = collectionEntity.beaconId;
								}
								else {
									/*
									 * Moving from collection to collection
									 */
									Entity collectionEntityOriginal = ProxiExplorer.getInstance()
											.getEntityModel()
											.getEntityById(entity.parentId, null, EntityTree.User);
									
									collectionEntityOriginal.getChildren().remove(entity);

									collectionEntity.getChildren().add(entity);
									entity.parentId = collectionEntity.id;
									entity.beaconId = collectionEntity.beaconId;
								}
								if (collectionEntity.getChildren().size() > 1) {
									Collections.sort(collectionEntity.getChildren(), new EntityList.SortEntitiesByModifiedDate());
								}
							}
						}
					}

					ProxiExplorer.getInstance().getEntityModel().rebuildEntityList();
					ProxiExplorer.getInstance().getEntityModel().setLastActivityDate(DateUtils.nowDate().getTime());
					ImageUtils.showToastNotification(getString(R.string.alert_moved), Toast.LENGTH_SHORT);
					bind(true);
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
		if (!isFinishing() && mEntityModelUser != null) {
			if (!Aircandi.getInstance().getUser().id.equals(mEntityModelUser.id)
					|| ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate().longValue() > mEntityModelRefreshDate.longValue()
					|| ProxiExplorer.getInstance().getEntityModel().getLastActivityDate().longValue() > mEntityModelActivityDate.longValue()) {
				invalidateOptionsMenu();
				bind(true);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_POST)) {
			return R.layout.candi_form;
		}
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_PICTURE)) {
			return R.layout.candi_form;
		}
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_LINK)) {
			return R.layout.candi_form;
		}
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_COLLECTION)) {
			return R.layout.candi_form;
		}
		else {
			return 0;
		}
	}
}