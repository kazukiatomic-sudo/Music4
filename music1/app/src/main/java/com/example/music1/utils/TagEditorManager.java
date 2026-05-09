package com.example.music1.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import com.example.music1.models.Cancion;

import java.io.ByteArrayOutputStream;

public class TagEditorManager {
    private static final String TAG = "TagEditorManager";
    private static final String PREF_NAME = "music_tags";
    private static TagEditorManager instance;
    private SharedPreferences prefs;
    private Context context;

    private static final String PREFIX_TITULO = "titulo_";
    private static final String PREFIX_ARTISTA = "artista_";
    private static final String PREFIX_ALBUM = "album_";
    private static final String PREFIX_PORTADA = "portada_";

    private TagEditorManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "TagEditorManager inicializado");
    }

    public static synchronized TagEditorManager getInstance(Context context) {
        if (instance == null) {
            instance = new TagEditorManager(context.getApplicationContext());
            Log.d(TAG, "getInstance: Nueva instancia creada");
        }
        return instance;
    }

    private String getKey(long id, String uri) {
        String key = id + "_" + (uri != null ? uri.hashCode() : "raw_" + id);
        Log.v(TAG, "getKey: " + key);
        return key;
    }

    public void guardarTitulo(long id, String uri, String titulo) {
        String key = PREFIX_TITULO + getKey(id, uri);
        prefs.edit().putString(key, titulo).apply();
        Log.d(TAG, "guardarTitulo: ID=" + id + ", titulo=" + titulo);
    }

    public String getTitulo(long id, String uri, String tituloOriginal) {
        String key = PREFIX_TITULO + getKey(id, uri);
        String guardado = prefs.getString(key, "");
        String resultado = guardado.isEmpty() ? tituloOriginal : guardado;
        Log.v(TAG, "getTitulo: ID=" + id + ", resultado=" + resultado);
        return resultado;
    }

    public void guardarArtista(long id, String uri, String artista) {
        String key = PREFIX_ARTISTA + getKey(id, uri);
        prefs.edit().putString(key, artista).apply();
        Log.d(TAG, "guardarArtista: ID=" + id + ", artista=" + artista);
    }

    public String getArtista(long id, String uri, String artistaOriginal) {
        String key = PREFIX_ARTISTA + getKey(id, uri);
        String guardado = prefs.getString(key, "");
        String resultado = guardado.isEmpty() ? artistaOriginal : guardado;
        Log.v(TAG, "getArtista: ID=" + id + ", resultado=" + resultado);
        return resultado;
    }

    public void guardarAlbum(long id, String uri, String album) {
        String key = PREFIX_ALBUM + getKey(id, uri);
        prefs.edit().putString(key, album).apply();
        Log.d(TAG, "guardarAlbum: ID=" + id + ", album=" + album);
    }

    public String getAlbum(long id, String uri, String albumOriginal) {
        String key = PREFIX_ALBUM + getKey(id, uri);
        String guardado = prefs.getString(key, "");
        String resultado = guardado.isEmpty() ? albumOriginal : guardado;
        Log.v(TAG, "getAlbum: ID=" + id + ", resultado=" + resultado);
        return resultado;
    }

    public String getAlbumPersonalizado(long id, String uri, String albumOriginal) {
        return getAlbum(id, uri, albumOriginal);
    }

    public void guardarPortada(long id, String uri, Bitmap bitmap) {
        String key = PREFIX_PORTADA + getKey(id, uri);
        String encoded = bitmapToBase64(bitmap);
        prefs.edit().putString(key, encoded).apply();
        Log.d(TAG, "guardarPortada: ID=" + id);
    }

    public Bitmap getPortada(long id, String uri) {
        String key = PREFIX_PORTADA + getKey(id, uri);
        String encoded = prefs.getString(key, "");
        if (!encoded.isEmpty()) {
            Log.v(TAG, "getPortada: ID=" + id + ", portada encontrada");
            return base64ToBitmap(encoded);
        }
        Log.v(TAG, "getPortada: ID=" + id + ", sin portada personalizada");
        return null;
    }

    public void eliminarPortada(long id, String uri) {
        String key = PREFIX_PORTADA + getKey(id, uri);
        prefs.edit().remove(key).apply();
        Log.d(TAG, "eliminarPortada: ID=" + id);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String encoded) {
        byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}