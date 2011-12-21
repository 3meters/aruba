package com.proxibase.aircandi.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

import com.proxibase.aircandi.widgets.WebImageView;

public class DrawableManager {

	private final Map	drawableMap;

	public DrawableManager() {
		drawableMap = new HashMap();
	}

	public Drawable fetchDrawable(String urlString) {
		if (drawableMap.containsKey(urlString)) {
			return (Drawable) drawableMap.get(urlString);
		}

		Logger.d(this, "Image url:" + urlString);
		try {
			InputStream is = fetch(urlString);
			Drawable drawable = Drawable.createFromStream(is, "src");
			drawableMap.put(urlString, drawable);
			Logger.v(this, "got a thumbnail drawable: " + drawable.getBounds()
																					+ ", "
																					+ drawable.getIntrinsicHeight()
																					+ ","
																					+ drawable.getIntrinsicWidth()
																					+ ", "
																					+ drawable.getMinimumHeight()
																					+ ","
																					+ drawable.getMinimumWidth());
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

	public void fetchDrawableOnThread(final String urlString, final WebImageView imageView) {
		if (drawableMap.containsKey(urlString)) {
			ImageUtils.showDrawableInImageView((Drawable) drawableMap.get(urlString), imageView);
		}

		final Handler handler = new Handler() {

			@Override
			public void handleMessage(Message message) {
				ImageUtils.showDrawableInImageView((Drawable) message.obj, imageView);
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

	private InputStream fetch(String urlString) throws MalformedURLException, IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlString);
		HttpResponse response = httpClient.execute(request);
		return response.getEntity().getContent();
	}
}
