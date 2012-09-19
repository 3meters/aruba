package com.aircandi.service.objects;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;

import android.graphics.Bitmap;


/**
 * @author Jayma
 */
public class Comment implements Cloneable, Serializable{

	private static final long	serialVersionUID	= 4362288672244719448L;
	
	@Expose
	public String	title;
	@Expose
	public String	description;
	@Expose
	public String	name;
	@Expose
	public String	location;
	@Expose
	public String	imageUri;
	@Expose
	@SerializedName("_creator")
	public String	creatorId;
	@Expose
	public Number	createdDate;

	/* For client use only */
	public Bitmap	imageBitmap;

	public Comment() {}
	
	public static Comment setFromPropertiesFromMap(Comment comment, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		comment.title = (String) map.get("title");
		comment.description = (String) map.get("description");
		comment.name = (String) map.get("name");
		comment.location = (String) map.get("location");
		comment.imageUri = (String) map.get("imageUri");
		comment.creatorId = (String) map.get("_creator");
		comment.createdDate = (Number) map.get("createdDate");
		return comment;
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
					Object value = f.get(this);
					map.put(name, value);
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