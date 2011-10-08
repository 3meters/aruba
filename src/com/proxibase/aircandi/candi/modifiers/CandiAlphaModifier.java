package com.proxibase.aircandi.candi.modifiers;

import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.util.modifier.ease.IEaseFunction;

public class CandiAlphaModifier extends AlphaModifier {

	private float	mFromAlpha;
	private float	mToAlpha;

	public CandiAlphaModifier(AlphaModifier pAlphaModifier) {
		super(pAlphaModifier);
	}

	public CandiAlphaModifier(float pDuration, float pFromAlpha, float pToAlpha, IEaseFunction pEaseFunction) {
		super(pDuration, pFromAlpha, pToAlpha, pEaseFunction);
		mFromAlpha = pFromAlpha;
		mToAlpha = pToAlpha;
	}

	public CandiAlphaModifier(float pDuration, float pFromAlpha, float pToAlpha, IEntityModifierListener pEntityModifierListener,
			IEaseFunction pEaseFunction) {
		super(pDuration, pFromAlpha, pToAlpha, pEntityModifierListener, pEaseFunction);
		mFromAlpha = pFromAlpha;
		mToAlpha = pToAlpha;
	}

	public CandiAlphaModifier(float pDuration, float pFromAlpha, float pToAlpha, IEntityModifierListener pEntityModifierListener) {
		super(pDuration, pFromAlpha, pToAlpha, pEntityModifierListener);
		mFromAlpha = pFromAlpha;
		mToAlpha = pToAlpha;
	}

	public CandiAlphaModifier(float pDuration, float pFromAlpha, float pToAlpha) {
		super(pDuration, pFromAlpha, pToAlpha);
		mFromAlpha = pFromAlpha;
		mToAlpha = pToAlpha;
	}

	public float getFromAlpha() {
		return mFromAlpha;
	}

	public float getToAlpha() {
		return mToAlpha;
	}
}
