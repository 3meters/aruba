package com.proxibase.aircandi.components;

import android.content.Intent;
import android.media.AsyncPlayer;
import android.media.AudioManager;
import android.net.Uri;

import com.proxibase.aircandi.Aircandi;
import com.proxibase.aircandi.core.CandiConstants;

public class MediaUtils {

	public static void playVideo(String uri) {
		if (uri == null) {
			uri = CandiConstants.URL_AIRCANDI_MEDIA + "video/cezanne.mp3";
		}
		Intent tostart = new Intent(Intent.ACTION_VIEW);
		tostart.setDataAndType(Uri.parse(uri), "video/*");
		Aircandi.applicationContext.startActivity(tostart);
	}

	public static void playAudio(String uri) {
		AsyncPlayer player = new AsyncPlayer("Aircandi");
		player.stop();
		player.play(Aircandi.applicationContext, Uri.parse(uri), false, AudioManager.STREAM_MUSIC);
	}
}