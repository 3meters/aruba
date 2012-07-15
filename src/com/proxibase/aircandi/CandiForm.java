package com.proxibase.aircandi;

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

import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.components.CandiPagerAdapter;
import com.proxibase.aircandi.components.CommandType;
import com.proxibase.aircandi.components.EntityList;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.GeoLocation;
import com.proxibase.service.objects.ServiceData;
import com.proxibase.service.objects.User;

public class CandiForm extends CandiActivity {

	private List<Entity>	mEntitiesForPaging	= new ArrayList<Entity>();
	private ViewPager		mViewPager;
	private Entity			mEntity;
	private Number			mEntityModelRefreshDate;
	private Number			mEntityModelActivityDate;
	private User			mEntityModelUser;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			initialize();
			bind(true);
		}
	}

	public void initialize() {}

	public void bind(Boolean useProxiExplorer) {
		/*
		 * Navigation setup for action bar icon and title
		 */
		if (mCommon.mCollectionId.equals(ProxiConstants.ROOT_COLLECTION_ID)) {
			if (mCommon.mCollectionType == ProxiExplorer.CollectionType.CandiByRadar) {
				mCommon.setActionBarTitleAndIcon(null, R.string.navigation_radar, true);
			}
			else if (mCommon.mCollectionType == ProxiExplorer.CollectionType.CandiByUser) {
				mCommon.setActionBarTitleAndIcon(null, R.string.navigation_mycandi, true);
			}
		}
		else {
			Entity collectionEntity = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mCollectionId, mCommon.mCollectionType);
			mCommon.setActionBarTitleAndIcon(collectionEntity, true);
		}

		if (useProxiExplorer) {
			/*
			 * Entity is coming from entity model.
			 */
			mEntity = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mEntityId, mCommon.mCollectionType);
			mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
			mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
			mEntityModelUser = Aircandi.getInstance().getUser();

			/* Was likely deleted from the entity model */
			if (mEntity == null) {
				onBackPressed();
			}
			else {
				/* Get the view pager configured */
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
						mCommon.stopTitlebarProgress();
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
		mCommon.startTitlebarProgress();
		bind(false);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onChildrenButtonClick(View v) {
		IntentBuilder intentBuilder = new IntentBuilder(this, CandiList.class);

		intentBuilder.setCommandType(CommandType.View);
		intentBuilder.setCollectionId(mCommon.mEntityId);
		intentBuilder.setCollectionType(mCommon.mCollectionType);

		Intent intent = intentBuilder.create();
		startActivity(intent);
		overridePendingTransition(R.anim.slide_in_right_long, R.anim.slide_out_left_long);
	}

	public void onBrowseCommentsButtonClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity.commentCount > 0) {
			IntentBuilder intentBuilder = new IntentBuilder(this, CommentList.class);
			intentBuilder.setCommandType(CommandType.View);
			intentBuilder.setEntityId(entity.id);
			intentBuilder.setCollectionId(entity.id);
			intentBuilder.setCollectionType(mCommon.mCollectionType);
			Intent intent = intentBuilder.create();
			startActivity(intent);
		}
	}

	public void onMoveButtonClick(View view) {
		showCandiPicker();
	}

	public void onMapButtonClick(View view) {
		String entityId = (String) view.getTag();
		IntentBuilder intentBuilder = new IntentBuilder(this, MapBrowse.class);
		intentBuilder.setCommandType(CommandType.View);
		intentBuilder.setEntityId(entityId);
		intentBuilder.setEntityLocation(mCommon.mEntityLocation);
		Intent intent = intentBuilder.create();
		startActivity(intent);
	}

	public void onImageClick(View view) {

		Intent intent = null;
		if (mEntity.imageUri != null && !mEntity.imageUri.equals("")) {
			IntentBuilder intentBuilder = new IntentBuilder(this, PictureBrowse.class);
			intentBuilder.setCommandType(CommandType.View);
			intentBuilder.setEntityId(mEntity.id);
			intentBuilder.setCollectionType(mCommon.mCollectionType);
			intent = intentBuilder.create();
		}
		else {
			intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setData(Uri.parse(mEntity.linkUri));
		}
		startActivity(intent);
		overridePendingTransition(R.anim.form_in, R.anim.browse_out);
	}

	public void onAddCandiButtonClick(View view) {
		mCommon.showTemplatePicker();
	}

	public void onEditCandiButtonClick(View view) {
		IntentBuilder intentBuilder = new IntentBuilder(this, EntityForm.class);
		intentBuilder.setCommandType(CommandType.Edit);
		intentBuilder.setEntityId(mEntity.id);
		intentBuilder.setEntityType(mEntity.type);
		intentBuilder.setCollectionType(mCommon.mCollectionType);
		Intent intent = intentBuilder.create();
		startActivity(intent);
		overridePendingTransition(R.anim.form_in, R.anim.browse_out);
	}

	public void onNewCommentButtonClick(View view) {
		IntentBuilder intentBuilder = new IntentBuilder(this, CommentForm.class);
		intentBuilder.setParentEntityId(mEntity.id);
		Intent intent = intentBuilder.create();
		startActivity(intent);
		overridePendingTransition(R.anim.form_in, R.anim.browse_out);
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
						intentBuilder.setParentEntityId(parentId);
						intentBuilder.setEntityType(entityType);
						Intent redirectIntent = intentBuilder.create();
						startActivity(redirectIntent);
						overridePendingTransition(R.anim.form_in, R.anim.browse_out);
					}
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_CANDI_PICK) {
				if (intent != null && intent.getExtras() != null) {
					Bundle extras = intent.getExtras();
					final String parentEntityId = extras.getString(getString(R.string.EXTRA_ENTITY_ID));
					if (parentEntityId != null && !parentEntityId.equals("")) {
						/*
						 * Update entity link and refresh parent entity info
						 */
					}
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	static public ViewGroup buildCandiInfo(final Entity entity, final ViewGroup candiInfoView, GeoLocation mLocation, boolean refresh) {

		final TextView title = (TextView) candiInfoView.findViewById(R.id.candi_info_title);
		final TextView subtitle = (TextView) candiInfoView.findViewById(R.id.candi_info_subtitle);
		final WebImageView imageCandi = (WebImageView) candiInfoView.findViewById(R.id.candi_info_image);
		final ViewGroup imageCandiHolder = (ViewGroup) candiInfoView.findViewById(R.id.candi_info_image_holder);
		final TextView description = (TextView) candiInfoView.findViewById(R.id.candi_info_description);
		final AuthorBlock authorBlock = (AuthorBlock) candiInfoView.findViewById(R.id.block_author);
		final Button comments = (Button) candiInfoView.findViewById(R.id.button_comments);
		final ImageView map = (ImageView) candiInfoView.findViewById(R.id.button_map);
		final Button newComment = (Button) candiInfoView.findViewById(R.id.button_comment);
		final Button newCandi = (Button) candiInfoView.findViewById(R.id.button_new);
		final Button editCandi = (Button) candiInfoView.findViewById(R.id.button_edit);

		final View holderChildren = (View) candiInfoView.findViewById(R.id.holder_button_children);
		final WebImageView imageChildren = (WebImageView) candiInfoView.findViewById(R.id.button_children_image);
		final TextView textChildren = (TextView) candiInfoView.findViewById(R.id.button_children_text);

		/* Candi image */

		if (entity.imageUri != null || entity.linkUri != null) {
			ImageRequestBuilder builder = new ImageRequestBuilder(imageCandi);
			builder.setImageUri(entity.getMasterImageUri());
			builder.setImageFormat(entity.getMasterImageFormat());
			builder.setLinkZoom(entity.linkZoom);
			builder.setLinkJavascriptEnabled(entity.linkJavascriptEnabled);
			ImageRequest imageRequest = builder.create();
			imageCandi.setImageRequest(imageRequest);
			imageCandiHolder.setVisibility(View.VISIBLE);
		}
		else {
			imageCandiHolder.setVisibility(View.GONE);
		}

		/* Author block */

		if (entity.creator != null) {
			authorBlock.bindToAuthor(entity.creator, entity.modifiedDate.longValue(), entity.locked);
			authorBlock.setVisibility(View.VISIBLE);
		}
		else {
			authorBlock.setVisibility(View.GONE);
		}

		/* Adjust buttons */

		newCandi.setVisibility(View.GONE);
		newComment.setVisibility(View.GONE);
		editCandi.setVisibility(View.GONE);
		holderChildren.setVisibility(View.GONE);
		if (!entity.locked) {
			if (entity.root) {
				newCandi.setVisibility(View.VISIBLE);
			}
			newComment.setVisibility(View.VISIBLE);
		}

		if (entity.creatorId.equals(Aircandi.getInstance().getUser().id)) {
			editCandi.setVisibility(View.VISIBLE);
		}

		boolean visibleChildren = (entity.children != null && (entity.hasVisibleChildren()) || (entity.childCount != null && entity.childCount > 0));
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
			textChildren.setText(String.valueOf(entity.childCount));

			holderChildren.setVisibility(View.VISIBLE);
		}

		/* Comments */

		if (entity.commentCount != null && entity.commentCount > 0) {
			comments.setText(String.valueOf(entity.commentCount) + (entity.commentCount == 1 ? " Comment" : " Comments"));
			comments.setTag(entity);
			comments.setVisibility(View.VISIBLE);
		}
		else {
			comments.setVisibility(View.GONE);
		}

		/* Map */
		GeoLocation entityLocation = entity.location != null ? entity.location : mLocation;
		if (entityLocation == null || entityLocation.latitude == null || entityLocation.longitude == null) {
			map.setVisibility(View.GONE);
		}
		else {
			map.setVisibility(View.VISIBLE);
			map.setTag(entity.id);
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

	private void updateViewPager()
	{
		if (mViewPager == null) {

			mViewPager = (ViewPager) CandiForm.this.findViewById(R.id.view_pager);

			List<Entity> entitiesForPaging = ProxiExplorer.getInstance().getEntityModel().getCollectionById(mCommon.mCollectionId, mCommon.mCollectionType);

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

			mViewPager.setAdapter(new CandiPagerAdapter(CandiForm.this, mViewPager, mEntitiesForPaging));

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
		overridePendingTransition(R.anim.form_in, R.anim.browse_out);
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
		else {
			return 0;
		}
	}
}