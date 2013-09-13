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
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ObjectType;
import com.aircandi.service.HttpService.RequestType;
import com.aircandi.service.HttpService.ResponseFormat;
import com.aircandi.service.HttpService.ServiceDataWrapper;
import com.aircandi.service.ServiceRequest;
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
		 * We use this as a choke point for all calls to the aircandi SERVICE.
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
			else if (entity.getCount(Constants.TYPE_LINK_APPLINK, Direction.IN) == null) {
				entity.linksInCounts.add(new Count(Constants.TYPE_LINK_APPLINK, shortcuts.size()));
			}
			else {
				entity.getCount(Constants.TYPE_LINK_APPLINK, Direction.IN).count = entity.getCount(Constants.TYPE_LINK_APPLINK, Direction.IN).count.intValue()
						+ shortcuts.size();
			}
			for (Shortcut shortcut : shortcuts) {
				Link link = new Link(entity.id, shortcut.schema, true, shortcut.getId());
				link.shortcut = shortcut;
				entity.linksIn.add(link);
			}
		}

		/* Flag whether shortcuts were part of the request */
		entity.shortcuts = linkOptions != null ? linkOptions.getShortcuts() : false;
	}

	public ServiceResponse loadEntity(String entityId, LinkOptions linkOptions) {
		/*
		 * Retrieves entity from cache if available otherwise downloads the entity from the SERVICE. If refresh is true
		 * then bypasses the cache and downloads from the SERVICE.
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
			parameters.putString("links", "object:" + HttpService.objectToJson(linkOptions));
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
			final ServiceData serviceData = (ServiceData) HttpService.jsonToObjects(jsonResponse, ObjectType.ENTITY, ServiceDataWrapper.TRUE);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;

			if (loadedEntities != null && loadedEntities.size() > 0) {
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
			parameters.putString("links", "object:" + HttpService.objectToJson(linkOptions));
		}

		if (cursor != null) {
			parameters.putString("cursor", "object:" + HttpService.objectToJson(cursor));
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
			final ServiceData serviceData = (ServiceData) HttpService.jsonToObjects(jsonResponse, ObjectType.ENTITY, ServiceDataWrapper.TRUE);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;

			if (loadedEntities != null && loadedEntities.size() > 0) {
				for (Entity entity : loadedEntities) {
					if (cursor != null && cursor.direction != null && cursor.direction.equals("OUT")) {
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
			parameters.putString("links", "object:" + HttpService.objectToJson(linkOptions));
		}

		if (cursor != null) {
			parameters.putString("cursor", "object:" + HttpService.objectToJson(cursor));
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
			stopwatch.segmentTime("Load entities: SERVICE call started");
		}

		ServiceResponse serviceResponse = dispatch(serviceRequest, stopwatch);

		if (stopwatch != null) {
			stopwatch.segmentTime("Load entities: SERVICE call complete");
		}

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = (ServiceData) HttpService.jsonToObjects(jsonResponse, ObjectType.ENTITY, ServiceDataWrapper.TRUE);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;

			if (stopwatch != null) {
				stopwatch.segmentTime("Load entities: entities deserialized");
			}

			if (loadedEntities != null && loadedEntities.size() > 0) {
				decorate(loadedEntities, linkOptions);
				upsertEntities(loadedEntities);
				if (stopwatch != null) {
					stopwatch.segmentTime("Load entities: entities decorated and pushed to cache");
				}
			}

		}

		return serviceResponse;
	}

	public ServiceResponse loadEntitiesNearLocation(AirLocation location, LinkOptions linkOptions, List<String> excludePlaceIds) {

		final Bundle parameters = new Bundle();
		parameters.putString("location", "object:" + HttpService.objectToJson(location));
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
			parameters.putString("links", "object:" + HttpService.objectToJson(linkOptions));
		}

		if (excludePlaceIds != null && excludePlaceIds.size() > 0) {
			parameters.putStringArrayList("excludePlaceIds", (ArrayList<String>) excludePlaceIds);
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_PLACES + "getNearLocation")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (Aircandi.getInstance().getUser() != null) {
			serviceRequest.setSession(Aircandi.getInstance().getUser().session);
		}

		ServiceResponse serviceResponse = EntityManager.getInstance().dispatch(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = (ServiceData) HttpService.jsonToObjects(jsonResponse, ObjectType.PLACE, ServiceDataWrapper.TRUE);

			/* Do a bit of fixup */
			final List<Entity> entities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;
			for (Entity entity : entities) {
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					/* No id means it's a synthetic */
					Place place = (Place) entity;
					place.locked = false;
					place.enabled = true;

					if (entity.id == null) {
						place.id = place.getProvider().id;
						place.modifiedDate = DateTime.nowDate().getTime();
						place.synthetic = true;
					}
					else {
						place.synthetic = false;
					}
				}
			}

			/* Places LOCKED IN by proximity trump places LOCKED IN by location */
			final List<Place> proximityPlaces = (List<Place>) EntityManager.getInstance().getPlaces(null, true);

			Iterator<Place> iterProximityPlaces = proximityPlaces.iterator();
			Iterator<Entity> iterLocationPlaces = entities.iterator();

			while (iterLocationPlaces.hasNext()) {
				Entity locPlace = iterLocationPlaces.next();

				while (iterProximityPlaces.hasNext()) {
					Place proxPlace = iterProximityPlaces.next();

					if (proxPlace.id.equals(locPlace.id)) {
						iterLocationPlaces.remove();
					}
					else if (!proxPlace.getProvider().type.equals("aircandi")) {
						if (proxPlace.getProvider().id.equals(locPlace.id)) {
							iterLocationPlaces.remove();
						}
					}
				}
			}

			/* Remove all synthetic places from the cache just to help constrain the cache size */
			removeEntities(Constants.SCHEMA_ENTITY_PLACE, null, true);

			/* Push place entities to cache */
			decorate(entities, new LinkOptions(null, null, false, null));
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
		 * Updates user objects that are embedded IN entities.
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

	public void addLink(String toId, String type, String fromId, Shortcut toShortcut, Shortcut fromShortcut) {

		Long time = DateTime.nowDate().getTime();

		Entity toEntity = get(toId);
		if (toEntity != null) {

			if (toEntity.linksIn == null) {
				toEntity.linksIn = new ArrayList<Link>();
			}

			if (toEntity.linksInCounts == null) {
				toEntity.linksInCounts = new ArrayList<Count>();
				toEntity.linksInCounts.add(new Count(type, 1));
			}
			else if (toEntity.getCount(type, Direction.IN) == null) {
				toEntity.linksInCounts.add(new Count(type, 1));
			}
			else {
				toEntity.getCount(type, Direction.IN).count = toEntity.getCount(type, Direction.IN).count.intValue() + 1;
			}

			Link link = new Link(toId, type, true, fromId);
			link.modifiedDate = time;

			if (fromShortcut != null) {
				link.shortcut = fromShortcut;
			}

			toEntity.linksIn.add(link);
		}
		/*
		 * Fixup OUT links too.
		 */
		Entity fromEntity = get(fromId);
		if (fromEntity != null) {

			if (fromEntity.linksOut == null) {
				fromEntity.linksOut = new ArrayList<Link>();
			}

			if (fromEntity.linksOutCounts == null) {
				fromEntity.linksOutCounts = new ArrayList<Count>();
				fromEntity.linksOutCounts.add(new Count(type, 1));
			}
			else if (fromEntity.getCount(type, Direction.OUT) == null) {
				fromEntity.linksOutCounts.add(new Count(type, 1));
			}
			else {
				fromEntity.getCount(type, Direction.OUT).count = fromEntity.getCount(type, Direction.OUT).count.intValue() + 1;
			}

			Link link = new Link(toId, type, true, fromId);
			link.modifiedDate = time;

			if (toShortcut != null) {
				link.shortcut = toShortcut;
			}

			fromEntity.linksOut.add(link);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Cache deletes
	// --------------------------------------------------------------------------------------------

	public synchronized Entity removeEntityTree(String entityId) {
		/*
		 * Clean OUT entity and every entity related to entity
		 */
		Entity staleEntity = remove(entityId);
		if (staleEntity != null) {
			staleEntity.stale = true;

			List<Entity> entities = (List<Entity>) staleEntity.getLinkedEntitiesByLinkTypes(null, null, Direction.IN, true);
			for (Entity childEntity : entities) {
				staleEntity = remove(childEntity.id);
				if (staleEntity != null) {
					staleEntity.stale = true;
				}
			}
		}
		return staleEntity;
	}

	public synchronized void removeEntities(String schema, String type, Boolean synthetic) {
		final Iterator iter = keySet().iterator();
		Entity entity = null;
		while (iter.hasNext()) {
			entity = get(iter.next());
			if (schema.equals(Constants.SCHEMA_ANY) || entity.schema.equals(schema)) {
				if (type == null || type.equals(Constants.TYPE_ANY) || (entity.type != null && entity.type.equals(type))) {
					if (synthetic == null || entity.synthetic == synthetic) {
						iter.remove();
					}
				}
			}
		}
	}

	public void removeLink(String toId, String type, String fromId) {

		Entity toEntity = get(toId);
		if (toEntity != null) {

			if (toEntity.linksInCounts != null) {
				Count count = toEntity.getCount(type, Direction.IN);
				if (count != null) {
					count.count = count.count.intValue() - 1;
				}
			}

			if (toEntity.linksIn != null) {
				for (Link link : toEntity.linksIn) {
					if (link.fromId != null && link.fromId.equals(fromId) && link.type.equals(type)) {
						toEntity.linksIn.remove(link);
						break;
					}
				}
			}
		}

		/*
		 * Fixup OUT links too
		 */
		Entity fromEntity = get(fromId);
		if (fromEntity != null) {

			if (fromEntity.linksOutCounts != null) {
				Count count = fromEntity.getCount(type, Direction.OUT);
				if (count != null) {
					count.count = count.count.intValue() - 1;
				}
			}

			if (fromEntity.linksOut != null) {
				for (Link link : fromEntity.linksOut) {
					if (link.toId != null && link.toId.equals(toId) && link.type.equals(type)) {
						fromEntity.linksOut.remove(link);
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
