package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.aircandi.components.EntityList;
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
public class Entity extends ServiceEntry implements Cloneable, Serializable {

	private static final long	serialVersionUID	= -3902834532692561618L;

	/* Annotation syntax: @Expose (serialize = false, deserialize = false) */

	/* Database fields */

	@Expose
	public String				type;
	@Expose
	public Boolean				root;
	@Expose
	public String				uri;
	@Expose
	public String				label;
	@Expose
	public String				title;
	@Expose
	public String				subtitle;
	@Expose
	public String				description;
	@Expose
	public String				imageUri;
	@Expose
	public String				imagePreviewUri;
	@Expose
	public String				linkUri;
	@Expose
	public String				linkPreviewUri;
	@Expose
	public Boolean				linkZoom;
	@Expose
	public Boolean				linkJavascriptEnabled;
	@Expose
	public Number				signalFence			= -200f;
	@Expose
	public Boolean				locked;
	@Expose
	public Boolean				enabled;
	@Expose
	public String				visibility			= "public";
	@Expose
	public List<Comment>		comments;
	@Expose(serialize = false, deserialize = true)
	public Number				activityDate;

	/* Synthetic service fields */

	@Expose(serialize = false, deserialize = true)
	@SerializedName("_beacon")
	public String				beaconId;										/* Used to connect beacon object */

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

	@Expose(serialize = false, deserialize = true)
	public GeoLocation			location;

	@Expose(serialize = false, deserialize = true)
	public Place				place;
	/*
	 * For client use only
	 */

	/* These are all controlled by the parent in the case of child entities. */
	public Boolean				hidden				= false;
	public Boolean				global				= false;
	public Boolean				synthetic			= false;

	/* These have meaning for child entities */
	public Boolean				rookie				= true;
	public Date					discoveryTime;

	public Entity() {}

	@Override
	public Entity clone() {
		try {
			final Entity entity = (Entity) super.clone();
			if (this.comments != null) {
				entity.comments = (List<Comment>) ((ArrayList) this.comments).clone();
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
			return entity;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public Entity copy() {
		/*
		 * We make sure that all entities in the children and parents
		 * collections are new entity objects. Any other object properties on
		 * the entity object are still references to the same instance including:
		 * 
		 * - Beacon object
		 * - Comments in the comment list
		 * - GeoLocation
		 */
		try {
			final Entity entity = (Entity) super.clone();
			if (this.comments != null) {
				entity.comments = (List<Comment>) ((ArrayList) this.comments).clone();
			}
			return entity;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
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
		ServiceEntry.copyProperties(from, to);

		to.type = from.type;
		to.title = from.title;
		to.label = from.label;
		to.subtitle = from.subtitle;
		to.description = from.description;
		to.uri = from.uri;

		to.linkJavascriptEnabled = from.linkJavascriptEnabled;
		to.linkZoom = from.linkZoom;

		to.imageUri = from.imageUri;
		to.imagePreviewUri = from.imagePreviewUri;
		to.linkUri = from.linkUri;
		to.linkPreviewUri = from.linkPreviewUri;

		to.parentId = from.parentId;
		to.beaconId = from.beaconId;

		to.location = from.location;
		to.place = from.place;
		to.locked = from.locked;
		to.signalFence = from.signalFence;
		to.visibility = from.visibility;

		to.comments = from.comments;
		to.commentCount = from.commentCount;
		to.commentsMore = from.commentsMore;

		to.activityDate = from.activityDate;
	}

	public static Entity setFromPropertiesFromMap(Entity entity, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		entity = (Entity) ServiceEntry.setFromPropertiesFromMap(entity, map);

		entity.type = (String) map.get("type");
		entity.uri = (String) map.get("uri");
		entity.root = (Boolean) map.get("root");
		entity.title = (String) map.get("title");
		entity.label = (String) map.get("label");
		entity.subtitle = (String) map.get("subtitle");
		entity.description = (String) map.get("description");
		entity.linkUri = (String) map.get("linkUri");
		entity.linkPreviewUri = (String) map.get("linkPreviewUri");
		entity.linkJavascriptEnabled = (Boolean) map.get("linkJavascriptEnabled");
		entity.linkZoom = (Boolean) map.get("linkZoom");
		entity.locked = (Boolean) map.get("locked");
		entity.imagePreviewUri = (String) map.get("imagePreviewUri");
		entity.imageUri = (String) map.get("imageUri");
		entity.signalFence = (Number) map.get("signalFence");
		entity.visibility = (String) map.get("visibility");

		if (map.get("comments") != null) {
			entity.comments = new ArrayList<Comment>();
			List<LinkedHashMap<String, Object>> commentMaps = (List<LinkedHashMap<String, Object>>) map.get("comments");
			for (LinkedHashMap<String, Object> commentMap : commentMaps) {
				entity.comments.add(Comment.setFromPropertiesFromMap(new Comment(), commentMap));
			}
		}
		entity.commentCount = (Integer) map.get("commentCount");
		entity.commentsMore = (Boolean) map.get("commentsMore");

		entity.children = new EntityList<Entity>();
		if (map.get("children") != null) {
			List<LinkedHashMap<String, Object>> childMaps = (List<LinkedHashMap<String, Object>>) map.get("children");
			for (LinkedHashMap<String, Object> childMap : childMaps) {
				entity.children.add(Entity.setFromPropertiesFromMap(new Entity(), childMap));
			}
		}
		entity.childCount = (Integer) map.get("childCount");
		entity.childrenMore = (Boolean) map.get("childrenMore");

		if (map.get("location") != null) {
			entity.location = (GeoLocation) GeoLocation.setFromPropertiesFromMap(new GeoLocation(), (HashMap<String, Object>) map.get("location"));
		}

		if (map.get("place") != null) {
			entity.place = (Place) Place.setFromPropertiesFromMap(new Place(), (HashMap<String, Object>) map.get("place"));
		}
		
		entity.parentId = (String) map.get("_parent");
		entity.beaconId = (String) map.get("_beacon");

		entity.activityDate = (Number) map.get("activityDate");

		return entity;
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

	public boolean hasVisibleChildren() {
		for (Entity childEntity : getChildren()) {
			if (!childEntity.hidden) {
				return true;
			}
		}
		return false;
	}

	public String getMasterImageUri() {
		/*
		 * Special case where type==post
		 */
		String masterImageUri = null;
		if (this.type.equals(CandiConstants.TYPE_CANDI_POST)) {
			masterImageUri = this.creator.imageUri;
			if (!masterImageUri.startsWith("http:") && !masterImageUri.startsWith("https:") && !masterImageUri.startsWith("resource:")) {
				masterImageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + this.creator.imageUri;
			}
		}
		else {
			if (this.synthetic) {
				masterImageUri = imagePreviewUri;
			}
			else if (imagePreviewUri != null && !imagePreviewUri.equals("")) {
				if (!imagePreviewUri.toLowerCase().startsWith("resource:")) {
					masterImageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imagePreviewUri;
				}
				else {
					masterImageUri = imagePreviewUri;
				}
			}
			else if (linkPreviewUri != null && !linkPreviewUri.equals("")) {
				masterImageUri = linkPreviewUri;
			}
			else if (linkUri != null && !linkUri.equals("")) {
				masterImageUri = linkUri;
			}
			else if (creator != null) {
				if (creator.imageUri != null && !creator.imageUri.equals("")) {
					masterImageUri = creator.imageUri;
				}
				else if (creator.linkUri != null && !creator.linkUri.equals("")) {
					masterImageUri = creator.linkUri;
				}
			}
		}
		return masterImageUri;
	}

	public ImageFormat getMasterImageFormat() {

		/*
		 * Special case where type==post
		 */
		ImageFormat imageFormat = ImageFormat.Binary;
		if (this.type.equals(CandiConstants.TYPE_CANDI_POST)) {
			imageFormat = ImageFormat.Binary;
		}
		else {

			if (imagePreviewUri != null && !imagePreviewUri.equals("")) {
				imageFormat = ImageFormat.Binary;
			}
			else if (linkPreviewUri != null && !linkPreviewUri.equals("")) {
				imageFormat = ImageFormat.Html;
			}
			else if (linkUri != null && !linkUri.equals("")) {
				imageFormat = ImageFormat.Html;
			}
			else if (creator != null) {
				if (creator.imageUri != null && !creator.imageUri.equals("")) {
					imageFormat = ImageFormat.Binary;
				}
				else if (creator.linkUri != null && !creator.linkUri.equals("")) {
					imageFormat = ImageFormat.Html;
				}
			}
		}
		return imageFormat;
	}

	public EntityList<Entity> getChildren() {
		EntityList<Entity> childEntities = ProxiExplorer.getInstance().getEntityModel().getChildren(this.id);
		return childEntities;
	}

	public Entity getParent() {
		Entity entity = ProxiExplorer.getInstance().getEntityModel().getEntity(this.parentId);
		return entity;
	}

	public Beacon getBeacon() {
		Beacon beacon = ProxiExplorer.getInstance().getEntityModel().getBeacon(this.beaconId);
		return beacon;
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
}