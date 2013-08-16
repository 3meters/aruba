package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.components.EntityManager;
import com.aircandi.components.LocationManager;
import com.aircandi.service.Expose;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.utilities.DateTime;

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
	@Expose
	public Number				position;

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
	public String				toId;											// Used to find entities this entity is linked to
	@Expose(serialize = false, deserialize = true)
	public String				fromId;										// Used to find entities this entity is linked from

	@Expose(serialize = false, deserialize = true)
	public List<Entity>			entities;

	// --------------------------------------------------------------------------------------------
	// Client fields (none are transferred)
	// --------------------------------------------------------------------------------------------	

	public Boolean				hidden				= false;					// Flag entities not currently visible because of fencing.
	public Float				distance;										// Used to cache most recent distance calculation.
	public Boolean				checked				= false;					// Used to track selection in lists.
	public Boolean				stale				= false;					// Used to determine that an entity ref is no longer current.
	public Boolean				synthetic			= false;					// Entity is not persisted with service.
	public Boolean				shortcuts			= false;					// Do links have shortcuts?

	public Entity() {}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public static Entity makeEntity(String schema) {
		if (schema == null) {
			throw new IllegalArgumentException("Entity.makeEntity(): schema parameter is null");
		}
		Entity entity = null;

		if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			entity = new Place();
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
			entity = new Post();
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
			entity = new Candigram();
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			entity = new Applink();
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
			entity = new Comment();
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
			entity = new User();
		}

		entity.schema = schema;
		entity.id = "temp:" + DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME); // Temporary

		entity.signalFence = -100.0f;

		if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			Place place = (Place) entity;
			place.provider = new ProviderMap();
			place.provider.aircandi = Aircandi.getInstance().getUser().id;
			place.category = new Category();
			place.category.id = "generic";
			place.category.photo = new Photo(ProxiConstants.PATH_PROXIBASE_SERVICE_ASSETS_CATEGORIES + "generic_88.png", null, null, null,
					PhotoSource.assets_categories);
			place.category.photo.colorize = true;
		}
		return entity;
	}

	public Shortcut getShortcut() {
		Shortcut shortcut = new Shortcut()
				.setAppId(id)
				.setId(id)
				.setName(name != null ? name : null)
				.setPhoto(getPhoto())
				.setSchema(schema != null ? schema : null)
				.setApp(schema != null ? schema : null);
		return shortcut;
	}

	public Integer getPosition() {
		return position != null ? position.intValue() : 0;
	}

	public Boolean isTempId() {
		if (id != null && id.substring(0, 5).equals("temp:")) {
			return true;
		}
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------

	public Boolean isHidden() {
		Boolean oldIsHidden = hidden;
		this.hidden = false;
		/*
		 * Make it harder to fade out than it is to fade in. Entities are only New
		 * for the first scan that discovers them.
		 */
		if (signalFence != null) {
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

		Photo photo = getPhoto();
		String photoUri = photo.getSizedUri(250, 250); // sizing ignored if source doesn't support it
		if (photoUri == null) {
			photoUri = photo.getUri();
		}

		if (!photoUri.startsWith("http:") && !photoUri.startsWith("https:") && !photoUri.startsWith("resource:")) {
			photoUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + photoUri;
		}

		return photoUri;
	}

	public Photo getPhoto() {
		if (this.photo != null) {
			return this.photo;
		}
		else {
			return getDefaultPhoto();
		}
	}

	public Photo getDefaultPhoto() {
		Photo photo = new Photo("resource:img_placeholder_logo_bw", null, null, null, null);
		photo.usingDefault = true;
		return photo;
	}

	public AirLocation getLocation() {
		AirLocation loc = null;
		Beacon parent = null;
		if (getParent() instanceof Beacon) {
			parent = (Beacon) getParent();
		}
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
						Beacon beacon = (Beacon) EntityManager.getEntityCache().get(link.toId);
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
						Beacon beacon = (Beacon) EntityManager.getEntityCache().get(link.toId);
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
			Beacon beacon = (Beacon) EntityManager.getEntityCache().get(activeLink.toId);
			return beacon;
		}
		return null;
	}

	public List<? extends Entity> getLinkedEntitiesByLinkTypes(List<String> linkTypes, List<String> schemas, Direction direction, Boolean traverse) {
		final List<Entity> entities = new ArrayList<Entity>();
		if (linksIn != null) {
			if (direction == Direction.in || direction == Direction.both) {
				for (Link link : linksIn) {
					if ((linkTypes == null || linkTypes.contains(link.type)) && link.strong) {
						Entity entity = EntityManager.getEntity(link.fromId);
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
		}
		if (linksOut != null) {
			if (direction == Direction.out || direction == Direction.both) {
				for (Link link : linksOut) {
					if ((linkTypes == null || linkTypes.contains(link.type)) && link.strong) {
						Entity entity = EntityManager.getEntity(link.toId);
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
		}
		return entities;
	}

	public Entity getParent() {
		return EntityManager.getEntity(toId);
	}

	public Boolean hasActiveProximityLink() {
		if (linksOut != null) {
			for (Link link : linksOut) {
				if (link.proximity != null && link.type.equals("proximity")) {
					Beacon beacon = (Beacon) EntityManager.getEntity(link.toId);
					if (beacon != null) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public Count getCount(String countType, Direction direction) {
		List<Count> linkCounts = linksInCounts;
		if (direction == Direction.out) {
			linkCounts = linksOutCounts;
		}

		if (linkCounts != null) {
			for (Count linkCount : linkCounts) {
				if (linkCount.type.equals(countType)) {
					return linkCount;
				}
			}
		}
		return null;
	}

	public Integer getCount(List<String> countTypes, Direction direction) {
		Integer count = 0;
		List<Count> linkCounts = linksInCounts;
		if (direction == Direction.out) {
			linkCounts = linksOutCounts;
		}
		if (linkCounts != null) {
			for (Count linkCount : linkCounts) {
				if (countTypes.contains(linkCount.type)) {
					count += linkCount.count.intValue();
				}
			}
		}
		return count;
	}

	public Link getLinkByType(String linkType, String otherId, Direction direction) {
		List<Link> links = linksIn;
		if (direction == Direction.out) {
			links = linksOut;
		}
		if (links != null) {
			for (Link link : links) {
				if (link.type.equals(linkType)) {
					if (otherId == null || otherId.equals(direction == Direction.in ? link.fromId : link.toId)) {
						return link;
					}
				}
			}
		}
		return null;
	}

	public Link removeLinksByType(String linkType, String otherId, Direction direction) {
		List<Link> links = linksIn;
		if (direction == Direction.out) {
			links = linksOut;
		}
		if (links != null) {
			Iterator<Link> iterLinks = links.iterator();
			while (iterLinks.hasNext()) {
				Link link = iterLinks.next();
				if (link.type.equals(linkType)) {
					if (otherId == null || otherId.equals(direction == Direction.in ? link.fromId : link.toId)) {
						iterLinks.remove();
					}
				}
			}
		}
		return null;
	}

	public List<Shortcut> getShortcuts(ShortcutSettings settings) {

		List<Shortcut> shortcuts = new ArrayList<Shortcut>();
		List<Link> links = settings.direction == Direction.in ? linksIn : linksOut;

		if (links != null) {
			for (Link link : links) {
				if ((settings.linkType == null || link.type.equals(settings.linkType)) && link.shortcut != null) {
					if (settings.linkSchema == null || (link.shortcut != null && link.shortcut.schema.equals(settings.linkSchema))) {
						if (settings.synthetic == null || link.shortcut.isSynthetic() == settings.synthetic) {
							/*
							 * Must clone or the groups added below will cause circular references
							 * that choke serializing to json.
							 */
							shortcuts.add(link.shortcut.clone());
						}
					}
				}
			}

			if (shortcuts.size() > 0 && settings.groupedByApp) {

				final Map<String, List<Shortcut>> shortcutLists = new HashMap<String, List<Shortcut>>();
				for (Shortcut shortcut : shortcuts) {
					if (shortcutLists.containsKey(shortcut.app)) {
						shortcutLists.get(shortcut.app).add(shortcut);
					}
					else {
						List<Shortcut> list = new ArrayList<Shortcut>();
						list.add(shortcut);
						shortcutLists.put(shortcut.app, list);
					}
				}

				shortcuts.clear();
				final Iterator iter = shortcutLists.keySet().iterator();
				while (iter.hasNext()) {
					List<Shortcut> list = shortcutLists.get(iter.next());
					Shortcut shortcut = list.get(0);
					shortcut.setCount(0);
					Count count = getCount(shortcut.app, settings.direction);
					if (count != null) {
						shortcut.setCount(count.count.intValue());
					}
					shortcut.group = list;
					shortcuts.add(shortcut);
				}
			}
		}

		return shortcuts;
	}

	public Boolean byAppUser(String linkType) {
		if (linksIn != null) {
			for (Link link : linksIn) {
				if (link.type.equals(linkType) && link.fromId.equals(Aircandi.getInstance().getUser().id)) {
					return true;
				}
			}
		}
		return false;
	}

	public List<Shortcut> getClientShortcuts() {

		List<Shortcut> shortcuts = new ArrayList<Shortcut>();

		if (this.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			Shortcut shortcut = Shortcut.builder(this
					, Constants.SCHEMA_ENTITY_APPLINK
					, Constants.TYPE_APP_POST
					, Constants.ACTION_VIEW_AUTO
					, "pictures"
					, "resource:img_picture"
					, 10
					, false
					, true);
			Link link = getLinkByType(Constants.TYPE_LINK_PICTURE, null, Direction.in);
			if (link != null) {
				shortcut.photo = link.shortcut.getPhoto();
				shortcut.appId = link.fromId;
			}
			shortcuts.add(shortcut);

			shortcut = Shortcut.builder(this
					, Constants.SCHEMA_ENTITY_APPLINK
					, Constants.TYPE_APP_CANDIGRAM
					, Constants.ACTION_VIEW_AUTO
					, "candigrams"
					, "resource:ic_launcher"
					, 10
					, false
					, true);
			link = getLinkByType(Constants.TYPE_LINK_CANDIGRAM, null, Direction.in);
			if (link != null) {
				shortcut.photo = link.shortcut.getPhoto();
				shortcut.appId = link.fromId;
			}
			shortcuts.add(shortcut);

		}
		else if (this.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
			Shortcut shortcut = Shortcut.builder(this
					, Constants.SCHEMA_ENTITY_APPLINK
					, Constants.TYPE_APP_POST
					, Constants.ACTION_VIEW_AUTO
					, "pictures"
					, "resource:img_picture"
					, 10
					, false
					, true);
			Link link = getLinkByType(Constants.TYPE_LINK_PICTURE, null, Direction.in);
			if (link != null) {
				shortcut.photo = link.shortcut.getPhoto();
				shortcut.appId = link.fromId;
			}
			shortcuts.add(shortcut);

		}

		shortcuts.add(Shortcut.builder(this
				, Constants.SCHEMA_ENTITY_APPLINK
				, Constants.TYPE_APP_COMMENT
				, Constants.ACTION_VIEW_FOR
				, "comments"
				, "resource:img_comment"
				, 20
				, false
				, true));

		shortcuts.add(Shortcut.builder(this
				, Constants.SCHEMA_ENTITY_APPLINK
				, Constants.TYPE_APP_MAP
				, Constants.ACTION_VIEW
				, "map"
				, "resource:img_map"
				, 30
				, false
				, true));

		return shortcuts;
	}

	public void removeLink() {}

	// --------------------------------------------------------------------------------------------
	// Copy and serialization
	// --------------------------------------------------------------------------------------------

	public static Entity setPropertiesFromMap(Entity entity, Map map, Boolean nameMapping) {

		synchronized (entity) {
			/*
			 * Need to include any properties that need to survive encode/decoded between activities.
			 */
			entity = (Entity) ServiceBase.setPropertiesFromMap(entity, map, nameMapping);

			entity.subtitle = (String) map.get("subtitle");
			entity.description = (String) map.get("description");
			entity.signalFence = (Number) map.get("signalFence");
			entity.position = (Number) map.get("position");

			entity.hidden = (Boolean) (map.get("hidden") != null ? map.get("hidden") : false);
			entity.synthetic = (Boolean) (map.get("synthetic") != null ? map.get("synthetic") : false);
			entity.shortcuts = (Boolean) (map.get("shortcuts") != null ? map.get("shortcuts") : false);
			entity.stale = (Boolean) (map.get("stale") != null ? map.get("stale") : false);
			entity.checked = (Boolean) (map.get("checked") != null ? map.get("checked") : false);

			entity.toId = (String) (nameMapping ? map.get("_to") : map.get("toId"));
			entity.fromId = (String) (nameMapping ? map.get("_from") : map.get("fromId"));

			if (map.get("photo") != null) {
				entity.photo = Photo.setPropertiesFromMap(new Photo(), (HashMap<String, Object>) map.get("photo"), nameMapping);
			}

			if (map.get("location") != null) {
				entity.location = AirLocation.setPropertiesFromMap(new AirLocation(), (HashMap<String, Object>) map.get("location"), nameMapping);
			}

			if (map.get("linksIn") != null) {
				entity.linksIn = new ArrayList<Link>();
				final List<LinkedHashMap<String, Object>> linkMaps = (List<LinkedHashMap<String, Object>>) map.get("linksIn");
				for (Map<String, Object> linkMap : linkMaps) {
					entity.linksIn.add(Link.setPropertiesFromMap(new Link(), linkMap, nameMapping));
				}
			}

			if (map.get("linksOut") != null) {
				entity.linksOut = new ArrayList<Link>();
				final List<LinkedHashMap<String, Object>> linkMaps = (List<LinkedHashMap<String, Object>>) map.get("linksOut");
				for (Map<String, Object> linkMap : linkMaps) {
					entity.linksOut.add(Link.setPropertiesFromMap(new Link(), linkMap, nameMapping));
				}
			}

			if (map.get("linksInCounts") != null) {
				entity.linksInCounts = new ArrayList<Count>();
				final List<LinkedHashMap<String, Object>> countMaps = (List<LinkedHashMap<String, Object>>) map.get("linksInCounts");
				for (Map<String, Object> countMap : countMaps) {
					entity.linksInCounts.add(Count.setPropertiesFromMap(new Count(), countMap, nameMapping));
				}
			}

			if (map.get("linksOutCounts") != null) {
				entity.linksOutCounts = new ArrayList<Count>();
				final List<LinkedHashMap<String, Object>> countMaps = (List<LinkedHashMap<String, Object>>) map.get("linksOutCounts");
				for (Map<String, Object> countMap : countMaps) {
					entity.linksOutCounts.add(Count.setPropertiesFromMap(new Count(), countMap, nameMapping));
				}
			}

			if (map.get("entities") != null) {
				entity.entities = new ArrayList<Entity>();
				synchronized (entity.entities) {
					final List<LinkedHashMap<String, Object>> childMaps = (List<LinkedHashMap<String, Object>>) map.get("entities");
					for (Map<String, Object> childMap : childMaps) {
						String schema = (String) childMap.get("schema");
						if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
							entity.entities.add(Place.setPropertiesFromMap(new Place(), childMap, nameMapping));
						}
						else if (schema.equals(Constants.SCHEMA_ENTITY_BEACON)) {
							entity.entities.add(Beacon.setPropertiesFromMap(new Beacon(), childMap, nameMapping));
						}
						else if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
							entity.entities.add(Post.setPropertiesFromMap(new Post(), childMap, nameMapping));
						}
						else if (schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
							entity.entities.add(Candigram.setPropertiesFromMap(new Candigram(), childMap, nameMapping));
						}
						else if (schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
							entity.entities.add(Applink.setPropertiesFromMap(new Applink(), childMap, nameMapping));
						}
						else if (schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
							entity.entities.add(Comment.setPropertiesFromMap(new Comment(), childMap, nameMapping));
						}
						else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
							entity.entities.add(User.setPropertiesFromMap(new User(), childMap, nameMapping));
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
	// Classes
	// --------------------------------------------------------------------------------------------	

	public static class SortByPositionModifiedDate implements Comparator<Entity> {

		@Override
		public int compare(Entity object1, Entity object2) {
			if (object1.getPosition().intValue() < object2.getPosition().intValue()) {
				return -1;
			}
			if (object1.getPosition().intValue() == object2.getPosition().intValue()) {
				if (object1.modifiedDate == null || object2.modifiedDate == null) {
					return 0;
				}
				else {
					if (object1.modifiedDate.longValue() < object2.modifiedDate.longValue()) {
						return 1;
					}
					else if (object1.modifiedDate.longValue() == object2.modifiedDate.longValue()) {
						return 0;
					}
					return -1;
				}
			}
			return 1;
		}
	}

	public static class SortByModifiedDate implements Comparator<Entity> {

		@Override
		public int compare(Entity object1, Entity object2) {

			if (object1.modifiedDate.longValue() < object2.modifiedDate.longValue()) {
				return 1;
			}
			else if (object1.modifiedDate.longValue() == object2.modifiedDate.longValue()) {
				return 0;
			}
			return -1;
		}
	}

	public static class SortByPosition implements Comparator<Entity> {

		@Override
		public int compare(Entity object1, Entity object2) {

			if (object1.getPosition() < object2.getPosition()) {
				return -1;
			}
			if (object1.getPosition().equals(object2.getPosition())) {
				return 0;
			}
			return 1;
		}
	}
}