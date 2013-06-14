package com.aircandi.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestType;
import com.aircandi.service.HttpService.ResponseFormat;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Cursor;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.ServiceBase;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.ServiceEntry;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.DateUtils;

public class EntityCache extends HashMap<String, Entity> {

	private static final long	serialVersionUID	= 3254271713007384499L;
	private Number				mLastActivityDate	= DateUtils.nowDate().getTime();

	// --------------------------------------------------------------------------------------------
	// Cache loading
	// --------------------------------------------------------------------------------------------

	ServiceResponse dispatch(ServiceRequest serviceRequest) {
		/*
		 * We use this as a choke point for all calls to the aircandi service.
		 */
		final ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		return serviceResponse;
	}

	public ServiceResponse loadEntity(String entityId, LinkOptions linkOptions, Stopwatch stopwatch) {
		/*
		 * Retrieves entity from cache if available otherwise downloads the entity from the service. If refresh is true
		 * then bypasses the cache and downloads from the service.
		 */
		final List<String> entityIds = new ArrayList<String>();
		entityIds.add(entityId);
		final ServiceResponse response = loadEntities(entityIds, linkOptions, null, stopwatch);
		return response;
	}

	public ServiceResponse loadEntities(List<String> entityIds, LinkOptions linkOptions, String registrationId, Stopwatch stopwatch) {

		final Bundle parameters = new Bundle();
		parameters.putStringArrayList("entityIds", (ArrayList<String>) entityIds);
		parameters.putString("links", "object:" + HttpService.convertObjectToJsonSmart(linkOptions, true, true));

		if (registrationId != null) {
			parameters.putString("registrationId", registrationId);
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntities")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		if (Aircandi.getInstance().getUser() != null) {
			serviceRequest.setSession(Aircandi.getInstance().getUser().session);
		}

		if (stopwatch != null) {
			stopwatch.segmentTime("Load entities: service call started");
		}

		ServiceResponse serviceResponse = dispatch(serviceRequest);

		if (stopwatch != null) {
			stopwatch.segmentTime("Load entities: service call complete");
		}

		if (serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;

			if (stopwatch != null) {
				stopwatch.segmentTime("Load entities: entities deserialized");
			}

			if (loadedEntities != null && loadedEntities.size() > 0) {
				upsertEntities(loadedEntities);
			}
		}

		return serviceResponse;
	}

	public ServiceResponse loadEntitiesForEntity(String entityId, List<String> linkTypes, LinkOptions linkOptions, Cursor cursor) {

		final Bundle parameters = new Bundle();
		parameters.putString("entityId", entityId);
		parameters.putStringArrayList("linkTypes", (ArrayList<String>) linkTypes);
		parameters.putString("links", "object:" + HttpService.convertObjectToJsonSmart(linkOptions, true, true));

		if (cursor != null) {
			parameters.putString("cursor", "object:" + HttpService.convertObjectToJsonSmart(cursor, true, true));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForEntity")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		if (Aircandi.getInstance().getUser() != null) {
			serviceRequest.setSession(Aircandi.getInstance().getUser().session);
		}

		ServiceResponse serviceResponse = dispatch(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;

			if (loadedEntities != null && loadedEntities.size() > 0) {
				upsertEntities(loadedEntities);
			}
		}

		return serviceResponse;
	}

	public ServiceResponse loadEntitiesByOwner(String ownerId, List<String> entitySchemas, LinkOptions linkOptions, Cursor cursor) {

		final Bundle parameters = new Bundle();
		parameters.putString("ownerId", ownerId);
		parameters.putStringArrayList("entitySchemas", (ArrayList<String>) entitySchemas);
		parameters.putString("links", "object:" + HttpService.convertObjectToJsonSmart(linkOptions, true, true));

		if (cursor != null) {
			parameters.putString("cursor", "object:" + HttpService.convertObjectToJsonSmart(cursor, true, true));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesByOwner")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		if (Aircandi.getInstance().getUser() != null) {
			serviceRequest.setSession(Aircandi.getInstance().getUser().session);
		}

		ServiceResponse serviceResponse = dispatch(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) serviceResponse.data;
			final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
			final List<Entity> loadedEntities = (List<Entity>) serviceData.data;
			serviceResponse.data = serviceData;

			if (loadedEntities != null && loadedEntities.size() > 0) {
				upsertEntities(loadedEntities);
			}
		}

		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Cache updates
	// --------------------------------------------------------------------------------------------	

	public void upsertEntities(List<Entity> entities) {
		for (Entity entity : entities) {
			upsertEntity(entity);
		}
	}

	public synchronized Entity upsertEntity(Entity freshEntity) {

		removeEntityTree(freshEntity.id);
		put(freshEntity.id, freshEntity);
		List<Applink> applinks = freshEntity.getApplinks();
		for (Applink applink : applinks) {
			put(applink.id, applink);
		}
		mLastActivityDate = DateUtils.nowDate().getTime();
		/*
		 * We only do children work if the new entity has them.
		 */
		if (freshEntity.entities != null) {

			synchronized (freshEntity.entities) {

				Iterator iter = freshEntity.entities.iterator();
				while (iter.hasNext()) {
					Entity freshChild = (Entity) iter.next();
					freshChild.toId = freshEntity.id;
					put(freshChild.id, freshChild);
					applinks = freshChild.getApplinks();
					for (Applink applink : applinks) {
						put(applink.id, applink);
					}
				}
				freshEntity.entities.clear();
				freshEntity.entities = null;
			}
		}
		return get(freshEntity.id);
	}

	public synchronized void updateEntityUser(User user) {
		/*
		 * Updates user objects that are embedded in entities.
		 */
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
		mLastActivityDate = DateUtils.nowDate().getTime();
	}

	public void addLinkTo(String toId, String verb) {
		Entity entity = null;

		if (ServiceEntry.getTypeFromId(toId) == "entities") {
			entity = get(toId);
		}
		else if (ServiceEntry.getTypeFromId(toId) == "users") {
			entity = get(toId);
		}

		if (entity != null) {
			Long time = DateUtils.nowDate().getTime();
			if (verb.equals("like")) {
				/*
				 * Add item to linksIn
				 * Add or increment linksInCounts
				 */
			}
			else if (verb.equals("watch")) {
				/*
				 * Add item to linksIn
				 * Add or increment linksInCounts
				 * Set watchedDate
				 */
			}
			entity.activityDate = time;
			setLastActivityDate(time);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Cache deletes
	// --------------------------------------------------------------------------------------------

	public synchronized Entity removeEntityTree(String entityId) {
		/*
		 * Clean out every entity related to entityId
		 */
		Entity staleEntity = remove(entityId);
		if (staleEntity != null) {
			staleEntity.stale = true;

			List<Entity> entities = (List<Entity>) staleEntity.getLinkedEntitiesByLinkType(Constants.TYPE_ANY, null, Direction.in, true);
			for (Entity childEntity : entities) {
				staleEntity = remove(childEntity.id);
				if (staleEntity != null) {
					staleEntity.stale = true;
				}
			}

			setLastActivityDate(DateUtils.nowDate().getTime());
		}
		return staleEntity;
	}

	public synchronized void removeEntities(String schema, String type, Boolean synthetic) {
		final Iterator iter = keySet().iterator();
		Entity entity = null;
		while (iter.hasNext()) {
			entity = get(iter.next());
			if (schema.equals(Constants.SCHEMA_ANY) || entity.schema.equals(schema)) {
				if (type.equals(Constants.TYPE_ANY) || (entity.type != null && entity.type.equals(type))) {
					if (synthetic == null || entity.synthetic == synthetic) {
						iter.remove();
					}
				}
			}
		}
		mLastActivityDate = DateUtils.nowDate().getTime();
	}

	public void removeLinkTo(String toId, String verb) {

		ServiceBase entry = null;

		if (ServiceEntry.getTypeFromId(toId) == "entities") {
			entry = get(toId);
		}
		else if (ServiceEntry.getTypeFromId(toId) == "users") {
			entry = get(toId);
		}

		if (entry != null) {
			if (verb.equals("like")) {
				/*
				 * Remove item to linksIn
				 * Remove or decrement linksInCounts
				 */
			}
			else if (verb.equals("watch")) {
				/*
				 * Remove item to linksIn
				 * Remove or decrement linksInCounts
				 */
			}
			Long time = DateUtils.nowDate().getTime();
			entry.activityDate = time;
			setLastActivityDate(time);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Cache reads
	// --------------------------------------------------------------------------------------------

	public List<? extends Entity> getEntities(String schema, String type, Boolean synthetic, Integer radius, Boolean proximity) {
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
									if (distance == null || distance <= radius) {
										entities.add(entity);
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

	public List<? extends Entity> getEntitiesByOwner(String ownerId, String schema, String type, Boolean synthetic, Integer radius, Boolean proximity) {
		List<Entity> entities = new ArrayList<Entity>();
		final Iterator iter = keySet().iterator();
		Entity entity = null;
		while (iter.hasNext()) {
			entity = get(iter.next());
			if (entity.ownerId != null && entity.ownerId.equals(ownerId)) {
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
										if (distance == null || distance <= radius) {
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

	public List<? extends Entity> getEntitiesByEntity(String entityId, String schema, String type, Boolean synthetic, Integer radius, Boolean proximity) {
		List<Entity> entities = new ArrayList<Entity>();
		final Iterator iter = keySet().iterator();
		Entity entity = null;
		while (iter.hasNext()) {
			entity = get(iter.next());
			if (entity.id != null && entity.id.equals(entityId)) {
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
										if (distance == null || distance <= radius) {
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
	
	// --------------------------------------------------------------------------------------------
	// Set/get
	// --------------------------------------------------------------------------------------------

	public Number getLastActivityDate() {
		return mLastActivityDate;
	}

	public void setLastActivityDate(Number lastActivityDate) {
		mLastActivityDate = lastActivityDate;
	}

}
