package com.proxibase.aircandi.components;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;

import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.ProxiExplorer.CollectionType;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.ServiceData;

public class EntityProvider implements IEntityProvider {

	private CollectionType		mCollectionType;
	private String				mCollectionId;
	private Entity				mCollectionEntity;
	private String				mUserId;
	private Boolean				mUseProxiExplorer;
	private Boolean				mFirstLoad	= true;
	private EntityList<Entity>	mEntities	= new EntityList<Entity>();

	public EntityProvider(CollectionType collectionType, String collectionId, String userId, Boolean useProxiExplorer) {
		/*
		 * Currently, this class handles can surface all entity collection types and can handle chunking
		 * for children and mycandi. Chunking of top level entities is handled by proxi explorer
		 */
		mCollectionType = collectionType;
		mCollectionId = collectionId;
		mUserId = userId;
		mUseProxiExplorer = useProxiExplorer;
		mEntities.setCollectionType(mCollectionType);

		if (mUseProxiExplorer) {
			mEntities = ProxiExplorer.getInstance().getEntityModel().getCollectionById(mCollectionId, mCollectionType);
			mCollectionEntity = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCollectionId, mCollectionType);
		}
	}

	@Override
	public EntityList<Entity> loadEntities() {
		/*
		 * Loaded entities are appended to the collection the provider is bound to and returned
		 * to the caller.
		 */
		Bundle parameters = new Bundle();
		ServiceRequest serviceRequest = new ServiceRequest();

		if (mCollectionType == CollectionType.CandiByUser) {

			if (mFirstLoad && mUseProxiExplorer && (mEntities != null && mEntities.size() > 0)) {
				mFirstLoad = false;
				return mEntities;
			}

			if (mCollectionEntity.superRoot) {
				/*
				 * Handles chunking mycandi top level entities.
				 */
				parameters.putString("userId", mUserId);
				parameters.putString("eagerLoad", "object:{\"children\":true,\"parents\":false,\"comments\":false}");
				parameters.putString("options", "object:{\"limit\":"
						+ String.valueOf(CandiConstants.RADAR_ENTITY_LIMIT)
						+ ",\"skip\":" + String.valueOf(mEntities.size())
						+ ",\"sort\":{\"modifiedDate\":-1} "
						+ ",\"children\":{\"limit\":"
						+ String.valueOf(CandiConstants.RADAR_CHILDENTITY_LIMIT)
						+ ",\"skip\":0"
						+ ",\"sort\":{\"modifiedDate\":-1}}"
						+ "}");

				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntitiesForUser").setRequestType(RequestType.Method)
						.setParameters(parameters).setResponseFormat(ResponseFormat.Json);
			}
			else {
				/*
				 * Handles chunking children
				 */
				ArrayList<String> entityIds = new ArrayList<String>();
				entityIds.add(mCollectionId);
				parameters.putStringArrayList("entityIds", entityIds);
				parameters.putString("eagerLoad", "object:{\"children\":true,\"parents\":false,\"comments\":false}");
				parameters.putString("options", "object:{\"limit\":"
						+ String.valueOf(CandiConstants.RADAR_ENTITY_LIMIT)
						+ ",\"skip\":0"
						+ ",\"sort\":{\"modifiedDate\":-1} "
						+ ",\"children\":{\"limit\":"
						+ String.valueOf(CandiConstants.RADAR_CHILDENTITY_LIMIT)
						+ ",\"skip\":" + String.valueOf(mEntities.size())
						+ ",\"sort\":{\"modifiedDate\":-1}}"
						+ "}");

				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntities").setRequestType(RequestType.Method)
						.setParameters(parameters).setResponseFormat(ResponseFormat.Json);
			}
		}
		else if (mCollectionType == ProxiExplorer.CollectionType.CandiByRadar) {

			if (mCollectionEntity.superRoot) {
				return mEntities;
			}

			if (mFirstLoad && mUseProxiExplorer && mEntities.size() > 0) {
				mFirstLoad = false;
				return mEntities;
			}
			/*
			 * Handles chunking children
			 */
			ArrayList<String> entityIds = new ArrayList<String>();
			entityIds.add(mCollectionId);
			parameters.putStringArrayList("entityIds", entityIds);
			parameters.putString("eagerLoad", "object:{\"children\":true,\"parents\":false,\"comments\":false}");
			parameters.putString("options", "object:{\"limit\":"
					+ String.valueOf(CandiConstants.RADAR_ENTITY_LIMIT)
					+ ",\"skip\":0"
					+ ",\"sort\":{\"modifiedDate\":-1} "
					+ ",\"children\":{\"limit\":"
					+ String.valueOf(CandiConstants.RADAR_CHILDENTITY_LIMIT)
					+ ",\"skip\":" + String.valueOf(mEntities.size())
					+ ",\"sort\":{\"modifiedDate\":-1}}"
					+ "}");

			serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "getEntities").setRequestType(RequestType.Method)
					.setParameters(parameters).setResponseFormat(ResponseFormat.Json);
		}

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.Success) {
			String jsonResponse = (String) serviceResponse.data;
			ServiceData serviceData = ProxibaseService.convertJsonToObjects(jsonResponse, Entity.class, GsonType.ProxibaseService);

			List<Entity> moreEntitiesList = (List<Entity>) serviceData.data;
			EntityList<Entity> moreEntities = new EntityList<Entity>();
			moreEntities.setCollectionType(mCollectionType);

			if (mCollectionEntity.superRoot) {
				moreEntities.addAll(moreEntitiesList);
			}
			else {
				moreEntities.addAll(moreEntitiesList.get(0).children);
			}

			/* Append the new batch of entities */
			synchronized (mEntities) {
				mEntities.addAll(moreEntities);
			}

			mFirstLoad = false;
			return moreEntities;
		}
		return null;
	}

	@Override
	public Entity getCollectionEntity() {
		return mCollectionEntity;
	}

	@Override
	public EntityList<Entity> getEntities() {
		return mEntities;
	}
}
