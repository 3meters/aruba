package com.proxibase.aircandi.candi.modifiers;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.util.modifier.ease.IEaseFunction;

import com.proxibase.aircandi.candi.views.CandiView;
import com.proxibase.aircandi.candi.views.ZoneView;

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
				if (mEntity instanceof CandiView) {
					CandiView view = (CandiView) mEntity;
					if (view.mBodySprite != null) {

						view.mBodySprite.setAlpha(1);
						view.mBodySprite.setVisible(false);
						
						if (view.mReflectionSprite != null && view.mReflectionActive) {
							view.mReflectionSprite.setAlpha(1);
							view.mReflectionSprite.setVisible(false);
						}
						
						if (view.mTitleSprite != null) {
							view.mTitleSprite.setAlpha(1);
							view.mTitleSprite.setVisible(false);
						}
					}
				}
				else if (mEntity instanceof ZoneView) {
					ZoneView view = (ZoneView) mEntity;
					if (view.mBodySprite != null) {

						view.mBodySprite.setAlpha(1);
						view.mBodySprite.setVisible(false);
						
						if (view.mReflectionSprite != null) {
							view.mReflectionSprite.setAlpha(1);
							view.mReflectionSprite.setVisible(false);
						}
						
						if (view.mTitleSprite != null) {
							view.mTitleSprite.setAlpha(1);
							view.mTitleSprite.setVisible(false);
						}
					}
				}
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
				if (mEntity instanceof CandiView) {
					CandiView view = (CandiView) mEntity;
					if (view.mBodySprite != null) {

						view.mBodySprite.setAlpha(0);
						view.mBodySprite.setVisible(true);
						
						if (view.mReflectionSprite != null && view.mReflectionActive) {
							view.mReflectionSprite.setAlpha(0);
							view.mReflectionSprite.setVisible(true);
						}
						
						if (view.mTitleSprite != null) {
							view.mTitleSprite.setAlpha(0);
							view.mTitleSprite.setVisible(true);
						}
					}
				}
				else if (mEntity instanceof ZoneView) {
					ZoneView view = (ZoneView) mEntity;
					if (view.mBodySprite != null) {

						view.mBodySprite.setAlpha(0);
						view.mBodySprite.setVisible(true);
						
						if (view.mReflectionSprite != null) {
							view.mReflectionSprite.setAlpha(0);
							view.mReflectionSprite.setVisible(true);
						}
						
						if (view.mTitleSprite != null) {
							view.mTitleSprite.setAlpha(0);
							view.mTitleSprite.setVisible(true);
						}
					}
				}
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
