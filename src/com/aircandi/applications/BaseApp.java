package com.aircandi.applications;

import android.app.Activity;

import com.aircandi.service.objects.Entity;

public abstract class BaseApp {
	
	public BaseApp() {}

	public static void view(Activity activity, Entity entity) {}

	public static void viewFor(Activity activity, Entity entity, String linkType) {}

	public static void edit(Activity activity, Entity entity) {}

	public static void insert(Activity activity) {}
}
