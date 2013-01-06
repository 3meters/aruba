package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class Place extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= -3599862145425838670L;

	@Expose
	public String				source;
	@Expose
	public String				sourceId;

	/* Can come from foursquare or our own custom places */

	@Expose
	public String				website;
	@Expose
	public String				facebook;
	@Expose
	public Contact				contact;
	@Expose
	public Location				location;
	@Expose
	public List<Category>		categories;

	/* Only comes from foursquare */

	@Expose(serialize = false)
	public String				sourceUri;										// Link to foursquare page
	@Expose(serialize = false)
	public String				sourceUriShort;								// Link to foursquare page
	@Expose(serialize = false)
	public Number				rating;
	@Expose(serialize = false)
	public Menu					menu;
	/*
	 * There could be more photos available than we actually received.
	 */
	@Expose(serialize = false)
	public Integer				photoCount			= 0;
	@Expose(serialize = false)
	public List<Photo>			photos;
	/*
	 * There could be more tips available than we actually received.
	 */
	@Expose(serialize = false)
	public Integer				tipCount			= 0;
	@Expose(serialize = false)
	public List<Tip>			tips;
	@Expose(serialize = false)
	public List<String>			tags;
	@Expose(serialize = false)
	public List<Phrase>			phrases;

	public Place() {}

	@Override
	public Place clone() {
		try {
			final Place place = (Place) super.clone();
			if (this.categories != null) {
				place.categories = (List<Category>) ((ArrayList) this.categories).clone();
			}
			if (this.photos != null) {
				place.photos = (List<Photo>) ((ArrayList) this.photos).clone();
			}
			if (this.tips != null) {
				place.tips = (List<Tip>) ((ArrayList) this.tips).clone();
			}
			if (this.phrases != null) {
				place.phrases = (List<Phrase>) ((ArrayList) this.phrases).clone();
			}
			if (this.location != null) {
				place.location = this.location.clone();
			}
			if (this.contact != null) {
				place.contact = this.contact.clone();
			}
			if (this.menu != null) {
				place.menu = this.menu.clone();
			}
			return place;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static Place setPropertiesFromMap(Place place, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		place.source = (String) map.get("source");
		place.sourceId = (String) map.get("sourceId");
		place.sourceUri = (String) map.get("sourceUri");
		place.sourceUriShort = (String) map.get("sourceUriShort");
		place.website = (String) map.get("website");
		place.facebook = (String) map.get("facebook");
		place.rating = (Number) map.get("rating");

		if (map.get("location") != null) {
			place.location = (Location) Location.setPropertiesFromMap(new Location(), (HashMap<String, Object>) map.get("location"));
		}

		if (map.get("contact") != null) {
			place.contact = (Contact) Contact.setPropertiesFromMap(new Contact(), (HashMap<String, Object>) map.get("contact"));
		}

		if (map.get("categories") != null) {
			List<LinkedHashMap<String, Object>> categoryMaps = (List<LinkedHashMap<String, Object>>) map.get("categories");

			place.categories = new ArrayList<Category>();
			for (LinkedHashMap<String, Object> categoryMap : categoryMaps) {
				place.categories.add(Category.setPropertiesFromMap(new Category(), categoryMap));
			}
		}

		if (map.get("menu") != null) {
			place.menu = (Menu) Menu.setPropertiesFromMap(new Menu(), (HashMap<String, Object>) map.get("menu"));
		}

		if (map.get("photos") != null) {
			LinkedHashMap<String, Object> photosMap = (LinkedHashMap<String, Object>) map.get("photos");
			List<LinkedHashMap<String, Object>> photoGroups = (List<LinkedHashMap<String, Object>>) photosMap.get("groups");
			place.photoCount = (Integer) photosMap.get("count");

			place.photos = new ArrayList<Photo>();
			for (LinkedHashMap<String, Object> photoGroup : photoGroups) {
				List<LinkedHashMap<String, Object>> photoItems = (List<LinkedHashMap<String, Object>>) photoGroup.get("items");
				for (LinkedHashMap<String, Object> photoItem : photoItems) {
					place.photos.add(Photo.setPropertiesFromMap(new Photo(), photoItem));
				}
			}
		}

		if (map.get("tips") != null) {
			LinkedHashMap<String, Object> tipsMap = (LinkedHashMap<String, Object>) map.get("tips");
			List<LinkedHashMap<String, Object>> tipGroups = (List<LinkedHashMap<String, Object>>) tipsMap.get("groups");
			place.tipCount = (Integer) tipsMap.get("count");

			place.tips = new ArrayList<Tip>();
			for (LinkedHashMap<String, Object> tipGroup : tipGroups) {
				List<LinkedHashMap<String, Object>> tipItems = (List<LinkedHashMap<String, Object>>) tipGroup.get("items");
				for (LinkedHashMap<String, Object> tipItem : tipItems) {
					place.tips.add(Tip.setPropertiesFromMap(new Tip(), tipItem));
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
				place.phrases.add(Phrase.setPropertiesFromMap(new Phrase(), phraseMap));
			}
		}
		return place;
	}

	public Contact getContact() {
		if (contact == null) {
			contact = new Contact();
		}
		return contact;
	}

	public Location getLocation() {
		if (location == null) {
			location = new Location();
		}
		return location;
	}

	public Category getCategoryPrimary() {
		Category categoryDefault = null;
		if (categories != null && categories.size() > 0) {
			for (Category category : categories) {
				categoryDefault = category;
				if (category.primary != null && category.primary) {
					return category;
				}
			}
		}
		return categoryDefault;
	}

	public String getCategoryString() {
		String categories = "";
		if (this.categories != null && this.categories.size() > 0) {
			for (Category category : this.categories) {
				if (category.primary != null && category.primary) {
					categories += "<b>" + category.name + "</b>, ";
				}
				else {
					categories += category.name + ", ";
				}
			}
			categories = categories.substring(0, categories.length() - 2);
		}
		return categories;
	}

	public Integer getCategoryColorResId(Boolean dark, Boolean mute, Boolean semi) {
		int colorResId = R.color.accent_gray;
		if (semi) {
			colorResId = R.color.accent_gray_semi;
		}
		Category category = getCategoryPrimary();
		if (category != null) {

			Random rand = new Random(category.name.hashCode());
			int colorIndex = rand.nextInt(5 - 1 + 1) + 1;
			if (colorIndex == 1) {
				colorResId = R.color.accent_blue;
				if (dark) {
					colorResId = R.color.accent_blue_dark;
				}
				if (mute) {
					colorResId = R.color.accent_blue_dark_mute;
				}
				if (semi) {
					colorResId = R.color.accent_blue_dark_mute_semi;
				}
			}
			else if (colorIndex == 2) {
				colorResId = R.color.accent_orange;
				if (dark) {
					colorResId = R.color.accent_orange_dark;
				}
				if (mute) {
					colorResId = R.color.accent_orange_dark_mute;
				}
				if (semi) {
					colorResId = R.color.accent_orange_dark_mute_semi;
				}
			}
			else if (colorIndex == 3) {
				colorResId = R.color.accent_green;
				if (dark) {
					colorResId = R.color.accent_green_dark;
				}
				if (mute) {
					colorResId = R.color.accent_green_dark_mute;
				}
				if (semi) {
					colorResId = R.color.accent_green_dark_mute_semi;
				}
			}
			else if (colorIndex == 4) {
				colorResId = R.color.accent_purple;
				if (dark) {
					colorResId = R.color.accent_purple_dark;
				}
				if (mute) {
					colorResId = R.color.accent_purple_dark_mute;
				}
				if (semi) {
					colorResId = R.color.accent_purple_dark_mute_semi;
				}
			}
			else if (colorIndex == 5) {
				colorResId = R.color.accent_red;
				if (dark) {
					colorResId = R.color.accent_red_dark;
				}
				if (mute) {
					colorResId = R.color.accent_red_dark_mute;
				}
				if (semi) {
					colorResId = R.color.accent_red_dark_mute_semi;
				}
			}
		}
		return colorResId;
	}

	public Integer getCategoryColor(Boolean dark, Boolean mute, Boolean semi) {
		int colorResId = R.color.accent_gray;
		if (semi) {
			colorResId = R.color.accent_gray_semi;
		}
		Category category = getCategoryPrimary();
		if (category != null) {

			Random rand = new Random(category.name.hashCode());
			int colorIndex = rand.nextInt(5 - 1 + 1) + 1;
			if (colorIndex == 1) {
				colorResId = R.color.accent_blue;
				if (dark) {
					colorResId = R.color.accent_blue_dark;
				}
				if (mute) {
					colorResId = R.color.accent_blue_dark_mute;
				}
				if (semi) {
					colorResId = R.color.accent_blue_dark_mute_semi;
				}
			}
			else if (colorIndex == 2) {
				colorResId = R.color.accent_orange;
				if (dark) {
					colorResId = R.color.accent_orange_dark;
				}
				if (mute) {
					colorResId = R.color.accent_orange_dark_mute;
				}
				if (semi) {
					colorResId = R.color.accent_orange_dark_mute_semi;
				}
			}
			else if (colorIndex == 3) {
				colorResId = R.color.accent_green;
				if (dark) {
					colorResId = R.color.accent_green_dark;
				}
				if (mute) {
					colorResId = R.color.accent_green_dark_mute;
				}
				if (semi) {
					colorResId = R.color.accent_green_dark_mute_semi;
				}
			}
			else if (colorIndex == 4) {
				colorResId = R.color.accent_purple;
				if (dark) {
					colorResId = R.color.accent_purple_dark;
				}
				if (mute) {
					colorResId = R.color.accent_purple_dark_mute;
				}
				if (semi) {
					colorResId = R.color.accent_purple_dark_mute_semi;
				}
			}
			else if (colorIndex == 5) {
				colorResId = R.color.accent_red;
				if (dark) {
					colorResId = R.color.accent_red_dark;
				}
				if (mute) {
					colorResId = R.color.accent_red_dark_mute;
				}
				if (semi) {
					colorResId = R.color.accent_red_dark_mute_semi;
				}
			}
		}
		return Aircandi.getInstance().getResources().getColor(colorResId);
	}
}