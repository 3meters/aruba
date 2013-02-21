package com.aircandi.ui.builders;

import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.FontManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.WebImageView;

public class CategoryBuilder extends FormActivity {

	private WebImageView	mImage;
	private Spinner			mSpinnerCategory;
	private Spinner			mSpinnerSubCategory;
	private Spinner			mSpinnerSubSubCategory;
	private TextView		mTitle;
	private Integer			mSpinnerItem;

	private Category		mOriginalCategory;
	private Integer			mOriginalCategoryIndex;
	private Integer			mOriginalSubCategoryIndex;
	private Integer			mOriginalSubSubCategoryIndex;

	private Category		mCategory;
	private Category		mSubCategory;
	private Category		mSubSubCategory;

	private List<Category>	mCategories;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initialize();
	}

	private void initialize() {
		Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			String jsonCategory = extras.getString(CandiConstants.EXTRA_CATEGORY);
			if (jsonCategory != null) {
				mOriginalCategory = (Category) ProxibaseService.convertJsonToObjectInternalSmart(jsonCategory, ServiceDataType.Category);
			}
		}
		mImage = (WebImageView) findViewById(R.id.image);
		mSpinnerCategory = (Spinner) findViewById(R.id.category);
		mSpinnerSubCategory = (Spinner) findViewById(R.id.sub_category);
		mSpinnerSubSubCategory = (Spinner) findViewById(R.id.sub_sub_category);
		mTitle = (TextView) findViewById(R.id.title);
		mTitle.setText(R.string.dialog_category_builder_title);

		mSpinnerItem = mCommon.mThemeTone.equals("dark") ? R.layout.spinner_item_dark : R.layout.spinner_item_light;

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.title));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_save));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_cancel));

		if (ProxiManager.getInstance().getEntityModel().getCategories().size() == 0) {
			loadCategories();
		}
		else {
			mCategories = ProxiManager.getInstance().getEntityModel().getCategories();
			if (mCategories != null) {
				if (mOriginalCategory != null) {
					setCategoryIndexes();
				}
				initCategorySpinner();
			}
		}
	}

	private void loadCategories() {
		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("LoadCategories");
				ModelResult result = ProxiManager.getInstance().getEntityModel().loadCategories();
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					mCategories = ProxiManager.getInstance().getEntityModel().getCategories();
					if (mCategories != null) {
						if (mOriginalCategory != null) {
							setCategoryIndexes();
						}
						initCategorySpinner();
					}
				}
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onSaveButtonClick(View view) {
		gather();
		doSave();
	}

	private void gather() {}

	@Override
	protected Boolean isDialog() {
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void doSave() {
		Intent intent = new Intent();
		if (mSubSubCategory != null) {
			Category category = new Category();
			category.id = mSubSubCategory.id;
			category.name = mSubSubCategory.name;
			category.icon = mSubSubCategory.iconUri();
			String jsonCategory = ProxibaseService.convertObjectToJsonSmart(category, false, true);
			intent.putExtra(CandiConstants.EXTRA_CATEGORY, jsonCategory);
		}
		else if (mSubCategory != null) {
			Category category = new Category();
			category.id = mSubCategory.id;
			category.name = mSubCategory.name;
			category.icon = mSubCategory.iconUri();
			String jsonCategory = ProxibaseService.convertObjectToJsonSmart(category, false, true);
			intent.putExtra(CandiConstants.EXTRA_CATEGORY, jsonCategory);
		}
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	private void setCategoryIndexes() {
		int categoryIndex = 0;
		for (Category category : mCategories) {
			if (category.id.equals(mOriginalCategory.id)) {
				mOriginalCategoryIndex = categoryIndex;
				return;
			}
			else {
				int subcategoryIndex = 0;
				for (Category subcategory : category.categories) {
					if (subcategory.id.equals(mOriginalCategory.id)) {
						mOriginalCategoryIndex = categoryIndex;
						mOriginalSubCategoryIndex = subcategoryIndex;
						return;
					}
					else if (subcategory.categories != null && subcategory.categories.size() > 0) {
						int subsubcategoryIndex = 0;
						for (Category subsubcategory : subcategory.categories) {
							if (subsubcategory.id.equals(mOriginalCategory.id)) {
								mOriginalCategoryIndex = categoryIndex;
								mOriginalSubCategoryIndex = subcategoryIndex;
								mOriginalSubSubCategoryIndex = subsubcategoryIndex;
								return;
							}
							subsubcategoryIndex++;
						}
					}
					subcategoryIndex++;
				}
			}
			categoryIndex++;
		}
	}

	private void initCategorySpinner() {

		List<String> categories = ProxiManager.getInstance().getEntityModel().getCategoriesAsStringArray(mCategories);
		CategoryAdapter adapter = new CategoryAdapter(CategoryBuilder.this
				, mSpinnerItem
				, categories
				, R.string.form_place_category_hint);

		if (mCommon.mThemeTone.equals("dark")) {
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
				adapter.setDropDownViewResource(R.layout.spinner_item_light);
			}
		}

		mSpinnerCategory.setAdapter(adapter);

		if (mOriginalCategory == null) {
			mSpinnerCategory.setSelection(adapter.getCount());
		}

		mSpinnerCategory.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				if (position < mCategories.size()) {

					mCategory = (Category) mCategories.get(position);
					if (mCategory.iconUri() != null) {
						updateCustomImage(mCategory.iconUri(), mCategory);
					}

					mSubCategory = null;
					mSubSubCategory = null;
					mSpinnerSubCategory.setVisibility(View.INVISIBLE);
					mSpinnerSubSubCategory.setVisibility(View.INVISIBLE);

					initSubcategorySpinner(position);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		if (mOriginalCategory != null && mOriginalCategoryIndex != null) {
			mSpinnerCategory.setSelection(mOriginalCategoryIndex);
			if (mOriginalSubCategoryIndex == null) {
				mOriginalCategory = null;
			}
		}
	}

	private void initSubcategorySpinner(Integer position) {

		final List<String> categories = ProxiManager.getInstance().getEntityModel().getCategoriesAsStringArray(mCategory.categories);

		if (categories.size() > 0) {

			final CategoryAdapter adapter = new CategoryAdapter(CategoryBuilder.this
					, mSpinnerItem
					, categories
					, R.string.form_place_sub_category_hint);

			if (mCommon.mThemeTone.equals("dark")) {
				if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
					adapter.setDropDownViewResource(R.layout.spinner_item_light);
				}
			}

			mSpinnerSubCategory.setVisibility(View.VISIBLE);
			mSpinnerSubCategory.setAdapter(adapter);

			if (mOriginalCategory == null) {
				mSpinnerSubCategory.setSelection(adapter.getCount());
			}

			mSpinnerSubCategory.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_UP) {
						if (mSpinnerSubCategory.getSelectedItemPosition() == adapter.getCount()) {
							mSpinnerSubCategory.setSelection(0);
						}
					}
					return false;
				}
			});

			mSpinnerSubCategory.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

					/* Do nothing when the hint item is selected */
					if (position < mCategory.categories.size()) {
						mSubCategory = (Category) mCategory.categories.get(position);
						if (mSubCategory.iconUri() != null) {
							updateCustomImage(mSubCategory.iconUri(), mSubCategory);
						}

						mSubSubCategory = null;
						mSpinnerSubSubCategory.setVisibility(View.INVISIBLE);

						initSubsubcategorySpinner(position);
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {}
			});

			if (mOriginalCategory != null && mOriginalSubCategoryIndex != null) {
				mSpinnerSubCategory.setSelection(mOriginalSubCategoryIndex);
				if (mOriginalSubSubCategoryIndex == null) {
					mOriginalCategory = null;
				}
			}
		}
	}

	private void initSubsubcategorySpinner(Integer position) {

		List<String> categories = ProxiManager.getInstance().getEntityModel().getCategoriesAsStringArray(mSubCategory.categories);
		if (categories.size() > 0) {

			final CategoryAdapter adapter = new CategoryAdapter(CategoryBuilder.this
					, mSpinnerItem
					, categories
					, R.string.form_place_sub_category_hint);

			if (mCommon.mThemeTone.equals("dark")) {
				if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
					adapter.setDropDownViewResource(R.layout.spinner_item_light);
				}
			}

			mSpinnerSubSubCategory.setVisibility(View.VISIBLE);
			mSpinnerSubSubCategory.setAdapter(adapter);

			if (mOriginalCategory == null) {
				mSpinnerSubSubCategory.setSelection(adapter.getCount());
			}

			mSpinnerSubSubCategory.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_UP) {
						if (mSpinnerSubSubCategory.getSelectedItemPosition() == adapter.getCount()) {
							mSpinnerSubSubCategory.setSelection(0);
						}
					}
					return false;
				}
			});

			mSpinnerSubSubCategory.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

					/* Do nothing when the hint item is selected */
					if (position < mSubCategory.categories.size()) {
						mSubSubCategory = (Category) mSubCategory.categories.get(position);
						if (mSubSubCategory.iconUri() != null) {
							updateCustomImage(mSubSubCategory.iconUri(), mSubSubCategory);
						}
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {}
			});

			if (mOriginalCategory != null && mOriginalSubSubCategoryIndex != null) {
				mSpinnerSubSubCategory.setSelection(mOriginalSubSubCategoryIndex);
				mOriginalCategory = null;
			}
		}
	}

	private void updateCustomImage(String uri, Category category) {

		Boolean boostColor = !android.os.Build.MODEL.toLowerCase(Locale.US).equals("nexus 4");
		int color = Place.getCategoryColor(category.name, true, boostColor, false);
		mImage.getImageView().setColorFilter(color, PorterDuff.Mode.MULTIPLY);

		int colorResId = Place.getCategoryColorResId(category.name, true, boostColor, false);
		if (findViewById(R.id.color_layer) != null) {
			((View) findViewById(R.id.color_layer)).setBackgroundResource(colorResId);
			((View) findViewById(R.id.color_layer)).setVisibility(View.VISIBLE);
			((View) findViewById(R.id.reverse_layer)).setVisibility(View.VISIBLE);
		}
		else {
			mImage.getImageView().setBackgroundResource(colorResId);
		}

		mImage.getImageView().setTag(uri);
		BitmapRequest bitmapRequest = new BitmapRequest(uri, mImage.getImageView());
		bitmapRequest.setBrokenDrawableResId(R.drawable.ic_app);
		bitmapRequest.setImageSize(mImage.getSizeHint());
		bitmapRequest.setImageRequestor(mImage.getImageView());
		BitmapManager.getInstance().masterFetch(bitmapRequest);
	}

	private class CategoryAdapter extends ArrayAdapter {

		private List<String>	mCategories;

		public CategoryAdapter(Context context, int textViewResourceId, List categories, Integer categoryHint) {
			super(context, textViewResourceId, categories);
			categories.add(getString(categoryHint));
			mCategories = categories;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view = super.getView(position, convertView, parent);

			TextView text = (TextView) view.findViewById(R.id.spinner_name);
			if (mCommon.mThemeTone.equals("dark")) {
				if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
					text.setTextColor(Aircandi.getInstance().getResources().getColor(R.color.text_dark));
				}
			}

			FontManager.getInstance().setTypefaceDefault(text);

			if (position == getCount()) {
				text.setText("");
				text.setHint(mCategories.get(getCount())); //"Hint to be displayed"
			}

			return view;
		}

		@Override
		public int getCount() {
			return super.getCount() - 1; // you dont display last item. It is used as hint.
		}
	}

	@Override
	protected int getLayoutID() {
		return R.layout.builder_category;
	}
}