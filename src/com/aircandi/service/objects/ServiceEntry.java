package com.aircandi.service.objects;

import java.util.Map;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class ServiceEntry extends ServiceBase {

	private static final long	serialVersionUID	= -4468666434251114969L;

	public ServiceEntry() {}

	public static ServiceEntry setPropertiesFromMap(ServiceEntry serviceEntry, Map map, Boolean nameMapping) {

		serviceEntry = (ServiceEntry) ServiceBase.setPropertiesFromMap(serviceEntry, map, nameMapping);
		return serviceEntry;
	}

	@Override
	public String getCollection() {
		return null;
	}

	public static String getSchemaFromId(String id) {

		String schemaId = id.substring(0, 2);
		String schema = null;
		if (schemaId.equals("us")) schema = "user";
		if (schemaId.equals("ac")) schema = "action";
		if (schemaId.equals("se")) schema = "session";
		if (schemaId.equals("in")) schema = "install";
		if (schemaId.equals("li")) schema = "link";
		if (schemaId.equals("do")) schema = "document";

		if (schemaId.equals("ap")) schema = "applink";
		if (schemaId.equals("be")) schema = "beacon";
		if (schemaId.equals("co")) schema = "comment";
		if (schemaId.equals("pl")) schema = "place";
		if (schemaId.equals("po")) schema = "post";

		return schema;
	}

	public static String getSchemaIdFromSchema(String schema) {

		String typeId = null;
		if (schema.equals("user")) typeId = "us";
		if (schema.equals("session")) typeId = "se";
		if (schema.equals("link")) typeId = "li";
		if (schema.equals("action")) typeId = "ac";
		if (schema.equals("document")) typeId = "do";
		if (schema.equals("install")) typeId = "in";

		if (schema.equals("applink")) typeId = "ap";
		if (schema.equals("beacon")) typeId = "be";
		if (schema.equals("comment")) typeId = "co";
		if (schema.equals("place")) typeId = "pl";
		if (schema.equals("post")) typeId = "po";
		return typeId;
	}
}