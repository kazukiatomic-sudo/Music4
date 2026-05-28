package com.example.music1;

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

        sessionManager = new SessionManager(this);
        usuario = sessionManager.getUsuario();

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

        // ✅ FIX #17: Soporte para los 6 avatares
        int avatar = usuario.getAvatar();
        switch (avatar) {
            case 1:
                ivAvatar.setImageResource(R.drawable.avatar1);
                break;
            case 2:
                ivAvatar.setImageResource(R.drawable.avatar2);
                break;
            case 3:
                ivAvatar.setImageResource(R.drawable.avatar3);
                break;
            case 4:
                ivAvatar.setImageResource(R.drawable.avatar4);
                break;
            case 5:
                ivAvatar.setImageResource(R.drawable.avatar5);
                break;
            case 6:
                ivAvatar.setImageResource(R.drawable.avatar6);
                break;
            default:
                ivAvatar.setImageResource(R.drawable.avatar1);
                break;
        }
    }

    private void setupListeners() {
        cardFavoritos.setOnClickListener(v -> {
            Intent intent = new Intent(this, FavoritoActivity.class);
            startActivity(intent);
        });

        cardHistorial.setOnClickListener(v -> {
            // ✅ FIX #35: Ir a historial real (crear esta activity o usar SugerirActivity por ahora)
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