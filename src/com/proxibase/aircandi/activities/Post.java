package com.proxibase.aircandi.activities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.aircandi.models.PostEntity;
import com.proxibase.aircandi.models.BaseEntity.SubType;
import com.proxibase.aircandi.utils.AircandiUI;
import com.proxibase.aircandi.utils.DateUtils;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.S3;
import com.proxibase.aircandi.utils.ImageManager.IImageReadyListener;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.sdk.android.proxi.consumer.Beacon.BeaconType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.SimpleModifyListener;
import com.proxibase.sdk.android.util.ProxiConstants;
import com.proxibase.sdk.android.util.Utilities;

public class Post extends AircandiActivity {

	private PostEntity	mPostEntity;
	private Uri			mPhotoDeviceUri;
	private String		mPhotoServiceUri;
	private boolean		mPhotoChanged			= false;
	private Bitmap		mPhotoThumbnailBitmap	= null;
	private boolean		mProcessing				= false;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		super.onCreate(savedInstanceState);

		bindEntity();
		bindLayout();
	}

	public void onSaveButtonClick(View view) {
		if (!mProcessing) {
			mProcessing = true;
			startTitlebarProgress();
			doSave();
		}
	}

	public void onDeleteButtonClick(View view) {
		if (!mProcessing) {
			mProcessing = true;
			startTitlebarProgress();
			doDelete();
		}
	}

	public void onCancelButtonClick(View view) {
		if (!mProcessing) {
			mProcessing = true;
			startTitlebarProgress();
			setResult(Activity.RESULT_CANCELED);
			finish();
			overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
		}
	}

	public void onAddPhotoButtonClick(View view) {
		if (!mProcessing) {
			mProcessing = true;
			showAddPhotoDialog();
		}
	}

	public void onClearPhotoButtonClick(View view) {
		if (!mProcessing) {
			mProcessing = true;
			clearPhoto();
		}
	}

	public void pickPhoto() {
		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		startActivityForResult(photoPickerIntent, CandiConstants.ACTIVITY_PHOTO_PICK);
	}

	public void pickVideo() {
		Intent videoPickerIntent = new Intent(Intent.ACTION_PICK);
		videoPickerIntent.setType("video/*");
		startActivityForResult(videoPickerIntent, CandiConstants.ACTIVITY_VIDEO_PICK);
	}

	public void takeVideo() {
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(takeVideoIntent, CandiConstants.ACTIVITY_VIDEO_MAKE);
	}

	public void takePhoto() {
		Intent takePictureFromCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (ImageManager.getInstance().hasImageCaptureBug()) {
			takePictureFromCameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File("/sdcard/tmp")));
		}
		else
			takePictureFromCameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(takePictureFromCameraIntent, CandiConstants.ACTIVITY_PHOTO_MAKE);
	}

	public void bindEntity() {

		if (mVerb == Verb.New) {
			mPostEntity = new PostEntity();
			mPostEntity.beaconId = mBeacon.id;
			mPostEntity.signalFence = -100f;
			mPostEntity.createdById = String.valueOf(mUser.id);
			mPostEntity.enabled = true;
			mPostEntity.locked = false;
			mPostEntity.entityType = CandiConstants.TYPE_CANDI_POST;

			if (mSubType == SubType.Topic) {
				mPostEntity.parentEntityId = mParentEntityId;
			}
		}
		else if (mVerb == Verb.Edit) {
			String jsonResponse = ProxibaseService.getInstance().selectAsString(mEntityProxy.getEntryUri(), ResponseFormat.Json);
			mPostEntity = (PostEntity) ProxibaseService.convertJsonToObject(jsonResponse, PostEntity.class);
		}
	}

	private void bindLayout() {

		if (mVerb == Verb.New) {
			((Button) findViewById(R.id.DeleteButton)).setVisibility(View.GONE);
			if (mSubType == SubType.Topic) {
				((CheckBox) findViewById(R.id.Locked)).setVisibility(View.VISIBLE);
				((CheckBox) findViewById(R.id.Locked)).setChecked(false);
			}
			if (mSubType == SubType.Comment) {
				((CheckBox) findViewById(R.id.Locked)).setVisibility(View.GONE);
			}
		}
		if (mVerb == Verb.Edit) {
			((EditText) findViewById(R.id.PhotoTitle)).setText(mPostEntity.title);
			((EditText) findViewById(R.id.PhotoDescription)).setText(mPostEntity.description);
			((Button) findViewById(R.id.DeleteButton)).setVisibility(View.VISIBLE);

			if (mSubType == SubType.Topic) {
				((CheckBox) findViewById(R.id.Locked)).setVisibility(View.VISIBLE);
				((CheckBox) findViewById(R.id.Locked)).setChecked(mPostEntity.locked);
			}
			if (mSubType == SubType.Comment) {
				((CheckBox) findViewById(R.id.Locked)).setVisibility(View.GONE);
				((CheckBox) findViewById(R.id.Locked)).setChecked(false);
			}

			if (mPostEntity.photoImageUri != null && mPostEntity.photoImageUri != "") {
				addPhotoFromService(mPostEntity.photoImageUri);
			}
			else {
				clearPhoto();
			}
		}
		((ImageView) findViewById(R.id.PhotoThumbnail)).requestFocus();
	}

	public void doSave() {

		// If we have a new device photo then push it to S3
		if (mPhotoChanged && mPhotoDeviceUri != null) {
			/*
			 * TODO: If update then we might have orphaned a photo in S3
			 */
			Bitmap bitmap = ImageManager.getInstance().loadBitmapFromDevice(mPhotoDeviceUri, "250");
			if (bitmap != null) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
				byte[] bitmapBytes = outputStream.toByteArray();
				String imageKey = String.valueOf(mUser.id) + "_" + String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME)) + ".jpg";
				ByteArrayInputStream inputStream = new ByteArrayInputStream(bitmapBytes);
				ObjectMetadata metadata = new ObjectMetadata();
				metadata.setContentLength(bitmapBytes.length);
				metadata.setContentType("image/jpeg");

				try {
					S3.getInstance().putObject(CandiConstants.S3_BUCKET_IMAGES, imageKey, inputStream, metadata);
					S3.getInstance().setObjectAcl(CandiConstants.S3_BUCKET_IMAGES, imageKey, CannedAccessControlList.PublicRead);
					mPhotoServiceUri = CandiConstants.URL_AIRCANDI_MEDIA + CandiConstants.S3_BUCKET_IMAGES + "/" + imageKey;
				}
				catch (final AmazonServiceException exception) {
					exception.printStackTrace();
				}
				catch (final AmazonClientException exception) {
					exception.printStackTrace();
				}
				finally {
					try {
						outputStream.close();
						inputStream.close();
					}
					catch (IOException exception) {
						exception.printStackTrace();
					}
				}
			}
		}

		// Insert beacon if it isn't already registered
		if (mBeacon != null && mBeacon.isUnregistered) {
			mBeacon.registeredById = String.valueOf(mUser.id);
			mBeacon.beaconType = BeaconType.Fixed.name().toLowerCase();
			mBeacon.beaconSetId = ProxiConstants.BEACONSET_WORLD;
			mBeacon.insert();
		}

		if (mVerb == Verb.New) {
			insertEntity();
		}
		else if (mVerb == Verb.Edit) {
			updateEntity();
		}
	}

	private void insertEntity() {

		mPostEntity.title = ((EditText) findViewById(R.id.PhotoTitle)).getText().toString();
		mPostEntity.label = ((EditText) findViewById(R.id.PhotoTitle)).getText().toString();
		mPostEntity.description = ((EditText) findViewById(R.id.PhotoDescription)).getText().toString();
		if (mSubType == SubType.Topic) {
			mPostEntity.locked = ((CheckBox) findViewById(R.id.Locked)).isChecked();
		}
		if (mParentEntityId != 0) {
			mPostEntity.parentEntityId = mParentEntityId;
		}
		else {
			mPostEntity.parentEntityId = null;
		}
		mPostEntity.imageUri = mPhotoServiceUri;
		mPostEntity.imageFormat = "binary";
		mPostEntity.photoImageUri = mPhotoServiceUri;
		mPostEntity.enabled = true;
		mPostEntity.createdDate = DateUtils.nowString();

		mPostEntity.insertAsync(new SimpleModifyListener() {

			@Override
			public void onComplete(String jsonResponse) {

				runOnUiThread(new Runnable() {

					public void run() {
						stopTitlebarProgress();
						AircandiUI.showToastNotification(Post.this, "Candi Saved", Toast.LENGTH_SHORT);
						Intent intent = new Intent();

						// We are editing so set the dirty flag
						if (mSubType == SubType.Topic)
							intent.putExtra(getString(R.string.EXTRA_BEACON_DIRTY), mPostEntity.beaconId);
						else if (mSubType == SubType.Comment)
							intent.putExtra(getString(R.string.EXTRA_ENTITY_DIRTY), mPostEntity.parentEntityId);
						intent.putExtra(getString(R.string.EXTRA_RESULT_VERB), Verb.New);

						setResult(Activity.RESULT_FIRST_USER, intent);
						finish();
						overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
					}
				});
			}
		});
	}

	private void updateEntity() {

		mPostEntity.photoImageUri = mPhotoServiceUri;
		mPostEntity.imageUri = mPhotoServiceUri;
		mPostEntity.title = ((EditText) findViewById(R.id.PhotoTitle)).getText().toString();
		mPostEntity.label = ((EditText) findViewById(R.id.PhotoTitle)).getText().toString();
		mPostEntity.description = ((EditText) findViewById(R.id.PhotoDescription)).getText().toString();
		if (mSubType == SubType.Topic) {
			mPostEntity.locked = ((CheckBox) findViewById(R.id.Locked)).isChecked();
		}

		mPostEntity.updateAsync(new SimpleModifyListener() {

			@Override
			public void onComplete(String response) {
				// Post the processed result back to the UI thread
				Post.this.runOnUiThread(new Runnable() {

					public void run() {
						stopTitlebarProgress();
						AircandiUI.showToastNotification(Post.this, "Saved", Toast.LENGTH_SHORT);
						Intent intent = new Intent();
						intent.putExtra(getString(R.string.EXTRA_ENTITY_DIRTY), mPostEntity.id);
						intent.putExtra(getString(R.string.EXTRA_RESULT_VERB), Verb.Edit);

						setResult(Activity.RESULT_FIRST_USER, intent);
						finish();
						overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
					}
				});

			}
		});
	}

	public void doDelete() {

		/*
		 * TODO: We need to update the service so the recursive entity delete also
		 * deletes any associated resources stored with S3. As currently coded, we will
		 * be orphaning any images associated with child entities.
		 */

		// If there is an image stored with S3 then delete it
		if (mPostEntity.photoImageUri != "") {
			try {
				String imageKey = mPostEntity.photoImageUri.substring(mPostEntity.photoImageUri.lastIndexOf("/") + 1);
				S3.getInstance().deleteObject(CandiConstants.S3_BUCKET_IMAGES, imageKey);
			}
			catch (final AmazonServiceException exception) {
				exception.printStackTrace();
			}
			catch (final AmazonClientException exception) {
				exception.printStackTrace();
			}
		}

		// Delete the entity from the service
		Bundle parameters = new Bundle();
		parameters.putInt("entityId", mPostEntity.id);

		try {
			ProxibaseService.getInstance().webMethod("DeleteEntityWithChildren", parameters, ResponseFormat.Json, "");
		}
		catch (ClientProtocolException exception) {
			exception.printStackTrace();
		}
		catch (URISyntaxException exception) {
			exception.printStackTrace();
		}
		catch (IOException exception) {
			exception.printStackTrace();
		}

		stopTitlebarProgress();
		AircandiUI.showToastNotification(Post.this, "Deleted", Toast.LENGTH_SHORT);
		Intent intent = new Intent();
		intent.putExtra(getString(R.string.EXTRA_ENTITY_DIRTY), mPostEntity.id);
		intent.putExtra(getString(R.string.EXTRA_RESULT_VERB), Verb.Delete);
		setResult(Activity.RESULT_FIRST_USER, intent);
		finish();
		overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
	}

	private void deletePhotoFromService(String imageKey) {
		// If there is an image stored with S3 then delete it
		if (mPostEntity.photoImageUri != "") {
			try {
				S3.getInstance().deleteObject(CandiConstants.S3_BUCKET_IMAGES, imageKey);
			}
			catch (final AmazonServiceException exception) {
				exception.printStackTrace();
			}
			catch (final AmazonClientException exception) {
				exception.printStackTrace();
			}
		}
	}

	@Override
	protected int getLayoutID() {
		return R.layout.post;
	}

	private void clearPhoto() {
		mPhotoChanged = true;
		mPhotoDeviceUri = null;
		mPhotoServiceUri = null;

		// Delete photo from service
		if (mPostEntity.photoImageUri != null && mPostEntity.photoImageUri != "")
			deletePhotoFromService(mPostEntity.photoImageUri.substring(mPostEntity.photoImageUri.lastIndexOf("/") + 1));

		((ImageView) findViewById(R.id.PhotoThumbnail)).setImageBitmap(null);
		((ImageView) findViewById(R.id.PhotoThumbnail)).setAnimation(null);
		((ImageView) findViewById(R.id.PhotoThumbnail)).setVisibility(View.GONE);

		((Button) findViewById(R.id.AddPhotoButton)).setVisibility(View.VISIBLE);
		((Button) findViewById(R.id.ClearPhotoButton)).setVisibility(View.GONE);

		if (mPhotoThumbnailBitmap != null) {
			mPhotoThumbnailBitmap = null;
		}
		mProcessing = false;
	}

	private void addPhotoFromDevice(Uri imageUri) {
		mPhotoChanged = true;
		mPhotoDeviceUri = imageUri;
		mPhotoThumbnailBitmap = ImageManager.getInstance().loadBitmapFromDevice(imageUri, "250");

		((ImageView) findViewById(R.id.PhotoThumbnail)).setImageBitmap(mPhotoThumbnailBitmap);
		((ImageView) findViewById(R.id.PhotoThumbnail)).setVisibility(View.VISIBLE);
		((Button) findViewById(R.id.AddPhotoButton)).setVisibility(View.GONE);
		((Button) findViewById(R.id.ClearPhotoButton)).setVisibility(View.VISIBLE);
	}

	private void addPhotoFromService(final String imageUri) {
		mPhotoServiceUri = imageUri;

		if (ImageManager.getInstance().hasImage(imageUri)) {
			mPhotoThumbnailBitmap = ImageManager.getInstance().getImage(imageUri);
		}
		else {
			ImageRequest imageRequest = new ImageManager.ImageRequest();
			imageRequest.imageId = imageUri;
			imageRequest.imageUri = imageUri;
			imageRequest.imageShape = "square";
			imageRequest.widthMinimum = 80;
			imageRequest.showReflection = false;
			imageRequest.imageReadyListener = new IImageReadyListener() {

				@Override
				public void onImageReady(Bitmap bitmap) {
					Utilities.Log(CandiConstants.APP_NAME, "Photo", "Image fetched: " + imageUri);
					mPhotoThumbnailBitmap = ImageManager.getInstance().getImage(imageUri);
				}
			};
			Utilities.Log(CandiConstants.APP_NAME, "Photo", "Fetching Image: " + imageUri);
			ImageManager.getInstance().fetchImageAsynch(imageRequest);
		}

		Animation animation = AnimationUtils.loadAnimation(Post.this, R.anim.fade_in_medium);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		animation.setStartOffset(500);

		((ImageView) findViewById(R.id.PhotoThumbnail)).setImageBitmap(mPhotoThumbnailBitmap);
		((ImageView) findViewById(R.id.PhotoThumbnail)).startAnimation(animation);

		((ImageView) findViewById(R.id.PhotoThumbnail)).setVisibility(View.VISIBLE);
		((Button) findViewById(R.id.AddPhotoButton)).setVisibility(View.GONE);
		((Button) findViewById(R.id.ClearPhotoButton)).setVisibility(View.VISIBLE);
	}

	private void showAddPhotoDialog() {

		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				final Dialog dialog = new Dialog(Post.this, R.style.AircandiDialogTheme);
				final RelativeLayout dialogLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.temp_dialog_add_photo, null);
				dialog.setContentView(dialogLayout, new FrameLayout.LayoutParams(
						dialog.getWindow().getWindowManager().getDefaultDisplay().getWidth() - 40, LayoutParams.FILL_PARENT, Gravity.CENTER));
				dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
				dialog.getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_bg));

				((Button) dialogLayout.findViewById(R.id.SelectPhotoButton)).setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						pickPhoto();
						overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
						dialog.dismiss();
					}
				});
				((Button) dialogLayout.findViewById(R.id.TakePhotoButton)).setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						takePhoto();
						overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
						dialog.dismiss();
					}
				});
				dialog.show();

			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		/*
		 * Called before onResume. If we are returning from the market app, we
		 * get a zero result code whether the user decided to start an install
		 * or not.
		 */
		if (requestCode == CandiConstants.ACTIVITY_PHOTO_PICK) {
			if (resultCode == Activity.RESULT_OK) {
				Uri imageUri = data.getData();
				addPhotoFromDevice(imageUri);
			}
		}
		else if (requestCode == CandiConstants.ACTIVITY_PHOTO_MAKE) {
			if (resultCode == Activity.RESULT_OK) {
				Uri imageUri = null;
				if (ImageManager.getInstance().hasImageCaptureBug()) {
					File imageFile = new File("/sdcard/tmp");
					try {
						imageUri = Uri.parse(android.provider.MediaStore.Images.Media.insertImage(getContentResolver(), imageFile.getAbsolutePath(),
								null, null));
						if (!imageFile.delete()) {
						}
					}
					catch (FileNotFoundException exception) {
						exception.printStackTrace();
					}
				}
				else {
					imageUri = data.getData();
				}
				addPhotoFromDevice(imageUri);
			}
		}
		mProcessing = false;
	}
}