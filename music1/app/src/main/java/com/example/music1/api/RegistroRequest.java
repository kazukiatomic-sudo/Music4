package com.example.music1.api;

public class RegistroRequest {
    public String nombre;
    public String email;
    public String password;
    public int avatar;
    public RegistroRequest(String nombre, String email, String password, int avatar) {
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.avatar = avatar;
    }
}