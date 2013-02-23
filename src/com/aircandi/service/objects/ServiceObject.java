package com.aircandi.service.objects;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;

public abstract class ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 5341986472204947192L;

	/*
	 * Annotation syntax:
	 * 
	 * @Expose (serialize = false, deserialize = false)
	 * 
	 * @SerializedName("_nametoserialize")
	 */

	public HashMap<String, Object> getHashMap(Boolean useAnnotations, Boolean excludeNulls) {
		final HashMap<String, Object> map = new HashMap<String, Object>();

		try {
			final Class<?> cls = this.getClass();
			final Field fields[] = cls.getDeclaredFields();
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
					else {
						if (value != null || (!excludeNulls)) {
							map.put(name, value);
						}
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
