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
	public Boolean				ignoreInactive;
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

	public static LinkOptions getDefault(LinkProfile linkProfile) {

		if (linkProfile == LinkProfile.NoLinks) {
			return null;
		}
		else {
			User user = Aircandi.getInstance().getUser();
			LinkOptions linkOptions = new LinkOptions().setActive(new ArrayList<LinkSettings>());
			linkOptions.shortcuts = true;

			if (linkProfile == LinkProfile.LinksForPlace || linkProfile == LinkProfile.LinksForProximity) {
				Number limit = ProxiConstants.LIMIT_LINKS_DEFAULT;
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_PROXIMITY, true, false, true, false, limit));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_APPLINK, true, false, true, false, limit));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_COMMENT, false, false, true, false));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_PICTURE, true, false, true, false, 1)); // just one so we can preview
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CANDIGRAM, true, false, true, false, 1)); // just one so we can preview
				if (user != null) {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, true, false, true, false, limit, Maps.asMap("_from", user.id)));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, true, false, true, false, limit, Maps.asMap("_from", user.id)));
				}
				else {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, false, false, true, false, limit));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, false, false, true, false, limit));
				}
			}
			else if (linkProfile == LinkProfile.LinksForCandigram) {
				Number limit = ProxiConstants.LIMIT_LINKS_DEFAULT;
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_APPLINK, true, false, true, false, limit));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_COMMENT, false, false, true));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_PICTURE, true, false, true, false, 1)); // just one so we can preview
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CANDIGRAM, true, false, true, true, limit).setDirection(Direction.out));
				if (user != null) {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, true, false, true, false, limit, Maps.asMap("_from", user.id)));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, true, false, true, false, limit, Maps.asMap("_from", user.id)));
				}
				else {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, false, false, true, false, limit));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, false, false, true, false, limit));
				}
			}
			else if (linkProfile == LinkProfile.LinksForPicture) {
				Number limit = ProxiConstants.LIMIT_LINKS_DEFAULT;
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_APPLINK, true, false, true, false, limit));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_COMMENT, false, false, true));
				if (user != null) {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, true, false, true, false, limit, Maps.asMap("_from", user.id)));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, true, false, true, false, limit, Maps.asMap("_from", user.id)));
				}
				else {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, false, false, true, false, limit));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, false, false, true, false, limit));
				}
			}
			else if (linkProfile == LinkProfile.LinksForUser) {
				Number limit = ProxiConstants.LIMIT_USER_OWNED_ENTITIES;
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CREATE, true, false, true, false, limit).setDirection(Direction.out));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, false, false, true, false, limit).setDirection(Direction.both));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, true, false, true, false, limit).setDirection(Direction.both));
			}
			return linkOptions;
		}
	}

	public Boolean getIgnoreInactive() {
		return ignoreInactive;
	}

	public LinkOptions setIgnoreInactive(Boolean ignoreInactive) {
		this.ignoreInactive = ignoreInactive;
		return this;
	}

	public enum LinkProfile {
		LinksForProximity,
		LinksForPlace,
		LinksForCandigram,
		LinksForPicture,
		LinksForUser,
		NoLinks,
	}
}