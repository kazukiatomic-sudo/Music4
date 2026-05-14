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

// ✅ Usar el sistema de sesión unificado para evitar cierres
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(MainActivity.this, PerfilUsuarioActivity.class));
            finish();
            return;
        }

        btnGuardar.setOnClickListener(v -> {
            if (editNombre == null) return; // Evita que la app se cierre si el ID está mal

            String nombreUsuario = editNombre.getText().toString().trim();
            if (nombreUsuario.isEmpty()) {
                editNombre.setError("Escribe tu nombre");
                return;
            }

            // Guardar nombre y crear sesión básica
            sessionManager.guardarUsuario(new com.example.music1.models.Usuario(0, nombreUsuario, ""));
            
            // Ir a la siguiente pantalla
            startActivity(new Intent(MainActivity.this, AvatarActivity.class));
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