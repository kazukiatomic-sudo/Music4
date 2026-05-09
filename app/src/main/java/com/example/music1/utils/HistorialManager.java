package com.example.music1.utils;

import android.content.Context;
import android.content.SharedPreferences;
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
    private static final String PREF_NAME = "historial";
    private static final String KEY_RECIENTES = "canciones_recientes";
    private static final String KEY_CONTADORES = "contadores";
    private static final int MAX_RECIENTES = 20;

    private static HistorialManager instance;
    private SharedPreferences prefs;
    private Gson gson;
    private List<Cancion> recientes;
    private List<CancionReproducida> masReproducidas;

    private HistorialManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        cargarDatos();
        Log.d(TAG, "HistorialManager inicializado");
    }

    public static synchronized HistorialManager getInstance(Context context) {
        if (instance == null) {
            instance = new HistorialManager(context.getApplicationContext());
            Log.d(TAG, "getInstance: Nueva instancia creada");
        }
        return instance;
    }

    private void cargarDatos() {
        String jsonRecientes = prefs.getString(KEY_RECIENTES, "");
        if (!jsonRecientes.isEmpty()) {
            Type type = new TypeToken<List<Cancion>>(){}.getType();
            recientes = gson.fromJson(jsonRecientes, type);
            if (recientes != null) {
                for (Cancion c : recientes) {
                    if (c != null) c.restaurarUri();
                }
            }
            Log.i(TAG, "cargarDatos: " + (recientes != null ? recientes.size() : 0) + " recientes cargados");
        } else {
            recientes = new ArrayList<>();
        }

        String jsonContadores = prefs.getString(KEY_CONTADORES, "");
        if (!jsonContadores.isEmpty()) {
            Type type = new TypeToken<List<CancionReproducida>>(){}.getType();
            masReproducidas = gson.fromJson(jsonContadores, type);
            if (masReproducidas != null) {
                for (CancionReproducida cr : masReproducidas) {
                    if (cr != null && cr.getCancion() != null) {
                        cr.getCancion().restaurarUri();
                    }
                }
            }
            Log.i(TAG, "cargarDatos: " + (masReproducidas != null ? masReproducidas.size() : 0) + " mas reproducidas cargadas");
        } else {
            masReproducidas = new ArrayList<>();
        }
    }

    private void guardarDatos() {
        String jsonRecientes = gson.toJson(recientes);
        prefs.edit().putString(KEY_RECIENTES, jsonRecientes).apply();

        String jsonContadores = gson.toJson(masReproducidas);
        prefs.edit().putString(KEY_CONTADORES, jsonContadores).apply();
        Log.d(TAG, "guardarDatos: Datos guardados");
    }

    public void registrarReproduccion(Cancion cancion) {
        if (cancion == null) {
            Log.w(TAG, "registrarReproduccion: cancion es null");
            return;
        }
        Log.i(TAG, "registrarReproduccion: " + cancion.getTitulo());

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