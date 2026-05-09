package com.example.music1.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.example.music1.models.Cancion;

import java.io.File;

public class ArchivoManager {
    private static final String TAG = "ArchivoManager";
    private Context context;

    public ArchivoManager(Context context) {
        this.context = context;
        Log.d(TAG, "ArchivoManager inicializado");
    }

    public boolean eliminarCancion(Cancion cancion) {
        Log.d(TAG, "eliminarCancion: Intentando eliminar: " + (cancion != null ? cancion.getTitulo() : "null"));

        try {
            if (cancion.esCancionDePrueba()) {
                Log.w(TAG, "eliminarCancion: Cancion de prueba, no se puede eliminar");
                Toast.makeText(context, "Las canciones de prueba no se pueden eliminar", Toast.LENGTH_SHORT).show();
                return false;
            }

            Uri uri = cancion.getUri();
            if (uri == null) {
                Log.e(TAG, "eliminarCancion: URI es null");
                return false;
            }

            ContentResolver resolver = context.getContentResolver();
            int deleted = resolver.delete(uri, null, null);

            if (deleted > 0) {
                Log.i(TAG, "eliminarCancion: Cancion eliminada exitosamente: " + cancion.getTitulo());
                Toast.makeText(context, "✓ Canción eliminada: " + cancion.getTitulo(), Toast.LENGTH_LONG).show();
                return true;
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                String path = obtenerRutaRealDesdeUri(uri);
                if (path != null) {
                    File file = new File(path);
                    if (file.exists()) {
                        boolean eliminado = file.delete();
                        if (eliminado) {
                            resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    MediaStore.Audio.Media.DATA + "=?", new String[]{path});
                            Log.i(TAG, "eliminarCancion: Archivo eliminado directamente: " + path);
                            Toast.makeText(context, "✓ Canción eliminada", Toast.LENGTH_LONG).show();
                            return true;
                        }
                    }
                }
            }

            Log.w(TAG, "eliminarCancion: No se pudo eliminar la cancion");
            Toast.makeText(context, "No se pudo eliminar la canción", Toast.LENGTH_SHORT).show();
            return false;

        } catch (Exception e) {
            Log.e(TAG, "eliminarCancion: Error", e);
            Toast.makeText(context, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private String obtenerRutaRealDesdeUri(Uri uri) {
        String[] projection = {MediaStore.Audio.Media.DATA};
        try (android.database.Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                String path = cursor.getString(columnIndex);
                Log.v(TAG, "obtenerRutaRealDesdeUri: " + path);
                return path;
            }
        } catch (Exception e) {
            Log.e(TAG, "obtenerRutaRealDesdeUri: Error", e);
        }
        Log.w(TAG, "obtenerRutaRealDesdeUri: No se pudo obtener la ruta");
        return null;
    }
}