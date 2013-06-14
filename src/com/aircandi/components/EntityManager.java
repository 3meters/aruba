package com.aircandi.components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.BuildConfig;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProximityManager.ArrayListType;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestType;
import com.aircandi.service.HttpService.ResponseFormat;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.HttpServiceException;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Comment;
import com.aircandi.service.objects.Cursor;
import com.aircandi.service.objects.Device;
import com.aircandi.service.objects.Document;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.DefaultType;
import com.aircandi.service.objects.LinkSettings;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Provider;
import com.aircandi.service.objects.ServiceBase.UpdateScope;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.ServiceEntry;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.DateUtils;
import com.aircandi.utilities.ImageUtils;

public class EntityManager {

	private final EntityCache	mEntityCache	= (EntityCache) Collections.synchronizedMap(new EntityCache());

	/*
	 * The photo collection enables swiping between photos while staying at the same level of the hierarchy.
	 */
	private List<Photo>			mPhotos			= Collections.synchronizedList(new ArrayList<Photo>());
	/*
	 * Categories are cached by a background thread.
	 */
	private List<Category>		mCategories		= Collections.synchronizedList(new ArrayList<Category>());

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
		final ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Combo service/cache queries
	// --------------------------------------------------------------------------------------------

	public Entity getEntity(String entityId) {
		return mEntityCache.get(entityId);
	}

	public synchronized ModelResult getEntity(String entityId, Boolean refresh, LinkOptions linkOptions) {
		/*
		 * Retrieves entity from cache if available otherwise downloads the entity from the service. If refresh is true
		 * then bypasses the cache and downloads from the service.
		 */
		final List<String> entityIds = new ArrayList<String>();
		entityIds.add(entityId);
		final ModelResult result = getEntities(entityIds, refresh, linkOptions);
		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final List<Entity> entities = (List<Entity>) result.data;
			result.data = entities.get(0);
		}
		return result;
	}

	private synchronized ModelResult getEntities(List<String> entityIds, Boolean refresh, LinkOptions linkOptions) {
		final ModelResult result = new ProximityManager.ModelResult();

		final List<String> getEntityIds = new ArrayList<String>();

		for (String entityId : entityIds) {
			Entity entity = mEntityCache.get(entityId);
			if (refresh || entity == null) {
				getEntityIds.add(entityId);
			}
		}

		if (getEntityIds.size() > 0) {
			result.serviceResponse = mEntityCache.loadEntities(getEntityIds, LinkOptions.getDefault(DefaultType.PlaceEntities), null, null);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				final List<Entity> entities = new ArrayList<Entity>();
				for (String entityId : entityIds) {
					Entity entity = mEntityCache.get(entityId);
					entities.add(entity);
				}
				result.data = entities;
			}
		}
		return result;
	}

	public synchronized ModelResult getEntitiesForEntity(String entityId, Boolean refresh, String[] linkTypes, Cursor cursor,
			LinkOptions linkOptions) {
		final ModelResult result = new ProximityManager.ModelResult();

		if (linkOptions != null) {
			String userId = Aircandi.getInstance().getUser().id;
			Number limit = ProxiConstants.RADAR_CHILDENTITY_LIMIT;
			linkOptions = new LinkOptions().setActive(new ArrayList<LinkSettings>());
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_POST, true, false, true, limit));
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_APPLINK, true, false, true, limit));
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_COMMENT, false, false, true));
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_LIKE, false, true, true, limit, "object:{\"_from\":\"" + userId + "\"}"));
			linkOptions.getActive().add(new LinkSettings(Constants.TYPE_LINK_WATCH, false, true, true, limit, "object:{\"_from\":\"" + userId + "\"}"));
		}

		final Bundle parameters = new Bundle();
		parameters.putString("entityId", entityId);
		if (linkOptions != null) {
			parameters.putString("links", "object:" + HttpService.convertObjectToJsonSmart(linkOptions, true, true));
		}

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForEntity")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		if (Aircandi.getInstance().getUser() != null) {
			serviceRequest.setSession(Aircandi.getInstance().getUser().session);
		}

		result.serviceResponse = dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
			final List<Entity> fetchedEntities = (List<Entity>) serviceData.data;

			/* Remove any links to beacons not currently visible */
			Beacon beacon = null;
			for (Entity entity : fetchedEntities) {
				if (entity.linksOut != null) {
					for (int i = entity.linksOut.size() - 1; i >= 0; i--) {
						beacon = (Beacon) mEntityCache.get(entity.linksOut.get(i).toId);
						if (beacon == null) {
							entity.linksOut.remove(i);
						}
					}
				}
			}

			if (fetchedEntities != null && fetchedEntities.size() > 0) {
				mEntityCache.upsertEntities(fetchedEntities);
			}
			result.data = fetchedEntities;
		}

		return result;
	}

	public ModelResult getUser(String userId, Boolean refresh) {
		final ModelResult result = new ProximityManager.ModelResult();

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

			result.serviceResponse = dispatch(serviceRequest);
			if (result.serviceResponse.responseCode == ResponseCode.Success) {
				final String jsonResponse = (String) result.serviceResponse.data;
				User user = (User) HttpService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.User).data;
				mEntityCache.put(userId, user);
			}
		}

		result.serviceResponse.data = mEntityCache.get(userId);
		return result;
	}

	public ModelResult getUserEntities(String userId, Boolean refresh, Integer limit) {

		final List<Entity> entities = new ArrayList<Entity>();
		final ModelResult result = new ProximityManager.ModelResult();

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

			result.serviceResponse = dispatch(serviceRequest);

			if (result.serviceResponse.responseCode == ResponseCode.Success) {

				final String jsonResponse = (String) result.serviceResponse.data;
				final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
				entities.addAll((List<Entity>) serviceData.data);
				mEntityCache.upsertEntities(entities);
				result.data = EntityManager.getInstance().getEntityCache().getEntitiesByOwner(
						userId,
						Constants.SCHEMA_ANY,
						Constants.TYPE_ANY,
						null,
						null,
						null);
			}
		}
		else {
			result.data = EntityManager.getInstance().getEntityCache().getEntitiesByOwner(
					userId,
					Constants.SCHEMA_ANY,
					Constants.TYPE_ANY,
					null,
					null,
					null);
		}
		return result;
	}

	public ModelResult getUserWatching(String userId, String collectionType, Boolean refresh, Integer limit) {
		final ModelResult result = new ProximityManager.ModelResult();
		User watcher = (User) getEntity(userId);
		List<String> schemas = new ArrayList<String>();

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

			result.serviceResponse = dispatch(serviceRequest);

			if (result.serviceResponse.responseCode != ResponseCode.Success) {
				return result;
			}

			final String jsonResponse = (String) result.serviceResponse.data;

			if (collectionType.equals("entities")) {
				final List<Entity> entities = new ArrayList<Entity>();
				final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Entity);
				entities.addAll((List<Entity>) serviceData.data);
				mEntityCache.upsertEntities(entities);
			}
			else if (collectionType.equals("users")) {
				final List<User> users = new ArrayList<User>();
				final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.User);
				users.addAll((List<User>) serviceData.data);
				for (User user : users) {
					mEntityCache.put(user.id, user);
				}
			}
		}

		if (collectionType.equals("entities")) {
			schemas.add(Constants.SCHEMA_ENTITY_PLACE);
			result.data = watcher.getLinkedEntitiesByLinkType(Constants.TYPE_LINK_WATCH, schemas, Direction.out, false);
		}
		else if (collectionType.equals("users")) {
			schemas.add(Constants.SCHEMA_ENTITY_USER);
			result.data = watcher.getLinkedEntitiesByLinkType(Constants.TYPE_LINK_WATCH, schemas, Direction.out, false);
		}
		return result;
	}

	// --------------------------------------------------------------------------------------------
	// Service queries
	// --------------------------------------------------------------------------------------------

	public synchronized ModelResult loadCategories() {

		final ModelResult result = new ProximityManager.ModelResult();

		final Bundle parameters = new Bundle();
		parameters.putString("source", "foursquare");

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_PLACES + "getCategories")
				.setRequestType(RequestType.Method)
				.setParameters(parameters)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Category);
			mCategories = (List<Category>) serviceData.data;
			result.serviceResponse.data = mCategories;
		}
		return result;
	}

	public ModelResult getApplinkSuggestions(List<Entity> applinks, Place entity) {

		final ModelResult result = new ProximityManager.ModelResult();

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

		result.serviceResponse = dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = HttpService.convertJsonToObjectsSmart(jsonResponse, ServiceDataType.Applink);
			result.serviceResponse.data = serviceData.data;
		}
		return result;
	}

	public ModelResult getPlacePhotos(Provider provider, long count, long offset) {
		final ModelResult result = new ProximityManager.ModelResult();

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

		result.serviceResponse = dispatch(serviceRequest);

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
		final ModelResult result = new ProximityManager.ModelResult();

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

		result.serviceResponse = dispatch(serviceRequest);
		return result;

	}

	@SuppressWarnings("ucd")
	public ModelResult signout() {
		final ModelResult result = new ProximityManager.ModelResult();

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

			result.serviceResponse = dispatch(serviceRequest);
		}

		return result;
	}

	public ModelResult updatePassword(String userId, String passwordOld, String passwordNew) {
		final ModelResult result = new ProximityManager.ModelResult();

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

		result.serviceResponse = dispatch(serviceRequest);

		return result;
	}

	public ModelResult insertUser(User user, Bitmap bitmap) {

		/* Pre-fetch an id so a failed request can be retried */
		final ModelResult result = getDocumentId(User.collectionId);

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
			result.serviceResponse = dispatch(serviceRequest);

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
					result.serviceResponse = storeImageAtS3(null, user, bitmap);
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
					result.serviceResponse = dispatch(serviceRequest);

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
		final ModelResult result = new ProximityManager.ModelResult();

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
				result.serviceResponse = storeImageAtS3(null, user, bitmap);
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

				result.serviceResponse = dispatch(serviceRequest);
				mEntityCache.updateEntityUser(user);
			}
		}
		else {
			/*
			 * entity.creator is what we show for entity authors. To make the entity model consistent
			 * with this update to the profile we walk all the entities and update where creator.id
			 * equals the signed in user.
			 */
			mEntityCache.updateEntityUser(user);
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

		result.serviceResponse = dispatch(serviceRequest);
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
			result = getDocumentId(entity.getCollection());

			if (result.serviceResponse.responseCode == ResponseCode.Success) {

				entity.id = (String) result.serviceResponse.data;
				/*
				 * Upload image to S3 as needed
				 */
				if (bitmap != null) {
					result.serviceResponse = storeImageAtS3(entity, null, bitmap);
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
							AirLocation location = LocationManager.getInstance().getAirLocationLocked();
							if (location != null) {

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
					parameters.putString("entity", "object:" + HttpService.convertObjectToJsonSmart(entity, true, true));

					final ServiceRequest serviceRequest = new ServiceRequest()
							.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "insertEntity")
							.setRequestType(RequestType.Method)
							.setParameters(parameters)
							.setSession(Aircandi.getInstance().getUser().session)
							.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
							.setRetry(false)
							.setResponseFormat(ResponseFormat.Json);

					result.serviceResponse = dispatch(serviceRequest);
				}

				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					final String jsonResponse = (String) result.serviceResponse.data;
					final ServiceData serviceData = HttpService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.Entity);

					final Entity insertedEntity = (Entity) serviceData.data;

					/* We want to retain the parent relationship */
					if (entity.toId != null) {
						insertedEntity.toId = entity.toId;
					}

					mEntityCache.upsertEntity(insertedEntity);
					result.data = insertedEntity;
				}
			}
		}
		else {
			mEntityCache.upsertEntity(entity);
		}

		return result;
	}

	public ModelResult updateEntity(Entity entity, Bitmap bitmap) {
		final ModelResult result = new ProximityManager.ModelResult();

		/* Upload new images to S3 as needed. */
		if (bitmap != null) {
			result.serviceResponse = storeImageAtS3(entity, null, bitmap);
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
			parameters.putString("entity", "object:" + HttpService.convertObjectToJsonSmart(entity, true, true));

			final ServiceRequest serviceRequest = new ServiceRequest()
					.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "updateEntity")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setSession(Aircandi.getInstance().getUser().session)
					.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
					.setRetry(false)
					.setResponseFormat(ResponseFormat.Json);

			result.serviceResponse = dispatch(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			entity.activityDate = DateUtils.nowDate().getTime();
			mEntityCache.upsertEntity(entity);
		}

		return result;
	}

	public ModelResult deleteEntity(String entityId, Boolean cacheOnly) {
		final ModelResult result = new ProximityManager.ModelResult();

		if (!cacheOnly) {
			final Entity entity = mEntityCache.get(entityId);
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

			result.serviceResponse = dispatch(serviceRequest);
		}

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			mEntityCache.removeEntityTree(entityId);
		}
		return result;
	}

	public ModelResult trackEntity(Entity entity, List<Beacon> beacons, Beacon primaryBeacon, String actionType, Boolean untuning) {

		final ModelResult result = new ProximityManager.ModelResult();
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
					if (location != null) {

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

		result.serviceResponse = dispatch(serviceRequest);

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
		final ModelResult result = new ProximityManager.ModelResult();

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

			result.serviceResponse = dispatch(serviceRequest);
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
		final ModelResult result = new ProximityManager.ModelResult();

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

		result.serviceResponse = dispatch(serviceRequest);
		/*
		 * We update the cache directly instead of refreshing from the service
		 */
		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			/*
			 * Fail could be because of ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE which is what
			 * prevents any user from liking the same entity more than once.
			 */
			mEntityCache.addLinkTo(toId, verb);
		}

		return result;
	}

	public ModelResult unverbSomething(String fromId, String toId, String verb, String actionType) {
		final ModelResult result = new ProximityManager.ModelResult();

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

		result.serviceResponse = dispatch(serviceRequest);
		/*
		 * We update the cache directly instead of refreshing from the service
		 */
		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			/*
			 * Fail could be because of ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE which is what
			 * prevents any user from liking the same entity more than once.
			 */
			mEntityCache.removeLinkTo(toId, verb);
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

		result.serviceResponse = dispatch(serviceRequest);
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

		result.serviceResponse = dispatch(serviceRequest);
		return result;
	}

	public ModelResult insertDocument(Document document) {

		/* Pre-fetch an id so a failed request can be retried */
		ModelResult result = getDocumentId(Document.collectionId);

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

			result.serviceResponse = dispatch(serviceRequest);
		}

		return result;
	}

	@SuppressWarnings("ucd")
	public ModelResult insertLink(Link link) {

		/* Pre-fetch an id so a failed request can be retried */
		ModelResult result = getDocumentId(Link.collectionId);

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

			result.serviceResponse = dispatch(serviceRequest);

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
		final ModelResult result = new ProximityManager.ModelResult();

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST + Beacon.collectionId)
				.setRequestType(RequestType.Insert)
				.setRequestBody(HttpService.convertObjectToJsonSmart(beacon, true, true))
				.setSocketTimeout(ProxiConstants.TIMEOUT_SOCKET_UPDATES)
				.setRetry(false)
				.setResponseFormat(ResponseFormat.Json);

		result.serviceResponse = dispatch(serviceRequest);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final String jsonResponse = (String) result.serviceResponse.data;
			final ServiceData serviceData = HttpService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.Beacon);
			result.data = serviceData.data;
		}

		return result;
	}

	ServiceResponse storeImageAtS3(Entity entity, User user, Bitmap bitmap) {
		/*
		 * TODO: We are going with a garbage collection scheme for orphaned
		 * images. We need to use an extended property on S3 items that is set to a date when collection is ok. This
		 * allows downloaded entities to keep working even if an image for entity has changed.
		 */

		/* Make sure the bitmap is less than or equal to the maximum size we want to persist. */
		bitmap = ImageUtils.ensureBitmapScaleForS3(bitmap);

		/*
		 * Push it to S3. It is always formatted/compressed as a jpeg.
		 */
		try {
			final String stringDate = DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME);
			final String imageKey = String.valueOf((user != null) ? user.id : Aircandi.getInstance().getUser().id) + "_" + stringDate + ".jpg";
			S3.putImage(imageKey, bitmap, Constants.IMAGE_QUALITY_S3);

			/* Update the photo object for the entity or user */
			if (entity != null) {
				entity.photo = new Photo(imageKey, null, bitmap.getWidth(), bitmap.getHeight(), PhotoSource.aircandi);
			}
			else if (user != null) {
				user.photo = new Photo(imageKey, null, bitmap.getWidth(), bitmap.getHeight(), PhotoSource.aircandi);
			}
		}
		catch (HttpServiceException exception) {
			return new ServiceResponse(ResponseCode.Failed, null, exception);
		}

		return new ServiceResponse();
	}

	public ModelResult upsizeSynthetic(Place synthetic, List<Beacon> beacons, Beacon primaryBeacon) {
		final Entity entity = Place.upsizeFromSynthetic(synthetic);
		ModelResult result = EntityManager.getInstance().insertEntity(entity
				, beacons
				, primaryBeacon
				, entity.photo != null ? entity.photo.getBitmap() : null
				, false
				, null);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			/*
			 * Success so remove the synthetic entity.
			 */
			EntityManager.getInstance().getEntityCache().removeEntityTree(synthetic.id);
			/*
			 * Cached beacons come from the beacon scan process so tuning won't add them
			 * so we need to do it here.
			 */
			if (primaryBeacon != null) {
				if (EntityManager.getInstance().getEntity(primaryBeacon.id) == null) {
					/*
					 * Insert beacon in service. Could fail because it
					 * is already in the beacons collection.
					 */
					result = EntityManager.getInstance().insertBeacon(primaryBeacon);
					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						EntityManager.getInstance().getEntityCache().upsertEntity((Beacon) result.data);
					}
				}
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------------------------
	// Cache queries
	// --------------------------------------------------------------------------------------------

	public ModelResult getEntitiesByListType(ArrayListType arrayListType, Boolean refresh, String collectionId, String userId, Integer limit) {
		/*
		 * Used by candi list
		 */
		ModelResult result = new ProximityManager.ModelResult();
		if (arrayListType == ArrayListType.TunedPlaces) {
			result.data = getPlaces(false, null);
		}
		else if (arrayListType == ArrayListType.SyntheticPlaces) {
			result.data = getPlaces(true, null);
		}
		else if (arrayListType == ArrayListType.OwnedByUser) {
			result = getUserEntities(userId, refresh, limit);
		}
		else if (arrayListType == ArrayListType.InCollection) {
			result.data = EntityManager.getInstance().getEntityCache().getEntitiesByOwner(
					collectionId,
					Constants.SCHEMA_ANY,
					Constants.TYPE_ANY,
					null,
					null,
					null);
		}
		return result;
	}

	public List<? extends Entity> getPlaces(Boolean synthetic, Boolean proximity) {
		Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(
				Constants.PREF_SEARCH_RADIUS,
				Constants.PREF_SEARCH_RADIUS_DEFAULT));

		List<Place> places = (List<Place>) EntityManager.getInstance().getEntityCache().getEntities(
				Constants.SCHEMA_ENTITY_PLACE,
				Constants.TYPE_ANY,
				synthetic,
				searchRangeMeters,
				proximity);

		Collections.sort(places, new Place.SortEntitiesByProximityAndDistance());
		return places.size() > ProxiConstants.RADAR_PLACES_LIMIT ? places.subList(0, ProxiConstants.RADAR_PLACES_LIMIT) : places;
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

	public Entity loadEntityFromResources(Integer entityResId) {
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
			final Entity entity = (Entity) HttpService.convertJsonToObjectInternalSmart(jsonEntity, ServiceDataType.Entity);
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
	// Set/Get routines
	// --------------------------------------------------------------------------------------------

	public List<Photo> getPhotos() {
		return mPhotos;
	}

	public List<Category> getCategories() {
		return mCategories;
	}

	public EntityCache getEntityCache() {
		return mEntityCache;
	}
}