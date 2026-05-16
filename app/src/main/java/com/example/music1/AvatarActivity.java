package com.example.music1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AvatarActivity extends AppCompatActivity {
    private static final String TAG = "AvatarActivity";
    private static final long DELAY_SELECCION = 300;

    // FIX: soportar los 6 avatares disponibles
    private ImageView avatar1, avatar2, avatar3, avatar4, avatar5, avatar6;
    private final ImageView[] avatares = new ImageView[6];
    private SharedPreferences prefs;
    private int avatarSeleccionado = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar);
        Log.i(TAG, "onCreate: Iniciando seleccion de avatar");

        prefs = getSharedPreferences("music1", MODE_PRIVATE);

        avatar1 = findViewById(R.id.avatar1);
        avatar2 = findViewById(R.id.avatar2);
        avatar3 = findViewById(R.id.avatar3);
        avatar4 = findViewById(R.id.avatar4);
        avatar5 = findViewById(R.id.avatar5);
        avatar6 = findViewById(R.id.avatar6);

        avatares[0] = avatar1;
        avatares[1] = avatar2;
        avatares[2] = avatar3;
        avatares[3] = avatar4;
        avatares[4] = avatar5;
        avatares[5] = avatar6;

        for (int i = 0; i < avatares.length; i++) {
            if (avatares[i] == null) continue;
            final int num = i + 1;
            avatares[i].setOnClickListener(v -> seleccionarAvatar(num));
        }

        mostrarAvatarSeleccionado();
    }

    private void mostrarAvatarSeleccionado() {
        avatarSeleccionado = prefs.getInt("avatar", 1);
        actualizarSeleccionVisual(avatarSeleccionado);
    }

    private void actualizarSeleccionVisual(int num) {
        for (int i = 0; i < avatares.length; i++) {
            if (avatares[i] == null) continue;
            if (i + 1 == num) {
                avatares[i].setBackgroundResource(R.drawable.borde_seleccionado);
            } else {
                avatares[i].setBackground(null);
            }
        }
    }

    private void seleccionarAvatar(int num) {
        Log.i(TAG, "seleccionarAvatar: Avatar " + num + " seleccionado");
        avatarSeleccionado = num;
        actualizarSeleccionVisual(num);

        prefs.edit()
            .putInt("avatar", num)
            .putBoolean("bienvenida_completada", true)
            .apply();

        Toast.makeText(this, "Avatar " + num + " seleccionado", Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(AvatarActivity.this, TipoMusicaActivity.class);
            startActivity(intent);
            finish();
        }, DELAY_SELECCION);
    }
}
