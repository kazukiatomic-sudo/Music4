package com.example.music1.models;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.example.music1.R;
import com.example.music1.utils.TagEditorManager;

import java.io.Serializable;
import java.util.Random;

public class Cancion implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String TAG = "Cancion";

    private long id;
    private String titulo;
    private String artista;
    private String album;
    private long duracion;
    private long albumId;
    private int rawResource;
    private int imagenAlbum;
    private String uriString;
    private static final Random random = new Random();

    public Cancion(long id, String titulo, String artista, String album, long duracion, Uri uri, long albumId) {
        this.id = id;
        this.titulo = titulo;
        this.artista = artista;
        this.album = album;
        this.duracion = duracion;
        this.albumId = albumId;
        this.uriString = uri != null ? uri.toString() : "";
        this.rawResource = -1;
        this.imagenAlbum = obtenerImagenAleatoria();
    }

    public Cancion(String titulo, String artista, int rawResource, int imagenAlbum, int duracionSegundos) {
        this.id = System.currentTimeMillis();
        this.titulo = titulo;
        this.artista = artista;
        this.album = "Álbum de prueba";
        this.duracion = duracionSegundos * 1000L;
        this.rawResource = rawResource;
        this.imagenAlbum = imagenAlbum;
        this.uriString = "";
        this.albumId = -1;
    }

    private int obtenerImagenAleatoria() {
        int num = random.nextInt(10) + 1;
        switch(num) {
            case 1: return R.drawable.album1;
            case 2: return R.drawable.album2;
            case 3: return R.drawable.album3;
            case 4: return R.drawable.album4;
            case 5: return R.drawable.album5;
            case 6: return R.drawable.album6;
            case 7: return R.drawable.album7;
            case 8: return R.drawable.album8;
            case 9: return R.drawable.album9;
            case 10: return R.drawable.album10;
            default: return R.drawable.album1;
        }
    }

    public long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getArtista() { return artista; }
    public String getAlbum() { return album; }
    public long getDuracion() { return duracion; }
    public int getRawResource() { return rawResource; }
    public int getImagenAlbum() { return imagenAlbum; }
    public long getAlbumId() { return albumId; }
    public String getUriString() { return uriString; }

    public Uri getUri() {
        if (uriString != null && !uriString.isEmpty()) {
            return Uri.parse(uriString);
        }
        if (id > 0 && rawResource == -1) {
            return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }
        return null;
    }

    public void restaurarUri() {}

    public boolean esCancionDePrueba() {
        return rawResource != -1;
    }

    public String getDuracionFormateada() {
        int segundos = (int) ((duracion / 1000) % 60);
        int minutos = (int) ((duracion / (1000 * 60)) % 60);
        return String.format("%d:%02d", minutos, segundos);
    }

    public String getTituloMostrable(Context context) {
        TagEditorManager manager = TagEditorManager.getInstance(context);
        return manager.getTitulo(this.id, this.uriString, this.titulo);
    }

    public String getArtistaMostrable(Context context) {
        TagEditorManager manager = TagEditorManager.getInstance(context);
        return manager.getArtista(this.id, this.uriString, this.artista);
    }

    public String getAlbumMostrable(Context context) {
        TagEditorManager manager = TagEditorManager.getInstance(context);
        return manager.getAlbum(this.id, this.uriString, this.album);
    }

    public Bitmap getPortadaMostrable(Context context) {
        TagEditorManager manager = TagEditorManager.getInstance(context);
        return manager.getPortada(this.id, this.uriString);
    }
}