package com.example.music1.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TagEditorManager {
    private static final String TAG = "TagEditorManager";
    private static final String PREF_NAME = "music_tags_v2"; // ✅ Versión nueva
    private static TagEditorManager instance;
    private SharedPreferences prefs;

    private static final String PREFIX_TITULO = "titulo_";
    private static final String PREFIX_ARTISTA = "artista_";
    private static final String PREFIX_ALBUM = "album_";
    private static final String PREFIX_PORTADA = "portada_";

    private TagEditorManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "TagEditorManager inicializado");
    }

    public static synchronized TagEditorManager getInstance(Context context) {
        if (instance == null) {
            instance = new TagEditorManager(context.getApplicationContext());
        }
        return instance;
    }

    // ✅ CORREGIDO: Usa hash determinístico basado en ID + URI string
    private String getKey(long id, String uri) {
        String raw = id + "_" + (uri != null ? uri : "null");
        return md5(raw);
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "md5 error", e);
            return String.valueOf(input.hashCode()); // fallback
        }
    }

    public void guardarTitulo(long id, String uri, String titulo) {
        prefs.edit().putString(PREFIX_TITULO + getKey(id, uri), titulo).apply();
    }

    public String getTitulo(long id, String uri, String tituloOriginal) {
        String guardado = prefs.getString(PREFIX_TITULO + getKey(id, uri), "");
        return guardado.isEmpty() ? tituloOriginal : guardado;
    }

    public void guardarArtista(long id, String uri, String artista) {
        prefs.edit().putString(PREFIX_ARTISTA + getKey(id, uri), artista).apply();
    }

    public String getArtista(long id, String uri, String artistaOriginal) {
        String guardado = prefs.getString(PREFIX_ARTISTA + getKey(id, uri), "");
        return guardado.isEmpty() ? artistaOriginal : guardado;
    }

    public void guardarAlbum(long id, String uri, String album) {
        prefs.edit().putString(PREFIX_ALBUM + getKey(id, uri), album).apply();
    }

    public String getAlbum(long id, String uri, String albumOriginal) {
        String guardado = prefs.getString(PREFIX_ALBUM + getKey(id, uri), "");
        return guardado.isEmpty() ? albumOriginal : guardado;
    }

    public void guardarPortada(long id, String uri, Bitmap bitmap) {
        String encoded = bitmapToBase64(bitmap);
        prefs.edit().putString(PREFIX_PORTADA + getKey(id, uri), encoded).apply();
    }

    public Bitmap getPortada(long id, String uri) {
        String encoded = prefs.getString(PREFIX_PORTADA + getKey(id, uri), "");
        if (!encoded.isEmpty()) {
            return base64ToBitmap(encoded);
        }
        return null;
    }

    public void eliminarPortada(long id, String uri) {
        prefs.edit().remove(PREFIX_PORTADA + getKey(id, uri)).apply();
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String encoded) {
        byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}