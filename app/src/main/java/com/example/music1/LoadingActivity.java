package com.example.music1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.VideoView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class LoadingActivity extends AppCompatActivity {
    private static final String TAG = "LoadingActivity";
    private VideoView loadingVideo;
    private static final int LOADING_DURATION = 2500;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        Log.i(TAG, "onCreate: Iniciando LoadingActivity");

        loadingVideo = findViewById(R.id.loadingVideo);
        prefs = getSharedPreferences("music1", MODE_PRIVATE);

        try {
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.loading_video);
            loadingVideo.setVideoURI(videoUri);
            loadingVideo.setOnPreparedListener(mp -> {
                mp.setVolume(0f, 0f);
                loadingVideo.start();
                Log.d(TAG, "Video de loading iniciado");
            });
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error al cargar video", e);
            loadingVideo.setVisibility(android.view.View.GONE);
        }

        new Handler().postDelayed(() -> {
            String nombre = prefs.getString("nombre", "");
            int avatar = prefs.getInt("avatar", 0);
            Log.d(TAG, "postDelayed: nombre=" + nombre + ", avatar=" + avatar);

            Intent intent;
            if (nombre.isEmpty() || avatar == 0) {
                // Primera vez: pedir nombre y avatar
                intent = new Intent(LoadingActivity.this, MainActivity.class);
                Log.i(TAG, "Primera vez, navegando a MainActivity");
            } else {
                // Ya tiene nombre y avatar
                intent = new Intent(LoadingActivity.this, TipoMusicaActivity.class);
                Log.i(TAG, "Usuario registrado, navegando a TipoMusicaActivity");
            }

            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, LOADING_DURATION);
    }
}