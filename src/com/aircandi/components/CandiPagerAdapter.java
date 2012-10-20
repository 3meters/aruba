package com.aircandi.components;

import java.util.List;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aircandi.CandiForm;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.objects.Entity;
import com.aircandi.R;

public class CandiPagerAdapter extends PagerAdapter {

	private Context			mContext;
	private LayoutInflater	mInflater;
	private List<Entity>	mEntities;

	public CandiPagerAdapter(Context context, ViewPager viewPager, List<Entity> entities) {
		super();
		mContext = context;
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mEntities = entities;
	}

	@Override
	public int getCount() {
		return mEntities.size();
	}

	@Override
	public void startUpdate(View arg0) {}

	@Override
	public Object instantiateItem(View collection, int position) {
		Entity entity = mEntities.get(position);
		Integer layoutId = R.layout.temp_candi_form;
		if (entity.type.equals(CandiConstants.TYPE_CANDI_POST)) {
			layoutId = R.layout.temp_candi_form_post;
		}
		else if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
			layoutId = R.layout.temp_candi_form_place;
		}
		ViewGroup candiInfoView = (ViewGroup) mInflater.inflate(layoutId, null);
		candiInfoView = CandiForm.buildCandiInfo(entity, candiInfoView, null, false);
		((ViewPager) collection).addView(candiInfoView, 0);
		return candiInfoView;
	}

	public int getItemPosition(Object object) {
		/*
		 * Causes the view pager to recreate all the pages
		 * when notifyDataSetChanged is call on the adapter.
		 */
		return POSITION_NONE;
	}

	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((ViewGroup) view);
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((ViewGroup) object);
	}

	@Override
	public void finishUpdate(View arg0) {}

	@Override
	public Parcelable saveState() {
		return null;
	}

	@Override
	public void restoreState(Parcelable state, ClassLoader loader) {}

	public List<Entity> getEntities() {
		return mEntities;
	}

	public void setEntities(List<Entity> entities) {
		mEntities = entities;
	}

}
