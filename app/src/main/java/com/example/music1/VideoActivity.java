package com.example.music1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.VideoView;
import android.widget.MediaController;

import androidx.appcompat.app.AppCompatActivity;

public class VideoActivity extends AppCompatActivity {

    private VideoView videoView;
    private Button btnDetener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        videoView = findViewById(R.id.videoView);
        btnDetener = findViewById(R.id.btnDetener);

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video_fondo);
        videoView.setVideoURI(videoUri);

        MediaController controller = new MediaController(this);
        controller.setAnchorView(videoView);
        videoView.setMediaController(controller);
        videoView.start();

        // FIX: navegar directamente a TipoMusicaActivity en vez de MainActivity
        // Antes: MainActivity redirigía de nuevo a TipoMusicaActivity, creando un stack innecesario
        btnDetener.setOnClickListener(v -> {
            if (videoView.isPlaying()) videoView.stopPlayback();
            Intent intent = new Intent(VideoActivity.this, TipoMusicaActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView.isPlaying()) videoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!videoView.isPlaying()) videoView.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoView.stopPlayback();
    }
}
