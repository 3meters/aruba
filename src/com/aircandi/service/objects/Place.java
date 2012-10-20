package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class Place implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public String				source;
	@Expose
	public String				sourceId;
	@Expose
	public String				sourceUri;
	@Expose
	public String				uri;
	@Expose
	public Location				location;
	@Expose
	public Contact				contact;
	@Expose
	public List<Category>		categories;
	@Expose
	public Number				rating;
	@Expose
	public Menu					menu;
	/*
	 * There could be more photos available than we actually received.
	 */
	@Expose
	public Integer				photoCount = 0;
	@Expose
	public List<Photo>			photos;
	/*
	 * There could be more tips available than we actually received.
	 */
	@Expose
	public Integer				tipCount = 0;
	@Expose
	public List<Tip>			tips;
	@Expose
	public List<String>			tags;
	@Expose
	public List<Phrase>			phrases;

	public Place() {}

	public static Place setFromPropertiesFromMap(Place place, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		place.source = (String) map.get("source");
		place.sourceId = (String) map.get("sourceId");
		place.sourceUri = (String) map.get("sourceUri");
		place.uri = (String) map.get("uri");
		place.rating = (Number) map.get("rating");

		if (map.get("location") != null) {
			place.location = (Location) Location.setFromPropertiesFromMap(new Location(), (HashMap<String, Object>) map.get("location"));
		}

		if (map.get("contact") != null) {
			place.contact = (Contact) Contact.setFromPropertiesFromMap(new Contact(), (HashMap<String, Object>) map.get("contact"));
		}

		if (map.get("categories") != null) {
			List<LinkedHashMap<String, Object>> categoryMaps = (List<LinkedHashMap<String, Object>>) map.get("categories");

			place.categories = new ArrayList<Category>();
			for (LinkedHashMap<String, Object> categoryMap : categoryMaps) {
				place.categories.add(Category.setFromPropertiesFromMap(new Category(), categoryMap));
			}
		}

		if (map.get("menu") != null) {
			place.menu = (Menu) Menu.setFromPropertiesFromMap(new Menu(), (HashMap<String, Object>) map.get("menu"));
		}
		
		if (map.get("photos") != null) {
			LinkedHashMap<String, Object> photosMap = (LinkedHashMap<String, Object>) map.get("photos");
			List<LinkedHashMap<String, Object>> photoGroups = (List<LinkedHashMap<String, Object>>) photosMap.get("groups");
			
			place.photos = new ArrayList<Photo>();
			for (LinkedHashMap<String, Object> photoGroup : photoGroups) {
				place.photoCount += (Integer) photoGroup.get("count");
				List<LinkedHashMap<String, Object>> photoItems = (List<LinkedHashMap<String, Object>>) photoGroup.get("items");
				for (LinkedHashMap<String, Object> photoItem : photoItems) {
					place.photos.add(Photo.setFromPropertiesFromMap(new Photo(), photoItem));
				}
			}
		}
		
		if (map.get("tips") != null) {
			LinkedHashMap<String, Object> tipsMap = (LinkedHashMap<String, Object>) map.get("tips");
			List<LinkedHashMap<String, Object>> tipGroups = (List<LinkedHashMap<String, Object>>) tipsMap.get("groups");
			
			place.tips = new ArrayList<Tip>();
			for (LinkedHashMap<String, Object> tipGroup : tipGroups) {
				place.tipCount += (Integer) tipGroup.get("count");
				List<LinkedHashMap<String, Object>> tipItems = (List<LinkedHashMap<String, Object>>) tipGroup.get("items");
				for (LinkedHashMap<String, Object> tipItem : tipItems) {
					place.tips.add(Tip.setFromPropertiesFromMap(new Tip(), tipItem));
				}
			}
		}

		if (map.get("tags") != null) {
			place.tags = (List<String>) map.get("tags");
		}

		if (map.get("phrases") != null) {
			List<LinkedHashMap<String, Object>> phraseMaps = (List<LinkedHashMap<String, Object>>) map.get("phrases");

			place.phrases = new ArrayList<Phrase>();
			for (LinkedHashMap<String, Object> phraseMap : phraseMaps) {
				place.phrases.add(Phrase.setFromPropertiesFromMap(new Phrase(), phraseMap));
			}
		}
		return place;
	}

}