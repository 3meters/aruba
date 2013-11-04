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
import com.aircandi.ui.EntityGrid;
import com.aircandi.ui.PictureForm;
import com.aircandi.ui.edit.PictureEdit;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;

public class Pictures {
	public static int	ICON_COLOR	= R.color.accent_green_dark;

	public static void view(Context context, String entityId, String linkType, String parentId) {
		IntentBuilder intentBuilder = new IntentBuilder(context, PictureForm.class)
				.setListLinkType(linkType)
				.setListLinkSchema(Constants.SCHEMA_ENTITY_PICTURE)
				.setEntityId(entityId)
				.setEntityParentId(parentId);
		context.startActivity(intentBuilder.create());
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
	}

	public static void viewFor(Context context, String entityId, String linkType, Direction direction) {
		if (direction == null) {
			direction = Direction.in;
		}
		IntentBuilder intentBuilder = new IntentBuilder(context, EntityGrid.class)
				.setEntityId(entityId)
				.setListLinkType(linkType)
				.setListLinkDirection(direction.name())
				.setListLinkSchema(Constants.SCHEMA_ENTITY_PICTURE)
				.setListItemResId(R.layout.temp_grid_item_entity)
				.setListPageSize(ServiceConstants.PAGE_SIZE_PICTURES)
				.setListNewEnabled(true);

		context.startActivity(intentBuilder.create());
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
	}

	public static Intent viewForGetIntent(Context context, String entityId, String linkType, Direction direction, String title) {
		if (direction == null) {
			direction = Direction.in;
		}
		IntentBuilder intentBuilder = new IntentBuilder(context, EntityGrid.class)
				.setEntityId(entityId)
				.setListLinkType(linkType)
				.setListLinkDirection(direction.name())
				.setListLinkSchema(Constants.SCHEMA_ENTITY_PICTURE)
				.setListTitle(title)
				.setListItemResId(R.layout.temp_grid_item_entity)
				.setListPageSize(ServiceConstants.PAGE_SIZE_PICTURES)
				.setListNewEnabled(true);

		return intentBuilder.create();
	}

	@SuppressWarnings("ucd")
	public static void edit(Context context, Entity entity) {
		IntentBuilder intentBuilder = new IntentBuilder(context, PictureEdit.class).setEntity(entity);
		((Activity) context).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_EDIT);
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
	}

	@SuppressWarnings("ucd")
	public static void insert(Context context) {
		IntentBuilder intentBuilder = new IntentBuilder(context, PictureEdit.class).setEntitySchema(Constants.SCHEMA_ENTITY_PICTURE);
		((Activity) context).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_INSERT);
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PAGE_TO_PAGE);
	}
}
