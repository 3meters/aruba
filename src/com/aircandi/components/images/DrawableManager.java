package com.aircandi.components.images;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
import android.widget.ImageView;

import com.aircandi.components.GifDecoder;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.images.ImageRequest.ImageResponse;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.ImageUtils;

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
	public void fetchDrawableOnThread(final String uri, final ViewHolder holder, final RequestListener listener) {

		synchronized (mBitmapCache) {
			if (mBitmapCache.containsKey(uri) && mBitmapCache.get(uri).get() != null) {
				Bitmap bitmap = mBitmapCache.get(uri).get();
				
				if (listener != null) {
					bitmap = listener.onFilter(bitmap);
				}
				BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
				ImageUtils.showDrawableInImageView(bitmapDrawable, holder.itemImage, false, AnimUtils.fadeInMedium());
				if (listener != null) {
					listener.onComplete(new ServiceResponse());
				}
				return;
			}
		}

		final DrawableHandler handler = new DrawableHandler(this) {

			@Override
			public void handleMessage(Message message) {
				DrawableManager drawableManager = getDrawableManager().get();
				if (drawableManager != null) {
					if (((String) holder.itemImage.getTag()).equals(uri)) {
						ServiceResponse serviceResponse = (ServiceResponse) message.obj;
						if (serviceResponse.responseCode == ResponseCode.Success) {
							ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
							
							if (listener != null) {
								imageResponse.bitmap = listener.onFilter(imageResponse.bitmap);
							}
							BitmapDrawable drawable = new BitmapDrawable(imageResponse.bitmap);
							ImageUtils.showDrawableInImageView((Drawable) drawable, holder.itemImage, true, AnimUtils.fadeInMedium());
						}
						if (listener != null) {
							listener.onComplete(serviceResponse);
						}
					}
				}
			}
		};

		Thread thread = new Thread() {

			@Override
			public void run() {
				ServiceResponse serviceResponse = fetchDrawable(uri, listener);
				Message message = handler.obtainMessage(1, serviceResponse);
				handler.sendMessage(message);
			}
		};
		thread.start();
	}

	private ServiceResponse fetchDrawable(final String uri, final RequestListener listener) {

		ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(uri)
				.setRequestType(RequestType.Get)
				.setResponseFormat(ResponseFormat.Bytes)
				.setRequestListener(listener);

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.Success) {

			byte[] imageBytes = (byte[]) serviceResponse.data;

			String extension = "";
			int i = uri.lastIndexOf('.');
			if (i > 0) {
				extension = uri.substring(i + 1);
			}

			Bitmap bitmap = null;
			if (extension.equals("gif")) {
				InputStream inputStream = new ByteArrayInputStream(imageBytes);
				GifDecoder decoder = new GifDecoder();
				decoder.read(inputStream);
				bitmap = decoder.getBitmap();
			}
			else {
				bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
			}

			if (bitmap == null) {
				throw new IllegalStateException("Stream could not be decoded to a bitmap: " + uri);
			}
			
			if (listener != null) {
				bitmap = listener.onFilter(bitmap);
			}

			serviceResponse.data = new ImageResponse(bitmap, uri);
			mBitmapCache.put(uri, new SoftReference(bitmap));
		}
		return serviceResponse;
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

	public static class ViewHolder {

		public ImageView	itemImage;
		public Object		data;
	}
}
