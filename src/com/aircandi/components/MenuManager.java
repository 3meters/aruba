package com.aircandi.components;

import android.app.Activity;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.aircandi.beta.R;

public class MenuManager {

	public static boolean onCreateOptionsMenu(Activity activity, Menu menu) {
		
		String activityName = activity.getClass().getSimpleName();
		final SherlockActivity sherlock = (SherlockActivity) activity;
		
		/* Browsing */
		
		if (activityName.equals("RadarForm")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_radar, menu);
		}
		else if (activityName.equals("PlaceForm") || activityName.equals("PostForm")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_candi, menu);
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_base, menu);
		}
		else if (activityName.equals("EntityList")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_entity_list, menu);
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_base, menu);
		}
		else if (activityName.equals("UserForm")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_user, menu);
		}
		else if (activityName.equals("HelpForm")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_help, menu);
		}

		/* Editing */

		else if (activityName.equals("CommentEdit")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_comment_form, menu);
		}
		else if (activityName.equals("FeedbackEdit")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_send, menu);
		}
		else if (activityName.equals("TuningEdit")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_tuning_wizard, menu);
		}
		else if (activityName.equals("ApplinkEdit") || activityName.equals("ApplinksEdit")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_builder, menu);
		}
		else if (activityName.equals("UserEdit") || activityName.equals("PasswordEdit")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_builder, menu);
		}
		else if (activityName.contains("Edit")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_entity, menu);
		}
		else if (activityName.contains("Builder")) {
			sherlock.getSupportMenuInflater().inflate(R.menu.menu_builder, menu);
		}
		else {
			return false;
		}
		
		return true;
	}
}
