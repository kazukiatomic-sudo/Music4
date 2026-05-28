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
 * TagEditorManager - Guarda y recupera metadatos editados por el usuario (título, artista, álbum, portada)
 * Las portadas se guardan como archivos JPEG en el directorio interno de la app.
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
        if (!coversDir.exists()) {
            boolean creado = coversDir.mkdirs();
            Log.d(TAG, "Directorio de portadas creado: " + creado + " en " + coversDir.getAbsolutePath());
        }
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

    // ✅ CORREGIDO: MD5 con manejo de excepción robusto
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
            Log.e(TAG, "MD5 no disponible, usando hashCode como fallback", e);
            // Fallback seguro
            return String.valueOf(input.hashCode());
        }
    }

    public void guardarTitulo(long id, String uri, String titulo) {
        if (titulo == null || titulo.trim().isEmpty()) return;
        prefs.edit().putString(PREFIX_TITULO + getKey(id, uri), titulo.trim()).apply();
        Log.d(TAG, "Título guardado: " + titulo);
    }

    public String getTitulo(long id, String uri, String tituloOriginal) {
        String key = PREFIX_TITULO + getKey(id, uri);
        String guardado = prefs.getString(key, "");
        return guardado.isEmpty() ? (tituloOriginal != null ? tituloOriginal : "Sin título") : guardado;
    }

    public void guardarArtista(long id, String uri, String artista) {
        if (artista == null || artista.trim().isEmpty()) return;
        prefs.edit().putString(PREFIX_ARTISTA + getKey(id, uri), artista.trim()).apply();
        Log.d(TAG, "Artista guardado: " + artista);
    }

    public String getArtista(long id, String uri, String artistaOriginal) {
        String key = PREFIX_ARTISTA + getKey(id, uri);
        String guardado = prefs.getString(key, "");
        return guardado.isEmpty() ? (artistaOriginal != null ? artistaOriginal : "Artista desconocido") : guardado;
    }

    public void guardarAlbum(long id, String uri, String album) {
        if (album == null || album.trim().isEmpty()) return;
        prefs.edit().putString(PREFIX_ALBUM + getKey(id, uri), album.trim()).apply();
        Log.d(TAG, "Álbum guardado: " + album);
    }

    public String getAlbum(long id, String uri, String albumOriginal) {
        String key = PREFIX_ALBUM + getKey(id, uri);
        String guardado = prefs.getString(key, "");
        return guardado.isEmpty() ? (albumOriginal != null ? albumOriginal : "Álbum desconocido") : guardado;
    }

    /**
     * Guarda la portada como archivo JPEG en disco.
     * ✅ CORREGIDO: Verifica que el bitmap sea válido y no demasiado grande
     */
    public void guardarPortada(long id, String uri, Bitmap bitmap) {
        if (bitmap == null) {
            Log.w(TAG, "guardarPortada: Bitmap nulo, no se guarda");
            return;
        }

        // Limitar tamaño máximo para evitar archivos enormes
        int maxDimension = 1024;
        Bitmap bitmapToSave = bitmap;
        if (bitmap.getWidth() > maxDimension || bitmap.getHeight() > maxDimension) {
            float scale = Math.min((float) maxDimension / bitmap.getWidth(), (float) maxDimension / bitmap.getHeight());
            int newWidth = Math.round(bitmap.getWidth() * scale);
            int newHeight = Math.round(bitmap.getHeight() * scale);
            bitmapToSave = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            if (bitmapToSave != bitmap) {
                Log.d(TAG, "Portada redimensionada de " + bitmap.getWidth() + "x" + bitmap.getHeight() +
                        " a " + newWidth + "x" + newHeight);
            }
        }

        String key = getKey(id, uri);
        File coverFile = new File(coversDir, key + ".jpg");

        try (FileOutputStream fos = new FileOutputStream(coverFile)) {
            bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            prefs.edit().putString(PREFIX_PORTADA_PATH + key, coverFile.getAbsolutePath()).apply();
            Log.d(TAG, "Portada guardada en: " + coverFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error guardando portada", e);
        } finally {
            // Si se creó un bitmap redimensionado y es diferente al original, reciclarlo
            if (bitmapToSave != bitmap && bitmapToSave != null) {
                bitmapToSave.recycle();
            }
        }
    }

    /**
     * ✅ CORREGIDO: Lee la portada desde el archivo en disco con manejo de memoria
     */
    public Bitmap getPortada(long id, String uri) {
        String key = getKey(id, uri);
        String path = prefs.getString(PREFIX_PORTADA_PATH + key, "");
        if (path.isEmpty()) return null;

        File coverFile = new File(path);
        if (!coverFile.exists()) {
            prefs.edit().remove(PREFIX_PORTADA_PATH + key).apply();
            Log.w(TAG, "Archivo de portada no existe: " + path);
            return null;
        }

        try {
            // Decodificar con opciones para limitar memoria
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Usa menos memoria que ARGB_8888
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);

            if (bitmap == null) {
                Log.e(TAG, "No se pudo decodificar la portada: " + path);
                prefs.edit().remove(PREFIX_PORTADA_PATH + key).apply();
                coverFile.delete();
            }
            return bitmap;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "OutOfMemoryError al cargar portada", e);
            return null;
        }
    }

    /**
     * Elimina la portada (archivo en disco y referencia en prefs)
     */
    public void eliminarPortada(long id, String uri) {
        String key = getKey(id, uri);
        String path = prefs.getString(PREFIX_PORTADA_PATH + key, "");
        if (!path.isEmpty()) {
            File file = new File(path);
            if (file.exists()) {
                boolean deleted = file.delete();
                Log.d(TAG, "Archivo de portada eliminado: " + deleted);
            }
            prefs.edit().remove(PREFIX_PORTADA_PATH + key).apply();
        }
    }

    /**
     * Limpia todas las portadas en caché (útil para liberar espacio)
     */
    public void limpiarCachePortadas() {
        if (coversDir != null && coversDir.exists()) {
            File[] files = coversDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".jpg")) {
                        file.delete();
                    }
                }
            }
        }
        // También limpiar las referencias en SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith(PREFIX_PORTADA_PATH)) {
                editor.remove(key);
            }
        }
        editor.apply();
        Log.d(TAG, "Caché de portadas limpiado");
    }
}