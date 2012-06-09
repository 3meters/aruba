package com.proxibase.aircandi.components;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;

import com.proxibase.aircandi.CandiList.MethodType;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
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

	private MethodType		mMethodType;
	private String			mParentEntityId;
	private Entity			mParentEntity;
	private String			mUserId;
	private Boolean			mMore		= false;
	private List<Entity>	mEntities	= new ArrayList<Entity>();

	public EntityProvider(MethodType methodType, String userId, String parentEntityId) {
		mMethodType = methodType;
		mParentEntityId = parentEntityId;
		mUserId = userId;
	}

	@Override
	public List<Entity> loadEntities() {
		Bundle parameters = new Bundle();
		ServiceRequest serviceRequest = new ServiceRequest();
		if (mMethodType == MethodType.CandiForParent) {

			ArrayList<String> entityIds = new ArrayList<String>();
			entityIds.add(mParentEntityId);
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
		else if (mMethodType == MethodType.CandiByUser) {

			parameters.putString("userId", mUserId);
			parameters.putString("eagerLoad", "object:{\"children\":false,\"parents\":false,\"comments\":false}");
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
		else if (mMethodType == MethodType.CandiByRadar) {
			mEntities = ProxiExplorer.getInstance().getEntityModel().getEntities();
			mMore = false;
			return mEntities;
		}

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.Success) {
			String jsonResponse = (String) serviceResponse.data;
			ServiceData serviceData = ProxibaseService.convertJsonToObjects(jsonResponse, Entity.class, GsonType.ProxibaseService);

			List<Entity> moreEntities = (List<Entity>) serviceData.data;
			mMore = serviceData.more;
			if (mMethodType == MethodType.CandiForParent) {
				mParentEntity = moreEntities.get(0);
				moreEntities = mParentEntity.children;
				mMore = mParentEntity.childrenMore;
			}

			for (Entity entity : moreEntities) {
				mEntities.add(entity);
			}

			return moreEntities;
		}
		return null;
	}

	@Override
	public Boolean isMore() {
		return mMore;
	}

	@Override
	public void reset() {
		mEntities.clear();
		mMore = false;
	}

	@Override
	public Entity getParentEntity() {
		return mParentEntity;
	}

	@Override
	public List<Entity> getEntities() {
		return mEntities;
	}

	@Override
	public void setMore(boolean more) {
		mMore = more;
	}
}
