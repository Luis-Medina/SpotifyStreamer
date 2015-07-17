package com.luismedinaweb.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.luismedinaweb.spotifystreamer.models.ParcelableTrack;

import java.util.ArrayList;


public class PlayerActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.

            ArrayList<ParcelableTrack> tracks = getIntent().getParcelableArrayListExtra(PlayerFragment.TRACKS_PARAM);
            int selectedPosition = getIntent().getIntExtra(PlayerFragment.SELECTED_INDEX_PARAM, -1);
            PlayerFragment fragment;
            if (tracks != null) {
                fragment = PlayerFragment.newInstance(tracks, selectedPosition);
            } else {
                fragment = new PlayerFragment();
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.player_container, fragment)
                    .commit();
        }

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        int height = displaymetrics.heightPixels;
        int width = displaymetrics.widthPixels;
        Log.e("MEASUREMENTS", "HEIGHT: " + height + "    WIDTH: " + width);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
