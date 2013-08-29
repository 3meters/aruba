package com.aircandi.ui.edit;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ViewFlipper;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.TabManager;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Post;
import com.aircandi.ui.base.BaseEntityEdit;

public class PictureEdit extends BaseEntityEdit {

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

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	protected String getLinkType() {
		return Constants.TYPE_LINK_PICTURE;
	};

	@Override
	protected void beforeInsert(Entity entity) {
		if (Aircandi.currentPlace != null) {
			((Post) entity).placeId = Aircandi.currentPlace.id;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.post_edit;
	}
}