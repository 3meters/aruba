package com.aircandi;

import com.aircandi.components.ProxiExplorer.EntityTree;

import android.os.Bundle;
import android.view.View;

public class CandiForm extends CandiFormBase {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			bind(false);
		}
	}

	public void bind(Boolean refresh) {
		doBind(refresh, false, EntityTree.Radar);
	}

	public void onChildrenButtonClick(View v) {
		showChildrenForEntity(CandiList.class);
	}
}