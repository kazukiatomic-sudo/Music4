package com.example.music1;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music1.adapters.ListaMusicaAdapter;
import com.example.music1.models.Cancion;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class ListaMusicaLocalActivity extends AppCompatActivity {

    private static final String TAG = "ListaMusicaLocal";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private RecyclerView recyclerView;
    private ListaMusicaAdapter adapter;
    private List<Cancion> listaCanciones = new ArrayList<>();
    private ProgressBar progressBar;
    private LinearLayout emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_musica_local);

        initViews();
        checkPermissionsAndLoadMusic();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        
        // ✅ ARREGLO RETROCESO: Cambiamos finish() por un Intent claro para no cerrar la app
        toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(this, TipoMusicaActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        setSupportActionBar(toolbar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListaMusicaAdapter(listaCanciones, cancion -> {
            Intent intent = new Intent(this, ReproductorLocalActivity.class);
            intent.putExtra("cancion_serializada", cancion);
            intent.putExtra("lista_posicion", listaCanciones.indexOf(cancion));
            intent.putExtra("lista_canciones", new ArrayList<>(listaCanciones));
            // ✅ IMPORTANTE: No llamar a finish() aquí para que el reproductor pueda volver
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    // ✅ PUNTO LÍDER: Los 3 puntos superiores (Inflar el menú)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Asegúrate de que el archivo menu_lista_musica.xml exista en res/menu
        getMenuInflater().inflate(R.menu.menu_lista_musica, menu);
        return true;
    }

    // ✅ PUNTO LÍDER: Funcionalidad de los botones del menú
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            startActivity(new Intent(this, BusquedaMusicaActivity.class));
            return true;
        } else if (id == R.id.action_favoritos) {
            startActivity(new Intent(this, FavoritoActivity.class));
            return true;
        } else if (id == R.id.action_perfil) { // Añadí este por si quieres ir al perfil
            startActivity(new Intent(this, PerfilUsuarioActivity.class));
            return true;
        } else if (id == R.id.action_sugerir) {
            startActivity(new Intent(this, SugerirActivity.class));
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    // ✅ ARREGLO RETROCESO: Controlar el botón físico del celular
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Esto asegura que vuelva al menú principal de "Tipo de Música" y no cierre la app
        Intent intent = new Intent(this, TipoMusicaActivity.class);
        startActivity(intent);
        finish();
    }

    private void checkPermissionsAndLoadMusic() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        } else {
            cargarMusicaLocal();
        }
    }

    private void cargarMusicaLocal() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);

        new Thread(() -> {
            List<Cancion> canciones = getMusicFromDevice();
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (canciones.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                } else {
                    listaCanciones.clear();
                    listaCanciones.addAll(canciones);
                    adapter.updateList(listaCanciones);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    private List<Cancion> getMusicFromDevice() {
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

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        try (Cursor cursor = contentResolver.query(collection, projection, selection, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

                do {
                    long id = cursor.getLong(idColumn);
                    String title = cursor.getString(titleColumn);
                    String artist = cursor.getString(artistColumn);
                    String album = cursor.getString(albumColumn);
                    long duration = cursor.getLong(durationColumn);
                    long albumId = cursor.getLong(albumIdColumn);

                    if (title != null && duration > 0) {
                        Uri uri = ContentUris.withAppendedId(collection, id);
                        Cancion cancion = new Cancion(id, title,
                                artist != null ? artist : "Artista desconocido",
                                album != null ? album : "Álbum desconocido",
                                duration, uri, albumId);
                        canciones.add(cancion);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al leer música", e);
        }
        return canciones;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cargarMusicaLocal();
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_LONG).show();
            }
        }
    }
}