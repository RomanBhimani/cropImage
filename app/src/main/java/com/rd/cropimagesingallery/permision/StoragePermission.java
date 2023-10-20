package com.rd.cropimagesingallery.permision;
import android.app.Activity;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class StoragePermission {
    public static void RequestPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(activity, new String[]{"android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO", "android.permission.READ_MEDIA_AUDIO"}, 100);
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"}, 100);
        }
    }
    public static boolean PermissionGranted(Activity activity) {
        return Build.VERSION.SDK_INT >= 33 ? ContextCompat.checkSelfPermission(activity, "android.permission.READ_MEDIA_IMAGES") == 0 && ContextCompat.checkSelfPermission(activity, "android.permission.READ_MEDIA_AUDIO") == 0 && ContextCompat.checkSelfPermission(activity, "android.permission.READ_MEDIA_VIDEO") == 0 : ContextCompat.checkSelfPermission(activity, "android.permission.READ_EXTERNAL_STORAGE") == 0 && ContextCompat.checkSelfPermission(activity, "android.permission.WRITE_EXTERNAL_STORAGE") == 0;
    }
}