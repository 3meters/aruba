package com.proxibase.aircandi.candi.sprites;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.DelayModifier;
import org.anddev.andengine.entity.modifier.MoveXModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseExponentialInOut;
import org.anddev.andengine.util.modifier.ease.IEaseFunction;

import com.proxibase.aircandi.candi.models.ZoneModel;
import com.proxibase.aircandi.controllers.Aircandi;
import com.proxibase.aircandi.utils.Utilities;

public class CameraTargetSprite extends CandiRectangle {

	public CameraTargetSprite(final float x, final float y, final float width, final float height) {
		super(x, y, width, height);
	}

	public float moveToZone(ZoneModel targetZone, float duration) {
		return this.moveToZone(targetZone, duration, null);
	}

	public float moveToZone(ZoneModel targetZone, float duration, IEaseFunction easeFunction) {
		return this.moveToZone(targetZone, duration, easeFunction, null);
	}

	public float moveToZone(ZoneModel targetZone, float duration, IEaseFunction easeFunction, final MoveListener moveListener) {

		if (targetZone == null)
			return 0;

		if (easeFunction == null)
			easeFunction = EaseExponentialInOut.getInstance();

		Utilities.Log(Aircandi.APP_NAME, "CameraSprite", "MoveToZone: Setting focus to zone: " + String.valueOf(targetZone.getZoneIndex()));

		SequenceEntityModifier modSequence = new SequenceEntityModifier(new IEntityModifierListener() {

			@Override
			public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
				if (moveListener != null)
					moveListener.onMoveFinished();
			}

			@Override
			public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {}
		},

		new DelayModifier(0), new MoveXModifier(duration, this.getX(), targetZone.getX(), easeFunction));

		this.registerEntityModifier(modSequence);
		float distanceMoved = Math.abs(this.getX() - targetZone.getX());
		return distanceMoved;
	}

	public interface MoveListener {

		public void onMoveFinished();
	}
}
