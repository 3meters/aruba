package com.proxibase.aircandi.controllers;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.media.AudioManager;
import android.media.SoundPool;

public class CxMediaPlayer
{
	private SoundPool	mShortPlayer	= null;
	private HashMap		mSounds			= new HashMap();

	// Constructor
	public CxMediaPlayer(Activity pContext) {
		
		// setup Soundpool
		this.mShortPlayer = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);

		// 0-9 Buttons
		mSounds.put(R.raw.notification1, this.mShortPlayer.load(pContext, R.raw.notification1, 1));
		mSounds.put(R.raw.notification2, this.mShortPlayer.load(pContext, R.raw.notification2, 1));
		mSounds.put(R.raw.notification3, this.mShortPlayer.load(pContext, R.raw.notification3, 1));
		mSounds.put(R.raw.notification4, this.mShortPlayer.load(pContext, R.raw.notification4, 1));
		mSounds.put(R.raw.notification5, this.mShortPlayer.load(pContext, R.raw.notification5, 1));

	}

	// Plays the passed preloaded resource
	public void playShortResource(int piResource)
	{
		int iSoundId = ((Map<Integer, Integer>) mSounds).get(piResource);
		this.mShortPlayer.play(iSoundId, 0.99f, 0.99f, 0, 0, 1);
	}

	// Cleanup
	public void Release()
	{
		// Cleanup
		this.mShortPlayer.release();
		this.mShortPlayer = null;
	}
}