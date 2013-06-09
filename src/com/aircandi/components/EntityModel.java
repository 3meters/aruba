package com.aircandi.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiManager.ArrayListType;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.ProxiManager.WifiScanResult;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestType;
import com.aircandi.service.HttpService.ResponseFormat;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Comment;
import com.aircandi.service.objects.Device;
import com.aircandi.service.objects.Document;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Observation;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Provider;
import com.aircandi.service.objects.ServiceBase;
import com.aircandi.service.objects.ServiceBase.UpdateScope;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.ServiceEntry;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.DateUtils;

public class EntityModel {

	private final ProxiManager			mProxiManager;
	final Map<String, Entity>			mEntityCache		= Collections.synchronizedMap(new HashMap<String, Entity>());
	final Map<String, User>				mUserCache			= Collections.synchronizedMap(new HashMap<String, User>());
	private final List<Beacon>			mBeacons			= Collections.synchronizedList(new ArrayList<Beacon>());
	private List<Photo>					mPhotos				= Collections.synchronizedList(new ArrayList<Photo>());
	private List<Category>				mCategories			= Collections.synchronizedList(new ArrayList<Category>());
	private final Map<String, Applink>	mApplinkMeta			= Collections.synchronizedMap(new HashMap<String, Applink>());
	private List<WifiScanResult>		mWifiList			= Collections.synchronizedList(new ArrayList<WifiScanResult>());

	private Number						mLastActivityDate	= DateUtils.nowDate().getTime();
	private Number						mLastRefreshDate;
	private Number						mLastBeaconLockedDate;

	public EntityModel(ProxiManager proxiManager) {
		mProxiManager = proxiManager;
	}

	// --------------------------------------------------------------------------------------------
	// Combo service/cache queries
	// --------------------------------------------------------------------------------------------

	public synchronized ModelResult getEntity(String entityId, Boolean refresh, String jsonEagerLoad, String jsonOptions) {
		/*
		 * Retrieves entity from cache if available otherwise downloads the entity from the service. If refresh is true
		 * then bypasses the cache and downloads from the service.
		 */
		final List<String> entityIds = new ArrayList<String>();
		entityIds.add(entityId);
		final ModelResult result = getEntities(entityIds, refresh, jsonEagerLoad, jsonOptions);
		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final List<Entity> entities = (List<Entity>) result.data;
			result.data = entities.get(0);
		}
		return result;
	}

	private synchronized ModelResult getEntities(List<String> entityIds, Boolean refresh, String jsonEagerLoad, String jsonOptions) {
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

			if (Aircandi.getInstance().getUser() != null) {
				serviceRequest.setSession(Aircandi.getInstance().getUser().session);
			}

			result.serviceResponse = mProxiManager.dispatch(serviceRequest);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				final String jsonResponse = (String) result.serviceResponse.data;
				final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
				final List<Entity> fetchedEntities = (List<Entity>) serviceData.data;

				/* Remove any links to beacons not currently visible */
				Beacon beacon = null;
				for (Entity entity : fetchedEntities) {
					if (entity.linksOut != null) {
						for (int i = entity.linksOut.size() - 1; i >= 0; i--) {
							beacon = getBeacon(entity.linksOut.get(i).toId);
							if (beacon == null) {
								entity.linksOut.remove(i);
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

			final List<String> userIds = new ArrayList<String>();
			userIds.add(userId);

			final Bundle parameters = new Bundle();
			parameters.putStringArrayList("userIds", (ArrayList<String>) userIds);

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getUsers")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setResponseFormat(ResponseFormat.Json)
					.setSession(Aircandi.getInstance().getUser().session);

			result.serviceResponse = mProxiManager.dispatch(serviceRequest);
			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				final String jsonResponse = (String) result.serviceResponse.data;
				User user = (User) HttpService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.User).data;
				mUserCache.put(userId, user);
			}
		}

		result.serviceResponse.data = mUserCache.get(userId);
		return result;
	}

	public ModelResult getUserEntities(String userId, Boolean refresh, Integer limit) {
		final List<Entity> entities = new ArrayList<Entity>();
		final ModelResult result = new ProxiManager.ModelResult();

		if (refresh) {

			final Bundle parameters = new Bundle();
			parameters.putString("userId", userId);
			parameters.putString("eagerLoad", "object:{\"children\":true,\"parents\":false,\"comments\":false}");
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

			if (Aircandi.getInstance().getUser() != null) {
				serviceRequest.setSession(Aircandi.getInstance().getUser().session);
			}

			result.serviceResponse = mProxiManager.dispatch(serviceRequest);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {

				final String jsonResponse = (String) result.serviceResponse.data;
				final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
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

	public ModelResult getUserWatched(String userId, String collectionType, Boolean refresh, Integer limit) {
		final ModelResult result = new ProxiManager.ModelResult();

		if (refresh) {

			final Bundle parameters = new Bundle();
			parameters.putString("userId", userId);
			parameters.putString("collectionId", ServiceEntry.getIdFromType(collectionType));
			parameters.putString("eagerLoad", "object:{\"children\":false,\"parents\":false,\"comments\":false}");
			parameters.putString("options", "object:{\"limit\":"
					+ String.valueOf(limit)
					+ ",\"skip\":0"
					+ ",\"sort\":{\"modifiedDate\":-1}}");

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getWatchedForUser")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setResponseFormat(ResponseFormat.Json);

			if (Aircandi.getInstance().getUser() != null) {
				serviceRequest.setSession(Aircandi.getInstance().getUser().session);
			}

			result.serviceResponse = mProxiManager.dispatch(serviceRequest);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {

				final String jsonResponse = (String) result.serviceResponse.data;

				if (collectionType.equals("entities")) {
					final List<Entity> entities = new ArrayList<Entity>();
					final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
					entities.addAll((List<Entity>) serviceData.data);
					upsertEntities(entities);
					result.data = getCacheUserWatchedEntities(userId);
				}
				else if (collectionType.equals("users")) {
					final List<User> users = new ArrayList<User>();
					final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.User);
					users.addAll((List<User>) serviceData.data);
					for (User user : users) {
						mUserCache.put(user.id, user);
					}
					result.data = getCacheUserWatchedUsers(userId);
				}
			}
		}
		else {
			if (collectionType.equals("entities")) {
				result.data = getCacheUserWatchedEntities(userId);
			}
			else if (collectionType.equals("users")) {
				result.data = getCacheUserWatchedUsers(userId);
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------------------------
	// Service queries
	// --------------------------------------------------------------------------------------------

	public synchronized ModelResult loadCategories() {

		final ModelResult result = new ProxiManager.ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("source", "foursquare");

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_PLACES + "getCategories")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mProxiManager.dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Category);
			mCategories = (List<Category>) serviceData.data;
			result.serviceResponse.data = mCategories;
		}
		return result;
	}

	public ModelResult getApplinkSuggestions(List<Entity> applinks, Place entity) {

		final ModelResult result = new ProxiManager.ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putInt("timeout", ProxiConstants.SOURCE_SUGGESTIONS_TIMEOUT);
		final List<String> sourceStrings = new ArrayList<String>();
		for (Entity applink : applinks) {
			sourceStrings.add("object:" + HttpService.convertObjectToJsonSmart(applink, true, true));
		}
		parameters.putStringArrayList("sources", (ArrayList<String>) sourceStrings);

		if (entity != null) {
			StringBuilder placeString = new StringBuilder();
			Provider provider = entity.getProvider();
			placeString.append("object:{\"id\":\"" + provider.id + "\",\"provider\":\"" + provider.type + "\"");
			if (entity.name != null) {
				placeString.append(",\"name\":\"" + entity.name + "\"");
			}
			if (entity.phone != null) {
				placeString.append(",\"phone\":\"" + entity.phone + "\"");
			}
			placeString.append("}");
			parameters.putString("place", placeString.toString());
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_SOURCES + "suggest")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mProxiManager.dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Applink);
			result.serviceResponse.data = serviceData.data;
		}
		return result;
	}

	public ModelResult getPlacePhotos(Provider provider, long count, long offset) {
		final ModelResult result = new ProxiManager.ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("provider", provider.type);
		parameters.putString("id", provider.id);
		parameters.putLong("limit", count);
		parameters.putLong("skip", offset);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getPlacePhotos")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mProxiManager.dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Photo);
			final List<Photo> photos = (List<Photo>) serviceData.data;
			result.serviceResponse.data = photos;
		}
		return result;

	}

	public ModelResult getDocumentId(String collection) {

		final ModelResult result = new ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST + collection + "/genId")
				.setRequestType(RequestType.Get)
				.setSuppressUI(true)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceEntry serviceEntry = (ServiceEntry) HttpService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.ServiceEntry).data;
			result.serviceResponse.data = serviceEntry.id;
		}
		return result;
	}

	// --------------------------------------------------------------------------------------------
	// User updates
	// --------------------------------------------------------------------------------------------

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

		result.serviceResponse = mProxiManager.dispatch(serviceRequest);
		return result;

	}

	@SuppressWarnings("ucd")
	public ModelResult signout() {
		final ModelResult result = new ProxiManager.ModelResult();

		final User user = Aircandi.getInstance().getUser();
		if (user != null && user.session != null) {
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

			result.serviceResponse = mProxiManager.dispatch(serviceRequest);
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

		result.serviceResponse = mProxiManager.dispatch(serviceRequest);

		return result;
	}

	public ModelResult insertUser(User user, Bitmap bitmap) {

		/* Pre-fetch an id so a failed request can be retried */
		final ModelResult result = ProxiManager.getInstance().getEntityModel().getDocumentId(User.collectionId);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {

			user.id = (String) result.serviceResponse.data;

			user.updateScope = UpdateScope.Property;
			if (user.photo != null) {
				user.photo.updateScope = UpdateScope.Property;
			}

			ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_USER + "create")
					.setRequestType(RequestType.Insert)
					.setRequestBody(HttpService.convertObjectToJsonSmart(user, true, true))
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setUseSecret(true)
					.setResponseFormat(ResponseFormat.Json);

			/* Insert user. */
			result.serviceResponse = mProxiManager.dispatch(serviceRequest);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {

				String jsonResponse = (String) result.serviceResponse.data;
				ServiceData serviceData = HttpService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.None);
				user = serviceData.user;
				user.session = serviceData.session;
				/*
				 * Put image to S3 if we have one. Handles setting up the photo
				 * object on user
				 */
				if (bitmap != null && !bitmap.isRecycled()) {
					result.serviceResponse = mProxiManager.storeImageAtS3(null, user, bitmap);
				}

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					/*
					 * Update user to capture the uri for the image we saved.
					 */
					serviceRequest = new ServiceRequest()
							.setUri(user.getEntryUri())
							.setRequestType(RequestType.Update)
							.setRequestBody(HttpService.convertObjectToJsonSmart(user, true, true))
							.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
							.setRetry(false)
							.setSession(user.session)
							.setResponseFormat(ResponseFormat.Json);

					/* Doing an update so we don't need anything back */
					result.serviceResponse = mProxiManager.dispatch(serviceRequest);

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						jsonResponse = (String) result.serviceResponse.data;
						serviceData = HttpService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.User);
						final User insertedUser = (User) serviceData.data;
						insertedUser.session = user.session;
						result.data = insertedUser;
					}
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
				result.serviceResponse = mProxiManager.storeImageAtS3(null, user, bitmap);
			}

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				/*
				 * Service handles modifiedId and modifiedDate based
				 * on the session info passed with request.
				 */
				user.updateScope = UpdateScope.Object;
				if (user.photo != null) {
					user.photo.updateScope = UpdateScope.Property;
				}

				final ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(user.getEntryUri())
						.setRequestType(RequestType.Update)
						.setRequestBody(HttpService.convertObjectToJsonSmart(user, true, false))
						.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
						.setRetry(false)
						.setSession(Aircandi.getInstance().getUser().session)
						.setResponseFormat(ResponseFormat.Json);

				result.serviceResponse = mProxiManager.dispatch(serviceRequest);
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

	public ModelResult checkSession() {
		ModelResult result = new ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "checkSession")
				.setRequestType(RequestType.Get)
				.setResponseFormat(ResponseFormat.Json)
				.setSession(Aircandi.getInstance().getUser().session);

		result.serviceResponse = mProxiManager.dispatch(serviceRequest);
		return result;
	}

	// --------------------------------------------------------------------------------------------
	// Entity updates
	// --------------------------------------------------------------------------------------------

	public ModelResult insertEntity(Entity entity, List<Beacon> beacons, Beacon primaryBeacon, Bitmap bitmap, Boolean cacheOnly, Boolean skipNotifications) {
		/*
		 * Inserts the entity in the entity service collection and Links are created to all the included beacons. The
		 * inserted entity is retrieved from the service and pushed into the local cache. The cached entity is returned
		 * in the data property of the result object.
		 * 
		 * @return a ModelResult object. The data property includes the just inserted entity.
		 */
		/*
		 * This is the only place we use the children property
		 * set when deserializing from the service. After this
		 * all references to the children are dynamically assembled
		 * in the getChildren method on entities.
		 */
		ModelResult result = new ModelResult();

		if (!cacheOnly) {
			Logger.i(this, "Inserting entity: " + entity.name);

			/* Pre-fetch an id so a failed request can be retried */
			result = ProxiManager.getInstance().getEntityModel().getDocumentId(entity.getCollection());

			if (result.serviceResponse.responseCode == ResponseCode.Success) {

				entity.id = (String) result.serviceResponse.data;
				/*
				 * Upload image to S3 as needed
				 */
				if (bitmap != null) {
					result.serviceResponse = mProxiManager.storeImageAtS3(entity, null, bitmap);
				}

				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					/* Construct entity, link, and observation */
					final Bundle parameters = new Bundle();

					if (skipNotifications != null) {
						parameters.putBoolean("skipNotifications", skipNotifications);
					}

					/* Primary beacon id */
					if (primaryBeacon != null) {
						parameters.putString("primaryBeaconId", primaryBeacon.id);
					}

					if (beacons != null && beacons.size() > 0) {
						/*
						 * Linking to beacons or sending to support nearby notifications
						 */
						final List<String> beaconStrings = new ArrayList<String>();

						for (Beacon beacon : beacons) {
							Observation observation = LocationManager.getInstance().getObservationLocked();
							if (observation != null) {

								beacon.location.lat = observation.latitude;
								beacon.location.lng = observation.longitude;

								if (observation.altitude != null) {
									beacon.location.altitude = observation.altitude;
								}
								if (observation.accuracy != null) {
									beacon.location.accuracy = observation.accuracy;
								}
								if (observation.bearing != null) {
									beacon.location.bearing = observation.bearing;
								}
								if (observation.speed != null) {
									beacon.location.speed = observation.speed;
								}
							}

							beacon.type = Constants.TYPE_BEACON_FIXED;
							beacon.locked = false;
							beaconStrings.add("object:" + HttpService.convertObjectToJsonSmart(beacon, true, true));
						}
						parameters.putStringArrayList("beacons", (ArrayList<String>) beaconStrings);
					}

					if (entity.toId != null) {
						/* Linking to another entity */
						parameters.putString("parentId", entity.toId);
					}

					/* Observation: only used as data package for the action that gets logged. */
					final Observation observation = LocationManager.getInstance().getObservationLocked();
					if (observation != null) {
						parameters.putString("observation",
								"object:" + HttpService.convertObjectToJsonSmart(observation, true, true));
					}

					if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
						Place place = (Place) entity;

						/* Sources configuration */
						if (!place.getProvider().type.equals("aircandi")) {
							parameters.putBoolean("insertApplinks", true);
							parameters.putInt("applinksTimeout", 10000);
						}

						/* Provider id if this is a custom place */
						if (place.provider.aircandi != null) {
							place.provider.aircandi = entity.id;
						}
					}

					/* Entity */
					entity.updateScope = UpdateScope.Property;
					if (entity.photo != null) {
						entity.photo.updateScope = UpdateScope.Property;
					}
					if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
						Place place = (Place) entity;
						if (place.category != null) {
							place.category.updateScope = UpdateScope.Property;
							if (place.category.photo != null) {
								place.category.photo.updateScope = UpdateScope.Property;
							}
						}
						if (place.provider != null) {
							place.provider.updateScope = UpdateScope.Property;
						}
					}

					parameters.putString("entity", "object:" + HttpService.convertObjectToJsonSmart(entity, true, true));

					final ServiceRequest serviceRequest = new ServiceRequest()
							.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "insertEntity")
							.setRequestType(RequestType.Method)
							.setParameters(parameters)
							.setSession(Aircandi.getInstance().getUser().session)
							.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
							.setRetry(false)
							.setResponseFormat(ResponseFormat.Json);

					result.serviceResponse = mProxiManager.dispatch(serviceRequest);
				}

				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					final String jsonResponse = (String) result.serviceResponse.data;
					final ServiceData serviceData = HttpService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.Entity);

					final Entity insertedEntity = (Entity) serviceData.data;

					/* We want to retain the parent relationship */
					if (entity.toId != null) {
						insertedEntity.toId = entity.toId;
					}

					upsertEntity(insertedEntity);
					result.data = insertedEntity;
				}
			}
		}
		else {
			upsertEntity(entity);
		}

		return result;
	}

	public ModelResult updateEntity(Entity entity, Bitmap bitmap) {
		final ModelResult result = new ProxiManager.ModelResult();

		/* Upload new images to S3 as needed. */
		if (bitmap != null) {
			result.serviceResponse = mProxiManager.storeImageAtS3(entity, null, bitmap);
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

			/* Entity */
			entity.updateScope = UpdateScope.Object;
			if (entity.photo != null) {
				entity.photo.updateScope = UpdateScope.Property;
			}
			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				Place place = (Place) entity;
				place.updateScope = UpdateScope.Property;
				if (place.category != null) {
					place.category.updateScope = UpdateScope.Property;
					if (place.category.photo != null) {
						place.category.photo.updateScope = UpdateScope.Property;
					}
				}
				if (place.provider != null) {
					place.provider.updateScope = UpdateScope.Property;
				}
			}

			parameters.putString("entity", "object:" + HttpService.convertObjectToJsonSmart(entity, true, true));

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "updateEntity")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setSession(Aircandi.getInstance().getUser().session)
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = mProxiManager.dispatch(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			entity.activityDate = DateUtils.nowDate().getTime();
			upsertEntity(entity);
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

			result.serviceResponse = mProxiManager.dispatch(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			removeEntity(entityId);
		}
		return result;
	}

	public ModelResult trackEntity(Entity entity, List<Beacon> beacons, Beacon primaryBeacon, String actionType, Boolean untuning) {

		final ModelResult result = new ProxiManager.ModelResult();
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
					Observation observation = LocationManager.getInstance().getObservationLocked();
					if (observation != null) {

						beacon.location.lat = observation.latitude;
						beacon.location.lng = observation.longitude;

						if (observation.altitude != null) {
							beacon.location.altitude = observation.altitude;
						}
						if (observation.accuracy != null) {
							beacon.location.accuracy = observation.accuracy;
						}
						if (observation.bearing != null) {
							beacon.location.bearing = observation.bearing;
						}
						if (observation.speed != null) {
							beacon.location.speed = observation.speed;
						}
					}
				}

				beacon.type = Constants.TYPE_BEACON_FIXED;
				beacon.locked = false;
				beaconStrings.add("object:" + HttpService.convertObjectToJsonSmart(beacon, true, true));
				beaconIds.add(beacon.id);
			}

			if (untuning) {
				parameters.putStringArrayList("beaconIds", (ArrayList<String>) beaconIds);
			}
			else {
				parameters.putStringArrayList("beacons", (ArrayList<String>) beaconStrings);
			}
		}

		/* Observation */
		final Observation observation = LocationManager.getInstance().getObservationLocked();
		if (observation != null) {
			parameters.putString("observation", "object:" + HttpService.convertObjectToJsonSmart(observation, true, true));
		}

		/* Entity */
		parameters.putString("entityId", entity.id);

		/* Action type - proximity or proximity_first */
		if (!untuning) {
			parameters.putString("actionType", actionType);
		}

		/* Method */
		String methodName = untuning ? "untrackEntity" : "trackEntity";

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + methodName)
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setSession(Aircandi.getInstance().getUser().session)
				.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
				.setRetry(false)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mProxiManager.dispatch(serviceRequest);

		/* Reproduce the service call effect locally */
		if (result.serviceResponse.responseCode == ResponseCode.Success) {

			//			entity.activityDate = DateUtils.nowDate().getTime(); // So place rank score gets updated
			//			setLastActivityDate(DateUtils.nowDate().getTime());	 // So collections get resorted

			if (beacons != null) {
				for (Beacon beacon : beacons) {
					Boolean primary = (primaryBeacon != null && primaryBeacon.id.equals(beacon.id));
					Link link = entity.getOutLinkByType(Constants.TYPE_LINK_PROXIMITY, beacon.id);
					if (link != null) {
						if (primary) {
							if (link.tuneCount != null) {
								link.tuneCount = link.tuneCount.intValue() + 1;
							}
							else {
								link.tuneCount = 1;
							}
							if (!link.proximity.primary) {
								link.proximity.primary = true;
							}
						}
					}
					else {
						link = new Link(beacon.id, entity.id);
						link.type = "proximity";
						link.proximity.signal = beacon.signal;
						if (primary) {
							if (link.tuneCount != null) {
								link.tuneCount = link.tuneCount.intValue() + 1;
							}
							else {
								link.tuneCount = 1;
							}
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

	public ModelResult insertComment(String entityId, Comment comment, Boolean cacheOnly) {
		final ModelResult result = new ProxiManager.ModelResult();

		if (!cacheOnly) {
			final Bundle parameters = new Bundle();
			parameters.putString("entityId", entityId);
			parameters.putString("comment", "object:" + HttpService.convertObjectToJsonSmart(comment, true, true));

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "insertComment")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setSession(Aircandi.getInstance().getUser().session)
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = mProxiManager.dispatch(serviceRequest);
		}

		/*
		 * We want to update the entity wherever it might be in the entity model while
		 * keeping the same instance.
		 */
		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			//insertCommentCache(entityId, comment);
		}

		return result;
	}

	public ModelResult verbSomething(String fromId, String toId, String verb, String actionType) {
		final ModelResult result = new ProxiManager.ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("fromId", fromId);
		parameters.putString("toId", toId);
		parameters.putString("verb", verb);
		parameters.putString("actionType", actionType);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "insertVerbLink")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setSession(Aircandi.getInstance().getUser().session)
				.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
				.setRetry(false)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mProxiManager.dispatch(serviceRequest);
		/*
		 * We update the cache directly instead of refreshing from the service
		 */
		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			/*
			 * Fail could be because of ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE which is what
			 * prevents any user from liking the same entity more than once.
			 */
			verbSomethingCache(toId, verb);
		}

		return result;
	}

	public ModelResult unverbSomething(String fromId, String toId, String verb, String actionType) {
		final ModelResult result = new ProxiManager.ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("fromId", fromId);
		parameters.putString("toId", toId);
		parameters.putString("verb", verb);
		parameters.putString("actionType", actionType);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "deleteVerbLink")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setSession(Aircandi.getInstance().getUser().session)
				.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
				.setRetry(false)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mProxiManager.dispatch(serviceRequest);
		/*
		 * We update the cache directly instead of refreshing from the service
		 */
		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			/*
			 * Fail could be because of ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE which is what
			 * prevents any user from liking the same entity more than once.
			 */
			unverbSomethingCache(toId, verb);
		}

		return result;
	}

	// --------------------------------------------------------------------------------------------
	// Other updates
	// --------------------------------------------------------------------------------------------

	public void processNotification(AirNotification notification) {}

	public ModelResult registerDevice(Device device) {
		ModelResult result = new ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST + Device.collectionId)
				.setRequestType(RequestType.Insert)
				.setRequestBody(HttpService.convertObjectToJsonSmart(device, true, true))
				.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
				.setRetry(false)
				.setSession(Aircandi.getInstance().getUser().session)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mProxiManager.dispatch(serviceRequest);
		return result;
	}

	public ModelResult unregisterDevice(String registrationId) {
		ModelResult result = new ModelResult();
		final Bundle parameters = new Bundle();
		parameters.putString("registrationId", registrationId);

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "unregisterDevice")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
				.setRetry(false)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mProxiManager.dispatch(serviceRequest);
		return result;
	}

	public ModelResult insertDocument(Document document) {

		/* Pre-fetch an id so a failed request can be retried */
		ModelResult result = ProxiManager.getInstance().getEntityModel().getDocumentId(Document.collectionId);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			document.id = (String) result.serviceResponse.data;
			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST + document.getCollection())
					.setRequestType(RequestType.Insert)
					.setRequestBody(HttpService.convertObjectToJsonSmart(document, true, true))
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setSession(Aircandi.getInstance().getUser().session)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = mProxiManager.dispatch(serviceRequest);
		}

		return result;
	}

	@SuppressWarnings("ucd")
	public ModelResult insertLink(Link link) {

		/* Pre-fetch an id so a failed request can be retried */
		ModelResult result = ProxiManager.getInstance().getEntityModel().getDocumentId(Link.collectionId);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			link.id = (String) result.serviceResponse.data;
			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST + link.getCollection())
					.setRequestType(RequestType.Insert)
					.setRequestBody(HttpService.convertObjectToJsonSmart(link, true, true))
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setSession(Aircandi.getInstance().getUser().session)
					.setRetry(false)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = mProxiManager.dispatch(serviceRequest);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				final String jsonResponse = (String) result.serviceResponse.data;
				final ServiceData serviceData = HttpService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.Link);
				result.data = serviceData.data;
			}
		}

		return result;
	}

	ModelResult insertBeacon(Beacon beacon) {
		/*
		 * We don't pre-fetch an id for beacons because the same beacon will always
		 * be assigned the same id by the service.
		 */
		final ModelResult result = new ProxiManager.ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST + Beacon.collectionId)
				.setRequestType(RequestType.Insert)
				.setRequestBody(HttpService.convertObjectToJsonSmart(beacon, true, true))
				.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
				.setRetry(false)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = mProxiManager.dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = HttpService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.Beacon);
			result.data = serviceData.data;
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

		result.serviceResponse = mProxiManager.dispatch(serviceRequest);
		return result;
	}

	// --------------------------------------------------------------------------------------------
	// Beacon cache
	// --------------------------------------------------------------------------------------------

	public void clearBeacons() {
		synchronized (mBeacons) {
			mBeacons.clear();
		}
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

	public void updateBeacons() {
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
						, scanResult.test);

				upsertBeacon(beacon);
			}
		}

		/* Sort beacons by signal strength */
		synchronized (mBeacons) {
			Collections.sort(mBeacons, new Beacon.SortBeaconsBySignalLevel());
		}

		mLastBeaconLockedDate = DateUtils.nowDate().getTime();

		BusProvider.getInstance().post(new BeaconsLockedEvent());
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
			result.data = getChildEntities(collectionId, Constants.TYPE_LINK_ANY);
		}
		return result;
	}

	/**
	 * Returns top level place entities of all types that should be visible in radar. Entities that are hidden
	 * have already been screened out. Entities are pre-sorted by place rank score and distance.
	 * 
	 * @return
	 */

	public List<? extends Entity> getAllPlaces(Boolean includeHidden) {
		/*
		 * This is the one case where refresh scenarios have been
		 * handled outside of this method.
		 */
		final List<Entity> entities = new ArrayList<Entity>();
		final Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(Constants.PREF_SEARCH_RADIUS,
				Constants.PREF_SEARCH_RADIUS_DEFAULT));

		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					Place entity = (Place) entry.getValue();
					if (includeHidden || !entity.hidden) {

						/* Must do this to cache the distance before sorting */
						Float distance = entity.getDistance();

						Beacon beacon = entity.getActiveBeacon(Constants.TYPE_LINK_PROXIMITY);
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

		Collections.sort(entities, new Place.SortEntitiesByProximityAndDistance());
		return entities.size() > ProxiConstants.RADAR_PLACES_LIMIT ? entities.subList(0, ProxiConstants.RADAR_PLACES_LIMIT) : entities;
	}

	public Float getMaxPlaceDistance() {
		final List<Entity> places = (List<Entity>) getAllPlaces(true); // refreshes distance calculation
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

	public List<? extends Entity> getAircandiPlaces() {
		/*
		 * This is the one case where refresh scenarios have been
		 * handled outside of this method.
		 */
		final List<Entity> entities = new ArrayList<Entity>();
		final Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(Constants.PREF_SEARCH_RADIUS,
				Constants.PREF_SEARCH_RADIUS_DEFAULT));

		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					Place entity = (Place) entry.getValue();
					if (!entity.synthetic) {
						Beacon beacon = entity.getActiveBeaconPrimaryOnly(Constants.TYPE_LINK_PROXIMITY);
						/* Must do this to cache the distance before sorting */
						Float distance = entity.getDistance();
						if (beacon != null) {
							entities.add(entity);
						}
						else {
							beacon = entity.getActiveBeaconPrimaryOnly(Constants.TYPE_LINK_PROXIMITY);
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
		Collections.sort(entities, new Place.SortEntitiesByProximityAndDistance());
		return entities;
	}

	public List<? extends Entity> getProximityPlaces() {
		/*
		 * This is the one case where refresh scenarios have been
		 * handled outside of this method.
		 */
		final List<Place> entities = new ArrayList<Place>();

		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					Place entity = (Place) entry.getValue();
					Beacon beacon = entity.getActiveBeaconPrimaryOnly(Constants.TYPE_LINK_PROXIMITY);
					if (beacon != null) {
						entities.add(entity);
					}
				}
			}
		}
		return entities;
	}

	public List<? extends Entity> getCollectionEntities() {
		/*
		 * This is the one case where refresh scenarios have been
		 * handled outside of this method.
		 */
		final List<Entity> entities = new ArrayList<Entity>();
		final Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(Constants.PREF_SEARCH_RADIUS,
				Constants.PREF_SEARCH_RADIUS_DEFAULT));
		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					Place entity = (Place) entry.getValue();
					if (!entity.hidden && !entity.synthetic) {
						Beacon beacon = entity.getActiveBeaconPrimaryOnly(Constants.TYPE_LINK_PROXIMITY);
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
		Collections.sort(entities, new Place.SortEntitiesByProximityAndDistance());
		return entities;
	}

	public List<? extends Entity> getRadarSynthetics() {
		/*
		 * This is the one case where refresh scenarios have been
		 * handled outside of this method.
		 */
		final List<Entity> entities = new ArrayList<Entity>();
		final Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(Constants.PREF_SEARCH_RADIUS,
				Constants.PREF_SEARCH_RADIUS_DEFAULT));
		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					Place entity = (Place) entry.getValue();
					if (!entity.hidden && entity.synthetic) {
						Float distance = entity.getDistance();
						Beacon beacon = entity.getActiveBeaconPrimaryOnly(Constants.TYPE_LINK_PROXIMITY);
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
		Collections.sort(entities, new Place.SortEntitiesByProximityAndDistance());
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
					if (type.equals(Constants.SCHEMA_ENTITY_POST)) {
						entities.add(entry.getValue());
					}
					else if (entry.getValue().type.equals(type)) {
						entities.add(entry.getValue());
					}
				}
			}
		}
		Collections.sort(entities, new Entity.SortEntitiesByModifiedDate());
		return entities;
	}

	public List<Entity> getCacheUserWatchedEntities(String userId) {
		final List<Entity> entities = new ArrayList<Entity>();
		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().watcherId != null && entry.getValue().watcherId.equals(userId)) {
					entities.add(entry.getValue());
				}
			}
		}
		Collections.sort(entities, new Entity.SortEntitiesByWatchedDate());
		return entities;
	}

	public List<User> getCacheUserWatchedUsers(String userId) {
		final List<User> users = new ArrayList<User>();
		synchronized (mUserCache) {
			for (Entry<String, User> entry : mUserCache.entrySet()) {
				if (entry.getValue().watcherId != null && entry.getValue().watcherId.equals(userId)) {
					users.add(entry.getValue());
				}
			}
		}
		Collections.sort(users, new Entity.SortEntitiesByWatchedDate());
		return users;
	}

	public Entity getCacheEntity(String entityId) {
		synchronized (mEntityCache) {
			return mEntityCache.get(entityId);
		}
	}

	@SuppressWarnings("ucd")
	public User getCacheUser(String userId) {
		synchronized (mUserCache) {
			return mUserCache.get(userId);
		}
	}

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

	public List<Entity> getChildEntities(String entityId, String linkType) {
		final List<Entity> entities = new ArrayList<Entity>();
		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().toId != null && entry.getValue().toId.equals(entityId)) {
					if (linkType.equals(Constants.TYPE_LINK_ANY) || entry.getValue().type.equals(linkType)) {
						entities.add(entry.getValue());
					}
				}
			}
		}
		Collections.sort(entities, new Entity.SortEntitiesByModifiedDate());
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

	private void verbSomethingCache(String toId, String verb) {
		Entity entity = null;

		if (ServiceEntry.getTypeFromId(toId) == "entities") {
			entity = getCacheEntity(toId);
		}
		else if (ServiceEntry.getTypeFromId(toId) == "users") {
			entity = getCacheUser(toId);
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
				entity.watchedDate = time;
			}
			entity.activityDate = time;
			setLastActivityDate(time);
		}
	}

	private void unverbSomethingCache(String toId, String verb) {

		ServiceBase entry = null;

		if (ServiceEntry.getTypeFromId(toId) == "entities") {
			entry = getCacheEntity(toId);
		}
		else if (ServiceEntry.getTypeFromId(toId) == "users") {
			entry = getCacheUser(toId);
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
				entry.watcherId = null;
				entry.watchedDate = null;
			}
			Long time = DateUtils.nowDate().getTime();
			entry.activityDate = time;
			setLastActivityDate(time);
		}
	}

	void upsertBeacon(Beacon beacon) {
		synchronized (mBeacons) {

			final Beacon beaconOriginal = getBeacon(beacon.id);
			if (beaconOriginal != null) {
				Beacon.copyFields(beacon, beaconOriginal);
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

	private Entity upsertEntity(Entity freshEntity) {
		/*
		 * This is the only place we use the children property
		 * set when deserializing from the service. After this
		 * all references to the children are dynamically assembled
		 * in the getChildren method on entities.
		 */
		synchronized (mEntityCache) {

			final Entity staleEntity = mEntityCache.get(freshEntity.id);

			/* Check to see if we have this entity keyed to the sourceId */
			if (staleEntity != null) {

				Entity.copyFields(freshEntity, staleEntity);
				/*
				 * We only do children work if the new entity has them.
				 */
				if (freshEntity.entities != null) {

					synchronized (freshEntity.entities) {

						/* Removes all children except source entities */
						final Map<String, Entity> staleChildren = removeChildren(staleEntity.id);

						Iterator iter = freshEntity.entities.iterator();
						while (iter.hasNext()) {
							Entity freshChild = (Entity) iter.next();
							Entity staleChild = staleChildren.get(freshChild.id);

							if (staleChild != null) {
								/* Copy property over stale child and put back into cache */
								Entity.copyFields(freshChild, staleChild);
								mEntityCache.put(freshChild.id, staleChild);
							}
							else {
								mEntityCache.put(freshChild.id, freshChild);
							}

							removeSourceEntities(freshChild.id);
							addApplink(freshChild, 0, "comment", "comments", "resource:img_post", freshEntity.getInCount(Constants.TYPE_LINK_COMMENT));
							addApplink(freshChild, 1, "like", "likes", "resource:img_like", freshEntity.getInCount(Constants.TYPE_LINK_LIKE));
							addApplink(freshChild, 2, "watch", "watching", "resource:img_watch", freshEntity.getInCount(Constants.TYPE_LINK_WATCH));
						}
						freshEntity.entities.clear();
						freshEntity.entities = null;
					}
				}
			}
			else {
				mEntityCache.put(freshEntity.id, freshEntity);

				if (freshEntity.entities != null) {

					synchronized (freshEntity.entities) {
						Iterator iter = freshEntity.entities.iterator();
						while (iter.hasNext()) {
							Entity freshChild = (Entity) iter.next();
							mEntityCache.put(freshChild.id, freshChild);

							removeSourceEntities(freshChild.id);
							
							addApplink(freshChild, 0, "map", "map", "resource:img_post", null);
							addApplink(freshChild, 1, "comment", "comments", "resource:img_post", freshEntity.getInCount(Constants.TYPE_LINK_COMMENT));
							addApplink(freshChild, 2, "like", "likes", "resource:img_like", freshEntity.getInCount(Constants.TYPE_LINK_LIKE));
							addApplink(freshChild, 3, "watch", "watching", "resource:img_watch", freshEntity.getInCount(Constants.TYPE_LINK_WATCH));
						}
						freshEntity.entities.clear();
						freshEntity.entities = null;
					}
				}
			}
		}

		/* Create virtual candi for sources */

		/* First remove any old stuff */
		removeSourceEntities(freshEntity.id);

		Integer position = 0;
		if (freshEntity.type.equals(Constants.SCHEMA_ENTITY_PLACE)) {

			/* Add the current stuff */
			
			/*
			 * Look for place.entities and push into cache.
			 * Set id property: sourceEntity.id = freshEntity.id + "." + source.type + "." + String.valueOf(DateUtils.nowDate().getTime());
			 * Set position
			 */
			if (freshEntity.entities != null) {
				for (@SuppressWarnings("unused") Entity entity : freshEntity.entities) {
				}
			}
		}

		/* Add synthetic sources */

		if (freshEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			addApplink(freshEntity, position, "map", "map", "resource:img_post", null);
			addApplink(freshEntity, position++, "comment", "comments", "resource:img_post", freshEntity.getInCount(Constants.TYPE_LINK_COMMENT));
			addApplink(freshEntity, position++, "like", "likes", "resource:img_like", freshEntity.getInCount(Constants.TYPE_LINK_LIKE));
			addApplink(freshEntity, position++, "watch", "watching", "resource:img_watch", freshEntity.getInCount(Constants.TYPE_LINK_WATCH));
		}

		setLastActivityDate(DateUtils.nowDate().getTime());

		return mEntityCache.get(freshEntity.id);
	}

	private void addApplink(Entity entity, Integer position, String type, String name, String image, Integer count) {

		final Applink applink = (Applink) mProxiManager.loadEntityFromResources(R.raw.applink_entity);
		applink.id = entity.id + "." + type;
		applink.name = name;
		applink.photo = new Photo(image, null, null, null, PhotoSource.resource);
		applink.type = type;
		applink.position = (position != null) ? position : 0;
		applink.intentSupport = false;
		applink.installDeclined = false;
		applink.count = count;

		applink.toId = entity.id;
		upsertEntity(applink);
	}

	private void updateUser(User user) {
		synchronized (mEntityCache) {
			for (Entry<String, Entity> entry : mEntityCache.entrySet()) {
				if (entry.getValue().creatorId != null && entry.getValue().creatorId.equals(user.id)) {
					if (entry.getValue().creator != null) {
						if (user.photo != null) {
							entry.getValue().creator.photo = user.photo.clone();
						}
						if (user.location != null) {
							entry.getValue().creator.location = user.location;
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
						if (user.location != null) {
							entry.getValue().owner.location = user.location;
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
						if (user.location != null) {
							entry.getValue().modifier.location = user.location;
						}
						if (user.name != null) {
							entry.getValue().modifier.name = user.name;
						}
					}
				}
			}
		}
		setLastActivityDate(DateUtils.nowDate().getTime());
	}

	@SuppressWarnings("ucd")
	public void removeEntitiesForUser(String userId) {
		/*
		 * We clean out user entities and their children when the entity
		 * is associated with a beacon that isn't a radar hit.
		 */
		synchronized (mEntityCache) {
			final Iterator iter = mEntityCache.keySet().iterator();
			Entity entity = null;
			while (iter.hasNext()) {
				entity = mEntityCache.get(iter.next());
				if (entity.toId == null && entity.ownerId.equals(userId)) {
					iter.remove();
				}
			}
		}
		setLastActivityDate(DateUtils.nowDate().getTime());
	}

	void removeSyntheticEntities() {
		synchronized (mEntityCache) {
			final Iterator iter = mEntityCache.keySet().iterator();
			Entity entity = null;
			while (iter.hasNext()) {
				entity = mEntityCache.get(iter.next());
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)
						&& ((Place)entity).synthetic) {
					iter.remove();
				}
			}
		}
		setLastActivityDate(DateUtils.nowDate().getTime());
	}

	void removeBeaconEntities() {
		synchronized (mEntityCache) {
			final Iterator iter = mEntityCache.keySet().iterator();
			Entity entity = null;
			while (iter.hasNext()) {
				entity = mEntityCache.get(iter.next());
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)
						&& !((Place)entity).synthetic
						&& (entity.linksOut != null && entity.linksOut.size() > 0)) {
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
			final Iterator iter = mEntityCache.keySet().iterator();
			Entity entity = null;
			while (iter.hasNext()) {
				entity = mEntityCache.get(iter.next());
				if (!entity.type.equals(Constants.SCHEMA_ENTITY_APPLINK)
						&& entity.toId != null
						&& entity.toId.equals(entityId)) {
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
			final Iterator iter = mEntityCache.keySet().iterator();
			Entity entity = null;
			while (iter.hasNext()) {
				entity = mEntityCache.get(iter.next());
				if (entity.type.equals(Constants.SCHEMA_ENTITY_APPLINK)
						&& entity.toId != null
						&& entity.toId.equals(entityId)) {
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

	public Map<String, Applink> getApplinkMeta() {
		return mApplinkMeta;
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