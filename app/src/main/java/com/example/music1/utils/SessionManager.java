package com.example.music1.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.music1.models.Usuario;
import com.google.gson.Gson;

public class SessionManager {
    private static final String PREF_NAME = "session_pref";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USUARIO = "usuario";

    // FIX: solo guardamos prefs, no un editor compartido
    // Antes: editor = prefs.edit() en constructor podía causar colisiones entre instancias
    private SharedPreferences prefs;
    private Gson gson;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void guardarUsuario(Usuario usuario) {
        // FIX: obtener editor localmente en cada operación de escritura
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USUARIO, gson.toJson(usuario));
        editor.apply();
    }

    public Usuario getUsuario() {
        String json = prefs.getString(KEY_USUARIO, null);
        if (json != null) {
            try {
                return gson.fromJson(json, Usuario.class);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void cerrarSesion() {
        // FIX: obtener editor localmente
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    public int getUsuarioId() {
        Usuario u = getUsuario();
        return u != null ? u.getId() : -1;
    }

    public String getNombreUsuario() {
        Usuario u = getUsuario();
        return u != null ? u.getNombre() : "";
    }

    public int getAvatarUsuario() {
        Usuario u = getUsuario();
        return u != null ? u.getAvatar() : 1;
    }
}
