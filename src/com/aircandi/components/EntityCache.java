package com.aircandi.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ServiceConstants;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.service.RequestType;
import com.aircandi.service.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.ServiceResponse;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Cursor;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Json;

@SuppressWarnings("ucd")
public class EntityCache implements Map<String, Entity> {

	private final Map<String, Entity>	mCacheMap	= Collections.synchronizedMap(new HashMap<String, Entity>());

	// --------------------------------------------------------------------------------------------
	// Cache loading
	// --------------------------------------------------------------------------------------------

	private ServiceResponse dispatch(ServiceRequest serviceRequest) {
		return dispatch(serviceRequest, null);
	}

	private ServiceResponse dispatch(ServiceRequest serviceRequest, Stopwatch stopwatch) {
		/*
		 * We use this as a choke point for all calls to the aircandi service.
		 */
		final ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest, stopwatch);
		return serviceResponse;
	}

	public void decorate(List<Entity> entities, LinkOptions linkOptions) {
		for (Entity entity : entities) {
			decorate(entity, linkOptions);
		}
	}

	public void decorate(Entity entity, LinkOptions linkOptions) {
		/*
		 * Adds client applinks before entity is pushed to the cache.
		 */
		if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)
				|| entity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)
				|| entity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {

			List<Shortcut> shortcuts = entity.getClientShortcuts();

			if (entity.linksIn == null) {
				entity.linksIn = new ArrayList<Link>();
			}
			if (entity.linksInCounts == null) {
				entity.linksInCounts = new ArrayList<Count>();
			}
			else if (entity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, Direction.in) == null) {
				entity.linksInCounts.add(new Count(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, shortcuts.size()));
			}
			else {
				entity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, Direction.in).count = entity.getCount(
						Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, Direction.in).count.intValue()
						+ shortcuts.size();
			}
			for (Shortcut shortcut : shortcuts) {
				Link link = new Link(shortcut.getId(), entity.id, Constants.TYPE_LINK_CONTENT, shortcut.schema, true);
				link.shortcut = shortcut;
				entity.linksIn.add(link);
			}
		}

		/* Flag whether shortcuts were part of the request */
		entity.shortcuts = linkOptions != null ? linkOptions.getShortcuts() : false;
	}

	public ServiceResponse loadEntity(String entityId, LinkOptions linkOptions) {
		/*
		 * Retrieves entity from cache if available otherwise downloads the entity from the service. If refresh is true
		 * then bypasses the cache and downloads from the service.
		 */
		final List<String> entityIds = new ArrayList<String>();
		entityIds.add(entityId);
		final ServiceResponse response = loadEntities(entityIds, linkOptions);
		return response;
	}

	public ServiceResponse loadEntities(List<String> entityIds, LinkOptions linkOptions) {

		final Bundle parameters = new Bundle();
		parameters.putStringArrayList("entityIds", (ArrayList<String>) entityIds);
		if (linkOptions != null) {
			parameters.putString("links", "object:" + Json.objectToJson(linkOptions));
		}

		if (linkOptions != null && linkOptions.ignoreInactive != null) {
			parameters.putBoolean("ignoreInactive", linkOptions.ignoreInactive);
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntities")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (Aircandi.getInstance().getUser() != null) {
			serviceRequest.setSession(Aircandi.getInstance().getUser().session);
		}

		ServiceResponse serviceResponse = dispatch(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;

			if (loadedEntities != null && loadedEntities.size() > 0) {
				/*
				 * Clear out any cache stamp overrides.
				 */
				for (Entity entity : loadedEntities) {
					if (EntityManager.getInstance().getCacheStampOverrides().containsKey(entity.id)) {
						Logger.v(this, "Clearing cache stamp override: " + entity.id);
						EntityManager.getInstance().getCacheStampOverrides().remove(entity.id);
					}
				}

				decorate(loadedEntities, linkOptions);
				upsertEntities(loadedEntities);
			}
		}

		return serviceResponse;
	}

	public ServiceResponse loadEntitiesForEntity(String entityId, LinkOptions linkOptions, Cursor cursor, Stopwatch stopwatch) {

		final Bundle parameters = new Bundle();
		parameters.putString("entityId", entityId);

		if (linkOptions != null) {
			parameters.putString("links", "object:" + Json.objectToJson(linkOptions));
		}

		if (cursor != null) {
			parameters.putString("cursor", "object:" + Json.objectToJson(cursor));
		}

		if (linkOptions != null && linkOptions.ignoreInactive != null) {
			parameters.putBoolean("ignoreInactive", linkOptions.ignoreInactive);
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForEntity")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (Aircandi.getInstance().getUser() != null) {
			serviceRequest.setSession(Aircandi.getInstance().getUser().session);
		}

		ServiceResponse serviceResponse = dispatch(serviceRequest, stopwatch);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;

			if (loadedEntities != null && loadedEntities.size() > 0) {
				for (Entity entity : loadedEntities) {
					/*
					 * Clear out any cache stamp overrides.
					 */
					if (EntityManager.getInstance().getCacheStampOverrides().containsKey(entity.id)) {
						Logger.v(this, "Clearing cache stamp override: " + entity.id);
						EntityManager.getInstance().getCacheStampOverrides().remove(entity.id);
					}
					if (cursor != null && cursor.direction != null && cursor.direction.equals("out")) {
						entity.fromId = entityId;
					}
					else {
						entity.toId = entityId;
					}
				}
				decorate(loadedEntities, linkOptions);
				upsertEntities(loadedEntities);
			}
		}

		return serviceResponse;
	}

	public ServiceResponse loadEntitiesByProximity(List<String> beaconIds, LinkOptions linkOptions, Cursor cursor, String registrationId, Stopwatch stopwatch) {

		final Bundle parameters = new Bundle();
		parameters.putStringArrayList("beaconIds", (ArrayList<String>) beaconIds);

		if (linkOptions != null) {
			parameters.putString("links", "object:" + Json.objectToJson(linkOptions));
		}

		if (cursor != null) {
			parameters.putString("cursor", "object:" + Json.objectToJson(cursor));
		}

		if (registrationId != null) {
			parameters.putString("registrationId", registrationId);
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesByProximity")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (Aircandi.getInstance().getUser() != null) {
			serviceRequest.setSession(Aircandi.getInstance().getUser().session);
		}

		if (stopwatch != null) {
			stopwatch.segmentTime("Load entities: service call started");
		}

		ServiceResponse serviceResponse = dispatch(serviceRequest, stopwatch);

		if (stopwatch != null) {
			stopwatch.segmentTime("Load entities: service call complete");
		}

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;

			if (stopwatch != null) {
				stopwatch.segmentTime("Load entities: entities deserialized");
			}

			if (loadedEntities != null && loadedEntities.size() > 0) {
				for (Entity entity : loadedEntities) {
					entity.proximity = true;
					/*
					 * Clear out any cache stamp overrides.
					 */
					if (EntityManager.getInstance().getCacheStampOverrides().containsKey(entity.id)) {
						Logger.v(this, "Clearing cache stamp override: " + entity.id);
						EntityManager.getInstance().getCacheStampOverrides().remove(entity.id);
					}
				}
				decorate(loadedEntities, linkOptions);
				upsertEntities(loadedEntities);
			}

		}

		return serviceResponse;
	}

	public ServiceResponse loadEntitiesNearLocation(AirLocation location, LinkOptions linkOptions, List<String> excludePlaceIds) {

		final Bundle parameters = new Bundle();
		parameters.putString("location", "object:" + Json.objectToJson(location));
		parameters.putInt("limit", 50);
		parameters.putString("provider",
				Aircandi.settings.getString(
						Constants.PREF_PLACE_PROVIDER,
						Constants.PREF_PLACE_PROVIDER_DEFAULT));
		parameters.putInt("radius", Integer.parseInt(
				Aircandi.settings.getString(
						Constants.PREF_SEARCH_RADIUS,
						Constants.PREF_SEARCH_RADIUS_DEFAULT)));

		if (linkOptions != null) {
			parameters.putString("links", "object:" + Json.objectToJson(linkOptions));
		}

		if (excludePlaceIds != null && excludePlaceIds.size() > 0) {
			parameters.putStringArrayList("excludePlaceIds", (ArrayList<String>) excludePlaceIds);
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_PLACES + "near")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (Aircandi.getInstance().getUser() != null) {
			serviceRequest.setSession(Aircandi.getInstance().getUser().session);
		}

		ServiceResponse serviceResponse = EntityManager.getInstance().dispatch(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.PLACE, Json.ServiceDataWrapper.TRUE);

			/* Do a bit of fixup */
			final List<Entity> entities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;
			for (Entity entity : entities) {
				entity.proximity = false;

				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {

					Place place = (Place) entity;
					place.locked = false;
					place.enabled = true;

					/* No id means it's a synthetic */
					if (place.id == null) {
						place.id = place.getProvider().id;
						place.modifiedDate = DateTime.nowDate().getTime();
						place.synthetic = true;
					}
					else {
						place.synthetic = false;
					}
				}
			}

			//			/* Places locked in by proximity trump places locked in by location */
			//			final List<Place> proximityPlaces = (List<Place>) EntityManager.getInstance().getPlaces(null, true);
			//
			//			Iterator<Place> iterProximityPlaces = proximityPlaces.iterator();
			//			Iterator<Entity> iterLocationPlaces = entities.iterator();
			//
			//			while (iterLocationPlaces.hasNext()) {
			//				Entity locPlace = iterLocationPlaces.next();
			//
			//				while (iterProximityPlaces.hasNext()) {
			//					Place proxPlace = iterProximityPlaces.next();
			//
			//					if (proxPlace.id.equals(locPlace.id)) {
			//						iterLocationPlaces.remove();
			//					}
			//					else if (!proxPlace.getProvider().type.equals("aircandi")) {
			//						if (proxPlace.getProvider().id.equals(locPlace.id)) {
			//							iterLocationPlaces.remove();
			//						}
			//					}
			//				}
			//			}

			/* Push place entities to cache */
			decorate(entities, new LinkOptions(false, null));
			upsertEntities(entities);
		}
		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Cache updates
	// --------------------------------------------------------------------------------------------	

	private void upsertEntities(List<Entity> entities) {
		for (Entity entity : entities) {
			upsertEntity(entity);
		}
	}

	public synchronized Entity upsertEntity(Entity entity) {
		removeEntityTree(entity.id);
		put(entity.id, entity);
		return get(entity.id);
	}

	public synchronized void updateEntityUser(Entity entity) {
		/*
		 * Updates user objects that are embedded in entities.
		 */
		User user = (User) entity;
		for (Entry<String, Entity> entry : entrySet()) {
			if (entry.getValue().creatorId != null && entry.getValue().creatorId.equals(user.id)) {
				if (entry.getValue().creator != null) {
					if (user.photo != null) {
						entry.getValue().creator.photo = user.photo.clone();
					}
					if (user.area != null) {
						entry.getValue().creator.area = user.area;
					}
					if (user.name != null) {
						entry.getValue().creator.name = user.name;
					}
				}
			}
			if (entry.getValue().ownerId != null && entry.getValue().ownerId.equals(user.id)) {
				if (entry.getValue().owner != null) {
					if (user.photo != null) {
						entry.getValue().owner.photo = user.photo.clone();
					}
					if (user.area != null) {
						entry.getValue().owner.area = user.area;
					}
					if (user.name != null) {
						entry.getValue().owner.name = user.name;
					}
				}
			}
			if (entry.getValue().modifierId != null && entry.getValue().modifierId.equals(user.id)) {
				if (entry.getValue().modifier != null) {
					if (user.photo != null) {
						entry.getValue().modifier.photo = user.photo.clone();
					}
					if (user.area != null) {
						entry.getValue().modifier.area = user.area;
					}
					if (user.name != null) {
						entry.getValue().modifier.name = user.name;
					}
				}
			}
		}
	}

	public void addLink(String toId, String type, String targetSchema, String fromId, Shortcut toShortcut, Shortcut fromShortcut) {

		Long time = DateTime.nowDate().getTime();

		Entity toEntity = get(toId);
		if (toEntity != null) {

			if (toEntity.linksIn == null) {
				toEntity.linksIn = new ArrayList<Link>();
			}

			if (toEntity.linksInCounts == null) {
				toEntity.linksInCounts = new ArrayList<Count>();
				toEntity.linksInCounts.add(new Count(type, targetSchema, 1));
			}
			else if (toEntity.getCount(type, targetSchema, Direction.in) == null) {
				toEntity.linksInCounts.add(new Count(type, targetSchema, 1));
			}
			else {
				toEntity.getCount(type, targetSchema, Direction.in).count = toEntity.getCount(type, targetSchema, Direction.in).count.intValue() + 1;
			}

			Link link = new Link(fromId, toId, type, targetSchema, true);
			link.modifiedDate = time;

			if (fromShortcut != null) {
				link.shortcut = fromShortcut;
			}

			toEntity.linksIn.add(link);
			toEntity.activityDate = time;
		}
		/*
		 * Fixup out links too.
		 */
		Entity fromEntity = get(fromId);
		if (fromEntity != null) {

			if (fromEntity.linksOut == null) {
				fromEntity.linksOut = new ArrayList<Link>();
			}

			if (fromEntity.linksOutCounts == null) {
				fromEntity.linksOutCounts = new ArrayList<Count>();
				fromEntity.linksOutCounts.add(new Count(type, targetSchema, 1));
			}
			else if (fromEntity.getCount(type, targetSchema, Direction.out) == null) {
				fromEntity.linksOutCounts.add(new Count(type, targetSchema, 1));
			}
			else {
				fromEntity.getCount(type, targetSchema, Direction.out).count = fromEntity.getCount(type, targetSchema, Direction.out).count.intValue() + 1;
			}

			Link link = new Link(fromId, toId, type, targetSchema, true);
			link.modifiedDate = time;

			if (toShortcut != null) {
				link.shortcut = toShortcut;
			}

			fromEntity.linksOut.add(link);
			fromEntity.activityDate = time;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Cache deletes
	// --------------------------------------------------------------------------------------------

	public synchronized void removeEntityTree(String entityId) {
		/*
		 * Clean out entity and every entity related to entity. Is not recursive
		 */
		Entity removedEntity = remove(entityId);
		if (removedEntity != null) {
			/*
			 * getLinked..() with traverse = true will return entities that are multiple links away.
			 * We get both strong and weak linked entities.
			 */
			List<Entity> entities = (List<Entity>) removedEntity.getLinkedEntitiesByLinkTypeAndSchema(null, null, Direction.in, true);
			for (Entity childEntity : entities) {
				remove(childEntity.id);
			}
		}
	}

	public synchronized Integer removeEntities(String schema, String type, Boolean synthetic, Boolean proximity) {

		Integer removeCount = 0;
		final Iterator iterEntities = keySet().iterator();
		Entity entity = null;
		while (iterEntities.hasNext()) {
			entity = get(iterEntities.next());
			if (schema.equals(Constants.SCHEMA_ANY) || entity.schema.equals(schema)) {
				if (type == null || type.equals(Constants.TYPE_ANY) || (entity.type != null && entity.type.equals(type))) {
					if (synthetic == null || entity.synthetic == synthetic) {
						if (proximity == null || entity.proximity == proximity) {
							iterEntities.remove();
							removeCount++;
						}
					}
				}
			}
		}
		return removeCount;
	}

	public void removeLink(String fromId, String toId, String type) {
		
		Long time = DateTime.nowDate().getTime();		

		Entity toEntity = get(toId);
		if (toEntity != null) {
			if (toEntity.linksIn != null) {
				for (Link link : toEntity.linksIn) {
					if (link.fromId != null && link.fromId.equals(fromId) && link.type.equals(type)) {
						toEntity.linksIn.remove(link);
						toEntity.activityDate = time;
						
						/* Adjust the count */
						if (toEntity.linksInCounts != null) {
							Count count = toEntity.getCount(type, link.targetSchema, Direction.in);
							if (count != null) {
								count.count = count.count.intValue() - 1;
							}
						}
						
						break;
					}
				}
			}
		}

		/*
		 * Fixup out links too
		 */
		Entity fromEntity = get(fromId);
		if (fromEntity != null) {
			if (fromEntity.linksOut != null) {
				for (Link link : fromEntity.linksOut) {
					if (link.toId != null && link.toId.equals(toId) && link.type.equals(type)) {
						fromEntity.linksOut.remove(link);
						fromEntity.activityDate = time;
						if (fromEntity.linksOutCounts != null) {
							Count count = fromEntity.getCount(type, link.targetSchema, Direction.out);
							if (count != null) {
								count.count = count.count.intValue() - 1;
							}
						}
						break;
					}
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Cache reads
	// --------------------------------------------------------------------------------------------

	public synchronized List<? extends Entity> getEntities(String schema, String type, Boolean synthetic, Integer radius, Boolean proximity) {
		List<Entity> entities = new ArrayList<Entity>();
		final Iterator iter = keySet().iterator();
		Entity entity = null;
		while (iter.hasNext()) {
			entity = get(iter.next());
			if (!entity.isHidden()) {
				if (schema == null || schema.equals(Constants.SCHEMA_ANY) || entity.schema.equals(schema)) {
					if (type == null || type.equals(Constants.TYPE_ANY) || (entity.type != null && entity.type.equals(type))) {
						if (synthetic == null || entity.synthetic == synthetic) {
							if (proximity == null || entity.getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true) != null) {
								if (radius == null) {
									entities.add(entity);
								}
								else {
									Float distance = entity.getDistance(true);
									if (distance != null && distance <= radius) {
										entities.add(entity);
									}
									else if (distance == null) {
										Beacon beacon = entity.getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, false);
										if (beacon != null) {
											entities.add(entity);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return entities;
	}

	@SuppressWarnings("ucd")
	public synchronized List<? extends Entity> getEntitiesForEntity(String entityId, String schema, String type, Boolean synthetic, Integer radius,
			Boolean proximity) {
		/*
		 * We rely on the toId property instead of traversing links.
		 */
		List<Entity> entities = new ArrayList<Entity>();
		final Iterator iter = keySet().iterator();
		Entity entity = null;
		while (iter.hasNext()) {
			entity = get(iter.next());
			if (entity.toId != null && entity.toId.equals(entityId)) {
				if (!entity.isHidden()) {
					if (schema == null || schema.equals(Constants.SCHEMA_ANY) || entity.schema.equals(schema)) {
						if (type == null || type.equals(Constants.TYPE_ANY) || (entity.type != null && entity.type.equals(type))) {
							if (synthetic == null || entity.synthetic == synthetic) {
								if (proximity == null || entity.getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true) != null) {
									if (radius == null) {
										entities.add(entity);
									}
									else {
										Float distance = entity.getDistance(true);
										if (distance != null && distance <= radius) {
											entities.add(entity);
										}
										else if (distance == null) {
											Beacon beacon = entity.getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, false);
											if (beacon != null) {
												entities.add(entity);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return entities;
	}

	// --------------------------------------------------------------------------------------------
	// Map methods
	// --------------------------------------------------------------------------------------------

	@Override
	public void clear() {
		mCacheMap.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return mCacheMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return mCacheMap.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, Entity>> entrySet() {
		return mCacheMap.entrySet();
	}

	@Override
	public Entity get(Object key) {
		return mCacheMap.get(key);
	}

	@Override
	public boolean isEmpty() {
		return mCacheMap.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return mCacheMap.keySet();
	}

	@Override
	public Entity put(String key, Entity value) {
		return mCacheMap.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Entity> map) {
		mCacheMap.putAll(map);
	}

	@Override
	public Entity remove(Object key) {
		return mCacheMap.remove(key);
	}

	@Override
	public int size() {
		return mCacheMap.size();
	}

	@Override
	public Collection<Entity> values() {
		return mCacheMap.values();
	}

}
