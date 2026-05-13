package com.example.music1.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.example.music1.models.Cancion;
import com.example.music1.models.CancionReproducida;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistorialManager {
    private static final String TAG = "HistorialManager";
    private static final String PREF_NAME = "historial_v2"; // ✅ Nueva versión
    private static final String KEY_RECIENTES = "canciones_recientes_data";
    private static final String KEY_CONTADORES = "contadores_data";
    private static final int MAX_RECIENTES = 20;

    private static HistorialManager instance;
    private SharedPreferences prefs;
    private Gson gson;
    private List<Cancion> recientes;
    private List<CancionReproducida> masReproducidas;

    // ✅ Clase auxiliar para guardar solo datos mínimos
    private static class CancionData {
        long id;
        String titulo;
        String artista;
        String album;
        long duracion;
        String uriString;
        long albumId;
        int imagenAlbum;
        int rawResource;
    }

    private HistorialManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        cargarDatos();
        Log.d(TAG, "HistorialManager inicializado");
    }

    public static synchronized HistorialManager getInstance(Context context) {
        if (instance == null) {
            instance = new HistorialManager(context.getApplicationContext());
        }
        return instance;
    }

    // ✅ Convierte Cancion a CancionData para guardar
    private CancionData cancionToData(Cancion c) {
        if (c == null) return null;
        CancionData data = new CancionData();
        data.id = c.getId();
        data.titulo = c.getTitulo();
        data.artista = c.getArtista();
        data.album = c.getAlbum();
        data.duracion = c.getDuracion();
        data.uriString = c.getUriString();
        data.albumId = c.getAlbumId();
        data.imagenAlbum = c.getImagenAlbum();
        data.rawResource = c.getRawResource();
        return data;
    }

    // ✅ Convierte CancionData a Cancion
    private Cancion dataToCancion(CancionData data) {
        if (data == null) return null;
        Uri uri = data.uriString != null && !data.uriString.isEmpty() ? Uri.parse(data.uriString) : null;

        if (data.rawResource != -1) {
            // Canción de prueba
            return new Cancion(data.titulo, data.artista, data.rawResource, data.imagenAlbum, (int)(data.duracion/1000));
        } else {
            return new Cancion(data.id, data.titulo, data.artista, data.album, data.duracion, uri, data.albumId);
        }
    }

    private void cargarDatos() {
        // Cargar recientes
        String jsonRecientes = prefs.getString(KEY_RECIENTES, "");
        if (!jsonRecientes.isEmpty()) {
            try {
                Type type = new TypeToken<List<CancionData>>(){}.getType();
                List<CancionData> datos = gson.fromJson(jsonRecientes, type);
                recientes = new ArrayList<>();
                if (datos != null) {
                    for (CancionData data : datos) {
                        Cancion c = dataToCancion(data);
                        if (c != null) {
                            c.restaurarUri();
                            recientes.add(c);
                        }
                    }
                }
                Log.i(TAG, "cargarDatos: " + recientes.size() + " recientes cargados");
            } catch (Exception e) {
                Log.e(TAG, "Error cargando recientes", e);
                recientes = new ArrayList<>();
            }
        } else {
            recientes = new ArrayList<>();
        }

        // Cargar más reproducidas
        String jsonContadores = prefs.getString(KEY_CONTADORES, "");
        if (!jsonContadores.isEmpty()) {
            try {
                Type type = new TypeToken<List<CancionReproducidaData>>(){}.getType();
                List<CancionReproducidaData> datos = gson.fromJson(jsonContadores, type);
                masReproducidas = new ArrayList<>();
                if (datos != null) {
                    for (CancionReproducidaData data : datos) {
                        Cancion c = dataToCancion(data.cancion);
                        if (c != null) {
                            c.restaurarUri();
                            masReproducidas.add(new CancionReproducida(c, data.contador));
                        }
                    }
                }
                Log.i(TAG, "cargarDatos: " + masReproducidas.size() + " más reproducidas cargadas");
            } catch (Exception e) {
                Log.e(TAG, "Error cargando contadores", e);
                masReproducidas = new ArrayList<>();
            }
        } else {
            masReproducidas = new ArrayList<>();
        }
    }

    // ✅ Clase auxiliar para CancionReproducida
    private static class CancionReproducidaData {
        CancionData cancion;
        int contador;
    }

    private void guardarDatos() {
        // Guardar recientes como CancionData
        List<CancionData> datosRecientes = new ArrayList<>();
        for (Cancion c : recientes) {
            if (c != null) {
                datosRecientes.add(cancionToData(c));
            }
        }
        String jsonRecientes = gson.toJson(datosRecientes);
        prefs.edit().putString(KEY_RECIENTES, jsonRecientes).apply();

        // Guardar más reproducidas como CancionReproducidaData
        List<CancionReproducidaData> datosContadores = new ArrayList<>();
        for (CancionReproducida cr : masReproducidas) {
            if (cr != null && cr.getCancion() != null) {
                CancionReproducidaData data = new CancionReproducidaData();
                data.cancion = cancionToData(cr.getCancion());
                data.contador = cr.getContador();
                datosContadores.add(data);
            }
        }
        String jsonContadores = gson.toJson(datosContadores);
        prefs.edit().putString(KEY_CONTADORES, jsonContadores).apply();

        Log.d(TAG, "guardarDatos: Datos guardados");
    }

    public void registrarReproduccion(Cancion cancion) {
        if (cancion == null) {
            Log.w(TAG, "registrarReproduccion: cancion es null");
            return;
        }
        Log.i(TAG, "registrarReproduccion: " + cancion.getTitulo());

        // Asegurar que tiene URI válida
        cancion.restaurarUri();

        // Actualizar recientes
        Cancion existente = null;
        for (Cancion c : recientes) {
            if (c != null && c.getId() == cancion.getId() &&
                    c.getUriString() != null && c.getUriString().equals(cancion.getUriString())) {
                existente = c;
                break;
            }
        }

        if (existente != null) {
            recientes.remove(existente);
        }

        recientes.add(0, cancion);

        if (recientes.size() > MAX_RECIENTES) {
            recientes.remove(recientes.size() - 1);
        }

        // Actualizar contadores
        boolean encontrado = false;
        for (CancionReproducida cr : masReproducidas) {
            if (cr != null && cr.getCancion() != null &&
                    cr.getCancion().getId() == cancion.getId() &&
                    cr.getCancion().getUriString() != null &&
                    cr.getCancion().getUriString().equals(cancion.getUriString())) {
                cr.incrementar();
                encontrado = true;
                break;
            }
        }
        if (!encontrado) {
            masReproducidas.add(new CancionReproducida(cancion, 1));
        }

        Collections.sort(masReproducidas, (a, b) -> {
            int countA = a != null ? a.getContador() : 0;
            int countB = b != null ? b.getContador() : 0;
            return Integer.compare(countB, countA);
        });

        guardarDatos();
        Log.d(TAG, "registrarReproduccion: Registro completado");
    }

    public List<Cancion> getRecientes() {
        List<Cancion> copia = new ArrayList<>();
        for (Cancion c : recientes) {
            if (c != null) {
                c.restaurarUri();
                copia.add(c);
            }
        }
        Log.v(TAG, "getRecientes: Retornando " + copia.size() + " canciones");
        return copia;
    }

    public List<CancionReproducida> getMasReproducidas() {
        List<CancionReproducida> copia = new ArrayList<>();
        for (CancionReproducida cr : masReproducidas) {
            if (cr != null && cr.getCancion() != null) {
                cr.getCancion().restaurarUri();
                copia.add(cr);
            }
        }
        Log.v(TAG, "getMasReproducidas: Retornando " + copia.size() + " canciones");
        return copia;
    }
}