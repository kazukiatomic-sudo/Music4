package com.example.music1.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.music1.api.AddFavoritoRequest;
import com.example.music1.api.ApiClient;
import com.example.music1.api.MusicApi;
import com.example.music1.api.RemoveFavoritoRequest;
import com.example.music1.api.RespuestaSimple;
import com.example.music1.models.Favorito;
import com.example.music1.models.FavoritoRemoto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritosManager {
    private static final String TAG = "FavoritosManager";
    private static final String PREF_NAME = "favoritos_v2"; // ✅ Nueva versión
    private static final String KEY_FAVORITOS = "lista_favoritos";
    private static FavoritosManager instance;
    private SharedPreferences prefs;
    private Gson gson;
    private List<Favorito> favoritos;

    private FavoritosManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        cargarFavoritos();
        Log.d(TAG, "FavoritosManager inicializado");
    }

    public static synchronized FavoritosManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoritosManager(context.getApplicationContext());
        }
        return instance;
    }

    private void cargarFavoritos() {
        String json = prefs.getString(KEY_FAVORITOS, "");
        if (!json.isEmpty()) {
            try {
                Type type = new TypeToken<List<Favorito>>(){}.getType();
                favoritos = gson.fromJson(json, type);
                Log.i(TAG, "cargarFavoritos: " + (favoritos != null ? favoritos.size() : 0) + " favoritos cargados");
            } catch (Exception e) {
                Log.e(TAG, "Error cargando favoritos", e);
                favoritos = new ArrayList<>();
            }
        } else {
            favoritos = new ArrayList<>();
        }
    }

    private void guardarFavoritos() {
        String json = gson.toJson(favoritos);
        prefs.edit().putString(KEY_FAVORITOS, json).apply();
        Log.d(TAG, "guardarFavoritos: " + favoritos.size() + " favoritos guardados");
    }

    public boolean agregarFavorito(Favorito favorito) {
        Log.d(TAG, "agregarFavorito: " + favorito.getTitulo());
        for (Favorito f : favoritos) {
            if (f.getId() == favorito.getId() && f.getUri().equals(favorito.getUri())) {
                Log.w(TAG, "agregarFavorito: Ya existe en favoritos");
                return false;
            }
        }
        favoritos.add(favorito);
        guardarFavoritos();
        Log.i(TAG, "agregarFavorito: Agregado exitosamente");
        return true;
    }

    public boolean quitarFavorito(long id, String uri) {
        Log.d(TAG, "quitarFavorito: ID=" + id);
        for (int i = 0; i < favoritos.size(); i++) {
            Favorito f = favoritos.get(i);
            if (f.getId() == id && f.getUri().equals(uri)) {
                favoritos.remove(i);
                guardarFavoritos();
                Log.i(TAG, "quitarFavorito: Eliminado: " + f.getTitulo());
                return true;
            }
        }
        Log.w(TAG, "quitarFavorito: No se encontro el favorito");
        return false;
    }

    public List<Favorito> getFavoritos() {
        return new ArrayList<>(favoritos);
    }

    public boolean esFavorito(long id, String uri) {
        for (Favorito f : favoritos) {
            if (f.getId() == id && f.getUri().equals(uri)) {
                return true;
            }
        }
        return false;
    }

    public void limpiarFavoritos() {
        Log.w(TAG, "limpiarFavoritos: Eliminando todos los favoritos");
        favoritos.clear();
        guardarFavoritos();
    }

    // ✅ MÉTODOS CON SINCRONIZACIÓN Y REINTENTOS

    public void agregarFavoritoConSync(Favorito favorito, int usuarioId) {
        agregarFavorito(favorito);
        enviarAlServidorConReintento(() -> {
            MusicApi api = ApiClient.getApi();
            AddFavoritoRequest request = new AddFavoritoRequest();
            request.usuario_id = usuarioId;
            request.favorito = new FavoritoRemoto(favorito);
            return api.addFavorito(request);
        }, "agregar favorito");
    }

    public void quitarFavoritoConSync(long id, String uri, int usuarioId) {
        quitarFavorito(id, uri);
        enviarAlServidorConReintento(() -> {
            MusicApi api = ApiClient.getApi();
            RemoveFavoritoRequest request = new RemoveFavoritoRequest();
            request.usuario_id = usuarioId;
            request.cancion_id = String.valueOf(id);
            return api.removeFavorito(request);
        }, "quitar favorito");
    }

    private interface ApiCall {
        Call<RespuestaSimple> execute();
    }

    private void enviarAlServidorConReintento(ApiCall apiCall, String accion) {
        enviarAlServidorConReintento(apiCall, accion, 0);
    }

    private void enviarAlServidorConReintento(ApiCall apiCall, String accion, int intento) {
        apiCall.execute().enqueue(new Callback<RespuestaSimple>() {
            @Override
            public void onResponse(Call<RespuestaSimple> call, Response<RespuestaSimple> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Log.d(TAG, "Sincronización exitosa: " + accion);
                } else {
                    Log.w(TAG, "Sincronización falló, reintento " + (intento + 1) + "/3: " + accion);
                    if (intento < 2) {
                        new android.os.Handler().postDelayed(() ->
                                enviarAlServidorConReintento(apiCall, accion, intento + 1), 2000);
                    }
                }
            }

            @Override
            public void onFailure(Call<RespuestaSimple> call, Throwable t) {
                Log.e(TAG, "Error de red en " + accion + ", reintento " + (intento + 1) + "/3", t);
                if (intento < 2) {
                    new android.os.Handler().postDelayed(() ->
                            enviarAlServidorConReintento(apiCall, accion, intento + 1), 3000);
                }
            }
        });
    }
}