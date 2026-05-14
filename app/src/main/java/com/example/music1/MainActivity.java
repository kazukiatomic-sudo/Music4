package com.example.music1;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.widget.Toast;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private EditText editNombre;
    private Button btnGuardar;
    private ImageView avatar;
    private SharedPreferences prefs;
    private static final int AVATAR_PREDETERMINADO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: Iniciando MainActivity");

        editNombre = findViewById(R.id.editNombre);
        btnGuardar = findViewById(R.id.btnGuardar);
        avatar = findViewById(R.id.avatar);

        prefs = getSharedPreferences("music1", MODE_PRIVATE);

// ✅ Verificar si ya pasó por el flujo completo
        String nombre = prefs.getString("nombre", "");
        boolean bienvenidaCompletada = prefs.getBoolean("bienvenida_completada", false);

        if (bienvenidaCompletada && !nombre.isEmpty()) {
            Log.d(TAG, "Usuario ya registrado: " + nombre);
            startActivity(new Intent(MainActivity.this, TipoMusicaActivity.class));
            finish();
            return;
        }

        btnGuardar.setOnClickListener(v -> {
            String nombreUsuario = editNombre.getText().toString().trim();
            Log.d(TAG, "btnGuardar click: nombre=" + nombreUsuario);

            if (nombreUsuario.isEmpty()) {
                editNombre.setError("El nombre es requerido");
                Toast.makeText(this, "El nombre es requerido", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("nombre", nombreUsuario);
            editor.apply();
            Log.i(TAG, "Nombre guardado: " + nombreUsuario);

            Intent i = new Intent(MainActivity.this, AvatarActivity.class);
            startActivity(i);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarAvatar();
    }

    private void actualizarAvatar() {
        int avatarGuardado = prefs.getInt("avatar", AVATAR_PREDETERMINADO);
        if (avatarGuardado == 1) {
            avatar.setImageResource(R.drawable.avatar1);
        } else {
            avatar.setImageResource(R.drawable.avatar2);
        }
    }
}