package com.aircandi.ui.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.EntityManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Maps;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Cursor;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.DefaultType;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.service.objects.Stat;
import com.aircandi.service.objects.User;
import com.aircandi.ui.EntityList;
import com.aircandi.ui.EntityList.ListMode;
import com.aircandi.ui.base.BaseEntityView;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

@SuppressWarnings("ucd")
public class UserForm extends BaseEntityView {

	private List<Entity>	mEntitiesOwned;

	@Override
	protected void bind(final Boolean refreshProposed) {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy(true);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("GetUser");

				Entity entity = EntityManager.getEntity(mCommon.mEntityId);
				Boolean refresh = refreshProposed;
				if (entity == null || !entity.shortcuts) {
					refresh = true;
				}

				ModelResult result = EntityManager.getInstance().getEntity(mEntity.id
						, refresh
						, LinkOptions.getDefault(DefaultType.LinksForUser));

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					mEntity = (Entity) result.data;

					result = EntityManager.getInstance().getEntitiesByOwner(mEntity.id
							, true
							, Arrays.asList(Constants.SCHEMA_ANY)
							, LinkOptions.getDefault(DefaultType.NoLinks)
							, new Cursor(Maps.asMap("modifiedDate", -1), 0, ProxiConstants.LIMIT_USER_OWNED_ENTITIES));

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						mEntitiesOwned = (List<Entity>) result.data;
					}
				}

				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					if (result.data != null) {
						mEntityModelRefreshDate = ProximityManager.getInstance().getLastBeaconLoadDate();
						mEntityModelActivityDate = EntityManager.getEntityCache().getLastActivityDate();
						mCommon.mActionBar.setTitle(mEntity.name);
						drawForm();
					}
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiUser);
				}
				mCommon.hideBusy(true);
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onMoreButtonClick(View view) {

		ShortcutSettings settings = (ShortcutSettings) view.getTag();
		IntentBuilder intentBuilder = null;

		intentBuilder = new IntentBuilder(this, EntityList.class)
				.setListMode(ListMode.EntitiesByOwner)
				.setEntitySchema(settings.targetSchema)
				.setUserId(mCommon.mUserId);

		Intent intent = intentBuilder.create();
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToPage);
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void draw() {
		drawForm();
	}

	private void drawForm() {

		User user = (User) mEntity;

		final WebImageView photo = (WebImageView) findViewById(R.id.photo);
		final TextView name = (TextView) findViewById(R.id.name);
		final TextView area = (TextView) findViewById(R.id.area);
		final TextView webUri = (TextView) findViewById(R.id.web_uri);
		final TextView bio = (TextView) findViewById(R.id.bio);
		final TextView stats = (TextView) findViewById(R.id.stats);

		setVisibility(photo, View.GONE);
		if (photo != null) {
			final String photoUri = mEntity.getPhotoUri();
			if (photoUri != null) {
				if (mEntity.creator == null || !photoUri.equals(mEntity.creator.getPhotoUri())) {

					final BitmapRequestBuilder builder = new BitmapRequestBuilder(photo).setImageUri(photoUri);
					final BitmapRequest imageRequest = builder.create();

					photo.setBitmapRequest(imageRequest);
					photo.setClickable(false);

					if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_POST)) {
						photo.setClickable(true);
					}
					setVisibility(photo, View.VISIBLE);
				}
			}
		}

		/* Description section */

		setVisibility(findViewById(R.id.section_details), View.GONE);

		setVisibility(name, View.GONE);
		if (name != null && user.name != null && !user.name.equals("")) {
			name.setText(Html.fromHtml(user.name));
			setVisibility(name, View.VISIBLE);
			setVisibility(findViewById(R.id.section_details), View.VISIBLE);
		}

		setVisibility(area, View.GONE);
		if (area != null && user.location != null && !user.location.equals("")) {
			area.setText(Html.fromHtml(user.area));
			setVisibility(area, View.VISIBLE);
			setVisibility(findViewById(R.id.section_details), View.VISIBLE);
		}

		setVisibility(webUri, View.GONE);
		if (webUri != null && user.webUri != null && !user.webUri.equals("")) {
			webUri.setText(Html.fromHtml(user.webUri));
			setVisibility(webUri, View.VISIBLE);
			setVisibility(findViewById(R.id.section_details), View.VISIBLE);
		}

		setVisibility(bio, View.GONE);
		if (bio != null && user.bio != null && !user.bio.equals("")) {
			bio.setText(Html.fromHtml(user.bio));
			setVisibility(bio, View.VISIBLE);
			setVisibility(findViewById(R.id.section_details), View.VISIBLE);
		}

		/* Stats */

		setVisibility(findViewById(R.id.section_stats), View.GONE);
		setVisibility(stats, View.GONE);
		final StringBuilder statString = new StringBuilder(500);
		Count count = user.getCount(Constants.TYPE_LINK_LIKE, Direction.in);
		if (count != null && user.getCount(Constants.TYPE_LINK_LIKE, Direction.in).count.intValue() > 0) {
			statString.append("Likes: " + String.valueOf(count.count.intValue()) + "<br/>");
		}
		count = user.getCount(Constants.TYPE_LINK_WATCH, Direction.in);
		if (count != null && user.getCount(Constants.TYPE_LINK_WATCH, Direction.in).count.intValue() > 0) {
			statString.append("Watchers: " + String.valueOf(count.count.intValue()) + "<br/>");
		}

		if (stats != null && user.stats != null && user.stats.size() > 0) {

			int tuneCount = 0;
			@SuppressWarnings("unused")
			int editCount = 0;
			@SuppressWarnings("unused")
			int insertCount = 0;

			int postInsertCount = 0;
			int placeInsertCount = 0;
			int commentInsertCount = 0;

			int postEditCount = 0;
			int placeEditCount = 0;

			for (Stat stat : user.stats) {
				if (stat.type.startsWith("link_proximity")) {
					tuneCount += stat.countBy.intValue();
				}

				if (stat.type.equals("entity_proximity")) {
					tuneCount += stat.countBy.intValue();
				}

				if (stat.type.startsWith("update_entity")) {
					editCount += stat.countBy.intValue();
				}

				if (stat.type.startsWith("insert_entity")) {
					insertCount += stat.countBy.intValue();
				}

				if (stat.type.equals("insert_entity_place")) {
					placeInsertCount += stat.countBy.intValue();
				}
				else if (stat.type.equals("insert_entity_post")) {
					postInsertCount += stat.countBy.intValue();
				}
				else if (stat.type.equals("insert_entity_comment")) {
					commentInsertCount += stat.countBy.intValue();
				}
				else if (stat.type.equals("update_entity_place")) {
					placeEditCount += stat.countBy.intValue();
				}
				else if (stat.type.equals("update_entity_post")) {
					postEditCount += stat.countBy.intValue();
				}
			}

			if (placeInsertCount > 0) {
				statString.append("Places created: " + String.valueOf(placeInsertCount) + "<br/>");
			}

			if (postInsertCount > 0) {
				statString.append("Posts authored: " + String.valueOf(postInsertCount) + "<br/>");
			}

			if (commentInsertCount > 0) {
				statString.append("Comments: " + String.valueOf(commentInsertCount) + "<br/>");
			}

			if (placeEditCount > 0) {
				statString.append("Places edited: " + String.valueOf(placeEditCount) + "<br/>");
			}

			if (postEditCount > 0) {
				statString.append("Posts edited: " + String.valueOf(postEditCount) + "<br/>");
			}

			if (tuneCount > 0) {
				statString.append("Places tuned: " + String.valueOf(tuneCount) + "<br/>");
			}

			stats.setText(Html.fromHtml(statString.toString()));
			setVisibility(stats, View.VISIBLE);
			setVisibility(findViewById(R.id.section_stats), View.VISIBLE);
		}

		/* Clear shortcut holder */
		((ViewGroup) findViewById(R.id.shortcut_holder)).removeAllViews();

		/* Place shortcuts */
		ShortcutSettings settings = new ShortcutSettings(null, Constants.SCHEMA_ENTITY_PLACE, Direction.out, false);
		List<Shortcut> shortcuts = new ArrayList<Shortcut>();
		for (Entity entity : mEntitiesOwned) {
			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				shortcuts.add(entity.getShortcut());
			}
		}

		Collections.sort(shortcuts, new Shortcut.SortByPosition());

		if (shortcuts.size() > 0) {
			drawShortcuts(shortcuts
					, settings
					, R.string.candi_section_shortcuts_place
					, R.string.candi_section_links_more
					, mResources.getInteger(R.integer.candi_flow_limit)
					, R.layout.temp_place_switchboard_item);
		}

		drawButtons();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.user_form;
	}
}