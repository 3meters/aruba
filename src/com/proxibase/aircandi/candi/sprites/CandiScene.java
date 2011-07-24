package com.proxibase.aircandi.candi.sprites;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.scene.Scene;

import com.proxibase.aircandi.candi.models.BaseModel;
import com.proxibase.aircandi.candi.views.BaseView;

public class CandiScene extends Scene {

	@SuppressWarnings("deprecation")
	public CandiScene(int pLayerCount) {
		super(pLayerCount);
	}

	@Override
	public void setAlpha(float alpha) {
		super.setAlpha(alpha);

		for (int i = 0; i < getChildCount(); i++) {
			IEntity child = getChild(i);
			child.setAlpha(alpha);
			for (int x = 0; x < child.getChildCount(); x++) {
				if (child.getChild(x) instanceof BaseView) {
					BaseView baseView = (BaseView) child.getChild(x);
					if (!((BaseModel)baseView.getModel()).isVisibleCurrent())
						continue;
				}
				child.getChild(x).setAlpha(alpha);
			}
		}
	}
}
