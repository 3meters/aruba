package com.aircandi.service.objects;

import java.util.ArrayList;
import java.util.List;

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
	public Boolean				shortcuts;
	@Expose
	public List<LinkSettings>	active;

	public LinkOptions(Boolean shortcuts, List<LinkSettings> active) {
		this.setShortcuts(shortcuts);
		this.active = active;
	}

	public LinkOptions() {}

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
			User currentUser = Aircandi.getInstance().getCurrentUser();
			LinkOptions linkOptions = new LinkOptions().setActive(new ArrayList<LinkSettings>());
			linkOptions.shortcuts = true;
			Resources resources = Aircandi.applicationContext.getResources();
			Number limitDefault = resources.getInteger(R.integer.limit_links_default);
			Number limitContent = resources.getInteger(R.integer.limit_links_content_default);
			Number limitProximity = resources.getInteger(R.integer.limit_links_proximity_default);
			Number limitCreate = resources.getInteger(R.integer.limit_links_create_default);
			Number limitWatch = resources.getInteger(R.integer.limit_links_watch_default);
			Number limitLike = resources.getInteger(R.integer.limit_links_like_default);
			Number limitApplinks = resources.getInteger(R.integer.limit_links_applinks_default);

			if (linkProfile == LinkProfile.LINKS_FOR_PLACE || linkProfile == LinkProfile.LINKS_FOR_BEACONS) {
				/*
				 * These are the same because LINKS_FOR_BEACONS is used to produce places and we want the same link
				 * profile for places regardless of what code path fetches them.
				 */
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_BEACON, true, true, limitProximity));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, true, true, limitApplinks));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_COMMENT, false, true, limitDefault));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PICTURE, true, true, limitContent)); 	// for preview and photo picker
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_CANDIGRAM, true, true, limitContent, Maps.asMap("inactive", false))); // just one so we can preview
				if (currentUser != null) {
					linkOptions.getActive().add(
							new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, true, true, limitLike, Maps.asMap("_from", currentUser.id)));
					linkOptions.getActive().add(
							new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, limitWatch, Maps.asMap("_from", currentUser.id)));
				}
				else {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, false, true, limitLike));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, false, true, limitWatch));
				}
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_CANDIGRAM) {
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, true, true, limitApplinks));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_COMMENT, false, true, limitDefault));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PICTURE, true, true, 1)); // just one so we can preview

				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, true, true, limitDefault, Maps.asMap("inactive", false))
								.setDirection(Direction.out));
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, true, true, limitDefault, Maps.asMap("inactive", true))
								.setDirection(Direction.out));

				if (currentUser != null) {
					linkOptions.getActive().add(
							new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, true, true, limitLike, Maps.asMap("_from", currentUser.id)));
					linkOptions.getActive().add(
							new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, limitWatch, Maps.asMap("_from", currentUser.id)));
				}
				else {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, false, true, limitLike));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, false, true, limitWatch));
				}
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_PICTURE) {
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, true, true, limitApplinks));
				linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_COMMENT, false, true, limitDefault));
				if (currentUser != null) {
					linkOptions.getActive().add(
							new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, true, true, limitLike, Maps.asMap("_from", currentUser.id)));
					linkOptions.getActive().add(
							new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, limitWatch, Maps.asMap("_from", currentUser.id)));
				}
				else {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, false, true, limitLike));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, false, true, limitWatch));
				}
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_CANDIGRAM, true, true, 1).setDirection(Direction.out));
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_COMMENT) {
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, true, true, 1).setDirection(Direction.out));
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PICTURE, true, true, 1).setDirection(Direction.out));
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_CANDIGRAM, true, true, 1).setDirection(Direction.out));
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_USER_CURRENT) {
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_PLACE, false, true, limitLike).setDirection(Direction.both));

				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PLACE, true, true, limitCreate).setDirection(Direction.out));
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PICTURE, true, true, limitCreate).setDirection(Direction.out));
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_CANDIGRAM, true, true, limitCreate).setDirection(Direction.out));

				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, true, true, limitWatch).setDirection(Direction.out));
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PICTURE, true, true, limitWatch).setDirection(Direction.out));
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_CANDIGRAM, true, true, limitWatch).setDirection(Direction.out));
				linkOptions.getActive().add(
						new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, limitWatch).setDirection(Direction.out));
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_USER) {
				if (currentUser != null) {
					linkOptions.getActive().add(
							new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, true, true, limitLike, Maps.asMap("_from", currentUser.id)));
					linkOptions.getActive().add(
							new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, limitWatch, Maps.asMap("_from", currentUser.id)));
				}
				else {
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, false, true, limitLike));
					linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, false, true, limitWatch));
				}
			}
			return linkOptions;
		}
	}

	public enum LinkProfile {
		LINKS_FOR_BEACONS,
		LINKS_FOR_PLACE,
		LINKS_FOR_CANDIGRAM,
		LINKS_FOR_PICTURE,
		LINKS_FOR_COMMENT,
		LINKS_FOR_USER,
		LINKS_FOR_USER_CURRENT,
		NO_LINKS, 
	}
}