package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import android.telephony.PhoneNumberUtils;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Place extends Entity implements Cloneable, Serializable {

	private static final long	serialVersionUID	= -3599862145425838670L;
	public static final String	collectionId		= "places";

	// --------------------------------------------------------------------------------------------
	// service fields
	// --------------------------------------------------------------------------------------------

	@Expose
	public String				address;
	@Expose
	public String				city;
	@Expose
	public String				region;
	@Expose
	public String				country;
	@Expose
	public String				postalCode;
	@Expose
	public String				phone;
	@Expose
	public ProviderMap			provider;
	@Expose
	public Category				category;

	// --------------------------------------------------------------------------------------------
	// client fields (NONE are transferred)
	// --------------------------------------------------------------------------------------------

	public Place() {}

	// --------------------------------------------------------------------------------------------
	// Set and get
	// --------------------------------------------------------------------------------------------	

	public static Place upsizeFromSynthetic(Place synthetic) {
		/*
		 * Sythetic entity created from foursquare data
		 * 
		 * We make a copy so these changes don't effect the synthetic entity
		 * in the entity model in case we keep it because of a failure.
		 */
		final Place entity = synthetic.clone();
		entity.locked = false;
		if (synthetic.category != null) {
			entity.subtitle = synthetic.category.name;
		}
		return entity;
	}

	@Override
	public Photo getPhoto() {
		Photo photo = this.photo;
		if (photo == null) {
			if (category != null && category.photo != null) {
				photo = category.photo.clone();
			}
			else {
				photo = getDefaultPhoto();
			}
		}
		photo.photoPlaceholder = getPlaceholderPhoto();
		photo.photoBroken = getBrokenPhoto();
		return photo;
	}

	public String getBeaconId() {
		final Beacon beacon = getActiveBeacon(Constants.TYPE_LINK_PROXIMITY, true);
		if (beacon != null) {
			return beacon.id;
		}
		return null;
	}

	public Provider getProvider() {
		if (provider.aircandi != null) {
			return new Provider(provider.aircandi, Constants.TYPE_PROVIDER_AIRCANDI);
		}
		else if (provider.foursquare != null) {
			return new Provider(provider.foursquare, Constants.TYPE_PROVIDER_FOURSQUARE);
		}
		else if (provider.google != null) {
			return new Provider(provider.google, Constants.TYPE_PROVIDER_GOOGLE);
		}
		else if (provider.factual != null) {
			return new Provider(provider.factual, Constants.TYPE_PROVIDER_FACTUAL);
		}
		return null;
	}

	public String getAddressBlock() {
		String addressBlock = "";
		if (address != null && !address.equals("")) {
			addressBlock = address + "<br/>";
		}

		if (city != null && region != null && !city.equals("") && !region.equals("")) {
			addressBlock += city + ", " + region;
		}
		else if (city != null && !city.equals("")) {
			addressBlock += city;
		}
		else if (region != null && !region.equals("")) {
			addressBlock += region;
		}

		if (postalCode != null && !postalCode.equals("")) {
			addressBlock += " " + postalCode;
		}
		return addressBlock;
	}

	public String getFormattedPhone() {
		return PhoneNumberUtils.formatNumber(phone);
	}

	public static Integer getCategoryColorResId(String categoryName, Boolean dark, Boolean mute, Boolean semi) {
		int colorResId = R.color.accent_gray;
		if (semi) {
			colorResId = R.color.accent_gray_semi;
		}
		if (categoryName != null && !categoryName.toLowerCase(Locale.US).equals("generic")) {

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

	public static Integer getCategoryColor(String categoryName, Boolean dark, Boolean mute, Boolean semi) {
		int colorResId = R.color.accent_gray;
		if (semi) {
			colorResId = R.color.accent_gray_semi;
		}

		if (categoryName != null && !categoryName.toLowerCase(Locale.US).equals("generic")) {

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

	@Override
	public String getCollection() {
		return collectionId;
	}

	// --------------------------------------------------------------------------------------------
	// Copy and serialization
	// --------------------------------------------------------------------------------------------

	public static Place setPropertiesFromMap(Place entity, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		entity = (Place) Entity.setPropertiesFromMap(entity, map, nameMapping);

		entity.address = (String) map.get("address");
		entity.city = (String) map.get("city");
		entity.region = (String) map.get("region");
		entity.country = (String) map.get("country");
		entity.postalCode = (String) map.get("postalCode");
		entity.phone = (String) map.get("phone");

		if (map.get("provider") != null) {
			entity.provider = ProviderMap.setPropertiesFromMap(new ProviderMap(), (HashMap<String, Object>) map.get("provider"), nameMapping);
		}

		if (map.get("category") != null) {
			entity.category = Category.setPropertiesFromMap(new Category(), (HashMap<String, Object>) map.get("category"), nameMapping);
		}

		return entity;
	}

	@Override
	public Place clone() {
		final Place place = (Place) super.clone();
		if (location != null) {
			place.location = location.clone();
		}
		if (provider != null) {
			place.provider = provider.clone();
		}
		if (category != null) {
			place.category = category.clone();
		}
		return place;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public static class SortByProximityAndDistance implements Comparator<Entity> {

		@Override
		public int compare(Entity object1, Entity object2) {

			if (object1.hasActiveProximity() && !object2.hasActiveProximity()) {
				return -1;
			}
			else if (object2.hasActiveProximity() && !object1.hasActiveProximity()) {
				return 1;
			}
			else {
				if (object1.distance == null || object2.distance == null) {
					return 0;
				}
				else if (object1.distance < object2.distance.intValue()) {
					return -1;
				}
				else if (object1.distance.intValue() > object2.distance.intValue()) {
					return 1;
				}
				else {
					return 0;
				}
			}
		}
	}

	@SuppressWarnings("ucd")
	public static class SortByDistance implements Comparator<Entity> {

		@Override
		public int compare(Entity object1, Entity object2) {

			if (object1.distance == null || object2.distance == null) {
				return 0;
			}
			else if (object1.distance < object2.distance.intValue()) {
				return -1;
			}
			else if (object1.distance.intValue() > object2.distance.intValue()) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}
}