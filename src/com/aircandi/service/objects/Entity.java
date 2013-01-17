package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.aircandi.CandiConstants;
import com.aircandi.ProxiConstants;
import com.aircandi.components.EntityList;
import com.aircandi.components.LocationManager;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;
import com.aircandi.utilities.MiscUtils;

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
			if (this.comments != null) {
				entity.comments = (List<Comment>) ((ArrayList) this.comments).clone();
			}
			if (this.sources != null) {
				entity.sources = (List<Source>) ((ArrayList) this.sources).clone();
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

		if (map.get("sources") != null) {
			entity.sources = new ArrayList<Source>();
			List<LinkedHashMap<String, Object>> sourceMaps = (List<LinkedHashMap<String, Object>>) map.get("sources");
			for (LinkedHashMap<String, Object> sourceMap : sourceMaps) {
				entity.sources.add(Source.setPropertiesFromMap(new Source(), sourceMap));
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
		to.sources = from.sources;
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
		Entity entityCopy = (Entity) MiscUtils.deepCopy(this);
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

	public static Entity upsizeFromSynthetic(Entity synthetic) {
		/*
		 * Sythetic entity created from foursquare data
		 * 
		 * We make a copy so these changes don't effect the synthetic entity
		 * in the entity model in case we keep it because of a failure.
		 */
		Entity entity = synthetic.clone();
		entity.id = null;
		entity.subtitle = synthetic.getCategoriesAsText();
		return entity;
	}

	public Photo getPhotoForSet() {
		if (photo == null) {
			photo = new Photo();
		}
		return photo;
	}

	public Photo getPhoto() {
		return photo != null ? photo : new Photo();
	}

	public void setPhoto(Photo photo) {
		this.photo = photo;
	}

	public Source getSource(String targetSource) {
		for (Source source : sources) {
			if (source.source.equals(targetSource)) {
				return source;
			}
		}
		return null;
	}

	public GeoLocation getLocation() {
		GeoLocation location = null;
		Entity parent = getParent();
		if (parent != null) {
			location = parent.getLocation();
		}
		else {
			Beacon beacon = getActiveBeacon("proximity");
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
		this.distance = -1f;
		Beacon beacon = getActiveBeacon("proximity");
		if (beacon != null) {
			this.distance = beacon.getDistance();
		}
		else {
			GeoLocation location = getLocation();
			if (location != null) {
				Observation observation = LocationManager.getInstance().getObservation();
				if (observation != null) {
					Float distanceByLocation = 0f;
					android.location.Location locationObserved = new android.location.Location(observation.provider);
					locationObserved.setLatitude(observation.latitude.doubleValue());
					locationObserved.setLongitude(observation.longitude.doubleValue());
					android.location.Location locationPlace = new android.location.Location("place");
					locationPlace.setLatitude(location.latitude.doubleValue());
					locationPlace.setLongitude(location.longitude.doubleValue());
					distanceByLocation = locationObserved.distanceTo(locationPlace);
					this.distance = distanceByLocation;
				}
			}
		}
		return this.distance;
	}

	public String getEntityPhotoUri() {

		/*
		 * If a special preview photo is available, we use it otherwise
		 * we use the standard photo.
		 * 
		 * Only posts and collections do not have photo objects
		 */
		String imageUri = "resource:placeholder_logo_bw";
		if (this.type.equals(CandiConstants.TYPE_CANDI_POST)) {
			if (this.creator.photo != null) {
				imageUri = this.creator.photo.getUri();
			}
			if (!imageUri.startsWith("http:") && !imageUri.startsWith("https:") && !imageUri.startsWith("resource:")) {
				imageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
			}
		}
		else if (this.type.equals(CandiConstants.TYPE_CANDI_LINK)) {
			imageUri = "resource:source_website_ii";
		}
		else if (this.type.equals(CandiConstants.TYPE_CANDI_SOURCE)) {
			imageUri = this.source.getImageUri();
		}
		else if (this.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
			if (this.photo != null) {
				imageUri = photo.getSizedUri(250, 250); // sizing ignored if source doesn't support it
				if (imageUri == null) {
					imageUri = photo.getUri();
				}
			}
			else if (this.place != null
					&& this.place.categories != null
					&& this.place.categories.size() > 0) {
				imageUri = this.place.categories.get(0).iconUri();
			}
			else if (creator != null) {
				if (creator.getUserPhotoUri() != null && !creator.getUserPhotoUri().equals("")) {
					imageUri = creator.getUserPhotoUri();
				}
			}
		}
		else {
			if (this.photo != null) {
				imageUri = photo.getSizedUri(250, 250); // sizing ignored if source doesn't support it
				if (imageUri == null) {
					imageUri = photo.getUri();
				}
			}
			else if (creator != null) {
				if (creator.getUserPhotoUri() != null && !creator.getUserPhotoUri().equals("")) {
					imageUri = creator.getUserPhotoUri();
				}
			}
		}

		return imageUri;
	}

	public Link getActiveLink(String linkType) {
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
						Beacon beacon = ProxiExplorer.getInstance().getEntityModel().getBeacon(link.toId);
						if (beacon != null && beacon.level.intValue() > strongestLevel) {
							strongestLink = link;
							strongestLevel = beacon.level.intValue();
						}
					}
				}
			}

			if (strongestLink == null) {
				for (Link link : links) {
					if (link.type.equals(linkType)) {
						Beacon beacon = ProxiExplorer.getInstance().getEntityModel().getBeacon(link.toId);
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

	public EntityList<Entity> getChildren() {
		EntityList<Entity> entities = ProxiExplorer.getInstance().getEntityModel().getChildEntities(this.id);
		return entities;
	}
	
	public EntityList<Entity> getSourceEntities() {
		EntityList<Entity> entities = ProxiExplorer.getInstance().getEntityModel().getSourceEntities(this.id);
		return entities;
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

	public Beacon getActiveBeacon(String linkType) {
		Link link = getActiveLink(linkType);
		if (link != null) {
			Beacon beacon = ProxiExplorer.getInstance().getEntityModel().getBeacon(link.toId);
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
				if (link.primary) {
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

							Beacon beacon = ProxiExplorer.getInstance().getEntityModel().getBeacon(link.toId);
							if (beacon != null) {
								int level = beacon.level.intValue();

								if (level <= -90)
									placeRankScore += 1;
								else if (level <= -80)
									placeRankScore += 2;
								else if (level <= -70)
									placeRankScore += 4;
								else
									placeRankScore += 8;
							}
						}
						else if (link.type.equals("browse")) {
							placeRankScore += (link.tuneCount.intValue() * 1);
						}
					}
				}
			}
		}
		return placeRankScore;
	}

	public String getBeaconId() {
		Link link = getActiveLink("proximity");
		if (link != null) {
			return link.toId;
		}
		return null;
	}

	public String getCategoriesAsText() {
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

	public String getSourcesAsText() {
		if (sources != null && sources.size() > 0) {
			String sourcesAsText = "";
			for (Source source : sources) {
				sourcesAsText += source.name + ", ";
			}
			sourcesAsText = sourcesAsText.substring(0, sourcesAsText.length() - 2);
			return sourcesAsText;
		}
		return null;
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

	public static class SortEntitiesByPlaceRankScoreDistance implements Comparator<Entity> {

		@Override
		public int compare(Entity entity1, Entity entity2) {

			/* synthetics */
			if (!entity1.synthetic && entity2.synthetic) {
				return -1;
			}
			if (entity1.synthetic && !entity2.synthetic) {
				return 1;
			}
			else {
				if (entity1.getPlaceRankScore() > entity2.getPlaceRankScore()) {
					return -1;
				}
				if (entity1.getPlaceRankScore() < entity2.getPlaceRankScore()) {
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

	public static class SortEntitiesBySourcePosition implements Comparator<Entity> {

		@Override
		public int compare(Entity entity1, Entity entity2) {

			if (entity1.source.position < entity2.source.position) {
				return -1;
			}
			if (entity1.source.position == entity2.source.position) {
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
					return -1;
				}
				else if (entity1.modifiedDate.longValue() == entity2.modifiedDate.longValue()) {
					return 0;
				}
				return 1;
			}
		}
	}
}