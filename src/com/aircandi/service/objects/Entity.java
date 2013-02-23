package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aircandi.CandiConstants;
import com.aircandi.ProxiConstants;
import com.aircandi.components.LocationManager;
import com.aircandi.components.ProxiManager;
import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;

/**
 * Entity as described by the proxi protocol standards.
 * 
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Entity extends ServiceEntryBase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= -3902834532692561618L;

	/* Annotation syntax: @Expose (serialize = false, deserialize = false) */

	/* Database fields */

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
	@Expose
	public List<Source>			sources;

	@Expose(serialize = false, deserialize = true)
	public List<Link>			links;

	@Expose(serialize = false, deserialize = true)
	public Number				activityDate;

	/* Synthetic service fields */

	@Expose(serialize = false, deserialize = true)
	@SerializedName("_parent")
	public String				parentId;										/* Used to connect beacon object */

	@Expose(serialize = false, deserialize = true)
	public List<Entity>	children;

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
	public Boolean				synthetic			= false;
	public Boolean				checked				= false;

	/* Stash for place entities */
	public List<Source>			sourceSuggestions;

	/* Used when this is a source entity */
	public Source				source;

	/* Cached for easier sorting */
	public Float				distance;

	public Entity() {}

	@Override
	public Entity clone() {
		try {
			final Entity entity = (Entity) super.clone();
			if (comments != null) {
				entity.comments = (List<Comment>) ((ArrayList) comments).clone();
			}
			if (sources != null) {
				entity.sources = (List<Source>) ((ArrayList) sources).clone();
			}
			if (links != null) {
				entity.links = (List<Link>) ((ArrayList) links).clone();
			}
			if (owner != null) {
				entity.owner = owner.clone();
			}
			if (creator != null) {
				entity.creator = creator.clone();
			}
			if (modifier != null) {
				entity.modifier = modifier.clone();
			}
			if (photo != null) {
				entity.photo = photo.clone();
			}
			if (place != null) {
				entity.place = place.clone();
			}
			return entity;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static Entity setPropertiesFromMap(Entity entity, Map map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		entity = (Entity) ServiceEntryBase.setPropertiesFromMap(entity, map);

		entity.name = (String) map.get("name");
		entity.subtitle = (String) map.get("subtitle");
		entity.description = (String) map.get("description");
		entity.isCollection = (Boolean) map.get("isCollection");
		entity.locked = (Boolean) map.get("locked");
		entity.signalFence = (Number) map.get("signalFence");
		entity.visibility = (String) map.get("visibility");

		if (map.get("links") != null) {
			entity.links = new ArrayList<Link>();
			final List<LinkedHashMap<String, Object>> linkMaps = (List<LinkedHashMap<String, Object>>) map.get("links");
			for (Map<String,Object> linkMap : linkMaps) {
				entity.links.add(Link.setPropertiesFromMap(new Link(), linkMap));
			}
		}

		if (map.get("sources") != null) {
			entity.sources = new ArrayList<Source>();
			final List<LinkedHashMap<String, Object>> sourceMaps = (List<LinkedHashMap<String, Object>>) map.get("sources");
			for (Map<String,Object> sourceMap : sourceMaps) {
				entity.sources.add(Source.setPropertiesFromMap(new Source(), sourceMap));
			}
		}

		if (map.get("comments") != null) {
			entity.comments = new ArrayList<Comment>();
			final List<LinkedHashMap<String, Object>> commentMaps = (List<LinkedHashMap<String, Object>>) map.get("comments");
			for (Map<String,Object> commentMap : commentMaps) {
				entity.comments.add(Comment.setPropertiesFromMap(new Comment(), commentMap));
			}
		}

		entity.commentCount = (Integer) map.get("commentCount");
		entity.commentsMore = (Boolean) map.get("commentsMore");

		entity.children = new ArrayList<Entity>();
		if (map.get("children") != null) {
			final List<LinkedHashMap<String, Object>> childMaps = (List<LinkedHashMap<String, Object>>) map.get("children");
			for (Map<String,Object> childMap : childMaps) {
				entity.children.add(Entity.setPropertiesFromMap(new Entity(), childMap));
			}
		}
		entity.childCount = (Integer) map.get("childCount");
		entity.childrenMore = (Boolean) map.get("childrenMore");

		if (map.get("place") != null) {
			entity.place = Place.setPropertiesFromMap(new Place(), (HashMap<String, Object>) map.get("place"));
		}

		if (map.get("photo") != null) {
			entity.photo = Photo.setPropertiesFromMap(new Photo(), (HashMap<String, Object>) map.get("photo"));
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
		to.sources = from.sources;
		to.commentCount = from.commentCount;
		to.commentsMore = from.commentsMore;

		to.activityDate = from.activityDate;
	}

	@Override
	public String getCollection() {
		return "entities";
	}

	public static Entity upsizeFromSynthetic(Entity synthetic) {
		/*
		 * Sythetic entity created from foursquare data
		 * 
		 * We make a copy so these changes don't effect the synthetic entity
		 * in the entity model in case we keep it because of a failure.
		 */
		final Entity entity = synthetic.clone();
		entity.id = null;
		entity.locked = false;
		if (synthetic.place.category != null) {
			entity.subtitle = synthetic.place.category.name;
		}
		return entity;
	}

	public Photo getPhotoForSet() {
		if (photo == null) {
			photo = new Photo();
		}
		return photo;
	}

	public Photo getPhoto() {
		return (photo != null) ? photo : new Photo();
	}

	public void setPhoto(Photo photo) {
		this.photo = photo;
	}

	public GeoLocation getLocation() {
		GeoLocation location = null;
		final Entity parent = getParent();
		if (parent != null) {
			location = parent.getLocation();
		}
		else {
			final Beacon beacon = getActivePrimaryBeacon("proximity");
			if (beacon != null) {
				location = beacon.getLocation();
			}
		}
		if (location == null && place != null && place.location != null) {
			location = new GeoLocation(place.location.lat.doubleValue(), place.location.lng.doubleValue());
		}
		return location;
	}

	public Float getDistance() {
		distance = -1f;
		final Beacon beacon = getActivePrimaryBeacon("proximity");
		if (beacon != null) {
			distance = beacon.getDistance();
		}
		else {
			final GeoLocation location = getLocation();
			if (location != null) {
				final Observation observation = LocationManager.getInstance().getObservationLocked();
				if (observation != null) {
					Float distanceByLocation = 0f;
					final android.location.Location locationObserved = new android.location.Location(observation.provider);
					locationObserved.setLatitude(observation.latitude.doubleValue());
					locationObserved.setLongitude(observation.longitude.doubleValue());

					final android.location.Location locationPlace = new android.location.Location("place");
					locationPlace.setLatitude(location.latitude.doubleValue());
					locationPlace.setLongitude(location.longitude.doubleValue());

					distanceByLocation = locationObserved.distanceTo(locationPlace);

					distance = distanceByLocation;
				}
			}
		}
		return distance;
	}

	public String getEntityPhotoUri() {

		/*
		 * If a special preview photo is available, we use it otherwise
		 * we use the standard photo.
		 * 
		 * Only posts and collections do not have photo objects
		 */
		String imageUri = "resource:img_placeholder_logo_bw";
		if (type.equals(CandiConstants.TYPE_CANDI_POST)) {
			if (creator != null) {
				if (creator.getUserPhotoUri() != null && !creator.getUserPhotoUri().equals("")) {
					imageUri = creator.getUserPhotoUri();
				}
			}
			if (!imageUri.startsWith("http:") && !imageUri.startsWith("https:") && !imageUri.startsWith("resource:")) {
				imageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
			}
		}
		else if (type.equals(CandiConstants.TYPE_CANDI_SOURCE)) {
			final String sourceImageUri = source.getImageUri();
			if (sourceImageUri != null) {
				imageUri = source.getImageUri();
			}
		}
		else if (type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
			if (photo != null) {
				imageUri = photo.getSizedUri(250, 250); // sizing ignored if source doesn't support it
				if (imageUri == null) {
					imageUri = photo.getUri();
				}
			}
			else if (place != null && place.category != null) {
				imageUri = place.category.iconUri();
			}
		}
		else {
			if (photo != null) {
				imageUri = photo.getSizedUri(250, 250); // sizing ignored if source doesn't support it
				if (imageUri == null) {
					imageUri = photo.getUri();
				}
			}
		}

		return imageUri;
	}

	private Link getActiveLink(String linkType, Boolean primaryOnly) {
		/*
		 * If an entity has more than one viable link, we choose the one
		 * using the following priority:
		 * 
		 * - strongest primary
		 * - any primary
		 * - any non-primary
		 */
		if (links != null) {
			Link strongestLink = null;
			Integer strongestLevel = -200;
			for (Link link : links) {
				if (link.type.equals(linkType)) {
					if (link.primary != null && link.primary) {
						Beacon beacon = ProxiManager.getInstance().getEntityModel().getBeacon(link.toId);
						if (beacon != null && beacon.level.intValue() > strongestLevel) {
							strongestLink = link;
							strongestLevel = beacon.level.intValue();
						}
					}
				}
			}

			if (strongestLink == null && !primaryOnly) {
				for (Link link : links) {
					if (link.type.equals(linkType)) {
						Beacon beacon = ProxiManager.getInstance().getEntityModel().getBeacon(link.toId);
						if (beacon != null && beacon.level.intValue() > strongestLevel) {
							strongestLink = link;
							strongestLevel = beacon.level.intValue();
						}
					}
				}
			}

			return strongestLink;
		}
		return null;
	}

	public List<Entity> getChildren() {
		return ProxiManager.getInstance().getEntityModel().getChildEntities(id);
	}

	public List<Entity> getSourceEntities() {
		return ProxiManager.getInstance().getEntityModel().getSourceEntities(id);
	}

	public Place getPlace() {
		if (place == null) {
			place = new Place();
		}
		return place;
	}

	public Entity getParent() {
		return ProxiManager.getInstance().getEntityModel().getCacheEntity(parentId);
	}

	public Beacon getActivePrimaryBeacon(String linkType) {
		final Link link = getActiveLink(linkType, true);
		if (link != null) {
			final Beacon beacon = ProxiManager.getInstance().getEntityModel().getBeacon(link.toId);
			return beacon;
		}
		return null;
	}

	public Link getLink(Beacon beacon, String linkType) {
		if (links != null) {
			for (Link link : links) {
				if (link.toId.equals(beacon.id) && link.type.equals(linkType)) {
					return link;
				}
			}
		}
		return null;
	}

	public Boolean hasProximityLink() {
		if (links != null) {
			for (Link link : links) {
				if (link.primary && link.type.equals("proximity")) {
					return true;
				}
			}
		}
		return false;
	}

	public Integer getPlaceRankScore() {
		/*
		 * Place entities can get high scores in the following ways:
		 * 
		 * 1) Proximity: they have been tagged as physically near/at a beacon we can currently see.
		 * 2) Popular: they have been browsed a lot in range of a beacon we can currently see.
		 * 
		 * Inputs:
		 * - How many primary links are there.
		 * - Is the link a proximity or browse type.
		 * - How many times has the link been 'voted' on.
		 * - How close to a primary is the device. (signal level)
		 * 
		 * Only primary links get tuning points.
		 * A place entity can have multiple primary links as it gets extended.
		 */
		Integer placeRankScore = 0;
		if (links != null) {
			for (Link link : links) {
				/* Add points for being close to a primary */
				if (link.primary) {

					/* Bonus points for each tuned link */
					if (link.tuneCount != null) {
						if (link.type.equals("proximity")) {
							placeRankScore += (link.tuneCount.intValue() * 5);

							Beacon beacon = ProxiManager.getInstance().getEntityModel().getBeacon(link.toId);
							if (beacon != null) {
								int level = beacon.level.intValue();

								if (level <= -90) {
									placeRankScore += 1;
								}
								else if (level <= -80) {
									placeRankScore += 2;
								}
								else if (level <= -70) {
									placeRankScore += 4;
								}
								else {
									placeRankScore += 8;
								}
							}
						}
						else if (link.type.equals("browse")) {
							//placeRankScore += (link.tuneCount.intValue() * 1);
						}
					}
				}
			}
		}
		return placeRankScore;
	}

	public Integer getPlaceRankImpact() {
		final int placeRankScore = getPlaceRankScore();
		return (placeRankScore >= 5) ? placeRankScore : 0;
	}

	public String getBeaconId() {
		final Link link = getActiveLink("proximity", true);
		if (link != null) {
			return link.toId;
		}
		return null;
	}

	//	public String getSourcesAsText() {
	//		if (sources != null && sources.size() > 0) {
	//			String sourcesAsText = "";
	//			for (Source source : sources) {
	//				sourcesAsText += source.caption + ", ";
	//			}
	//			sourcesAsText = sourcesAsText.substring(0, sourcesAsText.length() - 2);
	//			return sourcesAsText;
	//		}
	//		return null;
	//	}

	@SuppressWarnings("ucd")
	public static enum Visibility {
		Public,
		Private
	}

	public static class SortEntitiesByProximityAndDistance implements Comparator<Entity> {

		@Override
		public int compare(Entity entity1, Entity entity2) {

			if (entity1.hasProximityLink() && !entity2.hasProximityLink()) {
				return -1;
			}
			else if (entity2.hasProximityLink() && !entity1.hasProximityLink()) {
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

	@SuppressWarnings("ucd")
	public static class SortEntitiesByDistance implements Comparator<Entity> {

		@Override
		public int compare(Entity entity1, Entity entity2) {

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

	public static class SortEntitiesBySourcePosition implements Comparator<Entity> {

		@Override
		public int compare(Entity entity1, Entity entity2) {

			if (entity1.source.position < entity2.source.position) {
				return -1;
			}
			if (entity1.source.position.equals(entity2.source.position)) {
				return 0;
			}
			return 1;
		}
	}

	public static class SortEntitiesByModifiedDate implements Comparator<Entity> {

		@Override
		public int compare(Entity entity1, Entity entity2) {

			if (!entity1.type.equals(CandiConstants.TYPE_CANDI_SOURCE) && entity2.type.equals(CandiConstants.TYPE_CANDI_SOURCE)) {
				return 1;
			}
			if (entity1.type.equals(CandiConstants.TYPE_CANDI_SOURCE) && !entity2.type.equals(CandiConstants.TYPE_CANDI_SOURCE)) {
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
}