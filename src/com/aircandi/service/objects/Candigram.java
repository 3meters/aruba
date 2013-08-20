package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Candigram extends Entity implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672244719448L;
	public static final String	collectionId		= "candigrams";

	// --------------------------------------------------------------------------------------------
	// Service fields
	// --------------------------------------------------------------------------------------------

	@Expose
	public Number				range;
	@Expose
	public Number				duration;
	@Expose
	public Boolean				nudge = false;
	@Expose
	public Boolean				capture = false;
	@Expose
	public Number				hopsMax;
	@Expose
	public Number				hopLastDate;
	@Expose
	public Number				hopNextDate;
	@Expose
	public Boolean				moveOnRead;
	@Expose
	public Boolean				cloneOnLike;

	// --------------------------------------------------------------------------------------------
	// Client fields (none are transferred)
	// --------------------------------------------------------------------------------------------

	public Candigram() {}

	public static Candigram setPropertiesFromMap(Candigram entity, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		synchronized (entity) {
			entity = (Candigram) Entity.setPropertiesFromMap(entity, map, nameMapping);

			entity.range = (Number) map.get("range");
			entity.duration = (Number) map.get("duration");
			entity.nudge = (Boolean) map.get("nudge");
			entity.capture = (Boolean) map.get("capture");
			entity.hopsMax = (Number) map.get("hopsMax");
			entity.hopLastDate = (Number) map.get("hopLastDate");
			entity.hopNextDate = (Number) map.get("hopNextDate");
			entity.moveOnRead = (Boolean) map.get("moveOnRead");
			entity.cloneOnLike = (Boolean) map.get("cloneOnLike");
		}
		return entity;
	}

	@Override
	public Candigram clone() {
		final Candigram clone = (Candigram) super.clone();
		return clone;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}
}