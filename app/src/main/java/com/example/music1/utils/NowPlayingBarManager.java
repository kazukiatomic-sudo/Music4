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
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.music1.R;
import com.example.music1.models.Cancion;
import com.example.music1.utils.FavoritosManager;
import com.example.music1.models.Favorito;

public class NowPlayingBarManager {
    private static NowPlayingBarManager instance;
    private LinearLayout nowPlayingBar;
    private ImageView ivCover;
    private TextView tvTitle, tvArtist;
    private ImageButton btnPlayPause, btnNext, btnPrev, btnShuffle, btnRepeat;
    private ImageButton btnFavorite, btnClose;
    private Activity currentActivity;
    private Cancion currentSong;
    private boolean isPlaying = false;
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    private boolean receiverRegistered = false;

    private BroadcastReceiver updateReceiver;

    private NowPlayingBarManager() {}

    public static synchronized NowPlayingBarManager getInstance() {
        if (instance == null) instance = new NowPlayingBarManager();
        return instance;
    }

    public void init(Activity activity) {
        // Desregistrar receiver de la activity anterior si existe
        if (receiverRegistered && currentActivity != null && !currentActivity.isDestroyed()) {
            try {
                LocalBroadcastManager.getInstance(currentActivity).unregisterReceiver(updateReceiver);
            } catch (Exception ignored) {}
            receiverRegistered = false;
        }

        this.currentActivity = activity;

        nowPlayingBar  = activity.findViewById(R.id.nowPlayingBar);
        ivCover        = activity.findViewById(R.id.ivNowPlayingCover);
        tvTitle        = activity.findViewById(R.id.tvNowPlayingTitle);
        tvArtist       = activity.findViewById(R.id.tvNowPlayingArtist);
        btnPlayPause   = activity.findViewById(R.id.btnNowPlayingPlayPause);
        btnNext        = activity.findViewById(R.id.btnNowPlayingNext);
        btnPrev        = activity.findViewById(R.id.btnNowPlayingPrev);
        btnShuffle     = activity.findViewById(R.id.btnNowPlayingShuffle);
        btnRepeat      = activity.findViewById(R.id.btnNowPlayingRepeat);
        btnFavorite    = activity.findViewById(R.id.btnNowPlayingFavorite);
        btnClose       = activity.findViewById(R.id.btnNowPlayingClose);

        if (nowPlayingBar == null) return;

        // Tap en la barra → abrir reproductor completo
        nowPlayingBar.setOnClickListener(v -> abrirReproductorCompleto());

        // Play / Pause
        btnPlayPause.setOnClickListener(v -> {
            Intent ctrl = new Intent("REPRODUCTOR_CONTROL");
            ctrl.putExtra("action", "PLAY_PAUSE");
            if (!trySendBroadcast(ctrl)) {
                MusicPlayerService service = MusicPlayerService.getInstance();
                if (service != null) service.playPause();
                else abrirReproductorCompleto();
            }
        });

        // Siguiente
        btnNext.setOnClickListener(v -> {
            Intent ctrl = new Intent("REPRODUCTOR_CONTROL");
            ctrl.putExtra("action", "NEXT");
            if (!trySendBroadcast(ctrl)) {
                MusicPlayerService service = MusicPlayerService.getInstance();
                if (service != null) service.next();
            }
        });

        // Anterior
        btnPrev.setOnClickListener(v -> {
            Intent ctrl = new Intent("REPRODUCTOR_CONTROL");
            ctrl.putExtra("action", "PREV");
            if (!trySendBroadcast(ctrl)) {
                MusicPlayerService service = MusicPlayerService.getInstance();
                if (service != null) service.previous();
            }
        });

        // Aleatorio
        btnShuffle.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            btnShuffle.setAlpha(isShuffle ? 1.0f : 0.5f);
            Intent ctrl = new Intent("REPRODUCTOR_CONTROL");
            ctrl.putExtra("action", "SHUFFLE");
            ctrl.putExtra("value", isShuffle);
            trySendBroadcast(ctrl);
            Toast.makeText(activity,
                isShuffle ? "Aleatorio activado" : "Aleatorio desactivado",
                Toast.LENGTH_SHORT).show();
        });

        // Repetir
        btnRepeat.setOnClickListener(v -> {
            isRepeat = !isRepeat;
            btnRepeat.setAlpha(isRepeat ? 1.0f : 0.5f);
            Intent ctrl = new Intent("REPRODUCTOR_CONTROL");
            ctrl.putExtra("action", "REPEAT");
            ctrl.putExtra("value", isRepeat);
            trySendBroadcast(ctrl);
            Toast.makeText(activity,
                isRepeat ? "Repetir activado" : "Repetir desactivado",
                Toast.LENGTH_SHORT).show();
        });

        // Favorito
        btnFavorite.setOnClickListener(v -> {
            if (currentSong == null) return;
            FavoritosManager fm = FavoritosManager.getInstance(activity);
            Favorito fav = new Favorito(
                currentSong.getId(), currentSong.getTitulo(), currentSong.getArtista(),
                currentSong.getAlbum(), currentSong.getDuracion(),
                currentSong.getUriString(), currentSong.getAlbumId());
            if (fm.esFavorito(fav.getId(), fav.getUri())) {
                fm.quitarFavorito(fav.getId(), fav.getUri());
                btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                Toast.makeText(activity, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
            } else {
                fm.agregarFavorito(fav);
                btnFavorite.setImageResource(R.drawable.ic_favorite);
                Toast.makeText(activity, "❤ Agregado a favoritos", Toast.LENGTH_SHORT).show();
            }
        });

        // Cerrar barra
        btnClose.setOnClickListener(v -> {
            ocultar();
            // Detener reproducción si no hay servicio activo
            MusicPlayerService service = MusicPlayerService.getInstance();
            if (service != null) service.detenerReproduccion();
            Intent ctrl = new Intent("REPRODUCTOR_CONTROL");
            ctrl.putExtra("action", "STOP");
            trySendBroadcast(ctrl);
        });

        setupBroadcastReceiver();
        sincronizarConServicio();
    }

    private void setupBroadcastReceiver() {
        if (currentActivity == null || currentActivity.isDestroyed()) return;

        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!"NOW_PLAYING_UPDATE".equals(intent.getAction())) return;
                if (currentActivity == null || currentActivity.isDestroyed()
                        || currentActivity.isFinishing()) return;
                Cancion cancion = (Cancion) intent.getSerializableExtra("cancion");
                boolean playing = intent.getBooleanExtra("isPlaying", false);
                actualizarBarra(cancion, playing);
            }
        };

        LocalBroadcastManager.getInstance(currentActivity)
            .registerReceiver(updateReceiver, new IntentFilter("NOW_PLAYING_UPDATE"));
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
        if (portada != null) ivCover.setImageBitmap(portada);
        else ivCover.setImageResource(cancion.getImagenAlbum());

        btnPlayPause.setImageResource(estaReproduciendo ? R.drawable.ic_pause : R.drawable.ic_play);

        // Actualizar icono favorito
        if (btnFavorite != null) {
            FavoritosManager fm = FavoritosManager.getInstance(currentActivity);
            btnFavorite.setImageResource(
                fm.esFavorito(cancion.getId(), cancion.getUriString())
                    ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        }
    }

    public void actualizarEstado(boolean estaReproduciendo) {
        this.isPlaying = estaReproduciendo;
        if (btnPlayPause != null) {
            btnPlayPause.setImageResource(estaReproduciendo ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    public void ocultar() {
        if (nowPlayingBar != null) nowPlayingBar.setVisibility(View.GONE);
    }

    private void abrirReproductorCompleto() {
        if (currentSong == null || currentActivity == null) return;
        Intent intent = new Intent(currentActivity, com.example.music1.ReproductorLocalActivity.class);
        intent.putExtra("cancion_serializada", currentSong);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        currentActivity.startActivity(intent);
    }

    private boolean trySendBroadcast(Intent intent) {
        if (currentActivity == null || currentActivity.isDestroyed()) return false;
        try {
            LocalBroadcastManager.getInstance(currentActivity).sendBroadcast(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

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
        btnPrev = null;
        btnShuffle = null;
        btnRepeat = null;
        btnFavorite = null;
        btnClose = null;
    }

    public void sincronizarConServicio() {
        MusicPlayerService service = MusicPlayerService.getInstance();
        if (service != null) actualizarBarra(service.getCurrentSong(), service.isMusicPlaying());
    }
}
