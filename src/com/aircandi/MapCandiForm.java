package com.aircandi;

import com.aircandi.components.ProxiExplorer.EntityTree;

import android.os.Bundle;
import android.view.View;

public class MapCandiForm extends CandiFormBase {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			bind(false);
		}
	}

	public void bind(Boolean useEntityModel) {
		doBind(useEntityModel, false, EntityTree.Map);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onChildrenButtonClick(View v) {
		showChildrenForEntity(MapCandiList.class);
	}

}