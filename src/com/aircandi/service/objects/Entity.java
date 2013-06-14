package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.components.EntityManager;
import com.aircandi.components.LocationManager;
import com.aircandi.service.Copy;
import com.aircandi.service.Expose;
import com.aircandi.service.objects.Link.Direction;

/**
 * Entity as described by the proxi protocol standards.
 * 
 * @author Jayma
 */
@SuppressWarnings("ucd")
public abstract class Entity extends ServiceBase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= -3902834532692561618L;

	// --------------------------------------------------------------------------------------------
	// Service fields
	// --------------------------------------------------------------------------------------------	

	/* Database fields */

	@Expose
	public String				subtitle;
	@Expose
	public String				description;
	@Expose
	public Photo				photo;
	@Expose
	public AirLocation			location;
	@Expose
	public Number				signalFence			= -100.0f;

	/* Synthetic fields */

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

	// --------------------------------------------------------------------------------------------
	// Client fields (none are transferred)
	// --------------------------------------------------------------------------------------------	

	private Boolean				hidden				= false;
	protected Float				distance;
	public Boolean				checked				= false;
	public Boolean				stale				= false;
	public Integer				position;
	public Boolean				synthetic			= false;					// Entity is not persisted with service
	public String				tagPrimary;
	public String				tagSecondary;

	public Entity() {}

	// --------------------------------------------------------------------------------------------
	// Set and get
	// --------------------------------------------------------------------------------------------

	public Boolean isHidden() {
		Boolean oldIsHidden = hidden;
		this.hidden = false;
		/*
		 * Make it harder to fade out than it is to fade in. Entities are only New
		 * for the first scan that discovers them.
		 */
		float signalThresholdFluid = signalFence.floatValue();
		Beacon beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
		if (beacon != null) {
			if (!oldIsHidden) {
				signalThresholdFluid = signalFence.floatValue() - 5;
			}

			/* Hide entities that are not within entity declared virtual range */
			if (Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
					&& Aircandi.settings.getBoolean(Constants.PREF_ENTITY_FENCING, Constants.PREF_ENTITY_FENCING_DEFAULT)
					&& beacon.signal.intValue() < signalThresholdFluid) {
				this.hidden = true;
			}
		}
		return this.hidden;
	}

	public Float getDistance(Boolean refresh) {

		if (refresh || distance == null) {
			final Beacon beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
			if (beacon != null) {
				distance = beacon.getDistance(refresh);
			}
			else {
				final AirLocation entityLocation = getLocation();
				final AirLocation deviceLocation = LocationManager.getInstance().getAirLocationLocked();

				if (entityLocation != null && deviceLocation != null) {
					distance = deviceLocation.distanceTo(entityLocation);
				}
			}
		}
		return distance;
	}

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

	public AirLocation getLocation() {
		AirLocation loc = null;
		final Beacon parent = (Beacon) getParent();
		if (parent != null) {
			loc = parent.location;
		}
		else {
			final Beacon beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
			if (beacon != null) {
				loc = beacon.location;
			}
		}
		if (loc == null
				&& this.location != null
				&& this.location.lat != null
				&& this.location.lng != null) {
			loc = new AirLocation(this.location.lat.doubleValue(), this.location.lng.doubleValue());
		}
		return loc;
	}

	public Beacon getActiveBeacon(String linkType, Boolean primaryOnly) {
		/*
		 * If an entity has more than one viable link, we choose the one
		 * using the following priority:
		 * 
		 * - strongest primary
		 * - any primary
		 * - any non-primary
		 */
		Link activeLink = null;
		if (linksOut != null) {
			Link strongestLink = null;
			Integer strongestLevel = -200;
			for (Link link : linksOut) {
				if (link.type.equals(linkType)) {
					if (link.proximity != null && link.proximity.primary) {
						Beacon beacon = (Beacon) EntityManager.getInstance().getEntityCache().get(link.toId);
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
						Beacon beacon = (Beacon) EntityManager.getInstance().getEntityCache().get(link.toId);
						if (beacon != null && beacon.signal.intValue() > strongestLevel) {
							strongestLink = link;
							strongestLevel = beacon.signal.intValue();
						}
					}
				}
			}
			activeLink = strongestLink;
		}

		if (activeLink != null) {
			Beacon beacon = (Beacon) EntityManager.getInstance().getEntityCache().get(activeLink.toId);
			return beacon;
		}
		return null;
	}

	public List<? extends Entity> getLinkedEntitiesByLinkType(String linkType, List<String> schemas, Direction direction, Boolean traverse) {
		List<String> linkTypes = null;
		if (!linkType.equals(Constants.TYPE_ANY)) {
			linkTypes = new ArrayList<String>();
			linkTypes.add(linkType);
		}
		List<? extends Entity> entities = getLinkedEntitiesByLinkTypes(linkTypes, schemas, direction, traverse);
		return entities;
	}

	public List<? extends Entity> getLinkedEntitiesByLinkTypes(List<String> linkTypes, List<String> schemas, Direction direction, Boolean traverse) {
		final List<Entity> entities = new ArrayList<Entity>();
		if (direction == Direction.in || direction == Direction.both) {
			for (Link link : linksIn) {
				if (linkTypes == null || linkTypes.contains(link.type)) {
					Entity entity = EntityManager.getInstance().getEntity(link.fromId);
					if (entity != null) {
						if (traverse) {
							entities.addAll(entity.getLinkedEntitiesByLinkTypes(linkTypes, schemas, Direction.in, traverse));
						}
						if (schemas == null || schemas.contains(entity.schema)) {
							entities.add(entity);
						}
					}
				}
			}
		}
		if (direction == Direction.out || direction == Direction.both) {
			for (Link link : linksOut) {
				if (linkTypes == null || linkTypes.contains(link.type)) {
					Entity entity = EntityManager.getInstance().getEntity(link.toId);
					if (entity != null) {
						if (traverse) {
							entities.addAll(entity.getLinkedEntitiesByLinkTypes(linkTypes, schemas, Direction.out, traverse));
						}
						if (schemas == null || schemas.contains(entity.schema)) {
							entities.add(entity);
						}
					}
				}
			}
		}
		return entities;
	}

	public Entity getParent() {
		return EntityManager.getInstance().getEntity(toId);
	}

	public Boolean hasActiveProximityLink() {
		if (linksOut != null) {
			for (Link link : linksOut) {
				if (link.proximity != null && link.type.equals("proximity")) {
					Beacon beacon = (Beacon) EntityManager.getInstance().getEntity(link.toId);
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

	public List<Applink> getApplinks() {
		List<Applink> applinks = new ArrayList<Applink>();
		applinks.add(Applink.builder(this, "map", "map", "resource:img_post", null));
		applinks.add(Applink.builder(this, "comment", "comments", "resource:img_post", this.getInCount(Constants.TYPE_LINK_COMMENT)));
		applinks.add(Applink.builder(this, "like", "likes", "resource:img_like", this.getInCount(Constants.TYPE_LINK_LIKE)));
		applinks.add(Applink.builder(this, "watch", "watching", "resource:img_watch", this.getInCount(Constants.TYPE_LINK_WATCH)));
		return applinks;
	}

	// --------------------------------------------------------------------------------------------
	// Copy and serialization
	// --------------------------------------------------------------------------------------------

	public static Entity setPropertiesFromMap(Entity entity, Map map) {

		synchronized (entity) {

			entity = (Entity) ServiceBase.setPropertiesFromMap(entity, map);

			entity.subtitle = (String) map.get("subtitle");
			entity.description = (String) map.get("description");
			entity.signalFence = (Number) map.get("signalFence");
			entity.toId = (String) map.get("_to");

			if (map.get("photo") != null) {
				entity.photo = Photo.setPropertiesFromMap(new Photo(), (HashMap<String, Object>) map.get("photo"));
			}

			if (map.get("location") != null) {
				entity.location = AirLocation.setPropertiesFromMap(new AirLocation(), (HashMap<String, Object>) map.get("location"));
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

	public static class SortLinksByCreatedDate implements Comparator<Link> {

		@Override
		public int compare(Link item1, Link item2) {

			if (item1.createdDate.longValue() < item2.createdDate.longValue()) {
				return 1;
			}
			else if (item1.createdDate.longValue() == item2.createdDate.longValue()) {
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