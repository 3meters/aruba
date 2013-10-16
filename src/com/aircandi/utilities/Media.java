package com.aircandi.utilities;

import com.aircandi.Aircandi;

public class Media {
	
	public static int playSound(Integer soundResId, Float multiplier) {
		return Aircandi.soundPool.play(soundResId, 1f, 1f, 0, 0, 1f);
	}
}