package com.aircandi;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AnimUtils;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.CandiListAdapter;
import com.aircandi.components.EntityList;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Entity.ImageFormat;
import com.aircandi.service.objects.Place;
import com.aircandi.widgets.WebImageView;

public class CandiTuner extends FormActivity {

	private ListView			mListViewCandi;

	private ViewGroup			mCustomDialog;
	private WebImageView		mCustomImage;
	private Spinner				mCustomCategory;
	private Spinner				mCustomSubCategory;
	private EditText			mCustomTitle;

	private Category			mCategory;
	private Category			mSubCategory;

	private Bitmap				mEntityBitmap;
	private String				mImageUri;
	private String				mLinkUri;
	private Boolean				mImageSelected	= false;
	private List<Category>		mCategories;

	private EntityList<Entity>	mEntities		= new EntityList<Entity>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
		bind();
	}

	private void initialize() {
		mListViewCandi = (ListView) findViewById(R.id.list);

		mCustomDialog = (ViewGroup) findViewById(R.id.custom_form);
		mCustomImage = (WebImageView) findViewById(R.id.image);
		mCustomTitle = (EditText) findViewById(R.id.title);
		mCustomCategory = (Spinner) findViewById(R.id.category);
		mCustomSubCategory = (Spinner) findViewById(R.id.sub_category);

		if (ProxiExplorer.getInstance().getEntityModel().getCategories().size() == 0) {
			loadCategories();
		}
	}

	public void bind() {

		EntityList<Entity> entities = ProxiExplorer.getInstance().getEntityModel().getRadarPlaces();
		if (entities != null) {
			entities.addAll(ProxiExplorer.getInstance().getEntityModel().getRadarSynthetics());
		}
		
		if (entities != null) {
			mEntities.clear();
			for (Entity entity : entities) {
				if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
					mEntities.add(entity);
				}
			}
		}
		CandiListAdapter adapter = new CandiListAdapter(CandiTuner.this, mEntities, R.layout.temp_listitem_candi_tuner);
		mListViewCandi.setAdapter(adapter);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onTuneButtonClick(View view) {
		tune();
	}

	public void onCustomButtonClick(View view) {
		if (mCategories == null) {
			initializeCustomForm();
		}
		openCustomForm();
	}

	public void onCancelCustomButtonClick(View view) {
		closeCustomForm();
	}

	public void onListItemClick(View view) {
		CheckBox check = (CheckBox) view.findViewById(R.id.check);
		check.setChecked(!check.isChecked());
		Entity entity = (Entity) check.getTag();
		entity.checked = check.isChecked();
	}

	public void onAddCustomButtonClick(View view) {
		if (validateCustomForm()) {
			closeCustomForm();
			Entity entity = createEntityFromCustomForm();
			if (entity != null) {
				mEntities.add(0, entity);
				((CandiListAdapter) mListViewCandi.getAdapter()).notifyDataSetChanged();
			}
			resetCustomForm();
		}
	}

	public void onChangePictureButtonClick(View view) {
		String defaultUri = mSubCategory != null ? mSubCategory.iconUri() : mCategory != null ? mCategory.iconUri() : null;
		String defaultSearch = mSubCategory != null ? mSubCategory.name : mCategory != null ? mCategory.name : null;
		showChangePictureDialog(false, false, defaultUri, defaultSearch, null, mCustomImage, new RequestListener() {

			@Override
			public void onComplete(Object response, String imageUri, String linkUri, Bitmap imageBitmap, String title, String description) {
				/*
				 * Search: bitmap sized for preview, imageUri for full size.
				 * None: bitmap=null, imageUri=resource:placeholder_logo
				 * Gallery: bitmap full sized, everything else is null
				 * Camera: bitmap full sized, everything else is null
				 * Website: bitmap sized for preview, linkUri for site, title for site
				 */

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					/*
					 * If we get back a bitmap and an imageUri we want to just store a preview.
					 * otherwise if the bitmap is wider that our default, we want to store
					 * both preview and native versions.
					 */
					mImageSelected = (imageBitmap != null);

					mEntityBitmap = imageBitmap;
					mImageUri = imageUri;
					mLinkUri = linkUri;
				}
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Core routines
	// --------------------------------------------------------------------------------------------

	public void tune() {

		final List<Beacon> beacons = ProxiExplorer.getInstance().getStrongestWifiAsBeacons(5);
		final Beacon primaryBeacon = beacons.size() > 0 ? beacons.get(0) : null;

		new AsyncTask<Object, Object, Object>() {

			@Override
			protected void onPreExecute() {
				setSupportProgressBarIndeterminateVisibility(true);
			}

			@Override
			protected Object doInBackground(Object... params) {

				List<Entity> entities = new ArrayList<Entity>();
				for (Entity entity : mEntities) {
					if (entity.checked) {
						entities.add(entity);
					}
				}
				ServiceResponse serviceResponse = ProxiExplorer.getInstance().tune(beacons, primaryBeacon, entities);
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				ServiceResponse serviceResponse = (ServiceResponse) response;
				setSupportProgressBarIndeterminateVisibility(false);
				if (serviceResponse.responseCode != ResponseCode.Success) {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.Tuning);
				}
				else {
					finish();
					AnimUtils.doOverridePendingTransition(CandiTuner.this, TransitionType.FormToCandiPage);
				}
			}
		}.execute();
	}

	private Entity createEntityFromCustomForm() {

		Entity entity = new Entity();
		entity.name = mCustomTitle.getEditableText().toString();
		entity.synthetic = true;
		entity.checked = true;
		entity.isCollection = true;
		entity.locked = false;
		entity.type = CandiConstants.TYPE_CANDI_PLACE;

		entity.getPhoto().setSourceName("aircandi");
		if (mImageSelected) {
			if (mLinkUri != null) {
				entity.getPhoto().setImageUri(mLinkUri);
				entity.getPhoto().setImageFormat(ImageFormat.Html);
			}
			else {
				if (mImageUri != null) {
					entity.getPhoto().setImageUri(mImageUri);
				}
			}
			entity.getPhoto().setBitmap(mEntityBitmap);
		}
		else {
			entity.getPhoto().setImageUri(mSubCategory.iconUri());
		}

		entity.place = new Place();
		entity.place.source = "user";
		entity.place.sourceId = Aircandi.getInstance().getUser().id;
		entity.place.categories = new ArrayList<Category>();
		mSubCategory.primary = true;
		entity.place.categories.add(mSubCategory);
		entity.subtitle = mSubCategory.name;

		return entity;
	}

	private Boolean validateCustomForm() {
		if (mCustomTitle.getEditableText().toString() == null || mCustomTitle.getEditableText().toString().equals("")) {
			mCommon.showAlertDialogSimple(null, getString(R.string.error_title_missing));
			return false;
		}
		if (mCustomCategory.getSelectedItemPosition() == mCustomCategory.getAdapter().getCount()) {
			mCommon.showAlertDialogSimple(null, getString(R.string.error_category_missing));
			return false;
		}
		if (mCustomSubCategory.getSelectedItemPosition() == mCustomSubCategory.getAdapter().getCount()) {
			mCommon.showAlertDialogSimple(null, getString(R.string.error_sub_category_missing));
			return false;
		}
		return true;

	}

	private void resetCustomForm() {
		mCustomCategory.setSelection(mCustomCategory.getAdapter().getCount());
		mCustomSubCategory.setVisibility(View.INVISIBLE);
		mCustomTitle.setText(null);
		mCustomImage.getImageView().setImageResource(R.drawable.placeholder_logo_bw);
	}

	private void initializeCustomForm() {
		mCategories = ProxiExplorer.getInstance().getEntityModel().getCategories();
		final List<String> categories = ProxiExplorer.getInstance().getEntityModel().getCategoriesAsStrings(mCategories);
		categories.add(getString(R.string.form_place_category_hint));

		ArrayAdapter adapter = new ArrayAdapter(CandiTuner.this, R.layout.sherlock_spinner_item, categories) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {

				View view = super.getView(position, convertView, parent);

				if (position == getCount()) {
					((TextView) view.findViewById(android.R.id.text1)).setText("");
					((TextView) view.findViewById(android.R.id.text1)).setHint(categories.get(getCount())); //"Hint to be displayed"
				}

				return view;
			}

			@Override
			public int getCount() {
				return super.getCount() - 1; // you dont display last item. It is used as hint.
			}
		};
		adapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
		mCustomCategory.setAdapter(adapter);
		mCustomCategory.setSelection(adapter.getCount());
		mCustomCategory.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				/* Do nothing when the hint item is selected */
				if (position < mCategories.size()) {

					mCategory = (Category) mCategories.get(position);
					if (mCategory.iconUri() != null) {
						updateCustomImage(mCategory.iconUri());
					}
					final List<String> categories = ProxiExplorer.getInstance().getEntityModel().getCategoriesAsStrings(mCategory.categories);
					categories.add(getString(R.string.form_place_sub_category_hint));

					ArrayAdapter adapter = new ArrayAdapter(CandiTuner.this, R.layout.sherlock_spinner_item, categories) {
						@Override
						public View getView(int position, View convertView, ViewGroup parent) {

							View view = super.getView(position, convertView, parent);

							if (position == getCount()) {
								((TextView) view.findViewById(android.R.id.text1)).setText("");
								((TextView) view.findViewById(android.R.id.text1)).setHint(categories.get(getCount())); //"Hint to be displayed"
							}

							return view;
						}

						@Override
						public int getCount() {
							return super.getCount() - 1; // you dont display last item. It is used as hint.
						}
					};

					adapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);

					mCustomSubCategory.setVisibility(View.VISIBLE);
					mCustomSubCategory.setAdapter(adapter);
					mCustomSubCategory.setSelection(adapter.getCount());
					mCustomSubCategory.setOnItemSelectedListener(new OnItemSelectedListener() {

						@Override
						public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
							/* Do nothing when the hint item is selected */
							if (position < mCategory.categories.size()) {
								mSubCategory = (Category) mCategory.categories.get(position);
								if (mSubCategory.iconUri() != null) {
									updateCustomImage(mSubCategory.iconUri());
								}
							}
						}

						@Override
						public void onNothingSelected(AdapterView<?> parent) {}
					});
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
	}

	private void openCustomForm() {
		Animation animation = AnimationUtils.loadAnimation(this, R.anim.activity_open_enter);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				mCustomDialog.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onAnimationEnd(Animation animation) {}

			@Override
			public void onAnimationRepeat(Animation animation) {}
		});
		mCustomDialog.startAnimation(animation);
	}

	private void closeCustomForm() {
		Animation animation = AnimationUtils.loadAnimation(this, R.anim.activity_close_exit);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {}

			@Override
			public void onAnimationEnd(Animation animation) {
				mCustomDialog.setVisibility(View.GONE);
				mCustomDialog.setAnimation(null);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {}
		});

		mCustomDialog.startAnimation(animation);
	}

	public void loadCategories() {
		new AsyncTask<Object, Object, Object>() {

			@Override
			protected Object doInBackground(Object... params) {
				ModelResult result = ProxiExplorer.getInstance().getEntityModel().loadCategories();
				return result;
			}

		}.execute();
	}

	public void updateCustomImage(String uri) {
		if (!mImageSelected) {
			ImageRequestBuilder builder = new ImageRequestBuilder(mCustomImage)
					.setImageUri(uri)
					.setImageFormat(ImageFormat.Binary);

			final ImageRequest imageRequest = builder.create();
			mCustomImage.setImageRequest(imageRequest);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
		for (Entity entity : mEntities) {
			if (entity.photo != null && entity.photo.getBitmap() != null && !entity.photo.getBitmap().isRecycled()) {
				entity.photo.getBitmap().recycle();
				entity.photo.setBitmap(null);
			}
		}
		System.gc();
	}

	@Override
	protected int getLayoutID() {
		return R.layout.candi_tuner;
	}

}