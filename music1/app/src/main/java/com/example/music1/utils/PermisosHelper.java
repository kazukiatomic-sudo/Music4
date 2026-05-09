package com.example.music1.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import android.os.Environment;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermisosHelper {
    private static final String TAG = "PermisosHelper";
    private static final int REQUEST_CODE_WRITE_STORAGE = 200;
    private static final int REQUEST_CODE_MANAGE_STORAGE = 201;

    public static boolean tienePermisoEscritura(Context context) {
        boolean tienePermiso;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tienePermiso = Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tienePermiso = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            tienePermiso = true;
        }
        Log.d(TAG, "tienePermisoEscritura: " + tienePermiso);
        return tienePermiso;
    }

    public static void solicitarPermisoEscritura(Activity activity) {
        Log.d(TAG, "solicitarPermisoEscritura: Solicitando permiso");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", activity.getPackageName())));
                activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
            }
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_STORAGE);
        }
    }

    public static boolean onRequestPermissionsResult(int requestCode, int[] grantResults) {
        if (requestCode == REQUEST_CODE_WRITE_STORAGE) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "onRequestPermissionsResult: WRITE_STORAGE granted=" + granted);
            return granted;
        }
        return false;
    }

    public static boolean tieneAccesoManageStorage(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean tiene = Environment.isExternalStorageManager();
            Log.d(TAG, "tieneAccesoManageStorage: " + tiene);
            return tiene;
        }
        return true;
    }
}