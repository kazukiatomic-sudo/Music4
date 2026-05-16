package com.example.music1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class NuevaBienvenidaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nueva_bienvenida);

        Button btnExplorar = findViewById(R.id.btnExplorar);

        // FIX: navegar directamente a TipoMusicaActivity en vez de pasar por MainActivity
        // Antes: iba a MainActivity que redirigía a TipoMusicaActivity, generando un stack innecesario
        btnExplorar.setOnClickListener(v -> {
            Intent intent = new Intent(NuevaBienvenidaActivity.this, TipoMusicaActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}
