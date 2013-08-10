package com.aircandi.components;

import android.app.Activity;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.aircandi.beta.R;

public class MenuManager {

	public static boolean onCreateOptionsMenu(Activity activity, Fragment fragment, Menu menu, MenuInflater inflater) {

		/* Browsing */

		if (activity != null) {

			String activityName = activity.getClass().getSimpleName();
			final SherlockFragmentActivity sherlock = (SherlockFragmentActivity) activity;
			MenuInflater menuInflater = sherlock.getSupportMenuInflater();

			if (activityName.equals("AircandiForm")) {
				menuInflater.inflate(R.menu.menu_base_end, menu);
			}
			else if (activityName.equals("PlaceForm")) {
				menuInflater.inflate(R.menu.menu_browse_place, menu);
				menuInflater.inflate(R.menu.menu_base_end, menu);
				menuInflater.inflate(R.menu.menu_base_help, menu);
			}
			else if (activityName.equals("PictureForm")) {
				menuInflater.inflate(R.menu.menu_browse_entity, menu);
				menuInflater.inflate(R.menu.menu_base_end, menu);
			}
			else if (activityName.equals("UserForm")) {
				menuInflater.inflate(R.menu.menu_browse_user, menu);
				menuInflater.inflate(R.menu.menu_base_end, menu);
			}
			else if (activityName.equals("EntityList")
					|| activityName.equals("EntityGrid")) {
				menuInflater.inflate(R.menu.menu_browse_entity_list, menu);
				menuInflater.inflate(R.menu.menu_base_end, menu);
			}
			else if (activityName.equals("HelpForm")) {
				menuInflater.inflate(R.menu.menu_browse_help, menu);
			}

			/* Editing */

			else if (activityName.equals("CommentEdit")
					|| activityName.equals("FeedbackEdit")) {
				menuInflater.inflate(R.menu.menu_insert_entity, menu);
			}
			else if (activityName.equals("ApplinkListEdit")) {
				menuInflater.inflate(R.menu.menu_edit_list, menu);
			}
			else if (activityName.equals("ApplinkEdit")
					|| activityName.equals("UserEdit")
					|| activityName.equals("PasswordEdit")
					|| activityName.equals("TuningEdit")
					|| activityName.contains("SignInEdit")) {
				menuInflater.inflate(R.menu.menu_edit_builder, menu);
			}
			else if (activityName.contains("Edit")) {
				menuInflater.inflate(R.menu.menu_edit_entity, menu);
			}
			else if (activityName.contains("Builder")) {
				menuInflater.inflate(R.menu.menu_edit_builder, menu);
			}
			else {
				return false;
			}
			return true;
		}

		/* Fragments */

		if (fragment != null) {

			String fragmentName = fragment.getClass().getSimpleName();
			MenuInflater menuInflater = inflater;

			if (fragmentName.equals("RadarFragment")) {
				menuInflater.inflate(R.menu.menu_browse_radar, menu);
				menuInflater.inflate(R.menu.menu_base_help, menu);
			}
			else if (fragmentName.equals("CreatedFragment")) {
				menuInflater.inflate(R.menu.menu_browse_created, menu);
			}
			else if (fragmentName.equals("WatchingFragment")) {
				menuInflater.inflate(R.menu.menu_browse_watching, menu);
			}
			else if (fragmentName.equals("NotificationFragment")) {
				menuInflater.inflate(R.menu.menu_browse_notification_list, menu);
			}
		}

		return true;
	}
}
