package com.example.music1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music1.models.Cancion;
import com.example.music1.adapters.FavoritoAdapter;
import com.example.music1.models.Favorito;
import com.example.music1.utils.FavoritosManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.example.music1.utils.MiniPlayerManager;

import java.util.List;
import java.util.ArrayList;

public class FavoritoActivity extends AppCompatActivity {
    private static final String TAG = "FavoritoActivity";
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private FavoritoAdapter adapter;
    private FavoritosManager favoritosManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favoritos);
        MiniPlayerManager.setupMiniPlayer(this);

        initViews();
        cargarFavoritos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarFavoritos();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoritosManager = FavoritosManager.getInstance(this);
    }

    private void cargarFavoritos() {
        List<Favorito> favoritos = favoritosManager.getFavoritos();

        if (favoritos.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            adapter = new FavoritoAdapter(favoritos, new FavoritoAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(Favorito favorito) {
                    // ✅ CORREGIDO: Convertir Favorito a Cancion correctamente
                    Cancion cancion = new Cancion(
                            favorito.getId(),
                            favorito.getTitulo(),
                            favorito.getArtista(),
                            favorito.getAlbum(),
                            favorito.getDuracion(),
                            favorito.getUri() != null ? android.net.Uri.parse(favorito.getUri()) : null,
                            favorito.getAlbumId()
                    );

                    // Crear playlist con esta sola canción
                    ArrayList<Cancion> playlist = new ArrayList<>();
                    playlist.add(cancion);

                    Intent intent = new Intent(FavoritoActivity.this, ReproductorLocalActivity.class);
                    intent.putExtra("cancion_serializada", cancion);
                    intent.putExtra("lista_canciones", playlist);
                    intent.putExtra("lista_posicion", 0);
                    startActivity(intent);
                }

                @Override
                public void onEliminarClick(Favorito favorito) {
                    cargarFavoritos(); // Refrescar después de eliminar
                }
            });
            recyclerView.setAdapter(adapter);
        }
    }
}