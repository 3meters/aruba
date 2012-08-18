package com.proxibase.aircandi.components;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

import com.proxibase.aircandi.PictureSearch;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ServiceRequest;

public class DrawableManager {
	/*
	 * Serves up BitmapDrawables but caches just the bitmap. The cache holds
	 * a soft reference to the bitmap that allows the gc to collect it if memory
	 * needs to be freed. If collected, we download the bitmap again.
	 */

	private final HashMap<String, SoftReference<Bitmap>>	mBitmapCache;

	public DrawableManager() {
		mBitmapCache = new HashMap<String, SoftReference<Bitmap>>();
	}

	@SuppressLint("HandlerLeak")
	@SuppressWarnings("deprecation")
	public void fetchDrawableOnThread(final String uri, final PictureSearch.ViewHolder holder) {

		synchronized (mBitmapCache) {
			if (mBitmapCache.containsKey(uri) && mBitmapCache.get(uri).get() != null) {
				BitmapDrawable bitmapDrawable = new BitmapDrawable(mBitmapCache.get(uri).get());
				ImageUtils.showDrawableInImageView(bitmapDrawable, holder.itemImage, false);
				return;
			}
		}

		final DrawableHandler handler = new DrawableHandler(this) {

			@Override
			public void handleMessage(Message message) {
				DrawableManager drawableManager = getDrawableManager().get();
				if (drawableManager != null) {
					if (((String) holder.itemImage.getTag()).equals(uri)) {
						ImageUtils.showDrawableInImageView((Drawable) message.obj, holder.itemImage, true);
					}
				}
			}
		};

		Thread thread = new Thread() {

			@Override
			public void run() {
				Drawable drawable = fetchDrawable(uri);
				Message message = handler.obtainMessage(1, drawable);
				handler.sendMessage(message);
			}
		};
		thread.start();
	}

	@SuppressWarnings("deprecation")
	private Drawable fetchDrawable(final String uri) {

		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(uri);
		serviceRequest.setRequestType(RequestType.Get);
		serviceRequest.setResponseFormat(ResponseFormat.Bytes);

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.Success) {

			byte[] imageBytes = (byte[]) serviceResponse.data;
			Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

			if (bitmap == null) {
				throw new IllegalStateException("Stream could not be decoded to a bitmap: " + uri);
			}
			BitmapDrawable drawable = new BitmapDrawable(bitmap);
			mBitmapCache.put(uri, new SoftReference(bitmap));
			return drawable;
		}
		return null;
	}

	/*
	 * We add a weak reference to the containing class which can
	 * be checked when handling messages to ensure we don't leak memory.
	 */
	@SuppressLint("HandlerLeak")
	class DrawableHandler extends Handler {

		private final WeakReference<DrawableManager>	mDrawableManager;

		DrawableHandler(DrawableManager drawableManager) {
			mDrawableManager = new WeakReference<DrawableManager>(drawableManager);
		}

		public WeakReference<DrawableManager> getDrawableManager() {
			return mDrawableManager;
		}
	}
}
