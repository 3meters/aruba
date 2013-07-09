package com.aircandi.ui.edit;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ViewFlipper;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.LocationManager;
import com.aircandi.components.TabManager;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ObjectType;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.helpers.AddressBuilder;
import com.aircandi.ui.helpers.CategoryBuilder;
import com.aircandi.ui.widgets.BuilderButton;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;

public class PlaceEdit extends BaseEntityEdit {

	private TabManager	mTabManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		if (mEntity != null) {
			if (mEntity.ownerId != null && (mEntity.ownerId.equals(Aircandi.getInstance().getUser().id))) {
				mTabManager = new TabManager(Constants.TABS_ENTITY_FORM_ID, mActionBar, (ViewFlipper) findViewById(R.id.flipper_form));
				mTabManager.initialize();
				mTabManager.doRestoreInstanceState(savedInstanceState);
			}
		}
	}

	@Override
	protected void draw() {
		super.draw();

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

	}

	@Override
	protected void drawPhoto() {
		super.drawPhoto();
		/*
		 * Special color layering if we are using the category photo.
		 */
		Place place = (Place) mEntity;
		if (place.photo == null && place.category != null) {

			final int color = Place.getCategoryColor((place.category != null)
					? place.category.name
					: null, true, mMuteColor, false);

			mPhoto.getImageView().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
			Integer colorResId = Place.getCategoryColorResId(place.category != null ? place.category.name : null,
					true, mMuteColor, false);

			if (findViewById(R.id.color_layer) != null) {
				(findViewById(R.id.color_layer)).setBackgroundResource(colorResId);
				(findViewById(R.id.color_layer)).setVisibility(View.VISIBLE);
				(findViewById(R.id.reverse_layer)).setVisibility(View.VISIBLE);
			}
			else {
				mPhoto.getImageView().setBackgroundResource(colorResId);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onAddressBuilderClick(View view) {
		final Intent intent = new Intent(this, AddressBuilder.class);
		final String jsonPlace = HttpService.objectToJson(mEntity);
		intent.putExtra(Constants.EXTRA_PLACE, jsonPlace);
		startActivityForResult(intent, Constants.ACTIVITY_ADDRESS_EDIT);
		Animate.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@SuppressWarnings("ucd")
	public void onCategoryBuilderClick(View view) {
		final Intent intent = new Intent(this, CategoryBuilder.class);
		if (((Place) mEntity).category != null) {
			final String jsonCategory = HttpService.objectToJson(((Place) mEntity).category);
			intent.putExtra(Constants.EXTRA_CATEGORY, jsonCategory);
		}
		startActivityForResult(intent, Constants.ACTIVITY_CATEGORY_EDIT);
		Animate.doOverridePendingTransition(this, TransitionType.PageToForm);
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
						final Place placeUpdated = (Place) HttpService.jsonToObject(jsonPlace, ObjectType.Place);
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
						final Category categoryUpdated = (Category) HttpService.jsonToObject(jsonCategory, ObjectType.Category);
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
						Applink applink = (Applink) HttpService.jsonToObject(jsonApplink, ObjectType.Applink);
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
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void beforeInsert(Entity entity) {
		((Place) mEntity).provider.aircandi = Aircandi.getInstance().getUser().id;
		/*
		 * We add location info as a consistent feature
		 */
		final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
		if (location != null) {
			((Place) mEntity).location.lat = location.lat;
			((Place) mEntity).location.lng = location.lng;
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.place_edit;
	}
}