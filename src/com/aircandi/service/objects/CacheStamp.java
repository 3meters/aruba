package com.aircandi.service.objects;

import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class CacheStamp {

	@Expose
	public Number	activityDate;
	@Expose
	public Number	modifiedDate;
	@Expose
	public Boolean	activity	= false;
	@Expose
	public Boolean	modified	= false;

	public CacheStamp() {}

	public CacheStamp(Number activityDate, Number modifiedDate) {
		this.activityDate = activityDate;
		this.modifiedDate = modifiedDate;
	}

	public static CacheStamp setPropertiesFromMap(CacheStamp cacheStamp, Map map, Boolean nameMapping) {
		cacheStamp.activityDate = (Number) map.get("activityDate");
		cacheStamp.modifiedDate = (Number) map.get("modifiedDate");
		cacheStamp.activity = (Boolean) map.get("activity");
		cacheStamp.modified = (Boolean) map.get("modified");

		return cacheStamp;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof CacheStamp)) return false;
		CacheStamp other = (CacheStamp) o;
		Boolean activityEqual = (this.activityDate == null ? other.activityDate == null : this.activityDate.equals(other.activityDate));
		Boolean modifiedEqual = (this.modifiedDate == null ? other.modifiedDate == null : this.modifiedDate.equals(other.modifiedDate));
		return (activityEqual && modifiedEqual);
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 37 * result + (int) (activityDate.longValue() ^ (activityDate.longValue() >>> 32));
		result = 37 * result + (int) (modifiedDate.longValue() ^ (modifiedDate.longValue() >>> 32));
		return result;
	}

	@Override
	public CacheStamp clone() {
		CacheStamp cacheStamp = null;
		try {
			cacheStamp = (CacheStamp) super.clone();
		}
		catch (CloneNotSupportedException exception) {
			exception.printStackTrace();
		}
		return cacheStamp;
	}

}