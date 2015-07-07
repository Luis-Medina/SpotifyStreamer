package com.luismedinaweb.spotifystreamer;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import java.io.IOException;

public class PlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    public static final String INCOMING_TRACK_URL = "incomingTrackURL";
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    private MediaPlayer mMediaPlayer;
    private boolean isStarted;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private String mCurrentTrackUrl;
    private Handler mHandler = new Handler();
    private Thread updateUIThread = null;
    private ParcelableTrack mCurrentTrack;
    private MediaSession mMediaSession;

    public PlayerService() {
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
//        HandlerThread thread = new HandlerThread("ServiceStartArguments",
//                Process.THREAD_PRIORITY_BACKGROUND);
//        thread.start();
//
//        // Get the HandlerThread's Looper and use it for our Handler
//        mServiceLooper = thread.getLooper();
//        mServiceHandler = new ServiceHandler(mServiceLooper);

    }

    private void setPlayButton(boolean setToPlay) {
        Intent replyIntent = new Intent();
        replyIntent.setAction(PlayerFragment.RECEIVER_ACTION_SET_PLAY_BUTTON);
        replyIntent.putExtra(PlayerFragment.RECEIVER_PARAM_PLAY_ICON, setToPlay);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(replyIntent);
    }

    private void updatePlayerPosition(double position) {
        Intent replyIntent = new Intent();
        replyIntent.setAction(PlayerFragment.RECEIVER_ACTION_UPDATE_SONG_PROGRESS);
        replyIntent.putExtra(PlayerFragment.RECEIVER_PARAM_CURRENT_POSITION, position);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(replyIntent);
    }

    public void seekToPosition(int position) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(position);
        }
    }

    public void startOrPausePlaying() {
        if (mMediaPlayer != null) {
            if (updateUIThread != null) {
                updateUIThread.interrupt();
            }
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                setPlayButton(true);

            } else {
                mMediaPlayer.start();
                setPlayButton(false);
                updateUIThread = new Thread(new UpdateUIRunnable());
                updateUIThread.start();


//            getActivity().runOnUiThread(new Runnable() {
//
//                @Override
//                public void run() {
//                    if(mMediaPlayer != null){
//                        if(mMediaPlayer.isPlaying()){
//                            double mCurrentPosition = mMediaPlayer.getCurrentPosition();
//                            Log.d("SEEKER", String.valueOf(mCurrentPosition));
//                            seekBar.setProgress((int)mCurrentPosition);
//                            int secs = (int) Math.ceil(mCurrentPosition/1000);
//                            Log.d("SEEKER", String.valueOf(secs));
//                            playingTimeTextView.setText(String.format("0:%02d", secs));
//                            finishTimeTextView.setText(String.format("0:%02d", 30 - secs));
//                        }else{
//                            mHandler.removeCallbacks(this);
//                            seekBar.setProgress(0);
//                            playingTimeTextView.setText("0:00");
//                            finishTimeTextView.setText("0:30");
//                        }
//                    }
//                    mHandler.postDelayed(this, 1000);
//                }
//            });

            }
        }
    }

    public void initMediaPlayer(ParcelableTrack selectedTrack) {
        mMediaPlayer = new MediaPlayer();
        mCurrentTrack = selectedTrack;
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        WifiManager.WifiLock wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        wifiLock.acquire();
        try {
            mMediaPlayer.setDataSource(selectedTrack.preview_url);

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Intent replyIntent = new Intent();
                    replyIntent.setAction(PlayerFragment.RECEIVER_ACTION_SET_UI_READY);
                    replyIntent.putExtra(PlayerFragment.RECEIVER_PARAM_SONG_LENGTH, mp.getDuration());
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(replyIntent);
                }
            });

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    updatePlayerPosition(0);
                    setPlayButton(true);
                }
            });

            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {

                }
            });

            mMediaPlayer.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public synchronized boolean isStarted() {
        return isStarted;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
//                new Intent(getApplicationContext(), MainActivity.class),
//                PendingIntent.FLAG_UPDATE_CURRENT);
//        Notification notification = new Notification();
//        notification.tickerText = "Playing ticker";
//        notification.icon = R.drawable.abc_btn_radio_material;
//        notification.flags |= Notification.FLAG_ONGOING_EVENT;
//        notification.setLatestEventInfo(getApplicationContext(), "MusicPlayerSample",
//                "Playing: ", pi);

//                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
//                new Intent(getApplicationContext(), PlayerActivity.class),
//                PendingIntent.FLAG_UPDATE_CURRENT);
//
//        NotificationCompat notification = new NotificationCompat.Builder(getApplicationContext())
//                // Show controls on lock screen even when user hides sensitive content.
//                .setVisibility(Notification.VISIBILITY_PUBLIC)
//                .setSmallIcon(R.drawable.notification)
//                        // Add media control buttons that invoke intents in your media service
//                .addAction(android.R.drawable.ic_media_previous, "Previous", pendingIntent) // #0
//                .addAction(android.R.drawable.ic_media_pause, "Pause", pendingIntent)  // #1
//                .addAction(android.R.drawable.ic_media_next, "Next", pendingIntent)     // #2
//                .setContentTitle("Wonderful music")
//                .setContentText("My Awesome Band")
//                .setLargeIcon(null)
//                        // Apply the media style template
//                .setStyle(new NotificationCompat.
//                        .setShowActionsInCompactView(new int[]{0, 1, 2})
//                        .setMediaSession(mMediaSession.getSessionToken()))
//                        .build();
//
//
//
//
//        startForeground(1, notification);
        isStarted = true;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service destroying", Toast.LENGTH_SHORT).show();
        if (mMediaPlayer != null) mMediaPlayer.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

    }

    private final class UpdateUIRunnable implements Runnable {

        @Override
        public void run() {
            double mCurrentPosition = 0;
            boolean cancelled = false;
            double maxDuration = mMediaPlayer.getDuration() - 1000;
            while (mCurrentPosition < maxDuration && !cancelled) {
                synchronized (this) {
                    mCurrentPosition = mMediaPlayer.getCurrentPosition();
                    try {
                        updatePlayerPosition(mCurrentPosition);
                        wait(1000);
                    } catch (InterruptedException e) {
                        cancelled = true;
                    }
                }
            }
        }
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        PlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PlayerService.this;
        }
    }
}
