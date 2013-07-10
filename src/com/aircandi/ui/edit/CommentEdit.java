package com.aircandi.ui.edit;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.aircandi.Aircandi;
import com.aircandi.beta.R;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.utilities.Dialogs;

public class CommentEdit extends BaseEntityEdit {

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mDescription.setImeOptions(EditorInfo.IME_ACTION_SEND);
		mDescription.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					if (validate()) {
						gather();
						update();
					}
					return true;
				}
				return false;
			}
		});
	}

	@Override
	protected void bind() {
		super.bind();
		/*
		 * We are always creating a new comment.
		 */
		mEntity.creator = Aircandi.getInstance().getUser();
		mEntity.creatorId = Aircandi.getInstance().getUser().id;
		mEntity.photo = Aircandi.getInstance().getUser().photo.clone();
		mEntity.name = Aircandi.getInstance().getUser().name;
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

	@Override
	protected boolean validate() {
		if (!super.validate()) {
			return false;
		}
		if (mDescription.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_message)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.comment_edit;
	}
}