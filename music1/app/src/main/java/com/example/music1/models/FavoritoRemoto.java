package com.example.music1.models;

public class FavoritoRemoto {
    public String cancion_id;
    public String titulo;
    public String artista;
    public String album;
    public long duracion;
    public String uri;
    public long album_id;
    public String fecha_agregado;

    public FavoritoRemoto() {}

    public FavoritoRemoto(Favorito favorito) {
        this.cancion_id = String.valueOf(favorito.getId());
        this.titulo = favorito.getTitulo();
        this.artista = favorito.getArtista();
        this.album = favorito.getAlbum();
        this.duracion = favorito.getDuracion();
        this.uri = favorito.getUri();
        this.album_id = favorito.getAlbumId();
    }
}