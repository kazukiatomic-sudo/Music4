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
        initViews();
        // FIX Medio 2: crear el adapter una sola vez en onCreate
        configurarAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // FIX Medio 2: en onResume solo actualizamos la lista, sin recrear el adapter
        actualizarLista();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoritosManager = FavoritosManager.getInstance(this);
    }

    private void configurarAdapter() {
        List<Favorito> favoritos = favoritosManager.getFavoritos();
        adapter = new FavoritoAdapter(favoritos, new FavoritoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Favorito favorito) {
                android.net.Uri uri = null;
                if (favorito.getUri() != null && !favorito.getUri().isEmpty()) {
                    uri = android.net.Uri.parse(favorito.getUri());
                }
                Cancion cancion = new Cancion(
                        favorito.getId(), favorito.getTitulo(), favorito.getArtista(),
                        favorito.getAlbum(), favorito.getDuracion(), uri, favorito.getAlbumId());
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
                actualizarLista();
            }
        });
        recyclerView.setAdapter(adapter);
        actualizarLista();
    }

    private void actualizarLista() {
        List<Favorito> favoritos = favoritosManager.getFavoritos();
        if (favoritos.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.updateList(favoritos);
        }
    }
}
