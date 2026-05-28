package com.example.music1;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.music1.models.Cancion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Mp3CutterDialog extends Dialog {

    private static final String TAG = "Mp3CutterDialog";
    private Cancion cancion;
    private Context context;
    private SeekBar seekBarInicio, seekBarFin;
    private TextView tvInicio, tvFin, tvTitulo, tvDuracionTotal;
    private Button btnCortar, btnCancelar, btnPrevisualizar;
    private Handler handler = new Handler(Looper.getMainLooper());

    private int duracionTotal;
    private int inicioMs = 0;
    private int finMs = 0;

    public Mp3CutterDialog(@NonNull Context context, Cancion cancion) {
        super(context);
        this.context = context;
        this.cancion = cancion;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_mp3_cutter);

        seekBarInicio = findViewById(R.id.seekBarInicio);
        seekBarFin = findViewById(R.id.seekBarFin);
        tvInicio = findViewById(R.id.tvInicio);
        tvFin = findViewById(R.id.tvFin);
        tvTitulo = findViewById(R.id.tvTitulo);
        tvDuracionTotal = findViewById(R.id.tvDuracionTotal);
        btnCortar = findViewById(R.id.btnCortar);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnPrevisualizar = findViewById(R.id.btnPrevisualizar);

        duracionTotal = (int) cancion.getDuracion();
        finMs = duracionTotal;

        tvTitulo.setText(cancion.getTitulo());
        tvDuracionTotal.setText("Duración total: " + formatTime(duracionTotal));

        seekBarInicio.setMax(duracionTotal);
        seekBarFin.setMax(duracionTotal);
        seekBarFin.setProgress(duracionTotal);

        tvInicio.setText(formatTime(inicioMs));
        tvFin.setText(formatTime(finMs));

        seekBarInicio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    inicioMs = progress;
                    tvInicio.setText(formatTime(inicioMs));
                    if (inicioMs > finMs) {
                        finMs = inicioMs;
                        seekBarFin.setProgress(finMs);
                        tvFin.setText(formatTime(finMs));
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarFin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (progress < inicioMs) {
                        progress = inicioMs;
                        seekBar.setProgress(progress);
                    }
                    finMs = progress;
                    tvFin.setText(formatTime(finMs));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnPrevisualizar.setOnClickListener(v -> previsualizar());
        btnCortar.setOnClickListener(v -> cortarAudio());
        btnCancelar.setOnClickListener(v -> dismiss());
    }

    private String formatTime(int ms) {
        int segundos = (ms / 1000) % 60;
        int minutos = (ms / (1000 * 60)) % 60;
        return String.format(Locale.US, "%02d:%02d", minutos, segundos);
    }

    private void previsualizar() {
        int duracionCorte = finMs - inicioMs;
        if (duracionCorte < 3000) {
            Toast.makeText(context, "El corte debe durar al menos 3 segundos", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(context, "Previsualizando desde " + formatTime(inicioMs) + " hasta " + formatTime(finMs), Toast.LENGTH_LONG).show();
        // Aquí se podría reproducir solo el segmento seleccionado
    }

    private void cortarAudio() {
        int duracionCorte = finMs - inicioMs;
        if (duracionCorte < 3000) {
            Toast.makeText(context, "El corte debe durar al menos 3 segundos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCortar.setEnabled(false);
        btnPrevisualizar.setEnabled(false);
        Toast.makeText(context, "✂ Cortando audio... esto puede tomar unos segundos", Toast.LENGTH_LONG).show();

        new Thread(() -> {
            try {
                boolean exito = realizarCorteMp3();
                new Handler(Looper.getMainLooper()).post(() -> {
                    btnCortar.setEnabled(true);
                    btnPrevisualizar.setEnabled(true);
                    if (exito) {
                        Toast.makeText(context, "✓ Audio cortado guardado en la carpeta Música/Cortes", Toast.LENGTH_LONG).show();
                        dismiss();
                    } else {
                        Toast.makeText(context, "Error al cortar el audio", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error al cortar audio", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    btnCortar.setEnabled(true);
                    btnPrevisualizar.setEnabled(true);
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private boolean realizarCorteMp3() {
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            // Obtener URI real del archivo original
            Uri uriOriginal = cancion.getUri();
            if (uriOriginal == null) {
                Log.e(TAG, "URI de canción es nula");
                return false;
            }

            // Abrir archivo original
            inputStream = new FileInputStream(new File(getRealPathFromUri(uriOriginal)));

            // Calcular bytes aproximados a copiar (MP3 ~128kbps = 16KB por segundo)
            int duracionCorteSegundos = (finMs - inicioMs) / 1000;
            int bytesPorSegundo = 16000; // 128kbps aproximado
            int inicioBytes = (inicioMs / 1000) * bytesPorSegundo;
            int totalBytesACopiar = duracionCorteSegundos * bytesPorSegundo;

            // Saltar al inicio del corte
            long bytesSaltados = inputStream.skip(inicioBytes);
            Log.d(TAG, "Bytes saltados: " + bytesSaltados);

            // Crear archivo de salida en el directorio de música
            File outputDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                outputDir = new File(context.getExternalFilesDir(null), "Cortes");
            } else {
                outputDir = new File(android.os.Environment.getExternalStorageDirectory(), "Music/Cortes");
            }

            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            String nombreArchivo = cancion.getTitulo() + "_cortado_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".mp3";
            File outputFile = new File(outputDir, nombreArchivo);

            outputStream = new FileOutputStream(outputFile);

            // Copiar bytes del segmento seleccionado
            byte[] buffer = new byte[8192];
            int bytesLeidos;
            int bytesCopiados = 0;

            while (bytesCopiados < totalBytesACopiar &&
                    (bytesLeidos = inputStream.read(buffer, 0, Math.min(buffer.length, totalBytesACopiar - bytesCopiados))) != -1) {
                outputStream.write(buffer, 0, bytesLeidos);
                bytesCopiados += bytesLeidos;
            }

            outputStream.flush();

            // Guardar en MediaStore para que aparezca en el reproductor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/Cortes");

                ContentResolver resolver = context.getContentResolver();
                Uri uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (FileOutputStream fos = (FileOutputStream) resolver.openOutputStream(uri)) {
                        FileInputStream fis = new FileInputStream(outputFile);
                        byte[] bufferCopy = new byte[8192];
                        int len;
                        while ((len = fis.read(bufferCopy)) != -1) {
                            fos.write(bufferCopy, 0, len);
                        }
                        fis.close();
                    }
                }
            }

            Log.i(TAG, "Audio cortado exitosamente en: " + outputFile.getAbsolutePath());
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error en realizarCorteMp3", e);
            return false;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error cerrando streams", e);
            }
        }
    }

    private String getRealPathFromUri(Uri uri) {
        String path = null;
        try {
            if (uri.getScheme().equals("content")) {
                String[] projection = {MediaStore.Audio.Media.DATA};
                try (android.database.Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                        path = cursor.getString(columnIndex);
                    }
                }
            } else if (uri.getScheme().equals("file")) {
                path = uri.getPath();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error obteniendo ruta real", e);
        }
        return path;
    }
}