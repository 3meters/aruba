package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class Menu extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public String				type;
	@Expose
	public String				uri;
	@Expose
	public String				mobileUri;

	public Menu() {}

	@Override
	public Menu clone() {
		try {
			final Menu menu = (Menu) super.clone();
			return menu;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}	

	public static Menu setPropertiesFromMap(Menu menu, HashMap map) {

		menu.type = (String) map.get("type");
		menu.uri = (String) map.get("url");
		menu.mobileUri = (String) map.get("mobileUrl");

		return menu;
	}
}