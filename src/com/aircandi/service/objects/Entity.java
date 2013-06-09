package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.components.ProxiManager;
import com.aircandi.service.Copy;
import com.aircandi.service.Expose;

/**
 * Entity as described by the proxi protocol standards.
 * 
 * @author Jayma
 */
@SuppressWarnings("ucd")
public abstract class Entity extends ServiceBase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= -3902834532692561618L;

	/* Annotation syntax: @Expose (serialize = false, deserialize = false) */

	/* Database fields */

	@Expose
	public String				schema;
	@Expose
	public String				subtitle;
	@Expose
	public String				description;
	@Expose
	public Photo				photo;
	@Expose
	public GeoLocation			location;
	@Expose
	public Number				signalFence			= -100.0f;

	/* Synthetic service fields */

	@Expose(serialize = false, deserialize = true)
	public List<Link>			linksIn;
	@Expose(serialize = false, deserialize = true)
	public List<Link>			linksOut;
	@Expose(serialize = false, deserialize = true)
	public List<Count>			linksInCounts;
	@Expose(serialize = false, deserialize = true)
	public List<Count>			linksOutCounts;

	@Expose(serialize = false, deserialize = true)
	@Copy(exclude = true)
	public List<Entity>			entities;

	@Expose(serialize = false, deserialize = true)
	public String				toId;											// Used to find entities this entity is linked to

	/* For client use only */

	@Copy(exclude = true)
	public Bitmap				imageBitmap;

	/* These client props are all controlled by the parent in the case of child entities. */

	@Copy(exclude = true)
	public Boolean				hidden				= false;
	@Copy(exclude = true)
	public Float				distance;

	/* List state */

	@Copy(exclude = true)
	public Boolean				checked				= false;
	@Copy(exclude = true)
	public Integer				position;

	public Entity() {}

	// --------------------------------------------------------------------------------------------
	// Set and get
	// --------------------------------------------------------------------------------------------	

	public String getPhotoUri() {

		/*
		 * If a special preview photo is available, we use it otherwise
		 * we use the standard photo.
		 * 
		 * Only posts and collections do not have photo objects
		 */
		String imageUri = "resource:img_placeholder_logo_bw";
		if (photo != null) {
			imageUri = photo.getSizedUri(250, 250); // sizing ignored if source doesn't support it
			if (imageUri == null) {
				imageUri = photo.getUri();
			}
		}
		else {
			if (creator != null) {
				if (creator.getPhotoUri() != null && !creator.getPhotoUri().equals("")) {
					imageUri = creator.getPhotoUri();
				}
			}
			if (!imageUri.startsWith("http:") && !imageUri.startsWith("https:") && !imageUri.startsWith("resource:")) {
				imageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
			}
		}

		return imageUri;
	}

	public GeoLocation getLocation() {
		GeoLocation loc = null;
		final Beacon parent = (Beacon) getParent();
		if (parent != null) {
			loc = parent.location;
		}
		else {
			final Beacon beacon = getActiveBeaconPrimaryOnly("proximity");
			if (beacon != null) {
				loc = beacon.location;
			}
		}
		if (loc == null
				&& this.location != null
				&& this.location.lat != null
				&& this.location.lng != null) {
			loc = new GeoLocation(this.location.lat.doubleValue(), this.location.lng.doubleValue());
		}
		return loc;
	}

	public Beacon getActiveBeaconPrimaryOnly(String linkType) {
		final Link link = getActiveLink(linkType, true);
		if (link != null) {
			final Beacon beacon = ProxiManager.getInstance().getEntityModel().getBeacon(link.toId);
			return beacon;
		}
		return null;
	}

	public Beacon getActiveBeacon(String linkType) {
		final Link link = getActiveLink(linkType, false);
		if (link != null) {
			final Beacon beacon = ProxiManager.getInstance().getEntityModel().getBeacon(link.toId);
			return beacon;
		}
		return null;
	}

	public Link getActiveLink(String linkType, Boolean primaryOnly) {
		/*
		 * If an entity has more than one viable link, we choose the one
		 * using the following priority:
		 * 
		 * - strongest primary
		 * - any primary
		 * - any non-primary
		 */
		if (linksOut != null) {
			Link strongestLink = null;
			Integer strongestLevel = -200;
			for (Link link : linksOut) {
				if (link.type.equals(linkType)) {
					if (link.proximity != null && link.proximity.primary) {
						Beacon beacon = ProxiManager.getInstance().getEntityModel().getBeacon(link.toId);
						if (beacon != null && beacon.signal.intValue() > strongestLevel) {
							strongestLink = link;
							strongestLevel = beacon.signal.intValue();
						}
					}
				}
			}

			if (strongestLink == null && !primaryOnly) {
				for (Link link : linksOut) {
					if (link.type.equals(linkType)) {
						Beacon beacon = ProxiManager.getInstance().getEntityModel().getBeacon(link.toId);
						if (beacon != null && beacon.signal.intValue() > strongestLevel) {
							strongestLink = link;
							strongestLevel = beacon.signal.intValue();
						}
					}
				}
			}

			return strongestLink;
		}
		return null;
	}

	public List<? extends Entity> getChildrenByLinkType(String linkType) {
		final List<Entity> entities = new ArrayList<Entity>();
		for (Link link : linksIn) {
			if (link.type.equals(linkType)) {
				Entity entity = ProxiManager.getInstance().getEntityModel().getCacheEntity(link.fromId);
				if (entity != null) {
					entities.add(entity);
				}
			}
		}
		return entities;
	}

	public List<? extends Entity> getChildrenByLinkType(List<String> linkTypes) {
		final List<Entity> entities = new ArrayList<Entity>();
		for (Link link : linksIn) {
			if (linkTypes.contains(link.type)) {
				Entity entity = ProxiManager.getInstance().getEntityModel().getCacheEntity(link.fromId);
				if (entity != null) {
					entities.add(entity);
				}
			}
		}
		return entities;
	}
	
	public List<Entity> getChildren() {
		final List<Entity> entities = new ArrayList<Entity>();
		for (Link link : linksIn) {
			Entity entity = ProxiManager.getInstance().getEntityModel().getCacheEntity(link.fromId);
			if (entity != null) {
				entities.add(entity);
			}
		}
		return entities;
	}

	public Entity getParent() {
		return ProxiManager.getInstance().getEntityModel().getCacheEntity(toId);
	}

	public Boolean hasActiveProximityLink() {
		if (linksOut != null) {
			for (Link link : linksOut) {
				if (link.proximity != null && link.type.equals("proximity")) {
					Beacon beacon = ProxiManager.getInstance().getEntityModel().getBeacon(link.toId);
					if (beacon != null) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public Integer getInCount(String countType) {
		Integer count = 0;
		for (Count linkCount : linksInCounts) {
			if (linkCount.type.equals(countType)) {
				count = linkCount.count.intValue();
				return count;
			}
		}
		return count;
	}

	public Integer getInCount(List<String> countTypes) {
		Integer count = 0;
		for (Count linkCount : linksInCounts) {
			if (countTypes.contains(linkCount.type)) {
				count += linkCount.count.intValue();
			}
		}
		return count;
	}
	
	public Link getInLinkByType(String linkType, String fromId) {
		for (Link link : linksIn) {
			if (link.type.equals(linkType)) {
				if (fromId == null || fromId.equals(link.fromId)) {
					return link;
				}
			}
		}
		return null;
	}

	public Link getOutLinkByType(String linkType, String toId) {
		for (Link link : linksOut) {
			if (link.type.equals(linkType)) {
				if (toId == null || toId.equals(link.toId)) {
					return link;
				}
			}
		}
		return null;
	}

	public Boolean byAppUser(String linkType) {
		for (Link link : linksIn) {
			if (link.type.equals(linkType) && link.fromId.equals(Aircandi.getInstance().getUser().id)) {
				return true;
			}
		}
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Copy and serialization
	// --------------------------------------------------------------------------------------------

	public static Entity setPropertiesFromMap(Entity entity, Map map) {

		synchronized (entity) {

			entity = (Entity) ServiceBase.setPropertiesFromMap(entity, map);

			entity.schema = (String) map.get("schema");
			entity.subtitle = (String) map.get("subtitle");
			entity.description = (String) map.get("description");
			entity.signalFence = (Number) map.get("signalFence");
			entity.toId = (String) map.get("_to");

			if (map.get("photo") != null) {
				entity.photo = Photo.setPropertiesFromMap(new Photo(), (HashMap<String, Object>) map.get("photo"));
			}

			if (map.get("linksIn") != null) {
				entity.linksIn = new ArrayList<Link>();
				final List<LinkedHashMap<String, Object>> linkMaps = (List<LinkedHashMap<String, Object>>) map.get("linksIn");
				for (Map<String, Object> linkMap : linkMaps) {
					entity.linksIn.add(Link.setPropertiesFromMap(new Link(), linkMap));
				}
			}

			if (map.get("linksOut") != null) {
				entity.linksIn = new ArrayList<Link>();
				final List<LinkedHashMap<String, Object>> linkMaps = (List<LinkedHashMap<String, Object>>) map.get("linksOut");
				for (Map<String, Object> linkMap : linkMaps) {
					entity.linksIn.add(Link.setPropertiesFromMap(new Link(), linkMap));
				}
			}

			if (map.get("linksInCounts") != null) {
				entity.linksInCounts = new ArrayList<Count>();
				final List<LinkedHashMap<String, Object>> countMaps = (List<LinkedHashMap<String, Object>>) map.get("linksInCounts");
				for (Map<String, Object> countMap : countMaps) {
					entity.linksInCounts.add(Count.setPropertiesFromMap(new Count(), countMap));
				}
			}

			if (map.get("linksOutCounts") != null) {
				entity.linksOutCounts = new ArrayList<Count>();
				final List<LinkedHashMap<String, Object>> countMaps = (List<LinkedHashMap<String, Object>>) map.get("linksOutCounts");
				for (Map<String, Object> countMap : countMaps) {
					entity.linksOutCounts.add(Count.setPropertiesFromMap(new Count(), countMap));
				}
			}

			if (map.get("entities") != null) {
				entity.entities = Collections.synchronizedList(new ArrayList<Entity>());
				synchronized (entity.entities) {
					final List<LinkedHashMap<String, Object>> childMaps = (List<LinkedHashMap<String, Object>>) map.get("entities");
					for (Map<String, Object> childMap : childMaps) {
						String schema = (String) childMap.get("schema");
						if (schema.equals("place")) {
							entity.entities.add(Place.setPropertiesFromMap(new Place(), childMap));
						}
						else if (schema.equals("beacon")) {
							entity.entities.add(Beacon.setPropertiesFromMap(new Beacon(), childMap));
						}
						else if (schema.equals("post")) {
							entity.entities.add(Post.setPropertiesFromMap(new Post(), childMap));
						}
						else if (schema.equals("applink")) {
							entity.entities.add(Applink.setPropertiesFromMap(new Applink(), childMap));
						}
						else if (schema.equals("comment")) {
							entity.entities.add(Comment.setPropertiesFromMap(new Comment(), childMap));
						}
						else if (schema.equals("user")) {
							entity.entities.add(User.setPropertiesFromMap(new User(), childMap));
						}
					}
				}
			}
		}

		return entity;
	}

	@Override
	public Entity clone() {
		final Entity entity = (Entity) super.clone();
		if (linksIn != null) {
			entity.linksIn = (List<Link>) ((ArrayList) linksIn).clone();
		}
		if (linksOut != null) {
			entity.linksOut = (List<Link>) ((ArrayList) linksOut).clone();
		}
		if (linksInCounts != null) {
			entity.linksInCounts = (List<Count>) ((ArrayList) linksInCounts).clone();
		}
		if (linksOutCounts != null) {
			entity.linksOutCounts = (List<Count>) ((ArrayList) linksOutCounts).clone();
		}
		if (photo != null) {
			entity.photo = photo.clone();
		}
		return entity;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------	

	public static class SortEntitiesByModifiedDate implements Comparator<Entity> {

		@Override
		public int compare(Entity entity1, Entity entity2) {

			if (!entity1.type.equals(Constants.SCHEMA_ENTITY_APPLINK) && entity2.type.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
				return 1;
			}
			if (entity1.type.equals(Constants.SCHEMA_ENTITY_APPLINK) && !entity2.type.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
				return -1;
			}
			else {
				if (entity1.modifiedDate.longValue() < entity2.modifiedDate.longValue()) {
					return 1;
				}
				else if (entity1.modifiedDate.longValue() == entity2.modifiedDate.longValue()) {
					return 0;
				}
				return -1;
			}
		}
	}

	public static class SortEntitiesByWatchedDate implements Comparator<Entity> {

		@Override
		public int compare(Entity item1, Entity item2) {

			if (item1.watchedDate.longValue() < item2.watchedDate.longValue()) {
				return 1;
			}
			else if (item1.watchedDate.longValue() == item2.watchedDate.longValue()) {
				return 0;
			}
			return -1;
		}
	}

	public static class SortEntitiesByPosition implements Comparator<Entity> {

		@Override
		public int compare(Entity entity1, Entity entity2) {
			if (entity1.position < entity2.position) {
				return -1;
			}
			if (entity1.position.equals(entity2.position)) {
				return 0;
			}
			return 1;
		}
	}
}