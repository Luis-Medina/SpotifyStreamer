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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String TRACKS_PARAM = "tracks";
    public static final String SELECTED_INDEX_PARAM = "selectedTrackPos";
    public static final String RECEIVER_ACTION_SET_UI_READY = "setUIReady";
    public static final String RECEIVER_ACTION_UPDATE_SONG_PROGRESS = "updateSongProgress";
    public static final String RECEIVER_ACTION_SET_PLAY_BUTTON = "setButtonToPlay";
    public static final String RECEIVER_ACTION_UPDATE_SONG_STATUS = "setSongStatus";
    public static final String RECEIVER_PARAM_SONG_LENGTH = "songLength";
    public static final String RECEIVER_PARAM_PLAY_ICON = "playIcon";
    public static final String RECEIVER_PARAM_CURRENT_POSITION = "currentPosition";
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
    private int mSelectedTrackPosition;
    private ParcelableTrack mSelectedTrack;
    private ArrayList<ParcelableTrack> mTrackList;
    private LocalBroadcastManager bManager;
    private ServiceUpdateReceiver serviceUpdateReceiver = new ServiceUpdateReceiver();
    private boolean mInfoFromService = false;

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
                initSeekBar(mService.getSongDuration());
            }
            if (mSelectedTrack != null) {
                enableMediaControls(true);
                mService.initMediaPlayer(mSelectedTrack, true);
            }

            getActivity().startService(new Intent(getActivity(), PlayerService.class));
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
    // TODO: Rename and change types and number of parameters
    public static PlayerFragment newInstance(ArrayList<ParcelableTrack> tracks, int selectedPosition) {
        PlayerFragment fragment = new PlayerFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(TRACKS_PARAM, tracks);
        args.putInt(SELECTED_INDEX_PARAM, selectedPosition);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        if (parcelableImage != null) {
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
        if (mBound) {
//            if (!mService.isStarted()) {
//                getActivity().startService(new Intent(getActivity(), PlayerService.class));
//            }
            mService.play();
        }
    }

    @OnClick(R.id.player_previous_button)
    public void onPreviousPressed() {
        if (mBound) {
            Toast.makeText(getActivity(), "Previous pressed", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.player_next_button)
    public void onNextPressed() {
        if (mBound) {
            Toast.makeText(getActivity(), "Next pressed", Toast.LENGTH_SHORT).show();
        }
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

        //Resize dialog when in two pane mode
        if (getDialog() != null) {
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getDialog().getWindow().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

            int height = displaymetrics.heightPixels;
            int width = displaymetrics.widthPixels;
            if (width < height) {
                width = (int) (width * 0.9);
                height = (int) (height * 0.7);
            } else {
                width = (int) (width * 0.7);
                height = (int) (height * 0.9);
            }

            getDialog().getWindow().setLayout(width, height);
        }

        enableMediaControls(false);

        if (getArguments() != null) {
            mTrackList = getArguments().getParcelableArrayList(TRACKS_PARAM);
            if (mTrackList != null) {
                mSelectedTrackPosition = getArguments().getInt(SELECTED_INDEX_PARAM);
                mSelectedTrack = mTrackList.get(mSelectedTrackPosition);

                initUIWithTrack(mSelectedTrack);
            }
        }

        // Bind to LocalService
        Intent intent = new Intent(getActivity(), PlayerService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

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
        View view = inflater.inflate(R.layout.fragment_player, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    public class ServiceUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(RECEIVER_ACTION_SET_UI_READY)) {
                enableMediaControls(true);
                initSeekBar(intent.getExtras().getInt(RECEIVER_PARAM_SONG_LENGTH));
//                if(mService != null){
//                    getActivity().startService(new Intent(getActivity(), PlayerService.class));
//                    //mService.startOrPausePlaying();
//                }else{
//                    Toast.makeText(context, "No service to play", Toast.LENGTH_SHORT).show();
//                }
            } else if (intent.getAction().equals(RECEIVER_ACTION_UPDATE_SONG_PROGRESS)) {
                double mCurrentPosition = intent.getExtras().getDouble(RECEIVER_PARAM_CURRENT_POSITION);
                //Log.d("SEEKER", String.valueOf(mCurrentPosition));
                seekBar.setProgress((int) mCurrentPosition);
                int secs = (int) Math.ceil(mCurrentPosition / 1000);
                //Log.d("SEEKER", String.valueOf(secs));
                playingTimeTextView.setText(String.format("0:%02d", secs));
                finishTimeTextView.setText(String.format("0:%02d", 30 - secs));
            } else if (intent.getAction().equals(RECEIVER_ACTION_SET_PLAY_BUTTON)) {
                boolean setToPlay = intent.getExtras().getBoolean(RECEIVER_PARAM_PLAY_ICON);
                if (setToPlay) {
                    playButton.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    playButton.setImageResource(android.R.drawable.ic_media_pause);
                }
            }

        }
    }

    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }
//
//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }
//
//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
//    public interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
//        public void onFragmentInteraction(Uri uri);
//    }

}
