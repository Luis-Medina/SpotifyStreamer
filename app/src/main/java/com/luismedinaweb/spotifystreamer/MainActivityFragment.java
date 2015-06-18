package com.luismedinaweb.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public static final String ARTIST_NAME_TAG = "artistName";
    public static final String ARTIST_ID_TAG = "artistID";
    private static final String LOG_TAG = MainActivityFragment.class.getSimpleName();
    private static final String SAVED_ARTIST_TAG = "savedArtist";
    private static final String SAVED_ARTIST_LIST_TAG = "savedArtistList";
    private ArtistsAdapter mArtistsAdapter;
    private SpotifyApi api;
    private SpotifyService spotify;

    public MainActivityFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        api = new SpotifyApi();
        spotify = api.getService();
        EditText editText = (EditText) getActivity().findViewById(R.id.artist_search_textview);
        ListView mArtistsListView = (ListView) getActivity().findViewById(R.id.artist_listview);
        mArtistsAdapter = new ArtistsAdapter(getActivity(), new ArrayList<Artist>());

        if(savedInstanceState != null){
            ArrayList<ParcelableArtist> artists = savedInstanceState.getParcelableArrayList(SAVED_ARTIST_LIST_TAG);
            if(artists != null){
                for(ParcelableArtist artist : artists){
                    mArtistsAdapter.add(artist.getSpotifyArtist());
                }
            }
        }


        mArtistsListView.setAdapter(mArtistsAdapter);
        mArtistsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Artist artist = mArtistsAdapter.getItem(position);
                Intent detailIntent = new Intent(getActivity(), TopTracksActivity.class)
                        .putExtra(ARTIST_NAME_TAG, artist.name)
                        .putExtra(ARTIST_ID_TAG, artist.id);
                startActivity(detailIntent);
            }
        });

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {

                    String artistToSearch = v.getText().toString();

                    InputMethodManager in = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    in.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                    if (!artistToSearch.isEmpty()) {
                        new SpotifyArtistSearchTask().execute(artistToSearch);
                    }
                    handled = true;
                }
                return handled;
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        EditText editText = (EditText) getActivity().findViewById(R.id.artist_search_textview);
        outState.putString(SAVED_ARTIST_TAG, editText.getText().toString());
        if(mArtistsAdapter != null){
            ArrayList<ParcelableArtist> artistList = new ArrayList<>();
            for(int i=0; i<mArtistsAdapter.getCount(); i++){
                artistList.add(new ParcelableArtist(mArtistsAdapter.getItem(i)));
            }
            outState.putParcelableArrayList(SAVED_ARTIST_LIST_TAG, artistList);
        }
        super.onSaveInstanceState(outState);
    }


    public class SpotifyArtistSearchTask extends AsyncTask<String, Void, ArtistsPager> implements Callback<ArtistsPager>{

        private final String LOG_TAG = SpotifyArtistSearchTask.class.getSimpleName();

        @Override
        protected ArtistsPager doInBackground(String... params) {
            try{
                spotify.searchArtists(params[0], this);
                return null;
            }
            catch (Exception e){
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            getActivity().findViewById(R.id.progressBar_Layout).setVisibility(View.VISIBLE);
        }

        @Override
        public void success(final ArtistsPager artistsPager, Response response) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getActivity().findViewById(R.id.progressBar_Layout).setVisibility(View.GONE);
                    mArtistsAdapter.clear();
                    if(artistsPager.artists.items.size() > 0){
                        for(Artist artist : artistsPager.artists.items){
                            mArtistsAdapter.add(artist);
                        }
                    }else{
                        Toast.makeText(getActivity(), "No results found! Please refine your search and try again.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        @Override
        public void failure(final RetrofitError error) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getActivity().findViewById(R.id.progressBar_Layout).setVisibility(View.GONE);
                    mArtistsAdapter.clear();
                    Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }


}
