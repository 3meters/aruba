package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Place extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= -3599862145425838670L;

	@Expose
	public String				provider;
	@Expose
	public String				id;

	/* Can come from foursquare or our own custom places */

	@Expose
	public Contact				contact;
	@Expose
	public Location				location;
	@Expose
	public Category				category;

	public Place() {}

	@Override
	public Place clone() {
		try {
			final Place place = (Place) super.clone();
			if (this.location != null) {
				place.location = this.location.clone();
			}
			if (this.contact != null) {
				place.contact = this.contact.clone();
			}
			if (this.category != null) {
				place.category = this.category.clone();
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
		place.provider = (String) map.get("provider");
		place.id = (String) map.get("id");

		if (map.get("location") != null) {
			place.location = Location.setPropertiesFromMap(new Location(), (HashMap<String, Object>) map.get("location"));
		}

		if (map.get("contact") != null) {
			place.contact = Contact.setPropertiesFromMap(new Contact(), (HashMap<String, Object>) map.get("contact"));
		}

		if (map.get("category") != null) {
			place.category = Category.setPropertiesFromMap(new Category(), (HashMap<String, Object>) map.get("category"));
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

	static public Integer getCategoryColorResId(String categoryName, Boolean dark, Boolean mute, Boolean semi) {
		int colorResId = R.color.accent_gray;
		if (semi) {
			colorResId = R.color.accent_gray_semi;
		}
		if (categoryName != null) {

			final Random rand = new Random(categoryName.hashCode());
			final int colorIndex = rand.nextInt(5 - 1 + 1) + 1;
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

	static public Integer getCategoryColor(String categoryName, Boolean dark, Boolean mute, Boolean semi) {
		int colorResId = R.color.accent_gray;
		if (semi) {
			colorResId = R.color.accent_gray_semi;
		}

		if (categoryName != null) {

			final Random rand = new Random(categoryName.hashCode());
			final int colorIndex = rand.nextInt(5 - 1 + 1) + 1;
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