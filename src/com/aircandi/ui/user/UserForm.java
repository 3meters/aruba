package com.aircandi.ui.user;

import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Stat;
import com.aircandi.service.objects.User;
import com.aircandi.ui.base.BaseEntityForm;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class UserForm extends BaseEntityForm {

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mLinkProfile = LinkProfile.LinksForUser;
	}

	@Override
	public void afterDatabind() {
		loadStats();
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	protected void loadStats() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("GetStats");

				/* Get user stats using rest api */
				ModelResult result = EntityManager.getInstance().getUserStats(mEntityId);
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					((User) mEntity).stats = (List<Stat>) result.data;
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					if (result.data != null) {
						drawStats();
					}
				}
				else {
					Routing.serviceError(UserForm.this, result.serviceResponse);
				}
				hideBusy();
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void draw() {

		setActivityTitle(mEntity.name);
		if (mMenuItemEdit != null) {
			mMenuItemEdit.setVisible(EntityManager.canUserEdit(mEntity));
		}
		if (mMenuItemSignout != null) {
			mMenuItemSignout.setVisible(EntityManager.canUserEdit(mEntity));
		}

		User user = (User) mEntity;

		final AirImageView photoView = (AirImageView) findViewById(R.id.photo);
		final TextView area = (TextView) findViewById(R.id.area);
		final TextView webUri = (TextView) findViewById(R.id.web_uri);
		final TextView bio = (TextView) findViewById(R.id.bio);

		UI.setVisibility(photoView, View.GONE);
		if (photoView != null) {
			Photo photo = mEntity.getPhoto();
			UI.drawPhoto(photoView, photo);
			if (photo.usingDefault == null || !photo.usingDefault) {
				photoView.setClickable(true);
			}
			UI.setVisibility(photoView, View.VISIBLE);
		}

		/* Description section */

		UI.setVisibility(findViewById(R.id.section_details), View.GONE);

		SectionLayout section = (SectionLayout) findViewById(R.id.section_details);
		if (section != null && user.name != null && !user.name.equals("")) {
			section.setHeaderTitle(Html.fromHtml(user.name).toString());
			UI.setVisibility(findViewById(R.id.section_details), View.VISIBLE);
		}

		UI.setVisibility(area, View.GONE);
		if (area != null && user.area != null && !user.area.equals("")) {
			area.setText(Html.fromHtml(user.area));
			UI.setVisibility(area, View.VISIBLE);
			UI.setVisibility(findViewById(R.id.section_details), View.VISIBLE);
		}

		UI.setVisibility(webUri, View.GONE);
		if (webUri != null && user.webUri != null && !user.webUri.equals("")) {
			webUri.setText(Html.fromHtml(user.webUri));
			UI.setVisibility(webUri, View.VISIBLE);
			UI.setVisibility(findViewById(R.id.section_details), View.VISIBLE);
		}

		UI.setVisibility(bio, View.GONE);
		if (bio != null && user.bio != null && !user.bio.equals("")) {
			bio.setText(Html.fromHtml(user.bio));
			UI.setVisibility(bio, View.VISIBLE);
			UI.setVisibility(findViewById(R.id.section_details), View.VISIBLE);
		}

		if (user.stats != null) {
			drawStats();
		}

		drawButtons();

		if (mScrollView != null) {
			mScrollView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void drawStats() {

		User user = (User) mEntity;
		final TextView stats = (TextView) findViewById(R.id.stats);

		UI.setVisibility(stats, View.GONE);
		final StringBuilder statString = new StringBuilder(500);

		/* Like and watch stats */

		Count count = user.getCount(Constants.TYPE_LINK_LIKE, Direction.in);
		if (count == null) count = new Count(Constants.TYPE_LINK_LIKE, 0);
		statString.append("Liked by: " + String.valueOf(count.count.intValue()) + "<br/>");

		count = user.getCount(Constants.TYPE_LINK_WATCH, Direction.in);
		if (count == null) count = new Count(Constants.TYPE_LINK_WATCH, 0);
		statString.append("Watchers: " + String.valueOf(count.count.intValue()) + "<br/>");

		/* Other stats */

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

				if (stat.type.equals("insert_entity_place_custom")) {
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
				statString.append("Places: " + String.valueOf(placeInsertCount) + "<br/>");
			}

			if (postInsertCount > 0) {
				statString.append("Pictures: " + String.valueOf(postInsertCount) + "<br/>");
			}

			if (commentInsertCount > 0) {
				statString.append("Comments: " + String.valueOf(commentInsertCount) + "<br/>");
			}

			if (placeEditCount > 0) {
				statString.append("Places edited: " + String.valueOf(placeEditCount) + "<br/>");
			}

			if (postEditCount > 0) {
				statString.append("Pictures edited: " + String.valueOf(postEditCount) + "<br/>");
			}

			if (tuneCount > 0) {
				statString.append("Places tuned: " + String.valueOf(tuneCount) + "<br/>");
			}

			stats.setText(Html.fromHtml(statString.toString()));
			UI.setVisibility(stats, View.VISIBLE);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		mMenuItemSignout = menu.findItem(R.id.signout);
		if (mMenuItemSignout != null) {
			mMenuItemSignout.setVisible(EntityManager.canUserEdit(mEntity));
		}

		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.user_form;
	}
}