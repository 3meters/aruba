package com.aircandi.service;

import android.graphics.Bitmap;

import com.aircandi.service.objects.Photo;

public class RequestListener {

	public void onStart() {}

	public void onError(Object response) {}

	public void onComplete() {}

	public void onComplete(Object response) {}

	public void onComplete(Object response, Photo photo, Bitmap bitmap, Boolean bitmapLocalOnly) {} // $codepro.audit.disable largeNumberOfParameters

	public void onProgressChanged(int progress) {}
	
	public void onCancel() {}
}