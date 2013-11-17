package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public abstract class ActivityBase extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= -9162254814199461867L;

	@Expose
	public String				trigger;										// create, watch
	@Expose
	public Action				action;

	/* client only */
	public Intent				intent;
	public String				title;
	public String				subtitle;
	public String				description;
	public Photo				photoBy;
	public Photo				photoOne;

	public ActivityBase() {}

	public static ActivityBase setPropertiesFromMap(ActivityBase base, Map map, Boolean nameMapping) {
		/*
		 * Need to include any properties that need to survive encode/decoded between activities.
		 */
		base.trigger = (String) map.get("trigger");

		if (map.get("action") != null) {
			base.action = Action.setPropertiesFromMap(new Action(), (HashMap<String, Object>) map.get("action"), nameMapping);
		}

		return base;
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
}