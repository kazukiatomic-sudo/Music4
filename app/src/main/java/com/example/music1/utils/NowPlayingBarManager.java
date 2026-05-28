package com.example.music1.utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.music1.R;
import com.example.music1.models.Cancion;

public class NowPlayingBarManager {
    private static NowPlayingBarManager instance;
    private LinearLayout nowPlayingBar;
    private ImageView ivCover;
    private TextView tvTitle, tvArtist;
    private ImageButton btnPlayPause, btnNext;
    private Activity currentActivity;
    private Cancion currentSong;
    private boolean isPlaying = false;
    private boolean receiverRegistered = false; // FIX: rastrear si el receiver está registrado

    private BroadcastReceiver updateReceiver;

    private NowPlayingBarManager() {}

    public static synchronized NowPlayingBarManager getInstance() {
        if (instance == null) {
            instance = new NowPlayingBarManager();
        }
        return instance;
    }

    public void init(Activity activity) {
        // FIX Crítico 2: Si ya había una activity anterior, desregistrar su receiver primero
        // Esto evita la fuga de BroadcastReceiver cuando se navega entre actividades
        if (receiverRegistered && currentActivity != null && !currentActivity.isDestroyed()) {
            try {
                LocalBroadcastManager.getInstance(currentActivity).unregisterReceiver(updateReceiver);
            } catch (Exception ignored) {}
            receiverRegistered = false;
        }

        this.currentActivity = activity;

        nowPlayingBar = activity.findViewById(R.id.nowPlayingBar);
        ivCover = activity.findViewById(R.id.ivNowPlayingCover);
        tvTitle = activity.findViewById(R.id.tvNowPlayingTitle);
        tvArtist = activity.findViewById(R.id.tvNowPlayingArtist);
        btnPlayPause = activity.findViewById(R.id.btnNowPlayingPlayPause);
        btnNext = activity.findViewById(R.id.btnNowPlayingNext);

        if (nowPlayingBar == null) return;

        nowPlayingBar.setOnClickListener(v -> abrirReproductorCompleto());

        btnPlayPause.setOnClickListener(v -> {
            // FIX Alto 2: Enviar broadcast para que la Activity controle su propio MediaPlayer
            // Esto evita que el servicio cree un segundo MediaPlayer sobre el de la Activity
            android.content.Intent controlIntent = new android.content.Intent("REPRODUCTOR_CONTROL");
            controlIntent.putExtra("action", "PLAY_PAUSE");
            boolean enviado = trySendBroadcast(controlIntent);
            if (!enviado) {
                // Si la Activity no está activa, controlar desde el servicio
                MusicPlayerService service = MusicPlayerService.getInstance();
                if (service != null) service.playPause();
                else abrirReproductorCompleto();
            }
        });

        btnNext.setOnClickListener(v -> {
            android.content.Intent controlIntent = new android.content.Intent("REPRODUCTOR_CONTROL");
            controlIntent.putExtra("action", "NEXT");
            boolean enviado = trySendBroadcast(controlIntent);
            if (!enviado) {
                MusicPlayerService service = MusicPlayerService.getInstance();
                if (service != null) service.next();
            }
        });

        setupBroadcastReceiver();

        // Sincronizar estado actual si ya había música reproduciéndose
        sincronizarConServicio();
    }

    private void setupBroadcastReceiver() {
        if (currentActivity == null || currentActivity.isDestroyed()) return;

        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("NOW_PLAYING_UPDATE".equals(intent.getAction())) {
                    // FIX: verificar que la activity sigue viva antes de actualizar UI
                    if (currentActivity == null || currentActivity.isDestroyed()
                            || currentActivity.isFinishing()) return;
                    Cancion cancion = (Cancion) intent.getSerializableExtra("cancion");
                    boolean playing = intent.getBooleanExtra("isPlaying", false);
                    actualizarBarra(cancion, playing);
                }
            }
        };

        IntentFilter filter = new IntentFilter("NOW_PLAYING_UPDATE");
        LocalBroadcastManager.getInstance(currentActivity).registerReceiver(updateReceiver, filter);
        receiverRegistered = true;
    }

    public void actualizarBarra(Cancion cancion, boolean estaReproduciendo) {
        if (nowPlayingBar == null) return;
        if (currentActivity == null || currentActivity.isDestroyed()) return;

        this.currentSong = cancion;
        this.isPlaying = estaReproduciendo;

        if (cancion == null) {
            nowPlayingBar.setVisibility(View.GONE);
            return;
        }

        nowPlayingBar.setVisibility(View.VISIBLE);

        String titulo = cancion.getTituloMostrable(currentActivity);
        String artista = cancion.getArtistaMostrable(currentActivity);

        tvTitle.setText(titulo != null ? titulo : "Sin título");
        tvArtist.setText(artista != null ? artista : "Artista desconocido");

        Bitmap portada = TagEditorManager.getInstance(currentActivity)
                .getPortada(cancion.getId(), cancion.getUriString());
        if (portada != null) {
            ivCover.setImageBitmap(portada);
        } else {
            ivCover.setImageResource(cancion.getImagenAlbum());
        }

        btnPlayPause.setImageResource(estaReproduciendo ?
                R.drawable.ic_pause : R.drawable.ic_play);
    }

    public void actualizarEstado(boolean estaReproduciendo) {
        this.isPlaying = estaReproduciendo;
        if (btnPlayPause != null) {
            btnPlayPause.setImageResource(estaReproduciendo ?
                    R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    public void ocultar() {
        if (nowPlayingBar != null) {
            nowPlayingBar.setVisibility(View.GONE);
        }
    }

    private void abrirReproductorCompleto() {
        if (currentSong == null || currentActivity == null) return;
        Intent intent = new Intent(currentActivity, com.example.music1.ReproductorLocalActivity.class);
        intent.putExtra("cancion_serializada", currentSong);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        currentActivity.startActivity(intent);
    }

    private boolean trySendBroadcast(android.content.Intent intent) {
        if (currentActivity == null || currentActivity.isDestroyed()) return false;
        try {
            androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(currentActivity).sendBroadcast(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // FIX Crítico 2: onDestroy limpia el receiver correctamente
    public void onDestroy() {
        if (receiverRegistered && currentActivity != null) {
            try {
                LocalBroadcastManager.getInstance(currentActivity).unregisterReceiver(updateReceiver);
            } catch (Exception ignored) {}
            receiverRegistered = false;
        }
        currentActivity = null;
        nowPlayingBar = null;
        ivCover = null;
        tvTitle = null;
        tvArtist = null;
        btnPlayPause = null;
        btnNext = null;
    }

    public void sincronizarConServicio() {
        MusicPlayerService service = MusicPlayerService.getInstance();
        if (service != null) {
            actualizarBarra(service.getCurrentSong(), service.isMusicPlaying());
        }
    }
}
