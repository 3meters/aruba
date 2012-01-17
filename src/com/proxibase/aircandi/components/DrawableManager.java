package com.proxibase.aircandi.components;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

import com.proxibase.aircandi.PictureSearch;
import com.proxibase.aircandi.PictureSearch.ViewHolder;
import com.proxibase.aircandi.components.ImageRequest.ImageShape;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class DrawableManager {

	private final Map	drawableMap;

	public DrawableManager() {
		drawableMap = new WeakHashMap();
	}

	public void fetchDrawable(final String urlString, final ViewHolder holder) {
		if (drawableMap.containsKey(urlString)) {
			ImageUtils.showDrawableInImageView((Drawable) drawableMap.get(urlString), holder.itemImage, false);
			return;
		}

		ImageRequestBuilder builder = new ImageRequestBuilder(holder.itemImage);
		builder.setFromUris(urlString, null);
		builder.setImageShape(ImageShape.Native);
		builder.setScaleToWidth(CandiConstants.IMAGE_WIDTH_ORIGINAL);
		builder.setSearchCache(false);
		builder.setUpdateCache(false);
		builder.setRequestListener(new RequestListener() {

			@Override
			public void onComplete(Object response) {
				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					Bitmap bitmap = (Bitmap) serviceResponse.data;
					BitmapDrawable drawable = new BitmapDrawable(bitmap);
					drawableMap.put(urlString, drawable);
					if (urlString.equals(holder.itemImage.getTag())) {
						ImageUtils.showDrawableInImageView(drawable, holder.itemImage, true);
					}
				}
			}
		});

		ImageRequest imageRequest = builder.create();
		ImageManager.getInstance().getImageLoader().fetchImage(imageRequest);
	}
	
	public Drawable fetchCacheDrawable(final String urlString) {

		if (drawableMap.containsKey(urlString)) {
			return (Drawable) drawableMap.get(urlString);
		}
		return null;
	}

	public Drawable fetchDrawable(final String urlString) {

		//		if (drawableMap.containsKey(urlString)) {
		//			return (Drawable) drawableMap.get(urlString);
		//		}

		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(urlString);
		serviceRequest.setRequestType(RequestType.Get);
		serviceRequest.setResponseFormat(ResponseFormat.Bytes);

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.Success) {

			byte[] imageBytes = (byte[]) serviceResponse.data;
			Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

			if (bitmap == null) {
				throw new IllegalStateException("Stream could not be decoded to a bitmap: " + urlString);
			}
			BitmapDrawable drawable = new BitmapDrawable(bitmap);
			drawableMap.put(urlString, drawable);			
			return drawable;
		}
		return null;

//		InputStream is = null;
//		try {
//			is = fetch(urlString);
//			Drawable drawable = Drawable.createFromStream(is, "src");
//			drawableMap.put(urlString, drawable);
//			is.close();
//			return drawable;
//		}
//		catch (MalformedURLException exception) {
//			Logger.e(this, "fetchDrawable failed", exception);
//			return null;
//		}
//		catch (IOException exception) {
//			Logger.e(this, "fetchDrawable failed", exception);
//			return null;
//		}
	}

	public void fetchDrawableOnThread(final String urlString, final PictureSearch.ViewHolder holder) {
		if (drawableMap.containsKey(urlString)) {
			ImageUtils.showDrawableInImageView((Drawable) drawableMap.get(urlString), holder.itemImage, false);
			return;
		}

		final Handler handler = new Handler() {

			@Override
			public void handleMessage(Message message) {
				if (((String) holder.itemImage.getTag()).equals(urlString)) {
					ImageUtils.showDrawableInImageView((Drawable) message.obj, holder.itemImage, true);
				}
			}
		};

		Thread thread = new Thread() {

			@Override
			public void run() {
				Drawable drawable = fetchDrawable(urlString);
				Message message = handler.obtainMessage(1, drawable);
				handler.sendMessage(message);
			}
		};
		thread.start();
	}

	@SuppressWarnings("unused")
	private InputStream fetch(String urlString) throws MalformedURLException, IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlString);
		HttpResponse response = httpClient.execute(request);
		return response.getEntity().getContent();
	}
}
