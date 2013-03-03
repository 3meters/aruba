package com.aircandi.components;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.aircandi.beta.R;
import com.aircandi.service.objects.Entity;
import com.aircandi.ui.widgets.CandiView;

public class RadarListAdapter extends ArrayAdapter<Entity> {

	private final LayoutInflater	mInflater;
	private final Integer			mItemLayoutId	= R.layout.temp_listitem_radar;
	private List<Entity>	mItems;

	public RadarListAdapter(Context context, List<Entity> entities) {
		super(context, 0, entities);
		mItems = entities;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		final RadarViewHolder holder;
		final Entity itemData = mItems.get(position);
		Logger.v(this, "getView: position = " + String.valueOf(position) + " name = " + itemData.name);

		if (view == null) {
			view = mInflater.inflate(mItemLayoutId, null);
			holder = new RadarViewHolder();
			holder.candiView = (CandiView) view.findViewById(R.id.candi_view);
			/* Need this line so clicks bubble up to the listview click handler */
			holder.candiView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
			view.setTag(holder);
		}
		else {
			holder = (RadarViewHolder) view.getTag();
		}

		if (itemData != null) {
			final Entity entity = itemData;
			holder.data = entity;
			holder.candiView.bindToEntity(entity);
		}
		return view;
	}

	@Override
	public Entity getItem(int position) {
		return mItems.get(position);
	}

	@Override
	public int getCount() {
		return mItems.size();
	}

	public List<Entity> getItems() {
		return mItems;
	}

	public void setItems(List<Entity> items) {
		mItems = items;
	}

	@SuppressWarnings("ucd")
	private static class RadarViewHolder {
		private CandiView	candiView;
		@SuppressWarnings("unused")
		public Object		data;
	}
}
