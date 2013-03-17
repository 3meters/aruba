package com.aircandi.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiManager.ArrayListType;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.ProxiManager.WifiScanResult;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Beacon.BeaconType;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Comment;
import com.aircandi.service.objects.Document;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Link.LinkType;
import com.aircandi.service.objects.Observation;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.Source;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.DateUtils;

public class EntityModel {

	private final ProxiManager			mEntityModel;
	final Map<String, Entity>			mEntityCache		= new HashMap<String, Entity>();
	final Map<String, User>				mUserCache			= new HashMap<String, User>();
	private final List<Beacon>			mBeacons			= new ArrayList<Beacon>();
	private List<Photo>					mPhotos				= new ArrayList<Photo>();
	private List<Category>				mCategories			= new ArrayList<Category>();
	private final Map<String, Source>	mSourceMeta			= new HashMap<String, Source>();
	private List<WifiScanResult>		mWifiList			= new ArrayList<WifiScanResult>();

	private Number						mLastActivityDate	= DateUtils.nowDate().getTime();
	private Number						mLastRefreshDate;
	private Number						mLastBeaconLockedDate;

	public EntityModel(ProxiManager proxiManager) {
		mEntityModel = proxiManager;
	}

	// --------------------------------------------------------------------------------------------
	// Combo service/cache queries
	// --------------------------------------------------------------------------------------------

	public ModelResult getUserEntities(String userId, Boolean refresh, Integer limit) {
		final List<Entity> entities = new ArrayList<Entity>();
		final ModelResult result = new ProxiManager.ModelResult();

		if (refresh) {

			final Bundle parameters = new Bundle();
			parameters.putString("userId", userId);
			parameters.putString("eagerLoad", "object:{\"children\":true,\"parents\":false,\"comments\":false}");
			parameters.putString("fields", "object:{\"entities\":{},\"comments\":{},\"children\":{},\"parents\":{}}");
			parameters.putString("options", "object:{\"limit\":"
					+ String.valueOf(limit)
					+ ",\"skip\":0"
					+ ",\"sort\":{\"modifiedDate\":-1} "
					+ ",\"children\":{\"limit\":"
					+ String.valueOf(ProxiConstants.RADAR_CHILDENTITY_LIMIT)
					+ ",\"skip\":0"
					+ ",\"sort\":{\"modifiedDate\":-1}}"
					+ "}");

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForUser")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = mEntityModel.dispatch(serviceRequest);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {

				final String jsonResponse = (String) result.serviceResponse.data;
				final ServiceData serviceData = ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
				entities.addAll((List<Entity>) serviceData.data);
				upsertEntities(entities);
				result.data = getCacheUserEntities(userId);
			}
		}
		else {
			result.data = getCacheUserEntities(userId);
		}
		return result;
	}

	/**
	 * Retrieves entity from cache if available otherwise downloads the entity from the service. If refresh is true
	 * then bypasses the cache and downloads from the service.
	 * 
	 * @param entityId
	 * @param refresh
	 * @param jsonEagerLoad
	 * @param jsonOptions
	 * @return
	 */
	public ModelResult getEntity(String entityId, Boolean refresh, String jsonEagerLoad, String jsonOptions) {
		final List<String> entityIds = new ArrayList<String>();
		entityIds.add(entityId);
		final ModelResult result = getEntities(entityIds, refresh, jsonEagerLoad, jsonOptions);
		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final List<Entity> entities = (List<Entity>) result.data;
			result.data = entities.get(0);
		}
		return result;
	}

	private ModelResult getEntities(List<String> entityIds, Boolean refresh, String jsonEagerLoad, String jsonOptions) {
		final ModelResult result = new ProxiManager.ModelResult();

		final List<String> getEntityIds = new ArrayList<String>();

		for (String entityId : entityIds) {
			Entity entity = getCacheEntity(entityId);
			if (refresh || entity == null) {
				getEntityIds.add(entityId);
			}
		}

		if (getEntityIds.size() > 0) {
			if (jsonEagerLoad == null) {
				jsonEagerLoad = "{\"children\":true,\"parents\":false,\"comments\":false}";
			}

			if (jsonOptions == null) {
				jsonOptions = "{\"limit\":"
						+ String.valueOf(ProxiConstants.RADAR_ENTITY_LIMIT)
						+ ",\"skip\":0"
						+ ",\"sort\":{\"modifiedDate\":-1} "
						+ ",\"children\":{\"limit\":"
						+ String.valueOf(ProxiConstants.RADAR_CHILDENTITY_LIMIT)
						+ ",\"skip\":0"
						+ ",\"sort\":{\"modifiedDate\":-1}}"
						+ "}";
			}

			final Bundle parameters = new Bundle();
			parameters.putStringArrayList("entityIds", (ArrayList<String>) getEntityIds);
			parameters.putString("eagerLoad", "object:" + jsonEagerLoad);
			parameters.putString("options", "object:" + jsonOptions);

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntities")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = mEntityModel.dispatch(serviceRequest);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				final String jsonResponse = (String) result.serviceResponse.data;
				final ServiceData serviceData = ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
				final List<Entity> fetchedEntities = (List<Entity>) serviceData.data;

				/* Remove any links to beacons not currently visible */
				Beacon beacon = null;
				for (Entity entity : fetchedEntities) {
					if (entity.links != null) {
						for (int i = entity.links.size() - 1; i >= 0; i--) {
							beacon = getBeacon(entity.links.get(i).toId);
							if (beacon == null) {
								entity.links.remove(i);
							}
						}
					}
				}

				if (fetchedEntities != null && fetchedEntities.size() > 0) {
					upsertEntities(fetchedEntities);
				}
			}
		}

		final List<Entity> entities = new ArrayList<Entity>();
		for (String entityId : entityIds) {
			Entity entity = getCacheEntity(entityId);
			entities.add(entity);
		}

		result.data = entities;
		return result;
	}

	public ModelResult getUser(String userId, Boolean refresh) {
		final ModelResult result = new ProxiManager.ModelResult();

		if (refresh) {

			final Bundle parameters = new Bundle();
			parameters.putString("userId", userId);

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getUser")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setResponseFormat(ResponseFormat.Json)
					.setSession(Aircandi.getInstance().getUser().session);

			result.serviceResponse = mEntityModel.dispatch(serviceRequest);
			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				final String jsonResponse = (String) result.serviceResponse.data;
				User user = (User) ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.User).data;
				mUserCache.put(userId, user);
			}
		}

		result.serviceResponse.data = mUserCache.get(userId);
		return result;
	}

	public synchronized ModelResult loadCategories() {

		final ModelResult result = new ProxiManager.ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("source", "foursquare");

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getPlaceCategories")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mEntityModel.dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Category);
			mCategories = (List<Category>) serviceData.data;
			result.serviceResponse.data = mCategories;
		}
		return result;
	}

	public ModelResult getSourceSuggestions(List<Source> sources) {

		if (sources == null || sources.size() == 0) {
			throw new IllegalArgumentException("sources parameter for getSourceSuggestions is null or size = 0");
		}

		final ModelResult result = new ProxiManager.ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putInt("timeout", ProxiConstants.SOURCE_SUGGESTIONS_TIMEOUT);
		final List<String> sourceStrings = new ArrayList<String>();
		for (Source source : sources) {
			sourceStrings.add("object:" + ProxibaseService.convertObjectToJsonSmart(source, true, true));
		}

		parameters.putStringArrayList("sources", (ArrayList<String>) sourceStrings);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_SOURCES + "suggest")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mEntityModel.dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Source);
			result.serviceResponse.data = serviceData.data;
		}
		return result;
	}

	public ModelResult getPlacePhotos(String provider, String id, long count, long offset) {
		final ModelResult result = new ProxiManager.ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("provider", provider);
		parameters.putString("id", id);
		parameters.putLong("limit", count);
		parameters.putLong("skip", offset);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getPlacePhotos")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mEntityModel.dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = ProxibaseService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Photo);
			final List<Photo> photos = (List<Photo>) serviceData.data;
			result.serviceResponse.data = photos;
		}
		return result;

	}

	// --------------------------------------------------------------------------------------------
	// Combo service/cache updates
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public ModelResult moveEntity(String entityId, String newParentId, Boolean toBeacon, Boolean cacheOnly) {

		final ModelResult result = new ProxiManager.ModelResult();

		if (!cacheOnly) {
			final Entity entity = getCacheEntity(entityId);

			final Link link = new Link();
			final Bundle parameters = new Bundle();

			link.toId = newParentId;
			link.fromId = entity.id;
			parameters.putString("link", "object:" + ProxibaseService.convertObjectToJsonSmart(link, true, true));
			parameters.putString("originalToId", (entity.parentId != null) ? entity.parentId : entity.getBeaconId());

			if (link.toId == null || link.fromId == null || parameters.getString("originalToId") == null) {
				throw new IllegalArgumentException("moveEntity: missing id for link update");
			}

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "updateLink")
					.setRequestType(RequestType.Method)
					.setResponseFormat(ResponseFormat.Json)
					.setParameters(parameters)
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setSession(Aircandi.getInstance().getUser().session);

			result.serviceResponse = mEntityModel.dispatch(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			moveEntity(entityId, !toBeacon ? newParentId : null, toBeacon ? newParentId : null);
		}

		return result;
	}

	public ModelResult deleteEntity(String entityId, Boolean cacheOnly) {
		final ModelResult result = new ProxiManager.ModelResult();

		if (!cacheOnly) {
			final Entity entity = getCacheEntity(entityId);
			/*
			 * Delete the entity and all links and observations it is associated with. We attempt to continue even
			 * if the call to delete the image failed.
			 */
			Logger.i(this, "Deleting entity: " + entity.name);

			final Bundle parameters = new Bundle();
			parameters.putString("entityId", entity.id);
			parameters.putBoolean("deleteChildren", true);

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "deleteEntity")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setSession(Aircandi.getInstance().getUser().session)
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = mEntityModel.dispatch(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			removeEntity(entityId);
		}
		return result;
	}

	/**
	 * Inserts the entity in the entity service collection and Links are created to all the included beacons. The
	 * inserted entity is retrieved from the service and pushed into the local cache. The cached entity is returned
	 * in the data property of the result object.
	 * 
	 * @param entity
	 * @param beacons
	 * @param primaryBeacon
	 * @param bitmap
	 * @param cacheOnly
	 * 
	 * @return a ModelResult object. The data property includes the just inserted entity.
	 */

	public ModelResult insertEntity(Entity entity, List<Beacon> beacons, Beacon primaryBeacon, Bitmap bitmap, Boolean cacheOnly) {
		final ModelResult result = new ProxiManager.ModelResult();
		/*
		 * This is the only place we use the children property
		 * set when deserializing from the service. After this
		 * all references to the children are dynamically assembled
		 * in the getChildren method on entities.
		 */

		if (!cacheOnly) {
			Logger.i(this, "Inserting entity: " + entity.name);
			/*
			 * Upload image to S3 as needed
			 */
			if (bitmap != null) {
				result.serviceResponse = mEntityModel.storeImageAtS3(entity, null, bitmap);
			}

			if (result.serviceResponse.responseCode == ResponseCode.Success) {

				/* Construct entity, link, and observation */
				final Bundle parameters = new Bundle();

				/* Primary beacon id */
				if (primaryBeacon != null) {
					parameters.putString("primaryBeaconId", primaryBeacon.id);
				}

				if (beacons != null && beacons.size() > 0) {
					/*
					 * Linking to beacons
					 */
					final List<String> beaconStrings = new ArrayList<String>();

					for (Beacon beacon : beacons) {
						Observation observation = LocationManager.getInstance().getObservationLocked();
						if (observation != null) {

							beacon.latitude = observation.latitude;
							beacon.longitude = observation.longitude;

							if (observation.altitude != null) {
								beacon.altitude = observation.altitude;
							}
							if (observation.accuracy != null) {
								beacon.accuracy = observation.accuracy;
							}
							if (observation.bearing != null) {
								beacon.bearing = observation.bearing;
							}
							if (observation.speed != null) {
								beacon.speed = observation.speed;
							}
						}

						beacon.beaconType = BeaconType.Fixed.name().toLowerCase(Locale.US);
						beacon.locked = false;
						beaconStrings.add("object:" + ProxibaseService.convertObjectToJsonSmart(beacon, true, true));
					}
					parameters.putStringArrayList("beacons", (ArrayList<String>) beaconStrings);
				}
				else if (entity.parentId != null) {
					/*
					 * Linking to another entity
					 */
					parameters.putString("parentId", entity.parentId);
				}

				/* Observation: only used as data package for the action that gets logged. */
				final Observation observation = LocationManager.getInstance().getObservationLocked();
				if (observation != null) {
					parameters.putString("observation",
							"object:" + ProxibaseService.convertObjectToJsonSmart(observation, true, true));
				}

				/* Sources configuration */
				if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
					parameters.putBoolean("suggestSources", true);
					parameters.putInt("suggestTimeout", 10000);
				}

				/* Entity */
				parameters.putString("entity", "object:" + ProxibaseService.convertObjectToJsonSmart(entity, true, true));

				final ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "insertEntity")
						.setRequestType(RequestType.Method)
						.setParameters(parameters)
						.setSession(Aircandi.getInstance().getUser().session)
						.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
						.setRetry(false)
						.setResponseFormat(ResponseFormat.Json);

				result.serviceResponse = mEntityModel.dispatch(serviceRequest);
			}

			if (result.serviceResponse.responseCode == ResponseCode.Success) {

				final String jsonResponse = (String) result.serviceResponse.data;
				final ServiceData serviceData = ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.Entity);

				final Entity insertedEntity = (Entity) serviceData.data;

				/* We want to retain the parent relationship */
				if (entity.parentId != null) {
					insertedEntity.parentId = entity.parentId;
				}

				upsertEntity(insertedEntity);
				result.data = insertedEntity;
			}
		}
		else {
			upsertEntity(entity);
		}

		return result;
	}

	public ModelResult trackEntity(Entity entity, List<Beacon> beacons, Beacon primaryBeacon, String actionType) {

		final ModelResult result = new ProxiManager.ModelResult();
		Logger.i(this, "Tracking entity");

		/* Construct entity, link, and observation */
		final Bundle parameters = new Bundle();

		/* Beacons */
		if (primaryBeacon != null) {
			parameters.putString("primaryBeaconId", primaryBeacon.id);
		}

		if (beacons != null && beacons.size() > 0) {
			final List<String> beaconStrings = new ArrayList<String>();
			for (Beacon beacon : beacons) {
				if (beacon.id.equals(primaryBeacon.id)) {
					Observation observation = LocationManager.getInstance().getObservationLocked();
					if (observation != null) {

						beacon.latitude = observation.latitude;
						beacon.longitude = observation.longitude;

						if (observation.altitude != null) {
							beacon.altitude = observation.altitude;
						}
						if (observation.accuracy != null) {
							beacon.accuracy = observation.accuracy;
						}
						if (observation.bearing != null) {
							beacon.bearing = observation.bearing;
						}
						if (observation.speed != null) {
							beacon.speed = observation.speed;
						}
					}
				}

				beacon.beaconType = BeaconType.Fixed.name().toLowerCase(Locale.US);
				beacon.locked = false;
				beaconStrings.add("object:" + ProxibaseService.convertObjectToJsonSmart(beacon, true, true));
			}

			parameters.putStringArrayList("beacons", (ArrayList<String>) beaconStrings);
		}

		/* Observation */
		final Observation observation = LocationManager.getInstance().getObservationLocked();
		if (observation != null) {
			parameters.putString("observation",
					"object:" + ProxibaseService.convertObjectToJsonSmart(observation, true, true));
		}

		/* Entity */
		parameters.putString("entityId", entity.id);

		/* Action type */
		parameters.putString("actionType", actionType);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "trackEntity")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setSession(Aircandi.getInstance().getUser().session)
				.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
				.setRetry(false)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mEntityModel.dispatch(serviceRequest);

		/* Reproduce the service call effect locally */
		if (result.serviceResponse.responseCode == ResponseCode.Success) {

			entity.activityDate = DateUtils.nowDate().getTime(); // So place rank score gets updated
			setLastActivityDate(DateUtils.nowDate().getTime());	 // So collections get resorted

			if (beacons != null) {
				for (Beacon beacon : beacons) {
					Boolean primary = (primaryBeacon != null && primaryBeacon.id.equals(beacon.id));
					Link link = entity.getLink(beacon, actionType);
					if (link != null) {
						if (primary) {
							if (link.tuneCount != null) {
								link.tuneCount = link.tuneCount.intValue() + 1;
							}
							else {
								link.tuneCount = 1;
							}
							if (!link.primary) {
								link.primary = true;
							}
						}
					}
					else {
						link = new Link(beacon.id, entity.id);
						link.type = actionType;
						link.signal = beacon.level;
						if (primary) {
							if (link.tuneCount != null) {
								link.tuneCount = link.tuneCount.intValue() + 1;
							}
							else {
								link.tuneCount = 1;
							}
							link.primary = true;
						}
						if (entity.links == null) {
							entity.links = new ArrayList<Link>();
						}
						entity.links.add(link);
					}
				}
			}
		}

		return result;
	}

	public ModelResult updateEntity(Entity entity, Bitmap bitmap) {
		final ModelResult result = new ProxiManager.ModelResult();

		/* Upload new images to S3 as needed. */
		if (bitmap != null) {
			result.serviceResponse = mEntityModel.storeImageAtS3(entity, null, bitmap);
		}

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			Logger.i(this, "Updating entity: " + entity.name);

			/*
			 * Construct entity, link, and observation
			 * 
			 * Note: A property will be removed from the document if it is set to null. The routine
			 * to convert objects to json takes a parameter to ignore or serialize props set to null.
			 * For now, I have special case code to ensure that photo is seriallized as null even
			 * if ignoreNulls = true.
			 */
			final Bundle parameters = new Bundle();
			parameters.putBoolean("skipActivityDate", false);
			parameters.putString("entity", "object:" + ProxibaseService.convertObjectToJsonSmart(entity, true, true));

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "updateEntity")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setSession(Aircandi.getInstance().getUser().session)
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = mEntityModel.dispatch(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			entity.activityDate = DateUtils.nowDate().getTime();
			upsertEntity(entity);
		}

		return result;
	}

	public ModelResult insertComment(String entityId, Comment comment, Boolean cacheOnly) {
		final ModelResult result = new ProxiManager.ModelResult();

		if (!cacheOnly) {
			final Bundle parameters = new Bundle();
			parameters.putString("entityId", entityId);
			parameters.putString("comment", "object:" + ProxibaseService.convertObjectToJsonSmart(comment, true, true));

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "insertComment")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setSession(Aircandi.getInstance().getUser().session)
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = mEntityModel.dispatch(serviceRequest);
		}

		/*
		 * We want to update the entity wherever it might be in the entity model while
		 * keeping the same instance.
		 */
		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			insertComment(entityId, comment);
		}

		return result;
	}

	public ModelResult insertDocument(Document document) {
		final ModelResult result = new ProxiManager.ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST + document.getCollection())
				.setRequestType(RequestType.Insert)
				.setRequestBody(ProxibaseService.convertObjectToJsonSmart(document, true, true))
				.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
				.setRetry(false)
				.setSession(Aircandi.getInstance().getUser().session)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mEntityModel.dispatch(serviceRequest);

		return result;
	}

	public ModelResult signin(String email, String password) {
		final ModelResult result = new ProxiManager.ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("user", "object:{"
				+ "\"email\":\"" + email + "\","
				+ "\"password\":\"" + password + "\""
				+ "}");

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_AUTH + "signin")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
				.setRetry(false)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mEntityModel.dispatch(serviceRequest);
		return result;

	}

	@SuppressWarnings("ucd")
	public ModelResult signout() {
		final ModelResult result = new ProxiManager.ModelResult();

		final User user = Aircandi.getInstance().getUser();
		if (user.session != null) {
			/*
			 * We use a short timeout with no retry because failure doesn't
			 * really hurt anything.
			 */
			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_AUTH + "signout")
					.setRequestType(RequestType.Get)
					.setSession(user.session)
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_QUERIES)
					.setRetry(false)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = mEntityModel.dispatch(serviceRequest);
		}

		return result;
	}

	public ModelResult updatePassword(String userId, String passwordOld, String passwordNew) {
		final ModelResult result = new ProxiManager.ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("user", "object:{"
				+ "\"_id\":\"" + userId + "\","
				+ "\"oldPassword\":\"" + passwordOld + "\","
				+ "\"newPassword\":\"" + passwordNew + "\""
				+ "}");

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_USER + "changepw")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
				.setRetry(false)
				.setSession(Aircandi.getInstance().getUser().session)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mEntityModel.dispatch(serviceRequest);

		return result;
	}

	@SuppressWarnings("ucd")
	public ModelResult insertLink(Link link) {
		final ModelResult result = new ProxiManager.ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST + link.getCollection())
				.setRequestType(RequestType.Insert)
				.setRequestBody(ProxibaseService.convertObjectToJsonSmart(link, true, true))
				.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
				.setRetry(false)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mEntityModel.dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.Link);
			result.data = serviceData.data;
		}

		return result;
	}

	ModelResult insertBeacon(Beacon beacon) {
		final ModelResult result = new ProxiManager.ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST + beacon.getCollection())
				.setRequestType(RequestType.Insert)
				.setRequestBody(ProxibaseService.convertObjectToJsonSmart(beacon, true, true))
				.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
				.setRetry(false)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mEntityModel.dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.Beacon);
			result.data = serviceData.data;
		}

		return result;
	}

	/**
	 * Inserts a new user into the service.
	 * 
	 * @param user
	 * @param bitmap
	 * @return a ModelResult object. The data property includes the just inserted user.
	 */

	public ModelResult insertUser(User user, Bitmap bitmap) {
		final ModelResult result = new ProxiManager.ModelResult();

		ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_USER + "create")
				.setRequestType(RequestType.Insert)
				.setRequestBody(ProxibaseService.convertObjectToJsonSmart(user, true, true))
				.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
				.setRetry(false)
				.setUseSecret(true)
				.setResponseFormat(ResponseFormat.Json);

		/*
		 * Insert user.
		 */
		result.serviceResponse = mEntityModel.dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {

			String jsonResponse = (String) result.serviceResponse.data;
			ServiceData serviceData = ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.None);
			user = serviceData.user;
			user.session = serviceData.session;
			/*
			 * Put image to S3 if we have one. Handles setting up the photo
			 * object on user
			 */
			if (bitmap != null && !bitmap.isRecycled()) {
				result.serviceResponse = mEntityModel.storeImageAtS3(null, user, bitmap);
			}

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				/*
				 * Update user to capture the uri for the image we saved.
				 */
				serviceRequest = new ServiceRequest()
						.setUri(user.getEntryUri())
						.setRequestType(RequestType.Update)
						.setRequestBody(ProxibaseService.convertObjectToJsonSmart(user, true, true))
						.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
						.setRetry(false)
						.setSession(user.session)
						.setResponseFormat(ResponseFormat.Json);

				/* Doing an update so we don't need anything back */
				result.serviceResponse = mEntityModel.dispatch(serviceRequest);

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					jsonResponse = (String) result.serviceResponse.data;
					serviceData = ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.User);
					final User insertedUser = (User) serviceData.data;
					insertedUser.session = user.session;
					result.data = insertedUser;
				}
			}
		}
		return result;
	}

	public ModelResult updateUser(User user, Bitmap bitmap, Boolean cacheOnly) {
		final ModelResult result = new ProxiManager.ModelResult();

		if (!cacheOnly) {
			/*
			 * TODO: We are going with a garbage collection scheme for orphaned images. We
			 * need to use an extended property on S3 items that is set to a date when
			 * collection is ok. This allows downloaded entities to keep working even if
			 * an image for entity has changed.
			 */

			/*
			 * Put image to S3 if we have a new one. Handles updating the photo
			 * object on user
			 */
			if (bitmap != null && !bitmap.isRecycled()) {
				result.serviceResponse = mEntityModel.storeImageAtS3(null, user, bitmap);
			}

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				/*
				 * Service handles modifiedId and modifiedDate based
				 * on the session info passed with request.
				 */
				final ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(user.getEntryUri())
						.setRequestType(RequestType.Update)
						.setRequestBody(ProxibaseService.convertObjectToJsonSmart(user, true, false))
						.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
						.setRetry(false)
						.setSession(Aircandi.getInstance().getUser().session)
						.setResponseFormat(ResponseFormat.Json);

				result.serviceResponse = mEntityModel.dispatch(serviceRequest);
				updateUser(user);
			}
		}
		else {
			/*
			 * entity.creator is what we show for entity authors. To make the entity model consistent
			 * with this update to the profile we walk all the entities and update where creator.id
			 * equals the signed in user.
			 */
			updateUser(user);
		}
		return result;
	}

	@SuppressWarnings("ucd")
	public ModelResult logAction(String targetId, String targetSource, String actionType) {
		final ModelResult result = new ProxiManager.ModelResult();
		final Bundle parameters = new Bundle();

		parameters.putString("targetId", targetId);
		parameters.putString("targetSource", targetSource);
		parameters.putString("actionType", actionType);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "logAction")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setSession(Aircandi.getInstance().getUser().session)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mEntityModel.dispatch(serviceRequest);
		return result;
	}

	// --------------------------------------------------------------------------------------------
	// Beacon routines
	// --------------------------------------------------------------------------------------------

	public void clearBeacons() {
		synchronized (mEntityModel.mEntityModel.mBeacons) {
			mEntityModel.mEntityModel.mBeacons.clear();
		}
	}

	void updateBeacons() {
		/*
		 * Makes sure that the beacon collection is an accurate representation
		 * of the latest wifi scan.
		 */
		clearBeacons();

		/*
		 * Insert beacons for the latest scan results.
		 */
		synchronized (mWifiList) {
			WifiScanResult scanResult = null;
			for (int i = 0; i < mWifiList.size(); i++) {
				scanResult = mWifiList.get(i);
				Beacon beacon = new Beacon(scanResult.BSSID
						, scanResult.SSID
						, scanResult.SSID
						, scanResult.level
						, DateUtils.nowDate()
						, scanResult.test);

				mEntityModel.mEntityModel.upsertBeacon(beacon);
			}
		}

		/* Sort beacons by signal strength */
		synchronized (mEntityModel.mEntityModel.mBeacons) {
			Collections.sort(mEntityModel.mEntityModel.mBeacons, new Beacon.SortBeaconsBySignalLevel());
		}

		mLastBeaconLockedDate = DateUtils.nowDate().getTime();

		BusProvider.getInstance().post(new BeaconsLockedEvent());
	}

	public Collection<Beacon> getBeacons() {
		return mBeacons;
	}

	public Beacon getBeacon(String beaconId) {
		for (Beacon beacon : mBeacons) {
			if (beacon.id.equals(beaconId)) {
				return beacon;
			}
		}
		return null;
	}

	// --------------------------------------------------------------------------------------------
	// Entity cache fetch routines
	// --------------------------------------------------------------------------------------------

	public ModelResult getEntitiesByListType(ArrayListType arrayListType, Boolean refresh, String collectionId, String userId, Integer limit) {
		/*
		 * Used by candi list
		 */
		ModelResult result = new ProxiManager.ModelResult();
		if (arrayListType == ArrayListType.TunedPlaces) {
			result.data = getAircandiPlaces();
		}
		else if (arrayListType == ArrayListType.SyntheticPlaces) {
			result.data = getRadarSynthetics();
		}
		else if (arrayListType == ArrayListType.OwnedByUser) {
			result = getUserEntities(userId, refresh, limit);
		}
		else if (arrayListType == ArrayListType.Collections) {
			result.data = getCollectionEntities();
		}
		else if (arrayListType == ArrayListType.InCollection) {
			result.data = getChildEntities(collectionId);
		}
		return result;
	}

	/**
	 * Returns top level place entities of all types that should be visible in radar. Entities that are hidden
	 * have already been screened out. Entities are pre-sorted by place rank score and distance.
	 * 
	 * @return
	 */

	public List<Entity> getAllPlaces(Boolean includeHidden) {
		/*
		 * This is the one case where refresh scenarios have been
		 * handled outside of this method.
		 */
		final List<Entity> entities = new ArrayList<Entity>();
		final Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(CandiConstants.PREF_SEARCH_RADIUS,
				CandiConstants.PREF_SEARCH_RADIUS_DEFAULT));

		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
					Entity entity = entry.getValue();
					if (includeHidden || !entity.hidden) {

						/* Must do this to cache the distance before sorting */
						Float distance = entity.getDistance();

						Beacon beacon = entity.getActiveBeacon(LinkType.proximity.name());
						if (beacon != null) {
							entities.add(entity);
						}
						else {
							/* No beacon for this entity so check using location */
							if (distance != null && distance != -1 && distance < searchRangeMeters) {
								entities.add(entity);
							}
						}
					}
				}
			}
		}

		Collections.sort(entities, new Entity.SortEntitiesByProximityAndDistance());
		return entities.size() > ProxiConstants.RADAR_PLACES_LIMIT ? entities.subList(0, ProxiConstants.RADAR_PLACES_LIMIT) : entities;
	}

	public Float getMaxPlaceDistance() {
		final List<Entity> places = getAllPlaces(true); // refreshes distance calculation
		Float maxDistance = 0f;
		for (Entity entity : places) {
			if (entity.distance > maxDistance) {
				maxDistance = entity.distance;
			}
		}
		return maxDistance;
	}

	/**
	 * Returns sorted list of all the entities that came from the aircandi service. Entities that are hidden
	 * have already been screened out. Only includes top level place entities.
	 * 
	 * @return
	 */

	public List<Entity> getAircandiPlaces() {
		/*
		 * This is the one case where refresh scenarios have been
		 * handled outside of this method.
		 */
		final List<Entity> entities = new ArrayList<Entity>();
		final Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(CandiConstants.PREF_SEARCH_RADIUS,
				CandiConstants.PREF_SEARCH_RADIUS_DEFAULT));

		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
					Entity entity = entry.getValue();
					if (!entity.synthetic) {
						Beacon beacon = entity.getActiveBeaconPrimary(LinkType.proximity.name());
						/* Must do this to cache the distance before sorting */
						Float distance = entity.getDistance();
						if (beacon != null) {
							entities.add(entity);
						}
						else {
							beacon = entity.getActiveBeaconPrimary(LinkType.browse.name());
							/*
							 * Entities that were first found by beacon hang around and could
							 * later be visible via location if we continue with this approach.
							 * 
							 * One thought is that if it qualifies then so be it. Sorting should
							 * put it in the right priority order and place with beacons should
							 * sort higher.
							 */
							/* No beacon for this entity so check using location */
							if (beacon != null || (distance != null && distance != -1 && distance < searchRangeMeters)) {
								entities.add(entity);
							}
						}
					}
				}
			}
		}
		Collections.sort(entities, new Entity.SortEntitiesByProximityAndDistance());
		return entities;
	}

	public List<Entity> getProximityPlaces() {
		/*
		 * This is the one case where refresh scenarios have been
		 * handled outside of this method.
		 */
		final List<Entity> entities = new ArrayList<Entity>();

		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
					Entity entity = entry.getValue();
					Beacon beacon = entity.getActiveBeaconPrimary(LinkType.proximity.name());
					if (beacon != null) {
						entities.add(entity);
					}
				}
			}
		}
		return entities;
	}

	public List<Entity> getCollectionEntities() {
		/*
		 * This is the one case where refresh scenarios have been
		 * handled outside of this method.
		 */
		final List<Entity> entities = new ArrayList<Entity>();
		final Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(CandiConstants.PREF_SEARCH_RADIUS,
				CandiConstants.PREF_SEARCH_RADIUS_DEFAULT));
		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().isCollection) {
					Entity entity = entry.getValue();
					if (!entity.hidden && !entity.synthetic) {
						Beacon beacon = entity.getActiveBeaconPrimary(LinkType.proximity.name());
						/* Must do this to cache the distance before sorting */
						Float distance = entity.getDistance();
						if (beacon != null) {
							entities.add(entity);
						}
						else {
							/*
							 * Entities that were first found by beacon hang around and could
							 * later be visible via location if we continue with this approach.
							 * 
							 * One thought is that if it qualifies then so be it. Sorting should
							 * put it in the right priority order and place with beacons should
							 * sort higher.
							 */
							/* No beacon for this entity so check using location */
							if (distance != null && distance < searchRangeMeters) {
								entities.add(entity);
							}
						}
					}
				}
			}
		}
		Collections.sort(entities, new Entity.SortEntitiesByProximityAndDistance());
		return entities;
	}

	public List<Entity> getRadarSynthetics() {
		/*
		 * This is the one case where refresh scenarios have been
		 * handled outside of this method.
		 */
		final List<Entity> entities = new ArrayList<Entity>();
		final Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(CandiConstants.PREF_SEARCH_RADIUS,
				CandiConstants.PREF_SEARCH_RADIUS_DEFAULT));
		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
					Entity entity = entry.getValue();
					if (!entity.hidden && entity.synthetic) {
						Float distance = entity.getDistance();
						Beacon beacon = entity.getActiveBeaconPrimary(LinkType.proximity.name());
						if (beacon != null) {
							entities.add(entity);
						}
						else {
							/* No beacon for this entity so check using location */
							if (distance != null && distance < searchRangeMeters) {
								entities.add(entity);
							}
						}
					}
				}
			}
		}
		Collections.sort(entities, new Entity.SortEntitiesByProximityAndDistance());
		return entities;
	}

	public List<Entity> getCacheUserEntities(String userId) {
		final List<Entity> entities = new ArrayList<Entity>();
		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().ownerId != null && entry.getValue().ownerId.equals(userId)) {
					entities.add(entry.getValue());
				}
			}
		}
		Collections.sort(entities, new Entity.SortEntitiesByModifiedDate());
		return entities;
	}

	public List<Entity> getCacheUserEntities(String userId, String type) {
		final List<Entity> entities = new ArrayList<Entity>();
		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().ownerId != null && entry.getValue().ownerId.equals(userId)) {
					if (entry.getValue().type.equals(type)) {
						entities.add(entry.getValue());
					}
				}
			}
		}
		Collections.sort(entities, new Entity.SortEntitiesByModifiedDate());
		return entities;
	}

	public List<Entity> getBeaconEntities(String beaconId) {
		final List<Entity> entities = new ArrayList<Entity>();
		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().getBeaconId() != null && entry.getValue().getBeaconId().equals(beaconId)) {
					entities.add(entry.getValue());
				}
			}
		}
		Collections.sort((List<Entity>) entities, new Entity.SortEntitiesByModifiedDate());
		return entities;
	}

	public Entity getCacheEntity(String entityId) {
		synchronized (mEntityCache) {
			return mEntityCache.get(entityId);
		}
	}

	@SuppressWarnings("ucd")
	public List<Entity> getCacheEntities(List<String> entityIds) {
		final List<Entity> entities = new ArrayList<Entity>();
		synchronized (mEntityCache) {
			for (String entityId : entityIds) {
				Entity entity = mEntityCache.get(entityId);
				if (entity != null) {
					entities.add(entity);
				}
			}
		}
		return entities;
	}

	public List<Entity> getChildEntities(String entityId) {
		final List<Entity> entities = new ArrayList<Entity>();
		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (!entry.getValue().type.equals(CandiConstants.TYPE_CANDI_SOURCE)
						&& entry.getValue().parentId != null
						&& entry.getValue().parentId.equals(entityId)) {
					entities.add(entry.getValue());
				}
			}
		}
		Collections.sort(entities, new Entity.SortEntitiesByModifiedDate());
		return entities;
	}

	public List<Entity> getSourceEntities(String entityId) {
		final List<Entity> entities = new ArrayList<Entity>();
		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().type.equals(CandiConstants.TYPE_CANDI_SOURCE)
						&& (entry.getValue().source.system == null || !entry.getValue().source.system)
						&& entry.getValue().parentId != null
						&& entry.getValue().parentId.equals(entityId)) {
					entities.add(entry.getValue());
				}
			}
		}
		Collections.sort(entities, new Entity.SortEntitiesBySourcePosition());
		return entities;
	}

	// --------------------------------------------------------------------------------------------
	// Other fetch routines
	// --------------------------------------------------------------------------------------------

	public Photo getPhoto(String imageUri) {
		for (Photo photo : mPhotos) {
			if (photo.getUri() != null) {
				if (photo.getUri().equals(imageUri)) {
					return photo;
				}
			}
			else {
				if (photo.getUri().equals(imageUri)) {
					return photo;
				}
			}
		}
		return null;
	}

	public List<String> getCategoriesAsStringArray(List<Category> categories) {
		final List<String> categoryStrings = new ArrayList<String>();
		for (Category category : categories) {
			categoryStrings.add(category.name);
		}
		return categoryStrings;
	}

	// --------------------------------------------------------------------------------------------
	// Entity cache modification routines
	// --------------------------------------------------------------------------------------------

	private void insertComment(String entityId, Comment comment) {
		final Entity entity = getCacheEntity(entityId);
		if (entity != null) {
			if (entity.comments == null) {
				entity.comments = new ArrayList<Comment>();
			}
			entity.comments.add(0, comment);
			if (entity.commentCount == null) {
				entity.commentCount = 0;
			}
			entity.commentCount++;
			setLastActivityDate(DateUtils.nowDate().getTime());
		}
	}

	void upsertBeacon(Beacon beacon) {
		synchronized (mBeacons) {

			final Beacon beaconOriginal = getBeacon(beacon.id);
			if (beaconOriginal != null) {
				Beacon.copyProperties(beacon, beaconOriginal);
			}
			else {
				mBeacons.add(beacon);
			}
		}
		setLastActivityDate(DateUtils.nowDate().getTime());
	}

	void upsertEntities(List<Entity> entities) {
		for (Entity entity : entities) {
			upsertEntity(entity);
		}
	}

	private Entity upsertEntity(Entity entity) {
		/*
		 * This is the only place we use the children property
		 * set when deserializing from the service. After this
		 * all references to the children are dynamically assembled
		 * in the getChildren method on entities.
		 */
		Entity original = entity;
		synchronized (mEntityCache) {
			final Entity entityOriginal = mEntityModel.mEntityModel.mEntityCache.get(entity.id);

			/* Check to see if we have this entity keyed to the sourceId */
			if (entityOriginal != null) {
				original = entityOriginal;
				Entity.copyProperties(entity, entityOriginal);
				/*
				 * We only do children work if the new entity has them.
				 */
				if (entity.children != null) {
					/*
					 * If entity and entityOriginal are the same object then there isn't
					 * anything to do since the cache by definition is up-to-date.
					 */
					if (!entity.children.equals(entityOriginal.children)) {

						/* Removes all children except source entities */
						final Map<String, Entity> removedChildren = removeChildren(entityOriginal.id);
						entityOriginal.children.clear();

						for (Entity childEntity : entity.children) {

							Entity removedChild = removedChildren.get(childEntity.id);
							if (removedChild != null) {
								Entity.copyProperties(childEntity, removedChild);
								mEntityCache.put(childEntity.id, removedChild);
								entityOriginal.children.add(removedChild);
							}
							else {
								mEntityCache.put(childEntity.id, childEntity);
								entityOriginal.children.add(childEntity);
							}

							removeSourceEntities(childEntity.id);
							addCommentSource(childEntity, null);
						}
					}
				}
			}
			else {
				mEntityCache.put(entity.id, entity);
				if (entity.children != null) {
					for (Entity childEntity : entity.children) {
						mEntityCache.put(childEntity.id, childEntity);
						removeSourceEntities(childEntity.id);
						addCommentSource(childEntity, null);
					}
				}
			}
		}

		/* Create virtual candi for sources */

		/* First remove any old stuff */
		removeSourceEntities(entity.id);

		Integer position = 0;
		if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {

			/* Add the current stuff */
			if (entity.sources != null) {
				for (Source source : entity.sources) {
					if ((source.system == null || !source.system) && (source.hidden == null || !source.hidden)) {
						source.intentSupport = true;
						if (source.type.equals("facebook")
								|| source.type.equals("yahoolocal")
								|| source.type.equals("citysearch")
								|| source.type.equals("citygrid")
								|| source.type.equals("urbanspoon")
								|| source.type.equals("opentable")
								|| source.type.equals("openmenu")) {
							source.intentSupport = false;
						}

						if (source.label == null) {
							source.label = source.type;
						}

						if (!mEntityModel.mEntityModel.mSourceMeta.containsKey(source.type)) {
							mEntityModel.mEntityModel.mSourceMeta.put(source.type, new Source(source.intentSupport, false));
						}

						/* We are getting duplicates which will have to be addressed on the service side */
						Entity sourceEntity = mEntityModel.loadEntityFromResources(R.raw.source_entity);
						if (sourceEntity != null) {
							/* Transfers from source item */
							sourceEntity.id = entity.id + "." + source.type + "." + String.valueOf(DateUtils.nowDate().getTime());
							sourceEntity.name = source.label;
							if (source.icon != null) {
								sourceEntity.photo = new Photo(source.icon, null, null, null, PhotoSource.external);
							}
							source.position = position;
							sourceEntity.source = source;
							sourceEntity.parentId = entity.id;
							upsertEntity(sourceEntity);
							position++;
						}
					}
				}
			}
		}

		/* Add comment source */

		if (!entity.type.equals(CandiConstants.TYPE_CANDI_SOURCE)) {
			addCommentSource(entity, position);
		}

		setLastActivityDate(DateUtils.nowDate().getTime());

		return original;
	}

	private void addCommentSource(Entity entity, Integer position) {

		final Entity sourceEntity = mEntityModel.loadEntityFromResources(R.raw.source_entity);
		sourceEntity.id = entity.id + ".comments";
		sourceEntity.name = "comments";
		final Source source = new Source();
		source.label = "comments";
		source.icon = "resource:img_post";
		source.type = "comments";
		source.position = (position != null) ? position : 0;
		source.intentSupport = false;
		source.installDeclined = false;
		sourceEntity.source = source;

		sourceEntity.commentCount = entity.commentCount;
		sourceEntity.parentId = entity.id;
		upsertEntity(sourceEntity);
	}

	private void updateUser(User user) {
		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().creatorId != null && entry.getValue().creatorId.equals(user.id)) {
					if (entry.getValue().creator != null) {
						entry.getValue().creator.photo = user.photo.clone();
						entry.getValue().creator.location = user.location;
						entry.getValue().creator.name = user.name;
					}
				}
				if (entry.getValue().ownerId != null && entry.getValue().ownerId.equals(user.id)) {
					if (entry.getValue().owner != null) {
						entry.getValue().owner.photo = user.photo.clone();
						entry.getValue().owner.location = user.location;
						entry.getValue().owner.name = user.name;
					}
				}
				if (entry.getValue().modifierId != null && entry.getValue().modifierId.equals(user.id)) {
					if (entry.getValue().modifier != null) {
						entry.getValue().modifier.photo = user.photo.clone();
						entry.getValue().modifier.location = user.location;
						entry.getValue().modifier.name = user.name;
					}
				}
			}
		}
		setLastActivityDate(DateUtils.nowDate().getTime());
	}

	private void moveEntity(String moveEntityId, String parentId, String beaconId) {
		final Entity entity = getCacheEntity(moveEntityId);
		entity.parentId = parentId;
		setLastActivityDate(DateUtils.nowDate().getTime());
	}

	@SuppressWarnings("ucd")
	public void removeEntitiesForUser(String userId) {
		/*
		 * We clean out user entities and their children when the entity
		 * is associated with a beacon that isn't a radar hit.
		 */
		synchronized (mEntityCache) {
			final Iterator iter = mEntityModel.mEntityModel.mEntityCache.keySet().iterator();
			Entity entity = null;
			while (iter.hasNext()) {
				entity = mEntityModel.mEntityModel.mEntityCache.get(iter.next());
				if (entity.parentId == null && entity.ownerId.equals(userId)) {
					iter.remove();
				}
			}
		}
		setLastActivityDate(DateUtils.nowDate().getTime());
	}

	void removeSyntheticEntities() {
		synchronized (mEntityCache) {
			final Iterator iter = mEntityModel.mEntityModel.mEntityCache.keySet().iterator();
			Entity entity = null;
			while (iter.hasNext()) {
				entity = mEntityModel.mEntityModel.mEntityCache.get(iter.next());
				if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)
						&& entity.synthetic) {
					iter.remove();
				}
			}
		}
		setLastActivityDate(DateUtils.nowDate().getTime());
	}

	void removeBeaconEntities() {
		synchronized (mEntityCache) {
			final Iterator iter = mEntityModel.mEntityModel.mEntityCache.keySet().iterator();
			Entity entity = null;
			while (iter.hasNext()) {
				entity = mEntityModel.mEntityModel.mEntityCache.get(iter.next());
				if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)
						&& !entity.synthetic
						&& (entity.links != null && entity.links.size() > 0)) {
					iter.remove();
				}
			}
		}
		setLastActivityDate(DateUtils.nowDate().getTime());
	}

	@SuppressWarnings("ucd")
	public void removeEntitiesForBeacon(String beaconId) {
		synchronized (mEntityCache) {
			final Iterator iter = mEntityModel.mEntityModel.mEntityCache.keySet().iterator();
			Entity entity = null;
			while (iter.hasNext()) {
				entity = mEntityModel.mEntityModel.mEntityCache.get(iter.next());
				if (entity.parentId == null
						&& entity.getBeaconId() != null
						&& entity.getBeaconId().equals(beaconId)) {
					iter.remove();
				}
			}
		}
		setLastActivityDate(DateUtils.nowDate().getTime());
	}

	Entity removeEntity(String entityId) {
		/*
		 * Clean out every entity related to entityId
		 */
		Entity entity = null;
		synchronized (mEntityCache) {
			entity = mEntityCache.remove(entityId);
		}
		removeChildren(entityId);
		removeSourceEntities(entityId);
		setLastActivityDate(DateUtils.nowDate().getTime());
		return entity;
	}

	public void removeAllEntities() {
		synchronized (mEntityCache) {
			mEntityCache.clear();
		}
	}

	private Map<String, Entity> removeChildren(String entityId) {
		/*
		 * This will clean all entities that have entityId as parentId. This
		 * will take care synthesized source entities as well.
		 */
		final Map<String, Entity> entities = new HashMap<String, Entity>();
		synchronized (mEntityCache) {
			final Iterator iter = mEntityModel.mEntityModel.mEntityCache.keySet().iterator();
			Entity entity = null;
			while (iter.hasNext()) {
				entity = mEntityModel.mEntityModel.mEntityCache.get(iter.next());
				if (!entity.type.equals(CandiConstants.TYPE_CANDI_SOURCE)
						&& entity.parentId != null
						&& entity.parentId.equals(entityId)) {
					entities.put(entity.id, entity);
					iter.remove();
				}
			}
		}
		return entities;
	}

	private Map<String, Entity> removeSourceEntities(String entityId) {
		/*
		 * This will clean all entities that have entityId as parentId. This
		 * will take care synthesized source entities as well.
		 */
		final Map<String, Entity> entities = new HashMap<String, Entity>();
		synchronized (mEntityCache) {
			final Iterator iter = mEntityModel.mEntityModel.mEntityCache.keySet().iterator();
			Entity entity = null;
			while (iter.hasNext()) {
				entity = mEntityModel.mEntityModel.mEntityCache.get(iter.next());
				if (entity.type.equals(CandiConstants.TYPE_CANDI_SOURCE)
						&& entity.parentId != null
						&& entity.parentId.equals(entityId)) {
					entities.put(entity.id, entity);
					iter.remove();
				}
			}
		}
		return entities;
	}

	// --------------------------------------------------------------------------------------------
	// Set/Get routines
	// --------------------------------------------------------------------------------------------

	public Number getLastBeaconRefreshDate() {
		return mLastRefreshDate;
	}

	public void setLastBeaconRefreshDate(Number lastRefreshDate) {
		mLastRefreshDate = lastRefreshDate;
	}

	public Number getLastActivityDate() {
		return mLastActivityDate;
	}

	public void setLastActivityDate(Number lastActivityDate) {
		mLastActivityDate = lastActivityDate;
	}

	public List<Photo> getPhotos() {
		return mPhotos;
	}

	public void setPhotos(List<Photo> photos) {
		mPhotos = photos;
	}

	public List<Category> getCategories() {
		return mCategories;
	}

	public Map<String, Source> getSourceMeta() {
		return mSourceMeta;
	}

	public Number getLastBeaconLockedDate() {
		return mLastBeaconLockedDate;
	}

	public void setLastBeaconDate(Number lastBeaconDate) {
		mLastBeaconLockedDate = lastBeaconDate;
	}

	public List<WifiScanResult> getWifiList() {
		return mWifiList;
	}

	public void setWifiList(List<WifiScanResult> wifiList) {
		mWifiList = wifiList;
	}

}