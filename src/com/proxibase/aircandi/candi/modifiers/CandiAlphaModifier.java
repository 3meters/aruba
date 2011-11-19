package com.proxibase.aircandi.candi.modifiers;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.util.modifier.ease.IEaseFunction;

public class CandiAlphaModifier extends AlphaModifier {

	private float	mFromAlpha;
	private float	mToAlpha;
	private IEntity	mEntity;

	public CandiAlphaModifier(IEntity entity, float pDuration, float pFromAlpha, float pToAlpha, IEaseFunction pEaseFunction) {
		super(pDuration, pFromAlpha, pToAlpha, pEaseFunction);
		mFromAlpha = pFromAlpha;
		mToAlpha = pToAlpha;
		mEntity = entity;
	}

	public CandiAlphaModifier(IEntity entity, float pDuration, float pFromAlpha, float pToAlpha, IEntityModifierListener pEntityModifierListener,
			IEaseFunction pEaseFunction) {
		super(pDuration, pFromAlpha, pToAlpha, pEntityModifierListener, pEaseFunction);
		mFromAlpha = pFromAlpha;
		mToAlpha = pToAlpha;
		mEntity = entity;
	}

	public CandiAlphaModifier(IEntity entity, float pDuration, float pFromAlpha, float pToAlpha, IEntityModifierListener pEntityModifierListener) {
		super(pDuration, pFromAlpha, pToAlpha, pEntityModifierListener);
		mFromAlpha = pFromAlpha;
		mToAlpha = pToAlpha;
		mEntity = entity;
	}

	public CandiAlphaModifier(IEntity entity, float pDuration, float pFromAlpha, float pToAlpha) {
		super(pDuration, pFromAlpha, pToAlpha);
		mFromAlpha = pFromAlpha;
		mToAlpha = pToAlpha;
		mEntity = entity;
	}

	@Override
	protected void onModifierFinished(IEntity pItem) {
		if (mToAlpha == 0) {
			if (mEntity != null) {
				mEntity.setVisible(false);
				mEntity.setAlpha(1);
			}
		}
		super.onModifierFinished(pItem);
	}

	@Override
	protected void onModifierStarted(IEntity pItem) {
		if (mFromAlpha == 0) {
			if (mEntity != null) {
				mEntity.setAlpha(0);
				mEntity.setVisible(true);
			}
		}
		super.onModifierStarted(pItem);
	}
	
	@Override
	protected void onSetValue(final IEntity pEntity, final float pPercentageDone, final float pAlpha) {
		pEntity.setAlpha(pAlpha);
	}


	public void setEntity(IEntity entity) {
		this.mEntity = entity;
	}

	public IEntity getEntity() {
		return mEntity;
	}
}
