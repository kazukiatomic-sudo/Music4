package com.example.music1;

import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.music1.models.Cancion;
import com.example.music1.models.Favorito;
import com.example.music1.models.Usuario;
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
    private final Handler handler = new Handler();
    private Cancion cancionActual;
    private List<Cancion> playlistActual = new ArrayList<>();
    private int posicionActual = 0;
    private boolean isPlaying = false;
    private boolean isRepeat = false;
    private boolean isShuffle = false;
    private Random random = new Random();
    private Equalizer equalizer;
    private RotateAnimation rotateAnimation;
    private FavoritosManager favoritosManager;
    private TagEditorManager tagEditorManager;
    private SessionManager sessionManager;
    private EditTagsDialog editTagsDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reproductor_local);

        favoritosManager = FavoritosManager.getInstance(this);
        tagEditorManager = TagEditorManager.getInstance(this);
        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        cargarPlaylist();
        setupListeners();
        setupRotateAnimation();

        if (cancionActual != null) {
            inicializarMediaPlayer();
        }
    }

    private void initViews() {
        albumCover = findViewById(R.id.albumCover);
        songTitle = findViewById(R.id.songTitle);
        songArtist = findViewById(R.id.songArtist);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        playPauseButton = findViewById(R.id.playPauseButton);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        repeatButton = findViewById(R.id.repeatButton);
        shuffleButton = findViewById(R.id.shuffleButton);
        eqButton = findViewById(R.id.eqButton);
        favButton = findViewById(R.id.favButton);
        seekBar = findViewById(R.id.seekBar);

        prevButton.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.VISIBLE);
        shuffleButton.setVisibility(View.VISIBLE);

        repeatButton.setAlpha(0.5f);
        shuffleButton.setAlpha(0.5f);
    }

    private void cargarPlaylist() {
        Intent intent = getIntent();

        try {
            if (intent.hasExtra("cancion_serializada")) {
                Serializable obj = intent.getSerializableExtra("cancion_serializada");
                if (obj instanceof Cancion) {
                    cancionActual = (Cancion) obj;
                    cancionActual.restaurarUri();
                    Log.d(TAG, "Canción cargada desde serializable: " + cancionActual.getTitulo());
                }
            }

            if (intent.hasExtra("lista_canciones")) {
                Serializable listObj = intent.getSerializableExtra("lista_canciones");
                if (listObj instanceof List<?>) {
                    List<?> temp = (List<?>) listObj;
                    if (!temp.isEmpty() && temp.get(0) instanceof Cancion) {
                        playlistActual = (List<Cancion>) listObj;
                        for (Cancion c : playlistActual) {
                            if (c != null) c.restaurarUri();
                        }
                        Log.d(TAG, "Playlist cargada: " + playlistActual.size() + " canciones");
                    }
                }
            }

            posicionActual = intent.getIntExtra("lista_posicion", 0);

            if (playlistActual == null || playlistActual.isEmpty()) {
                playlistActual = new ArrayList<>();
                if (cancionActual != null) playlistActual.add(cancionActual);
            }

            if (cancionActual != null) return;
        } catch (Exception e) {
            Log.e(TAG, "Error al deserializar playlist", e);
        }

        // Método legacy
        long cancionId = intent.getLongExtra("cancion_id", -1);
        String titulo = intent.getStringExtra("cancion_titulo");
        String artista = intent.getStringExtra("cancion_artista");
        String album = intent.getStringExtra("cancion_album");
        long duracion = intent.getLongExtra("cancion_duracion", 0);
        long albumId = intent.getLongExtra("cancion_album_id", 0);
        String uriString = intent.getStringExtra("cancion_uri");

        if (cancionId != -1 && titulo != null) {
            Uri uri;
            if (uriString != null && !uriString.isEmpty()) {
                uri = Uri.parse(uriString);
            } else {
                uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cancionId);
            }
            cancionActual = new Cancion(cancionId, titulo, artista, album, duracion, uri, albumId);
            playlistActual = new ArrayList<>();
            playlistActual.add(cancionActual);
            posicionActual = 0;
        }

        if (cancionActual == null) {
            Toast.makeText(this, "Error: Datos de canción inválidos", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void inicializarMediaPlayer() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            if (cancionActual == null) {
                Toast.makeText(this, "Error: Canción nula", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            cancionActual.restaurarUri();
            Uri uri = cancionActual.getUri();
            if (uri == null) {
                Toast.makeText(this, "Error: No se pudo acceder al archivo", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, "Reproduciendo URI: " + uri.toString());

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                configurarMediaPlayer();
                reproducirCancion();
                actualizarUI();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                Toast.makeText(this, "Error al reproducir canción", Toast.LENGTH_SHORT).show();
                return true;
            });

        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar MediaPlayer", e);
            Toast.makeText(this, "Error al cargar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void configurarMediaPlayer() {
        if (mediaPlayer == null) return;

        totalTime.setText(cancionActual.getDuracionFormateada());
        seekBar.setMax((int) cancionActual.getDuracion());

        try {
            int audioSessionId = mediaPlayer.getAudioSessionId();
            if (audioSessionId != -1 && equalizer == null) {
                equalizer = new Equalizer(0, audioSessionId);
                equalizer.setEnabled(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al configurar ecualizador", e);
        }

        mediaPlayer.setOnCompletionListener(mp -> {
            if (isRepeat) {
                mediaPlayer.start();
            } else {
                siguienteCancion();
            }
        });
    }

    private void actualizarUI() {
        if (cancionActual == null) return;

        String tituloMostrable = tagEditorManager.getTitulo(
                cancionActual.getId(), cancionActual.getUriString(), cancionActual.getTitulo());
        String artistaMostrable = tagEditorManager.getArtista(
                cancionActual.getId(), cancionActual.getUriString(), cancionActual.getArtista());

        songTitle.setText(tituloMostrable);
        songArtist.setText(artistaMostrable);

        Bitmap portada = tagEditorManager.getPortada(cancionActual.getId(), cancionActual.getUriString());
        if (portada != null) albumCover.setImageBitmap(portada);
        else albumCover.setImageResource(cancionActual.getImagenAlbum());

        actualizarIconoFavorito();
    }

    private void setupRotateAnimation() {
        rotateAnimation = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(4000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setInterpolator(new android.view.animation.LinearInterpolator());
    }

    private void setupListeners() {
        playPauseButton.setOnClickListener(v -> {
            if (isPlaying) pausarCancion();
            else reproducirCancion();
        });

        prevButton.setOnClickListener(v -> cancionAnterior());
        nextButton.setOnClickListener(v -> siguienteCancion());

        repeatButton.setOnClickListener(v -> {
            isRepeat = !isRepeat;
            repeatButton.setAlpha(isRepeat ? 1.0f : 0.5f);
            Toast.makeText(this, isRepeat ? "Repetir activado" : "Repetir desactivado", Toast.LENGTH_SHORT).show();
        });

        shuffleButton.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            shuffleButton.setAlpha(isShuffle ? 1.0f : 0.5f);
            Toast.makeText(this, isShuffle ? "Shuffle activado" : "Shuffle desactivado", Toast.LENGTH_SHORT).show();
        });

        eqButton.setOnClickListener(v -> {
            if (equalizer != null) new EqualizerDialog(this, equalizer).show();
            else Toast.makeText(this, "Ecualizador no disponible", Toast.LENGTH_SHORT).show();
        });

        // ✅ CORREGIDO: Favoritos sin pedir login para música local
        favButton.setOnClickListener(v -> toggleFavoritoLocal());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) mediaPlayer.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    // ✅ NUEVO: Favoritos SOLO locales (sin login)
    private void toggleFavoritoLocal() {
        if (cancionActual == null) return;

        Favorito fav = new Favorito(
                cancionActual.getId(),
                cancionActual.getTitulo(),
                cancionActual.getArtista(),
                cancionActual.getAlbum(),
                cancionActual.getDuracion(),
                cancionActual.getUriString(),
                cancionActual.getAlbumId()
        );

        if (favoritosManager.esFavorito(fav.getId(), fav.getUri())) {
            favoritosManager.quitarFavorito(fav.getId(), fav.getUri());
            Toast.makeText(this, "❌ Eliminado de favoritos locales", Toast.LENGTH_SHORT).show();
        } else {
            favoritosManager.agregarFavorito(fav);
            Toast.makeText(this, "❤️ Agregado a favoritos locales", Toast.LENGTH_SHORT).show();
        }
        actualizarIconoFavorito();
    }

    private void reproducirCancion() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            playPauseButton.setImageResource(R.drawable.ic_pause);
            albumCover.startAnimation(rotateAnimation);
            actualizarSeekBar();
            HistorialManager.getInstance(this).registrarReproduccion(cancionActual);
        }
    }

    private void pausarCancion() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            playPauseButton.setImageResource(R.drawable.ic_play);
            albumCover.clearAnimation();
            handler.removeCallbacks(updateSeekBar);
        }
    }

    private void siguienteCancion() {
        if (playlistActual.isEmpty()) return;

        if (isShuffle && playlistActual.size() > 1) {
            int nuevaPosicion;
            do {
                nuevaPosicion = random.nextInt(playlistActual.size());
            } while (nuevaPosicion == posicionActual && playlistActual.size() > 1);
            posicionActual = nuevaPosicion;
        } else {
            posicionActual++;
            if (posicionActual >= playlistActual.size()) {
                posicionActual = 0;
            }
        }

        cancionActual = playlistActual.get(posicionActual);
        cancionActual.restaurarUri();
        inicializarMediaPlayer();
    }

    private void cancionAnterior() {
        if (playlistActual.isEmpty()) return;

        if (isShuffle && playlistActual.size() > 1) {
            int nuevaPosicion;
            do {
                nuevaPosicion = random.nextInt(playlistActual.size());
            } while (nuevaPosicion == posicionActual);
            posicionActual = nuevaPosicion;
        } else {
            posicionActual--;
            if (posicionActual < 0) {
                posicionActual = playlistActual.size() - 1;
            }
        }

        cancionActual = playlistActual.get(posicionActual);
        cancionActual.restaurarUri();
        inicializarMediaPlayer();
    }

    private void actualizarSeekBar() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
            currentTime.setText(milisegundosATiempo(mediaPlayer.getCurrentPosition()));
            handler.postDelayed(updateSeekBar, 1000);
        }
    }

    private final Runnable updateSeekBar = this::actualizarSeekBar;

    private String milisegundosATiempo(int ms) {
        return String.format(Locale.US, "%d:%02d", (ms / 60000), (ms / 1000) % 60);
    }

    private void actualizarIconoFavorito() {
        if (cancionActual != null && favoritosManager != null) {
            favButton.setImageResource(favoritosManager.esFavorito(cancionActual.getId(), cancionActual.getUriString())
                    ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        }
    }

    // ✅ CORREGIDO: Botón físico "Atrás" NO detiene la música
    @Override
    public void onBackPressed() {
        // Mover la actividad a segundo plano en lugar de destruirla
        moveTaskToBack(true);
        // No llamar a super.onBackPressed() para no destruir la Activity
        // La música sigue sonando
        Toast.makeText(this, "🎵 La música sigue sonando en segundo plano", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_reproductor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            eliminarCancion();
            return true;
        } else if (id == R.id.action_details) {
            mostrarDetalles();
            return true;
        } else if (id == R.id.action_search_youtube) {
            buscarEnYouTube();
            return true;
        } else if (id == R.id.action_share) {
            compartirCancion();
            return true;
        } else if (id == R.id.action_edit_tags) {
            abrirEditorEtiquetas();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void abrirEditorEtiquetas() {
        if (cancionActual == null) return;
        editTagsDialog = new EditTagsDialog(this, cancionActual, () -> {
            actualizarUI();
            Toast.makeText(this, "Etiquetas actualizadas", Toast.LENGTH_SHORT).show();
        });
        editTagsDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (editTagsDialog != null && editTagsDialog.isShowing() && requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            editTagsDialog.onImagePicked(data.getData());
        }
    }

    private void eliminarCancion() {
        if (cancionActual == null) return;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar")
                .setMessage("¿Eliminar \"" + cancionActual.getTitulo() + "\"?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    try {
                        getContentResolver().delete(cancionActual.getUri(), null, null);
                        favoritosManager.quitarFavorito(cancionActual.getId(), cancionActual.getUriString());
                        Toast.makeText(this, "Eliminada", Toast.LENGTH_SHORT).show();
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void mostrarDetalles() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Detalles")
                .setMessage("Título: " + cancionActual.getTitulo() + "\nArtista: " + cancionActual.getArtista() +
                        "\nÁlbum: " + cancionActual.getAlbum() + "\nDuración: " + cancionActual.getDuracionFormateada())
                .setPositiveButton("Cerrar", null).show();
    }

    private void buscarEnYouTube() {
        String query = cancionActual.getTitulo() + " " + cancionActual.getArtista();
        String url = "https://www.youtube.com/results?search_query=" + Uri.encode(query);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void compartirCancion() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, "🎵 " + cancionActual.getTitulo() + " - " + cancionActual.getArtista());
        startActivity(Intent.createChooser(share, "Compartir"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (equalizer != null) {
            equalizer.release();
            equalizer = null;
        }
        handler.removeCallbacks(updateSeekBar);
        // ⚠️ NO liberamos el MediaPlayer aquí para que siga sonando en segundo plano
        // Si quieres detenerlo al cerrar la app, se manejará en otro lado
    }
}