package com.proxibase.aircandi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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

import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.AircandiCommon.ServiceOperation;
import com.proxibase.aircandi.components.AnimUtils.TransitionType;
import com.proxibase.aircandi.components.AnimUtils;
import com.proxibase.aircandi.components.CandiPagerAdapter;
import com.proxibase.aircandi.components.CommandType;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.EntityList;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.ProxiExplorer.EntityTree;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ProxibaseServiceException.ErrorCode;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.Entity.ImageFormat;
import com.proxibase.service.objects.GeoLocation;
import com.proxibase.service.objects.Link;
import com.proxibase.service.objects.ServiceData;
import com.proxibase.service.objects.User;

public class CandiForm extends CandiActivity {

	protected List<Entity>	mEntitiesForPaging	= new ArrayList<Entity>();
	protected ViewPager		mViewPager;
	protected Entity		mEntity;
	protected Number		mEntityModelRefreshDate;
	protected Number		mEntityModelActivityDate;
	protected User			mEntityModelUser;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			bind(true);
		}
	}

	public void bind(Boolean useProxiExplorer) {
		/*
		 * Navigation setup for action bar icon and title
		 */
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);

		if (useProxiExplorer) {
			/*
			 * Entity is coming from entity model.
			 */
			mEntity = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mEntityId, mCommon.mParentId, EntityTree.Radar);
			mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
			mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
			mEntityModelUser = Aircandi.getInstance().getUser();
			
			/* Was likely deleted from the entity model */
			if (mEntity == null) {
				onBackPressed();
			}
			else {
				mCommon.mActionBar.setTitle(mEntity.title);
				updateViewPager();
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

						/* Sort the children if there are any */
						if (mEntity.children != null && mEntity.children.size() > 1) {
							Collections.sort(mEntity.children, new EntityList.SortEntitiesByModifiedDate());
						}

						/* Get the view pager configured */
						updateViewPager();

						mCommon.showProgressDialog(false, null);
					}
					else {
						mCommon.handleServiceError(serviceResponse);
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
	
	public void onChildrenButtonClick(View v) {
		IntentBuilder intentBuilder = new IntentBuilder(this, CandiList.class);

		/*
		 * mCommon.mEntityId is the original entity the user navigated to but
		 * they could have swiped using the viewpager to a different entity so
		 * we need to use mEntity to get the right entity context.
		 */
		intentBuilder.setCommandType(CommandType.View);
		intentBuilder.setEntityId(mEntity.id);
		intentBuilder.setParentEntityId(mEntity.parentId);
		intentBuilder.setCollectionId(mEntity.id);
		intentBuilder.setEntityTree(mCommon.mEntityTree);

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
		IntentBuilder intentBuilder = new IntentBuilder(this, EntityForm.class);
		intentBuilder.setCommandType(CommandType.Edit);
		intentBuilder.setEntityId(mEntity.id);
		intentBuilder.setParentEntityId(mEntity.parentId);
		intentBuilder.setEntityType(mEntity.type);
		intentBuilder.setEntityTree(mCommon.mEntityTree);
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
					String parentEntityIdNew = null;
					Bundle extras = intent.getExtras();
					if (extras != null) {
						parentEntityIdNew = extras.getString(getString(R.string.EXTRA_ENTITY_ID));
					}
					/*
					 * If parent entity is null then the candi is being moved to the top on its own.
					 * 
					 * Special case: user could have a top level candi and choose to move it to
					 * top so it's a no-op.
					 */
					if (parentEntityIdNew == null && mEntity.parent == null) {
						return;
					}
					moveCandi(mEntity, parentEntityIdNew);
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
		final Button newCandi = (Button) candiInfoView.findViewById(R.id.button_new);
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
		if (parentGroup != null && entity.parent == null && entity.parentId == null) {
			parentGroup.setVisibility(View.GONE);
		}
		else {
			parentGroup.setVisibility(View.VISIBLE);
			parentText.setText(Aircandi.getInstance().getString(R.string.name_entity_type_collection) + ": " + entity.parent.label);
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
			if (entity.parent == null) {
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

		boolean visibleChildren = (entity.children != null && entity.hasVisibleChildren());
		if (visibleChildren) {
			Entity childEntity = entity.children.get(0);

			/* image */
			ImageRequestBuilder builder = new ImageRequestBuilder(imageChildren);
			builder.setImageUri(childEntity.getMasterImageUri());
			builder.setImageFormat(childEntity.getMasterImageFormat());
			builder.setLinkZoom(childEntity.linkZoom);
			builder.setLinkJavascriptEnabled(childEntity.linkJavascriptEnabled);
			ImageRequest imageRequest = builder.create();
			imageChildren.setImageRequest(imageRequest);

			/* child count */
			textChildren.setText(String.valueOf(entity.children.size()));

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

	protected void updateViewPager()
	{
		if (mViewPager == null) {

			mViewPager = (ViewPager) findViewById(R.id.view_pager);

			List<Entity> entitiesForPaging = ProxiExplorer.getInstance().getEntityModel().getCollectionById(mCommon.mCollectionId, mCommon.mEntityTree);

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
		Intent intent = new Intent(this, CandiPicker.class);
		startActivityForResult(intent, CandiConstants.ACTIVITY_CANDI_PICK);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	private void moveCandi(final Entity entityToMove, final String parentEntityIdNew) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				//mCommon.showProgressDialog(true, getString(R.string.progress_moving));
			}

			@Override
			protected Object doInBackground(Object... params) {
				ServiceResponse serviceResponse = new ServiceResponse();
				Bundle parameters = new Bundle();

				/* We could be relinking to either another entity or a beacon */
				Link link = new Link();
				if (parentEntityIdNew == null) {
					link.toId = entityToMove.beacon.id;
					link.fromId = entityToMove.id;
				}
				else {
					link.toId = parentEntityIdNew;
					link.fromId = entityToMove.id;
				}
				parameters.putString("link",
						"object:" + ProxibaseService.convertObjectToJson(link, GsonType.ProxibaseService));

				/* Entity */
				parameters.putString("originalToId", entityToMove.parent != null ? entityToMove.parent.id : entityToMove.beacon.id);

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "updateLink")
						.setRequestType(RequestType.Method)
						.setParameters(parameters)
						.setSession(Aircandi.getInstance().getUser().session)
						.setResponseFormat(ResponseFormat.Json);

				serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				ServiceResponse serviceResponse = (ServiceResponse) response;
				//mCommon.showProgressDialog(false, null);
				if (serviceResponse.responseCode != ResponseCode.Success) {
					if (serviceResponse.exception.getErrorCode() == ErrorCode.SessionException) {
						AircandiCommon.showAlertDialog(R.drawable.icon_app
								, getResources().getString(R.string.alert_session_expired_title)
								, getResources().getString(R.string.alert_session_expired_message)
								, CandiForm.this, android.R.string.ok, null, new OnClickListener() {

									public void onClick(DialogInterface dialog, int which) {}
								}, null);
					}
					else {
						mCommon.handleServiceError(serviceResponse, ServiceOperation.CandiMove, CandiForm.this);
					}
				}
				else {
					/*
					 * Fixup the entity model.
					 * 
					 * - Could have moved to a different parent
					 * - Could have moved from parent to top
					 * - Could have moved from top to a parent
					 * 
					 * The entity we have been passed could have come from radar or user collections so we
					 * need to look it up for each.
					 */
					Entity entity = ProxiExplorer.getInstance().getEntityModel()
							.getEntityById(entityToMove.id, entityToMove.parentId, EntityTree.Radar);
					if (parentEntityIdNew == null) {
						/*
						 * Moving to top. We assume beacon has been set to the beacon used by the original parent.
						 */
						Entity parentEntityOriginal = ProxiExplorer.getInstance().getEntityModel()
								.getEntityById(entity.parentId, null, EntityTree.Radar);
						parentEntityOriginal.children.remove(entity);
						entity.beacon.entities.add(entity);
						entity.parent = null;
						entity.parentId = null;
					}
					else {
						/*
						 * Moving to parent
						 */
						Entity parentEntityNew = ProxiExplorer.getInstance().getEntityModel()
								.getEntityById(parentEntityIdNew, null, EntityTree.Radar);
						if (entity.parent != null) {
							Entity parentEntityOriginal = ProxiExplorer.getInstance().getEntityModel()
									.getEntityById(entity.parentId, null, EntityTree.Radar);
							parentEntityOriginal.children.remove(entity);
						}
						if (parentEntityNew.children == null) {
							parentEntityNew.children = new EntityList<Entity>();
						}
						parentEntityNew.children.add(entity);
						entity.parent = parentEntityNew;
						entity.parentId = parentEntityNew.id;
						if (parentEntityNew.children.size() > 1) {
							Collections.sort(parentEntityNew.children, new EntityList.SortEntitiesByModifiedDate());
						}
					}

					/* The user candi collection might not be populated yet */
					entity = ProxiExplorer.getInstance().getEntityModel().getEntityById(entityToMove.id, entityToMove.parentId, EntityTree.User);
					if (entity != null) {
						if (parentEntityIdNew == null) {
							/*
							 * Moving to top. User candi are not associated with beacons.
							 */
							Entity parentEntityOriginal = ProxiExplorer.getInstance().getEntityModel()
									.getEntityById(entity.parentId, null, EntityTree.User);
							parentEntityOriginal.children.remove(entity);
							entity.parent = null;
							entity.parentId = null;
						}
						else {
							/*
							 * Moving to parent.
							 */
							if (entity.parent != null) {
								Entity parentEntityOriginal = ProxiExplorer.getInstance().getEntityModel()
										.getEntityById(entity.parentId, null, EntityTree.User);
								parentEntityOriginal.children.remove(entity);
							}
							Entity parentEntityNew = ProxiExplorer.getInstance().getEntityModel()
									.getEntityById(parentEntityIdNew, null, EntityTree.User);
							/* For user candi, we might be moving to a parent that isn't in the user candi. */
							if (parentEntityNew != null) {
								if (parentEntityNew.children == null) {
									parentEntityNew.children = new EntityList<Entity>();
								}
								parentEntityNew.children.add(entity);
								entity.parent = parentEntityNew;
								entity.parentId = parentEntityNew.id;
								if (parentEntityNew.children.size() > 1) {
									Collections.sort(parentEntityNew.children, new EntityList.SortEntitiesByModifiedDate());
								}
							}
						}
					}

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