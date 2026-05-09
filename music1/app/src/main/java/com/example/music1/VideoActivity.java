package com.example.music1;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.VideoView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class VideoActivity extends AppCompatActivity {
    private static final String TAG = "VideoActivity";
    private VideoView videoView;
    private Button btnDetener;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        videoView = findViewById(R.id.videoView);
        btnDetener = findViewById(R.id.btnDetener);

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video_fondo);
        videoView.setVideoURI(videoUri);

        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            videoView.start();
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(this, "Error al reproducir video", Toast.LENGTH_SHORT).show();
            return true;
        });

        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.musica_fondo);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar musica", e);
            Toast.makeText(this, "Error al cargar música", Toast.LENGTH_SHORT).show();
        }

        btnDetener.setOnClickListener(v -> {
            detenerTodoCompletamente();
            startActivity(new Intent(VideoActivity.this, MainActivity.class));
            finish();
        });
    }

    private void detenerTodoCompletamente() {
        if (videoView != null) {
            if (videoView.isPlaying()) {
                videoView.stopPlayback();
            }
            videoView.suspend();
        }

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            detenerTodoCompletamente();
        } else {
            if (videoView != null && videoView.isPlaying()) {
                videoView.pause();
            }
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView != null && !videoView.isPlaying()) {
            videoView.start();
        }
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
            videoView = null;
        }

        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "onDestroy: Error liberando mediaPlayer", e);
            } finally {
                mediaPlayer = null;
            }
        }
    }
}