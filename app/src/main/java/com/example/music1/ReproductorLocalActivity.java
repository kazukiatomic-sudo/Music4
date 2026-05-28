package com.example.music1;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.music1.models.Cancion;
import com.example.music1.models.Favorito;
import com.example.music1.utils.FavoritosManager;
import com.example.music1.utils.HistorialManager;
import com.example.music1.utils.SessionManager;
import com.example.music1.utils.TagEditorManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ReproductorLocalActivity extends AppCompatActivity {

    private static final String TAG = "ReproductorLocal";

    private ImageView albumCover;
    private TextView songTitle, songArtist, currentTime, totalTime;
    private ImageButton playPauseButton, prevButton, nextButton, repeatButton, shuffleButton;
    private ImageButton eqButton, favButton;
    private SeekBar seekBar;

    private MediaPlayer mediaPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Cancion cancionActual;
    private List<Cancion> playlistActual = new ArrayList<>();
    private int posicionActual = 0;
    private boolean isPlaying = false;
    private boolean isRepeat = false;
    private boolean isShuffle = false;
    private boolean isFadeEnabled = false;
    private boolean isGapless = false;
    private boolean isDriveMode = false;
    private float playbackSpeed = 1.0f;
    private CountDownTimer sleepTimer;
    private Random random = new Random();
    private Equalizer equalizer;
    private RotateAnimation rotateAnimation;
    private FavoritosManager favoritosManager;
    private TagEditorManager tagEditorManager;
    private SessionManager sessionManager;
    private EditTagsDialog editTagsDialog;

    private static final int[] THEMES = {
            android.R.color.black,
            android.R.color.holo_blue_dark,
            android.R.color.holo_purple,
            android.R.color.holo_green_dark
    };
    private static final String[] THEME_NAMES = {"Oscuro", "Azul", "Morado", "Verde"};
    private int currentTheme = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reproductor_local);
        Log.d(TAG, "onCreate: Iniciando ReproductorLocalActivity");

        favoritosManager = FavoritosManager.getInstance(this);
        tagEditorManager = TagEditorManager.getInstance(this);
        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();

        // ✅ Verificar si hay datos antes de continuar
        boolean tieneDatos = cargarPlaylist();
        Log.d(TAG, "onCreate: tieneDatos=" + tieneDatos);

        if (!tieneDatos || cancionActual == null) {
            Log.e(TAG, "onCreate: No se pudo cargar la canción");
            Toast.makeText(this, "Error: No se pudo cargar la canción", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupListeners();
        setupRotateAnimation();
        inicializarMediaPlayer();
    }

    private void initViews() {
        albumCover      = findViewById(R.id.albumCover);
        songTitle       = findViewById(R.id.songTitle);
        songArtist      = findViewById(R.id.songArtist);
        currentTime     = findViewById(R.id.currentTime);
        totalTime       = findViewById(R.id.totalTime);
        playPauseButton = findViewById(R.id.playPauseButton);
        prevButton      = findViewById(R.id.prevButton);
        nextButton      = findViewById(R.id.nextButton);
        repeatButton    = findViewById(R.id.repeatButton);
        shuffleButton   = findViewById(R.id.shuffleButton);
        eqButton        = findViewById(R.id.eqButton);
        favButton       = findViewById(R.id.favButton);
        seekBar         = findViewById(R.id.seekBar);

        prevButton.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.VISIBLE);
        shuffleButton.setVisibility(View.VISIBLE);
        repeatButton.setAlpha(0.5f);
        shuffleButton.setAlpha(0.5f);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_reproductor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_letra)          { buscarLetra();            return true; }
        if (id == R.id.action_add_playlist)   { agregarAPlaylist();       return true; }
        if (id == R.id.action_edit_tags)      { abrirEditorEtiquetas();   return true; }
        if (id == R.id.action_details)        { mostrarDetalles();        return true; }
        if (id == R.id.action_speed)          { mostrarVelocidad();       return true; }
        if (id == R.id.action_equalizer)      { abrirEcualizador();       return true; }
        if (id == R.id.action_cutter)         { abrirCortador();          return true; }
        if (id == R.id.action_search_youtube) { buscarEnYouTube();        return true; }
        if (id == R.id.action_theme)          { elegirTema();             return true; }
        if (id == R.id.action_drive_mode)     { toggleDriveMode(item);    return true; }
        if (id == R.id.action_lock_screen)    { toggleLockScreen();       return true; }
        if (id == R.id.action_settings)       { abrirAjustes();           return true; }
        if (id == R.id.action_hide)           { ocultarReproductor();     return true; }
        if (id == R.id.action_delete)         { eliminarCancion();        return true; }
        if (id == R.id.action_share)          { compartirCancion();       return true; }
        if (id == R.id.action_sleep_timer)    { mostrarSleepTimer();      return true; }
        if (id == R.id.action_fade)           { toggleFade(item);         return true; }
        if (id == R.id.action_gapless)        { toggleGapless(item);      return true; }

        return super.onOptionsItemSelected(item);
    }

    private void buscarLetra() {
        if (cancionActual == null) return;
        String query = cancionActual.getTitulo() + " " + cancionActual.getArtista() + " letra";
        String url = "https://www.google.com/search?q=" + Uri.encode(query);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void agregarAPlaylist() {
        if (cancionActual == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Agregar a lista de reproducción")
                .setMessage("\"" + cancionActual.getTitulo() + "\" se agregará a tu lista de reproducción actual.")
                .setPositiveButton("Agregar", (d, w) -> {
                    if (!playlistActual.contains(cancionActual)) {
                        playlistActual.add(cancionActual);
                    }
                    Toast.makeText(this, "✓ Agregado a la lista", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void abrirEditorEtiquetas() {
        if (cancionActual == null) return;
        editTagsDialog = new EditTagsDialog(this, cancionActual, () -> {
            actualizarUI();
            Toast.makeText(this, "Etiquetas actualizadas", Toast.LENGTH_SHORT).show();
        });
        editTagsDialog.show();
    }

    private void mostrarDetalles() {
        if (cancionActual == null) return;
        String info =
                "Título:    " + cancionActual.getTitulo() + "\n" +
                        "Artista:   " + cancionActual.getArtista() + "\n" +
                        "Álbum:     " + cancionActual.getAlbum() + "\n" +
                        "Duración:  " + cancionActual.getDuracionFormateada() + "\n" +
                        "ID:        " + cancionActual.getId() + "\n" +
                        "URI:       " + cancionActual.getUriString();
        new AlertDialog.Builder(this)
                .setTitle("Detalles de la canción")
                .setMessage(info)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void mostrarVelocidad() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(this, "Requiere Android 6.0 o superior", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] opciones = {"0.5×", "0.75×", "1.0× (normal)", "1.25×", "1.5×", "2.0×"};
        float[]  valores  = {0.5f,   0.75f,    1.0f,            1.25f,   1.5f,   2.0f};

        int seleccionado = 2;
        for (int i = 0; i < valores.length; i++) {
            if (Math.abs(valores[i] - playbackSpeed) < 0.01f) { seleccionado = i; break; }
        }

        final int[] sel = {seleccionado};
        new AlertDialog.Builder(this)
                .setTitle("Velocidad de reproducción")
                .setSingleChoiceItems(opciones, seleccionado, (d, which) -> sel[0] = which)
                .setPositiveButton("Aplicar", (d, w) -> {
                    playbackSpeed = valores[sel[0]];
                    aplicarVelocidad();
                    Toast.makeText(this, "Velocidad: " + opciones[sel[0]], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void abrirEcualizador() {
        if (equalizer != null) {
            new EqualizerDialog(this, equalizer).show();
        } else {
            Toast.makeText(this, "Ecualizador no disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirCortador() {
        if (cancionActual == null) {
            Toast.makeText(this, "No hay canción seleccionada", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cancionActual.esCancionDePrueba()) {
            Toast.makeText(this, "No se pueden cortar canciones de prueba", Toast.LENGTH_SHORT).show();
            return;
        }

        Mp3CutterDialog dialog = new Mp3CutterDialog(this, cancionActual);
        dialog.show();
    }

    private void buscarEnYouTube() {
        if (cancionActual == null) return;
        String query = cancionActual.getTitulo() + " " + cancionActual.getArtista();
        String url = "https://www.youtube.com/results?search_query=" + Uri.encode(query);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void elegirTema() {
        final int[] sel = {currentTheme};
        new AlertDialog.Builder(this)
                .setTitle("Tema del reproductor")
                .setSingleChoiceItems(THEME_NAMES, currentTheme, (d, which) -> sel[0] = which)
                .setPositiveButton("Aplicar", (d, w) -> {
                    currentTheme = sel[0];
                    View root = findViewById(android.R.id.content);
                    root.setBackgroundResource(THEMES[currentTheme]);
                    Toast.makeText(this, "Tema: " + THEME_NAMES[currentTheme], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void toggleDriveMode(MenuItem item) {
        isDriveMode = !isDriveMode;
        item.setTitle(isDriveMode ? "✓ Modo conducir" : "Modo de conducir");
        if (isDriveMode) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            prevButton.setScaleX(1.4f); prevButton.setScaleY(1.4f);
            nextButton.setScaleX(1.4f); nextButton.setScaleY(1.4f);
            playPauseButton.setScaleX(1.4f); playPauseButton.setScaleY(1.4f);
            Toast.makeText(this, "🚗 Modo conducir activado", Toast.LENGTH_SHORT).show();
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            prevButton.setScaleX(1f); prevButton.setScaleY(1f);
            nextButton.setScaleX(1f); nextButton.setScaleY(1f);
            playPauseButton.setScaleX(1f); playPauseButton.setScaleY(1f);
            Toast.makeText(this, "Modo conducir desactivado", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleLockScreen() {
        boolean keep = (getWindow().getAttributes().flags &
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0;
        if (!keep) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            Toast.makeText(this, "🔒 Pantalla activa mientras reproduce", Toast.LENGTH_SHORT).show();
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            Toast.makeText(this, "Pantalla de bloqueo normal", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirAjustes() {
        String[] opciones = {
                "Velocidad de reproducción",
                "Tema del reproductor",
                "Temporizador de apagado",
                "Desvanecer entre canciones",
                "Reproducción sin pausas"
        };
        new AlertDialog.Builder(this)
                .setTitle("⚙ Ajustes")
                .setItems(opciones, (d, which) -> {
                    switch (which) {
                        case 0: mostrarVelocidad(); break;
                        case 1: elegirTema(); break;
                        case 2: mostrarSleepTimer(); break;
                        case 3: toggleFadeDesdeAjustes(); break;
                        case 4: toggleGaplessDesdeAjustes(); break;
                    }
                })
                .show();
    }

    private void ocultarReproductor() {
        moveTaskToBack(true);
    }

    private void eliminarCancion() {
        if (cancionActual == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Eliminar canción")
                .setMessage("¿Eliminar \"" + cancionActual.getTitulo() + "\" del dispositivo?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    try {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.stop();
                        getContentResolver().delete(cancionActual.getUri(), null, null);
                        favoritosManager.quitarFavorito(cancionActual.getId(), cancionActual.getUriString());
                        Toast.makeText(this, "Canción eliminada", Toast.LENGTH_SHORT).show();
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void compartirCancion() {
        if (cancionActual == null) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT,
                "🎵 " + cancionActual.getTitulo() + " - " + cancionActual.getArtista());
        startActivity(Intent.createChooser(share, "Compartir canción"));
    }

    private void mostrarSleepTimer() {
        String[] opciones = {"5 minutos", "10 minutos", "15 minutos", "30 minutos",
                "45 minutos", "1 hora", "Personalizado",
                sleepTimer != null ? "❌ Cancelar temporizador" : "Sin temporizador activo"};

        new AlertDialog.Builder(this)
                .setTitle("⏱ Temporizador de apagado")
                .setItems(opciones, (d, which) -> {
                    int[] minutos = {5, 10, 15, 30, 45, 60, -1, 0};
                    if (which == 7 && sleepTimer == null) return;
                    if (which == 7) { cancelarSleepTimer(); return; }
                    if (which == 6) { mostrarPickerMinutos(); return; }
                    iniciarSleepTimer(minutos[which]);
                })
                .show();
    }

    private void mostrarPickerMinutos() {
        NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(1);
        picker.setMaxValue(120);
        picker.setValue(30);

        new AlertDialog.Builder(this)
                .setTitle("Minutos")
                .setView(picker)
                .setPositiveButton("Iniciar", (d, w) -> iniciarSleepTimer(picker.getValue()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void iniciarSleepTimer(int minutos) {
        cancelarSleepTimer();
        long ms = minutos * 60 * 1000L;
        sleepTimer = new CountDownTimer(ms, 60000) {
            @Override public void onTick(long millisLeft) {
                long minLeft = millisLeft / 60000;
                Toast.makeText(ReproductorLocalActivity.this,
                        "⏱ " + minLeft + " min para apagar", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFinish() {
                pausarCancion();
                Toast.makeText(ReproductorLocalActivity.this,
                        "⏱ Temporizador: reproducción detenida", Toast.LENGTH_LONG).show();
                sleepTimer = null;
            }
        }.start();
        Toast.makeText(this, "⏱ Apagado en " + minutos + " min", Toast.LENGTH_SHORT).show();
    }

    private void cancelarSleepTimer() {
        if (sleepTimer != null) {
            sleepTimer.cancel();
            sleepTimer = null;
            Toast.makeText(this, "Temporizador cancelado", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFade(MenuItem item) {
        isFadeEnabled = !isFadeEnabled;
        item.setTitle(isFadeEnabled ? "✓ Desvanecer" : "Desvanecer");
        Toast.makeText(this, isFadeEnabled
                ? "🎚 Fade entre canciones activado"
                : "Fade desactivado", Toast.LENGTH_SHORT).show();
    }

    private void toggleFadeDesdeAjustes() {
        isFadeEnabled = !isFadeEnabled;
        Toast.makeText(this, isFadeEnabled
                ? "🎚 Fade activado"
                : "Fade desactivado", Toast.LENGTH_SHORT).show();
    }

    private void toggleGapless(MenuItem item) {
        isGapless = !isGapless;
        item.setTitle(isGapless ? "✓ Sin pausas" : "Reproducción sin pausas");
        Toast.makeText(this, isGapless
                ? "▶ Reproducción sin pausas activada"
                : "Reproducción sin pausas desactivada", Toast.LENGTH_SHORT).show();
    }

    private void toggleGaplessDesdeAjustes() {
        isGapless = !isGapless;
        Toast.makeText(this, isGapless
                ? "▶ Sin pausas activado"
                : "Sin pausas desactivado", Toast.LENGTH_SHORT).show();
    }

    // ==================== MÉTODOS DEL REPRODUCTOR ====================

    private void aplicarVelocidad() {
        if (mediaPlayer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                PlaybackParams params = mediaPlayer.getPlaybackParams();
                params.setSpeed(playbackSpeed);
                mediaPlayer.setPlaybackParams(params);
                Log.d(TAG, "Velocidad aplicada: " + playbackSpeed + "x");
            } catch (Exception e) {
                Log.e(TAG, "Error aplicando velocidad", e);
            }
        }
    }

    private void configurarMediaPlayer() {
        if (mediaPlayer == null) return;
        totalTime.setText(cancionActual.getDuracionFormateada());
        seekBar.setMax((int) cancionActual.getDuracion());

        try {
            int sid = mediaPlayer.getAudioSessionId();
            if (sid != -1) { equalizer = new Equalizer(0, sid); equalizer.setEnabled(true); }
        } catch (Exception e) { Log.e(TAG, "Error ecualizador", e); }

        aplicarVelocidad();

        mediaPlayer.setOnCompletionListener(mp -> {
            if (isFadeEnabled) fadeOutAndNext();
            else if (isRepeat) {
                mediaPlayer.seekTo(0);
                aplicarVelocidad();
                mediaPlayer.start();
                actualizarSeekBar();
            } else {
                siguienteCancion();
            }
        });
    }

    private void fadeOutAndNext() {
        final float[] vol = {1.0f};
        handler.post(new Runnable() {
            @Override public void run() {
                if (vol[0] > 0.05f && mediaPlayer != null) {
                    vol[0] -= 0.05f;
                    mediaPlayer.setVolume(vol[0], vol[0]);
                    handler.postDelayed(this, 80);
                } else {
                    if (mediaPlayer != null) {
                        mediaPlayer.setVolume(1f, 1f);
                    }
                    if (isRepeat && mediaPlayer != null) {
                        mediaPlayer.seekTo(0);
                        aplicarVelocidad();
                        mediaPlayer.setVolume(1f, 1f);
                        mediaPlayer.start();
                    } else {
                        siguienteCancion();
                    }
                }
            }
        });
    }

    // ✅ Enviar broadcast para actualizar la NowPlayingBar
    private void enviarActualizacionNowPlaying() {
        Intent intent = new Intent("NOW_PLAYING_UPDATE");
        intent.putExtra("cancion", cancionActual);
        intent.putExtra("isPlaying", isPlaying);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // ✅ RETORNA boolean para saber si se cargaron los datos
    private boolean cargarPlaylist() {
        Intent intent = getIntent();
        Log.d(TAG, "cargarPlaylist: Intent recibido");

        try {
            if (intent.hasExtra("cancion_serializada")) {
                Serializable obj = intent.getSerializableExtra("cancion_serializada");
                if (obj instanceof Cancion) {
                    cancionActual = (Cancion) obj;
                    cancionActual.restaurarUri();
                    Log.d(TAG, "cargarPlaylist: Canción cargada: " + cancionActual.getTitulo());
                }
            }

            if (intent.hasExtra("lista_canciones")) {
                Serializable listObj = intent.getSerializableExtra("lista_canciones");
                if (listObj instanceof List<?>) {
                    List<?> temp = (List<?>) listObj;
                    if (!temp.isEmpty() && temp.get(0) instanceof Cancion) {
                        playlistActual = (List<Cancion>) listObj;
                        for (Cancion c : playlistActual) if (c != null) c.restaurarUri();
                        Log.d(TAG, "cargarPlaylist: Playlist cargada con " + playlistActual.size() + " canciones");
                    }
                }
            }

            posicionActual = intent.getIntExtra("lista_posicion", 0);

            if (playlistActual == null || playlistActual.isEmpty()) {
                playlistActual = new ArrayList<>();
                if (cancionActual != null) {
                    playlistActual.add(cancionActual);
                    Log.d(TAG, "cargarPlaylist: Playlist creada con canción actual");
                }
            }

            if (cancionActual != null) return true;

        } catch (Exception e) {
            Log.e(TAG, "Error deserializando playlist", e);
        }

        // Método legacy por extras individuales
        long cancionId  = intent.getLongExtra("cancion_id", -1);
        String titulo   = intent.getStringExtra("cancion_titulo");
        String artista  = intent.getStringExtra("cancion_artista");
        String album    = intent.getStringExtra("cancion_album");
        long duracion   = intent.getLongExtra("cancion_duracion", 0);
        long albumId    = intent.getLongExtra("cancion_album_id", 0);
        String uriStr   = intent.getStringExtra("cancion_uri");

        Log.d(TAG, "cargarPlaylist: Legacy - cancionId=" + cancionId + ", titulo=" + titulo);

        if (cancionId != -1 && titulo != null) {
            Uri uri = uriStr != null && !uriStr.isEmpty()
                    ? Uri.parse(uriStr)
                    : ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cancionId);
            cancionActual = new Cancion(cancionId, titulo, artista, album, duracion, uri, albumId);
            playlistActual = new ArrayList<>();
            playlistActual.add(cancionActual);
            posicionActual = 0;
            Log.d(TAG, "cargarPlaylist: Canción cargada por legacy");
            return true;
        }

        Log.e(TAG, "cargarPlaylist: No se pudo cargar ninguna canción");
        return false;
    }

    private void inicializarMediaPlayer() {
        Log.d(TAG, "inicializarMediaPlayer: Iniciando...");

        // FIX Bug 1: Detener el MusicPlayerService si estaba reproduciendo otra canción
        // Esto evita que suenen dos canciones a la vez cuando se abre una nueva desde la lista
        com.example.music1.utils.MusicPlayerService serviceActivo =
                com.example.music1.utils.MusicPlayerService.getInstance();
        if (serviceActivo != null) {
            serviceActivo.detenerReproduccion();
        }

        try {
            if (equalizer != null) {
                equalizer.release();
                equalizer = null;
            }

            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            if (cancionActual == null) {
                Log.e(TAG, "inicializarMediaPlayer: cancionActual es null");
                Toast.makeText(this, "Error: Canción no válida", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            cancionActual.restaurarUri();
            Uri uri = cancionActual.getUri();

            if (uri == null) {
                Log.e(TAG, "inicializarMediaPlayer: URI es null");
                Toast.makeText(this, "No se pudo acceder al archivo de audio", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, "inicializarMediaPlayer: URI=" + uri.toString());

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer preparado correctamente");
                configurarMediaPlayer();

                try {
                    mp.start();
                    isPlaying = true;
                    playPauseButton.setImageResource(R.drawable.ic_pause);
                    albumCover.startAnimation(rotateAnimation);
                    actualizarSeekBar();
                    HistorialManager.getInstance(ReproductorLocalActivity.this).registrarReproduccion(cancionActual);
                    enviarActualizacionNowPlaying(); // ✅ Notificar a la barra
                    // FIX Bug 2: Arrancar el MusicPlayerService para que NowPlayingBar funcione
                    // El servicio recibe la canción actual y actualiza la barra mini
                    arrancarServicioConCancionActual();
                    Log.d(TAG, "Reproducción iniciada correctamente");
                } catch (Exception e) {
                    Log.e(TAG, "Error al iniciar reproducción", e);
                    isPlaying = false;
                    playPauseButton.setImageResource(R.drawable.ic_play);
                    Toast.makeText(ReproductorLocalActivity.this, "Error al reproducir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                actualizarUI();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                Toast.makeText(ReproductorLocalActivity.this, "Error al reproducir el audio", Toast.LENGTH_SHORT).show();
                return true;
            });

        } catch (Exception e) {
            Log.e(TAG, "Error inicializando MediaPlayer", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void actualizarUI() {
        if (cancionActual == null) return;
        String tit = tagEditorManager.getTitulo(cancionActual.getId(), cancionActual.getUriString(), cancionActual.getTitulo());
        String art = tagEditorManager.getArtista(cancionActual.getId(), cancionActual.getUriString(), cancionActual.getArtista());
        songTitle.setText(tit);
        songArtist.setText(art);

        Bitmap portada = tagEditorManager.getPortada(cancionActual.getId(), cancionActual.getUriString());
        if (portada != null) albumCover.setImageBitmap(portada);
        else albumCover.setImageResource(cancionActual.getImagenAlbum());

        actualizarIconoFavorito();
    }

    private void setupRotateAnimation() {
        rotateAnimation = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(4000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setInterpolator(new android.view.animation.LinearInterpolator());
    }

    private void setupListeners() {
        playPauseButton.setOnClickListener(v -> {
            Log.d(TAG, "Play/Pause click, isPlaying=" + isPlaying);
            if (isPlaying) pausarCancion(); else reproducirCancion();
        });
        prevButton.setOnClickListener(v -> cancionAnterior());
        nextButton.setOnClickListener(v -> siguienteCancion());

        repeatButton.setOnClickListener(v -> {
            isRepeat = !isRepeat;
            repeatButton.setAlpha(isRepeat ? 1.0f : 0.5f);
        });
        shuffleButton.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            shuffleButton.setAlpha(isShuffle ? 1.0f : 0.5f);
        });

        eqButton.setOnClickListener(v -> abrirEcualizador());
        favButton.setOnClickListener(v -> toggleFavoritoLocal());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) mediaPlayer.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void toggleFavoritoLocal() {
        if (cancionActual == null) return;
        Favorito fav = new Favorito(cancionActual.getId(), cancionActual.getTitulo(),
                cancionActual.getArtista(), cancionActual.getAlbum(), cancionActual.getDuracion(),
                cancionActual.getUriString(), cancionActual.getAlbumId());

        if (favoritosManager.esFavorito(fav.getId(), fav.getUri())) {
            favoritosManager.quitarFavorito(fav.getId(), fav.getUri());
            Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
        } else {
            favoritosManager.agregarFavorito(fav);
            Toast.makeText(this, "❤ Agregado a favoritos", Toast.LENGTH_SHORT).show();
        }
        actualizarIconoFavorito();
    }

    private void reproducirCancion() {
        Log.d(TAG, "reproducirCancion: Intentando reproducir");
        if (mediaPlayer != null) {
            try {
                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                    isPlaying = true;
                    playPauseButton.setImageResource(R.drawable.ic_pause);
                    albumCover.startAnimation(rotateAnimation);
                    actualizarSeekBar();
                    HistorialManager.getInstance(this).registrarReproduccion(cancionActual);
                    enviarActualizacionNowPlaying(); // ✅ Notificar a la barra
                    Log.d(TAG, "reproducirCancion: Reproduciendo");
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "reproducirCancion: Error de estado", e);
                inicializarMediaPlayer();
            }
        } else {
            Log.w(TAG, "reproducirCancion: mediaPlayer es null, reinicializando");
            inicializarMediaPlayer();
        }
    }

    private void pausarCancion() {
        Log.d(TAG, "pausarCancion: Intentando pausar");
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPlaying = false;
                    playPauseButton.setImageResource(R.drawable.ic_play);
                    albumCover.clearAnimation();
                    enviarActualizacionNowPlaying(); // ✅ Notificar a la barra
                    Log.d(TAG, "pausarCancion: Pausado");
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "pausarCancion: Error", e);
                isPlaying = false;
                playPauseButton.setImageResource(R.drawable.ic_play);
            }
        } else {
            isPlaying = false;
            playPauseButton.setImageResource(R.drawable.ic_play);
        }
    }

    private void siguienteCancion() {
        if (playlistActual.isEmpty()) return;
        posicionActual = isShuffle && playlistActual.size() > 1
                ? nuevoIndiceAleatorio(posicionActual)
                : (posicionActual + 1) % playlistActual.size();
        cancionActual = playlistActual.get(posicionActual);
        cancionActual.restaurarUri();
        inicializarMediaPlayer();
    }

    private void cancionAnterior() {
        if (playlistActual.isEmpty()) return;
        posicionActual = isShuffle && playlistActual.size() > 1
                ? nuevoIndiceAleatorio(posicionActual)
                : (posicionActual - 1 + playlistActual.size()) % playlistActual.size();
        cancionActual = playlistActual.get(posicionActual);
        cancionActual.restaurarUri();
        inicializarMediaPlayer();
    }

    private int nuevoIndiceAleatorio(int actual) {
        int nuevo;
        do { nuevo = random.nextInt(playlistActual.size()); } while (nuevo == actual);
        return nuevo;
    }

    // ✅ CORREGIDO: Método actualizarSeekBar con manejo seguro de MediaPlayer
    private void actualizarSeekBar() {
        if (mediaPlayer != null && isPlaying) {
            try {
                if (mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    currentTime.setText(msATiempo(currentPosition));
                    handler.postDelayed(updateSeekBar, 1000);
                } else {
                    if (isPlaying) {
                        isPlaying = false;
                        playPauseButton.setImageResource(R.drawable.ic_play);
                        albumCover.clearAnimation();
                    }
                    handler.postDelayed(updateSeekBar, 500);
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "actualizarSeekBar: MediaPlayer en estado inválido", e);
                if (cancionActual != null) {
                    inicializarMediaPlayer();
                }
            }
        } else if (mediaPlayer == null && cancionActual != null) {
            Log.w(TAG, "actualizarSeekBar: MediaPlayer nulo, reinicializando");
            inicializarMediaPlayer();
            handler.postDelayed(updateSeekBar, 1000);
        }
    }

    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            actualizarSeekBar();
        }
    };

    private String msATiempo(int ms) {
        return String.format(Locale.US, "%d:%02d", ms / 60000, (ms / 1000) % 60);
    }

    private void actualizarIconoFavorito() {
        if (cancionActual != null && favoritosManager != null) {
            favButton.setImageResource(
                    favoritosManager.esFavorito(cancionActual.getId(), cancionActual.getUriString())
                            ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        }
    }

    // FIX Bug 2: Inicia el MusicPlayerService con la canción actual
    // Esto hace que la NowPlayingBar tenga referencia al servicio y pueda reproducir/pausar
    private void arrancarServicioConCancionActual() {
        try {
            Intent serviceIntent = new Intent(this, com.example.music1.utils.MusicPlayerService.class);
            serviceIntent.putExtra("cancion_serializada", cancionActual);
            if (!playlistActual.isEmpty()) {
                serviceIntent.putExtra("lista_canciones", new ArrayList<>(playlistActual));
                serviceIntent.putExtra("lista_posicion", posicionActual);
            }
            // Solo actualizamos metadatos del servicio — NO iniciamos reproducción desde el servicio
            // porque el MediaPlayer ya está corriendo en esta Activity
            serviceIntent.putExtra("solo_metadatos", true);
            startService(serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error arrancando servicio", e);
        }
    }

    // ✅ CORREGIDO: onBackPressed con mejor manejo al salir
    @Override
    public void onBackPressed() {
        finish();
    }

    // FIX Alto 2: Receptor para controlar el MediaPlayer desde la NowPlayingBar
    // Esto evita que la barra mini inicie un segundo MediaPlayer en el servicio
    private android.content.BroadcastReceiver controlReceiver;

    @Override
    protected void onResume() {
        super.onResume();
        if (isPlaying && mediaPlayer != null) handler.post(updateSeekBar);

        // Registrar receptor de control desde la barra mini
        controlReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context ctx, Intent intent) {
                String action = intent.getStringExtra("action");
                if ("PLAY_PAUSE".equals(action)) {
                    if (isPlaying) pausarCancion(); else reproducirCancion();
                } else if ("NEXT".equals(action)) {
                    siguienteCancion();
                }
            }
        };
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
            .registerReceiver(controlReceiver, new android.content.IntentFilter("REPRODUCTOR_CONTROL"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (controlReceiver != null) {
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(controlReceiver);
            controlReceiver = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (editTagsDialog != null && editTagsDialog.isShowing()
                && requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            editTagsDialog.onImagePicked(data.getData());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateSeekBar);
        cancelarSleepTimer();
        if (equalizer != null) {
            equalizer.release();
            equalizer = null;
        }
    }
}