package com.example.music1.models;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Favorito implements Parcelable {
    private long id;
    private String titulo;
    private String artista;
    private String album;
    private long duracion;
    private String uri;
    private long albumId;
    private long fechaAgregado;

    public Favorito(long id, String titulo, String artista, String album, long duracion, String uri, long albumId) {
        this.id = id;
        this.titulo = titulo;
        this.artista = artista;
        this.album = album;
        this.duracion = duracion;
        this.uri = uri;
        this.albumId = albumId;
        this.fechaAgregado = System.currentTimeMillis();
    }

    protected Favorito(Parcel in) {
        id = in.readLong();
        titulo = in.readString();
        artista = in.readString();
        album = in.readString();
        duracion = in.readLong();
        uri = in.readString();
        albumId = in.readLong();
        fechaAgregado = in.readLong();
    }

    public static final Creator<Favorito> CREATOR = new Creator<Favorito>() {
        @Override
        public Favorito createFromParcel(Parcel in) {
            return new Favorito(in);
        }
        @Override
        public Favorito[] newArray(int size) {
            return new Favorito[size];
        }
    };

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(titulo);
        dest.writeString(artista);
        dest.writeString(album);
        dest.writeLong(duracion);
        dest.writeString(uri);
        dest.writeLong(albumId);
        dest.writeLong(fechaAgregado);
    }

    public long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getArtista() { return artista; }
    public String getAlbum() { return album; }
    public long getDuracion() { return duracion; }
    public String getUri() { return uri; }
    public long getAlbumId() { return albumId; }
    public long getFechaAgregado() { return fechaAgregado; }

    public String getDuracionFormateada() {
        int segundos = (int) ((duracion / 1000) % 60);
        int minutos = (int) ((duracion / (1000 * 60)) % 60);
        return String.format("%d:%02d", minutos, segundos);
    }
}