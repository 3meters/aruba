package com.proxibase.aircandi.components;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

import com.proxibase.aircandi.PictureSearch;

public class DrawableManager {

	private final Map	drawableMap;

	public DrawableManager() {
		drawableMap = new WeakHashMap();
	}

	public Drawable fetchDrawable(String urlString) {
		
		if (drawableMap.containsKey(urlString)) {
			return (Drawable) drawableMap.get(urlString);
		}

		InputStream is = null;
		try {
			is = fetch(urlString);
			Drawable drawable = Drawable.createFromStream(is, "src");
			drawableMap.put(urlString, drawable);
			is.close();
			return drawable;
		}
		catch (MalformedURLException exception) {
			Logger.e(this, "fetchDrawable failed", exception);
			return null;
		}
		catch (IOException exception) {
			Logger.e(this, "fetchDrawable failed", exception);
			return null;
		}
	}

	public void fetchDrawableOnThread(final String urlString, final PictureSearch.ViewHolder holder) {
		if (drawableMap.containsKey(urlString)) {
			//Logger.v(this, "Image fetched from drawable cache: " + urlString);
			ImageUtils.showDrawableInImageView((Drawable) drawableMap.get(urlString), holder.itemImage, false);
			return;
		}

		final Handler handler = new Handler() {

			@Override
			public void handleMessage(Message message) {
				if (((String) holder.itemImage.getTag()).equals(urlString)) {
					ImageUtils.showDrawableInImageView((Drawable) message.obj, holder.itemImage, false);
				}
			}
		};

		Thread thread = new Thread() {

			@Override
			public void run() {
				Drawable drawable = fetchDrawable(urlString);
				//Logger.v(this, "Image downloaded: " + urlString);
				Message message = handler.obtainMessage(1, drawable);
				handler.sendMessage(message);
			}
		};
		thread.start();
	}

	private InputStream fetch(String urlString) throws MalformedURLException, IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlString);
		HttpResponse response = httpClient.execute(request);
		return response.getEntity().getContent();
	}
}
