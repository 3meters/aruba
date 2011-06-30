package com.proxibase.aircandi.candi.sprites;

import org.anddev.andengine.entity.modifier.DelayModifier;
import org.anddev.andengine.entity.modifier.MoveXModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.util.modifier.ease.EaseQuartOut;
import org.anddev.andengine.util.modifier.ease.IEaseFunction;

import com.proxibase.aircandi.candi.models.ZoneModel;
import com.proxibase.aircandi.controllers.Aircandi;
import com.proxibase.aircandi.utils.Utilities;

public class CameraTargetSprite extends CandiRectangle {

	public CameraTargetSprite(final float x, final float y, final float width, final float height) {
		super(x, y, width, height);
	}

	private float setTargetZone(ZoneModel targetZone, boolean animateSynchUp, float duration, float delay, IEaseFunction easeFunction) {
		SequenceEntityModifier modSequence = new SequenceEntityModifier(new DelayModifier(delay), new MoveXModifier(duration, this.getX(), targetZone
				.getX(), easeFunction));
		this.registerEntityModifier(modSequence);
		return Math.abs(this.getX() - targetZone.getX());
	}

	public float moveToZone(ZoneModel targetZone, float duration) {
		if (targetZone == null)
			return 0;
		Utilities.Log(Aircandi.APP_NAME, "CameraSprite", "MoveToZone: Setting focus to zone: " + String.valueOf(targetZone.getZoneIndex()));
		float distanceMoved = setTargetZone(targetZone, true, duration, 0, EaseQuartOut.getInstance());
		return distanceMoved;
	}
}
