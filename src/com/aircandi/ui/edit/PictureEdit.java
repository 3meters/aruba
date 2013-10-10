package com.aircandi.ui.edit;

import android.os.Bundle;
import android.widget.ViewFlipper;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.TabManager;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Post;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.utilities.Dialogs;

public class PictureEdit extends BaseEntityEdit {

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
		Post post = (Post) mEntity;
		if (post.photo == null) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_photo)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		return true;
	}

	@Override
	protected String getLinkType() {
		return Constants.TYPE_LINK_CONTENT;
	};

	@Override
	protected void beforeInsert(Entity entity) {
		if (Aircandi.getInstance().getCurrentPlace() != null) {
			((Post) entity).placeId = Aircandi.getInstance().getCurrentPlace().id;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.picture_edit;
	}
}