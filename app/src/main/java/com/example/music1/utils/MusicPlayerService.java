package com.example.music1.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.music1.R;
import com.example.music1.ReproductorLocalActivity;
import com.example.music1.models.Cancion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayerService extends Service {

    private static final String TAG = "MusicPlayerService";
    private static final String CHANNEL_ID = "MusicPlayerChannel";
    private static final int NOTIFICATION_ID = 1;

    // ✅ Singleton instance
    private static MusicPlayerService instance;

    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;
    private Cancion cancionActual;
    private List<Cancion> playlistActual = new ArrayList<>();
    private int posicionActual = 0;
    private boolean isPlaying = false;

    private final IBinder binder = new MusicBinder();

    public class MusicBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    // ✅ Obtener instancia del servicio
    public static MusicPlayerService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        crearCanalNotificacion();

        // Inicializar MediaSession
        mediaSession = new MediaSessionCompat(this, "MusicPlayerSession");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                reanudarReproduccion();
            }

            @Override
            public void onPause() {
                pausarReproduccion();
            }

            @Override
            public void onSkipToNext() {
                siguienteCancion();
            }

            @Override
            public void onSkipToPrevious() {
                cancionAnterior();
            }

            @Override
            public void onStop() {
                detenerReproduccion();
                stopSelf();
            }
        });
        mediaSession.setActive(true);

        Log.d(TAG, "MusicPlayerService creado y MediaSession activado");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("cancion_serializada")) {
            Serializable obj = intent.getSerializableExtra("cancion_serializada");
            if (obj instanceof Cancion) {
                cancionActual = (Cancion) obj;
                cancionActual.restaurarUri();

                if (intent.hasExtra("lista_canciones")) {
                    Serializable listObj = intent.getSerializableExtra("lista_canciones");
                    if (listObj instanceof List<?>) {
                        playlistActual = (List<Cancion>) listObj;
                        posicionActual = intent.getIntExtra("lista_posicion", 0);
                    }
                }

                // FIX Bug 2: Si solo_metadatos=true, solo actualizamos la barra y notificación
                // sin iniciar un segundo MediaPlayer (la Activity ya tiene uno corriendo)
                boolean soloMetadatos = intent.getBooleanExtra("solo_metadatos", false);
                if (soloMetadatos) {
                    isPlaying = true;
                    mostrarNotificacion();
                    actualizarNowPlayingBar();
                    Log.d(TAG, "Metadatos actualizados sin iniciar MediaPlayer: " + cancionActual.getTitulo());
                } else {
                    iniciarReproduccion();
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void iniciarReproduccion() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            if (cancionActual == null || cancionActual.getUri() == null) {
                Log.e(TAG, "iniciarReproduccion: Canción o URI nula");
                return;
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, cancionActual.getUri());
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                isPlaying = true;
                mostrarNotificacion();
                actualizarNowPlayingBar();

                // Actualizar metadata en MediaSession
                mediaSession.setMetadata(new android.support.v4.media.MediaMetadataCompat.Builder()
                        .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, cancionActual.getTitulo())
                        .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST, cancionActual.getArtista())
                        .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM, cancionActual.getAlbum())
                        .build());

                Log.d(TAG, "Reproduciendo: " + cancionActual.getTitulo());
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                siguienteCancion();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                return true;
            });

        } catch (Exception e) {
            Log.e(TAG, "Error iniciando reproducción", e);
        }
    }

    private void mostrarNotificacion() {
        Intent intent = new Intent(this, ReproductorLocalActivity.class);
        intent.putExtra("cancion_serializada", cancionActual);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(cancionActual.getTitulo())
                .setContentText(cancionActual.getArtista())
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(R.drawable.ic_previous, "Anterior", pendingIntent)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play, isPlaying ? "Pausar" : "Reproducir", pendingIntent)
                .addAction(R.drawable.ic_next, "Siguiente", pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true);

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reproductor de Música",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Control de reproducción de música");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // ✅ Enviar broadcast para actualizar la NowPlayingBar
    private void actualizarNowPlayingBar() {
        Intent intent = new Intent("NOW_PLAYING_UPDATE");
        intent.putExtra("cancion", cancionActual);
        intent.putExtra("isPlaying", isPlaying);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // ✅ Métodos públicos para controlar desde cualquier actividad
    public void playPause() {
        if (mediaPlayer != null) {
            if (isPlaying) {
                pausarReproduccion();
            } else {
                reanudarReproduccion();
            }
        }
    }

    public void next() {
        siguienteCancion();
    }

    public void previous() {
        cancionAnterior();
    }

    public Cancion getCurrentSong() {
        return cancionActual;
    }

    public boolean isMusicPlaying() {
        return isPlaying;
    }

    public void pausarReproduccion() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            mediaSession.setActive(false);
            actualizarNowPlayingBar();
        }
    }

    public void reanudarReproduccion() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            mediaSession.setActive(true);
            actualizarNowPlayingBar();
        }
    }

    public void detenerReproduccion() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;
        mediaSession.setActive(false);
        actualizarNowPlayingBar();
    }

    public void siguienteCancion() {
        if (playlistActual.isEmpty() || playlistActual.size() <= 1) return;
        posicionActual = (posicionActual + 1) % playlistActual.size();
        cancionActual = playlistActual.get(posicionActual);
        cancionActual.restaurarUri();
        iniciarReproduccion();
    }

    public void cancionAnterior() {
        if (playlistActual.isEmpty() || playlistActual.size() <= 1) return;
        posicionActual = (posicionActual - 1 + playlistActual.size()) % playlistActual.size();
        cancionActual = playlistActual.get(posicionActual);
        cancionActual.restaurarUri();
        iniciarReproduccion();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public Cancion getCancionActual() {
        return cancionActual;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
        stopForeground(true);
        Log.d(TAG, "MusicPlayerService destruido");
    }
}