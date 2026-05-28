package com.example.music1;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.music1.utils.SessionManager;
import com.google.android.material.imageview.ShapeableImageView;

public class TipoMusicaActivity extends AppCompatActivity {
    private static final String TAG = "TipoMusicaActivity";
    private CardView cardLocal, cardOnline, cardPerfil;
    private Button btnLocal, btnOnline;
    private ShapeableImageView ivAvatarPerfil;
    private TextView tvNombreUsuario, tvTituloSeleccion, tvSubtitulo, tvFooter;
    private SharedPreferences prefs;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tipo_musica);
        Log.i(TAG, "onCreate: Iniciando TipoMusicaActivity");

        initViews();
        cargarDatosUsuario();
        setupListeners();
        iniciarAnimaciones();
    }

    private void initViews() {
        cardPerfil = findViewById(R.id.cardPerfil);
        cardLocal = findViewById(R.id.cardLocal);
        cardOnline = findViewById(R.id.cardOnline);
        btnLocal = findViewById(R.id.btnLocal);
        btnOnline = findViewById(R.id.btnOnline);
        ivAvatarPerfil = findViewById(R.id.ivAvatarPerfil);
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        tvTituloSeleccion = findViewById(R.id.tvTituloSeleccion);
        tvSubtitulo = findViewById(R.id.tvSubtitulo);
        tvFooter = findViewById(R.id.tvFooter);

        prefs = getSharedPreferences("music1", MODE_PRIVATE);
        sessionManager = new SessionManager(this);
    }

    // Solo reemplaza el método cargarDatosUsuario() en TipoMusicaActivity

    private void cargarDatosUsuario() {
        String nombre = prefs.getString("nombre", "Usuario");
        int avatar = prefs.getInt("avatar", 1);

        tvNombreUsuario.setText(nombre);

        // ✅ FIX #17: Soporte para los 6 avatares
        switch (avatar) {
            case 1:
                ivAvatarPerfil.setImageResource(R.drawable.avatar1);
                break;
            case 2:
                ivAvatarPerfil.setImageResource(R.drawable.avatar2);
                break;
            case 3:
                ivAvatarPerfil.setImageResource(R.drawable.avatar3);
                break;
            case 4:
                ivAvatarPerfil.setImageResource(R.drawable.avatar4);
                break;
            case 5:
                ivAvatarPerfil.setImageResource(R.drawable.avatar5);
                break;
            case 6:
                ivAvatarPerfil.setImageResource(R.drawable.avatar6);
                break;
            default:
                ivAvatarPerfil.setImageResource(R.drawable.avatar1);
                break;
        }
    }

    private void setupListeners() {
        View.OnClickListener clickListener = v -> {
            v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                    })
                    .start();

            new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (v == cardLocal || v == btnLocal) {
                    irALocal();
                } else if (v == cardOnline || v == btnOnline) {
                    irAOnline();
                }
            }, 200);
        };

        cardLocal.setOnClickListener(clickListener);
        btnLocal.setOnClickListener(clickListener);
        cardOnline.setOnClickListener(clickListener);
        btnOnline.setOnClickListener(clickListener);
    }

    private void irALocal() {
        Intent intent = new Intent(TipoMusicaActivity.this, ListaMusicaLocalActivity.class);
        startActivity(intent);
    }

    private void irAOnline() {
        // Verificar si ya hay sesión activa
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            // Si ya está logueado, va directo al perfil
            Intent intent = new Intent(TipoMusicaActivity.this, PerfilUsuarioActivity.class);
            startActivity(intent);
        } else {
            // Si no, va al login
            Intent intent = new Intent(TipoMusicaActivity.this, LoginActivity.class);
            startActivity(intent);
        }
    }

    private void iniciarAnimaciones() {
        cardPerfil.setAlpha(0f);
        cardPerfil.setTranslationY(50f);
        cardPerfil.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(600)
                .setStartDelay(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        tvTituloSeleccion.setAlpha(0f);
        tvTituloSeleccion.setTranslationY(30f);
        tvTituloSeleccion.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(500)
                .setStartDelay(400)
                .start();

        tvSubtitulo.setAlpha(0f);
        tvSubtitulo.setTranslationY(30f);
        tvSubtitulo.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(500)
                .setStartDelay(600)
                .start();

        cardLocal.setAlpha(0f);
        cardLocal.setTranslationY(50f);
        cardLocal.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(600)
                .setStartDelay(800)
                .start();

        cardOnline.setAlpha(0f);
        cardOnline.setTranslationY(50f);
        cardOnline.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(600)
                .setStartDelay(1000)
                .start();

        tvFooter.setAlpha(0f);
        tvFooter.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(1200)
                .start();

        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_infinite);
        rotateAnimation.setDuration(3000);
        ivAvatarPerfil.startAnimation(rotateAnimation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDatosUsuario();
        // FIX Bajo 2: reanudar animación del avatar al volver a la pantalla
        if (ivAvatarPerfil != null) {
            android.view.animation.Animation rot = android.view.animation.AnimationUtils
                .loadAnimation(this, R.anim.rotate_infinite);
            rot.setDuration(3000);
            ivAvatarPerfil.startAnimation(rot);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // FIX Bajo 2: detener animación cuando la pantalla no es visible para ahorrar CPU
        if (ivAvatarPerfil != null) ivAvatarPerfil.clearAnimation();
    }
}