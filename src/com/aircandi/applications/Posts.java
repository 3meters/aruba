package com.aircandi.applications;

import android.app.Activity;

import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.IntentBuilder;
import com.aircandi.service.objects.Entity;
import com.aircandi.ui.EntityList;
import com.aircandi.ui.PostForm;
import com.aircandi.ui.edit.PostEdit;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;

public class Posts extends BaseApp {

	public static void view(Activity activity, String entityId) {
		IntentBuilder intentBuilder = new IntentBuilder(activity, PostForm.class).setEntityId(entityId);
		activity.startActivity(intentBuilder.create());
		Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
	}

	public static void viewFor(Activity activity, Entity entity, String linkType) {
		IntentBuilder intentBuilder = new IntentBuilder(activity, EntityList.class)
				.setEntityId(entity.id)
				.setListLinkType(linkType)
				.setListLinkSchema(Constants.SCHEMA_ENTITY_POST)
				.setListItemResId(R.layout.temp_listitem_entity)
				.setListNewEnabled(true);

		activity.startActivity(intentBuilder.create());
		Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
	}

	public static void edit(Activity activity, Entity entity) {
		IntentBuilder intentBuilder = new IntentBuilder(activity, PostEdit.class).setEntity(entity);
		activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_EDIT);
		Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
	}

	public static void insert(Activity activity) {
		IntentBuilder intentBuilder = new IntentBuilder(activity, PostEdit.class).setEntitySchema(Constants.SCHEMA_ENTITY_POST);
		activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_INSERT);
		Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
	}
}
