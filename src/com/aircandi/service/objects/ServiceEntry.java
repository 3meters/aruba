package com.aircandi.service.objects;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import com.aircandi.service.Expose;
import com.aircandi.service.ProxiConstants;
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

public abstract class ServiceEntry implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 5341986472204947191L;

	/*
	 * Annotation syntax:
	 * 
	 * @Expose (serialize = false, deserialize = false)
	 * 
	 * @SerializedName("_nametoserialize")
	 */
	@Expose
	@SerializedName("_id")
	public String				id;

	/* User ids */

	@Expose
	@SerializedName("_owner")
	public String				ownerId;

	@Expose
	@SerializedName("_creator")
	public String				creatorId;

	@Expose
	@SerializedName("_modifier")
	public String				modifierId;

	/* Dates */

	@Expose(serialize = false, deserialize = true)
	public Number				createdDate;

	@Expose(serialize = false, deserialize = true)
	public Number				modifiedDate;

	@Expose(serialize = false, deserialize = true)
	public String				name;

	@Expose(serialize = false, deserialize = true)
	public String				namelc;

	/* Users (client) */

	@Expose(serialize = false, deserialize = true)
	public User					owner;

	@Expose(serialize = false, deserialize = true)
	public User					creator;

	@Expose(serialize = false, deserialize = true)
	public User					modifier;

	/* Client use only */
	public String				timeSince;

	public String getEntryUri() {
		String root = ProxiConstants.URL_PROXIBASE_SERVICE_REST;
		String entity = this.getCollection();
		String uri = root + entity + "/" + id;
		return uri;
	}

	public abstract String getCollection();

	public static ServiceEntry setFromPropertiesFromMap(ServiceEntry entry, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		entry.id = (String) map.get("_id");

		entry.creatorId = (String) map.get("_creator");
		entry.ownerId = (String) map.get("_owner");
		entry.modifierId = (String) map.get("modifierId");

		entry.createdDate = (Number) map.get("createdDate");
		entry.modifiedDate = (Number) map.get("modifiedDate");

		if (map.get("creator") != null) {
			entry.creator = (User) User.setFromPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("creator"));
		}
		if (map.get("owner") != null) {
			entry.owner = (User) User.setFromPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("owner"));
		}
		if (map.get("modifier") != null) {
			entry.modifier = (User) User.setFromPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("modifier"));
		}

		return entry;
	}

	public static void copyProperties(ServiceEntry fromEntry, ServiceEntry toEntry) {
		/*
		 * Properties are copied from one entity to another.
		 */
		toEntry.ownerId = fromEntry.ownerId;
		toEntry.owner = fromEntry.owner;
		toEntry.modifierId = fromEntry.modifierId;
		toEntry.modifier = fromEntry.modifier;
		toEntry.modifiedDate = fromEntry.modifiedDate;
		toEntry.creatorId = fromEntry.creatorId;
		toEntry.creator = fromEntry.creator;
		toEntry.createdDate = fromEntry.createdDate;
	}

	public HashMap<String, Object> getHashMap(Boolean useAnnotations) {
		HashMap<String, Object> map = new HashMap<String, Object>();

		try {
			Class<?> cls = this.getClass();
			Field fields[] = cls.getDeclaredFields();
			for (Field f : fields) {
				if (!Modifier.isStatic(f.getModifiers())
						&& Modifier.isPublic(f.getModifiers())) {
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
					if (value != null) {
						map.put(name, value);
					}
				}
			}

			cls = this.getClass().getSuperclass();
			Field fieldsSuper[] = cls.getDeclaredFields();
			for (Field f : fieldsSuper) {
				if (!Modifier.isStatic(f.getModifiers())
						&& Modifier.isPublic(f.getModifiers())) {
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
					if (value != null) {
						map.put(name, value);
					}
				}

			}

		}
		catch (IllegalArgumentException exception) {
			exception.printStackTrace();
		}
		catch (IllegalAccessException exception) {
			exception.printStackTrace();
		}
		return map;
	}
}
