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
public class Category extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public String				name;
	@Expose
	public String				id;
	@Expose
	public String				pluralName;
	@Expose
	public String				shortName;
	@Expose
	public Boolean				primary;
	@Expose
	public Icon					icon;
	@Expose
	public List<Category>		categories;

	public Category() {}

	@Override
	public Category clone() {
		try {
			final Category category = (Category) super.clone();
			if (this.icon != null) {
				category.icon = this.icon.clone();
			}
			if (this.categories != null) {
				category.categories = (List<Category>) ((ArrayList) this.categories).clone();
			}
			return category;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static Category setPropertiesFromMap(Category category, HashMap map) {

		category.name = (String) map.get("name");
		category.pluralName = (String) map.get("pluralName");
		category.primary = (Boolean) map.get("primary");
		category.id = (String) map.get("id");

		if (map.get("icon") != null) {
			category.icon = (Icon) Icon.setPropertiesFromMap(new Icon(), (HashMap<String, Object>) map.get("icon"));
		}

		if (map.get("categories") != null) {
			List<LinkedHashMap<String, Object>> categoryMaps = (List<LinkedHashMap<String, Object>>) map.get("categories");

			category.categories = new ArrayList<Category>();
			for (LinkedHashMap<String, Object> categoryMap : categoryMaps) {
				category.categories.add(Category.setPropertiesFromMap(new Category(), categoryMap));
			}
		}

		return category;
	}

	public String iconUri() {
		if (icon != null && icon.prefix != null) {
			String iconUri = icon.prefix + "88" + icon.suffix;
			return iconUri;
		}
		return null;
	}
}