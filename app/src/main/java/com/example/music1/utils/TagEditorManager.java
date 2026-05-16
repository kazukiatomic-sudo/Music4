package com.example.music1.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * FIX: Las portadas ya NO se guardan en SharedPreferences como Base64 (límite ~1MB,
 * lento y propenso a corrupción). Ahora se guardan como archivos JPEG en el
 * directorio interno de la app (getFilesDir()/covers/), y solo se almacena
 * la ruta en SharedPreferences.
 */
public class TagEditorManager {
    private static final String TAG = "TagEditorManager";
    private static final String PREF_NAME = "music_tags_v3";
    private static TagEditorManager instance;
    private SharedPreferences prefs;
    private File coversDir;

    private static final String PREFIX_TITULO = "titulo_";
    private static final String PREFIX_ARTISTA = "artista_";
    private static final String PREFIX_ALBUM = "album_";
    private static final String PREFIX_PORTADA_PATH = "portada_path_";

    private TagEditorManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        coversDir = new File(context.getApplicationContext().getFilesDir(), "covers");
        if (!coversDir.exists()) coversDir.mkdirs();
        Log.d(TAG, "TagEditorManager inicializado. coversDir=" + coversDir.getAbsolutePath());
    }

    public static synchronized TagEditorManager getInstance(Context context) {
        if (instance == null) {
            instance = new TagEditorManager(context.getApplicationContext());
        }
        return instance;
    }

    private String getKey(long id, String uri) {
        String raw = id + "_" + (uri != null ? uri : "null");
        return md5(raw);
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
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

    /**
     * FIX: guarda la portada como archivo JPEG en disco, no en SharedPreferences.
     */
    public void guardarPortada(long id, String uri, Bitmap bitmap) {
        String key = getKey(id, uri);
        File coverFile = new File(coversDir, key + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(coverFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            prefs.edit().putString(PREFIX_PORTADA_PATH + key, coverFile.getAbsolutePath()).apply();
            Log.d(TAG, "Portada guardada en: " + coverFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error guardando portada", e);
        }
    }

    /**
     * FIX: lee la portada desde el archivo en disco.
     */
    public Bitmap getPortada(long id, String uri) {
        String key = getKey(id, uri);
        String path = prefs.getString(PREFIX_PORTADA_PATH + key, "");
        if (path.isEmpty()) return null;

        File coverFile = new File(path);
        if (!coverFile.exists()) {
            // El archivo fue eliminado externamente — limpiar la preferencia
            prefs.edit().remove(PREFIX_PORTADA_PATH + key).apply();
            return null;
        }
        return BitmapFactory.decodeFile(path);
    }

    /**
     * FIX: eliminar portada borra el archivo en disco y la referencia en prefs.
     */
    public void eliminarPortada(long id, String uri) {
        String key = getKey(id, uri);
        String path = prefs.getString(PREFIX_PORTADA_PATH + key, "");
        if (!path.isEmpty()) {
            new File(path).delete();
            prefs.edit().remove(PREFIX_PORTADA_PATH + key).apply();
        }
    }
}
