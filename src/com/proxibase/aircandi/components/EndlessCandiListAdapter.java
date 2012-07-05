package com.proxibase.aircandi.components;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.service.objects.Entity;

public class EndlessCandiListAdapter extends EndlessAdapter {

	private List<Entity>		mMoreEntities	= new ArrayList<Entity>();
	private EntityList<Entity>	mProxiEntities;
	private LayoutInflater		mInflater;

	public EndlessCandiListAdapter(Context context, EntityList<Entity> proxiEntities, int itemLayoutId) {
		/*
		 * I don't like but we need to clone the entity list because the adapter really wants it's own copy.
		 */
		super(new CandiListAdapter(context, proxiEntities.clone(), itemLayoutId));
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mProxiEntities = proxiEntities;
	}

	@Override
	protected View getPendingView(ViewGroup parent) {
		View view = mInflater.inflate(mProxiEntities.isMore() ? R.layout.temp_candi_list_item_placeholder : R.layout.temp_candi_list_item_placeholder_empty, null);
		return view;
	}

	@Override
	protected boolean cacheInBackground() {
		mMoreEntities.clear();
		if (mProxiEntities.isMore()) {
			ServiceResponse serviceResponse = ProxiExplorer.getInstance().getEntityModel().chunkEntities(mProxiEntities);
			if (serviceResponse.responseCode == ResponseCode.Success) {
				mMoreEntities = (List<Entity>) serviceResponse.data;
				if (mProxiEntities.isMore()) {
					return ((getWrappedAdapter().getCount() + mMoreEntities.size()) < CandiConstants.RADAR_ENTITY_MAX_LIMIT);
				}
			}
		}
		return false;
	}

	@Override
	protected void appendCachedData() {
		ArrayAdapter<Entity> adapterEntities = (ArrayAdapter<Entity>) getWrappedAdapter();
		for (Entity entity : mMoreEntities) {
			adapterEntities.add(entity);
		}
	}
}
