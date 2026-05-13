package com.example.music1.api;

        import com.example.music1.models.FavoritoRemoto;
        import java.util.List;
        import retrofit2.Call;
        import retrofit2.http.*;

public interface MusicApi {

    @POST("registro.php")
    Call<RespuestaRegistro> registrar(@Body RegistroRequest datos);

    @POST("login.php")
    Call<RespuestaLogin> login(@Body LoginRequest request);

    @GET("get_favoritos.php")
    Call<List<FavoritoRemoto>> getFavoritos(@Query("usuario_id") int usuarioId);

    @POST("sync_favoritos.php")
    Call<RespuestaSimple> syncFavoritos(@Body SyncFavoritosRequest request);

    @POST("add_favorito.php")
    Call<RespuestaSimple> addFavorito(@Body AddFavoritoRequest request);

    @POST("remove_favorito.php")
    Call<RespuestaSimple> removeFavorito(@Body RemoveFavoritoRequest request);
}