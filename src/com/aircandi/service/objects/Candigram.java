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
	public String				range;
	@Expose
	public Number				timeout;
	@Expose
	public Boolean				nudgeable;
	@Expose
	public Boolean				capturable;
	@Expose
	public Number				maxHops;
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

			entity.range = (String) map.get("range");
			entity.timeout = (Number) map.get("timeout");
			entity.nudgeable = (Boolean) map.get("nudgeable");
			entity.capturable = (Boolean) map.get("capturable");
			entity.maxHops = (Number) map.get("maxHops");
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