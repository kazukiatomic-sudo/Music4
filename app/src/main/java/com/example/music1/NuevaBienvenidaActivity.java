package com.example.music1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class NuevaBienvenidaActivity extends AppCompatActivity {
    private static final String TAG = "NuevaBienvenida";
    private ImageView avatarGrande;
    private TextView txtBienvenidaPersonal;
    private Button btnExplorar;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nueva_bienvenida);

        avatarGrande = findViewById(R.id.avatarGrande);
        txtBienvenidaPersonal = findViewById(R.id.txtBienvenidaPersonal);
        btnExplorar = findViewById(R.id.btnExplorar);

        prefs = getSharedPreferences("music1", MODE_PRIVATE);

        String nombre = prefs.getString("nombre", "Usuario");
        int avatar = prefs.getInt("avatar", 1);

        txtBienvenidaPersonal.setText("Bienvenido, " + nombre);

        if (avatar == 1) {
            avatarGrande.setImageResource(R.drawable.avatar1);
        } else {
            avatarGrande.setImageResource(R.drawable.avatar2);
        }

        setupAnimations();

        btnExplorar.setOnClickListener(v -> {
            Intent intent = new Intent(NuevaBienvenidaActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setupAnimations() {
        avatarGrande.setAlpha(0f);
        avatarGrande.animate().alpha(1f).setDuration(800).start();

        txtBienvenidaPersonal.setTranslationY(50);
        txtBienvenidaPersonal.animate().translationY(0).alpha(1f).setDuration(600).setStartDelay(200).start();
    }
}