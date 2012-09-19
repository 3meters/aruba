package com.aircandi.service.objects;

import java.util.HashMap;

import com.aircandi.core.CandiConstants;
import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class Session extends ServiceEntry {

	private static final long	serialVersionUID	= 127428776257201066L;

	@Expose
	public String				key;

	/* Dates */

	@Expose
	public Number				expirationDate;

	public Session() {}

	public Boolean renewSession(long currentTime) {
		if (expirationDate.longValue() < (currentTime + CandiConstants.SIXTY_MINUTES)) {
			return true;
		}
		return false;
	}

	public static Session setFromPropertiesFromMap(Session session, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		session = (Session) ServiceEntry.setFromPropertiesFromMap(session, map);
		session.key = (String) map.get("key");
		session.expirationDate = (Number) map.get("expirationDate");

		return session;
	}

	@Override
	public String getCollection() {
		return "sessions";
	}

}