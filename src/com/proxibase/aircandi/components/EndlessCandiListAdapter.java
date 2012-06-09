package com.proxibase.aircandi.components;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.service.objects.Entity;

public class EndlessCandiListAdapter extends EndlessAdapter {

	private List<Entity>	mMoreEntities	= new ArrayList<Entity>();
	private EntityProvider	mEntityProvider;
	private LayoutInflater	mInflater;

	public EndlessCandiListAdapter(Context context, List<Entity> entities, EntityProvider entityProvider, Integer itemLayoutId) {
		super(new CandiListAdapter(context, entities, itemLayoutId));
		mEntityProvider = entityProvider;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	protected View getPendingView(ViewGroup parent) {
		View view = mInflater.inflate(R.layout.temp_candi_list_item_placeholder, null);
		return (view);
	}

	@Override
	protected boolean cacheInBackground() {
		mMoreEntities.clear();
		if (mEntityProvider.isMore()) {
			List<Entity> moreEntities = mEntityProvider.loadEntities();
			mMoreEntities = moreEntities;
			if (mEntityProvider.isMore()) {
				return ((getWrappedAdapter().getCount() + mMoreEntities.size()) < CandiConstants.RADAR_ENTITY_MAX_LIMIT);
			}
		}
		return false;
	}

	@Override
	protected void appendCachedData() {
		ArrayAdapter<Entity> entities = (ArrayAdapter<Entity>) getWrappedAdapter();
		for (Entity entity : mMoreEntities) {
			entities.add(entity);
		}
	}
}
