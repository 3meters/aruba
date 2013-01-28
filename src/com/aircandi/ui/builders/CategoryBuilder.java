package com.aircandi.ui.builders;

import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.FontManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.CategorySimple;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.WebImageView;

public class CategoryBuilder extends FormActivity {

	private CategorySimple	mOriginalCategory;

	private WebImageView	mImage;
	private Spinner			mSpinnerCategory;
	private Spinner			mSpinnerSubcategory;
	private Spinner			mSpinnerSubsubcategory;
	private TextView		mTitle;

	private Category		mCategory;
	private Integer			mCategoryIndex;
	private Category		mSubcategory;
	private Integer			mSubcategoryIndex;
	private Category		mSubsubcategory;
	private Integer			mSubsubcategoryIndex;

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
				mOriginalCategory = (CategorySimple) ProxibaseService.convertJsonToObjectInternalSmart(jsonCategory, ServiceDataType.CategorySimple);
			}
		}
		mImage = (WebImageView) findViewById(R.id.image);
		mSpinnerCategory = (Spinner) findViewById(R.id.category);
		mSpinnerSubcategory = (Spinner) findViewById(R.id.sub_category);
		mSpinnerSubsubcategory = (Spinner) findViewById(R.id.sub_sub_category);
		mTitle = (TextView) findViewById(R.id.title);
		mTitle.setText(R.string.dialog_category_builder_title);

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.title));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_save));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_cancel));

		if (ProxiExplorer.getInstance().getEntityModel().getCategories().size() == 0) {
			loadCategories();
		}
		else {
			mCategories = ProxiExplorer.getInstance().getEntityModel().getCategories();
			if (mCategories != null) {
				if (mOriginalCategory != null) {
					setCategoryIndexes();
				}
				initializeSpinners();
			}
		}
	}

	private void loadCategories() {
		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("LoadCategories");
				ModelResult result = ProxiExplorer.getInstance().getEntityModel().loadCategories();
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					mCategories = ProxiExplorer.getInstance().getEntityModel().getCategories();
					if (mCategories != null) {
						if (mOriginalCategory != null) {
							setCategoryIndexes();
						}
						initializeSpinners();
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
		if (mSubsubcategory != null) {
			CategorySimple categorySimple = new CategorySimple();
			categorySimple.id = mSubsubcategory.id;
			categorySimple.name = mSubsubcategory.name;
			categorySimple.icon = mSubsubcategory.iconUri();
			String jsonCategory = ProxibaseService.convertObjectToJsonSmart(categorySimple, false, true);
			intent.putExtra(CandiConstants.EXTRA_CATEGORY, jsonCategory);
		}
		else if (mSubcategory != null) {
			CategorySimple categorySimple = new CategorySimple();
			categorySimple.id = mSubcategory.id;
			categorySimple.name = mSubcategory.name;
			categorySimple.icon = mSubcategory.iconUri();
			String jsonCategory = ProxibaseService.convertObjectToJsonSmart(categorySimple, false, true);
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
			int subcategoryIndex = 0;
			for (Category subcategory : category.categories) {
				if (subcategory.id.equals(mOriginalCategory.id)) {
					mCategoryIndex = categoryIndex;
					mSubcategoryIndex = subcategoryIndex;
					return;
				}
				else if (subcategory.categories != null && subcategory.categories.size() > 0) {
					int subsubcategoryIndex = 0;
					for (Category subsubcategory : subcategory.categories) {
						if (subsubcategory.id.equals(mOriginalCategory.id)) {
							mCategoryIndex = categoryIndex;
							mSubcategoryIndex = subcategoryIndex;
							mSubsubcategoryIndex = subsubcategoryIndex;
							return;
						}
						subsubcategoryIndex++;
					}
				}
				subcategoryIndex++;
			}
			categoryIndex++;
		}
	}

	private void initializeSpinners() {
		final List<String> categories = ProxiExplorer.getInstance().getEntityModel().getCategoriesAsStringArray(mCategories);
		categories.add(getString(R.string.form_place_category_hint));

		ArrayAdapter adapter = new ArrayAdapter(this, R.layout.spinner_item, categories) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {

				View view = super.getView(position, convertView, parent);

				FontManager.getInstance().setTypefaceDefault((TextView) view.findViewById(R.id.spinner_name));

				if (position == getCount()) {
					((TextView) view.findViewById(R.id.spinner_name)).setText("");
					((TextView) view.findViewById(R.id.spinner_name)).setHint(categories.get(getCount())); //"Hint to be displayed"
				}

				return view;
			}

			@Override
			public int getCount() {
				return super.getCount() - 1; // you dont display last item. It is used as hint.
			}
		};

		//adapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
		mSpinnerCategory.setAdapter(adapter);

		if (mOriginalCategory == null) {
			mSpinnerCategory.setSelection(adapter.getCount());
		}

		mSpinnerCategory.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				/* Do nothing when the hint item is selected */
				if (position < mCategories.size()) {

					mCategory = (Category) mCategories.get(position);
					if (mCategory.iconUri() != null) {
						updateCustomImage(mCategory.iconUri(), mCategory);
					}
					final List<String> categories = ProxiExplorer.getInstance().getEntityModel().getCategoriesAsStringArray(mCategory.categories);
					categories.add(getString(R.string.form_place_sub_category_hint));

					ArrayAdapter adapter = new ArrayAdapter(CategoryBuilder.this, R.layout.spinner_item, categories) {
						@Override
						public View getView(int position, View convertView, ViewGroup parent) {

							View view = super.getView(position, convertView, parent);

							FontManager.getInstance().setTypefaceDefault((TextView) view.findViewById(R.id.spinner_name));

							if (position == getCount()) {
								((TextView) view.findViewById(R.id.spinner_name)).setText("");
								((TextView) view.findViewById(R.id.spinner_name)).setHint(categories.get(getCount())); //"Hint to be displayed"
							}

							return view;
						}

						@Override
						public int getCount() {
							return super.getCount() - 1; // you dont display last item. It is used as hint.
						}
					};

					mSpinnerSubcategory.setVisibility(View.VISIBLE);
					mSpinnerSubcategory.setAdapter(adapter);

					if (mOriginalCategory == null) {
						mSpinnerSubcategory.setSelection(adapter.getCount());
					}
					mSpinnerSubcategory.setOnItemSelectedListener(new OnItemSelectedListener() {

						@Override
						public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

							/* Do nothing when the hint item is selected */
							if (position < mCategory.categories.size()) {
								mSubcategory = (Category) mCategory.categories.get(position);
								if (mSubcategory.iconUri() != null) {
									updateCustomImage(mSubcategory.iconUri(), mSubcategory);
								}

								final List<String> categories = ProxiExplorer.getInstance().getEntityModel().getCategoriesAsStringArray(mSubcategory.categories);
								categories.add(getString(R.string.form_place_sub_category_hint));

								ArrayAdapter adapter = new ArrayAdapter(CategoryBuilder.this, R.layout.spinner_item, categories) {
									@Override
									public View getView(int position, View convertView, ViewGroup parent) {

										View view = super.getView(position, convertView, parent);

										FontManager.getInstance().setTypefaceDefault((TextView) view.findViewById(R.id.spinner_name));

										if (position == getCount()) {
											((TextView) view.findViewById(R.id.spinner_name)).setText("");
											((TextView) view.findViewById(R.id.spinner_name)).setHint(categories.get(getCount())); //"Hint to be displayed"
										}

										return view;
									}

									@Override
									public int getCount() {
										return super.getCount() - 1; // you dont display last item. It is used as hint.
									}
								};

								mSpinnerSubsubcategory.setVisibility(View.VISIBLE);
								mSpinnerSubsubcategory.setAdapter(adapter);

								if (mOriginalCategory == null) {
									mSpinnerSubsubcategory.setSelection(adapter.getCount());
								}
								mSpinnerSubsubcategory.setOnItemSelectedListener(new OnItemSelectedListener() {

									@Override
									public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

										/* Do nothing when the hint item is selected */
										if (position < mSubcategory.categories.size()) {
											mSubsubcategory = (Category) mSubcategory.categories.get(position);
											if (mSubsubcategory.iconUri() != null) {
												updateCustomImage(mSubsubcategory.iconUri(), mSubsubcategory);
											}
										}
									}

									@Override
									public void onNothingSelected(AdapterView<?> parent) {}
								});
								
								if (mOriginalCategory != null && mSubsubcategoryIndex != null) {
									mSpinnerSubsubcategory.setSelection(mSubsubcategoryIndex);
									mOriginalCategory = null;
								}
							}
						}

						@Override
						public void onNothingSelected(AdapterView<?> parent) {}
					});
					
					if (mOriginalCategory != null && mSubcategoryIndex != null) {
						mSpinnerSubcategory.setSelection(mSubcategoryIndex);
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		if (mOriginalCategory != null && mCategoryIndex != null) {
			mSpinnerCategory.setSelection(mCategoryIndex);
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

	@Override
	protected int getLayoutID() {
		return R.layout.builder_category;
	}
}