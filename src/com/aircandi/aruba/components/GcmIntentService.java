package com.aircandi.aruba.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.aircandi.Constants;
import com.aircandi.objects.EventType;
import com.aircandi.objects.ServiceMessage;

public class GcmIntentService extends com.aircandi.components.GcmIntentService {

	@Override
	protected Boolean isValidSchema(ServiceMessage message) {
		String[] validSchemas = { Constants.SCHEMA_ENTITY_APPLINK
				, Constants.SCHEMA_ENTITY_BEACON
				, Constants.SCHEMA_ENTITY_PICTURE
				, Constants.SCHEMA_ENTITY_PLACE
				, Constants.SCHEMA_ENTITY_COMMENT
				, Constants.SCHEMA_ENTITY_USER };

		if (message.action.entity != null) {
			if (!Arrays.asList(validSchemas).contains(message.action.entity.schema)) return false;
		}
		if (message.action.toEntity != null) {
			if (!Arrays.asList(validSchemas).contains(message.action.toEntity.schema)) return false;
		}
		return true;
	}

	@Override
	protected Boolean isValidEvent(ServiceMessage message) {
		List<String> events = new ArrayList<String>();
		events.add(EventType.INSERT_PLACE);
		events.add(EventType.INSERT_PICTURE_TO_PLACE);
		events.add(EventType.INSERT_COMMENT_TO_PLACE);
		events.add(EventType.INSERT_COMMENT_TO_PICTURE);

		if (message.action.entity != null) {
			if (!events.contains(message.action.event)) return false;
		}

		return true;
	}
}
