package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Activity extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672244719448L;

	@Expose
	public String				trigger;									// create, watch
	@Expose
	public Action				action;
	@Expose
	public String				summary;
	@Expose
	public Boolean				grouped;
	@Expose
	public Number				sortDate;
	@Expose
	public Number				activityDate;
	@Expose
	public Number				sentDate;

	/* client only */
	public Intent				intent;
	public String				title;
	public String				subtitle;
	public String				description;
	public Photo				photoBy;
	public Photo				photoOne;

	public Activity() {}

	public static Activity setPropertiesFromMap(Activity activity, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		activity.trigger = (String) map.get("trigger");
		activity.summary = (String) map.get("summary");
		activity.grouped = (Boolean) map.get("grouped");
		activity.sentDate = (Number) map.get("sentDate");
		activity.sortDate = (Number) map.get("sortDate");
		activity.activityDate = (Number) map.get("activityDate");

		if (map.get("action") != null) {
			activity.action = Action.setPropertiesFromMap(new Action(), (HashMap<String, Object>) map.get("action"), nameMapping);
		}

		return activity;
	}
	
	public String getTriggerCategory() {
		if (this.trigger.contains("nearby")) return TriggerType.NEARBY;
		if (this.trigger.contains("watch")) return TriggerType.WATCH;
		if (this.trigger.contains("own")) return TriggerType.OWN;
		return null;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public static class TriggerType {
		public static String	NEARBY		= "nearby";	// sent because this user is nearby
		public static String	WATCH		= "watch";		// sent because this user is watching the entity
		public static String	WATCH_TO	= "watch_to";	// sent because this user is watching the 'to' entity
		public static String	WATCH_FROM	= "watch_from";		// sent because this user is watching the 'from' entity
		public static String	WATCH_USER	= "watch_user";		// sent because this user is watching another user
		public static String	OWN			= "own";		// sent because this user is the owner of the entity
		public static String	OWN_TO		= "own_to";	// sent because this user is the owner of the 'to' entity
		public static String	OWN_FROM	= "own_from";	// sent because this user is the owner of the 'from' entity
	}

	public static class SortBySortDate implements Comparator<Activity> {

		@Override
		public int compare(Activity object1, Activity object2) {
			if (object1.sortDate == null || object2.sortDate == null) {
				return 0;
			}
			else {
				if (object1.sortDate.longValue() < object2.sortDate.longValue()) {
					return 1;
				}
				else if (object1.sortDate.longValue() == object2.sortDate.longValue()) {
					return 0;
				}
				return -1;
			}
		}
	}
}