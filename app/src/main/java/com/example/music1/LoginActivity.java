package com.example.music1;

import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.music1.api.ApiClient;
import com.example.music1.api.LoginRequest;
import com.example.music1.api.MusicApi;
import com.example.music1.api.RespuestaLogin;
import com.example.music1.models.Favorito;
import com.example.music1.models.FavoritoRemoto;
import com.example.music1.utils.FavoritosManager;
import com.example.music1.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegistro;
    private ProgressBar progressBar;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            irActividadPrincipal();
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegistro = findViewById(R.id.tvRegistro);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> realizarLogin());
        tvRegistro.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegistroActivity.class));
        });
    }

    private void realizarLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        MusicApi api = ApiClient.getApi();
        Call<RespuestaLogin> call = api.login(new LoginRequest(email, password));
        call.enqueue(new Callback<RespuestaLogin>() {
            @Override
            public void onResponse(Call<RespuestaLogin> call, Response<RespuestaLogin> response) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    sessionManager.guardarUsuario(response.body().usuario);
                    sincronizarFavoritosDesdeServidor(response.body().usuario.getId());
                    Toast.makeText(LoginActivity.this, "Bienvenido " + response.body().usuario.getNombre(), Toast.LENGTH_SHORT).show();
                    irActividadPrincipal();
                } else {
                    String msg = response.body() != null ? response.body().message : "Error de conexión";
                    Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RespuestaLogin> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Reemplaza el método sincronizarFavoritosDesdeServidor() en LoginActivity

    private void sincronizarFavoritosDesdeServidor(int usuarioId) {
        MusicApi api = ApiClient.getApi();
        Call<List<FavoritoRemoto>> call = api.getFavoritos(usuarioId);
        call.enqueue(new Callback<List<FavoritoRemoto>>() {
            @Override
            public void onResponse(Call<List<FavoritoRemoto>> call, Response<List<FavoritoRemoto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    FavoritosManager manager = FavoritosManager.getInstance(LoginActivity.this);

                    // ✅ FIX #32: Limpiar SOLO favoritos locales que están en servidor?
                    // Mejor: No limpiar todo, sino hacer merge sin duplicados
                    List<Favorito> favoritosExistentes = manager.getFavoritos();

                    for (FavoritoRemoto fr : response.body()) {
                        if (fr.cancion_id == null || fr.cancion_id.isEmpty()) continue;
                        try {
                            long id = Long.parseLong(fr.cancion_id);

                            // ✅ Verificar si ya existe localmente
                            boolean yaExiste = false;
                            for (Favorito existente : favoritosExistentes) {
                                if (existente.getId() == id &&
                                        (existente.getUri() != null && existente.getUri().equals(fr.uri))) {
                                    yaExiste = true;
                                    break;
                                }
                            }

                            if (!yaExiste) {
                                Favorito fav = new Favorito(
                                        id,
                                        fr.titulo,
                                        fr.artista,
                                        fr.album,
                                        fr.duracion,
                                        fr.uri,
                                        fr.album_id
                                );
                                manager.agregarFavorito(fav);
                            }
                        } catch (NumberFormatException e) {
                            Log.e("LoginActivity", "cancion_id no válido: " + fr.cancion_id, e);
                        }
                    }
                    Log.d("LoginActivity", "Sincronización completada. Total favoritos: " + manager.getFavoritos().size());
                }
            }

            @Override
            public void onFailure(Call<List<FavoritoRemoto>> call, Throwable t) {
                Log.e("LoginActivity", "Error al sincronizar favoritos", t);
            }
        });
    }
    private void irActividadPrincipal() {
        Intent intent = new Intent(LoginActivity.this, PerfilUsuarioActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
