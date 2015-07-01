package com.luismedinaweb.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * A placeholder fragment containing a simple view.
 */
public class TopTracksActivityFragment extends Fragment {

    public static final String TRACK_ID = "trackID";
    private static final String SAVED_TRACK_LIST_TAG = "savedTrackList";
    private static final String LOG_TAG = TopTracksActivityFragment.class.getSimpleName();
    private final String COUNTRY_KEY = "country";
    private TracksAdapter mTracksAdapter;
    private SpotifyApi api;
    private SpotifyService spotify;
    private String artistID;
    private Toast mToast;

    public TopTracksActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_top_tracks, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments != null) {
            artistID = arguments.getString(MainActivityFragment.ARTIST_ID_TAG);
        }

        ListView mTracksListView = (ListView) getActivity().findViewById(R.id.track_listview);
        mTracksAdapter = new TracksAdapter(getActivity(), new ArrayList<Track>());

        api = new SpotifyApi();
        spotify = api.getService();

        if (savedInstanceState != null) {
            ArrayList<ParcelableTrack> parcelableTracks = savedInstanceState.getParcelableArrayList(SAVED_TRACK_LIST_TAG);
            if (parcelableTracks != null) {
                for (ParcelableTrack parcelableTrack : parcelableTracks) {
                    mTracksAdapter.add(parcelableTrack.getSpotifyTrack());
                }
            }
        } else {
            if (artistID != null) {
                Activity activity = getActivity();
                if (activity != null && isAdded()) {
                    if (NetworkUtils.isOnline(activity)) {

                        Map<String, Object> options = new HashMap<>();
                        options.put(COUNTRY_KEY,
                                PreferenceManager
                                        .getDefaultSharedPreferences(getActivity())
                                        .getString(getString(R.string.pref_country_key), getString(R.string.pref_country_default)));

                        spotify.getArtistTopTrack(artistID, options, new Callback<Tracks>() {
                            @Override
                            public void success(final Tracks tracks, Response response) {
                                final Activity activity = getActivity();
                                if (activity != null && isAdded()) {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mTracksAdapter.clear();
                                            if (tracks.tracks.size() > 0) {
                                                for (Track track : tracks.tracks) {
                                                    mTracksAdapter.add(track);
                                                }
                                            } else {
                                                showToast(activity, getString(R.string.no_results), Toast.LENGTH_LONG);
                                            }
                                        }
                                    });
                                }
                            }

                            @Override
                            public void failure(final RetrofitError error) {
                                final Activity activity = getActivity();
                                if (activity != null && isAdded()) {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mTracksAdapter.clear();
                                            String message;
                                            if (error.getKind() == RetrofitError.Kind.NETWORK) {
                                                message = getString(R.string.connection_error);
                                            } else {
                                                SpotifyError spotifyError = SpotifyError.fromRetrofitError(error);
                                                message = (spotifyError.hasErrorDetails()
                                                        ? spotifyError.getErrorDetails().message
                                                        : spotifyError.getMessage());
                                            }
                                            showToast(activity, message, Toast.LENGTH_LONG);
                                        }
                                    });
                                }
                            }
                        });
                    } else {
                        showToast(activity, getString(R.string.no_connection), Toast.LENGTH_LONG);
                    }
                }
            }
        }

        mTracksListView.setAdapter(mTracksAdapter);
        mTracksListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((ClickCallback) getActivity()).onItemSelected(mTracksAdapter.getItem(position).id);
            }
        });

        super.onActivityCreated(savedInstanceState);
    }

    private void showToast(Context context, String message, int duration) {
        if (context == null) {
            context = getActivity();
        }
        if (mToast != null) {
            mToast.cancel();
        }
        if (context != null) {
            mToast = Toast.makeText(context, message, duration);
            mToast.show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mTracksAdapter != null) {
            ArrayList<ParcelableTrack> parcelableTracks = new ArrayList<>();
            for (int i = 0; i < mTracksAdapter.getCount(); i++) {
                parcelableTracks.add(new ParcelableTrack(mTracksAdapter.getItem(i)));
            }
            outState.putParcelableArrayList(SAVED_TRACK_LIST_TAG, parcelableTracks);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mToast != null) mToast.cancel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mToast != null) mToast.cancel();
    }


    public interface ClickCallback {

        void onItemSelected(String trackId);
    }

}
