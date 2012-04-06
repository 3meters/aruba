package com.proxibase.aircandi;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.Command.CommandVerb;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.GeoLocation;

public class CandiForm extends CandiActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bind();
		Tracker.trackPageView("/CandiForm");
	}

	@Override
	public void bind() {
		super.bind();
		mCommon.mNewCandiIsRoot = false;

		/* We always get the freshest version because the data could be stale */
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, "Loading...");
			}

			@Override
			protected Object doInBackground(Object... params) {
				ServiceResponse serviceResponse = ProxiExplorer.getInstance().getEntityFromService(mCommon.mEntity.id, false);
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;

				if (serviceResponse.responseCode == ResponseCode.Success) {
					mCommon.mEntity = (Entity) serviceResponse.data;
					ViewGroup candiInfoView = (ViewGroup) findViewById(R.id.candi_form);
					buildCandiInfo(mCommon.mEntity, candiInfoView, false);
					((ViewGroup) findViewById(R.id.group_candi_content)).setVisibility(View.VISIBLE);
					mCommon.showProgressDialog(false, null);
					mCommon.stopTitlebarProgress();
				}
				else {
					mCommon.handleServiceError(serviceResponse);
				}
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onCandiInfoClick(View v) {
		startCandiList();
	}

	public void onCommentsClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity.commentsCount > 0) {
			IntentBuilder intentBuilder = new IntentBuilder(this, CommentList.class);
			intentBuilder.setCommand(new Command(CommandVerb.View));
			intentBuilder.setEntity(entity);
			Intent intent = intentBuilder.create();
			startActivityForResult(intent, 0);
		}
	}

	public void onMapClick(View view) {
		String entityId = (String) view.getTag();
		IntentBuilder intentBuilder = new IntentBuilder(this, MapBrowse.class);
		intentBuilder.setCommand(new Command(CommandVerb.View));
		intentBuilder.setEntityId(entityId);
		intentBuilder.setEntityLocation(mCommon.mEntityLocation);
		Intent intent = intentBuilder.create();
		startActivityForResult(intent, 0);
	}

	public void onImageClick(View view) {

		Intent intent = null;
		if (mCommon.mEntity.imageUri != null && !mCommon.mEntity.imageUri.equals("")) {
			IntentBuilder intentBuilder = new IntentBuilder(this, PictureBrowse.class);
			intentBuilder.setCommand(new Command(CommandVerb.View));
			intentBuilder.setEntity(mCommon.mEntity);
			intent = intentBuilder.create();
		}
		else {
			intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setData(Uri.parse(mCommon.mEntity.linkUri));
		}
		startActivity(intent);
		overridePendingTransition(R.anim.form_in, R.anim.browse_out);
	}

	public void onRefreshClick(View view) {
		mCommon.startTitlebarProgress();
		bind();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		mLastResultCode = resultCode;
		if (resultCode == CandiConstants.RESULT_ENTITY_UPDATED
				|| resultCode == CandiConstants.RESULT_ENTITY_INSERTED
				|| resultCode == CandiConstants.RESULT_COMMENT_INSERTED) {
			bind();
			if (resultCode == CandiConstants.RESULT_ENTITY_INSERTED) {
				startCandiList();
			}
		}
		else if (resultCode == CandiConstants.RESULT_ENTITY_DELETED) {
			onBackPressed();
		}
		else if (resultCode == CandiConstants.RESULT_PROFILE_UPDATED
					|| resultCode == CandiConstants.RESULT_USER_SIGNED_IN) {
			mCommon.updateUserPicture();
			bind();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean rebind = mCommon.doOptionsItemSelected(item);
		if (rebind) {
			bind();
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	public ViewGroup buildCandiInfo(final Entity entity, final ViewGroup candiInfoView, boolean refresh) {

		final WebImageView imageAuthor = (WebImageView) candiInfoView.findViewById(R.id.image_author);
		final TextView title = (TextView) candiInfoView.findViewById(R.id.candi_info_title);
		final TextView subtitle = (TextView) candiInfoView.findViewById(R.id.candi_info_subtitle);
		final WebImageView image = (WebImageView) candiInfoView.findViewById(R.id.candi_info_image);
		final ViewGroup imageHolder = (ViewGroup) candiInfoView.findViewById(R.id.candi_info_image_holder);
		final TextView description = (TextView) candiInfoView.findViewById(R.id.candi_info_description);
		final AuthorBlock authorBlock = (AuthorBlock) candiInfoView.findViewById(R.id.block_author);
		final ImageView navigate = (ImageView) candiInfoView.findViewById(R.id.image_forward);
		final Button comments = (Button) candiInfoView.findViewById(R.id.button_comments);
		final ImageView map = (ImageView) candiInfoView.findViewById(R.id.button_map);
		final Button newComment = (Button) candiInfoView.findViewById(R.id.button_comment);
		final Button newCandi = (Button) candiInfoView.findViewById(R.id.button_new);
		final Button editCandi = (Button) candiInfoView.findViewById(R.id.button_edit);
		final ImageView listCandi = (ImageView) candiInfoView.findViewById(R.id.button_list);

		/* Author image */

		if (!refresh) {
			if (entity.creator != null) {
				if (entity.creator.imageUri != null && !entity.creator.imageUri.equals("")) {
					ImageRequestBuilder builder = new ImageRequestBuilder(imageAuthor);
					builder.setFromUris(entity.creator.imageUri, null);
					ImageRequest imageRequest = builder.create();
					imageAuthor.setImageRequest(imageRequest);
				}
			}
		}

		/* Candi image */

		if (entity.imageUri != null || entity.linkUri != null) {
			ImageRequestBuilder builder = new ImageRequestBuilder(image);
			builder.setImageUri(entity.getMasterImageUri());
			builder.setImageFormat(entity.getMasterImageFormat());
			builder.setLinkZoom(entity.linkZoom);
			builder.setLinkJavascriptEnabled(entity.linkJavascriptEnabled);
			ImageRequest imageRequest = builder.create();
			image.setImageRequest(imageRequest);
			imageHolder.setVisibility(View.VISIBLE);
		}
		else {
			imageHolder.setVisibility(View.GONE);
		}

		/* Author block */

		if (entity.creator != null) {
			authorBlock.bindToAuthor(entity.creator, entity.modifiedDate.longValue(), entity.locked);
			authorBlock.setVisibility(View.VISIBLE);
		}
		else {
			authorBlock.setVisibility(View.GONE);
		}

		/* Update any UI indicators related to child candies */

		boolean visibleChildren = (entity.children != null && entity.hasVisibleChildren()) || entity.childrenCount > 0;
		if (visibleChildren) {
			((ViewGroup) candiInfoView.findViewById(R.id.group_candi_content)).setClickable(true);
			navigate.setVisibility(View.VISIBLE);
			navigate.setClickable(true);
		}
		else {
			((ViewGroup) candiInfoView.findViewById(R.id.group_candi_content)).setClickable(false);
			navigate.setVisibility(View.GONE);
			navigate.setClickable(false);
		}

		/* Adjust buttons */

		newCandi.setVisibility(View.GONE);
		newComment.setVisibility(View.GONE);
		editCandi.setVisibility(View.GONE);
		listCandi.setVisibility(View.GONE);
		if (!entity.locked) {
			if (entity.root) {
				newCandi.setVisibility(View.VISIBLE);
				newCandi.setTag(new Command(CommandVerb.Dialog, "Add\nCandi", "NewCandi", entity.type, entity.id, entity.id, null));
			}
			newComment.setVisibility(View.VISIBLE);
			newComment.setTag(new Command(CommandVerb.New, "Comment", "CommentForm", null, entity.id, entity.id, null));
		}
		if (entity.creatorId.equals(Aircandi.getInstance().getUser().id)) {
			editCandi.setVisibility(View.VISIBLE);
			editCandi.setTag(new Command(CommandVerb.Edit, "Edit", "EntityForm", entity.type, entity.id, null, null));
		}
		if (visibleChildren) {
			listCandi.setVisibility(View.VISIBLE);
		}

		/* Comments */

		if (entity.commentsCount > 0) {
			comments.setText(String.valueOf(entity.commentsCount) + (entity.commentsCount == 1 ? " Comment" : " Comments"));
			comments.setTag(entity);
			comments.setVisibility(View.VISIBLE);
		}
		else {
			comments.setVisibility(View.GONE);
		}

		/* Map */
		GeoLocation entityLocation = entity.location != null ? entity.location : mCommon.mEntityLocation;
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

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == CandiConstants.DIALOG_NEW_CANDI_ID) {
			return mCommon.mIconContextMenu.createMenu(getString(R.string.dialog_new_message), this);
		}
		return super.onCreateDialog(id);
	}

	private void startCandiList() {
		IntentBuilder intentBuilder = new IntentBuilder(this, CandiList.class);
		intentBuilder.setCommand(new Command(CommandVerb.View));
		intentBuilder.setEntity(mCommon.mEntity);
		Intent intent = intentBuilder.create();

		startActivity(intent);
		overridePendingTransition(R.anim.slide_in_right_long, R.anim.slide_out_left_long);
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