package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class CategorySimple extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public String				name;
	@Expose
	public String				id;
	@Expose
	public String				icon;

	public CategorySimple() {}

	@Override
	public CategorySimple clone() {
		try {
			final CategorySimple category = (CategorySimple) super.clone();
			return category;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static CategorySimple setPropertiesFromMap(CategorySimple category, HashMap map) {

		category.name = (String) map.get("name");
		category.id = (String) map.get("id");
		category.icon = (String) map.get("icon");

		return category;
	}

	public String iconUri() {
		return icon;
	}
}