package com.aircandi.applications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.IntentBuilder;
import com.aircandi.service.objects.Entity;
import com.aircandi.ui.EntityList;
import com.aircandi.ui.edit.CommentEdit;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;

public class Comments extends BaseApp {

	public static void view(Activity activity, String entityId) {}

	public static void viewFor(Activity activity, Entity entity, String linkType) {
		IntentBuilder intentBuilder = new IntentBuilder(activity, EntityList.class)
				.setEntityId(entity.id)
				.setListLinkType(linkType)
				.setListLinkSchema(Constants.SCHEMA_ENTITY_COMMENT)
				.setListItemResId(R.layout.temp_listitem_comment)
				.setListNewEnabled(true);

		activity.startActivity(intentBuilder.create());
		Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
	}

	public static Intent viewForGetIntent(Context context, String entityId, String linkType) {
		IntentBuilder intentBuilder = new IntentBuilder(context, EntityList.class)
				.setEntityId(entityId)
				.setListLinkType(linkType)
				.setListLinkSchema(Constants.SCHEMA_ENTITY_COMMENT)
				.setListItemResId(R.layout.temp_listitem_comment)
				.setListNewEnabled(true);

		return intentBuilder.create();
	}
	
	public static void edit(Activity activity, Entity entity) {
		IntentBuilder intentBuilder = new IntentBuilder(activity, CommentEdit.class).setEntity(entity);
		activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_EDIT);
		Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
	}

	public static void insert(Activity activity) {
		IntentBuilder intentBuilder = new IntentBuilder(activity, CommentEdit.class).setEntitySchema(Constants.SCHEMA_ENTITY_COMMENT);
		activity.startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_INSERT);
		Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
	}
}
