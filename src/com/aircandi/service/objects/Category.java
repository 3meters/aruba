package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aircandi.ProxiConstants;
import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Category extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public String				name;
	@Expose
	public String				id;
	@Expose
	public String				icon;
	
	@Expose(serialize = false, deserialize = true)
	public List<Category>		categories;

	public Category() {}

	@Override
	public Category clone() {
		try {
			final Category category = (Category) super.clone();
			if (categories != null) {
				category.categories = (List<Category>) ((ArrayList) categories).clone();
			}
			return category;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static Category setPropertiesFromMap(Category category, Map map) {

		category.name = (String) map.get("name");
		category.id = (String) map.get("id");
		category.icon = (String) map.get("icon");

		if (map.get("categories") != null) {
			final List<LinkedHashMap<String, Object>> categoryMaps = (List<LinkedHashMap<String, Object>>) map.get("categories");

			category.categories = new ArrayList<Category>();
			for (Map<String,Object> categoryMap : categoryMaps) {
				category.categories.add(Category.setPropertiesFromMap(new Category(), categoryMap));
			}
		}

		return category;
	}

	public String iconUri() {
		return ProxiConstants.URL_PROXIBASE_SERVICE + icon;
	}
}