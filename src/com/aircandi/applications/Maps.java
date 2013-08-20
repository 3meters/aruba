package com.aircandi.applications;

import android.app.Activity;
import android.content.Context;

import com.aircandi.R;
import com.aircandi.components.IntentBuilder;
import com.aircandi.ui.MapForm;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;

public class Maps {
	
	public static int ICON_COLOR = R.color.accent_blue_dark;

	public static void view(Context context, String entityId) {
		IntentBuilder intentBuilder = new IntentBuilder(context, MapForm.class).setEntityId(entityId);
		context.startActivity(intentBuilder.create());
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PageToPage);
	}
}
