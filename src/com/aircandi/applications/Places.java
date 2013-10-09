package com.aircandi.applications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.IntentBuilder;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.ui.EntityList;
import com.aircandi.ui.PlaceForm;
import com.aircandi.ui.edit.PlaceEdit;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;

public class Places {

	public static int	ICON_COLOR	= R.color.accent_red;

	public static void view(Context context, String entityId, String parentId) {
		IntentBuilder intentBuilder = new IntentBuilder(context, PlaceForm.class)
				.setEntityId(entityId)
				.setEntityParentId(parentId);
		context.startActivity(intentBuilder.create());
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
	}

	public static void viewFor(Context context, String entityId, String linkType, Direction direction) {
		if (direction == null) {
			direction = Direction.in;
		}
		IntentBuilder intentBuilder = new IntentBuilder(context, EntityList.class)
				.setEntityId(entityId)
				.setListLinkType(linkType)
				.setListLinkDirection(direction.name())
				.setListLinkSchema(Constants.SCHEMA_ENTITY_PLACE)
				.setListItemResId(R.layout.temp_listitem_entity)
				.setListPageSize(ServiceConstants.PAGE_SIZE_PLACES)
				.setListNewEnabled(true);

		context.startActivity(intentBuilder.create());
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
	}

	public static Intent viewForGetIntent(Context context, String entityId, String linkType, Direction direction, Boolean linkInactive, String title) {
		if (direction == null) {
			direction = Direction.in;
		}
		IntentBuilder intentBuilder = new IntentBuilder(context, EntityList.class)
				.setEntityId(entityId)
				.setListLinkType(linkType)
				.setListLinkDirection(direction.name())
				.setListLinkInactive(linkInactive != null ? linkInactive.toString() : null) // we pass string because bundles don't support null for booleans
				.setListTitle(title)
				.setListLinkSchema(Constants.SCHEMA_ENTITY_PLACE)
				.setListItemResId(R.layout.temp_listitem_entity)
				.setListPageSize(ServiceConstants.PAGE_SIZE_PLACES)
				.setListNewEnabled(true);

		return intentBuilder.create();
	}

	@SuppressWarnings("ucd")
	public static void edit(Context context, Entity entity) {
		IntentBuilder intentBuilder = new IntentBuilder(context, PlaceEdit.class).setEntity(entity);
		((Activity) context).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_EDIT);
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
	}

	@SuppressWarnings("ucd")
	public static void insert(Context context) {
		IntentBuilder intentBuilder = new IntentBuilder(context, PlaceEdit.class).setEntitySchema(Constants.SCHEMA_ENTITY_PLACE);
		((Activity) context).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_INSERT);
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
	}
}
