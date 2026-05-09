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

import java.util.List;

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
                    Intent intent = new Intent(FavoritoActivity.this, ReproductorLocalActivity.class);
                    // Enviar favorito como canción no es posible directamente
// Usar método legacy por ahora
                    intent.putExtra("cancion_id", favorito.getId());
                    intent.putExtra("cancion_titulo", favorito.getTitulo());
                    intent.putExtra("cancion_artista", favorito.getArtista());
                    intent.putExtra("cancion_album", favorito.getAlbum());
                    intent.putExtra("cancion_duracion", favorito.getDuracion());
                    intent.putExtra("cancion_uri", favorito.getUri());
                    intent.putExtra("cancion_album_id", favorito.getAlbumId());
                }

                @Override
                public void onEliminarClick(Favorito favorito) {
                    if (favoritosManager.getFavoritos().isEmpty()) {
                        cargarFavoritos();
                    }
                }
            });
            recyclerView.setAdapter(adapter);
        }
    }
}