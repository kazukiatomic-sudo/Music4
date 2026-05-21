package com.example.music1;

import com.example.music1.utils.MiniPlayerManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.music1.models.Cancion;
import com.example.music1.models.CancionReproducida;
import com.example.music1.utils.HistorialManager;
import com.example.music1.utils.TagEditorManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class SugerirActivity extends AppCompatActivity {
    private static final String TAG = "SugerirActivity";
    private LinearLayout carruselRecientes;
    private LinearLayout carruselTop;
    private TextView tvEmpty;
    private HistorialManager historialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sugerir);

        MiniPlayerManager.setupMiniPlayer(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        carruselRecientes = findViewById(R.id.carruselRecientes);
        carruselTop = findViewById(R.id.carruselTop);
        tvEmpty = findViewById(R.id.tvEmpty);

        historialManager = HistorialManager.getInstance(this);
        cargarDatos();
    }

    private void cargarDatos() {
        List<Cancion> recientes = historialManager.getRecientes();
        List<CancionReproducida> masReproducidas = historialManager.getMasReproducidas();

        if (recientes.isEmpty() && masReproducidas.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);

        carruselRecientes.removeAllViews();
        for (Cancion cancion : recientes) {
            View item = crearItemCarrusel(cancion, false, 0);
            carruselRecientes.addView(item);
        }

        carruselTop.removeAllViews();
        for (CancionReproducida cr : masReproducidas) {
            View item = crearItemCarrusel(cr.getCancion(), true, cr.getContador());
            carruselTop.addView(item);
        }

        findViewById(R.id.btnVerTodosRecientes).setOnClickListener(v -> {
            Toast.makeText(this, "Lista de recientes próximamente", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnVerTodosTop).setOnClickListener(v -> {
            Toast.makeText(this, "Top canciones próximamente", Toast.LENGTH_SHORT).show();
        });
    }

    private View crearItemCarrusel(Cancion cancion, boolean mostrarContador, int contador) {
        View item = LayoutInflater.from(this).inflate(R.layout.item_carrusel_sugerir, null);

        TextView tvTitulo = item.findViewById(R.id.tvTitulo);
        TextView tvArtista = item.findViewById(R.id.tvArtista);
        TextView tvContador = item.findViewById(R.id.tvContador);

        TagEditorManager manager = TagEditorManager.getInstance(this);
        String tituloMostrable = manager.getTitulo(cancion.getId(), cancion.getUriString(), cancion.getTitulo());
        String artistaMostrable = manager.getArtista(cancion.getId(), cancion.getUriString(), cancion.getArtista());

        tvTitulo.setText(tituloMostrable);
        tvArtista.setText(artistaMostrable);

        if (mostrarContador && contador > 0) {
            tvContador.setText(String.valueOf(contador));
            tvContador.setVisibility(View.VISIBLE);
        }

        item.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, ReproductorLocalActivity.class);
                intent.putExtra("cancion_serializada", cancion);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error al reproducir", e);
                Toast.makeText(this, "Error al reproducir", Toast.LENGTH_SHORT).show();
            }
        });

        return item;
    }
}