package com.aircandi.ui;

import java.util.Collections;
import java.util.List;

import android.os.AsyncTask;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.DefaultType;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.widgets.CandiView;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;

public class PostForm extends BaseEntityForm {

	@Override
	protected void bind(final Boolean refreshProposed) {
		/*
		 * Navigation setup for action bar icon and title
		 */
		Logger.d(this, "Binding candi form");

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy();
				mBusyManager.startBodyBusyIndicator();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("GetEntity");

				Entity entity = EntityManager.getEntity(mEntityId);
				Boolean refresh = refreshProposed;
				if (entity == null || !entity.shortcuts) {
					refresh = true;
				}

				final ModelResult result = EntityManager.getInstance().getEntity(mEntityId
						, refresh
						, LinkOptions.getDefault(DefaultType.LinksForPost));
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					if (result.data != null) {
						mEntity = (Entity) result.data;
						mEntityModelRefreshDate = ProximityManager.getInstance().getLastBeaconLoadDate();
						mEntityModelActivityDate = EntityManager.getEntityCache().getLastActivityDate();
						mActionBar.setTitle(mEntity.name);
						draw();
					}
				}
				else {
					Routing.serviceError(PostForm.this, result.serviceResponse);
				}
				mBusyManager.hideBusy();
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void draw() {
		drawForm();
	}

	private void drawForm() {
		/*
		 * For now, we assume that the candi form isn't recycled.
		 * 
		 * We leave most of the views visible by default so they are visible in the layout editor.
		 * 
		 * - WebImageView primary image is visible by default
		 * - WebImageView child views are gone by default
		 * - Header views are visible by default
		 */
		final CandiView candiView = (CandiView) findViewById(R.id.candi_view);
		final WebImageView image = (WebImageView) findViewById(R.id.photo);
		final TextView name = (TextView) findViewById(R.id.name);
		final TextView subtitle = (TextView) findViewById(R.id.subtitle);

		final TextView description = (TextView) findViewById(R.id.description);
		final UserView user_one = (UserView) findViewById(R.id.user_one);
		final UserView user_two = (UserView) findViewById(R.id.user_two);

		if (candiView != null) {
			/*
			 * This is a place entity with a fancy image widget
			 */
			candiView.bindToPlace((Place) mEntity);
		}
		else {
			UI.setVisibility(image, View.GONE);
			if (image != null) {
				final String photoUri = mEntity.getPhotoUri();
				if (photoUri != null) {
					if (mEntity.creator == null || !photoUri.equals(mEntity.creator.getPhotoUri())) {

						final BitmapRequestBuilder builder = new BitmapRequestBuilder(image).setImageUri(photoUri);
						final BitmapRequest imageRequest = builder.create();

						image.setBitmapRequest(imageRequest);
						image.setClickable(false);

						if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_POST)) {
							image.setClickable(true);
						}
						UI.setVisibility(image, View.VISIBLE);
					}
				}
			}

			name.setText(null);
			subtitle.setText(null);

			UI.setVisibility(name, View.GONE);
			if (name != null && mEntity.name != null && !mEntity.name.equals("")) {
				name.setText(Html.fromHtml(mEntity.name));
				UI.setVisibility(name, View.VISIBLE);
			}

			UI.setVisibility(subtitle, View.GONE);
			if (subtitle != null && mEntity.subtitle != null && !mEntity.subtitle.equals("")) {
				subtitle.setText(Html.fromHtml(mEntity.subtitle));
				UI.setVisibility(subtitle, View.VISIBLE);
			}
		}

		/* Primary candi image */

		description.setText(null);

		UI.setVisibility(findViewById(R.id.section_description), View.GONE);
		if (description != null && mEntity.description != null && !mEntity.description.equals("")) {
			description.setText(Html.fromHtml(mEntity.description));
			UI.setVisibility(findViewById(R.id.section_description), View.VISIBLE);
		}

		/* Clear shortcut holder */
		((ViewGroup) findViewById(R.id.shortcut_holder)).removeAllViews();

		/* Synthetic applink shortcuts */
		ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_APPLINK, null, Direction.in, true);
		List<Shortcut> shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, true);
		if (shortcuts.size() > 0) {
			drawShortcuts(shortcuts
					, settings
					, R.string.candi_section_shortcuts_place
					, R.string.candi_section_links_more
					, mResources.getInteger(R.integer.candi_flow_limit)
					, R.layout.temp_place_switchboard_item);
		}

		/* Service applink shortcuts */
		settings = new ShortcutSettings(Constants.TYPE_LINK_APPLINK, null, Direction.in, false);
		shortcuts = (List<Shortcut>) mEntity.getShortcuts(settings, true);
		Collections.sort(shortcuts, new Shortcut.SortByPosition());

		if (shortcuts.size() > 0) {
			drawShortcuts(shortcuts
					, settings
					, null
					, null
					, mResources.getInteger(R.integer.candi_flow_limit)
					, R.layout.temp_place_switchboard_item);
		}

		/* Creator block */

		UI.setVisibility(user_one, View.GONE);
		UI.setVisibility(user_two, View.GONE);
		UserView user = user_one;

		if (user != null
				&& mEntity.creator != null
				&& !mEntity.creator.id.equals(ProxiConstants.ADMIN_USER_ID)) {

			if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				if (((Place) mEntity).getProvider().type.equals("aircandi")) {
					user.setLabel(getString(R.string.candi_label_user_created_by));
					user.bindToUser(mEntity.creator, mEntity.createdDate.longValue(), mEntity.locked);
					UI.setVisibility(user, View.VISIBLE);
					user = user_two;
				}
			}
			else {
				if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_POST)) {
					user.setLabel(getString(R.string.candi_label_user_added_by));
				}
				else {
					user.setLabel(getString(R.string.candi_label_user_created_by));
				}
				user.bindToUser(mEntity.creator, mEntity.createdDate.longValue(), mEntity.locked);
				UI.setVisibility(user_one, View.VISIBLE);
				user = user_two;
			}
		}

		/* Editor block */

		if (user != null && mEntity.modifier != null && !mEntity.modifier.id.equals(ProxiConstants.ADMIN_USER_ID)) {
			if (mEntity.createdDate.longValue() != mEntity.modifiedDate.longValue()) {
				user.setLabel(getString(R.string.candi_label_user_edited_by));
				user.bindToUser(mEntity.modifier, mEntity.modifiedDate.longValue(), null);
				UI.setVisibility(user, View.VISIBLE);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.post_form;
	}

}