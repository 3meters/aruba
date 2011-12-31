package com.proxibase.aircandi;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.CandiSearchActivity.CandiTask;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.ImageManager.ImageRequest;
import com.proxibase.aircandi.components.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;

public class CandiForm extends AircandiActivity {

	private Runnable	mUserSignedInRunnable;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bind();
		GoogleAnalyticsTracker.getInstance().trackPageView("/CandiForm");
	}

	protected void bind() {
		showProgressDialog(true, "Loading...");
		ViewGroup candiInfoView = (ViewGroup) findViewById(R.id.candi_form);
		buildCandiInfo(mEntityProxy, candiInfoView);
		showProgressDialog(false, null);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onCommandButtonClick(View view) {

		try {
			final Command command = (Command) view.getTag();
			final String commandHandler = "com.proxibase.aircandi." + command.handler;
			GoogleAnalyticsTracker.getInstance().trackEvent("Clicks", "Command", command.label, 0);
			String message = getString(R.string.signin_message_new_candi) + " " + command.label;
			mUserSignedInRunnable = new Runnable() {

				@Override
				public void run() {
					try {
						Class clazz = Class.forName(commandHandler, false, this.getClass().getClassLoader());
						Logger.i(mContext, "Starting activity: " + clazz.toString());
						EntityProxy entityProxy = command.entity;

						if (command.verb.equals("new")) {
							Intent intent = Aircandi.buildIntent(mContext, command.entity, command.entity.id, command.includeChildren, null,
										command, CandiTask.None, command.entity.beacon, mUser, clazz);
							startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
						}
						else {
							Intent intent = Aircandi.buildIntent(mContext, entityProxy, 0, command.includeChildren, null, command,
										CandiTask.None, null, mUser, clazz);
							startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
						}
					}
					catch (ClassNotFoundException exception) {
						exception.printStackTrace();
					}
					finally {
						mUserSignedInRunnable = null;
					}
				}
			};

			if (!command.verb.equals("view")) {
				if (mUser != null && mUser.anonymous) {
					Intent intent = Aircandi.buildIntent(mContext, null, 0, false, null, new Command("edit"), CandiTask.None, null, null,
								SignInForm.class);
					intent.putExtra(getString(R.string.EXTRA_MESSAGE), message);
					startActivityForResult(intent, CandiConstants.ACTIVITY_SIGNIN);
				}
				else {
					mHandler.post(mUserSignedInRunnable);
				}
			}
			else {
				mHandler.post(mUserSignedInRunnable);
			}
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (IllegalStateException e) {
			e.printStackTrace();
		}
		catch (SecurityException exception) {
			exception.printStackTrace();
		}
	}

	public void onCandiInfoClick(View v) {
		Intent intent = Aircandi.buildIntent(mContext, mEntityProxy, 0, false, null, new Command("view"), mCandiTask, null, mUser,
				CandiList.class);
		startActivity(intent);
	}

	public void onCommentsClick(View view) {
		EntityProxy entity = (EntityProxy) view.getTag();
		if (entity.commentCount > 0) {
			Intent intent = Aircandi.buildIntent(this, entity, 0, false, null, new Command("view"), mCandiTask, null, mUser, CommentList.class);
			startActivity(intent);
		}
	}

	public void onBackPressed() {
		super.onBackPressed();
		//overridePendingTransition(R.anim.fade_in_short, R.anim.fade_out_short);
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	public ViewGroup buildCandiInfo(final EntityProxy entity, final ViewGroup candiInfoView) {

		/* Build menus */
		TableLayout table = configureMenus(entity);
		if (table != null) {
			RelativeLayout slideContent = (RelativeLayout) candiInfoView.findViewById(R.id.candi_info_slider_content);
			slideContent.removeAllViews();
			slideContent.addView(table);
			((View) candiInfoView.findViewById(R.id.slide_actions_info)).setVisibility(View.VISIBLE);
		}
		else {
			((View) candiInfoView.findViewById(R.id.slide_actions_info)).setVisibility(View.GONE);
		}

		/* Update any UI indicators related to child candies */
		if (entity.children != null && entity.hasVisibleChildren()) {
			((ImageView) candiInfoView.findViewById(R.id.image_forward)).setVisibility(View.VISIBLE);
		}
		else {
			((ImageView) candiInfoView.findViewById(R.id.image_forward)).setVisibility(View.GONE);
		}

		final WebImageView image = (WebImageView) candiInfoView.findViewById(R.id.image_public);
		final ImageView imageReflection = (ImageView) candiInfoView.findViewById(R.id.image_public_reflection);
		final TextView title = (TextView) candiInfoView.findViewById(R.id.candi_info_title);
		final TextView subtitle = (TextView) candiInfoView.findViewById(R.id.candi_info_subtitle);
		final TextView description = (TextView) candiInfoView.findViewById(R.id.candi_info_description);
		final AuthorBlock authorBlock = (AuthorBlock) candiInfoView.findViewById(R.id.block_author);
		final Button comments = (Button) candiInfoView.findViewById(R.id.button_comments);
		final ImageView navigate = (ImageView) candiInfoView.findViewById(R.id.image_forward);

		/* Candi image */
		if (entity.imageUri != null && entity.imageUri.length() != 0) {
			ImageRequest imageRequest = new ImageRequest(entity.imageUri, ImageShape.Square, entity.imageFormat,
					entity.javascriptEnabled,
					CandiConstants.IMAGE_WIDTH_SEARCH_MAX, true, true, true, 1, this, null);
			image.setImageRequest(imageRequest, imageReflection);
		}

		/* Author block */
		if (entity.author != null) {
			authorBlock.bindToAuthor(entity.author, DateUtils.wcfToDate(entity.createdDate));
		}
		else {
			authorBlock.setVisibility(View.GONE);
		}

		/* Navigate indicator */
		if (entity.childCount == 0) {
			((ViewGroup) candiInfoView.findViewById(R.id.group_candi_content)).setClickable(false);
			navigate.setVisibility(View.GONE);
		}
		else {
			((ViewGroup) candiInfoView.findViewById(R.id.group_candi_content)).setClickable(true);
			navigate.setVisibility(View.VISIBLE);
		}

		/* Comments */
		comments.setText(String.valueOf(entity.commentCount) + (entity.commentCount == 1 ? "\nComment" : "\nComments"));
		comments.setTag(entity);

		/* Candi text */
		title.setText(null);
		subtitle.setText(null);
		description.setText(null);

		if (entity.title != null) {
			title.setText(Html.fromHtml(entity.title));
		}
		if (entity.subtitle != null) {
			subtitle.setText(Html.fromHtml(entity.subtitle));
		}
		if (entity.description != null) {
			description.setText(Html.fromHtml(entity.description));
		}

		return candiInfoView;
	}

	public TableLayout configureMenus(EntityProxy entity) {

		Boolean needMoreButton = false;

		if (entity.commands == null || entity.commands.size() == 0) {
			return null;
		}
		if (entity.commands.size() > 6) {
			needMoreButton = true;
		}

		/* Get the table we use for grouping and clear it */
		final TableLayout table = new TableLayout(mContext);

		/* Make the first row */
		TableRow tableRow = (TableRow) mInflater.inflate(R.layout.temp_tablerow_commands, null);
		final TableRow.LayoutParams rowLp = new TableRow.LayoutParams();
		rowLp.setMargins(0, 0, 0, 0);
		TableLayout.LayoutParams tableLp;

		/* Loop the streams */
		Integer commandCount = 0;
		RelativeLayout commandButtonContainer;
		for (Command command : entity.commands) {
			/*
			 * TODO: This is a temporary hack. The service shouldn't pass commands
			 * that this user doesn't have sufficient permissions for.
			 */
			if (command.name.toLowerCase().contains("edit")) {
				if (entity.createdById != null && !entity.createdById.toString().equals(mUser.id)) {
					continue;
				}
			}

			/* Make a button and configure it */
			command.entity = entity;
			commandButtonContainer = (RelativeLayout) mInflater.inflate(R.layout.temp_button_command, null);

			final TextView commandButton = (TextView) commandButtonContainer.findViewById(R.id.CommandButton);
			commandButtonContainer.setTag(command);
			if (needMoreButton && commandCount == 5) {
				commandButton.setText("More...");
				commandButton.setTag(command);
			}
			else {
				commandButton.setText(command.labelCustom != null ? command.labelCustom : command.label);
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
				tableRow = (TableRow) mInflater.inflate(R.layout.temp_tablerow_commands, null);
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
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.candi_form;
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
}