package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Map;

import com.aircandi.service.Expose;


/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Contact extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= -4242495727786003116L;
	
	@Expose
	public String				phone;
	@Expose
	public String				formattedPhone;
	@Expose
	public String				twitter;

	public Contact() {}
	
	@Override
	public Contact clone() {
		try {
			final Contact contact = (Contact) super.clone();
			return contact;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}	

	public static Contact setPropertiesFromMap(Contact contact, Map map) {
		
		contact.phone = (String) map.get("phone");
		contact.formattedPhone = (String) map.get("formattedPhone");
		contact.twitter = (String) map.get("twitter");
		
		return contact;
	}

}