package com.proxibase.aircandi.components;

import java.util.Map;
import java.util.WeakHashMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

import com.proxibase.aircandi.PictureSearch;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class DrawableManager {

	private final Map	drawableMap;

	public DrawableManager() {
		drawableMap = new WeakHashMap();
	}

	public void fetchDrawableOnThread(final String uri, final PictureSearch.ViewHolder holder) {
		if (drawableMap.containsKey(uri)) {
			ImageUtils.showDrawableInImageView((Drawable) drawableMap.get(uri), holder.itemImage, false);
			return;
		}

		final Handler handler = new Handler() {

			@Override
			public void handleMessage(Message message) {
				if (((String) holder.itemImage.getTag()).equals(uri)) {
					ImageUtils.showDrawableInImageView((Drawable) message.obj, holder.itemImage, true);
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
			drawableMap.put(uri, drawable);
			return drawable;
		}
		return null;
	}

}
