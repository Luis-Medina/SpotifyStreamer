package com.luismedinaweb.spotifystreamer;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.luismedinaweb.spotifystreamer.models.ParcelableImage;
import com.luismedinaweb.spotifystreamer.models.ParcelableTrack;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * PlayerFragment.OnFragmentInteractionListener interface
 * to handle interaction events.
 * Use the {@link PlayerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlayerFragment extends DialogFragment {

    public static final String TRACKS_PARAM = "tracks";
    public static final String SELECTED_INDEX_PARAM = "selectedTrackPos";
    public static final String RECEIVER_ACTION_SET_UI_READY = "setUIReady";
    public static final String RECEIVER_ACTION_UPDATE_SONG_PROGRESS = "updateSongProgress";
    public static final String RECEIVER_ACTION_SET_PLAY_BUTTON = "setButtonToPlay";
    public static final String RECEIVER_ACTION_UPDATE_SONG_STATUS = "setSongStatus";
    public static final String RECEIVER_PARAM_SONG_LENGTH = "songLength";
    public static final String RECEIVER_PARAM_PLAY_ICON = "playIcon";
    public static final String RECEIVER_PARAM_CURRENT_POSITION = "currentPosition";
    public static final String RECEIVER_PARAM_CURRENT_TRACK = "currentTrack";
    public static final String PARAM_IS_TWO_PANE = "isTwoPane";
    private static final String TWO_PANE_PARAM = "isTwoPane";
    private static final String SAVED_KEY_CURRENT_PROGRESS = "trackCurrentProgress";
    private static final String SAVED_KEY_MAX_PROGRESS = "trackMaxProgress";
    private static final String LOG_TAG = PlayerFragment.class.getSimpleName();
    private final IntentFilter broadcastReceiverIntent = new IntentFilter();
    boolean mBound = false;
    PlayerService mService;
    @Bind(R.id.player_artist_textView)
    TextView artistTextView;
    @Bind(R.id.player_album_imageView)
    ImageView albumImageView;
    @Bind(R.id.player_album_textView)
    TextView albumTextView;
    @Bind(R.id.player_track_textView)
    TextView trackTextView;
    @Bind(R.id.playing_time_textview)
    TextView playingTimeTextView;
    @Bind(R.id.finish_time_textview)
    TextView finishTimeTextView;
    @Bind(R.id.player_next_button)
    ImageButton nextButton;
    @Bind(R.id.player_play_button)
    ImageButton playButton;
    @Bind(R.id.player_previous_button)
    ImageButton previousButton;
    @Bind(R.id.player_seekBar)
    SeekBar seekBar;
    private boolean mTwoPane;
    private int mSelectedTrackPosition;
    private ParcelableTrack mSelectedTrack;
    private ArrayList<ParcelableTrack> mTrackList;
    private LocalBroadcastManager bManager;
    private ServiceUpdateReceiver serviceUpdateReceiver = new ServiceUpdateReceiver();
    private ShareActionProvider mShareActionProvider;
    private boolean mIsPlaying = false;

    //private OnFragmentInteractionListener mListener;
    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PlayerService.LocalBinder binder = (PlayerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            if (mSelectedTrack == null) {
                mSelectedTrack = mService.getCurrentTrack();
                initUIWithTrack(mSelectedTrack);
                initSeekBar(mService.getSongDuration() * 1000);
            }
            if (mSelectedTrack != null) {
                enableMediaControls(true);
                mService.initMediaPlayer(mSelectedTrack, true, mTrackList);
            }

            startPlayerService(null);

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public PlayerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PlayerFragment.
     */
    public static PlayerFragment newInstance(ArrayList<ParcelableTrack> tracks, int selectedPosition, boolean twoPane) {
        PlayerFragment fragment = new PlayerFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(TRACKS_PARAM, tracks);
        args.putInt(SELECTED_INDEX_PARAM, selectedPosition);
        args.putBoolean(TWO_PANE_PARAM, twoPane);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    /**
     * The system calls this only when creating the layout in a dialog.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    private void initUIWithTrack(ParcelableTrack selectedTrack) {
        artistTextView.setText(selectedTrack.getArtistName());
        albumTextView.setText(selectedTrack.getAlbumName());
        trackTextView.setText(selectedTrack.name);
        ParcelableImage parcelableImage = selectedTrack.getTrackImage();
        if (parcelableImage != null && parcelableImage.url != null) {
            Picasso.with(getActivity()).load(parcelableImage.url)
                    .error(R.drawable.error)
                    .placeholder(R.drawable.placeholder)
                    .into(albumImageView);
        }

        playingTimeTextView.setText("0:00");
        finishTimeTextView.setText("0:00");
    }

    private void initSeekBar(int max) {
        seekBar.setMax(max);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mBound && fromUser) {
                    mService.seekToPosition(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });
    }

    private void enableMediaControls(boolean enable) {
        playButton.setEnabled(enable);
        nextButton.setEnabled(enable);
        previousButton.setEnabled(enable);
        seekBar.setEnabled(enable);
    }

    @OnClick(R.id.player_play_button)
    public void onPlayPressed() {
        if (mIsPlaying) {
            startPlayerService(PlayerService.ACTION_PAUSE);
        } else {
            startPlayerService(PlayerService.ACTION_PLAY);
        }
    }

    //TODO: Not Working!!! Make same as play/pause
    @OnClick(R.id.player_previous_button)
    public void onPreviousPressed() {
        enableMediaControls(false);
        startPlayerService(PlayerService.ACTION_PREVIOUS);
    }

    //TODO: Not Working!!! Make same as play/pause
    @OnClick(R.id.player_next_button)
    public void onNextPressed() {
        enableMediaControls(false);
        startPlayerService(PlayerService.ACTION_NEXT);
    }

    private void startPlayerService(String action) {
        Intent intent = new Intent(getActivity(), PlayerService.class);
        intent.putExtra(PARAM_IS_TWO_PANE, mTwoPane);
        if (action != null) intent.setAction(action);
        getActivity().startService(intent);
    }

    private void initializeBroadcastReceiver() {
        try {
            bManager = LocalBroadcastManager.getInstance(getActivity());
            broadcastReceiverIntent.addAction(RECEIVER_ACTION_SET_UI_READY);
            broadcastReceiverIntent.addAction(RECEIVER_ACTION_SET_PLAY_BUTTON);
            broadcastReceiverIntent.addAction(RECEIVER_ACTION_UPDATE_SONG_PROGRESS);
            broadcastReceiverIntent.addAction(RECEIVER_ACTION_UPDATE_SONG_STATUS);
            bManager.registerReceiver(serviceUpdateReceiver, broadcastReceiverIntent);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        enableMediaControls(false);

        if (getArguments() != null) {
            mTrackList = getArguments().getParcelableArrayList(TRACKS_PARAM);
            if (mTrackList != null) {
                mSelectedTrackPosition = getArguments().getInt(SELECTED_INDEX_PARAM);
                mSelectedTrack = mTrackList.get(mSelectedTrackPosition);

                initUIWithTrack(mSelectedTrack);
            }
            mTwoPane = getArguments().getBoolean(TWO_PANE_PARAM);
        }

        // Bind to LocalService
        Intent intent = new Intent(getActivity(), PlayerService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }



    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAVED_KEY_CURRENT_PROGRESS, seekBar.getProgress());
        outState.putInt(SAVED_KEY_MAX_PROGRESS, seekBar.getMax());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        initializeBroadcastReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            bManager.unregisterReceiver(serviceUpdateReceiver);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to unregister: " + e.getMessage());
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // Unbind from the service
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.the_player_fragment, container, false);
        ButterKnife.bind(this, view);
        if (savedInstanceState != null) {
            seekBar.setMax(savedInstanceState.getInt(SAVED_KEY_MAX_PROGRESS));
            seekBar.setProgress(savedInstanceState.getInt(SAVED_KEY_CURRENT_PROGRESS));
        }
        return view;
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of track changes.
     */
    public interface TrackChanged {
        /**
         * Callback for when an item has been selected.
         */
        void trackChanged(ParcelableTrack track);
    }

    public class ServiceUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(RECEIVER_ACTION_SET_UI_READY)) {
                mSelectedTrack = intent.getExtras().getParcelable(RECEIVER_PARAM_CURRENT_TRACK);
                initUIWithTrack(mSelectedTrack);
                if (getActivity() != null)
                    ((TrackChanged) getActivity()).trackChanged(mSelectedTrack);
                enableMediaControls(true);
                initSeekBar(intent.getExtras().getInt(RECEIVER_PARAM_SONG_LENGTH));

            } else if (intent.getAction().equals(RECEIVER_ACTION_UPDATE_SONG_PROGRESS)) {
                double mCurrentPosition = intent.getExtras().getDouble(RECEIVER_PARAM_CURRENT_POSITION);
                int maxDuration = intent.getExtras().getInt(RECEIVER_PARAM_SONG_LENGTH);
                //Log.d("SEEKER", String.valueOf(mCurrentPosition));

                int secs = (int) Math.ceil(mCurrentPosition / 1000);
                //Log.d("SEEKER", String.valueOf(secs));
                if (secs >= maxDuration) {
                    secs = 0;
                    mCurrentPosition = 0;
                    seekBar.setMax(maxDuration * 1000);
                }
                playingTimeTextView.setText(String.format("0:%02d", secs));
                finishTimeTextView.setText(String.format("0:%02d", maxDuration - secs));
                seekBar.setProgress((int) mCurrentPosition);

            } else if (intent.getAction().equals(RECEIVER_ACTION_SET_PLAY_BUTTON)) {
                mIsPlaying = !intent.getExtras().getBoolean(RECEIVER_PARAM_PLAY_ICON);
                if (mIsPlaying) {
                    playButton.setImageResource(android.R.drawable.ic_media_pause);
                } else {
                    playButton.setImageResource(android.R.drawable.ic_media_play);
                }
            }

        }
    }



}
