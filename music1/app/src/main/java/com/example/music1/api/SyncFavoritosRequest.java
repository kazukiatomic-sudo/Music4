package com.example.music1.api;

import com.example.music1.models.FavoritoRemoto;
import java.util.List;

public class SyncFavoritosRequest {
    public int usuario_id;
    public List<FavoritoRemoto> favoritos;
}