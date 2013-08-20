package com.aircandi.applications;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.SpinnerData;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.ui.CandigramForm;
import com.aircandi.ui.EntityGrid;
import com.aircandi.ui.edit.CandigramEdit;
import com.aircandi.ui.edit.CandigramWizard;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;

public class Candigrams {
	
	public static int TYPE_DEFAULT_POSITION = 0;
	public static int RANGE_DEFAULT_POSITION = 2;
	public static int DURATION_DEFAULT_POSITION = 1;
	public static int ICON_COLOR = R.color.brand_pink_lighter;
	

	public static void view(Context context, String entityId, String parentId) {
		IntentBuilder intentBuilder = new IntentBuilder(context, CandigramForm.class)
				.setEntityId(entityId)
				.setEntityParentId(parentId);
		context.startActivity(intentBuilder.create());
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PageToPage);
	}

	public static void viewFor(Context context, String entityId, String linkType, Direction direction) {
		if (direction == null) {
			direction = Direction.in;
		}
		IntentBuilder intentBuilder = new IntentBuilder(context, EntityGrid.class)
				.setEntityId(entityId)
				.setListLinkType(linkType)
				.setListLinkDirection(direction.name())
				.setListLinkSchema(Constants.SCHEMA_ENTITY_CANDIGRAM)
				.setListItemResId(R.layout.temp_griditem_entity)
				.setListNewEnabled(true);

		context.startActivity(intentBuilder.create());
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PageToPage);
	}

	public static Intent viewForGetIntent(Context context, String entityId, String linkType, Direction direction) {
		if (direction == null) {
			direction = Direction.in;
		}
		IntentBuilder intentBuilder = new IntentBuilder(context, EntityGrid.class)
				.setEntityId(entityId)
				.setListLinkType(linkType)
				.setListLinkDirection(direction.name())
				.setListLinkSchema(Constants.SCHEMA_ENTITY_CANDIGRAM)
				.setListItemResId(R.layout.temp_griditem_entity)
				.setListNewEnabled(true);

		return intentBuilder.create();
	}

	@SuppressWarnings("ucd")
	public static void edit(Context context, Entity entity) {
		IntentBuilder intentBuilder = new IntentBuilder(context, CandigramEdit.class).setEntity(entity);
		((Activity) context).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_EDIT);
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PageToPage);
	}

	@SuppressWarnings("ucd")
	public static void insert(Context context) {
		IntentBuilder intentBuilder = new IntentBuilder(context, CandigramWizard.class).setEntitySchema(Constants.SCHEMA_ENTITY_CANDIGRAM);
		((Activity) context).startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_ENTITY_INSERT);
		Animate.doOverridePendingTransition((Activity) context, TransitionType.PageToPage);
	}
	
	public static SpinnerData getSpinnerData(Context context, PropertyType propertyType) {
		
		
		Integer entriesResId = null;
		Integer valuesResId = null;
		Integer descriptionsResId = null;
		
		if (propertyType == PropertyType.type) {
			entriesResId = R.array.candigram_type_entries;
			valuesResId = R.array.candigram_type_values;
			descriptionsResId = R.array.candigram_type_descriptions;
		}
		else if (propertyType == PropertyType.range) {
			entriesResId = R.array.candigram_range_entries;
			valuesResId = R.array.candigram_range_values;
			descriptionsResId = R.array.candigram_range_descriptions;
		}
		else if (propertyType == PropertyType.duration) {
			entriesResId = R.array.candigram_duration_entries;
			valuesResId = R.array.candigram_duration_values;
			descriptionsResId = R.array.candigram_duration_descriptions;
		}
		
		SpinnerData data = new SpinnerData(context);
		data.setEntries(entriesResId);
		data.setEntryValues(valuesResId);
		data.setDescriptions(descriptionsResId);
		
		return data;
	}


	public static List<PropertyValue> getPropertyValues(Context context, PropertyType propertyType) {
		
		List<PropertyValue> propertyValues = new ArrayList<PropertyValue>();
		
		Integer entriesResId = null;
		Integer valuesResId = null;
		Integer descriptionsResId = null;
		
		if (propertyType == PropertyType.type) {
			entriesResId = R.array.candigram_type_entries;
			valuesResId = R.array.candigram_type_values;
			descriptionsResId = R.array.candigram_type_descriptions;
		}
		
		String[] entries = context.getResources().getStringArray(entriesResId);
		String[] values = context.getResources().getStringArray(valuesResId);
		String[] description = context.getResources().getStringArray(descriptionsResId);
		
		for (int i = 0; i < entries.length; i++) {
			propertyValues.add(new PropertyValue(entries[i], values[i], description[i]));
		}
		
		return propertyValues;
	}
	

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public static class PropertyValue {

		public String	name;
		public Object	value;
		public String	description;

		public PropertyValue(String name, Object value, String description) {
			this.name = name;
			this.value = value;
			this.description = description;
		}
	}

	public enum PropertyType {
		type,
		range,
		duration,
		capture,
		locked
	}

}
