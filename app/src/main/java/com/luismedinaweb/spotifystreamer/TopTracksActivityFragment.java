package com.luismedinaweb.spotifystreamer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private static final String LOG_TAG = TopTracksActivityFragment.class.getSimpleName();
    private TracksAdapter mTracksAdapter;
    private SpotifyApi api;
    private SpotifyService spotify;
    private String artistID;
    private ArrayList<Track> trackList = new ArrayList<>();

    public TopTracksActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_top_tracks, container, false);
        artistID = getActivity().getIntent().getStringExtra(MainActivityFragment.ARTIST_ID_TAG);

        api = new SpotifyApi();
        spotify = api.getService();

        mTracksAdapter = new TracksAdapter(getActivity(), new ArrayList<Track>());

        ListView mTracksListView = (ListView) rootView.findViewById(R.id.track_listview);
        mTracksListView.setAdapter(mTracksAdapter);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        new SpotifyTopTracksSearchTask().execute(artistID);
    }

    public class SpotifyTopTracksSearchTask extends AsyncTask<String, Void, Tracks> implements Callback<Tracks> {

        private final String LOG_TAG = SpotifyTopTracksSearchTask.class.getSimpleName();
        private final String COUNTRY_KEY = "country";
        private String USER_COUNTRY;

        @Override
        protected Tracks doInBackground(String... params) {
            try{
                Map<String, Object> options = new HashMap<>();
                options.put(COUNTRY_KEY, USER_COUNTRY);
                spotify.getArtistTopTrack(params[0], options, this);
                return null;
            }
            catch (Exception e){
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            USER_COUNTRY =  PreferenceManager
                    .getDefaultSharedPreferences(getActivity())
                    .getString(getString(R.string.pref_country_key), getString(R.string.pref_country_default));
        }

        @Override
        public void success(final Tracks tracks, Response response) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTracksAdapter.clear();
                    trackList.clear();
                    if(tracks.tracks.size() > 0){
                        for(Track track : tracks.tracks){
                            mTracksAdapter.add(track);
                            trackList.add(track);
                        }
                    }else{
                        Toast.makeText(getActivity(), "No results found!", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        @Override
        public void failure(final RetrofitError error) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTracksAdapter.clear();
                    SpotifyError spotifyError = SpotifyError.fromRetrofitError(error);
                    Toast.makeText(getActivity(),
                            (spotifyError.hasErrorDetails() ? spotifyError.getErrorDetails().message : spotifyError.getMessage()),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
