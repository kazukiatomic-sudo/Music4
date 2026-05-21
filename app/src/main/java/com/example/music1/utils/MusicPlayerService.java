package com.example.music1.utils;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

public class MusicPlayerService extends Service {

    private final IBinder binder = new LocalBinder();

    private MediaPlayer mediaPlayer;

    private BroadcastReceiver toggleReceiver;

    public class LocalBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        toggleReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {

                if (mediaPlayer == null)
                    return;

                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.start();
                }
            }
        };

        registerReceiver(toggleReceiver,
                new IntentFilter("ACTION_TOGGLE_PLAY"));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void playMusic(String path) {

        try {

            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();

            mediaPlayer.setDataSource(path);

            mediaPlayer.prepare();

            mediaPlayer.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pauseMusic() {

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void resumeMusic() {

        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    public boolean isPlaying() {

        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (toggleReceiver != null) {
            unregisterReceiver(toggleReceiver);
        }

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}