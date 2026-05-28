package com.example.music1;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private VideoView videoView;
    private ImageView btnIniciar;
    private TextView txtTitulo, txtSubtitulo;
    private MediaPlayer mediaPlayer;
    private Animation rotateAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Log.i(TAG, "onCreate: Iniciando SplashActivity");

        videoView = findViewById(R.id.videoView);
        btnIniciar = findViewById(R.id.btnIniciar);
        txtTitulo = findViewById(R.id.txtTitulo);
        txtSubtitulo = findViewById(R.id.txtSubtitulo);

        try {
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.splash_video);
            videoView.setVideoURI(videoUri);

            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mp.setVolume(0f, 0f);
                videoView.start();
                Log.d(TAG, "Video de splash iniciado");
                iniciarAnimacionesEntrada();
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Error en video de splash");
                videoView.setVisibility(View.GONE);
                iniciarAnimacionesEntrada();
                return true;
            });
        } catch (Exception e) {
            Log.e(TAG, "Error al configurar video", e);
            videoView.setVisibility(View.GONE);
            iniciarAnimacionesEntrada();
        }

        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.splash_music);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(0.3f, 0.3f);
                mediaPlayer.start();
                Log.d(TAG, "Musica de splash iniciada");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error al cargar musica de splash", e);
        }

        rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_infinite);

        btnIniciar.setOnClickListener(v -> {
            Log.d(TAG, "btnIniciar click");
            detenerMusicaSplash();
            v.clearAnimation();

            v.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();

                        new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            Intent intent = new Intent(SplashActivity.this, LoadingActivity.class);
                            startActivity(intent);
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            finish();
                        }, 200);
                    })
                    .start();
        });
    }

    private void detenerMusicaSplash() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            Log.d(TAG, "Música de splash detenida");
        }
    }

    private void iniciarAnimacionesEntrada() {
        ObjectAnimator animTitulo = ObjectAnimator.ofFloat(txtTitulo, "alpha", 0f, 1f);
        animTitulo.setDuration(1000);
        animTitulo.setStartDelay(500);
        animTitulo.setInterpolator(new AccelerateDecelerateInterpolator());
        animTitulo.start();

        ObjectAnimator animSubtitulo = ObjectAnimator.ofFloat(txtSubtitulo, "alpha", 0f, 1f);
        animSubtitulo.setDuration(1000);
        animSubtitulo.setStartDelay(800);
        animSubtitulo.setInterpolator(new AccelerateDecelerateInterpolator());
        animSubtitulo.start();

        btnIniciar.setAlpha(0f);

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 0.3f, 1.1f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 0.3f, 1.1f, 1f);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);

        ObjectAnimator animBoton = ObjectAnimator.ofPropertyValuesHolder(btnIniciar, scaleX, scaleY, alpha);
        animBoton.setDuration(1200);
        animBoton.setStartDelay(1200);
        animBoton.setInterpolator(new AccelerateDecelerateInterpolator());
        animBoton.start();

        btnIniciar.postDelayed(() -> {
            btnIniciar.startAnimation(rotateAnimation);
        }, 1300);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) videoView.pause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView != null) videoView.start();
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) mediaPlayer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detenerMusicaSplash();
        if (btnIniciar != null) {
            btnIniciar.clearAnimation();
        }
    }
}