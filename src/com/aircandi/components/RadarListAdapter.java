package com.aircandi.components;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.aircandi.R;
import com.aircandi.service.objects.Entity;
import com.aircandi.ui.widgets.CandiView;

public class RadarListAdapter extends ArrayAdapter<Entity> {

	private LayoutInflater	mInflater;
	private Integer			mItemLayoutId	= R.layout.temp_listitem_radar;
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
		Entity itemData = (Entity) mItems.get(position);

		if (view == null) {
			view = mInflater.inflate(mItemLayoutId, null);
			holder = new RadarViewHolder();
			holder.candiView = (CandiView) view.findViewById(R.id.candi_view);
			holder.candiView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
			holder.candiView.initialize();
			holder.candiView.setLayoutId(R.layout.widget_candi_view_radar);
			view.setTag(holder);
		}
		else {
			holder = (RadarViewHolder) view.getTag();
		}

		if (itemData != null) {
			Entity entity = itemData;
			holder.data = entity;
			holder.candiView.bindToEntity(entity);
		}
		return view;
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
	}

	@Override
	public Entity getItem(int position) {
		return mItems.get(position);
	}

	@Override
	public int getCount() {
		return mItems.size();
	}

	public static class RadarViewHolder {

		public int			position;
		public CandiView	candiView;
		public Object		data;
	}
}
