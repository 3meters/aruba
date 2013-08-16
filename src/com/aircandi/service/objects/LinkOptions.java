package com.aircandi.service.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.components.Maps;
import com.aircandi.service.Expose;
import com.aircandi.service.objects.Link.Direction;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class LinkOptions extends ServiceObject {

	private static final long	serialVersionUID	= -274203160211174564L;

	@Expose
	public Map					loadSort;
	@Expose
	public Map					loadWhere;
	@Expose
	public Boolean				shortcuts;
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

		if (defaultType == DefaultType.NoLinks) {
			return null;
		}
		else {
			User user = Aircandi.getInstance().getUser();
			Number limit = ProxiConstants.LIMIT_CHILD_ENTITIES;
			LinkOptions linkOptions = new LinkOptions().setActive(new ArrayList<LinkSettings>());

			if (defaultType == DefaultType.LinksForPlace) {
				linkOptions.shortcuts = true;
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_PROXIMITY, true, false, true, limit));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_APPLINK, true, false, true, limit));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_COMMENT, false, false, true));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_PICTURE, true, false, true, 1)); // just one so we can preview
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CANDIGRAM, true, false, true, 1)); // just one so we can preview
				if (user != null) {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, true, false, true, limit, Maps.asMap("_from", user.id)));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, true, false, true, limit, Maps.asMap("_from", user.id)));
				}
				else {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, false, false, true, limit));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, false, false, true, limit));
				}
			}
			else if (defaultType == DefaultType.LinksForProximity) {
				linkOptions = getDefault(DefaultType.LinksForPlace);
			}
			else if (defaultType == DefaultType.LinksForCandigram) {
				linkOptions.shortcuts = true;
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_APPLINK, true, false, true, limit));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_COMMENT, false, false, true));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_PICTURE, true, false, true, 1)); // just one so we can preview
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CANDIGRAM, true, false, true, limit).setDirection(Direction.out));
				if (user != null) {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, true, false, true, limit, Maps.asMap("_from", user.id)));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, true, false, true, limit, Maps.asMap("_from", user.id)));
				}
				else {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, false, false, true, limit));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, false, false, true, limit));
				}
			}
			else if (defaultType == DefaultType.LinksForPost) {
				linkOptions.shortcuts = true;
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_APPLINK, true, false, true, limit));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_COMMENT, false, false, true));
				if (user != null) {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, true, false, true, limit, Maps.asMap("_from", user.id)));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, true, false, true, limit, Maps.asMap("_from", user.id)));
				}
				else {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, false, false, true, limit));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, false, false, true, limit));
				}
			}
			else if (defaultType == DefaultType.LinksForUser) {
				linkOptions.shortcuts = true;
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CREATE, true, false, true, limit).setDirection(Direction.out));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, false, false, true, limit).setDirection(Direction.both));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, true, false, true, limit).setDirection(Direction.both));
			}
			return linkOptions;
		}
	}

	public enum DefaultType {
		LinksForProximity,
		LinksForPlace,
		LinksForCandigram,
		LinksForPost,
		LinksForUser,
		NoLinks,
	}
}