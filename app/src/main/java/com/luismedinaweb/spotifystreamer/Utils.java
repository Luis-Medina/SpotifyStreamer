package com.luismedinaweb.spotifystreamer;

import android.content.Intent;

import com.luismedinaweb.spotifystreamer.models.ParcelableTrack;

/**
 * Created by Luis on 7/17/2015.
 */
public class Utils {

    public static Intent createShareTrackIntent(ParcelableTrack track) {
        if (track != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, track.getExternalURL());
            return shareIntent;
        }
        return null;
    }

}
