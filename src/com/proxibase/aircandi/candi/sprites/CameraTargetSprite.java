package com.proxibase.aircandi.candi.sprites;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.DelayModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.anddev.andengine.entity.modifier.MoveYModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseExponentialInOut;
import org.anddev.andengine.util.modifier.ease.IEaseFunction;

import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;

public class CameraTargetSprite extends CandiRectangle {

	private CandiPatchPresenter	mCandiPatchPresenter;

	public CameraTargetSprite(final float x, final float y, final float width, final float height, CandiPatchPresenter candiPatchPresenter) {
		super(x, y, width, height);
		mCandiPatchPresenter = candiPatchPresenter;
	}

	public float moveToTop(float duration, IEaseFunction easeFunction, final MoveListener moveListener) {

		if (easeFunction == null) {
			easeFunction = EaseExponentialInOut.getInstance();
		}

		SequenceEntityModifier modifier = new SequenceEntityModifier(
				new IEntityModifierListener() {

					@Override
					public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
						if (moveListener != null) {
							moveListener.onMoveFinished();
						}
					}

					@Override
					public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
						mCandiPatchPresenter.renderingActivateBump();
						if (moveListener != null) {
							moveListener.onMoveStarted();
						}
					}

				}
				, new DelayModifier(0)
				, new MoveYModifier(duration, this.getY(), mCandiPatchPresenter.getBoundsMinY(), easeFunction));

		float distanceMoved = Math.abs(this.getY() - mCandiPatchPresenter.getBoundsMinY());
		this.registerEntityModifier(modifier);

		return distanceMoved;
	}

	public float moveToBottom(float duration, IEaseFunction easeFunction, final MoveListener moveListener) {

		if (easeFunction == null) {
			easeFunction = EaseExponentialInOut.getInstance();
		}

		SequenceEntityModifier modifier = new SequenceEntityModifier(
				new IEntityModifierListener() {

					@Override
					public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
						if (moveListener != null) {
							moveListener.onMoveFinished();
						}
					}

					@Override
					public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
						mCandiPatchPresenter.renderingActivateBump();
						if (moveListener != null) {
							moveListener.onMoveStarted();
						}
					}

				}
				, new DelayModifier(0)
				, new MoveYModifier(duration, this.getY(), mCandiPatchPresenter.getBoundsMaxY(), easeFunction));

		float distanceMoved = Math.abs(this.getY() - mCandiPatchPresenter.getBoundsMaxY());
		this.registerEntityModifier(modifier);

		return distanceMoved;
	}

	public interface MoveListener {

		public void onMoveStarted();

		public void onMoveFinished();
	}
}
