package com.example.music1.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.music1.models.Usuario;

public class SessionManager {
    private static final String PREF_NAME = "user_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_AVATAR = "user_avatar";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void guardarUsuario(Usuario usuario) {
        editor.putInt(KEY_USER_ID, usuario.getId());
        editor.putString(KEY_USER_NAME, usuario.getNombre());
        editor.putString(KEY_USER_EMAIL, usuario.getEmail());
        editor.putInt(KEY_USER_AVATAR, usuario.getAvatar());
        editor.apply();
    }

    public Usuario getUsuario() {
        int id = prefs.getInt(KEY_USER_ID, -1);
        if (id == -1) return null;
        String nombre = prefs.getString(KEY_USER_NAME, "");
        String email = prefs.getString(KEY_USER_EMAIL, "");
        int avatar = prefs.getInt(KEY_USER_AVATAR, 1);
        return new Usuario(id, nombre, email, avatar);
    }

    public void cerrarSesion() {
        editor.clear().apply();
    }

    public boolean isLoggedIn() {
        return prefs.getInt(KEY_USER_ID, -1) != -1;
    }
public void logout() {
    SharedPreferences.Editor editor = prefs.edit();
    editor.clear();
    editor.apply();
}
}