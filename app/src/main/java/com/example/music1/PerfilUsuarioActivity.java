package com.example.music1;

import com.example.music1.utils.MiniPlayerManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.music1.models.Usuario;
import com.example.music1.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.imageview.ShapeableImageView;

public class PerfilUsuarioActivity extends AppCompatActivity {

    private ShapeableImageView ivAvatar;
    private TextView tvNombreUsuario, tvEmailUsuario;
    private CardView cardFavoritos, cardHistorial, cardCerrarSesion;
    private SessionManager sessionManager;
    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_usuario);

        MiniPlayerManager.setupMiniPlayer(this);

        sessionManager = new SessionManager(this);
        usuario = sessionManager.getUsuario();

        // Si no hay usuario logueado, volver al login
        if (usuario == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        cargarDatosUsuario();
        setupListeners();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivAvatar = findViewById(R.id.ivAvatar);
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        tvEmailUsuario = findViewById(R.id.tvEmailUsuario);
        cardFavoritos = findViewById(R.id.cardFavoritos);
        cardHistorial = findViewById(R.id.cardHistorial);
        cardCerrarSesion = findViewById(R.id.cardCerrarSesion);
    }

    private void cargarDatosUsuario() {
        tvNombreUsuario.setText(usuario.getNombre());
        tvEmailUsuario.setText(usuario.getEmail());

        if (usuario.getAvatar() == 1) {
            ivAvatar.setImageResource(R.drawable.avatar1);
        } else {
            ivAvatar.setImageResource(R.drawable.avatar2);
        }
    }

    private void setupListeners() {
        cardFavoritos.setOnClickListener(v -> {
            // En un futuro: mostrar favoritos del usuario logueado
            Intent intent = new Intent(this, FavoritoActivity.class);
            startActivity(intent);
        });

        cardHistorial.setOnClickListener(v -> {
            // En un futuro: mostrar historial del usuario logueado
            Intent intent = new Intent(this, SugerirActivity.class);
            startActivity(intent);
        });

        cardCerrarSesion.setOnClickListener(v -> {
            sessionManager.cerrarSesion();
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}