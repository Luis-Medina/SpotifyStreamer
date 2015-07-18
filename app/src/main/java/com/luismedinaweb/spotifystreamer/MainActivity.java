package com.luismedinaweb.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.luismedinaweb.spotifystreamer.models.ParcelableTrack;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity implements MainActivityFragment.ClickCallback, TopTracksActivityFragment.ClickCallback,
        PlayerFragment.TrackChanged {

    private static final String TRACKSFRAGMENT_TAG = "DFTAG";
    private static final String PLAYERFRAGMENT_TAG = "PFTAG";
    private static final int MENU_ITEM_NOW_PLAYING = 1;
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String SAVED_ARTIST_NAME = "artistName";
    private boolean mTwoPane;
    private int mTrackPosition = -1;
    private boolean mShowPlayerDialog = false;
    private ParcelableTrack mCurrentTrack;
    private ShareActionProvider mShareActionProvider;

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

            } else {
                if (getSupportActionBar() != null)
                    getSupportActionBar().setSubtitle(savedInstanceState.getString(SAVED_ARTIST_NAME));
            }

        } else {
            mTwoPane = false;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getBooleanExtra(PlayerService.PARAM_SHOW_PLAYER_DIALOG, false)) {
            mShowPlayerDialog = true;
        }
        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        if (mTwoPane && mShowPlayerDialog) {
            mShowPlayerDialog = false;
            showPlayerDialogIfNeeded();
        }
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (getSupportActionBar() != null) {
            if (getSupportActionBar().getSubtitle() != null) {
                outState.putString(SAVED_ARTIST_NAME, getSupportActionBar().getSubtitle().toString());
            }
        }

        super.onSaveInstanceState(outState);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.action_share);

        if (mTwoPane) {
            // Fetch and store ShareActionProvider
            mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        } else {
            menu.removeItem(R.id.action_share);
        }

        return true;
    }

    private void setShareIntent() {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(Utils.createShareTrackIntent(mCurrentTrack));
        }
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
            if (mTwoPane) {
                showPlayerDialogIfNeeded();
            } else {
                startActivity(new Intent(this, PlayerActivity.class));
            }
            return true;
        }
        if (id == R.id.action_share) {
            if (mCurrentTrack == null) {
                Toast.makeText(getApplicationContext(), "No track available.", Toast.LENGTH_LONG).show();
            }
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

            if (getSupportActionBar() != null) getSupportActionBar().setSubtitle(artistName);
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
            PlayerFragment fragment = PlayerFragment.newInstance(tracks, selectedTrack, true);
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.

            fragment.show(fragmentManager, PLAYERFRAGMENT_TAG);

        }
    }


    @Override
    public void trackChanged(ParcelableTrack track) {
        mCurrentTrack = track;
        setShareIntent();
    }
}
