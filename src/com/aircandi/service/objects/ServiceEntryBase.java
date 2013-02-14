package com.aircandi.service.objects;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

import com.aircandi.ProxiConstants;
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

	private static final long		serialVersionUID	= 5341986472204947191L;

	/*
	 * Annotation syntax:
	 * 
	 * @Expose (serialize = false, deserialize = false)
	 * 
	 * @SerializedName("_nametoserialize")
	 */
	@Expose
	@SerializedName("_id")
	public String					id;

	@Expose
	public String					name;

	@Expose(serialize = false, deserialize = true)
	public String					namelc;

	@Expose
	public String					type;

	/* Property bag */

	@Expose
	public HashMap<String, Object>	data;

	/* User ids */

	@Expose(serialize = false, deserialize = true)
	@SerializedName("_owner")
	public String					ownerId;

	@Expose(serialize = false, deserialize = true)
	@SerializedName("_creator")
	public String					creatorId;

	@Expose(serialize = false, deserialize = true)
	@SerializedName("_modifier")
	public String					modifierId;

	/* Dates */

	@Expose(serialize = false, deserialize = true)
	public Number					createdDate;

	@Expose(serialize = false, deserialize = true)
	public Number					modifiedDate;

	/* Users (client) */

	@Expose(serialize = false, deserialize = true)
	public User						owner;

	@Expose(serialize = false, deserialize = true)
	public User						creator;

	@Expose(serialize = false, deserialize = true)
	public User						modifier;

	public ServiceEntryBase() {}

	public String getEntryUri() {
		String root = ProxiConstants.URL_PROXIBASE_SERVICE_REST;
		String entity = this.getCollection();
		String uri = root + entity + "/" + id;
		return uri;
	}

	public abstract String getCollection();

	public static ServiceEntryBase setPropertiesFromMap(ServiceEntryBase entry, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		entry.id = (String) (map.get("_id") != null ? map.get("_id") : map.get("id"));
		entry.data = (HashMap<String, Object>) map.get("data");

		entry.ownerId = (String) (map.get("_owner") != null ? map.get("_owner") : map.get("ownerId"));
		entry.creatorId = (String) (map.get("_creator") != null ? map.get("_creator") : map.get("creatorId"));
		entry.modifierId = (String) (map.get("_modifier") != null ? map.get("_modifier") : map.get("modifierId"));

		entry.createdDate = (Number) map.get("createdDate");
		entry.modifiedDate = (Number) map.get("modifiedDate");
		entry.type = (String) map.get("type");

		if (map.get("creator") != null) {
			entry.creator = (User) User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("creator"));
		}
		if (map.get("owner") != null) {
			entry.owner = (User) User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("owner"));
		}
		if (map.get("modifier") != null) {
			entry.modifier = (User) User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("modifier"));
		}

		return entry;
	}

	public static void copyProperties(ServiceEntryBase fromEntry, ServiceEntryBase toEntry) {
		/*
		 * Properties are copied from one entity to another.
		 */
		toEntry.name = fromEntry.name;
		toEntry.type = fromEntry.type;
		
		toEntry.ownerId = fromEntry.ownerId;
		toEntry.owner = fromEntry.owner;
		
		toEntry.modifierId = fromEntry.modifierId;
		toEntry.modifier = fromEntry.modifier;
		toEntry.modifiedDate = fromEntry.modifiedDate;
		
		toEntry.creatorId = fromEntry.creatorId;
		toEntry.creator = fromEntry.creator;
		toEntry.createdDate = fromEntry.createdDate;
	}

	public HashMap<String, Object> getHashMap(Boolean useAnnotations, Boolean excludeNulls) {
		HashMap<String, Object> map = new HashMap<String, Object>();

		try {
			Class<?> cls = this.getClass();
			Field fields[] = cls.getDeclaredFields();
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

					if (value instanceof ServiceObject) {
						HashMap childMap = ((ServiceObject) value).getHashMap(useAnnotations, excludeNulls);
						map.put(name, childMap);
					}
					else if (value instanceof ArrayList) {
						ArrayList<Object> list = new ArrayList<Object>();
						for (Object obj : (ArrayList) value) {
							if (obj instanceof ServiceObject) {
								HashMap childMap = ((ServiceObject) obj).getHashMap(useAnnotations, excludeNulls);
								list.add(childMap);
							}
							else {
								if (obj != null || (!excludeNulls)) {
									list.add(obj);
								}
							}
						}
						map.put(name, list);
					}
					else {
						if (name.equals("photo") && value == null) {
							map.put(name, value);
						}
						else if (value != null || (!excludeNulls)) {
							map.put(name, value);
						}
					}
				}
			}

			cls = this.getClass().getSuperclass();
			Field fieldsSuper[] = cls.getDeclaredFields();
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
					if (value != null || (!excludeNulls)) {
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
