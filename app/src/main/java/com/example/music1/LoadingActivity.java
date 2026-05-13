package com.example.music1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class LoadingActivity extends AppCompatActivity {
    private static final String TAG = "LoadingActivity";
    // Reducimos la duración a 1200ms para que sea una transición rápida y elegante
    private static final int LOADING_DURATION = 1200; 
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Asegúrate de que en activity_loading.xml el fondo sea negro
        setContentView(R.layout.activity_loading); 
        Log.i(TAG, "onCreate: Iniciando LoadingActivity (Modo Rápido)");

        prefs = getSharedPreferences("music1", MODE_PRIVATE);

        // Ya no cargamos el VideoView ni el Uri del video. Directo al temporizador.
        new Handler().postDelayed(() -> {
            boolean esPrimeraVez = esPrimeraVez();
            Log.d(TAG, "postDelayed: esPrimeraVez=" + esPrimeraVez);

            Intent intent;
            if (esPrimeraVez) {
                intent = new Intent(LoadingActivity.this, MainActivity.class);
                Log.i(TAG, "Navegando a MainActivity (Registro)");
            } else {
                intent = new Intent(LoadingActivity.this, TipoMusicaActivity.class);
                Log.i(TAG, "Navegando a TipoMusicaActivity");
            }

            startActivity(intent);
            // Mantenemos la transición suave (fade) que es más profesional
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, LOADING_DURATION);
    }

    private boolean esPrimeraVez() {
        boolean nombreGuardado = !prefs.getString("nombre", "").isEmpty();
        boolean avatarGuardado = prefs.contains("avatar");
        boolean bienvenidaCompletada = prefs.getBoolean("bienvenida_completada", false);

        boolean yaRegistrado = bienvenidaCompletada && nombreGuardado && avatarGuardado;
        return !yaRegistrado;
    }
}