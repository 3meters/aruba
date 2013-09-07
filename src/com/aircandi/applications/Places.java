package com.aircandi.applications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.aircandi.Constants;
import com.aircandi.R;
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

	public static void view(Context context, String entityId) {
		IntentBuilder intentBuilder = new IntentBuilder(context, PlaceForm.class).setEntityId(entityId);
		context.startActivity(intentBuilder.create());
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PageToPage);
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
				.setListNewEnabled(true);

		context.startActivity(intentBuilder.create());
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PageToPage);
	}

	public static Intent viewForGetIntent(Context context, String entityId, String linkType, Direction direction) {
		if (direction == null) {
			direction = Direction.in;
		}
		IntentBuilder intentBuilder = new IntentBuilder(context, EntityList.class)
				.setEntityId(entityId)
				.setListLinkType(linkType)
				.setListLinkDirection(direction.name())
				.setListLinkSchema(Constants.SCHEMA_ENTITY_PLACE)
				.setListItemResId(R.layout.temp_listitem_entity)
				.setListNewEnabled(true);

		return intentBuilder.create();
	}

	@SuppressWarnings("ucd")
	public static void edit(Context context, Entity entity) {
		IntentBuilder intentBuilder = new IntentBuilder(context, PlaceEdit.class).setEntity(entity);
		((Activity) context).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_EDIT);
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PageToPage);
	}

	@SuppressWarnings("ucd")
	public static void insert(Context context) {
		IntentBuilder intentBuilder = new IntentBuilder(context, PlaceEdit.class).setEntitySchema(Constants.SCHEMA_ENTITY_PLACE);
		((Activity) context).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_INSERT);
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PageToPage);
	}
}