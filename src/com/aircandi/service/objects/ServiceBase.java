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
import com.aircandi.service.Copy;
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
public abstract class ServiceBase implements Cloneable, Serializable {

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
	@SerializedName(name = "_id")
	public String				id;
	@Expose(serialize = false, deserialize = true)
	public String				type;
	@Expose
	@SerializedName(name = "type", excludeNull = true)
	public String				subtype;
	@Expose
	public String				name;
	@Expose(serialize = false, deserialize = true)
	public String				namelc;
	@Expose
	public Boolean				enabled;
	@Expose
	public Boolean				locked;
	@Expose
	public Boolean				system;

	/* Property bags */

	@Expose
	public Map<String, Object>	data;
	@Expose
	public Map<String, Object>	cdata;

	/* User ids */

	@Expose(serialize = false, deserialize = true)
	@SerializedName(name = "_owner")
	public String				ownerId;
	@Expose(serialize = false, deserialize = true)
	@SerializedName(name = "_creator")
	public String				creatorId;
	@Expose(serialize = false, deserialize = true)
	@SerializedName(name = "_modifier")
	public String				modifierId;

	/* Dates */

	@Expose(serialize = false, deserialize = true)
	public Number				createdDate;
	@Expose(serialize = false, deserialize = true)
	public Number				modifiedDate;
	@Expose(serialize = false, deserialize = true)
	public Number				activityDate;

	/* Users (client) */

	@Expose(serialize = false, deserialize = true)
	public User					owner;
	@Expose(serialize = false, deserialize = true)
	public User					creator;
	@Expose(serialize = false, deserialize = true)
	public User					modifier;
	@Expose(serialize = false, deserialize = true)
	@SerializedName(name = "_watcher")
	public String				watcherId;									/* Used to connect to watcher */
	@Expose(serialize = false, deserialize = true)
	public Number				watchedDate;

	/* Local client only */

	public UpdateScope			updateScope			= UpdateScope.Object;

	protected ServiceBase() {}

	// --------------------------------------------------------------------------------------------
	// Set and get
	// --------------------------------------------------------------------------------------------	

	public String getEntryUri() {
		final String root = ProxiConstants.URL_PROXIBASE_SERVICE_REST;
		final String entity = this.getCollection();
		final String uri = root + entity + "/" + id;
		return uri;
	}

	public abstract String getCollection();

	// --------------------------------------------------------------------------------------------
	// Copy and serialization
	// --------------------------------------------------------------------------------------------

	public static ServiceBase setPropertiesFromMap(ServiceBase base, Map map) {

		base.id = (String) ((map.get("_id") != null) ? map.get("_id") : map.get("id"));
		base.name = (String) map.get("name");
		base.type = (String) map.get("type");
		base.subtype = (String) map.get("subtype");
		base.enabled = (Boolean) map.get("enabled");
		base.locked = (Boolean) map.get("locked");
		base.system = (Boolean) map.get("system");
		base.data = (HashMap<String, Object>) map.get("data");
		base.cdata = (HashMap<String, Object>) map.get("cdata");

		base.ownerId = (String) ((map.get("_owner") != null) ? map.get("_owner") : map.get("ownerId"));
		base.creatorId = (String) ((map.get("_creator") != null) ? map.get("_creator") : map.get("creatorId"));
		base.modifierId = (String) ((map.get("_modifier") != null) ? map.get("_modifier") : map.get("modifierId"));
		base.watcherId = (String) ((map.get("_watcher") != null) ? map.get("_watcher") : map.get("watcherId"));

		base.createdDate = (Number) map.get("createdDate");
		base.modifiedDate = (Number) map.get("modifiedDate");
		base.activityDate = (Number) map.get("activityDate");
		base.watchedDate = (Number) map.get("watchedDate");

		if (map.get("creator") != null) {
			base.creator = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("creator"));
		}
		if (map.get("owner") != null) {
			base.owner = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("owner"));
		}
		if (map.get("modifier") != null) {
			base.modifier = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("modifier"));
		}

		return base;
	}

	@Override
	public ServiceBase clone() {
		ServiceBase entry = null;
		try {
			entry = (ServiceBase) super.clone();
			if (owner != null) {
				entry.owner = owner.clone();
			}
			if (creator != null) {
				entry.creator = creator.clone();
			}
			if (modifier != null) {
				entry.modifier = modifier.clone();
			}
			if (data != null) {
				entry.data = new HashMap(data);
			}
			if (cdata != null) {
				entry.cdata = new HashMap(cdata);
			}
		}
		catch (CloneNotSupportedException exception) {
			exception.printStackTrace();
		}
		return entry;
	}

	public static <T> void copyFields(T target, T source) {
		Class<?> clazz = source.getClass();

		for (Field field : clazz.getFields()) {

			if (field.isAnnotationPresent(Copy.class)) {
				Copy annotation = field.getAnnotation(Copy.class);
				if (!annotation.exclude()) {
					continue;
				}
			}

			try {
				Object value = field.get(source);
				field.set(target, value);
			}
			catch (IllegalArgumentException exception) {
				exception.printStackTrace();
			}
			catch (IllegalAccessException exception) {
				exception.printStackTrace();
			}
		}
	}

	/**
	 * Use to generate a generic map from the typed object.
	 * 
	 * @param useAnnotations
	 * @param excludeNulls
	 * @return
	 */
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

					Boolean excludeNull = false;
					if (useAnnotations) {
						if (f.isAnnotationPresent(SerializedName.class)) {
							SerializedName annotation = f.getAnnotation(SerializedName.class);
							name = annotation.name();
							excludeNull = annotation.excludeNull();
						}
					}

					Object value = f.get(this);

					/* Exclude annotation always wins regardless of other settings */
					if (value == null && !excludeNull) {
						excludeNull = (updateScope == UpdateScope.Property || excludeNulls);
					}

					if (value != null || excludeNull) {

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

					Boolean excludeNull = false;
					if (useAnnotations) {
						if (f.isAnnotationPresent(SerializedName.class)) {
							SerializedName annotation = f.getAnnotation(SerializedName.class);
							name = annotation.name();
							excludeNull = annotation.excludeNull();
						}
					}

					Object value = f.get(this);

					if (value == null && !excludeNull) {
						excludeNull = (updateScope == UpdateScope.Property || excludeNulls);
					}

					if (value != null || excludeNull) {

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

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	/**
	 * Object: All properties are serialized including nulls.</br>
	 * Property: Only non-null properties are serialized.</br>
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
