package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class ServiceActivity extends ActivityBase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 2794888576925195033L;
	@Expose
	public String				summary;
	@Expose
	public Boolean				grouped;
	@Expose
	public Number				sortDate;
	@Expose
	public Number				activityDate;

	public ServiceActivity() {}

	public static ServiceActivity setPropertiesFromMap(ServiceActivity activity, Map map, Boolean nameMapping) {
		
		activity = (ServiceActivity) ActivityBase.setPropertiesFromMap(activity, map, nameMapping);		
		activity.summary = (String) map.get("summary");
		activity.grouped = (Boolean) map.get("grouped");
		activity.sortDate = (Number) map.get("sortDate");
		activity.activityDate = (Number) map.get("activityDate");

		return activity;
	}
	
	public static class SortBySortDate implements Comparator<ServiceActivity> {

		@Override
		public int compare(ServiceActivity object1, ServiceActivity object2) {
			if (object1.sortDate == null || object2.sortDate == null) {
				return 0;
			}
			else {
				if (object1.sortDate.longValue() < object2.sortDate.longValue()) {
					return 1;
				}
				else if (object1.sortDate.longValue() == object2.sortDate.longValue()) {
					return 0;
				}
				return -1;
			}
		}
	}
}