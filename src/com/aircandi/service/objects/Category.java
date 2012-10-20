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
public class Category implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public String				name;
	@Expose
	public String				pluralName;
	@Expose
	public Boolean				primary;
	@Expose
	public Icon					icon;
	@Expose
	public List<Category>		categories;

	public Category() {}

	public static Category setFromPropertiesFromMap(Category category, HashMap map) {

		category.name = (String) map.get("name");
		category.pluralName = (String) map.get("pluralName");
		category.primary = (Boolean) map.get("primary");

		if (map.get("icon") != null) {
			category.icon = (Icon) Icon.setFromPropertiesFromMap(new Icon(), (HashMap<String, Object>) map.get("icon"));
		}

		if (map.get("categories") != null) {
			List<LinkedHashMap<String, Object>> categoryMaps = (List<LinkedHashMap<String, Object>>) map.get("categories");

			category.categories = new ArrayList<Category>();
			for (LinkedHashMap<String, Object> categoryMap : categoryMaps) {
				category.categories.add(Category.setFromPropertiesFromMap(new Category(), categoryMap));
			}
		}

		return category;
	}

}