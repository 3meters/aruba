package com.aircandi.ui.edit;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ViewFlipper;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.LocationManager;
import com.aircandi.components.TabManager;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.widgets.BuilderButton;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;

public class PlaceEdit extends BaseEntityEdit {

	private TabManager	mTabManager;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		if (mEntity != null) {
			if (mEntity.ownerId != null && (mEntity.ownerId.equals(Aircandi.getInstance().getCurrentUser().id))) {
				mTabManager = new TabManager(Constants.TABS_ENTITY_FORM_ID, mActionBar, (ViewFlipper) findViewById(R.id.flipper_form));
				mTabManager.initialize();
				mTabManager.doRestoreInstanceState(savedInstanceState);
			}
		}
	}

	@Override
	public void draw() {

		/* Place content */
		Place place = (Place) mEntity;

		if (findViewById(R.id.address) != null) {
			final String addressBlock = place.getAddressBlock();
			if (!addressBlock.equals("")) {
				((BuilderButton) findViewById(R.id.address)).setText(place.address);
			}
		}
		if (place.category != null) {
			if (findViewById(R.id.category) != null) {
				((BuilderButton) findViewById(R.id.category)).setText(place.category.name);
			}
		}
		super.draw();
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onAddressBuilderClick(View view) {
		Routing.route(this, Route.ADDRESS_EDIT, mEntity);
	}

	@SuppressWarnings("ucd")
	public void onCategoryBuilderClick(View view) {
		Routing.route(this, Route.CATEGORY_EDIT, mEntity);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_ADDRESS_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					mDirty = true;
					final Bundle extras = intent.getExtras();

					final String jsonPlace = extras.getString(Constants.EXTRA_PLACE);
					if (jsonPlace != null) {
						final Place placeUpdated = (Place) Json.jsonToObject(jsonPlace, Json.ObjectType.PLACE);
						if (placeUpdated.phone != null) {
							placeUpdated.phone = placeUpdated.phone.replaceAll("[^\\d.]", "");
						}
						mEntity = placeUpdated;
						((BuilderButton) findViewById(R.id.address)).setText(((Place) mEntity).address);
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_CATEGORY_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String jsonCategory = extras.getString(Constants.EXTRA_CATEGORY);
					if (jsonCategory != null) {
						final Category categoryUpdated = (Category) Json.jsonToObject(jsonCategory, Json.ObjectType.CATEGORY);
						if (categoryUpdated != null) {
							mDirty = true;
							((Place) mEntity).category = categoryUpdated;
							((BuilderButton) findViewById(R.id.category)).setText(categoryUpdated.name);
							drawPhoto();
						}
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_APPLINKS_EDIT) {

				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final List<String> jsonApplinks = extras.getStringArrayList(Constants.EXTRA_ENTITIES);
					mApplinks.clear();
					for (String jsonApplink : jsonApplinks) {
						Applink applink = (Applink) Json.jsonToObject(jsonApplink, Json.ObjectType.APPLINK);
						mApplinks.add(applink);
					}
					mDirty = true;
					drawShortcuts(mEntity);
				}
			}
			else {
				super.onActivityResult(requestCode, resultCode, intent);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	protected boolean validate() {
		if (!super.validate()) {
			return false;
		}
		/*
		 * Transfering values from the controls to the entity is easier
		 * with candigrams.
		 */
		gather();
		Place place = (Place) mEntity;
		if (place.name == null) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_place_name)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		return true;
	}

	@Override
	protected void gather() {
		super.gather();
		if (!mEditing) {
			((Place) mEntity).provider.aircandi = Aircandi.getInstance().getCurrentUser().id;
			/*
			 * Custom places get the current location.
			 * 
			 * Upsized places inherited a location from the place authority.
			 */
			final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
			if (location != null) {
				if (mEntity.location == null) {
					mEntity.location = new AirLocation();
				}
				mEntity.location.lat = location.lat;
				mEntity.location.lng = location.lng;
			}
		}
	}

	@Override
	protected String getLinkType() {
		return Constants.TYPE_LINK_PROXIMITY;
	};

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.place_edit;
	}
}