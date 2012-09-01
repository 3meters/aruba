package com.aircandi.components;

import android.content.Intent;
import android.media.AsyncPlayer;
import android.media.AudioManager;
import android.net.Uri;

import com.aircandi.Aircandi;

public class MediaUtils {

	public static void playVideo(String uri) {
		if (uri == null) {
			uri = "video/cezanne.mp3";
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