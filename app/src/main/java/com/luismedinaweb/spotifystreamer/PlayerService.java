package com.luismedinaweb.spotifystreamer;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.ArrayList;

public class PlayerService extends Service implements MediaSessionCompat.OnActiveChangeListener, AudioManager.OnAudioFocusChangeListener {

    public static final String INCOMING_TRACK_URL = "incomingTrackURL";
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_REWIND = "action_rewind";
    public static final String ACTION_FAST_FORWARD = "action_fast_foward";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";
    public static final String ACTION_CHECK_RUNNING = "action_check_running";
    public static final String PARAM_SHOW_PLAYER_DIALOG = "showPlayerDialog";
    private static boolean isStarted;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    private MediaPlayer mMediaPlayer;
    private Thread updateUIThread = null;
    private ParcelableTrack mCurrentTrack;
    private int mSongDuration;
    private MediaSessionCompat mMediaSession;
    private MediaControllerCompat mController;
    private RemoteControlReceiver mRemoteControlReceiver;
    private int mServiceID;
    private Bitmap albumImageBitmap;
    private ArrayList<ParcelableTrack> mTrackList = new ArrayList<>();
    private int mTrackPosition;
    private PendingIntent mEnterIntent;

    public PlayerService() {
    }

    public static synchronized boolean isStarted() {
        return isStarted;
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

        mRemoteControlReceiver = new RemoteControlReceiver();

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), PlayerService.RemoteControlReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        ComponentName componentName = new ComponentName(getApplicationContext(), RemoteControlReceiver.class);
        mMediaSession = new MediaSessionCompat(getApplicationContext(), "mySession", componentName, pendingIntent);


        try {
            mController = new MediaControllerCompat(getApplicationContext(), mMediaSession.getSessionToken());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        mMediaSession.setCallback(new MediaSessionCompat.Callback() {
                                      @Override
                                      public void onPlay() {
                                          super.onPlay();
                                          Log.e("MediaPlayerService", "onPlay");
                                          showNotification(buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE)));
                                          startOrPausePlaying();
                                      }

                                      @Override
                                      public void onPause() {
                                          super.onPause();
                                          Log.e("MediaPlayerService", "onPause");
                                          showNotification(buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY)));
                                          startOrPausePlaying();
                                      }

                                      @Override
                                      public void onSkipToNext() {
                                          super.onSkipToNext();
                                          Log.e("MediaPlayerService", "onSkipToNext");
                                          //Change media here
                                          if (mTrackPosition < mTrackList.size() - 1) {
                                              Log.e("MediaPlayerService", "doSkip");
                                              mTrackPosition++;
                                              initMediaPlayer(mTrackList.get(mTrackPosition), false, null);
                                              //sendUIMakeReady(mMediaPlayer.getDuration());
                                              showNotification(buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE)));
                                          }
                                      }

                                      @Override
                                      public void onSkipToPrevious() {
                                          super.onSkipToPrevious();
                                          Log.e("MediaPlayerService", "onSkipToPrevious");
                                          //Change media here
                                          if (mTrackPosition > 0) {
                                              mTrackPosition--;
                                              initMediaPlayer(mTrackList.get(mTrackPosition), false, null);
                                              showNotification(buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE)));
                                          }
                                      }


                                      @Override
                                      public void onStop() {
                                          super.onStop();
                                          Log.e("MediaPlayerService", "onStop");
                                          //Stop media player here
                                          if (mMediaPlayer.isPlaying()) startOrPausePlaying();
                                          stopForeground(true);
                                      }

                                      @Override
                                      public void onSeekTo(long pos) {
                                          super.onSeekTo(pos);
                                      }

                                  }
        );

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
        replyIntent.putExtra(PlayerFragment.RECEIVER_PARAM_SONG_LENGTH, mSongDuration);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(replyIntent);
    }

    private void sendUIMakeReady(int duration) {
        Intent replyIntent = new Intent();
        replyIntent.setAction(PlayerFragment.RECEIVER_ACTION_SET_UI_READY);
        replyIntent.putExtra(PlayerFragment.RECEIVER_PARAM_SONG_LENGTH, duration);
        replyIntent.putExtra(PlayerFragment.RECEIVER_PARAM_CURRENT_TRACK, mCurrentTrack);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(replyIntent);
    }

    public void seekToPosition(int position) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(position);
        }
    }

    private boolean requestAudioFocus() {
        AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        int result = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                    new Intent(getApplicationContext(), PlayerService.RemoteControlReceiver.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            mMediaSession.setMediaButtonReceiver(pendingIntent);
            return true;
        }

        return false;
    }

    public int getSongDuration() {
        if (mMediaPlayer != null) {
            return mSongDuration;
        }
        return 0;
    }

    public void play() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mController.getTransportControls().pause();
            } else {
                mController.getTransportControls().play();
            }
        }
    }

    private void startOrPausePlaying() {
        if (mMediaPlayer != null) {
            if (updateUIThread != null) {
                updateUIThread.interrupt();
            }
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                setPlayButton(true);
            } else {
                if (requestAudioFocus()) {
                    mMediaPlayer.start();
                    setPlayButton(false);
                    updateUIThread = new Thread(new UpdateUIRunnable());
                    updateUIThread.start();
                }
            }
        }
    }

    public ParcelableTrack getCurrentTrack() {
        return mCurrentTrack;
    }

    public void initMediaPlayer(ParcelableTrack selectedTrack, boolean fromUI, ArrayList<ParcelableTrack> tracks) {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            WifiManager.WifiLock wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                    .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

            wifiLock.acquire();
        }

        if (selectedTrack == mCurrentTrack) {
            if (mMediaPlayer.isPlaying()) {
                setPlayButton(false);
            } else {
                setPlayButton(true);
                updatePlayerPosition(mMediaPlayer.getCurrentPosition());
            }
            return;
        } else {
            if (mMediaPlayer.isPlaying()) {
                startOrPausePlaying();
            }
            mMediaPlayer.reset();
        }
        mCurrentTrack = selectedTrack;
        if (tracks != null && fromUI) {
            mTrackList.clear();
            mTrackList.addAll(tracks);
        }
        mTrackPosition = mTrackList.indexOf(mCurrentTrack);
        try {
            mMediaPlayer.setDataSource(selectedTrack.preview_url);
            //albumImageDrawable = (BitmapDrawable) albumImage;
            showNotification(buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE)));
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mSongDuration = mp.getDuration() / 1000;

                    sendUIMakeReady(mp.getDuration());
                    startOrPausePlaying();


                }
            });

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mMediaPlayer.seekTo(0);
                    updatePlayerPosition(mMediaPlayer.getCurrentPosition());
                    setPlayButton(true);
                    showNotification(buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY)));
                }
            });

            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {

                }
            });

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    switch (what) {
                        case MediaPlayer.MEDIA_ERROR_IO:
                            Toast.makeText(getApplicationContext(), "Spotify Streamer: I/O error", Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
            });

            mMediaPlayer.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null)
            return;

        String action = intent.getAction();

        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            mController.getTransportControls().play();
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            mController.getTransportControls().pause();
        } else if (action.equalsIgnoreCase(ACTION_FAST_FORWARD)) {
            mController.getTransportControls().fastForward();
        } else if (action.equalsIgnoreCase(ACTION_REWIND)) {
            mController.getTransportControls().rewind();
        } else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
            mController.getTransportControls().skipToPrevious();
        } else if (action.equalsIgnoreCase(ACTION_NEXT)) {
            mController.getTransportControls().skipToNext();
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            mController.getTransportControls().stop();
        }
    }

    private NotificationCompat.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), PlayerService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new NotificationCompat.Action.Builder(icon, title, pendingIntent).build();
    }

    private Notification buildNotification(NotificationCompat.Action action) {


        android.support.v4.app.NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(mCurrentTrack.name)
                .setContentText(mCurrentTrack.getArtistName())
                .setContentIntent(mEnterIntent)
                        //.setLargeIcon(null)
                .addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS))
                .addAction(action)
                .addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT))
                .addAction(generateAction(android.R.drawable.ic_delete, "Stop", ACTION_STOP));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (prefs.getBoolean(getString(R.string.pref_notifications_key), true)) {
            builder.setStyle(new NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mMediaSession.getSessionToken()));
        }

        if (mCurrentTrack.getTrackImage() != null && mCurrentTrack.getTrackImage().url != null) {
            Target target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    albumImageBitmap = bitmap;
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {

                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }

            };
            Picasso.with(this).load(mCurrentTrack.getTrackImage().url).into(target);
        }

        builder.setLargeIcon(albumImageBitmap);
        return builder.build();
    }

    private void showNotification(Notification notification) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO: Make MainActivity show Dialog with current track in 2 pane mode
        if (intent.getBooleanExtra(PlayerFragment.PARAM_IS_TWO_PANE, true)) {
            Intent mainActivityIntent = new Intent(getApplicationContext(), MainActivity.class);
            mainActivityIntent.putExtra(PARAM_SHOW_PLAYER_DIALOG, true);
            mEnterIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                    mainActivityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            mEnterIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                    new Intent(getApplicationContext(), PlayerActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        mServiceID = startId;
        if (!isStarted()) {
            startForeground(1, buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE)));
            isStarted = true;
        } else {
            handleIntent(intent);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(getApplicationContext(), "service destroying", Toast.LENGTH_SHORT).show();
        if (mMediaPlayer != null) mMediaPlayer.release();
        if (mMediaSession != null) mMediaSession.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public void onActiveChanged() {

    }

    @Override
    public void onAudioFocusChange(int focusChange) {

    }

    //TODO: Check functionality when hitting play/pause constantly
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

    public class RemoteControlReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (KeyEvent.KEYCODE_MEDIA_PLAY == event.getKeyCode()) {
                    Toast.makeText(context, "Key pressed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
