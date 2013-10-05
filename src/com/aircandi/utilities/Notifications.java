package com.aircandi.utilities;

import com.aircandi.Constants;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.service.objects.AirNotification.ActionType;
import com.aircandi.service.objects.AirNotification.NotificationType;
import com.aircandi.service.objects.Entity;
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
		notification.photoBy = photoBy(notification);
		notification.photoOne = photoOne(notification);
	}

	public static String subtitle(AirNotification notification) {
		if (notification.action.equals(ActionType.MOVE)) {
			if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM) && notification.typeTargetId != null) {

				if (notification.entity.type.equals(Constants.TYPE_APP_BOUNCE)) {

					if (notification.type.equals(NotificationType.NEARBY)) {
						return "kicked a candigram to a place nearby.";
					}
					else if (notification.type.equals(NotificationType.WATCH)) {
						if (notification.typeTargetId.equals(notification.entity.id)) {
							return "kicked a candigram you\'re watching to a new place.";
						}
						else if (notification.toEntity != null && notification.typeTargetId.equals(notification.toEntity.id)) {
							return "kicked a candigram to a place you\'re watching.";
						}
						else if (notification.fromEntity != null && notification.typeTargetId.equals(notification.fromEntity.id)) {
							return "kicked a candigram from a place you\'re watching.";
						}
					}
					else if (notification.type.equals(NotificationType.WATCH_USER)) {
						return "kicked a candigram to a new place.";
					}
					else if (notification.type.equals(NotificationType.OWN)) {
						if (notification.typeTargetId.equals(notification.entity.id)) {
							return "kicked a candigram you started to a new place.";
						}
						else if (notification.toEntity != null && notification.typeTargetId.equals(notification.toEntity.id)) {
							return "kicked a candigram to a place of yours.";
						}
						else if (notification.fromEntity != null && notification.typeTargetId.equals(notification.fromEntity.id)) {
							return "kicked a candigram from a place of yours.";
						}
					}
				}
				else if (notification.entity.type.equals(Constants.TYPE_APP_TOUR)) {

					if (notification.type.equals(NotificationType.NEARBY)) {
						return "A candigram has traveled to a place nearby";
					}
					else if (notification.type.equals(NotificationType.WATCH)) {

						if (notification.typeTargetId.equals(notification.entity.id)) {
							return "A candigram you\'re watching has traveled to a new place";
						}
						else if (notification.toEntity != null && notification.typeTargetId.equals(notification.toEntity.id)) {
							return "A candigram has traveled to a place you\'re watching";
						}
						else if (notification.fromEntity != null && notification.typeTargetId.equals(notification.fromEntity.id)) {
							return "A candigram has left a place you\'re watching";
						}
					}
					else if (notification.type.equals(NotificationType.OWN)) {
						if (notification.typeTargetId.equals(notification.entity.id)) {
							return "A candigram of yours has traveled to a new place.";
						}
						else if (notification.toEntity != null && notification.typeTargetId.equals(notification.toEntity.id)) {
							return "A candigram has traveled to a place of yours";
						}
						else if (notification.fromEntity != null && notification.typeTargetId.equals(notification.fromEntity.id)) {
							return "A candigram has left a place of yours";
						}
					}
				}
			}
		}
		else if (notification.action.equals(ActionType.INSERT)) {
			if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {

				if (notification.type.equals(NotificationType.NEARBY)) {
					return String.format("commented on a %1$s nearby.", notification.toEntity.getSchemaMapped());
				}
				else if (notification.type.equals(NotificationType.WATCH)) {
					return String.format("commented on a %1$s you are watching.", notification.toEntity.getSchemaMapped());
				}
				else if (notification.type.equals(NotificationType.WATCH_USER)) {
					return String.format("commented on a %1$s.", notification.toEntity.getSchemaMapped());
				}
				else if (notification.type.equals(NotificationType.OWN)) {
					return String.format("commented on a %1$s of yours.", notification.toEntity.getSchemaMapped());
				}
			}
			else if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {

				if (notification.type.equals(NotificationType.NEARBY)) {
					return String.format("added a %1$s to a %2$s nearby.", notification.entity.getSchemaMapped(), notification.toEntity.getSchemaMapped());
				}
				else if (notification.type.equals(NotificationType.WATCH)) {
					return String.format("added a %1$s to a %2$s you are watching.", notification.entity.getSchemaMapped(),
							notification.toEntity.getSchemaMapped());
				}
				else if (notification.type.equals(NotificationType.WATCH_USER)) {
					return String.format("added a %1$s to a %2$s.", notification.entity.getSchemaMapped(), notification.toEntity.getSchemaMapped());
				}
				else if (notification.type.equals(NotificationType.OWN)) {
					return String
							.format("added a %1$s to a %2$s of yours.", notification.entity.getSchemaMapped(), notification.toEntity.getSchemaMapped());
				}
			}
			else if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {

				if (notification.type.equals(NotificationType.NEARBY)) {
					return String
							.format("started a %1$s at a %2$s nearby.", notification.entity.getSchemaMapped(), notification.toEntity.getSchemaMapped());
				}
				else if (notification.type.equals(NotificationType.WATCH)) {
					return String.format("started a %1$s at a %2$s you are watching.", notification.entity.getSchemaMapped(),
							notification.toEntity.getSchemaMapped());
				}
				else if (notification.type.equals(NotificationType.WATCH_USER)) {
					return String.format("started a %1$s at a %2$s.", notification.entity.getSchemaMapped(), notification.toEntity.getSchemaMapped());
				}
				else if (notification.type.equals(NotificationType.OWN)) {
					return String
							.format("started a %1$s at a %2$s of yours.", notification.entity.getSchemaMapped(), notification.toEntity.getSchemaMapped());
				}
			}
			else if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {

				if (notification.type.equals(NotificationType.NEARBY)) {
					return "marked a new place nearby.";
				}
				else if (notification.type.equals(NotificationType.WATCH_USER)) {
					return "marked a new place.";
				}
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

	public static Photo photoBy(AirNotification notification) {
		Photo photo = null;
		if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)
				&& notification.entity.type.equals(Constants.TYPE_APP_TOUR)
				&& notification.action.equals(ActionType.MOVE)) {
			photo = notification.entity.getPhoto();
			photo.name = notification.entity.name;
			photo.shortcut = notification.entity.getShortcut();
		}
		else if (notification.user == null) {
			photo = Entity.getDefaultPhoto(Constants.SCHEMA_ENTITY_USER, null);
		}
		else {
			photo = notification.user.getPhoto();
			photo.name = notification.user.name;
			photo.shortcut = notification.user.getShortcut();
		}
		return photo;
	}

	public static Photo photoOne(AirNotification notification) {
		Photo photo = null;

		if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)
				&& notification.entity.type.equals(Constants.TYPE_APP_TOUR)
				&& notification.action.equals(ActionType.MOVE)) {
			photo = notification.toEntity.getPhoto();
			photo.name = notification.toEntity.getSchemaMapped();
			photo.shortcut = notification.toEntity.getShortcut();
		}
		else if (notification.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
			photo = notification.toEntity.getPhoto();
			photo.name = notification.toEntity.getSchemaMapped();
			photo.shortcut = notification.toEntity.getShortcut();
		}
		else {
			photo = notification.entity.getPhoto();
			photo.name = notification.entity.getSchemaMapped();
			photo.shortcut = notification.entity.getShortcut();
		}
		return photo;
	}

	public static Photo photoTwo(AirNotification notification) {
		Photo photo = notification.fromEntity.getPhoto();
		photo.name = notification.fromEntity.name;
		photo.shortcut = notification.fromEntity.getShortcut();
		return photo;
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
