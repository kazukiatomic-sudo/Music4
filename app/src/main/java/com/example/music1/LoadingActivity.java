package com.example.music1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;

public class LoadingActivity extends AppCompatActivity {
    private static final String TAG = "LoadingActivity";
    private static final int LOADING_DURATION = 1200; // ✅ Reducido: pantalla negra rápida
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        Log.i(TAG, "onCreate: Iniciando LoadingActivity");

        prefs = getSharedPreferences("music1", MODE_PRIVATE);

        // ✅ NUEVO: Botón hamburguesa (3 puntos) en la pantalla de carga
        View menuButton = findViewById(R.id.menuButton);
        if (menuButton != null) {
            menuButton.setOnClickListener(v -> mostrarMenuHamburguesa(v));
        }

        // Iniciar flujo normal después del tiempo de carga
        new Handler().postDelayed(() -> {
            boolean esPrimeraVez = esPrimeraVez();
            Log.d(TAG, "postDelayed: esPrimeraVez=" + esPrimeraVez);

            Intent intent;
            if (esPrimeraVez) {
                // Primera vez: flujo de registro
                intent = new Intent(LoadingActivity.this, MainActivity.class);
                Log.i(TAG, "Primera vez, navegando a MainActivity");
            } else {
                // Ya tiene nombre y avatar
                intent = new Intent(LoadingActivity.this, TipoMusicaActivity.class);
                Log.i(TAG, "Usuario registrado, navegando a TipoMusicaActivity");
            }

            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, LOADING_DURATION);
    }

    // ✅ NUEVO: Menú hamburguesa con opciones de navegación
    private void mostrarMenuHamburguesa(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.menu_hamburguesa, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_inicio) {
                // Ir a Inicio (MainActivity o pantalla principal)
                Intent intent = new Intent(LoadingActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.menu_perfil) {
                // Ir a Perfil
                Intent intent = new Intent(LoadingActivity.this, PerfilUsuarioActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.menu_configuracion) {
                // Por ahora: mostrar mensaje (puedes crear SettingsActivity después)
                Toast.makeText(LoadingActivity.this, "Configuración - Proximamente", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    // ✅ CORREGIDO: Detección correcta de primera vez
    private boolean esPrimeraVez() {
        boolean nombreGuardado = !prefs.getString("nombre", "").isEmpty();
        boolean avatarGuardado = prefs.contains("avatar");
        boolean bienvenidaCompletada = prefs.getBoolean("bienvenida_completada", false);

        // Si ya pasó por el flujo de bienvenida completo, no es primera vez
        boolean yaRegistrado = bienvenidaCompletada && nombreGuardado && avatarGuardado;

        Log.d(TAG, "esPrimeraVez: nombreGuardado=" + nombreGuardado +
                ", avatarGuardado=" + avatarGuardado +
                ", bienvenidaCompletada=" + bienvenidaCompletada +
                ", resultado=" + !yaRegistrado);

        return !yaRegistrado;
    }
}
