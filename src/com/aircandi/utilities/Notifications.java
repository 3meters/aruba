package com.aircandi.utilities;

import com.aircandi.Constants;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.service.objects.AirNotification.ActionType;
import com.aircandi.service.objects.AirNotification.NotificationType;
import com.aircandi.service.objects.Photo;

public class Notifications {

	/*
	 * Own
	 * 
	 * [George Snelling] commented on your [candigram] at [Taco Del Mar].
	 * [George Snelling] commented on your [picture] at [Taco Del Mar].
	 * [George Snelling] commented on your [place] [Taco Del Mar].
	 * 
	 * [George Snelling] added a [picture] to your [place] [Taco Del Mar].
	 * [George Snelling] added a [picture] to your [candigram] at [Taco Del Mar].
	 * [George Snelling] added a [candigram] to your [place] [Taco Del Mar].
	 * 
	 * [George Snelling] kicked a [candigram] to your [place] [Taco Del Mar].
	 * 
	 * Watching
	 * 
	 * [George Snelling] commented on a [candigram] you are watching.
	 * [George Snelling] commented on a [picture] you are watching.
	 * [George Snelling] commented on a [place] you are watching.
	 * 
	 * [George Snelling] added a [picture] to a [place] you are watching.
	 * [George Snelling] added a [picture] to a [candigram] you are watching.
	 * [George Snelling] added a [candigram] to a [place] you are watching.
	 * 
	 * [George Snelling] kicked a [candigram] to a [place] you are watching.
	 * 
	 * Nearby
	 * 
	 * [George Snelling] commented on a [candigram] nearby.
	 * [George Snelling] commented on a [picture] nearby.
	 * [George Snelling] commented on a [place] nearby.
	 * 
	 * [George Snelling] added a [picture] to a [place] nearby.
	 * [George Snelling] added a [picture] to a [candigram] nearby.
	 * [George Snelling] added a [candigram] to a [place] nearby.
	 * 
	 * [George Snelling] kicked a [candigram] to a [place] nearby.
	 * 
	 * Move
	 * 
	 * A candigram has traveled to a place nearby
	 * A candigram has traveled to your place Taco Del Mar.
	 * A candigram has traveled to a place you are watching.
	 */

	public static void decorate(AirNotification notification) {
		notification.title = title(notification);
		notification.subtitle = subtitle(notification);
		notification.description = description(notification);
		notification.photoFrom = photoFrom(notification);
		notification.photoTo = photoTo(notification);
	}

	public static String schemaFixup(String schema) {
		if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
			return Constants.SCHEMA_REMAP_PICTURE;
		}
		return schema;
	}

	public static String subtitle(AirNotification notification) {
		if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)
				&& notification.entity.type.equals(Constants.TYPE_APP_TOUR)
				&& notification.action.equals(ActionType.MOVE)) {

			if (notification.type.equals(NotificationType.NEARBY)) {
				return "A candigram has traveled to a place nearby";
			}
			else if (notification.type.equals(NotificationType.WATCH)) {
				return "A candigram has traveled to a place you are watching";
			}
			else if (notification.type.equals(NotificationType.OWN)) {
				return String.format("A candigram has traveled to a place %1$s of yours.", notification.toEntity.name);
			}
		}
		if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)
				&& notification.entity.type.equals(Constants.TYPE_APP_BOUNCE)
				&& notification.action.equals(ActionType.MOVE)) {

			if (notification.type.equals(NotificationType.NEARBY)) {
				return String.format("kicked a %1$s to a %2$s nearby.", schemaFixup(notification.entity.schema), schemaFixup(notification.toEntity.schema));
			}
			else if (notification.type.equals(NotificationType.WATCH)) {
				return String.format("kicked a %1$s to a %2$s you are watching.", schemaFixup(notification.entity.schema),
						schemaFixup(notification.toEntity.schema));
			}
			else if (notification.type.equals(NotificationType.WATCH_USER)) {
				return String.format("kicked a %1$s to a %2$s.", schemaFixup(notification.entity.schema), schemaFixup(notification.toEntity.schema));
			}
			else if (notification.type.equals(NotificationType.OWN)) {
				return String.format("kicked a %1$s to a %2$s of yours.", schemaFixup(notification.entity.schema), schemaFixup(notification.toEntity.schema));
			}
		}
		else if (notification.action.equals(ActionType.INSERT)
				&& notification.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {

			if (notification.type.equals(NotificationType.NEARBY)) {
				return String.format("commented on a %1$s nearby.", schemaFixup(notification.toEntity.schema));
			}
			else if (notification.type.equals(NotificationType.WATCH)) {
				return String.format("commented on a %1$s you are watching.", schemaFixup(notification.toEntity.schema));
			}
			else if (notification.type.equals(NotificationType.WATCH_USER)) {
				return String.format("commented on a %1$s.", schemaFixup(notification.toEntity.schema));
			}
			else if (notification.type.equals(NotificationType.OWN)) {
				return String.format("commented on a %1$s of yours.", schemaFixup(notification.toEntity.schema));
			}
		}
		else if (notification.action.equals(ActionType.INSERT)
				&& notification.entity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)
				|| notification.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {

			if (notification.type.equals(NotificationType.NEARBY)) {
				return String.format("added a %1$s to a %2$s nearby.", schemaFixup(notification.entity.schema), schemaFixup(notification.toEntity.schema));
			}
			else if (notification.type.equals(NotificationType.WATCH)) {
				return String.format("added a %1$s to a %2$s you are watching.", schemaFixup(notification.entity.schema),
						schemaFixup(notification.toEntity.schema));
			}
			else if (notification.type.equals(NotificationType.WATCH_USER)) {
				return String.format("added a %1$s to a %2$s.", schemaFixup(notification.entity.schema), schemaFixup(notification.toEntity.schema));
			}
			else if (notification.type.equals(NotificationType.OWN)) {
				return String.format("added a %1$s to a %2$s of yours.", schemaFixup(notification.entity.schema), schemaFixup(notification.toEntity.schema));
			}
		}
		return null;
	}

	public static String title(AirNotification notification) {
		if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)
				&& notification.entity.type.equals(Constants.TYPE_APP_TOUR)
				&& notification.action.equals(ActionType.MOVE)) {
			return notification.entity.name;
		}
		else if (notification.user == null) {
			return notification.entity.name;
		}
		else {
			return notification.user.name;
		}
	}

	public static Photo photoTo(AirNotification notification) {
		if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)
				&& notification.entity.type.equals(Constants.TYPE_APP_TOUR)
				&& notification.action.equals(ActionType.MOVE)) {
			return notification.toEntity.getPhoto();
		}
		else if (!notification.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
			return notification.entity.getPhoto();
		}
		return null;
	}

	public static Photo photoFrom(AirNotification notification) {
		if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)
				&& notification.entity.type.equals(Constants.TYPE_APP_TOUR)
				&& notification.action.equals(ActionType.MOVE)) {
			return notification.entity.getPhoto();
		}
		else if (notification.user == null) {
			return notification.entity.getPhoto();
		}
		else {
			return notification.user.getPhoto();
		}
	}

	public static String description(AirNotification notification) {
		if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
			return "\"" + notification.entity.description + "\"";
		}
		else {
			return notification.entity.description;
		}
	}
}
