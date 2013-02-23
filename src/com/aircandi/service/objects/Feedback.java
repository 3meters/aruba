package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Feedback extends ServiceEntryBase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672244719448L;

	@Expose
	public String				message;

	public Feedback() {}

	public static Feedback setPropertiesFromMap(Feedback feedback, Map map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		feedback = (Feedback) ServiceEntryBase.setPropertiesFromMap(feedback, map);
		
		feedback.message = (String) map.get("description");
		return feedback;
	}

	@Override
	public String getCollection() {
		return "";
	}
}