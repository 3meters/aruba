package com.aircandi.service.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.res.Resources;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
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

		if (linkProfile == LinkProfile.NO_LINKS) {
			return null;
		}
		else {
			User user = Aircandi.getInstance().getUser();
			LinkOptions linkOptions = new LinkOptions().setActive(new ArrayList<LinkSettings>());
			linkOptions.shortcuts = true;
			Resources resources = Aircandi.applicationContext.getResources();
			Number limitDefault = resources.getInteger(R.integer.limit_links_default);
			Number limitCreate = resources.getInteger(R.integer.limit_links_create_default);
			Number limitWatch = resources.getInteger(R.integer.limit_links_watch_default);
			Number limitLike = resources.getInteger(R.integer.limit_links_like_default);

			if (linkProfile == LinkProfile.LINKS_FOR_PLACE || linkProfile == LinkProfile.LINKS_FOR_PROXIMITY) {
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_BEACON, true, true, false, limitDefault));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, true, true, false, limitDefault));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_COMMENT, false, true, false));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PICTURE, true, true, false, limitDefault)); // for preview and photo picker
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_CANDIGRAM, true, true, false, 1)); // just one so we can preview
				if (user != null) {
					linkOptions.getActive().add(
							new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, true, true, false, resources
									.getInteger(R.integer.limit_links_like_default), Maps.asMap("_from", user.id)));
					linkOptions.getActive().add(
							new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, false, resources
									.getInteger(R.integer.limit_links_watch_default), Maps.asMap("_from", user.id)));
				}
				else {
					linkOptions.getActive().add(
							new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, false, true, false, resources
									.getInteger(R.integer.limit_links_like_default)));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, false, true, false, limitWatch));
				}
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_CANDIGRAM) {
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, true, true, false, limitDefault));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_COMMENT, false, true));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PICTURE, true, true, false, 1)); // just one so we can preview
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, true, true, true, limitDefault)
								.setDirection(Direction.out));
				if (user != null) {
					linkOptions.getActive()
							.add(new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, true, true, false, limitLike, Maps.asMap("_from",
									user.id)));
					linkOptions.getActive().add(
							new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, false, limitWatch, Maps.asMap("_from",
									user.id)));
				}
				else {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, false, true, false, limitLike));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, false, true, false, limitWatch));
				}
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_PICTURE) {
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, true, true, false, limitDefault));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_COMMENT, false, true));
				if (user != null) {
					linkOptions.getActive()
							.add(new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, true, true, false, limitLike, Maps.asMap("_from",
									user.id)));
					linkOptions.getActive().add(
							new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, false, limitWatch, Maps.asMap("_from",
									user.id)));
				}
				else {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, false, true, false, limitLike));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, false, true, false, limitWatch));
				}
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_USER) {
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_PLACE, false, true, false, limitLike).setDirection(Direction.both));

				linkOptions.getActive()
						.add(new LinkSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PLACE, true, true, false, limitCreate)
								.setDirection(Direction.out));
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PICTURE, true, true, false, limitCreate)
								.setDirection(Direction.out));
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_CANDIGRAM, true, true, false, limitCreate)
								.setDirection(Direction.out));

				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, true, true, false, limitWatch).setDirection(Direction.out));
				linkOptions.getActive()
						.add(new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PICTURE, true, true, false, limitWatch)
								.setDirection(Direction.out));
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_CANDIGRAM, true, true, false, limitWatch)
								.setDirection(Direction.out));
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, false, limitWatch).setDirection(Direction.out));
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
		LINKS_FOR_PROXIMITY,
		LINKS_FOR_PLACE,
		LINKS_FOR_CANDIGRAM,
		LINKS_FOR_PICTURE,
		LINKS_FOR_USER,
		NO_LINKS,
	}
}