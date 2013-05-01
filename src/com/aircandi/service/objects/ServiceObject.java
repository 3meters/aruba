package com.aircandi.service.objects;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aircandi.beta.BuildConfig;
import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;
import com.aircandi.service.objects.ServiceEntryBase.UpdateScope;

public class ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 5341986472204947192L;
	public UpdateScope			updateScope			= UpdateScope.Object;

	/*
	 * Annotation syntax:
	 * 
	 * @Expose (serialize = false, deserialize = false)
	 * 
	 * @SerializedName("_nametoserialize")
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
						map.put(name, value);
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
}
