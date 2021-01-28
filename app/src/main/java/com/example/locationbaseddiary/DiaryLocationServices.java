package com.example.locationbaseddiary;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgeo.proj4j.BasicCoordinateTransform;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.ProjCoordinate;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.params.BasicHttpParams;

public class DiaryLocationServices extends Service {

    private static final String TAG = "DiaryLocationServices";
    static final int LOCATION_SERVICE_ID = 777;
    static int DEFAULT_LOCATION_UPDATE_INTERVAL = 15000;
    static int FASTEST_LOCATION_UPDATE_INTERVAL = 10000;

    //private static String BASE_URL = "http://188.166.145.15:3000/edinburghpoi?street_name=eq.Hermiston+Gait";
    private static String BASE_URL = "http://188.166.145.15:3000/rpc/getclosest";


    private double deviceLat = 0.0;
    private double deviceLong = 0.0;

    CRSFactory factory = new CRSFactory();
    CoordinateReferenceSystem srcCrs = factory.createFromName("EPSG:4326");
    CoordinateReferenceSystem dstCrs = factory.createFromName("EPSG:27700");

    BasicCoordinateTransform transform = new BasicCoordinateTransform(srcCrs, dstCrs);


    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null){
                return;
            }
            for (Location location: locationResult.getLocations()){
                Log.d(TAG, "onLocationResult: " + location.toString());
                deviceLat = locationResult.getLastLocation().getLatitude();
                deviceLong = locationResult.getLastLocation().getLongitude();

                try {
                    jsonQueryPostgREST();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //convertWGStoBNG(deviceLat, deviceLong);
        }
    };


    private void jsonQueryPostgREST() throws JSONException, UnsupportedEncodingException {
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("Prefer", "params=single-object");
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json;charset=utf-8");

        JSONObject json = new JSONObject();

        // Hardcoded for Debug testing.
        json.put("northing", "671009.583082142");
        json.put("easting", "321016.697423582");
        json.put("poiclassname", "Bakeries");

        StringEntity se = new StringEntity(json.toString());

        client.post(this, BASE_URL, se, "application/json;charset=utf-8", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers,
                                  JSONArray response) {
                Log.d(TAG, "JSON: " + response.toString());

                try {


                } catch (Exception e) {

                    Log.e(TAG, e.toString());

                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable e,
                                  JSONArray response) {

                // for when the HTTP response is bad (e.g. 400)
                Log.d(TAG, "Request fail! Status code: " + statusCode);
                Log.d(TAG, "Fail response: " + response);
                Log.e(TAG, e.toString());

                Toast.makeText(DiaryLocationServices.this, "Request Failed",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void beginLocationUpdates(){
        String channelId = "location_notification_channel";
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent resultIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(),
                channelId
        );
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("Location Based Diary Service");
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setContentText("Running");
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            if (notificationManager != null
                    && notificationManager.getNotificationChannel(channelId) == null) {
                NotificationChannel notificationChannel = new NotificationChannel(
                        channelId,
                        "Location Service",
                        NotificationManager.IMPORTANCE_LOW
                );
                notificationChannel.setDescription("This channel is used by the Location Based Diary service");
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(DEFAULT_LOCATION_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_LOCATION_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        startForeground(LOCATION_SERVICE_ID, builder.build());
    }

    public void terminateLocationUpdates(){
        Log.d(TAG, "terminateLocationUpdates: Attempting to stop location updates...");
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("begin_DiaryLocationServices")) {
                    beginLocationUpdates();
                } else if (action.equals("terminate_DiaryLocationServices")) {
                    terminateLocationUpdates();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
