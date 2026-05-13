package com.example.music1;

import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.content.SharedPreferences;
import android.content.Intent;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class AvatarActivity extends AppCompatActivity {
    private static final String TAG = "AvatarActivity";
    private ImageView avatar1;
    private ImageView avatar2;
    private SharedPreferences prefs;
    private static final int AVATAR_UNO = 1;
    private static final int AVATAR_DOS = 2;
    private static final long DELAY_SELECCION = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar);
        Log.i(TAG, "onCreate: Iniciando seleccion de avatar");

        avatar1 = findViewById(R.id.avatar1);
        avatar2 = findViewById(R.id.avatar2);
        prefs = getSharedPreferences("music1", MODE_PRIVATE);

        mostrarAvatarSeleccionado();

        avatar1.setOnClickListener(v -> seleccionarAvatar(AVATAR_UNO));
        avatar2.setOnClickListener(v -> seleccionarAvatar(AVATAR_DOS));
    }

    private void mostrarAvatarSeleccionado() {
        int avatarGuardado = prefs.getInt("avatar", AVATAR_UNO);
        if (avatarGuardado == AVATAR_UNO) {
            avatar1.setBackgroundResource(R.drawable.borde_seleccionado);
            avatar2.setBackground(null);
        } else if (avatarGuardado == AVATAR_DOS) {
            avatar2.setBackgroundResource(R.drawable.borde_seleccionado);
            avatar1.setBackground(null);
        }
    }

    private void seleccionarAvatar(int avatarNumber) {
        Log.i(TAG, "seleccionarAvatar: Avatar " + avatarNumber + " seleccionado");

        if (avatarNumber == AVATAR_UNO) {
            avatar1.setBackgroundResource(R.drawable.borde_seleccionado);
            avatar2.setBackground(null);
        } else {
            avatar2.setBackgroundResource(R.drawable.borde_seleccionado);
            avatar1.setBackground(null);
        }

        // ✅ CORREGIDO: Usar UN SOLO editor
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("avatar", avatarNumber);
        editor.putBoolean("bienvenida_completada", true);
        editor.apply();

        Toast.makeText(this, "Avatar " + avatarNumber + " seleccionado", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(AvatarActivity.this, TipoMusicaActivity.class);
            startActivity(intent);
            finish();
        }, DELAY_SELECCION);
    }
}