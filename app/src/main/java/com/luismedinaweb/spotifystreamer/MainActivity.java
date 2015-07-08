package com.luismedinaweb.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity implements MainActivityFragment.ClickCallback, TopTracksActivityFragment.ClickCallback {

    private static final String TRACKSFRAGMENT_TAG = "DFTAG";
    private static final String PLAYERFRAGMENT_TAG = "PFTAG";
    private static final int MENU_ITEM_NOW_PLAYING = 1;
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.tracks_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.tracks_detail_container, new TopTracksActivityFragment(), TRACKSFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
        if (id == MENU_ITEM_NOW_PLAYING) {
            startActivity(new Intent(this, PlayerActivity.class));
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(String artistID, String artistName) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putString(MainActivityFragment.ARTIST_ID_TAG, artistID);
            args.putString(MainActivityFragment.ARTIST_NAME_TAG, artistName);

            TopTracksActivityFragment fragment = new TopTracksActivityFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.tracks_detail_container, fragment, TRACKSFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, TopTracksActivity.class)
                    .putExtra(MainActivityFragment.ARTIST_ID_TAG, artistID)
                    .putExtra(MainActivityFragment.ARTIST_NAME_TAG, artistName);
            startActivity(intent);
        }
    }

    @Override
    public void onItemSelected(ArrayList<ParcelableTrack> tracks, int selectedTrack) {
        if (mTwoPane) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            PlayerFragment fragment = PlayerFragment.newInstance(tracks, selectedTrack);
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            fragment.show(fragmentManager, PLAYERFRAGMENT_TAG);
        }
    }
}
