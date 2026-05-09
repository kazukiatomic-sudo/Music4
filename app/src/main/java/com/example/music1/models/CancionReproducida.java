package com.example.music1.models;

import android.util.Log;

public class CancionReproducida {
    private static final String TAG = "CancionReproducida";
    private Cancion cancion;
    private int contador;

    public CancionReproducida(Cancion cancion, int contador) {
        this.cancion = cancion;
        this.contador = contador;
    }

    public Cancion getCancion() { return cancion; }
    public int getContador() { return contador; }
    public void setContador(int contador) { this.contador = contador; }
    public void incrementar() { this.contador++; }
}