package com.example.music1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class LoadingActivity extends AppCompatActivity {
    private static final String TAG = "LoadingActivity";
    private static final int LOADING_DURATION = 2500;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        Log.i(TAG, "onCreate: Iniciando LoadingActivity");

        prefs = getSharedPreferences("music1", MODE_PRIVATE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean esPrimeraVez = esPrimeraVez();
            Log.d(TAG, "postDelayed: esPrimeraVez=" + esPrimeraVez);

            Intent intent;
            if (esPrimeraVez) {
                intent = new Intent(LoadingActivity.this, MainActivity.class);
                Log.i(TAG, "Primera vez, navegando a MainActivity");
            } else {
                intent = new Intent(LoadingActivity.this, TipoMusicaActivity.class);
                Log.i(TAG, "Usuario registrado, navegando a TipoMusicaActivity");
            }

            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, LOADING_DURATION);
    }

    private boolean esPrimeraVez() {
        boolean nombreGuardado = !prefs.getString("nombre", "").isEmpty();
        boolean avatarGuardado = prefs.contains("avatar");
        boolean bienvenidaCompletada = prefs.getBoolean("bienvenida_completada", false);
        boolean yaRegistrado = bienvenidaCompletada && nombreGuardado && avatarGuardado;

        Log.d(TAG, "esPrimeraVez: nombreGuardado=" + nombreGuardado +
                ", avatarGuardado=" + avatarGuardado +
                ", bienvenidaCompletada=" + bienvenidaCompletada +
                ", resultado=" + !yaRegistrado);

        return !yaRegistrado;
    }
}
