package com.example.music1;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music1.adapters.ResultadoBusquedaAdapter;
import com.example.music1.models.Cancion;
import com.google.android.material.appbar.MaterialToolbar;
import com.example.music1.utils.MiniPlayerManager;

import java.util.ArrayList;
import java.util.List;

public class BusquedaMusicaActivity extends AppCompatActivity {
    private static final String TAG = "BusquedaMusica";

    private EditText etBuscar;
    private ImageView ivClear;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ResultadoBusquedaAdapter adapter;

    private final List<Cancion> todasLasCanciones = new ArrayList<>();
    // FIX: usar Looper.getMainLooper() para el Handler
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // FIX: flag para saber si la carga inicial terminó
    private boolean cargaCompleta = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_busqueda_musica);
        MiniPlayerManager.setupMiniPlayer(this);

        initViews();
        cargarTodasLasCanciones();
        setupSearch();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etBuscar = findViewById(R.id.etBuscar);
        ivClear = findViewById(R.id.ivClear);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResultadoBusquedaAdapter(new ArrayList<>(), this::onCancionClick);
        recyclerView.setAdapter(adapter);

        // FIX: deshabilitar el campo de búsqueda hasta que la carga termine
        etBuscar.setEnabled(false);
        etBuscar.setHint("Cargando canciones...");

        ivClear.setOnClickListener(v -> {
            etBuscar.setText("");
            ivClear.setVisibility(View.GONE);
            adapter.updateList(new ArrayList<>());
            tvEmpty.setText("Escribe para buscar canciones");
            tvEmpty.setVisibility(View.VISIBLE);
        });
    }

    private void cargarTodasLasCanciones() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<Cancion> canciones = obtenerTodasLasCanciones();
            runOnUiThread(() -> {
                todasLasCanciones.clear();
                todasLasCanciones.addAll(canciones);
                progressBar.setVisibility(View.GONE);
                // FIX: habilitar la búsqueda solo cuando los datos ya están listos
                cargaCompleta = true;
                etBuscar.setEnabled(true);
                etBuscar.setHint("Buscar por título, artista o álbum...");
                Log.d(TAG, "Carga completa: " + todasLasCanciones.size() + " canciones");
            });
        }).start();
    }

    private List<Cancion> obtenerTodasLasCanciones() {
        List<Cancion> canciones = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
        };

        try (Cursor cursor = contentResolver.query(collection, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

                do {
                    String title = cursor.getString(titleColumn);
                    if (title == null || title.isEmpty()) continue;

                    long duration = cursor.getLong(durationColumn);
                    if (duration < 1000) continue;

                    Uri uri = ContentUris.withAppendedId(collection, cursor.getLong(idColumn));
                    Cancion cancion = new Cancion(
                            cursor.getLong(idColumn),
                            title,
                            cursor.getString(artistColumn) != null ? cursor.getString(artistColumn) : "Artista desconocido",
                            cursor.getString(albumColumn) != null ? cursor.getString(albumColumn) : "Álbum desconocido",
                            duration,
                            uri,
                            cursor.getLong(albumIdColumn)
                    );
                    canciones.add(cancion);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "obtenerTodasLasCanciones: Error", e);
        }
        return canciones;
    }

    private void setupSearch() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
                // FIX: no buscar si la carga aún no terminó
                if (!cargaCompleta) return;
                searchRunnable = () -> buscar(s.toString());
                handler.postDelayed(searchRunnable, 300);
                ivClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void buscar(String query) {
        if (query.isEmpty()) {
            adapter.updateList(new ArrayList<>());
            tvEmpty.setText("Escribe para buscar canciones");
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<Cancion> resultados = new ArrayList<>();
            String lowerQuery = query.toLowerCase();

            for (Cancion cancion : todasLasCanciones) {
                if (cancion.getTitulo().toLowerCase().contains(lowerQuery) ||
                        cancion.getArtista().toLowerCase().contains(lowerQuery) ||
                        cancion.getAlbum().toLowerCase().contains(lowerQuery)) {
                    resultados.add(cancion);
                }
            }

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (resultados.isEmpty()) {
                    adapter.updateList(new ArrayList<>());
                    tvEmpty.setText("No se encontraron canciones");
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    adapter.updateList(resultados);
                    recyclerView.setVisibility(View.VISIBLE);
                    tvEmpty.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void onCancionClick(Cancion cancion) {
        Intent intent = new Intent(this, ReproductorLocalActivity.class);
        intent.putExtra("cancion_serializada", cancion);
        startActivity(intent);
    }
}
