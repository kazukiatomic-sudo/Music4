package com.example.music1;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.music1.models.Cancion;
import com.example.music1.utils.TagEditorManager;

import java.io.IOException;
import java.io.InputStream;

public class EditTagsDialog extends Dialog {
    private static final String TAG = "EditTagsDialog";

    private Cancion cancion;
    private TagEditorManager manager;
    private OnTagsSavedListener listener;
    private AppCompatActivity activity;

    private ImageView ivPortada;
    private EditText etTitulo;
    private EditText etArtista;
    private EditText etAlbum;
    private Button btnGuardar;
    private Button btnCancelar;
    private Button btnEliminarPortada;
    private Button btnCambiarPortada;

    private Bitmap portadaSeleccionada;
    private boolean portadaCambiada = false;
    private static final int REQUEST_IMAGE_PICK = 1001;

    public interface OnTagsSavedListener {
        void onTagsSaved();
    }

    public EditTagsDialog(@NonNull Context context, Cancion cancion, OnTagsSavedListener listener) {
        super(context);
        this.cancion = cancion;
        this.listener = listener;
        this.manager = TagEditorManager.getInstance(context);

        if (context instanceof AppCompatActivity) {
            this.activity = (AppCompatActivity) context;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_edit_tags);

        initViews();
        cargarDatosActuales();
        setupListeners();
    }

    private void initViews() {
        ivPortada = findViewById(R.id.ivPortada);
        etTitulo = findViewById(R.id.etTitulo);
        etArtista = findViewById(R.id.etArtista);
        etAlbum = findViewById(R.id.etAlbum);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnEliminarPortada = findViewById(R.id.btnEliminarPortada);
        btnCambiarPortada = findViewById(R.id.btnCambiarPortada);
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (activity != null) {
            activity.startActivityForResult(intent, REQUEST_IMAGE_PICK);
        } else {
            Toast.makeText(getContext(), "No se puede abrir la galería", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                cargarImagenDesdeUri(imageUri);
                return true;
            }
        }
        return false;
    }

    private void cargarImagenDesdeUri(Uri imageUri) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {
                int maxSize = 512;
                float ratio = Math.min((float) maxSize / bitmap.getWidth(), (float) maxSize / bitmap.getHeight());
                int newWidth = Math.round(bitmap.getWidth() * ratio);
                int newHeight = Math.round(bitmap.getHeight() * ratio);
                Bitmap resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

                portadaSeleccionada = resized;
                ivPortada.setImageBitmap(resized);
                portadaCambiada = true;
                btnEliminarPortada.setVisibility(View.VISIBLE);

                if (inputStream != null) inputStream.close();
            } else {
                Toast.makeText(getContext(), "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "cargarImagenDesdeUri: Error", e);
            Toast.makeText(getContext(), "Error al cargar imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarDatosActuales() {
        String tituloGuardado = manager.getTitulo(cancion.getId(), cancion.getUriString(), cancion.getTitulo());
        String artistaGuardado = manager.getArtista(cancion.getId(), cancion.getUriString(), cancion.getArtista());
        String albumGuardado = manager.getAlbum(cancion.getId(), cancion.getUriString(), cancion.getAlbum());

        etTitulo.setText(tituloGuardado);
        etArtista.setText(artistaGuardado);
        etAlbum.setText(albumGuardado);

        Bitmap portadaGuardada = manager.getPortada(cancion.getId(), cancion.getUriString());
        if (portadaGuardada != null) {
            ivPortada.setImageBitmap(portadaGuardada);
            btnEliminarPortada.setVisibility(View.VISIBLE);
        } else {
            ivPortada.setImageResource(cancion.getImagenAlbum());
            btnEliminarPortada.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        ivPortada.setOnClickListener(v -> abrirGaleria());

        if (btnCambiarPortada != null) {
            btnCambiarPortada.setOnClickListener(v -> abrirGaleria());
        }

        btnEliminarPortada.setOnClickListener(v -> {
            manager.eliminarPortada(cancion.getId(), cancion.getUriString());
            ivPortada.setImageResource(cancion.getImagenAlbum());
            portadaSeleccionada = null;
            portadaCambiada = false;
            btnEliminarPortada.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Portada eliminada", Toast.LENGTH_SHORT).show();
        });

        btnGuardar.setOnClickListener(v -> guardarCambios());
        btnCancelar.setOnClickListener(v -> dismiss());
    }

    private void guardarCambios() {
        String nuevoTitulo = etTitulo.getText().toString().trim();
        String nuevoArtista = etArtista.getText().toString().trim();
        String nuevoAlbum = etAlbum.getText().toString().trim();

        if (nuevoTitulo.isEmpty()) {
            Toast.makeText(getContext(), "El título no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        manager.guardarTitulo(cancion.getId(), cancion.getUriString(), nuevoTitulo);
        manager.guardarArtista(cancion.getId(), cancion.getUriString(), nuevoArtista);
        manager.guardarAlbum(cancion.getId(), cancion.getUriString(), nuevoAlbum);

        if (portadaCambiada && portadaSeleccionada != null) {
            manager.guardarPortada(cancion.getId(), cancion.getUriString(), portadaSeleccionada);
        }

        Toast.makeText(getContext(), "✓ Guardado: " + nuevoTitulo, Toast.LENGTH_SHORT).show();

        if (listener != null) {
            listener.onTagsSaved();
        }

        dismiss();
    }
}