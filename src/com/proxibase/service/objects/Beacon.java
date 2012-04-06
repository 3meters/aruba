package com.proxibase.service.objects;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */

public class Beacon extends ServiceEntry {

	@Expose
	public String			ssid;
	@Expose
	public String			bssid;
	@Expose
	public String			label;
	@Expose
	public Boolean			locked;
	@Expose
	public String			visibility;
	@Expose
	public String			beaconType;
	@Expose
	public Number			latitude;
	@Expose
	public Number			longitude;
	@Expose
	public Number			altitude;
	@Expose
	public Number			accuracy;
	@Expose
	public Number			bearing;
	@Expose
	public Number			speed;

	// For client use only
	public int				scanLevelDb;
	public boolean			serviceVerified		= false;
	public boolean			registered			= false;
	public boolean			hidden				= false;
	public boolean			dirty				= false;
	public Date				discoveryTime;
	public boolean			detectedLastPass	= false;
	public BeaconState		state				= BeaconState.Normal;
	public List<Entity>		entities			= new ArrayList<Entity>();
	public int				scanMisses			= 0;
	public List<Integer>	scanPasses			= new ArrayList<Integer>();

	public Beacon() {}

	public Beacon(String bssid, String ssid, String label, int levelDb, Date discoveryTime) {
		this.id = "0003:" + bssid;
		this.ssid = ssid;
		this.bssid = bssid;
		this.label = label;
		this.scanLevelDb = levelDb;
		this.discoveryTime = discoveryTime;
	}

	public Integer getAvgBeaconLevel() {
		int scanHits = scanPasses.size();
		int scanLevelSum = 0;
		for (int scanLevel : this.scanPasses) {
			scanLevelSum += scanLevel;
		}
		return Math.round(scanLevelSum / scanHits);
	}

	public void addScanPass(int level) {
		scanPasses.add(0, level);
		while (scanPasses.size() > 1) {
			scanPasses.remove(1);
		}
	}

	@Override
	public String getCollection() {
		return "beacons";
	}

	public float getLevelPcnt() {

		int avgZoneLevel = Math.abs(this.getAvgBeaconLevel());
		float levelPcnt = 1f;

		if (avgZoneLevel >= 90)
			levelPcnt = .1f;
		else if (avgZoneLevel >= 80)
			levelPcnt = .3f;
		else if (avgZoneLevel >= 70)
			levelPcnt = .5f;
		else if (avgZoneLevel >= 60)
			levelPcnt = .7f;
		else if (avgZoneLevel >= 50)
			levelPcnt = .8f;

		return levelPcnt;
	}

	public static enum BeaconType {
		Fixed, Mobile, Temporary
	}

	public static enum BeaconState {
		New, Normal
	}
}