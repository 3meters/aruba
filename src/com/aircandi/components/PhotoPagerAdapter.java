package com.aircandi.components;

import java.util.List;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aircandi.R;
import com.aircandi.service.objects.Photo;
import com.aircandi.ui.PictureDetail;

public class PhotoPagerAdapter extends PagerAdapter {

	private Context			mContext;
	private LayoutInflater	mInflater;
	private List<Photo>		mPhotos;

	public PhotoPagerAdapter(Context context, ViewPager viewPager, List<Photo> photos) {
		super();
		mContext = context;
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mPhotos = photos;
	}

	@Override
	public int getCount() {
		return mPhotos.size();
	}

	@Override
	public void startUpdate(View arg0) {}

	@Override
	public Object instantiateItem(View collection, int position) {
		Photo photo = mPhotos.get(position);
		Integer layoutId = R.layout.temp_photo_detail;
		ViewGroup layout = (ViewGroup) mInflater.inflate(layoutId, null);
		layout = PictureDetail.buildPictureDetail(mContext, photo, layout);
		((ViewPager) collection).addView(layout, 0);
		return layout;
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

	public List<Photo> getPhotos() {
		return mPhotos;
	}

	public void setPhotos(List<Photo> photos) {
		mPhotos = photos;
	}

}
