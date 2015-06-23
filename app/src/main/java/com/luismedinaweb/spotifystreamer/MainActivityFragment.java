package com.luismedinaweb.spotifystreamer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        api = new SpotifyApi();
        spotify = api.getService();

        final SearchView searchView = (SearchView) getActivity().findViewById(R.id.search_artist);
        ListView mArtistsListView = (ListView) getActivity().findViewById(R.id.artist_listview);
        mArtistsAdapter = new ArtistsAdapter(getActivity(), new ArrayList<Artist>());

        if (savedInstanceState != null) {
            ArrayList<ParcelableArtist> artists = savedInstanceState.getParcelableArrayList(SAVED_ARTIST_LIST_TAG);
            if (artists != null) {
                for (ParcelableArtist artist : artists) {
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

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.isEmpty()) {
                    searchView.clearFocus();
                    getActivity().findViewById(R.id.progressBar_Layout).setVisibility(View.VISIBLE);

                    spotify.searchArtists(query, new Callback<ArtistsPager>() {
                        @Override
                        public void success(final ArtistsPager artistsPager, Response response) {
                            final Activity activity = getActivity();
                            if (activity != null && isAdded()) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        activity.findViewById(R.id.progressBar_Layout).setVisibility(View.GONE);
                                        mArtistsAdapter.clear();
                                        if (artistsPager.artists.items.size() > 0) {
                                            for (Artist artist : artistsPager.artists.items) {
                                                mArtistsAdapter.add(artist);
                                            }
                                        } else {
                                            Toast.makeText(activity, "No results found! Please refine your search and try again.", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                            }
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Activity activity = getActivity();
                            if (activity != null && isAdded()) {
                                activity.findViewById(R.id.progressBar_Layout).setVisibility(View.GONE);
                                mArtistsAdapter.clear();
                                Toast.makeText(activity, error.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        SearchView searchView = (SearchView) getActivity().findViewById(R.id.search_artist);
        outState.putString(SAVED_ARTIST_TAG, searchView.getQuery().toString());
        if (mArtistsAdapter != null) {
            ArrayList<ParcelableArtist> artistList = new ArrayList<>();
            for (int i = 0; i < mArtistsAdapter.getCount(); i++) {
                artistList.add(new ParcelableArtist(mArtistsAdapter.getItem(i)));
            }
            outState.putParcelableArrayList(SAVED_ARTIST_LIST_TAG, artistList);
        }
        super.onSaveInstanceState(outState);
    }



}
