package com.aircandi.components;

import java.util.Map;

import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.sender.HttpPostSender;
import org.acra.sender.ReportSenderException;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;

public class ReportSenderBugsense extends HttpPostSender {

	public ReportSenderBugsense(String formUri, Map<ReportField, String> mapping) {
		super(formUri, mapping);
	}

	@Override
	public void send(CrashReportData report) throws ReportSenderException {
		
		StringBuilder custom = new StringBuilder();
		/*
		 * These are null if we show the dialog to the user because there
		 * is enough time for the needed services to become unavailable.
		 */
		Boolean wifiEnabled = NetworkManager.getInstance().isWifiEnabled();
		custom.append("Custom_Wifi_Enabled=" + String.valueOf(wifiEnabled) + "\n");

		Integer networkTypeId = NetworkManager.getInstance().getNetworkTypeId();
		if (networkTypeId != null) {
			custom.append("Custom_Data_Network_Type=" + getNetworkTypeLabel(networkTypeId) + "\n");
		}

		/* Current location */
		if (LocationManager.getInstance().getLocationLocked() != null) {
			Double latitude = LocationManager.getInstance().getLocationLocked().getLatitude();
			Double longitude = LocationManager.getInstance().getLocationLocked().getLongitude();
			custom.append("Custom_Location=" + String.valueOf(latitude) + "," + String.valueOf(longitude) + "\n");
		}
		
		/* Current search radius */
		final Integer searchRangeMeters = Integer.parseInt(Aircandi.settings.getString(CandiConstants.PREF_SEARCH_RADIUS,
				CandiConstants.PREF_SEARCH_RADIUS_DEFAULT));
		custom.append("Custom_Radius_Meters=" + String.valueOf(searchRangeMeters) + "\n");

		report.put(ReportField.CUSTOM_DATA, custom.toString());
		super.send(report);
	}

	private String getNetworkTypeLabel(Integer networkTypeId) {

		String networkTypeLabel = null;
		if (networkTypeId == 0) {
			networkTypeLabel = "Mobile";
		}
		else if (networkTypeId == 1) {
			networkTypeLabel = "Wifi";
		}
		else if (networkTypeId == 2) {
			networkTypeLabel = "Mobile MMS";
		}
		else if (networkTypeId == 3) {
			networkTypeLabel = "Mobile SUPL";
		}
		else if (networkTypeId == 4) {
			networkTypeLabel = "Mobile DUN";
		}
		else if (networkTypeId == 5) {
			networkTypeLabel = "Mobile HiPri";
		}
		else if (networkTypeId == 6) {
			networkTypeLabel = "WiMax";
		}
		else if (networkTypeId == 7) {
			networkTypeLabel = "Bluetooth";
		}
		else if (networkTypeId == 8) {
			networkTypeLabel = "Dummy";
		}
		else if (networkTypeId == 9) {
			networkTypeLabel = "Ethernet";
		}
		return networkTypeLabel;

	}
}
