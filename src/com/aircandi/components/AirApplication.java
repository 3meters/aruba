package com.aircandi.components;

public class AirApplication {

	public Integer	iconResId;
	public String	title;
	public String	description;
	public String	schema;

	public AirApplication() {}

	public AirApplication(Integer iconResId, String title, String description, String schema) {
		this.iconResId = iconResId;
		this.description = description;
		this.title = title;
		this.schema = schema;
	}
}
