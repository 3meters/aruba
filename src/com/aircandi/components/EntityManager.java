package com.aircandi.components;

// import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.BuildConfig;
import com.aircandi.Constants;
import com.aircandi.ServiceConstants;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.service.RequestType;
import com.aircandi.service.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.ServiceResponse;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.CacheStamp;
import com.aircandi.service.objects.Candigram;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Cursor;
import com.aircandi.service.objects.Device;
import com.aircandi.service.objects.Document;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Provider;
import com.aircandi.service.objects.Proximity;
import com.aircandi.service.objects.ServiceBase.UpdateScope;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.ServiceEntry;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.Stat;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;

public class EntityManager {

	private static final EntityCache	mEntityCache	= new EntityCache();
	private Number						mActivityDate;

	/*
	 * The photo collection enables swiping between photos while staying at the same level of the hierarchy.
	 */
	private List<Photo>					mPhotos			= Collections.synchronizedList(new ArrayList<Photo>());
	/*
	 * Categories are cached by a background thread.
	 */
	private List<Category>				mCategories		= Collections.synchronizedList(new ArrayList<Category>());

	private EntityManager() {}

	private static class EntityModelHolder {
		public static final EntityManager	instance	= new EntityManager();
	}

	public static EntityManager getInstance() {
		return EntityModelHolder.instance;
	}

	ServiceResponse dispatch(ServiceRequest serviceRequest) {
		/*
		 * We use this as a choke point for all calls to the aircandi service.
		 */
		final ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest, null);
		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Cache queries
	// --------------------------------------------------------------------------------------------

	public static Entity getEntity(String entityId) {
		return mEntityCache.get(entityId);
	}

	// --------------------------------------------------------------------------------------------
	// Combo service/cache queries
	// --------------------------------------------------------------------------------------------

	public synchronized ModelResult getEntity(String entityId, Boolean refresh, LinkOptions linkOptions) {
		/*
		 * Retrieves entity from cache if available otherwise downloads the entity from the service. If refresh is true
		 * then bypasses the cache and downloads from the service.
		 */
		final ModelResult result = getEntities(Arrays.asList(entityId), refresh, linkOptions);
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final List<Entity> entities = (List<Entity>) result.data;
			if (entities.size() > 0) {
				result.data = entities.get(0);
			}
			else {
				result.data = null;
			}
		}
		return result;
	}

	private synchronized ModelResult getEntities(List<String> entityIds, Boolean refresh, LinkOptions linkOptions) {
		/*
		 * Results in a service request if missing entities or refresh is true.
		 */
		final ModelResult result = new ModelResult();

		final List<String> loadEntityIds = new ArrayList<String>();
		List<Entity> entities = new ArrayList<Entity>();

		for (String entityId : entityIds) {
			Entity entity = mEntityCache.get(entityId);
			if (refresh || entity == null) {
				loadEntityIds.add(entityId);
			}
			else {
				entities.add(entity);
			}
		}
		result.data = entities;

		if (loadEntityIds.size() > 0) {
			result.serviceResponse = mEntityCache.loadEntities(loadEntityIds, linkOptions);
			if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
				ServiceData serviceData = (ServiceData) result.serviceResponse.data;
				result.data = serviceData.data;
			}
		}
		return result;
	}

	public synchronized ModelResult loadEntitiesForEntity(String entityId, LinkOptions linkOptions, Cursor cursor, Stopwatch stopwatch) {
		final ModelResult result = new ModelResult();

		result.serviceResponse = mEntityCache.loadEntitiesForEntity(entityId, linkOptions, cursor, stopwatch);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			ServiceData serviceData = (ServiceData) result.serviceResponse.data;
			result.data = serviceData.data;
		}
		return result;
	}

	// --------------------------------------------------------------------------------------------
	// service queries
	// --------------------------------------------------------------------------------------------

	public synchronized CacheStamp loadCacheStamp(String entityId, CacheStamp cacheStamp) {

		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("entityId", entityId);

		if (cacheStamp.activityDate != null) {
			parameters.putLong("activityDate", cacheStamp.activityDate.longValue());
		}

		if (cacheStamp.modifiedDate != null) {
			parameters.putLong("modifiedDate", cacheStamp.modifiedDate.longValue());
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "checkActivity")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = dispatch(serviceRequest);

		/* In case of a failure, we echo back the provided cache stamp */
		CacheStamp cacheStampService = cacheStamp.clone();

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.RESULT, Json.ServiceDataWrapper.TRUE);
			if (serviceData.data != null) {
				cacheStampService = (CacheStamp) serviceData.data;
			}
		}
		return cacheStampService;
	}

	public synchronized ModelResult loadCategories() {

		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("source", "foursquare");

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_PLACES + "categories")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.CATEGORY, Json.ServiceDataWrapper.TRUE);
			mCategories = (List<Category>) serviceData.data;
			result.serviceResponse.data = mCategories;
		}
		return result;
	}

	public ModelResult refreshApplinks(List<Entity> applinks) {

		final ModelResult result = new ModelResult();
		final Bundle parameters = new Bundle();

		final List<String> entityStrings = new ArrayList<String>();
		for (Entity applink : applinks) {
			entityStrings.add("object:" + Json.objectToJson(applink, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));
		}
		parameters.putStringArrayList("applinks", (ArrayList<String>) entityStrings);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_APPLINKS + "refresh")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.APPLINK, Json.ServiceDataWrapper.TRUE);
			result.data = serviceData.data; // updated collection of applinks
		}
		return result;
	}

	public ModelResult suggestApplinks(List<Entity> applinks, Place place) {

		final ModelResult result = new ModelResult();
		final Bundle parameters = new Bundle();

		parameters.putString("place", "object:" + Json.objectToJson(place, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));

		final List<String> entityStrings = new ArrayList<String>();
		for (Entity applink : applinks) {
			entityStrings.add("object:" + Json.objectToJson(applink, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));
		}
		parameters.putStringArrayList("applinks", (ArrayList<String>) entityStrings);
		parameters.putInt("timeout", ServiceConstants.SOURCE_SUGGESTIONS_TIMEOUT);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_APPLINKS + "suggest")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.APPLINK, Json.ServiceDataWrapper.TRUE);
			result.data = serviceData.data; // updated collection of applinks
		}
		return result;
	}

	public ModelResult getPlacePhotos(Provider provider, long count, long offset) {
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("provider", provider.type);
		parameters.putString("id", provider.id);
		parameters.putLong("limit", count);
		parameters.putLong("skip", offset);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "getPlacePhotos")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.PHOTO, Json.ServiceDataWrapper.TRUE);
			final List<Photo> photos = (List<Photo>) serviceData.data;
			result.serviceResponse.data = photos;
		}
		return result;

	}

	private ModelResult getDocumentId(String collection) {

		final ModelResult result = new ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_REST + collection + "/genId")
				.setRequestType(RequestType.GET)
				.setSuppressUI(true)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest, null);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.SERVICE_ENTRY, Json.ServiceDataWrapper.TRUE);
			result.serviceResponse.data = ((ServiceEntry) serviceData.data).id;
		}
		return result;
	}

	// --------------------------------------------------------------------------------------------
	// user updates
	// --------------------------------------------------------------------------------------------

	public ModelResult signin(String email, String password, String activityName) {
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("user", "object:{"
				+ "\"email\":\"" + email + "\","
				+ "\"password\":\"" + password + "\""
				+ "}");

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_AUTH + "signin")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setActivityName(activityName)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = dispatch(serviceRequest);
		return result;

	}

	@SuppressWarnings("ucd")
	public ModelResult signout() {
		final ModelResult result = new ModelResult();

		final User user = Aircandi.getInstance().getUser();
		if (user != null && user.session != null) {
			/*
			 * We use a short timeout with no retry because failure doesn't
			 * really hurt anything.
			 */
			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_AUTH + "signout")
					.setRequestType(RequestType.GET)
					.setSession(user.session)
					.setResponseFormat(ResponseFormat.JSON);

			result.serviceResponse = dispatch(serviceRequest);
		}

		return result;
	}

	public ModelResult updatePassword(String userId, String passwordOld, String passwordNew, String activityName) {
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("user", "object:{"
				+ "\"_id\":\"" + userId + "\","
				+ "\"oldPassword\":\"" + passwordOld + "\","
				+ "\"newPassword\":\"" + passwordNew + "\""
				+ "}");

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_USER + "changepw")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setActivityName(activityName)
				.setSession(Aircandi.getInstance().getUser().session)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = dispatch(serviceRequest);

		return result;
	}

	public ModelResult registerUser(User user, Link link, Bitmap bitmap) {

		/* Pre-fetch an id so a failed request can be retried */
		final ModelResult result = getDocumentId(User.collectionId);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			user.id = (String) result.serviceResponse.data;

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_USER + "create")
					.setRequestType(RequestType.INSERT)
					.setRequestBody(Json.objectToJson(user, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE))
					.setUseSecret(true)
					.setResponseFormat(ResponseFormat.JSON);

			/* insert user. */
			result.serviceResponse = dispatch(serviceRequest);

			if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

				String jsonResponse = (String) result.serviceResponse.data;
				ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);
				user = serviceData.user;
				user.session = serviceData.session;
				/*
				 * Put image to S3 if we have one. Handles setting up the photo
				 * object on user
				 */
				if (bitmap != null && !bitmap.isRecycled()) {
					result.serviceResponse = storeImageAtS3(null, user, bitmap);
				}

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					/*
					 * Update user to capture the uri for the image we saved.
					 */
					serviceRequest = new ServiceRequest()
							.setUri(user.getEntryUri())
							.setRequestType(RequestType.UPDATE)
							.setRequestBody(Json.objectToJson(user, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE))
							.setSession(user.session)
							.setResponseFormat(ResponseFormat.JSON);

					/* Doing an update so we don't need anything back */
					result.serviceResponse = dispatch(serviceRequest);

					if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
						jsonResponse = (String) result.serviceResponse.data;
						serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.USER, Json.ServiceDataWrapper.TRUE);
						final User insertedUser = (User) serviceData.data;
						insertedUser.session = user.session;
						result.data = insertedUser;
					}
				}
			}

		}

		return result;
	}

	public ModelResult checkSession() {
		ModelResult result = new ModelResult();
		Logger.i(this, "Calling checkSession");

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "checkSession")
				.setRequestType(RequestType.GET)
				.setResponseFormat(ResponseFormat.JSON);

		if (Aircandi.getInstance().getUser() != null) {
			serviceRequest.setSession(Aircandi.getInstance().getUser().session);
		}

		result.serviceResponse = dispatch(serviceRequest);
		return result;
	}

	public ModelResult getUserStats(String entityId) {
		ModelResult result = new ModelResult();

		// http://ariseditions.com:8080/data/actions?countBy=_user,type&find={"_user":"us.000000.00000.000.000001"}
		try {
			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_REST
							+ "actions?countBy="
							+ URLEncoder.encode("_user,type", "utf-8")
							+ "&find="
							+ URLEncoder.encode("{\"_user\":\"" + entityId + "\"}", "utf-8"))
					.setRequestType(RequestType.GET)
					.setResponseFormat(ResponseFormat.JSON);

			if (Aircandi.getInstance().getUser() != null) {
				serviceRequest.setSession(Aircandi.getInstance().getUser().session);
			}

			result.serviceResponse = dispatch(serviceRequest);
			if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
				final String jsonResponse = (String) result.serviceResponse.data;
				final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.STAT, Json.ServiceDataWrapper.TRUE);
				final List<Stat> stats = (List<Stat>) serviceData.data;
				result.data = stats;
			}

		}
		catch (UnsupportedEncodingException exception) {
			exception.printStackTrace();
		}
		return result;
	}

	// --------------------------------------------------------------------------------------------
	// Entity updates
	// --------------------------------------------------------------------------------------------

	private ModelResult insertEntity(Entity entity) {
		return insertEntity(entity, null, null, null, null);
	}

	public ModelResult insertEntity(Entity entity, Link link, List<Beacon> beacons, Beacon primaryBeacon, Bitmap bitmap) {
		/*
		 * Inserts the entity in the entity service collection and Links are created to all the included beacons. The
		 * inserted entity is retrieved from the service and pushed into the local cache. The cached entity is returned
		 * in the data property of the result object.
		 * 
		 * Updates activityDate in the database:
		 * - on any upstream entities linked to in the process
		 * - beacons links can be created
		 * - custom link can be created
		 * - create link is created from user but not followed
		 */

		ModelResult result = new ModelResult();

		Logger.i(this, "Inserting entity: " + entity.name);

		/* Pre-fetch an id so a failed request can be retried */
		result = getDocumentId(entity.getCollection());

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			entity.id = (String) result.serviceResponse.data;
			/*
			 * Upload image to S3 as needed
			 */
			if (bitmap != null && !bitmap.isRecycled()) {
				result.serviceResponse = storeImageAtS3(entity, null, bitmap);
			}

			if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

				/* Construct entity, link, and observation */
				final Bundle parameters = new Bundle();

				/* Primary beacon id */
				if (primaryBeacon != null) {
					parameters.putString("primaryBeaconId", primaryBeacon.id);
				}

				if (beacons != null && beacons.size() > 0) {
					/*
					 * Linking to beacons or sending to support NEARBY notifications
					 */
					final List<String> beaconStrings = new ArrayList<String>();

					for (Beacon beacon : beacons) {
						AirLocation location = LocationManager.getInstance().getAirLocationLocked();
						if (location != null && !location.zombie) {

							beacon.location = new AirLocation();

							beacon.location.lat = location.lat;
							beacon.location.lng = location.lng;

							if (location.altitude != null) {
								beacon.location.altitude = location.altitude;
							}
							if (location.accuracy != null) {
								beacon.location.accuracy = location.accuracy;
							}
							if (location.bearing != null) {
								beacon.location.bearing = location.bearing;
							}
							if (location.speed != null) {
								beacon.location.speed = location.speed;
							}
							if (location.provider != null) {
								beacon.location.provider = location.provider;
							}
						}

						beacon.type = Constants.TYPE_BEACON_FIXED;
						beacon.locked = false;
						beaconStrings.add("object:" + Json.objectToJson(beacon, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));
					}
					parameters.putStringArrayList("beacons", (ArrayList<String>) beaconStrings);
				}

				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					Place place = (Place) entity;

					/* Sources configuration */
					if (!place.getProvider().type.equals("aircandi")) {
						parameters.putBoolean("insertApplinks", true);
						parameters.putInt("applinksTimeout", 10000);
					}

					/* PROVIDER id if this is a custom place */
					if (place.provider.aircandi != null) {
						place.provider.aircandi = entity.id;
					}
				}

				/* Link */
				if (link != null) {
					parameters.putString("link", "object:" + Json.objectToJson(link, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));
				}

				/* Entity */
				parameters.putString("entity", "object:" + Json.objectToJson(entity, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));

				if (entity.synthetic) {
					parameters.putBoolean("skipNotifications", true);
				}

				final ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "insertEntity")
						.setRequestType(RequestType.METHOD)
						.setParameters(parameters)
						.setSession(Aircandi.getInstance().getUser().session)
						.setResponseFormat(ResponseFormat.JSON);

				result.serviceResponse = dispatch(serviceRequest);
			}

			if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

				Json.ObjectType serviceDataType = Json.ObjectType.ENTITY;

				if (entity.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) serviceDataType = Json.ObjectType.APPLINK;
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_BEACON)) serviceDataType = Json.ObjectType.BEACON;
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) serviceDataType = Json.ObjectType.COMMENT;
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) serviceDataType = Json.ObjectType.PLACE;
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) serviceDataType = Json.ObjectType.POST;

				final String jsonResponse = (String) result.serviceResponse.data;
				final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, serviceDataType, Json.ServiceDataWrapper.TRUE);
				final Entity insertedEntity = (Entity) serviceData.data;
				/*
				 * Optimization: ADD soft 'create' link so user entity doesn't have to be refetched
				 */
				if (!entity.synthetic) {
					Aircandi.getInstance().getUser().activityDate = DateTime.nowDate().getTime();
					mEntityCache.addLink(insertedEntity.id
							, Constants.TYPE_LINK_CREATE
							, Aircandi.getInstance().getUser().id
							, insertedEntity.getShortcut()
							, Aircandi.getInstance().getUser().getShortcut());
				}

				result.data = insertedEntity;

				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					mActivityDate = DateTime.nowDate().getTime();
				}
			}
		}

		return result;
	}

	public ModelResult updateEntity(Entity entity, Bitmap bitmap) {
		/*
		 * Updates activityDate in the database:
		 * - on the updated entity
		 * - on any upstream entities the updated entity is linked to
		 * - inactive links are excluded
		 * - like/create/WATCH links are not followed
		 */
		final ModelResult result = new ModelResult();

		/* Upload new images to S3 as needed. */
		if (bitmap != null) {
			result.serviceResponse = storeImageAtS3(entity, null, bitmap);
		}

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
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
			parameters.putBoolean("returnEntity", false);
			entity.updateScope = UpdateScope.OBJECT;
			parameters.putString("entity", "object:" + Json.objectToJson(entity, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "updateEntity")
					.setRequestType(RequestType.METHOD)
					.setParameters(parameters)
					.setSession(Aircandi.getInstance().getUser().session)
					.setResponseFormat(ResponseFormat.JSON);

			result.serviceResponse = dispatch(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			/*
			 * Optimization: We crawl entities in the cache and update embedded
			 * user objects so we don't have to refresh all the affected entities
			 * from the service.
			 */
			if (entity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				mEntityCache.updateEntityUser(entity);
			}

			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				mActivityDate = DateTime.nowDate().getTime();
			}
		}

		return result;
	}

	public ModelResult deleteEntity(String entityId, Boolean cacheOnly) {
		/*
		 * Updates activityDate in the database:
		 * - on any upstream entities the deleted entity was linked to
		 * - inactive links are excluded
		 * - like/create/WATCH links are not followed
		 */
		final ModelResult result = new ModelResult();
		Entity entity = null;

		if (!cacheOnly) {
			entity = mEntityCache.get(entityId);
			/*
			 * Delete the entity and all links and observations it is associated with. We attempt to continue even
			 * if the call to delete the image failed.
			 */
			Logger.i(this, "Deleting entity: " + entity.name);

			final Bundle parameters = new Bundle();
			parameters.putString("entityId", entity.id);
			parameters.putBoolean("deleteChildren", true);

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "deleteEntity")
					.setRequestType(RequestType.METHOD)
					.setParameters(parameters)
					.setSession(Aircandi.getInstance().getUser().session)
					.setResponseFormat(ResponseFormat.JSON);

			result.serviceResponse = dispatch(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			mEntityCache.removeEntityTree(entityId);
			/*
			 * Remove 'create' link
			 * 
			 * FIXME: This needs to be generalized to hunt down all links that have
			 * this entity at either end and clean them up including any counts.
			 */
			Aircandi.getInstance().getUser().activityDate = DateTime.nowDate().getTime();
			mEntityCache.removeLink(entityId, Constants.TYPE_LINK_CREATE, Aircandi.getInstance().getUser().id);

			if (entity != null && entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				mActivityDate = DateTime.nowDate().getTime();
			}
		}
		return result;
	}

	public ModelResult trackEntity(Entity entity, List<Beacon> beacons, Beacon primaryBeacon, Boolean untuning) {

		final ModelResult result = new ModelResult();
		Logger.i(this, untuning ? "Untracking entity" : "Tracking entity");

		/* Construct entity, link, and observation */
		final Bundle parameters = new Bundle();

		/* Beacons */
		if (primaryBeacon != null) {
			parameters.putString("primaryBeaconId", primaryBeacon.id);
		}

		if (beacons != null && beacons.size() > 0) {

			final List<String> beaconStrings = new ArrayList<String>();
			final List<String> beaconIds = new ArrayList<String>();
			for (Beacon beacon : beacons) {
				if (beacon.id.equals(primaryBeacon.id)) {
					AirLocation location = LocationManager.getInstance().getAirLocationLocked();
					if (location != null && !location.zombie) {

						beacon.location = new AirLocation();

						beacon.location.lat = location.lat;
						beacon.location.lng = location.lng;

						if (location.altitude != null) {
							beacon.location.altitude = location.altitude;
						}
						if (location.accuracy != null) {
							beacon.location.accuracy = location.accuracy;
						}
						if (location.bearing != null) {
							beacon.location.bearing = location.bearing;
						}
						if (location.speed != null) {
							beacon.location.speed = location.speed;
						}
						if (location.provider != null) {
							beacon.location.provider = location.provider;
						}
					}
				}

				beacon.type = Constants.TYPE_BEACON_FIXED;
				beacon.locked = false;
				beaconStrings.add("object:" + Json.objectToJson(beacon, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));
				beaconIds.add(beacon.id);
			}

			if (untuning) {
				parameters.putStringArrayList("beaconIds", (ArrayList<String>) beaconIds);
			}
			else {
				parameters.putStringArrayList("beacons", (ArrayList<String>) beaconStrings);
			}
		}

		/* Entity */
		parameters.putString("entityId", entity.id);

		/* Method */
		String methodName = untuning ? "untrackEntity" : "trackEntity";

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + methodName)
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setSession(Aircandi.getInstance().getUser().session)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = dispatch(serviceRequest);

		/* Reproduce the service call effect locally */
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

			if (beacons != null) {
				for (Beacon beacon : beacons) {
					Boolean primary = (primaryBeacon != null && primaryBeacon.id.equals(beacon.id));
					Link link = entity.getLink(Constants.TYPE_LINK_PROXIMITY, beacon.id, Direction.out);
					if (link != null) {
						if (primary) {
							if (untuning) {
								link.incrementStat(Constants.TYPE_COUNT_LINK_PROXIMITY_MINUS);
							}
							else {
								link.incrementStat(Constants.TYPE_COUNT_LINK_PROXIMITY);
								if (!link.proximity.primary) {
									link.proximity.primary = true;
								}
							}
							/*
							 * If score goes to zero then the proximity links got deleted by the service.
							 * We want to mirror that in the cache without reloading the entity.
							 */
							if (link.getProximityScore() <= 0) {
								Iterator<Link> iterLinks = entity.linksOut.iterator();
								while (iterLinks.hasNext()) {
									Link temp = iterLinks.next();
									if (temp.equals(link)) {
										iterLinks.remove();
										break;
									}
								}
							}
						}
					}
					else {
						link = new Link(beacon.id, Constants.TYPE_LINK_PROXIMITY, false, entity.id);
						link.proximity = new Proximity();
						link.proximity.signal = beacon.signal;
						if (primary) {
							link.incrementStat(Constants.TYPE_COUNT_LINK_PROXIMITY);
							link.proximity.primary = true;
						}
						if (entity.linksOut == null) {
							entity.linksOut = new ArrayList<Link>();
						}
						entity.linksOut.add(link);
					}
				}
			}
		}

		return result;
	}

	public ModelResult insertLink(String fromId, String toId, String type, Boolean strong, Shortcut toShortcut, Shortcut fromShortcut, String actionType) {
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("fromId", fromId); 		// required
		parameters.putString("toId", toId);				// required
		parameters.putString("type", type);				// required
		parameters.putBoolean("strong", strong);
		parameters.putString("actionType", actionType);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "insertLink")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setSession(Aircandi.getInstance().getUser().session)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = dispatch(serviceRequest);
		/*
		 * We update the cache directly instead of refreshing from the service
		 */
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			/*
			 * Fail could be because of ServiceConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE which is what
			 * prevents any user from liking the same entity more than once.
			 */
			mEntityCache.addLink(toId, type, fromId, toShortcut, fromShortcut);
		}

		return result;
	}

	public ModelResult deleteLink(String fromId, String toId, String type, String actionType) {
		final ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("fromId", fromId); 		// required
		parameters.putString("toId", toId);				// required
		parameters.putString("type", type);				// required
		parameters.putString("actionType", actionType);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "deleteLink")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setSession(Aircandi.getInstance().getUser().session)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = dispatch(serviceRequest);
		/*
		 * We update the cache directly instead of refreshing from the service
		 */
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			/*
			 * Fail could be because of ServiceConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE which is what
			 * prevents any user from liking the same entity more than once.
			 */
			mEntityCache.removeLink(toId, type, fromId);
		}

		return result;
	}

	public ModelResult replaceEntitiesForEntity(String entityId, List<Entity> entitiesForEntity, String linkType) {
		/*
		 * Updates activityDate in the database:
		 * - on the parent entity
		 * - on any other upstream entities
		 * - inactive links are not followed
		 * - like/create/WATCH/proximity links are not followed
		 */
		final ModelResult result = new ModelResult();

		/* Upload new images to S3 as needed. */
		for (Entity entity : entitiesForEntity) {
			if (entity.photo != null && entity.photo.hasBitmap()) {
				result.serviceResponse = storeImageAtS3(entity, null, entity.photo.getBitmap());
				if (result.serviceResponse.responseCode != ResponseCode.SUCCESS) {
					return result;
				}
			}
		}

		final Bundle parameters = new Bundle();
		parameters.putString("entityId", entityId);

		final List<String> entityStrings = new ArrayList<String>();
		for (Entity entity : entitiesForEntity) {
			if (entity.isTempId()) {
				entity.id = null;
			}
			entityStrings.add("object:" + Json.objectToJson(entity, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE));
		}
		parameters.putStringArrayList("entities", (ArrayList<String>) entityStrings);
		parameters.putString("linkType", linkType);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "replaceEntitiesForEntity")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		if (Aircandi.getInstance().getUser() != null) {
			serviceRequest.setSession(Aircandi.getInstance().getUser().session);
		}

		result.serviceResponse = dispatch(serviceRequest);

		return result;
	}

	public ModelResult moveCandigram(Entity entity, Boolean skipMove, String toId) {
		/*
		 * moveCandigrams updates activityDate in the database:
		 * - on the candigram
		 * - on the old place candigram was linked to
		 * - on the new place candigram is linked to
		 * - on any other upstream entities with valid links
		 * - inactive links are not followed
		 * - like/create/WATCH/proximity links are not followed
		 */

		final ModelResult result = new ModelResult();

		/* Construct entity, link, and observation */
		final Bundle parameters = new Bundle();
		parameters.putString("method", "range");
		if (toId != null) {
			parameters.putString("toId", toId);
		}
		if (skipMove != null) {
			parameters.putBoolean("skipMove", skipMove);
		}
		parameters.putStringArrayList("entityIds", new ArrayList(Arrays.asList(entity.id)));

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "moveCandigrams")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setSession(Aircandi.getInstance().getUser().session)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = dispatch(serviceRequest);

		/* Return the new place the candigram has moved to */
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = (ServiceData) Json.jsonToObjects(jsonResponse, Json.ObjectType.ENTITY, Json.ServiceDataWrapper.TRUE);
			result.data = serviceData.data;
		}

		return result;
	}

	// --------------------------------------------------------------------------------------------
	// Other service tasks
	// --------------------------------------------------------------------------------------------

	public CacheStamp getCacheStamp() {
		CacheStamp cacheStamp = new CacheStamp(mActivityDate, null);
		return cacheStamp;
	}

	public ModelResult registerDevice(Device device) {
		ModelResult result = new ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_REST + Device.collectionId)
				.setRequestType(RequestType.INSERT)
				.setRequestBody(Json.objectToJson(device, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE))
				.setIgnoreResponseData(true)
				.setSession(Aircandi.getInstance().getUser().session)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = dispatch(serviceRequest);
		return result;
	}

	public ModelResult unregisterDevice(String registrationId) {
		ModelResult result = new ModelResult();
		final Bundle parameters = new Bundle();
		parameters.putString("registrationId", registrationId);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_METHOD + "unregisterDevice")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = dispatch(serviceRequest);
		return result;
	}

	public ModelResult insertDocument(Document document) {

		/* Pre-fetch an id so a failed request can be retried */
		ModelResult result = getDocumentId(Document.collectionId);

		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			document.id = (String) result.serviceResponse.data;
			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_REST + document.getCollection())
					.setRequestType(RequestType.INSERT)
					.setRequestBody(Json.objectToJson(document, Json.UseAnnotations.TRUE, Json.ExcludeNulls.TRUE))
					.setSession(Aircandi.getInstance().getUser().session)
					.setResponseFormat(ResponseFormat.JSON);

			result.serviceResponse = dispatch(serviceRequest);
		}

		return result;
	}

	public ModelResult sendInvite(List<String> emails, String invitor, String message) {

		ModelResult result = new ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putStringArrayList("emails", (ArrayList<String>) emails);

		if (invitor != null) {
			parameters.putString("name", invitor);
		}

		if (message != null) {
			parameters.putString("message", message);
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.URL_PROXIBASE_SERVICE_USER + "invite")
				.setRequestType(RequestType.METHOD)
				.setParameters(parameters)
				.setSession(Aircandi.getInstance().getUser().session)
				.setResponseFormat(ResponseFormat.JSON);

		result.serviceResponse = dispatch(serviceRequest);

		return result;
	}

	private ServiceResponse storeImageAtS3(Entity entity, User user, Bitmap bitmap) {
		/*
		 * TODO: We are going with a garbage collection scheme for orphaned
		 * images. We need to use an extended property on S3 items that is set to a date when collection is ok. This
		 * allows downloaded entities to keep working even if an image for entity has changed.
		 */

		/* Make sure the bitmap is less than or equal to the maximum size we want to persist. */
		bitmap = UI.ensureBitmapScaleForS3(bitmap);

		/*
		 * Push it to S3. It is always formatted/compressed as a jpeg.
		 */
		final String stringDate = DateTime.nowString(DateTime.DATE_NOW_FORMAT_FILENAME);
		final String imageKey = String.valueOf((user != null) ? user.id : Aircandi.getInstance().getUser().id) + "_" + stringDate + ".jpg";
		ServiceResponse serviceResponse = S3.putImage(imageKey, bitmap, Constants.IMAGE_QUALITY_S3);

		/* Update the photo object for the entity or user */
		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			if (entity != null) {
				entity.photo = new Photo(imageKey, null, bitmap.getWidth(), bitmap.getHeight(), PhotoSource.aircandi);
			}
			else if (user != null) {
				user.photo = new Photo(imageKey, null, bitmap.getWidth(), bitmap.getHeight(), PhotoSource.aircandi);
			}
		}

		return serviceResponse;
	}

	public ModelResult upsizeSynthetic(Place synthetic) {

		/* Decorate and clone */
		final Entity entity = Place.upsizeFromSynthetic(synthetic);

		/* insert in database */
		ModelResult result = EntityManager.getInstance().insertEntity(entity);

		/*
		 * Remove synthetic from the cache and add database entity
		 */
		if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
			Entity upsized = (Entity) result.data;
			mEntityCache.removeEntityTree(synthetic.id);
			mEntityCache.decorate(upsized, null);
			mEntityCache.upsertEntity(upsized);
		}

		return result;
	}

	// --------------------------------------------------------------------------------------------
	// Utilities
	// --------------------------------------------------------------------------------------------

	public static Boolean canUserAdd(Entity entity) {
		if (entity == null) return false;

		/* Schema doesn't support it */
		if (entity.schema.equals(Constants.SCHEMA_ENTITY_USER)
				|| entity.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)
				|| entity.schema.equals(Constants.SCHEMA_ENTITY_BEACON)
				|| entity.schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
			return false;
		}

		/* Current user is owner */
		if (entity.isOwnedByCurrentUser() || entity.isOwnedBySystem()) {
			return true;
		}

		/* Not owner and not NEARBY */
		if (Aircandi.currentPlace == null || (Aircandi.currentPlace != null && !Aircandi.currentPlace.hasActiveProximity())) {
			return false;
		}
		/* Not owner and NEARBY */
		return true;
	}

	public static Boolean canUserEdit(Entity entity) {
		if (entity == null) return false;

		if (entity.isOwnedByCurrentUser() || entity.isOwnedBySystem()) {
			return true;
		}
		return false;
	}

	public static Boolean canUserKick(Candigram entity) {
		if (entity == null) return false;

		/* Current user is owner */
		if (entity.isOwnedByCurrentUser() || entity.isOwnedBySystem()) {
			return true;
		}

		/* Nearby */
		Entity parent = entity.getParent(Constants.TYPE_LINK_CANDIGRAM);
		if (parent != null && parent.hasActiveProximity()) {
			return true;
		}

		/* Not owner and NEARBY */
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Cache queries
	// --------------------------------------------------------------------------------------------

	public List<? extends Entity> getPlaces(Boolean synthetic, Boolean proximity) {
		Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(
				Constants.PREF_SEARCH_RADIUS,
				Constants.PREF_SEARCH_RADIUS_DEFAULT));

		List<Place> places = (List<Place>) EntityManager.getEntityCache().getEntities(
				Constants.SCHEMA_ENTITY_PLACE,
				Constants.TYPE_ANY,
				synthetic,
				searchRangeMeters,
				proximity);

		Collections.sort(places, new Place.SortByProximityAndDistance());
		return places.size() > ServiceConstants.LIMIT_RADAR_PLACES ? places.subList(0, ServiceConstants.LIMIT_RADAR_PLACES) : places;
	}

	// --------------------------------------------------------------------------------------------
	// Other fetch routines
	// --------------------------------------------------------------------------------------------

	public Photo getPhoto(String photoUri) {
		for (Photo photo : mPhotos) {
			if (photo.getUri() != null) {
				if (photo.getUri().equals(photoUri)) {
					return photo;
				}
			}
			else {
				if (photo.getUri().equals(photoUri)) {
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

	public Entity loadEntityFromResources(Integer entityResId, Json.ObjectType objectType) {
		InputStream inputStream = null;
		BufferedReader reader = null;
		try {
			inputStream = Aircandi.applicationContext.getResources().openRawResource(entityResId);
			reader = new BufferedReader(new InputStreamReader(inputStream));
			final StringBuilder text = new StringBuilder(10000);
			String line;
			while ((line = reader.readLine()) != null) {
				text.append(line);
			}
			final String jsonEntity = text.toString();
			final Entity entity = (Entity) Json.jsonToObject(jsonEntity, objectType);
			return entity;
		}
		catch (IOException exception) {
			return null;
		}
		finally {
			try {
				inputStream.close();
				reader.close();
			}
			catch (IOException e) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace();
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Set/GET routines
	// --------------------------------------------------------------------------------------------

	public List<Photo> getPhotos() {
		return mPhotos;
	}

	public List<Category> getCategories() {
		return mCategories;
	}

	public static EntityCache getEntityCache() {
		return mEntityCache;
	}
}