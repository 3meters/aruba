package com.aircandi.utilities;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minidev.json.JSONValue;
import net.minidev.json.parser.ContainerFactory;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import com.aircandi.BuildConfig;
import com.aircandi.Constants;
import com.aircandi.components.bitmaps.ImageResult;
import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.AirMarker;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.CacheStamp;
import com.aircandi.service.objects.Candigram;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Comment;
import com.aircandi.service.objects.Install;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Post;
import com.aircandi.service.objects.ServiceBase.UpdateScope;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.ServiceEntry;
import com.aircandi.service.objects.ServiceObject;
import com.aircandi.service.objects.Session;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.Stat;
import com.aircandi.service.objects.User;

public class Json {

	public static Object jsonToObject(final String jsonString, Json.ObjectType objectType) {
		/*
		 * Caller will get back either an array of objectType or a single objectType.
		 */
		return Json.jsonToObject(jsonString, objectType, Json.ServiceDataWrapper.FALSE);
	}

	public static Object jsonToObject(final String jsonString, Json.ObjectType objectType, Json.ServiceDataWrapper serviceDataWrapper) {
		/*
		 * serviceDataWrapper
		 * 
		 * true: Caller will get back a ServiceData object with a data property that is
		 * either an array of objectType or a single objectType.
		 * 
		 * false: Caller will get back either an array of objectType or a single objectType.
		 */
		final Object object = Json.jsonToObjects(jsonString, objectType, serviceDataWrapper);
		if (object != null) {
			if (serviceDataWrapper == Json.ServiceDataWrapper.FALSE) {
				if (object instanceof List) {
					final List<Object> array = (List<Object>) object;
					if (array != null && array.size() > 0) {
						return array.get(0);
					}
				}
			}
			else {
				ServiceData serviceData = (ServiceData) object;
				if (serviceData.data instanceof List) {
					final List<Object> array = (List<Object>) serviceData.data;
					if (array != null && array.size() > 0) {
						serviceData.data = array.get(0);
						return serviceData;
					}
				}
			}
		}
		return object;
	}

	public static Object jsonToObjects(final String jsonString, final Json.ObjectType objectType, Json.ServiceDataWrapper serviceDataWrapper) {

		/*
		 * serviceDataWrapper
		 * 
		 * true: Caller will get back a ServiceData object with a data property that is
		 * either an array of objectType or a single objectType.
		 * 
		 * false: Caller will get back either an array of objectType or a single objectType.
		 */
		try {
			List<LinkedHashMap<String, Object>> maps = null;

			JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
			ContainerFactory containerFactory = new ContainerFactory() {
				@Override
				public Map createObjectContainer() {
					return new LinkedHashMap();
				}

				@Override
				public List<Object> createArrayContainer() {
					return new ArrayList<Object>();
				}
			};

			Map<String, Object> rootMap = (LinkedHashMap<String, Object>) parser.parse(jsonString, containerFactory);

			if (serviceDataWrapper == Json.ServiceDataWrapper.FALSE) {

				maps = new ArrayList<LinkedHashMap<String, Object>>();
				maps.add((LinkedHashMap<String, Object>) rootMap);
				Object object = Json.mapsToObjects(maps, objectType, false);
				return object;
			}
			else {

				ServiceData serviceData = ServiceData.setPropertiesFromMap(new ServiceData(), (HashMap) rootMap, true);
				/*
				 * The data property of ServiceData is always an array even
				 * if the request could only expect to return a single object.
				 */
				if (serviceData.d != null) {

					/* It's the results of a bing query */
					rootMap = (LinkedHashMap<String, Object>) serviceData.d;
					if (objectType == Json.ObjectType.IMAGE_RESULT) {

						/* Array of objects */
						maps = (List<LinkedHashMap<String, Object>>) rootMap.get("results");
						final List<Object> list = new ArrayList<Object>();
						for (Map<String, Object> map : maps) {
							list.add(ImageResult.setPropertiesFromMap(new ImageResult(), (HashMap) map, true));
						}
						serviceData.data = list;
					}
				}
				else if (serviceData.data != null) {

					if (serviceData.data instanceof List) {
						/* The data property is an array of objects */
						maps = (List<LinkedHashMap<String, Object>>) serviceData.data;
					}
					else {

						/* The data property is an object and we put it in an array */
						final Map<String, Object> map = (LinkedHashMap<String, Object>) serviceData.data;
						maps = new ArrayList<LinkedHashMap<String, Object>>();
						maps.add((LinkedHashMap<String, Object>) map);
					}
					serviceData.data = Json.mapsToObjects(maps, objectType, true);
				}
				return serviceData;
			}

		}
		catch (ParseException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		catch (Exception e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static Object mapsToObjects(List<LinkedHashMap<String, Object>> maps, final Json.ObjectType objectType, Boolean nameMapping) {

		try {

			final List<Object> list = new ArrayList<Object>();

			/* Decode each map into an object and add to an array */
			for (Map<String, Object> map : maps) {
				if (objectType == Json.ObjectType.SERVICE_ENTRY) {
					list.add(ServiceEntry.setPropertiesFromMap(new ServiceEntry(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.ENTITY) {
					String schema = (String) map.get("schema");
					if (schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
						list.add(Applink.setPropertiesFromMap(new Applink(), (HashMap) map, nameMapping));
					}
					else if (schema.equals(Constants.SCHEMA_ENTITY_BEACON)) {
						list.add(Beacon.setPropertiesFromMap(new Beacon(), (HashMap) map, nameMapping));
					}
					else if (schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
						list.add(Comment.setPropertiesFromMap(new Comment(), (HashMap) map, nameMapping));
					}
					else if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
						list.add(Place.setPropertiesFromMap(new Place(), (HashMap) map, nameMapping));
					}
					else if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
						list.add(Post.setPropertiesFromMap(new Post(), (HashMap) map, nameMapping));
					}
					else if (schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
						list.add(Candigram.setPropertiesFromMap(new Candigram(), (HashMap) map, nameMapping));
					}
					else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
						list.add(User.setPropertiesFromMap(new User(), (HashMap) map, nameMapping));
					}
				}
				else if (objectType == Json.ObjectType.USER) {
					list.add(User.setPropertiesFromMap(new User(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.AIR_MARKER) {
					list.add(AirMarker.setPropertiesFromMap(new AirMarker(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.SESSION) {
					list.add(Session.setPropertiesFromMap(new Session(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.BEACON) {
					list.add(Beacon.setPropertiesFromMap(new Beacon(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.PLACE) {
					list.add(Place.setPropertiesFromMap(new Place(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.APPLINK) {
					list.add(Applink.setPropertiesFromMap(new Applink(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.SHORTCUT) {
					list.add(Shortcut.setPropertiesFromMap(new Shortcut(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.RESULT) {
					list.add(CacheStamp.setPropertiesFromMap(new CacheStamp(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.POST) {
					list.add(Post.setPropertiesFromMap(new Post(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.COMMENT) {
					list.add(Comment.setPropertiesFromMap(new Comment(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.AIR_LOCATION) {
					list.add(AirLocation.setPropertiesFromMap(new AirLocation(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.AIR_NOTIFICATION) {
					AirNotification notification = AirNotification.setPropertiesFromMap(new AirNotification(), (HashMap) map, nameMapping);
					list.add(notification);
				}
				else if (objectType == Json.ObjectType.LINK) {
					list.add(Link.setPropertiesFromMap(new Link(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.IMAGE_RESULT) {
					list.add(ImageResult.setPropertiesFromMap(new ImageResult(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.PHOTO) {
					list.add(Photo.setPropertiesFromMap(new Photo(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.STAT) {
					list.add(Stat.setPropertiesFromMap(new Stat(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.CATEGORY) {
					list.add(Category.setPropertiesFromMap(new Category(), (HashMap) map, nameMapping));
				}
				else if (objectType == Json.ObjectType.INSTALL) {
					list.add(Install.setPropertiesFromMap(new Install(), (HashMap) map, nameMapping));
				}
			}
			return list;
		}
		catch (ClassCastException exception) {
			/*
			 * Sometimes we get back something that isn't a json object so we
			 * catch the exception, log it and keep going.
			 */
			Reporting.logException(exception);
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
		}
		return null;
	}

	public static String objectToJson(Object object) {
		return Json.objectToJson(object, Json.UseAnnotations.FALSE, Json.ExcludeNulls.TRUE);
	}

	public static String objectToJson(Object object, Json.UseAnnotations useAnnotations, Json.ExcludeNulls excludeNulls) {
		final Map map = Json.objectToMap(object, useAnnotations, excludeNulls);
		String json = JSONValue.toJSONString(map);
		return json;
	}

	public static Map<String, Object> objectToMap(Object object, Json.UseAnnotations useAnnotations, Json.ExcludeNulls excludeNullsProposed) {
		final Map<String, Object> map = new HashMap<String, Object>();

		/*
		 * Order of precedent
		 * 1. object.updateScope: PropertyValue = exclude nulls, Object = include nulls.
		 * 2. excludeNulls parameter: forces exclusion even if updateScope = Object.
		 */
		Boolean excludeNulls = (excludeNullsProposed == Json.ExcludeNulls.TRUE);
		try {
			if (((ServiceObject) object).updateScope == UpdateScope.OBJECT) {
				excludeNulls = false;
			}
		}
		catch (Exception e) {}

		Class<?> cls = object.getClass();

		try {
			while (true) {
				if (cls == null) {
					return map;
				}
				final Field[] fields = cls.getDeclaredFields();
				for (Field f : fields) {

					f.setAccessible(true); // Ensure trusted access
					/*
					 * We are only mapping public and protected fields.
					 */
					if (!Modifier.isStatic(f.getModifiers())
							&& (Modifier.isPublic(f.getModifiers()) || Modifier.isProtected(f.getModifiers()))) {

						if (useAnnotations == Json.UseAnnotations.TRUE) {
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

						String key = f.getName();
						/*
						 * Modify the name key if annotations are active and present.
						 */
						if (useAnnotations == Json.UseAnnotations.TRUE) {
							if (f.isAnnotationPresent(SerializedName.class)) {
								SerializedName annotation = f.getAnnotation(SerializedName.class);
								key = annotation.name();
							}
						}

						Object value = f.get(object);
						/*
						 * Only add to map if has value or meets null requirements.
						 */
						if (value != null || !excludeNulls) {
							/*
							 * Handle nested objects and arrays
							 */
							if (value instanceof ServiceObject) {
								Map childMap = Json.objectToMap(value, useAnnotations, excludeNullsProposed);
								map.put(key, childMap);
							}
							else if (value instanceof ArrayList) {
								List<Object> list = new ArrayList<Object>();
								for (Object obj : (ArrayList) value) {
									if (obj != null) {
										if (obj instanceof ServiceObject) {
											Map childMap = Json.objectToMap(obj, useAnnotations, excludeNullsProposed);
											list.add(childMap);
										}
										else {
											list.add(obj);
										}
									}
								}
								map.put(key, list);
							}
							else {
								map.put(key, value);
							}
						}
					}
				}
				cls = cls.getSuperclass();
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

	public static enum UseAnnotations {
		TRUE,
		FALSE
	}

	public static enum ServiceDataWrapper {
		TRUE,
		FALSE
	}

	@SuppressWarnings("ucd")
	public static enum ExcludeNulls {
		TRUE,
		FALSE
	}

	public static enum ObjectType {
		ENTITY,
		BEACON,
		USER,
		SESSION,
		PHOTO,
		LINK,
		RESULT,
		IMAGE_RESULT,
		AIR_LOCATION,
		CATEGORY,
		NONE,
		STAT,
		SERVICE_ENTRY,
		APPLINK,
		SHORTCUT,
		INSTALL,
		AIR_NOTIFICATION,
		PLACE,
		POST,
		COMMENT,
		AIR_MARKER
	}

}