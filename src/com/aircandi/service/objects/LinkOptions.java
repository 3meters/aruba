package com.aircandi.service.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.components.Maps;
import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class LinkOptions extends ServiceObject {

	private static final long	serialVersionUID	= -274203160211174564L;

	@Expose
	public Map					loadSort;
	@Expose
	public Map					loadWhere;
	@Expose
	private Boolean				shortcuts;
	@Expose
	public List<LinkSettings>	active;

	public LinkOptions(Map loadSort, Map loadWhere, Boolean shortcuts, List<LinkSettings> active) {
		this.loadSort = loadSort;
		this.loadWhere = loadWhere;
		this.setShortcuts(shortcuts);
		this.active = active;
	}

	public LinkOptions() {}

	public Map getLoadSort() {
		return loadSort;
	}

	public LinkOptions setLoadSort(Map loadSort) {
		this.loadSort = loadSort;
		return this;
	}

	public Map getLoadWhere() {
		return loadWhere;
	}

	public LinkOptions setLoadWhere(Map loadWhere) {
		this.loadWhere = loadWhere;
		return this;
	}

	public List<LinkSettings> getActive() {
		return active;
	}

	public LinkOptions setActive(List<LinkSettings> active) {
		this.active = active;
		return this;
	}

	public Boolean getShortcuts() {
		return shortcuts;
	}

	public LinkOptions setShortcuts(Boolean shortcuts) {
		this.shortcuts = shortcuts;
		return this;
	}

	public static LinkOptions getDefault(DefaultType defaultType) {

		User user = Aircandi.getInstance().getUser();
		Number limit = ProxiConstants.LIMIT_CHILD_ENTITIES;
		LinkOptions linkOptions = new LinkOptions()
				.setActive(new ArrayList<LinkSettings>());

		if (defaultType == DefaultType.PlaceEntities) {
			linkOptions.setShortcuts(true);
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_PROXIMITY, true, false, true, limit));
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_APPLINK, true, false, true, limit));
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_COMMENT, false, false, true));
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_POST, false, false, true, limit));
			if (user != null) {
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, true, false, true, limit, Maps.asMap("_from", user.id)));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, true, false, true, limit, Maps.asMap("_from", user.id)));
			}
			else {
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, false, false, true, limit));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, false, false, true, limit));
			}
		}
		else if (defaultType == DefaultType.BeaconEntities) {
			linkOptions.setShortcuts(false);
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_PROXIMITY, true, false, true, ProxiConstants.LIMIT_RADAR_PLACES));
		}
		else if (defaultType == DefaultType.UserEntities) {}
		else if (defaultType == DefaultType.UserWatchingEntities) {
			linkOptions.setShortcuts(true);
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, true, true, true, ProxiConstants.LIMIT_USER_WATCHING_ENTITIES));
		}

		return linkOptions;
	}

	public enum DefaultType {
		BeaconEntities,
		PlaceEntities,
		UserEntities,
		UserWatchingEntities
	}
}