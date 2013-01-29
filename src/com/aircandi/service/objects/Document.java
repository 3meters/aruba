package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @author Jayma
 */

@SuppressWarnings("ucd")
public class Document extends ServiceEntryBase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 694133954499515095L;

	public Document() {}

	public static Document setPropertiesFromMap(Document document, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		document = (Document) ServiceEntryBase.setPropertiesFromMap(document, map);
		
		return document;
	}

	@Override
	public String getCollection() {
		return "documents";
	}
}