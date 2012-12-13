package com.aircandi.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * The manifest Receiver is used to detect changes in battery state. 
 * When the system broadcasts a "Battery Low" warning we turn off
 * the passive location updates to conserve battery when the app is
 * in the background. 
 * 
 * When the system broadcasts "Battery OK" to indicate the battery
 * has returned to an okay state, the passive location updates are 
 * resumed.
 */
public class PowerStateChangedReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
	  
    boolean batteryLow = intent.getAction().equals(Intent.ACTION_BATTERY_LOW);
 
    PackageManager pm = context.getPackageManager();
    ComponentName passiveLocationReceiver = new ComponentName(context, PassiveLocationChangedReceiver.class);
    
    // Disable the passive location update receiver when the battery state is low.
    // Disabling the Receiver will prevent the app from initiating the background
    // downloads of nearby locations.
    pm.setComponentEnabledSetting(passiveLocationReceiver,
      batteryLow ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 
      PackageManager.DONT_KILL_APP);
	}
}