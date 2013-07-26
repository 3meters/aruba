package com.aircandi.components;

import android.app.Activity;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.aircandi.beta.R;

public class MenuManager {

	public static boolean onCreateOptionsMenu(Activity activity, Menu menu) {

		String activityName = activity.getClass().getSimpleName();
		final SherlockFragmentActivity sherlock = (SherlockFragmentActivity) activity;

		/* Browsing */

		if (activityName.equals("RadarForm")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_browse_radar, menu);
		}
		else if (activityName.equals("PlaceForm")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_browse_place, menu);
		}
		else if (activityName.equals("PictureForm")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_browse_entity, menu);
		}
		else if (activityName.equals("UserForm")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_browse_user, menu);
		}
		else if (activityName.equals("EntityList")
				|| activityName.equals("EntityGrid")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_browse_entity_list, menu);
		}
		else if (activityName.equals("NotificationList")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_browse_notification_list, menu);
		}
		else if (activityName.equals("HelpForm")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_browse_help, menu);
		}

		/* Editing */

		else if (activityName.equals("CommentEdit")
				|| activityName.equals("FeedbackEdit")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_insert_entity, menu);
		}
		else if (activityName.equals("ApplinkListEdit")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_edit_list, menu);
		}
		else if (activityName.equals("ApplinkEdit")
				|| activityName.equals("UserEdit")
				|| activityName.equals("PasswordEdit")
				|| activityName.equals("TuningEdit")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_edit_builder, menu);
		}
		else if (activityName.contains("Edit")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_edit_entity, menu);
		}
		else if (activityName.contains("Builder")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_edit_builder, menu);
		}
		else {
			return false;
		}

		return true;
	}
}
