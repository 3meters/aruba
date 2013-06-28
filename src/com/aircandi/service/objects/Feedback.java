package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Feedback extends ServiceBase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672244719448L;

	@Expose
	public String				message;

	public Feedback() {}

	public static Feedback setPropertiesFromMap(Feedback feedback, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		feedback = (Feedback) ServiceBase.setPropertiesFromMap(feedback, map, nameMapping);
		
		feedback.message = (String) map.get("description");
		feedback.message = (String) (nameMapping ? map.get("description") : map.get("message"));
		
		return feedback;
	}

	@Override
	public String getCollection() {
		return "";
	}
}