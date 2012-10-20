package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

import com.aircandi.service.Expose;


/**
 * @author Jayma
 */
public class Contact implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public String				phone;
	@Expose
	public String				formattedPhone;
	@Expose
	public String				twitter;

	public Contact() {}

	public static Contact setFromPropertiesFromMap(Contact contact, HashMap map) {
		
		contact.phone = (String) map.get("phone");
		contact.formattedPhone = (String) map.get("formattedPhone");
		contact.twitter = (String) map.get("twitter");
		
		return contact;
	}

}