package com.georain.ripple.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.graphics.Bitmap;
import com.georain.ripple.controller.Ripple;
import com.google.gson.annotations.Expose;
import com.threemeters.sdk.android.core.Stream;

/**
 * @author Jayma
 */
public class Tag
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String				entityId;
	@Expose
	public String				tagId;
	@Expose
	public String				label;
	@Expose
	public String				title;
	@Expose
	public String				subtitle;
	@Expose
	public String				description;
	@Expose
	public Float				signalThreshold;
	@Expose
	public String				pointResourceId;
	@Expose
	public String				spotId;
	@Expose
	public String				themeId;
	@Expose
	public List<Stream>			streams;

	// Client only fields
	public int					levelDb;
	public boolean				isServiceVerified	= false;
	public boolean				isTagged			= false;
	public boolean				isSelected			= false;
	public boolean				isFavorite			= false;
	public boolean				isDirty				= false;
	public Bitmap				image				= null;
	public Bitmap				imageWithReflection	= null;
	public String				imageUrl;
	public Date					discoveryTime;
	public boolean				isNew				= false;

	// public int scanHits = 0;
	// public int scanLevelSum = 0;
	public int					scanMisses			= 0;
	public ArrayList<Integer>	scanPasses			= new ArrayList<Integer>();

	public Tag() {}

	public Tag(String label, String bssid, String ssid, int levelDb) {
		this.label = label;
		this.tagId = bssid;
		this.levelDb = levelDb;
	}

	public int getAvgPointLevel()
	{
		int scanHits = scanPasses.size();
		int scanLevelSum = 0;
		for (int scanLevel : this.scanPasses)
			scanLevelSum += scanLevel;
		return Math.round(scanLevelSum / scanHits);
	}

	public void addScanPass(int level)
	{
		try
		{
			scanPasses.add(0, level);
			while (scanPasses.size() > 1)
				scanPasses.remove(1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
	}

	public float getLevelPcnt()
	{
		int avgZoneLevel = Math.abs(this.getAvgPointLevel());
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

	public String getUriOdata()
	{
		String root = Ripple.URL_RIPPLESERVICE_ODATA;
		String entity = "Entities";
		String uri = root + entity + "(guid'" + this.entityId + "')";
		return uri;
	}
}