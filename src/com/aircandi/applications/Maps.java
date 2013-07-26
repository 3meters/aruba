package com.aircandi.applications;

import android.app.Activity;
import android.content.Context;

import com.aircandi.components.IntentBuilder;
import com.aircandi.ui.MapSimpleForm;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;

public class Maps {

	public static void view(Context context, String entityId) {
		IntentBuilder intentBuilder = new IntentBuilder(context, MapSimpleForm.class).setEntityId(entityId);
		context.startActivity(intentBuilder.create());
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PageToPage);
	}
}
