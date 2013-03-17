package com.aircandi.components;

import java.util.Map;

import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.sender.HttpPostSender;
import org.acra.sender.ReportSenderException;

public class ReportSenderBugsense extends HttpPostSender {

	public ReportSenderBugsense(String formUri, Map<ReportField, String> mapping) {
		super(formUri, mapping);
	}

	@Override
	public void send(CrashReportData arg0) throws ReportSenderException {
		/*
		 * These are null if we show the dialog to the user because there
		 * is enough time for the needed services to become unavailable.
		 */
		Boolean wifiEnabled = NetworkManager.getInstance().isWifiEnabled();
		Boolean isMobileNetwork = NetworkManager.getInstance().isMobileNetwork();

		StringBuilder custom = new StringBuilder();
		custom.append("Custom_Wifi_Enabled=" + String.valueOf(wifiEnabled) + "\n");
		custom.append("Custom_Network_Mobile=" + String.valueOf(isMobileNetwork) + "\n");

		arg0.put(ReportField.CUSTOM_DATA, custom.toString());
		super.send(arg0);
	}
}
