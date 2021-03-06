package com.luismedinaweb.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.luismedinaweb.spotifystreamer.models.ParcelableTrack;

import java.util.ArrayList;


public class TopTracksActivity extends ActionBarActivity implements TopTracksActivityFragment.ClickCallback {

    private static final String PLAYERFRAGMENT_TAG = "PFTAG";
    private static final int MENU_ITEM_NOW_PLAYING = 1;
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_tracks);

        mTwoPane = getSupportFragmentManager().findFragmentById(R.id.fragment_main) != null;

        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setSubtitle(getIntent().getStringExtra(MainActivityFragment.ARTIST_NAME_TAG));
        }

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.

            Bundle arguments = new Bundle();
            arguments.putString(MainActivityFragment.ARTIST_ID_TAG, getIntent().getStringExtra(MainActivityFragment.ARTIST_ID_TAG));
            arguments.putString(MainActivityFragment.ARTIST_NAME_TAG, getIntent().getStringExtra(MainActivityFragment.ARTIST_NAME_TAG));

            TopTracksActivityFragment fragment = new TopTracksActivityFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.tracks_detail_container, fragment)
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_top_tracks, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        try {
            if (PlayerService.isStarted()) {
                if (menu.findItem(MENU_ITEM_NOW_PLAYING) == null) {
                    menu.add(Menu.NONE, MENU_ITEM_NOW_PLAYING, Menu.FIRST, getString(R.string.menu_item_now_playing));
                }
            } else {
                if (menu.findItem(MENU_ITEM_NOW_PLAYING) != null) {
                    menu.removeItem(MENU_ITEM_NOW_PLAYING);
                }
            }
        } catch (Exception e) {
            Log.e("MenuPreparation", e.getMessage());
        }
        return super.onPrepareOptionsMenu(menu);
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
        if(id == android.R.id.home){
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        if (id == MENU_ITEM_NOW_PLAYING) {
            if (mTwoPane) {
                showPlayerDialogIfNeeded();
            } else {
                startActivity(new Intent(this, PlayerActivity.class));
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showPlayerDialogIfNeeded() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        PlayerFragment theFragment = (PlayerFragment) fragmentManager.findFragmentByTag(PLAYERFRAGMENT_TAG);
        if (theFragment == null) {
            PlayerFragment fragment = PlayerFragment.newInstance(null, -1, mTwoPane);
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            fragment.show(fragmentManager, PLAYERFRAGMENT_TAG);

        }
    }

    @Override
    public void onItemSelected(ArrayList<ParcelableTrack> tracks, int selectedTrack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        PlayerFragment fragment = PlayerFragment.newInstance(tracks, selectedTrack, mTwoPane);
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.

            fragment.show(fragmentManager, PLAYERFRAGMENT_TAG);
        } else {
            Intent intent = new Intent(this, PlayerActivity.class)
                    .putParcelableArrayListExtra(PlayerFragment.TRACKS_PARAM, tracks)
                    .putExtra(PlayerFragment.SELECTED_INDEX_PARAM, selectedTrack);
            startActivity(intent);
//
//            // The device is smaller, so show the fragment fullscreen
//            FragmentTransaction transaction = fragmentManager.beginTransaction();
//            // For a little polish, specify a transition animation
//            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
//            // To make it fullscreen, use the 'content' root view as the container
//            // for the fragment, which is always the root view for the activity
//            transaction.add(R.id.player_container, fragment)
//                    .addToBackStack(null).commit();
        }
    }


}
