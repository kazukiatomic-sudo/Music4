package com.example.music1.utils;

/**
 * TODO: Este Service está pendiente de implementación.
 *
 * Actualmente ReproductorLocalActivity maneja el MediaPlayer directamente,
 * lo que causa que el audio se detenga al salir de la pantalla.
 *
 * Para reproducción en background correcta, este Service debe:
 * - Extender android.app.Service
 * - Gestionar el MediaPlayer y el Equalizer
 * - Publicar una Notification persistente (Android 8+ requiere startForeground)
 * - Exponer un IBinder para que las Activities controlen play/pause/next/prev
 * - Registrar un BroadcastReceiver para controles de auriculares y notificación
 *
 * Mientras no esté implementado, el reproductor funciona pero
 * el audio se corta al minimizar la app.
 */
public class MusicPlayerService {
    // Pendiente de implementación — ver comentario arriba
}
