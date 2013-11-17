package com.aircandi.utilities;

import android.text.TextUtils;

import com.aircandi.Constants;
import com.aircandi.components.Logger;
import com.aircandi.service.objects.Action.EventCategory;
import com.aircandi.service.objects.ActivityBase;
import com.aircandi.service.objects.ActivityBase.TriggerType;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Photo;

public class Activities {

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

	public static void decorate(ActivityBase activity) {
		activity.title = title(activity);
		activity.subtitle = subtitle(activity);
		if (activity.subtitle == null) {
			Logger.v(null, "missing subtitle");
		}
		activity.description = description(activity);
		activity.photoBy = photoBy(activity);
		activity.photoOne = photoOne(activity);
	}

	public static String subtitle(ActivityBase activity) {

		if (activity.action.getEventCategory().equals(EventCategory.MOVE)) {
			if (activity.action.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {

				if (activity.action.entity.type.equals(Constants.TYPE_APP_BOUNCE)) {

					if (activity.trigger.equals(TriggerType.NEARBY)) {
						return "kicked a candigram to a place nearby.";
					}
					else if (activity.trigger.equals(TriggerType.WATCH)) {
						return "kicked a candigram you\'re watching to a new place.";
					}
					else if (activity.trigger.equals(TriggerType.WATCH_TO)) {
						return "kicked a candigram to a place you\'re watching.";
					}
					else if (activity.trigger.equals(TriggerType.WATCH_USER)) {
						return "kicked a candigram to a new place.";
					}
					else if (activity.trigger.equals(TriggerType.OWN)) {
						return "kicked a candigram you started to a new place.";
					}
					else if (activity.trigger.equals(TriggerType.OWN_TO)) {
						return "kicked a candigram to a place of yours.";
					}
				}
				else if (activity.action.entity.type.equals(Constants.TYPE_APP_TOUR)) {

					if (activity.trigger.equals(TriggerType.NEARBY)) {
						return "A candigram has traveled to a place nearby";
					}
					else if (activity.trigger.equals(TriggerType.WATCH)) {
						return "A candigram you\'re watching has traveled to a new place";
					}
					else if (activity.trigger.equals(TriggerType.WATCH_TO)) {
						return "A candigram has traveled to a place you\'re watching";
					}
					else if (activity.trigger.equals(TriggerType.OWN)) {
						return "A candigram of yours has traveled to a new place.";
					}
					else if (activity.trigger.equals(TriggerType.OWN_TO)) {
						return "A candigram has traveled to a place of yours";
					}
				}
			}
		}
		else if (activity.action.getEventCategory().equals(EventCategory.EXPAND)) {
			if (activity.action.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {

				if (activity.action.entity.type.equals(Constants.TYPE_APP_EXPAND)) {

					if (activity.trigger.equals(TriggerType.NEARBY)) {
						return "repeated a candigram to a place nearby.";
					}
					else if (activity.trigger.equals(TriggerType.WATCH)) {
						return "repeated a candigram you\'re watching to a new place.";
					}
					else if (activity.trigger.equals(TriggerType.WATCH_TO)) {
						return "repeated a candigram to a place you\'re watching.";
					}
					else if (activity.trigger.equals(TriggerType.WATCH_USER)) {
						return "repeated a candigram to a new place.";
					}
					else if (activity.trigger.equals(TriggerType.OWN)) {
						return "repeated a candigram you started to a new place.";
					}
					else if (activity.trigger.equals(TriggerType.OWN_TO)) {
						return "repeated a candigram to a place of yours.";
					}
				}
			}
		}
		else if (activity.action.getEventCategory().equals(EventCategory.INSERT)) {
			if (activity.action.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {

				if (activity.trigger.equals(TriggerType.NEARBY)) {
					return String.format("commented on a %1$s nearby.", activity.action.toEntity.getSchemaMapped());
				}
				else if (activity.trigger.equals(TriggerType.WATCH_TO)) {
					return String.format("commented on a %1$s you are watching.", activity.action.toEntity.getSchemaMapped());
				}
				else if (activity.trigger.equals(TriggerType.WATCH_USER)) {
					return String.format("commented on a %1$s.", activity.action.toEntity.getSchemaMapped());
				}
				else if (activity.trigger.equals(TriggerType.OWN_TO)) {
					return String.format("commented on a %1$s of yours.", activity.action.toEntity.getSchemaMapped());
				}
			}
			else if (activity.action.entity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {

				if (activity.trigger.equals(TriggerType.NEARBY)) {
					return String
							.format("added a %1$s to a %2$s nearby.", activity.action.entity.getSchemaMapped(), activity.action.toEntity.getSchemaMapped());
				}
				else if (activity.trigger.equals(TriggerType.WATCH)) {
					return String.format("added a %1$s you are watching.", activity.action.entity.getSchemaMapped());
				}
				else if (activity.trigger.equals(TriggerType.WATCH_TO)) {
					return String.format("added a %1$s to a %2$s you are watching.", activity.action.entity.getSchemaMapped(),
							activity.action.toEntity.getSchemaMapped());
				}
				else if (activity.trigger.equals(TriggerType.WATCH_USER)) {
					return String.format("added a %1$s to a %2$s.", activity.action.entity.getSchemaMapped(), activity.action.toEntity.getSchemaMapped());
				}
				else if (activity.trigger.equals(TriggerType.OWN_TO)) {
					return String
							.format("added a %1$s to a %2$s of yours.", activity.action.entity.getSchemaMapped(), activity.action.toEntity.getSchemaMapped());
				}
			}
			else if (activity.action.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {

				if (activity.trigger.equals(TriggerType.NEARBY)) {
					return String
							.format("started a %1$s at a %2$s nearby.", activity.action.entity.getSchemaMapped(), activity.action.toEntity.getSchemaMapped());
				}
				else if (activity.trigger.equals(TriggerType.WATCH)) {
					return String.format("started a %1$s you are watching.", activity.action.entity.getSchemaMapped());
				}
				else if (activity.trigger.equals(TriggerType.WATCH_TO)) {
					return String.format("started a %1$s at a %2$s you are watching.", activity.action.entity.getSchemaMapped(),
							activity.action.toEntity.getSchemaMapped());
				}
				else if (activity.trigger.equals(TriggerType.WATCH_USER)) {
					return String.format("started a %1$s at a %2$s.", activity.action.entity.getSchemaMapped(), activity.action.toEntity.getSchemaMapped());
				}
				else if (activity.trigger.equals(TriggerType.OWN_TO)) {
					return String
							.format("started a %1$s at a %2$s of yours.", activity.action.entity.getSchemaMapped(), activity.action.toEntity.getSchemaMapped());
				}
			}
			else if (activity.action.entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {

				if (activity.trigger.equals(TriggerType.NEARBY)) {
					return "created a new place nearby.";
				}
				else if (activity.trigger.equals(TriggerType.WATCH_USER)) {
					return "created a new place.";
				}
				else if (activity.trigger.equals(TriggerType.WATCH)) {
					return "created a place you are watching.";
				}
			}
		}
		Logger.w(Activities.class, "activity missing subtitle");
		return null;
	}

	public static String title(ActivityBase activity) {
		if (activity.action.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)
				&& activity.action.entity.type.equals(Constants.TYPE_APP_TOUR)
				&& activity.action.getEventCategory().equals(EventCategory.MOVE)) {
			return activity.action.entity.name;
		}
		else if (activity.action.user == null) {
			return activity.action.entity.name;
		}
		else {
			return activity.action.user.name;
		}
	}

	public static Photo photoBy(ActivityBase activity) {
		Photo photo = null;
		if (activity.action.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)
				&& activity.action.entity.type.equals(Constants.TYPE_APP_TOUR)
				&& activity.action.getEventCategory().equals(EventCategory.MOVE)) {
			photo = activity.action.entity.getPhoto();
			photo.name = activity.action.entity.name;
			photo.shortcut = activity.action.entity.getShortcut();
		}
		else if (activity.action.user == null) {
			photo = Entity.getDefaultPhoto(Constants.SCHEMA_ENTITY_USER, null);
		}
		else {
			photo = activity.action.user.getPhoto();
			photo.name = activity.action.user.name;
			photo.shortcut = activity.action.user.getShortcut();
		}
		return photo;
	}

	public static Photo photoOne(ActivityBase activity) {
		Photo photo = null;

		if (activity.action.entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)
				&& activity.action.entity.type.equals(Constants.TYPE_APP_TOUR)
				&& activity.action.getEventCategory().equals(EventCategory.MOVE)) {
			photo = activity.action.toEntity.getPhoto();
			photo.name = TextUtils.isEmpty(activity.action.toEntity.name)
					? activity.action.toEntity.getSchemaMapped()
					: activity.action.toEntity.name;
			photo.shortcut = activity.action.toEntity.getShortcut();
		}
		else if (activity.action.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
			photo = activity.action.toEntity.getPhoto();
			photo.name = TextUtils.isEmpty(activity.action.toEntity.name)
					? activity.action.toEntity.getSchemaMapped()
					: activity.action.toEntity.name;
			photo.shortcut = activity.action.toEntity.getShortcut();
		}
		else {
			photo = activity.action.entity.getPhoto();
			photo.name = TextUtils.isEmpty(activity.action.entity.name)
					? activity.action.entity.getSchemaMapped()
					: activity.action.entity.name;
			photo.shortcut = activity.action.entity.getShortcut();
		}
		return photo;
	}

	public static Photo photoTwo(ActivityBase activity) {
		Photo photo = activity.action.fromEntity.getPhoto();
		photo.name = activity.action.fromEntity.name;
		photo.shortcut = activity.action.fromEntity.getShortcut();
		return photo;
	}

	public static String description(ActivityBase activity) {
		if (activity.action.entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
			return "\"" + activity.action.entity.description + "\"";
		}
		else {
			return activity.action.entity.description;
		}
	}
}
