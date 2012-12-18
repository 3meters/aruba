package com.aircandi;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.DrawableManager.ViewHolder;
import com.aircandi.components.EndlessAdapter;
import com.aircandi.components.ImageManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Photo;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

/*
 * We often will get duplicates because the ordering of images isn't
 * guaranteed while paging.
 */
public class PictureBrowse extends FormActivity {

	private GridView			mGridView;
	private ArrayList<Photo>	mImages;
	private String				mSource;
	private String				mSourceId;
	private long				mOffset		= 0;
	private LayoutInflater		mInflater;
	private Boolean				mAsPicker;
	private static long			PAGE_SIZE	= 30L;
	private static long			LIST_MAX	= 300L;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
		bind();
	}

	private void initialize() {

		Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			mAsPicker = extras.getBoolean(CandiConstants.EXTRA_AS_PICKER);
		}

		Entity entity = ProxiExplorer.getInstance().getEntityModel().getCacheEntity(mCommon.mEntityId);
		mSource = entity.place.source;
		mSourceId = entity.place.sourceId;
		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mGridView = (GridView) findViewById(R.id.grid_gallery);

		mGridView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

				String imageUri = mImages.get(position).getImageUri();
				if (imageUri != null && !imageUri.equals("")) {
					if (mAsPicker) {
						Intent intent = new Intent();
						intent.putExtra(CandiConstants.EXTRA_URI, imageUri);
						setResult(Activity.RESULT_OK, intent);
						finish();
					}
					else {
						ProxiExplorer.getInstance().getEntityModel().setPhotos(mImages);
						Intent intent = new Intent(PictureBrowse.this, PictureDetail.class);
						intent.putExtra(CandiConstants.EXTRA_URI, imageUri);
						startActivity(intent);
						AnimUtils.doOverridePendingTransition(PictureBrowse.this, TransitionType.CandiPageToForm);
					}
				}
			}
		});
	}

	protected void bind() {

		new AsyncTask<Object, Object, Object>() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				ServiceResponse serviceResponse = loadImages(PAGE_SIZE, 0);
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					mImages = (ArrayList<Photo>) serviceResponse.data;
					mOffset += PAGE_SIZE;
					mGridView.setAdapter(new EndlessImageAdapter(mImages));
				}
				else {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.PictureSearch);
				}
				mCommon.hideBusy();
			}
		}.execute();

	}

	private ServiceResponse loadImages(long count, long offset) {
		ModelResult result = ProxiExplorer.getInstance().getEntityModel().getPlacePhotos(mSource, mSourceId, count, offset);
		return result.serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onSearchClick(View view) {
		bind();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.gc();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	protected void unbindDrawables(View view) {
		if (view.getBackground() != null) {
			view.getBackground().setCallback(null);
		}
		if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}
			try {
				((ViewGroup) view).removeAllViews();
			}
			catch (Throwable e) {
				// NOP
			}
		}
	}

	protected void unbindImageViewDrawables(ImageView imageView) {
		if (imageView.getBackground() != null) {
			imageView.getBackground().setCallback(null);
		}
		if (imageView.getDrawable() != null) {
			imageView.getDrawable().setCallback(null);
			((BitmapDrawable) imageView.getDrawable()).getBitmap().recycle();
		}
	}

	@Override
	protected int getLayoutID() {
		return R.layout.picture_browse;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	class EndlessImageAdapter extends EndlessAdapter {

		ArrayList<Photo>	moreImages	= new ArrayList<Photo>();

		EndlessImageAdapter(ArrayList<Photo> list) {
			super(new ListAdapter(list));
		}

		@Override
		protected View getPendingView(ViewGroup parent) {
			View view = mInflater.inflate(R.layout.temp_picture_search_item_placeholder, null);
			return (view);
		}

		@Override
		protected boolean cacheInBackground() {
			/* What happens if there is a connectivity error? */
			moreImages.clear();

			ServiceResponse serviceResponse = loadImages(PAGE_SIZE, mOffset);
			if (serviceResponse.responseCode == ResponseCode.Success) {
				moreImages = (ArrayList<Photo>) serviceResponse.data;
				Logger.d(this, "Request more photos: start = " + String.valueOf(mOffset)
						+ " new total = "
						+ String.valueOf(getWrappedAdapter().getCount() + moreImages.size()));
				mOffset += PAGE_SIZE;
				return ((getWrappedAdapter().getCount() + moreImages.size()) < LIST_MAX);
			}
			else {
				mCommon.handleServiceError(serviceResponse, ServiceOperation.PictureBrowse);
				return false;
			}
		}

		@Override
		protected void appendCachedData() {
			ArrayAdapter<Photo> list = (ArrayAdapter<Photo>) getWrappedAdapter();
			for (Photo photo : moreImages) {
				list.add(photo);
			}
		}
	}

	private class ListAdapter extends ArrayAdapter<Photo> {

		public ListAdapter(ArrayList<Photo> list) {
			super(PictureBrowse.this, 0, list);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;
			final ViewHolder holder;
			Photo itemData = (Photo) mImages.get(position);

			if (view == null) {
				view = mInflater.inflate(R.layout.temp_picture_search_item, null);
				holder = new ViewHolder();
				holder.itemImage = (ImageView) view.findViewById(R.id.item_image);
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
			}

			if (itemData != null) {
				holder.data = itemData;
				holder.itemImage.setTag(itemData.getImageSizedUri(100, 100));
				holder.itemImage.setImageBitmap(null);
				ImageManager.getInstance().getDrawableManager().fetchDrawableOnThread(itemData.getImageSizedUri(100, 100), holder, null);
			}
			return view;
		}
	}
}