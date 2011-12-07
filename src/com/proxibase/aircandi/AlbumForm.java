package com.proxibase.aircandi;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.AlbumEntity;
import com.proxibase.aircandi.models.BaseEntity;
import com.proxibase.aircandi.utils.Exceptions;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageManager.ImageRequestListener;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageFormat;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class AlbumForm extends EntityBaseForm {

	@Override
	protected void bindEntity() {

		/* We handle all the elements that are different than the base entity. */
		if (mCommand.verb.equals("new")) {
			AlbumEntity entity = new AlbumEntity();
			entity.entityType = CandiConstants.TYPE_CANDI_ALBUM;
			entity.parentEntityId = null;
			entity.imageUri = "resource:placeholder_gallery";
			entity.imageFormat = ImageFormat.Binary.name().toLowerCase();
			mEntity = entity;
		}
		else if (mCommand.verb.equals("edit")) {
			String jsonResponse = null;
			try {
				jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json);
				mEntity = (AlbumEntity) ProxibaseService.convertJsonToObject(jsonResponse, AlbumEntity.class, GsonType.ProxibaseService);
			}
			catch (ProxibaseException exception) {
				Exceptions.Handle(exception);
			}
		}
		super.bindEntity();
	}

	@Override
	protected void drawEntity() {
		super.drawEntity();

		((TextView) findViewById(R.id.txt_header_title)).setText(getResources().getString(R.string.form_title_album));

		if (findViewById(R.id.chk_locked) != null) {
			((CheckBox) findViewById(R.id.chk_locked)).setVisibility(View.VISIBLE);
			((CheckBox) findViewById(R.id.chk_locked)).setChecked(((AlbumEntity) mEntity).locked);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onChangePictureButtonClick(View view) {
		showChangePictureDialog(false, new ImageRequestListener() {

			@Override
			public void onImageReady(Bitmap bitmap) {
				BaseEntity entity = (BaseEntity) mEntity;
				if (bitmap == null) {
					entity.imageUri = "resource:placeholder_gallery";
					entity.imageBitmap = ImageManager.getInstance().loadBitmapFromResources(R.drawable.placeholder_gallery);
				}
				else {
					entity.imageUri = "updated";
					entity.imageBitmap = bitmap;
				}
				showPicture(entity.imageBitmap, R.id.img_public_image);
			}

			@Override
			public void onProxibaseException(ProxibaseException exception) {
				/* Do nothing */
				}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void doSave(boolean updateImages) {
		super.doSave(true);
	}
	
	@Override
	protected void gather() {
		/*
		 * Handle properties that are not part of the base entity
		 */
		final AlbumEntity entity = (AlbumEntity) mEntity;
		entity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		super.gather();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.album_form;
	}
}