package com.example.locationbaseddiary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    static int FINE_LOCATION_REQUEST_CODE = 988;
    static int COARSE_LOCATION_REQUEST_CODE = 989;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startupLocationRequirementsChecks();
    }

    private void startupLocationRequirementsChecks(){
        if (verifyDeviceLocationEnabled(MainActivity.this)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED){
                startLocationServices(); // We're able to begin location tracking!
            }
            else {
                askLocationPermission();
            }
        }
        else {
            requestLocationFeaturesDialog();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Called. Application is probably running in Background...");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationServices();
    }

    private Boolean verifyDeviceLocationEnabled(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // API version >= 28
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm.isLocationEnabled();
        }
        else {
            // Older API
            int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            return (mode != Settings.Secure.LOCATION_MODE_OFF);
        }
    }

    private void askLocationPermission() {
        // If permission check fails this method is called to request access to required location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d(TAG, "askFineLocationPermission: show alert dialog...");
            }
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Log.d(TAG, "askFineLocationPermission: show alert dialog...");
            }
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, COARSE_LOCATION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == FINE_LOCATION_REQUEST_CODE || requestCode == COARSE_LOCATION_REQUEST_CODE){
            if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted
                startLocationServices();
            } else {
                requestLocationFeaturesDialog();
            }
        }
    }

    public void startLocationServices() {
        if (!isDiaryLocationServiceRunning()){
            Intent intent = new Intent(getApplicationContext(), DiaryLocationServices.class);
            intent.setAction("begin_DiaryLocationServices");
            startService(intent);
            Toast.makeText(this, "Diary Location Services Started", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopLocationServices() {
        if (isDiaryLocationServiceRunning()){
            Intent intent = new Intent(getApplicationContext(), DiaryLocationServices.class);
            intent.setAction("terminate_DiaryLocationServices");
            startService(intent);
            Toast.makeText(this, "Diary Location Services Stopped", Toast.LENGTH_SHORT).show();
        }
    }

    public Boolean isDiaryLocationServiceRunning() {
        ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            for (ActivityManager.RunningServiceInfo service :
                    activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (DiaryLocationServices.class.getName().equals(service.service.getClassName())) {
                    if (service.foreground) {
                       return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    private void requestLocationFeaturesDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setMessage("This application requires permission to use Android's Location features and that they be enabled, in order to find locations near you relevant to your reminders.")
                .setTitle("Location Permissions Required")
                .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        MainActivity.this.startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }
}