package com.aircandi.service.objects;

import java.util.ArrayList;
import java.util.List;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;

/**
 * @author Jayma
 */
public class LinkOptions {

	public String				loadSort;
	public String				loadWhere;
	public List<LinkSettings>	active;

	public LinkOptions(String loadSort, String loadWhere, List<LinkSettings> active) {
		this.loadSort = loadSort;
		this.loadWhere = loadWhere;
		this.active = active;
	}

	public LinkOptions() {}

	public String getLoadSort() {
		return loadSort;
	}

	public LinkOptions setLoadSort(String loadSort) {
		this.loadSort = loadSort;
		return this;
	}

	public String getLoadWhere() {
		return loadWhere;
	}

	public LinkOptions setLoadWhere(String loadWhere) {
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

	public static LinkOptions getDefault(DefaultType defaultType) {
		String userId = Aircandi.getInstance().getUser().id;
		Number limit = ProxiConstants.RADAR_CHILDENTITY_LIMIT;
		LinkOptions linkOptions = new LinkOptions().setActive(new ArrayList<LinkSettings>());
		if (defaultType == DefaultType.PlaceEntities) {
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_POST, true, false, true, limit));
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_APPLINK, true, false, true, limit));
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_COMMENT, false, false, true));
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, false, true, true, limit, "object:{\"_from\":\"" + userId + "\"}"));
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, false, true, true, limit, "object:{\"_from\":\"" + userId + "\"}"));
		}
		else if (defaultType == DefaultType.BeaconEntities) {
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_PROXIMITY, true, true, true, ProxiConstants.RADAR_PLACES_LIMIT));
		}
		return linkOptions;
	}

	public enum DefaultType {
		BeaconEntities,
		PlaceEntities,
	}
}