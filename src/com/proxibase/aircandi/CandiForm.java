package com.proxibase.aircandi;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.AircandiCommon.ActionButtonSet;
import com.proxibase.aircandi.components.AircandiCommon.IntentBuilder;
import com.proxibase.aircandi.components.Command.CommandVerb;
import com.proxibase.aircandi.components.ImageManager.ImageRequest;
import com.proxibase.aircandi.components.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.Entity;

public class CandiForm extends CandiActivity {

	private SlidingDrawer	mSlidingDrawer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bind(false);
		Tracker.trackPageView("/CandiForm");
	}

	@Override
	protected void bind(boolean refresh) {
		super.bind(refresh);

		mSlidingDrawer = (SlidingDrawer) findViewById(R.id.slide_actions_info);

		mCommon.showProgressDialog(true, "Loading...");
		ViewGroup candiInfoView = (ViewGroup) findViewById(R.id.candi_form);
		buildCandiInfo(mCommon.mEntity, candiInfoView, refresh);
		mCommon.showProgressDialog(false, null);

		if (Aircandi.getInstance().getToolstripOpen()) {
			if (mSlidingDrawer != null) {
				mSlidingDrawer.open();
			}
		}
		else if (Aircandi.getInstance().getFirstTimeCandiForm()) {
			Aircandi.getInstance().setFirstTimeCandiForm(false);
			Aircandi.applicationHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					if (mSlidingDrawer != null) {
						mSlidingDrawer.animateOpen();
					}
				}
			}, 500);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onCandiInfoClick(View v) {
		IntentBuilder intentBuilder = new IntentBuilder(this, CandiList.class);
		intentBuilder.setCommand(new Command(CommandVerb.View));
		intentBuilder.setEntity(mCommon.mEntity);
		Intent intent = intentBuilder.create();

		startActivity(intent);
	}

	public void onCommentsClick(View view) {
		Entity entity = (Entity) view.getTag();
		if (entity.commentCount > 0) {
			IntentBuilder intentBuilder = new IntentBuilder(this, CommentList.class);
			intentBuilder.setCommand(new Command(CommandVerb.View));
			intentBuilder.setEntity(entity);
			Intent intent = intentBuilder.create();
			startActivityForResult(intent, 0);
		}
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

	}

	public void onActionsClick(View view) {
		if (!mCommon.mEntity.locked) {
			mCommon.doActionsClick(view, true, ActionButtonSet.CandiForm);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		mLastResultCode = resultCode;
		if (resultCode == CandiConstants.RESULT_ENTITY_UPDATED
				|| resultCode == CandiConstants.RESULT_ENTITY_INSERTED
				|| resultCode == CandiConstants.RESULT_COMMENT_INSERTED) {
			refreshEntity();
		}
		else if (resultCode == CandiConstants.RESULT_ENTITY_DELETED) {
			setResult(CandiConstants.RESULT_ENTITY_DELETED);
			finish();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	protected void refreshEntity() {

		new AsyncTask() {

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
					bind(true);
				}
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	public ViewGroup buildCandiInfo(final Entity entity, final ViewGroup candiInfoView, boolean refresh) {

		/* Build menus */
		TableLayout table = null;
		List<Command> commands = new ArrayList<Command>();
		if (!entity.locked) {
			commands.add(new Command(CommandVerb.New, "Comment", "CommentForm", null, entity.id, entity.id, null));
		}
		if (entity.createdById.equals(Integer.parseInt(Aircandi.getInstance().getUser().id))) {
			commands.add(new Command(CommandVerb.Edit, "Edit", "EntityForm", entity.entityType, entity.id, null, null));
		}
		if (commands.size() > 0) {
			table = configureMenus(commands);
		}

		ViewGroup frameButtons = (ViewGroup) candiInfoView.findViewById(R.id.frame_buttons);
		if (table != null) {
			frameButtons.removeAllViews();
			frameButtons.addView(table);
		}
		else {
			frameButtons.setVisibility(View.GONE);
		}

		/*
		 * if (table != null) {
		 * RelativeLayout slideContent = (RelativeLayout) candiInfoView.findViewById(R.id.candi_info_slider_content);
		 * slideContent.removeAllViews();
		 * slideContent.addView(table);
		 * ((View) candiInfoView.findViewById(R.id.slide_actions_info)).setVisibility(View.VISIBLE);
		 * }
		 * else {
		 * ((View) candiInfoView.findViewById(R.id.slide_actions_info)).setVisibility(View.GONE);
		 * }
		 */

		final WebImageView imageAuthor = (WebImageView) candiInfoView.findViewById(R.id.image_author);
		final ImageView imageAuthorReflection = (ImageView) candiInfoView.findViewById(R.id.image_author_reflection);
		final TextView title = (TextView) candiInfoView.findViewById(R.id.candi_info_title);
		final TextView subtitle = (TextView) candiInfoView.findViewById(R.id.candi_info_subtitle);
		final WebImageView image = (WebImageView) candiInfoView.findViewById(R.id.candi_info_image);
		final ViewGroup imageHolder = (ViewGroup) candiInfoView.findViewById(R.id.candi_info_image_holder);
		final TextView description = (TextView) candiInfoView.findViewById(R.id.candi_info_description);
		final AuthorBlock authorBlock = (AuthorBlock) candiInfoView.findViewById(R.id.block_author);
		final Button comments = (Button) candiInfoView.findViewById(R.id.button_comments);
		final ImageView navigate = (ImageView) candiInfoView.findViewById(R.id.image_forward);

		/* Author image */
		if (!refresh) {
			if (entity.author != null) {
				if (entity.author.imageUri != null && !entity.author.imageUri.equals("")) {
					ImageRequest imageRequest = new ImageRequest(entity.author.imageUri, null, ImageShape.Square, false, false,
							CandiConstants.IMAGE_WIDTH_SEARCH_MAX, true, true, true, 1, this, null);
					imageAuthor.setImageRequest(imageRequest, imageAuthorReflection);
				}
			}
		}

		/* Candi image */
		if (entity.imageUri != null || entity.linkUri != null) {
			ImageRequest imageRequest = new ImageRequest(entity, ImageShape.Square,
					CandiConstants.IMAGE_WIDTH_SEARCH_MAX, true, true, true, 1, this, null);
			image.setImageRequest(imageRequest, null);
		}
		else {
			imageHolder.setVisibility(View.GONE);
		}

		/* Author block */
		if (!refresh) {
			if (entity.author != null) {
				authorBlock.bindToAuthor(entity.author, DateUtils.wcfToDate(entity.createdDate));
			}
			else {
				authorBlock.setVisibility(View.GONE);
			}
		}

		/* Update any UI indicators related to child candies */
		if ((entity.children != null && entity.hasVisibleChildren()) || entity.childCount > 0) {
			((ViewGroup) candiInfoView.findViewById(R.id.group_candi_content)).setClickable(true);
			navigate.setVisibility(View.VISIBLE);
			navigate.setClickable(true);
		}
		else {
			((ViewGroup) candiInfoView.findViewById(R.id.group_candi_content)).setClickable(false);
			navigate.setVisibility(View.GONE);
			navigate.setClickable(false);
		}

		/* Comments */
		if (entity.commentCount > 0) {
			comments.setText(String.valueOf(entity.commentCount) + (entity.commentCount == 1 ? " Comment" : " Comments"));
			comments.setTag(entity);
		}
		else {
			comments.setVisibility(View.GONE);
		}

		/* Candi text */
		title.setText(null);
		subtitle.setText(null);
		description.setText(null);

		if (entity.title != null && !entity.title.equals("")) {
			title.setText(Html.fromHtml(entity.title));
		}
		else {
			title.setVisibility(View.GONE);
		}
		if (entity.subtitle != null && !entity.subtitle.equals("")) {
			subtitle.setText(Html.fromHtml(entity.subtitle));
		}
		else {
			subtitle.setVisibility(View.GONE);
		}
		if (entity.description != null && !entity.description.equals("")) {
			description.setText(Html.fromHtml(entity.description));
		}
		else {
			description.setVisibility(View.GONE);
		}

		return candiInfoView;
	}

	public TableLayout configureMenus(List<Command> commands) {

		Boolean needMoreButton = false;

		if (commands == null || commands.size() == 0) {
			return null;
		}
		if (commands.size() > 6) {
			needMoreButton = true;
		}

		/* Get the table we use for grouping and clear it */
		final TableLayout table = new TableLayout(this);

		/* Make the first row */
		TableRow tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_commands, null);
		final TableRow.LayoutParams rowLp = new TableRow.LayoutParams();
		rowLp.setMargins(0, 0, 0, 0);
		TableLayout.LayoutParams tableLp;

		/* Loop the streams */
		Integer commandCount = 0;
		RelativeLayout commandButtonContainer;
		for (Command command : commands) {
			/*
			 * TODO: This is a temporary hack. The service shouldn't pass commands
			 * that this user doesn't have sufficient permissions for.
			 */
			/* Make a button and configure it */
			commandButtonContainer = (RelativeLayout) getLayoutInflater().inflate(R.layout.temp_button_command, null);

			final TextView commandButton = (TextView) commandButtonContainer.findViewById(R.id.CommandButton);
			commandButtonContainer.setTag(command);
			if (needMoreButton && commandCount == 5) {
				commandButton.setText("More...");
				commandButton.setTag(command);
			}
			else {
				commandButton.setText(command.label);
				commandButton.setTag(command);
			}

			/* Add button to row */
			tableRow.addView(commandButtonContainer, rowLp);
			commandCount++;

			/* If we have three in a row then commit it and make a new row */
			int newRow = 3;

			if (commandCount % newRow == 0) {
				tableLp = new TableLayout.LayoutParams();
				tableLp.setMargins(0, 0, 0, 0);
				table.addView(tableRow, tableLp);
				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_commands, null);
			}
			else if (commandCount == 6) {
				break;
			}
		}

		/* We might have an uncommited row with stream buttons in it */
		if (commandCount != 3) {
			tableLp = new TableLayout.LayoutParams();
			tableLp.setMargins(0, 3, 0, 3);
			table.addView(tableRow, tableLp);
		}
		return table;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onPause() {
		super.onPause();
		//Aircandi.getInstance().setToolstripOpen(mSlidingDrawer.isOpened());
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