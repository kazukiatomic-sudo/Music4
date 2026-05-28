package com.example.music1.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.music1.R;
import com.example.music1.ReproductorLocalActivity;

public class MiniPlayerManager {

    public static String tituloActual = "Sin reproducción";
    public static boolean reproduciendo = false;

    public static void setupMiniPlayer(Activity activity) {

        View miniPlayer = activity.findViewById(R.id.miniPlayer);

        if (miniPlayer == null)
            return;

        ImageButton btnPlay = activity.findViewById(R.id.btnMiniPlay);
        ImageButton btnAbrir = activity.findViewById(R.id.btnAbrirPlayer);

        TextView txtTitulo = activity.findViewById(R.id.txtMiniTitulo);
        TextView txtEstado = activity.findViewById(R.id.txtMiniEstado);

        txtTitulo.setText(tituloActual);

        if (reproduciendo) {
            txtEstado.setText("Reproduciendo");
            btnPlay.setImageResource(R.drawable.ic_pause);
        } else {
            txtEstado.setText("Pausado");
            btnPlay.setImageResource(R.drawable.ic_play);
        }

        btnPlay.setOnClickListener(v -> {
            MusicPlayerService service = MusicPlayerService.getInstance();

            if (service != null) {
                service.togglePlayPause();
                reproduciendo = service.isPlayingNow();
            } else {
                reproduciendo = !reproduciendo;
            }

            if (reproduciendo) {
                btnPlay.setImageResource(R.drawable.ic_pause);
                txtEstado.setText("Reproduciendo");
            } else {
                btnPlay.setImageResource(R.drawable.ic_play);
                txtEstado.setText("Pausado");
            }
        });

        btnAbrir.setOnClickListener(v -> {
            Intent intent = new Intent(activity, ReproductorLocalActivity.class);
            activity.startActivity(intent);
        });
    }
}
