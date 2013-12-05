package com.aircandi.aruba.objects;

import java.util.ArrayList;

import android.content.res.Resources;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.aruba.R;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkParams;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.Maps;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Links extends com.aircandi.service.objects.Links {

	private static final long	serialVersionUID	= -274203160211174564L;

	@Override
	public com.aircandi.service.objects.Links build(LinkProfile linkProfile) {

		com.aircandi.service.objects.Links links = null;

		if (linkProfile != LinkProfile.NO_LINKS) {

			User currentUser = Aircandi.getInstance().getCurrentUser();

			links = new Links().setActive(new ArrayList<LinkParams>());
			links.shortcuts = true;

			Resources resources = Aircandi.applicationContext.getResources();
			Number limitProximity = resources.getInteger(R.integer.limit_links_proximity_default);
			Number limitCreate = resources.getInteger(R.integer.limit_links_create_default);
			Number limitWatch = resources.getInteger(R.integer.limit_links_watch_default);
			Number limitApplinks = resources.getInteger(R.integer.limit_links_applinks_default);

			if (linkProfile == LinkProfile.LINKS_FOR_PLACE || linkProfile == LinkProfile.LINKS_FOR_BEACONS) {
				/*
				 * These are the same because LINKS_FOR_BEACONS is used to produce places and we want the same link
				 * profile for places regardless of what code path fetches them.
				 */
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_PROXIMITY, Constants.SCHEMA_ENTITY_BEACON, true, true, limitProximity));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, true, true, limitApplinks));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_COMMENT, false, true, 0));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PICTURE, true, true, 1)); 	// for preview and photo picker
				if (Aircandi.getInstance().getContainer().getBoolean(Aircandi.CANDIGRAMS_ENABLED)) {
					links.getActive().add(
							new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_CANDIGRAM, true, true, 1, Maps.asMap("inactive", false))); // just one so we can preview
				}
				links.getActive().add(
						new LinkParams(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, true, true, 1, Maps.asMap("_from", currentUser.id)));
				links.getActive().add(
						new LinkParams(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, 1, Maps.asMap("_from", currentUser.id)));
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_PICTURE) {
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, true, true, limitApplinks));
				links.getActive().add(new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_COMMENT, false, true, 0));
				links.getActive().add(
						new LinkParams(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, true, true, 1, Maps.asMap("_from", currentUser.id)));
				links.getActive().add(
						new LinkParams(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, 1, Maps.asMap("_from", currentUser.id)));
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_COMMENT) {
				links.getActive().add(
						new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE, true, true, 1).setDirection(Direction.out));
				links.getActive().add(
						new LinkParams(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PICTURE, true, true, 1).setDirection(Direction.out));
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_USER_CURRENT) {
				links.getActive().add(
						new LinkParams(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_PLACE, false, true, 0).setDirection(Direction.both));
				links.getActive().add(
						new LinkParams(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PLACE, true, true, limitCreate).setDirection(Direction.out));
				links.getActive().add(
						new LinkParams(Constants.TYPE_LINK_CREATE, Constants.SCHEMA_ENTITY_PICTURE, true, true, limitCreate).setDirection(Direction.out));
				links.getActive().add(
						new LinkParams(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PLACE, true, true, limitWatch).setDirection(Direction.out));
				links.getActive().add(
						new LinkParams(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_PICTURE, true, true, limitWatch).setDirection(Direction.out));
				links.getActive().add(
						new LinkParams(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, limitWatch).setDirection(Direction.out));
			}
			else if (linkProfile == LinkProfile.LINKS_FOR_USER) {
				links.getActive().add(
						new LinkParams(Constants.TYPE_LINK_LIKE, Constants.SCHEMA_ENTITY_USER, true, true, 1, Maps.asMap("_from", currentUser.id)));
				links.getActive().add(
						new LinkParams(Constants.TYPE_LINK_WATCH, Constants.SCHEMA_ENTITY_USER, true, true, 1, Maps.asMap("_from", currentUser.id)));
			}
			else {
				links = super.build(linkProfile);
			}
		}
		return links;
	}
}