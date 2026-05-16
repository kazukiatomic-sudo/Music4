package com.example.music1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.music1.api.ApiClient;
import com.example.music1.api.MusicApi;
import com.example.music1.api.RegistroRequest;
import com.example.music1.api.RespuestaRegistro;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistroActivity extends AppCompatActivity {

    private EditText etNombre, etEmail, etPassword, etConfirmPassword;
    private ImageView avatar1, avatar2;
    private Button btnRegistro;
    private TextView tvLogin;
    private ProgressBar progressBar;
    private int avatarSeleccionado = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        avatar1 = findViewById(R.id.avatar1);
        avatar2 = findViewById(R.id.avatar2);
        btnRegistro = findViewById(R.id.btnRegistro);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        avatar1.setOnClickListener(v -> seleccionarAvatar(1));
        avatar2.setOnClickListener(v -> seleccionarAvatar(2));

        btnRegistro.setOnClickListener(v -> registrarUsuario());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegistroActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void seleccionarAvatar(int num) {
        avatarSeleccionado = num;
        if (num == 1) {
            avatar1.setBackgroundResource(R.drawable.borde_avatar_seleccionado);
            avatar2.setBackground(null);
        } else {
            avatar2.setBackgroundResource(R.drawable.borde_avatar_seleccionado);
            avatar1.setBackground(null);
        }
    }

    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 4) {
            Toast.makeText(this, "La contraseña debe tener al menos 4 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegistro.setEnabled(false);

        MusicApi api = ApiClient.getApi();
        Call<RespuestaRegistro> call = api.registrar(new RegistroRequest(nombre, email, password, avatarSeleccionado));
        call.enqueue(new Callback<RespuestaRegistro>() {
            @Override
            public void onResponse(Call<RespuestaRegistro> call, Response<RespuestaRegistro> response) {
                progressBar.setVisibility(View.GONE);
                btnRegistro.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(RegistroActivity.this, "Registro exitoso. Ahora inicia sesión.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(RegistroActivity.this, LoginActivity.class));
                    finish();
                } else {
                    String msg = response.body() != null ? response.body().message : "Error desconocido";
                    Toast.makeText(RegistroActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RespuestaRegistro> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnRegistro.setEnabled(true);
                Toast.makeText(RegistroActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
