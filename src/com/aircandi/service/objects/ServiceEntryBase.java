package com.aircandi.service.objects;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aircandi.ProxiConstants;
import com.aircandi.beta.BuildConfig;
import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;

/*
 * Initial user settings (owner,creator):
 * 
 * entities: user, user
 * links: user, user
 * sessions: user, user
 * users: user, admin
 * 
 * beacons: admin, user
 * documents: admin, admin
 * observations: admin, user
 */

@SuppressWarnings("ucd")
public abstract class ServiceEntryBase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 5341986472204947191L;

	/*
	 * Annotation syntax:
	 * 
	 * @Expose (serialize = false, deserialize = false)
	 * 
	 * @SerializedName("_nametoserialize")
	 * 
	 * These annotations are only used when serializing. Each
	 * object has a setPropertiesFromMap method that handles how properties
	 * are deserialized.
	 */
	@Expose
	@SerializedName("_id")
	public String				id;

	@Expose
	public String				name;
	@Expose
	public String				type;

	@Expose(serialize = false, deserialize = true)
	public String				namelc;

	/* Property bag */

	@Expose
	public Map<String, Object>	data;

	/* User ids */

	@Expose(serialize = false, deserialize = true)
	@SerializedName("_owner")
	public String				ownerId;

	@Expose(serialize = false, deserialize = true)
	@SerializedName("_creator")
	public String				creatorId;

	@Expose(serialize = false, deserialize = true)
	@SerializedName("_modifier")
	public String				modifierId;

	/* Dates */

	@Expose(serialize = false, deserialize = true)
	public Number				createdDate;

	@Expose(serialize = false, deserialize = true)
	public Number				modifiedDate;

	/* Users (client) */

	@Expose(serialize = false, deserialize = true)
	public User					owner;

	@Expose(serialize = false, deserialize = true)
	public User					creator;

	@Expose(serialize = false, deserialize = true)
	public User					modifier;

	@Expose(serialize = false, deserialize = true)
	public Number				activityDate;

	@Expose(serialize = false, deserialize = true)
	public Integer				likeCount;

	@Expose(serialize = false, deserialize = true)
	public Boolean				liked;

	@Expose(serialize = false, deserialize = true)
	public Integer				watchCount;

	@Expose(serialize = false, deserialize = true)
	public Boolean				watched;

	@Expose(serialize = false, deserialize = true)
	@SerializedName("_watcher")
	public String				watcherId;									/* Used to connect to watcher */

	@Expose(serialize = false, deserialize = true)
	public Number				watchedDate;

	/* Local client only */

	public UpdateScope			updateScope			= UpdateScope.Object;

	protected ServiceEntryBase() {}

	public String getEntryUri() {
		final String root = ProxiConstants.URL_PROXIBASE_SERVICE_REST;
		final String entity = this.getCollection();
		final String uri = root + entity + "/" + id;
		return uri;
	}

	public abstract String getCollection();

	public static ServiceEntryBase setPropertiesFromMap(ServiceEntryBase entry, Map map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		entry.id = (String) ((map.get("_id") != null) ? map.get("_id") : map.get("id"));
		entry.data = (HashMap<String, Object>) map.get("data");

		entry.ownerId = (String) ((map.get("_owner") != null) ? map.get("_owner") : map.get("ownerId"));
		entry.creatorId = (String) ((map.get("_creator") != null) ? map.get("_creator") : map.get("creatorId"));
		entry.modifierId = (String) ((map.get("_modifier") != null) ? map.get("_modifier") : map.get("modifierId"));
		entry.watcherId = (String) ((map.get("_watcher") != null) ? map.get("_watcher") : map.get("watcherId"));

		entry.createdDate = (Number) map.get("createdDate");
		entry.modifiedDate = (Number) map.get("modifiedDate");
		entry.activityDate = (Number) map.get("activityDate");
		entry.watchedDate = (Number) map.get("watchedDate");
		entry.type = (String) map.get("type");

		entry.likeCount = (Integer) map.get("likeCount");
		entry.liked = (Boolean) map.get("liked");
		entry.watchCount = (Integer) map.get("watchCount");
		entry.watched = (Boolean) map.get("watched");

		if (map.get("creator") != null) {
			entry.creator = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("creator"));
		}
		if (map.get("owner") != null) {
			entry.owner = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("owner"));
		}
		if (map.get("modifier") != null) {
			entry.modifier = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("modifier"));
		}

		return entry;
	}

	public static void copyProperties(ServiceEntryBase fromEntry, ServiceEntryBase toEntry) {
		/*
		 * Properties are copied from one entity to another.
		 */
		toEntry.name = fromEntry.name;
		toEntry.type = fromEntry.type;

		toEntry.likeCount = fromEntry.likeCount;
		toEntry.liked = fromEntry.liked;

		toEntry.ownerId = fromEntry.ownerId;
		toEntry.owner = fromEntry.owner;

		toEntry.modifierId = fromEntry.modifierId;
		toEntry.modifier = fromEntry.modifier;
		toEntry.modifiedDate = fromEntry.modifiedDate;

		toEntry.watcherId = fromEntry.watcherId;
		toEntry.watchedDate = fromEntry.watchedDate;
		toEntry.watchCount = fromEntry.watchCount;
		toEntry.watched = fromEntry.watched;

		toEntry.creatorId = fromEntry.creatorId;
		toEntry.creator = fromEntry.creator;
		toEntry.createdDate = fromEntry.createdDate;


		toEntry.activityDate = fromEntry.activityDate;
	}

	public Map<String, Object> getHashMap(Boolean useAnnotations, Boolean excludeNulls) {
		final Map<String, Object> map = new HashMap<String, Object>();

		try {
			Class<?> cls = this.getClass();
			final Field[] fields = cls.getDeclaredFields();
			for (Field f : fields) {
				if (!Modifier.isStatic(f.getModifiers())
						&& (Modifier.isPublic(f.getModifiers()) || Modifier.isProtected(f.getModifiers()))) {

					if (useAnnotations) {
						if (!f.isAnnotationPresent(Expose.class)) {
							continue;
						}
						else {
							Expose annotation = f.getAnnotation(Expose.class);
							if (!annotation.serialize()) {
								continue;
							}
						}
					}
					String name = f.getName();
					if (useAnnotations) {
						if (f.isAnnotationPresent(SerializedName.class)) {
							SerializedName annotation = f.getAnnotation(SerializedName.class);
							name = annotation.value();
						}
					}
					
					Object value = f.get(this);

					if (value != null || updateScope == UpdateScope.Object || !excludeNulls) {

						if (value instanceof ServiceObject) {
							Map childMap = ((ServiceObject) value).getHashMap(useAnnotations, excludeNulls);
							map.put(name, childMap);
						}
						else if (value instanceof ArrayList) {
							List<Object> list = new ArrayList<Object>();
							for (Object obj : (ArrayList) value) {

								if (obj != null || updateScope == UpdateScope.Object || !excludeNulls) {
									if (obj instanceof ServiceObject) {
										Map childMap = ((ServiceObject) obj).getHashMap(useAnnotations, excludeNulls);
										list.add(childMap);
									}
									else {
										list.add(obj);
									}
								}
							}
							map.put(name, list);
						}
						else {
							map.put(name, value);
						}
					}
				}
			}

			cls = this.getClass().getSuperclass();
			final Field[] fieldsSuper = cls.getDeclaredFields();
			for (Field f : fieldsSuper) {
				if (!Modifier.isStatic(f.getModifiers())
						&& (Modifier.isPublic(f.getModifiers()) || Modifier.isProtected(f.getModifiers()))) {
					if (useAnnotations) {
						if (!f.isAnnotationPresent(Expose.class)) {
							continue;
						}
						else {
							Expose annotation = f.getAnnotation(Expose.class);
							if (!annotation.serialize()) {
								continue;
							}
						}
					}
					String name = f.getName();
					if (useAnnotations) {
						if (f.isAnnotationPresent(SerializedName.class)) {
							SerializedName annotation = f.getAnnotation(SerializedName.class);
							name = annotation.value();
						}
					}
					
					Object value = f.get(this);
					
					if (value != null || updateScope == UpdateScope.Object || !excludeNulls) {
						
						if (value instanceof ServiceObject) {
							Map childMap = ((ServiceObject) value).getHashMap(useAnnotations, excludeNulls);
							map.put(name, childMap);
						}
						else if (value instanceof ArrayList) {
							List<Object> list = new ArrayList<Object>();
							for (Object obj : (ArrayList) value) {

								if (obj != null || updateScope == UpdateScope.Object || !excludeNulls) {
									if (obj instanceof ServiceObject) {
										Map childMap = ((ServiceObject) obj).getHashMap(useAnnotations, excludeNulls);
										list.add(childMap);
									}
									else {
										list.add(obj);
									}
								}
							}
							map.put(name, list);
						}
						else {
							map.put(name, value);
						}
					}
				}
			}
		}
		catch (IllegalArgumentException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		catch (IllegalAccessException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		return map;
	}

	/**
	 * Object: All properties are serialized including nulls.
	 * Property: Only non-null properties are serialized.
	 * 
	 * @author Jayma
	 * 
	 */
	public static enum UpdateScope {
		Undefined,
		Object,
		Property
	}
}
