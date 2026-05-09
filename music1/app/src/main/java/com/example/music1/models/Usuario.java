package com.example.music1.models;

import java.io.Serializable;

public class Usuario implements Serializable {
    private int id;
    private String nombre;
    private String email;
    private int avatar;

    public Usuario(int id, String nombre, String email, int avatar) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.avatar = avatar;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public int getAvatar() { return avatar; }
}