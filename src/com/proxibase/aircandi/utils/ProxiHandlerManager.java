package com.proxibase.aircandi.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;

public class ProxiHandlerManager {

	private Activity	mActivity;
	private HashMap		mProxiHandlers	= new HashMap();

	public ProxiHandlerManager(Activity activity) {
		mActivity = activity;
	}

	public boolean startProxiHandler(String proxiHandlerAction, EntityProxy proxiEntity) {

		Intent intent = new Intent();
		intent.setAction(proxiHandlerAction);

		PackageManager packageManager = mActivity.getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, 0);

		if (list.size() == 0)
			return false;

		String jsonStream = ProxibaseService.getGson(GsonType.ProxibaseService).toJson(proxiEntity);
		intent.putExtra("ProxiEntity", jsonStream);
		mActivity.startActivity(intent);
		
		/*
		 * Supply a custom animation. This one will just fade the new
		 * activity on top. Note that we need to also supply an animation
		 * (here just doing nothing for the same amount of time) for the
		 * old activity to prevent it from going away too soon.
		 */
		
		//mActivity.overridePendingTransition(R.anim.summary_in, R.anim.hold);
		// mActivity.overridePendingTransition(R.anim.fade_in_medium, R.anim.fade_out_medium);
		return true;
	}

	public String getPublicName(String packageName) {

		PackageManager packageManager = mActivity.getPackageManager();
		try {
			ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
			String publicName = (String) info.loadLabel(packageManager);
			return publicName;
		}
		catch (NameNotFoundException exception) {
			return null;
		}
	}

	public void installProxiHandler() {}

	class PInfo {

		private String		appname;
		private String		pname;
		private String		versionName;
		private int			versionCode	= 0;
		@SuppressWarnings("unused")
		private Drawable	icon;

		private void prettyPrint() {
			Log.v(appname + "\t" + pname + "\t" + versionName + "\t" + versionCode, appname);
		}
	}

	@SuppressWarnings("unused")
	private ArrayList<PInfo> getPackages() {
		ArrayList<PInfo> apps = getInstalledApps(false); /* false = no system packages */
		final int max = apps.size();
		for (int i = 0; i < max; i++) {
			apps.get(i).prettyPrint();
		}
		return apps;
	}

	private ArrayList<PInfo> getInstalledApps(boolean getSysPackages) {
		ArrayList<PInfo> res = new ArrayList<PInfo>();
		List<PackageInfo> packs = mActivity.getPackageManager().getInstalledPackages(0);
		for (int i = 0; i < packs.size(); i++) {
			PackageInfo p = packs.get(i);
			if ((!getSysPackages) && (p.versionName == null)) {
				continue;
			}
			PInfo newInfo = new PInfo();
			newInfo.appname = p.applicationInfo.loadLabel(mActivity.getPackageManager()).toString();
			newInfo.pname = p.packageName;
			newInfo.versionName = p.versionName;
			newInfo.versionCode = p.versionCode;
			newInfo.icon = p.applicationInfo.loadIcon(mActivity.getPackageManager());
			res.add(newInfo);
		}
		return res;
	}

	public HashMap getProxiHandlers() {
		return mProxiHandlers;
	}
}
