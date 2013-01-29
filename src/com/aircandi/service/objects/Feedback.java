package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Feedback extends ServiceEntryBase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672244719448L;

	@Expose
	public String				message;
	@Expose
	@SerializedName("_creator")
	public String				creatorId;
	@Expose
	public Number				createdDate;

	public Feedback() {}

	public static Feedback setPropertiesFromMap(Feedback comment, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		comment.message = (String) map.get("description");
		comment.creatorId = (String) map.get("_creator");
		comment.createdDate = (Number) map.get("createdDate");
		return comment;
	}

	@Override
	public String getCollection() {
		return "beacons";
	}

}