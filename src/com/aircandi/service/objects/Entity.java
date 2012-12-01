package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.aircandi.components.EntityList;
import com.aircandi.components.GeoLocationManager;
import com.aircandi.components.GeoLocationManager.MeasurementSystem;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.Utilities;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.Expose;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.SerializedName;

/**
 * Entity as described by the proxi protocol standards.
 * 
 * @author Jayma
 */
public class Entity extends ServiceEntryBase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= -3902834532692561618L;

	/* Annotation syntax: @Expose (serialize = false, deserialize = false) */

	/* Database fields */

	@Expose
	public String				type;
	@Expose
	public String				subtitle;
	@Expose
	public String				description;
	@Expose
	public Photo				photo;
	@Expose
	public Place				place;
	@Expose
	public Number				signalFence			= -100.0f;
	@Expose
	public Boolean				isCollection;
	@Expose
	public Boolean				locked;
	@Expose
	public Boolean				enabled;
	@Expose
	public String				visibility			= "public";
	@Expose
	public List<Comment>		comments;

	@Expose(serialize = false, deserialize = true)
	public List<Link>			links;

	@Expose(serialize = false, deserialize = true)
	public Number				activityDate;

	/* Synthetic service fields */

	@Expose(serialize = false, deserialize = true)
	@SerializedName("_parent")
	public String				parentId;										/* Used to connect beacon object */

	@Expose(serialize = false, deserialize = true)
	public EntityList<Entity>	children;

	@Expose(serialize = false, deserialize = true)
	public Integer				childCount;

	@Expose(serialize = false, deserialize = true)
	public Boolean				childrenMore;

	@Expose(serialize = false, deserialize = true)
	public Integer				commentCount;

	@Expose(serialize = false, deserialize = true)
	public Boolean				commentsMore;

	/*
	 * For client use only
	 */

	/* These are all controlled by the parent in the case of child entities. */
	public Boolean				hidden				= false;
	public Boolean				global				= false;
	public Boolean				synthetic			= false;
	public Boolean				checked				= false;
	public Float				distance;										// Cached for easier sorting

	/* These have meaning for child entities */
	public Date					discoveryTime;

	public Entity() {}

	@Override
	public Entity clone() {
		try {
			final Entity entity = (Entity) super.clone();
			if (this.comments != null) {
				entity.comments = (List<Comment>) ((ArrayList) this.comments).clone();
			}
			if (this.links != null) {
				entity.links = (List<Link>) ((ArrayList) this.links).clone();
			}
			if (this.owner != null) {
				entity.owner = this.owner.clone();
			}
			if (this.creator != null) {
				entity.creator = this.creator.clone();
			}
			if (this.modifier != null) {
				entity.modifier = this.modifier.clone();
			}
			if (this.photo != null) {
				entity.photo = this.photo.clone();
			}
			if (this.place != null) {
				entity.place = this.place.clone();
			}
			return entity;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static Entity setPropertiesFromMap(Entity entity, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		entity = (Entity) ServiceEntryBase.setPropertiesFromMap(entity, map);

		entity.type = (String) map.get("type");
		entity.name = (String) map.get("name");
		entity.subtitle = (String) map.get("subtitle");
		entity.description = (String) map.get("description");
		entity.isCollection = (Boolean) map.get("isCollection");
		entity.locked = (Boolean) map.get("locked");
		entity.signalFence = (Number) map.get("signalFence");
		entity.visibility = (String) map.get("visibility");

		if (map.get("links") != null) {
			entity.links = new ArrayList<Link>();
			List<LinkedHashMap<String, Object>> linkMaps = (List<LinkedHashMap<String, Object>>) map.get("links");
			for (LinkedHashMap<String, Object> linkMap : linkMaps) {
				entity.links.add(Link.setPropertiesFromMap(new Link(), linkMap));
			}
		}

		if (map.get("comments") != null) {
			entity.comments = new ArrayList<Comment>();
			List<LinkedHashMap<String, Object>> commentMaps = (List<LinkedHashMap<String, Object>>) map.get("comments");
			for (LinkedHashMap<String, Object> commentMap : commentMaps) {
				entity.comments.add(Comment.setPropertiesFromMap(new Comment(), commentMap));
			}
		}
		entity.commentCount = (Integer) map.get("commentCount");
		entity.commentsMore = (Boolean) map.get("commentsMore");

		entity.children = new EntityList<Entity>();
		if (map.get("children") != null) {
			List<LinkedHashMap<String, Object>> childMaps = (List<LinkedHashMap<String, Object>>) map.get("children");
			for (LinkedHashMap<String, Object> childMap : childMaps) {
				entity.children.add(Entity.setPropertiesFromMap(new Entity(), childMap));
			}
		}
		entity.childCount = (Integer) map.get("childCount");
		entity.childrenMore = (Boolean) map.get("childrenMore");

		if (map.get("place") != null) {
			entity.place = (Place) Place.setPropertiesFromMap(new Place(), (HashMap<String, Object>) map.get("place"));
		}

		if (map.get("photo") != null) {
			entity.photo = (Photo) Photo.setPropertiesFromMap(new Photo(), (HashMap<String, Object>) map.get("photo"));
		}

		entity.parentId = (String) map.get("_parent");

		entity.activityDate = (Number) map.get("activityDate");

		return entity;
	}

	public static void copyProperties(Entity from, Entity to) {
		/*
		 * Properties are copied from one entity to another.
		 * 
		 * Local state properties we intentionally don't overwrite:
		 * 
		 * - children
		 * - hidden
		 * - global
		 * - rookie
		 * - discoveryTime
		 */
		ServiceEntryBase.copyProperties(from, to);

		to.type = from.type;
		to.name = from.name;
		to.subtitle = from.subtitle;
		to.description = from.description;

		to.parentId = from.parentId;

		to.place = from.place;
		to.photo = from.photo;
		to.locked = from.locked;
		to.isCollection = from.isCollection;
		to.signalFence = from.signalFence;
		to.visibility = from.visibility;

		to.links = from.links;

		to.childCount = from.childCount;
		to.comments = from.comments;
		to.commentCount = from.commentCount;
		to.commentsMore = from.commentsMore;

		to.activityDate = from.activityDate;
	}

	public Entity deepCopy() {
		/*
		 * A deep copy is created of the entire entity object using
		 * serialization/deserialization. All object properties are
		 * recreated as new instances
		 */
		Entity entityCopy = (Entity) Utilities.deepCopy(this);
		return entityCopy;
	}

	public String getCollection() {
		return "entities";
	}

	public Integer visibleChildrenCount() {
		int count = 0;
		for (Entity childEntity : getChildren()) {
			if (!childEntity.hidden) {
				count++;
			}
		}
		return count;
	}

	public GeoLocation getLocation() {
		GeoLocation location = null;
		Entity parent = getParent();
		if (parent != null) {
			location = parent.getLocation();
		}
		else {
			Beacon beacon = getBeacon();
			if (beacon != null) {
				location = beacon.getLocation();
			}
		}
		if (location == null && place != null && place.location != null) {
			location = new GeoLocation(place.location.lat.doubleValue(), place.location.lng.doubleValue());
		}
		return location;
	}

	public Float getDistance(MeasurementSystem system) {
		GeoLocation location = getLocation();
		Float distance = null;
		if (location != null) {
			Observation observation = GeoLocationManager.getInstance().getObservation();
			distance = (float) GeoLocationManager.distanceVincenty(observation.latitude.doubleValue()
					, observation.longitude.doubleValue()
					, location.latitude.doubleValue()
					, location.longitude.doubleValue()
					, system);
		}
		this.distance = distance;
		return distance;
	}

	public String getImageUri() {

		/*
		 * If a special preview photo is available, we use it otherwise
		 * we use the standard photo.
		 * 
		 * Only posts and collections do not have photo objects
		 */
		String imageUri = "resource:placeholder_logo";
		if (this.type.equals(CandiConstants.TYPE_CANDI_POST)) {
			if (this.creator.photo != null) {
				imageUri = this.creator.photo.getImageUri();
			}
			if (!imageUri.startsWith("http:") && !imageUri.startsWith("https:") && !imageUri.startsWith("resource:")) {
				imageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
			}
		}
		else {
			if (this.photo != null) {
				imageUri = photo.getImageSizedUri(250, 250);
				if (imageUri == null) {
					imageUri = photo.getImageUri();
				}
			}
			else if (creator != null) {
				if (creator.getImageUri() != null && !creator.getImageUri().equals("")) {
					imageUri = creator.getImageUri();
				}
			}
		}

		return imageUri;
	}

	public ImageFormat getImageFormat() {

		ImageFormat imageFormat = ImageFormat.Binary;

		if (this.photo != null) {
			imageFormat = photo.getImageFormat();
		}
		else if (creator != null && creator.photo != null) {
			if (creator.photo.getImageUri() != null && !creator.getImageUri().equals("")) {
				imageFormat = creator.photo.getImageFormat();
			}
		}

		return imageFormat;
	}

	public Link getActiveLink() {
		/*
		 * If an entity has more than one viable beaconLink, we choose the one
		 * that currently has the strongest beacon.
		 */
		if (links != null) {
			Link strongestLink = null;
			Integer strongestLevel = null;
			for (Link link : links) {
				if (strongestLink == null) {
					strongestLink = link;
					Beacon beacon = ProxiExplorer.getInstance().getEntityModel().getBeacon(link.toId);
					if (beacon != null) {
						strongestLevel = beacon.level.intValue();
					}
				}
				else {
					Beacon beacon = ProxiExplorer.getInstance().getEntityModel().getBeacon(link.toId);
					if (beacon != null && beacon.level.intValue() > strongestLevel) {
						strongestLink = link;
						strongestLevel = beacon.level.intValue();
					}
				}
			}
			return strongestLink;
		}
		return null;
	}

	public EntityList<Entity> getChildren() {
		EntityList<Entity> childEntities = ProxiExplorer.getInstance().getEntityModel().getChildren(this.id);
		return childEntities;
	}

	public Photo getPhoto() {
		if (photo == null) {
			photo = new Photo();
		}
		return photo;
	}

	public Place getPlace() {
		if (place == null) {
			place = new Place();
		}
		return place;
	}

	public Entity getParent() {
		Entity entity = ProxiExplorer.getInstance().getEntityModel().getCacheEntity(this.parentId);
		return entity;
	}

	public Beacon getBeacon() {
		Link link = getActiveLink();
		if (link != null) {
			Beacon beacon = ProxiExplorer.getInstance().getEntityModel().getBeacon(link.toId);
			return beacon;
		}
		return null;
	}

	public Integer getTuningScore() {
		Integer tuningScore = 0;
		if (links != null) {
			for (Link beaconLink : links) {
				/* One point for each link */
				tuningScore++;
				
				/* Bonus points for each tuned link */
				if (beaconLink.tuneCount != null) {
					tuningScore += beaconLink.tuneCount.intValue();
				}
			}
		}
		return tuningScore;
	}

	public String getBeaconId() {
		Link link = getActiveLink();
		if (link != null) {
			return link.toId;
		}
		return null;
	}

	public String getLinkId() {
		String linkId = getActiveLink().id;
		return linkId;
	}

	public String getCategories() {
		if (place != null && place.categories != null && place.categories.size() > 0) {
			String categories = "";
			for (Category category : place.categories) {
				categories += category.name + ", ";
			}
			categories = categories.substring(0, categories.length() - 2);
			return categories;
		}
		return null;
	}

	public static enum ImageFormat {
		Binary, Html
	}

	public static enum EntityState {
		Normal,
		New,
		Refreshed,
		Missing
	}

	public static enum Visibility {
		Public,
		Private
	}

	public static enum Source {
		Aircandi,
		Foursquare,
		User
	}

	public static class SortEntitiesByTuningScoreDistance implements Comparator<Entity> {

		@Override
		public int compare(Entity entity1, Entity entity2) {

			/* global versus user */
			if (!entity1.global && entity2.global) {
				return -1;
			}
			if (entity1.global && !entity2.global) {
				return 1;
			}
			else {
				/* synthetics */
				if (!entity1.synthetic && entity2.synthetic) {
					return -1;
				}
				if (entity1.synthetic && !entity2.synthetic) {
					return 1;
				}
				else {
					if (entity1.getTuningScore() > entity2.getTuningScore()) {
						return -1;
					}
					if (entity1.getTuningScore() < entity2.getTuningScore()) {
						return 1;
					}
					else {
						if (entity1.distance < entity2.distance.intValue()) {
							return -1;
						}
						else if (entity1.distance.intValue() > entity2.distance.intValue()) {
							return 1;
						}
						else {
							return 0;
						}
					}
				}
			}
		}
	}

	public static class SortEntitiesByModifiedDate implements Comparator<Entity> {

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

}