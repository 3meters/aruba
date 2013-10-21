package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.applications.Candigrams;
import com.aircandi.applications.Comments;
import com.aircandi.applications.Maps;
import com.aircandi.applications.Pictures;
import com.aircandi.components.EntityManager;
import com.aircandi.components.LocationManager;
import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.utilities.DateTime;

/**
 * Entity as described by the service protocol standards.
 * 
 * @author Jayma
 */
@SuppressWarnings("ucd")
public abstract class Entity extends ServiceBase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= -3902834532692561618L;

	// --------------------------------------------------------------------------------------------
	// service fields
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
	@SerializedName(name = "_place")
	public String				placeId;

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

	/* Place (synthesized for the client) */

	@Expose(serialize = false, deserialize = true)
	public Place				place;

	// --------------------------------------------------------------------------------------------
	// client fields (NONE are transferred)
	// --------------------------------------------------------------------------------------------	

	public Boolean				hidden				= false;					// Flag entities not currently visible because of fencing.
	public Float				distance;										// Used to cache most recent distance calculation.
	public Boolean				checked				= false;					// Used to track selection in lists.
	public Boolean				synthetic			= false;					// Entity is not persisted with service.
	public Boolean				shortcuts			= false;					// Do links have shortcuts?
	public Boolean				proximity			= false;					// Was this found based on proximity
	public Boolean				editing				= false;					// Used to flag when we can't use id to match up.

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
			place.provider.aircandi = Aircandi.getInstance().getCurrentUser().id;
			place.category = new Category();
			place.category.id = "generic";
			place.category.photo = new Photo("generic_88.png", null, null, null,
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
				.setApp(schema != null ? schema : null)
				.setPosition(position);

		shortcut.sortDate = sortDate != null ? sortDate : modifiedDate;
		shortcut.content = true;
		shortcut.action = Constants.ACTION_VIEW;
		return shortcut;
	}

	public CacheStamp getCacheStamp() {
		CacheStamp cacheStamp = new CacheStamp(this.activityDate, this.modifiedDate);
		return cacheStamp;
	}

	public Boolean isTempId() {
		if (id != null && id.substring(0, 5).equals("temp:")) {
			return true;
		}
		return false;
	}

	public Boolean isOwnedByCurrentUser() {
		Boolean owned = (ownerId != null
				&& Aircandi.getInstance().getCurrentUser() != null
				&& ownerId.equals(Aircandi.getInstance().getCurrentUser().id));
		return owned;
	}

	public Boolean isOwnedBySystem() {
		Boolean owned = (ownerId != null && ownerId.equals(ServiceConstants.ADMIN_USER_ID));
		return owned;
	}

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public LinkProfile getDefaultLinkProfile() {
		LinkProfile linkProfile = LinkProfile.NO_LINKS;
		if (this.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			linkProfile = LinkProfile.LINKS_FOR_PLACE;
		}
		else if (this.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
			linkProfile = LinkProfile.LINKS_FOR_CANDIGRAM;
		}
		else if (this.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
			linkProfile = LinkProfile.LINKS_FOR_PICTURE;
		}
		else if (this.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
			linkProfile = LinkProfile.LINKS_FOR_USER_CURRENT;
		}
		return linkProfile;
	}

	public Boolean isHidden() {
		Boolean oldIsHidden = hidden;
		this.hidden = false;
		/*
		 * Make it harder to fade out than it is to fade in. Entities are only NEW
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
			distance = null;
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

	public Photo getPhoto() {
		Photo photo = this.photo;
		if (photo == null) {
			photo = getDefaultPhoto();
		}
		photo.photoPlaceholder = getPlaceholderPhoto();
		photo.photoBroken = getBrokenPhoto();
		return photo;
	}

	public Photo getDefaultPhoto() {
		return getDefaultPhoto(this.schema, this.type);
	}

	public static Photo getDefaultPhoto(String schema, String type) {

		String prefix = "resource:img_placeholder_logo_bw";
		String source = PhotoSource.resource;
		if (schema != null && schema.equals(Constants.SCHEMA_ENTITY_APPLINK) && type != null) {
			prefix = type.toLowerCase(Locale.US) + ".png";
			source = PhotoSource.assets_applinks;
		}
		Photo photo = new Photo(prefix, null, null, null, source);
		return photo;
	}

	public Photo getPlaceholderPhoto() {
		return getDefaultPhoto();
	}

	public static Photo getBrokenPhoto() {
		String prefix = "resource:img_broken";
		String source = PhotoSource.resource;
		Photo photo = new Photo(prefix, null, null, null, source);
		return photo;
	}

	public AirLocation getLocation() {
		AirLocation loc = null;
		Entity parent = null;

		if (this.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
			parent = getParent(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PLACE);
		}
		else {
			parent = getParent(null, null);
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

	public Beacon getActiveBeacon(String type, Boolean primaryOnly) {
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
				if (link.type.equals(type)) {
					if (link.proximity != null && link.proximity.primary != null && link.proximity.primary) {
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
					if (link.type.equals(type)) {
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

	public List<? extends Entity> getLinkedEntitiesByLinkTypeAndSchema(List<String> type, List<String> schemas, Direction direction, Boolean traverse) {
		final List<Entity> entities = new ArrayList<Entity>();
		if (linksIn != null) {
			if (direction == Direction.in || direction == Direction.both) {
				for (Link link : linksIn) {
					if ((type == null || type.contains(link.type)) && link.strong) {
						Entity entity = EntityManager.getEntity(link.fromId);
						if (entity != null) {
							if (traverse) {
								entities.addAll(entity.getLinkedEntitiesByLinkTypeAndSchema(type, schemas, Direction.in, traverse));
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
					if ((type == null || type.contains(link.type)) && link.strong) {
						Entity entity = EntityManager.getEntity(link.toId);
						if (entity != null) {
							if (traverse) {
								entities.addAll(entity.getLinkedEntitiesByLinkTypeAndSchema(type, schemas, Direction.out, traverse));
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

	public Entity getParent(String type, String targetSchema) {
		if (toId == null && linksOut != null) {
			for (Link link : linksOut) {
				if (!link.inactive && (type == null || link.type.equals(type)) && (targetSchema == null || link.targetSchema.equals(targetSchema))) {
					return EntityManager.getEntity(link.toId);
				}
			}
			return null;
		}
		else {
			return EntityManager.getEntity(toId);
		}
	}

	public Link getParentLink(String type, String targetSchema) {
		if (linksOut != null) {
			for (Link link : linksOut) {
				if (!link.inactive && (type == null || link.type.equals(type)) && (targetSchema == null || link.targetSchema.equals(targetSchema))) {
					return link;
				}
			}
		}
		return null;
	}

	public Boolean hasActiveProximity() {
		if (linksOut != null) {
			for (Link link : linksOut) {
				if (link.type.equals(Constants.TYPE_LINK_PROXIMITY) && link.proximity != null) {
					Beacon beacon = (Beacon) EntityManager.getEntity(link.toId);
					if (beacon != null) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public Count getCount(String type, String schema, Boolean inactive, Direction direction) {
		List<Count> linkCounts = linksInCounts;
		if (direction == Direction.out) {
			linkCounts = linksOutCounts;
		}

		if (linkCounts != null) {
			for (Count linkCount : linkCounts) {
				if ((type == null || linkCount.type.equals(type))
						&& (schema == null || linkCount.schema.equals(schema))
						&& linkCount.inactive == inactive) {
					return linkCount;
				}
			}
		}
		return null;
	}

	public Link getLink(String type, String targetSchema, String targetId, Direction direction) {
		List<Link> links = linksIn;
		if (direction == Direction.out) {
			links = linksOut;
		}
		if (links != null) {
			for (Link link : links) {
				if (type == null || link.type.equals(type)) {
					if (targetSchema == null || link.targetSchema.equals(targetSchema)) {
						if (targetId == null || targetId.equals(direction == Direction.in ? link.fromId : link.toId)) {
							return link;
						}
					}
				}
			}
		}
		return null;
	}

	public Link removeLinksByTypeAndTargetSchema(String type, String targetSchema, String targetId, Direction direction) {
		List<Link> links = linksIn;
		if (direction == Direction.out) {
			links = linksOut;
		}
		if (links != null) {
			Iterator<Link> iterLinks = links.iterator();
			while (iterLinks.hasNext()) {
				Link link = iterLinks.next();
				if (link.type.equals(type) && link.targetSchema.equals(targetSchema)) {
					if (targetId == null || targetId.equals(direction == Direction.in ? link.fromId : link.toId)) {
						iterLinks.remove();
					}
				}
			}
		}
		return null;
	}

	public List<Shortcut> getShortcuts(ShortcutSettings settings, Comparator<ServiceBase> linkSorter, Comparator<Shortcut> shortcutSorter) {

		List<Shortcut> shortcuts = new ArrayList<Shortcut>();
		List<Link> links = settings.direction == Direction.in ? linksIn : linksOut;

		if (links != null) {
			if (linkSorter != null) {
				Collections.sort(links, linkSorter);
			}
			for (Link link : links) {
				if ((settings.linkType == null || link.type.equals(settings.linkType)) && link.shortcut != null) {
					if (settings.linkTargetSchema == null || (link.targetSchema.equals(settings.linkTargetSchema))) {
						if (settings.synthetic == null || link.shortcut.isSynthetic() == settings.synthetic) {
							/*
							 * Must clone or the groups added below will cause circular references
							 * that choke serializing to json.
							 */
							Shortcut shortcut = link.shortcut.clone();
							shortcut.inactive = link.inactive;
							shortcuts.add(shortcut);
						}
					}
				}
			}

			if (shortcutSorter != null) {
				Collections.sort(shortcuts, shortcutSorter);
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
					Count count = getCount(shortcut.linkType, shortcut.app, false, settings.direction);
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
				if (link.type.equals(linkType) && link.fromId.equals(Aircandi.getInstance().getCurrentUser().id)) {
					return true;
				}
			}
		}
		return false;
	}

	public List<Shortcut> getClientShortcuts() {
		/*
		 * Shortcuts are in shortcut.isActive() at draw time to determine if it gets shown
		 */
		List<Shortcut> shortcuts = new ArrayList<Shortcut>();
		/*
		 * For place only
		 */
		if (this.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {

			/* Picture */
			Shortcut shortcut = Shortcut.builder(this
					, Constants.SCHEMA_ENTITY_APPLINK
					, Constants.TYPE_APP_POST
					, Constants.ACTION_VIEW_AUTO
					, Aircandi.getInstance().getResources().getString(R.string.applink_name_pictures)
					, "resource:img_picture_temp"
					, 10
					, false
					, true);
			Link link = getLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PICTURE, null, Direction.in);
			if (link != null) {
				shortcut.photo = link.shortcut.getPhoto();
				shortcut.appId = link.fromId;
			}
			else {
				shortcut.photo.colorize = true;
				shortcut.photo.color = Aircandi.getInstance().getResources().getColor(Pictures.ICON_COLOR);
			}
			shortcut.linkType = Constants.TYPE_LINK_CONTENT;
			shortcuts.add(shortcut);

			/* Candigrams */
			shortcut = Shortcut.builder(this
					, Constants.SCHEMA_ENTITY_APPLINK
					, Constants.TYPE_APP_CANDIGRAM
					, Constants.ACTION_VIEW_AUTO
					, Aircandi.getInstance().getResources().getString(R.string.applink_name_candigrams)
					, "resource:img_candigram_temp"
					, 10
					, false
					, true);
			link = getLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_CANDIGRAM, null, Direction.in);
			if (link != null) {
				shortcut.photo = link.shortcut.getPhoto();
				shortcut.appId = link.fromId;
			}
			else {
				shortcut.photo.colorize = true;
				shortcut.photo.color = Aircandi.getInstance().getResources().getColor(Candigrams.ICON_COLOR);
			}
			shortcut.linkType = Constants.TYPE_LINK_CONTENT;
			shortcuts.add(shortcut);

			/* Maps: Map is evaluated in shortcut.isActive() at draw time to determine if it gets shown */
			shortcut = Shortcut.builder(this
					, Constants.SCHEMA_ENTITY_APPLINK
					, Constants.TYPE_APP_MAP
					, Constants.ACTION_VIEW
					, Aircandi.getInstance().getResources().getString(R.string.applink_name_map)
					, "resource:img_map_temp"
					, 30
					, false
					, true);
			shortcut.photo.colorize = true;
			shortcut.photo.color = Aircandi.getInstance().getResources().getColor(Maps.ICON_COLOR);
			shortcuts.add(shortcut);
		}
		/*
		 * For candigram only
		 */
		else if (this.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {

			/* Pictures */
			Shortcut shortcut = Shortcut.builder(this
					, Constants.SCHEMA_ENTITY_APPLINK
					, Constants.TYPE_APP_POST
					, Constants.ACTION_VIEW_AUTO
					, Aircandi.getInstance().getResources().getString(R.string.applink_name_pictures)
					, "resource:img_picture_temp"
					, 10
					, false
					, true);
			Link link = getLink(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PICTURE, null, Direction.in);
			if (link != null) {
				shortcut.photo = link.shortcut.getPhoto();
				shortcut.appId = link.fromId;
			}
			else {
				shortcut.photo.colorize = true;
				shortcut.photo.color = Aircandi.getInstance().getResources().getColor(Pictures.ICON_COLOR);
			}
			shortcut.linkType = Constants.TYPE_LINK_CONTENT;
			shortcuts.add(shortcut);

			/*
			 * Maps: Map is evaluated in shortcut.isActive() at draw time to determine if it gets shown
			 */
			shortcut = Shortcut.builder(this
					, Constants.SCHEMA_ENTITY_APPLINK
					, Constants.TYPE_APP_MAP
					, Constants.ACTION_VIEW
					, Aircandi.getInstance().getResources().getString(R.string.applink_name_map)
					, "resource:img_map_temp"
					, 30
					, false
					, true);
			shortcut.photo.colorize = true;
			shortcut.photo.color = Aircandi.getInstance().getResources().getColor(Maps.ICON_COLOR);
			shortcuts.add(shortcut);
		}

		/* Comments */
		Shortcut shortcut = Shortcut.builder(this
				, Constants.SCHEMA_ENTITY_APPLINK
				, Constants.TYPE_APP_COMMENT
				, Constants.ACTION_VIEW_FOR
				, Aircandi.getInstance().getResources().getString(R.string.applink_name_comments)
				, "resource:img_comment_temp"
				, 20
				, false
				, true);
		shortcut.photo.colorize = true;
		shortcut.photo.color = Aircandi.getInstance().getResources().getColor(Comments.ICON_COLOR);
		shortcut.linkType = Constants.TYPE_LINK_CONTENT;
		shortcuts.add(shortcut);

		return shortcuts;
	}

	public void removeLink() {}

	public String getSchemaMapped() {
		String schema = this.schema;
		if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
			schema = Constants.SCHEMA_REMAP_PICTURE;
		}
		return schema;
	}

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

			entity.hidden = (Boolean) (map.get("hidden") != null ? map.get("hidden") : false);
			entity.synthetic = (Boolean) (map.get("synthetic") != null ? map.get("synthetic") : false);
			entity.shortcuts = (Boolean) (map.get("shortcuts") != null ? map.get("shortcuts") : false);
			entity.checked = (Boolean) (map.get("checked") != null ? map.get("checked") : false);
			entity.placeId = (String) (nameMapping ? map.get("_place") : map.get("placeId"));
			entity.editing = (Boolean) (map.get("editing") != null ? map.get("checked") : false);

			entity.toId = (String) (nameMapping ? map.get("_to") : map.get("toId"));
			entity.fromId = (String) (nameMapping ? map.get("_from") : map.get("fromId"));

			if (map.get("photo") != null) {
				entity.photo = Photo.setPropertiesFromMap(new Photo(), (HashMap<String, Object>) map.get("photo"), nameMapping);
			}

			if (map.get("location") != null) {
				entity.location = AirLocation.setPropertiesFromMap(new AirLocation(), (HashMap<String, Object>) map.get("location"), nameMapping);
			}

			if (map.get("place") != null) {
				entity.place = Place.setPropertiesFromMap(new Place(), (HashMap<String, Object>) map.get("place"), nameMapping);
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
		if (place != null) {
			entity.place = place.clone();
		}
		return entity;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------	

}