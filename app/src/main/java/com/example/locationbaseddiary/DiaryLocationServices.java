package com.example.locationbaseddiary;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class DiaryLocationServices extends Service {

    private static final String TAG = "DiaryLocationServices";
    static final int LOCATION_SERVICE_ID = 777;
    static int DEFAULT_LOCATION_UPDATE_INTERVAL = 30000;
    static int FASTEST_LOCATION_UPDATE_INTERVAL = 25000;
    static int DEVICE_NOTIFICATION_INTERVAL = 180000; // Initially 1 notification every 3 minutes. Changes dependant on method of transportation.
    private long lastNotificationTime;

    //private static String BASE_URL = "http://188.166.145.15:3000/rpc/getclosest";
    private String BASE_URL;

    private static final boolean DEBUG = true; // Setting to false will enable notification intervals depending on detected Method of Transportation.

    private double deviceLat = 0.0;
    private double deviceLong = 0.0;

    private HashMap<String, ArrayList<String>> tasks; // Classname : list of task descriptions
    private HashMap<String, ArrayList<String>> results; // Classname : details of closest relevant place

    BroadcastReceiver receiver; // For receiving Activity Recognition updates.
    String activityType;

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
                    if (!tasks.isEmpty()){
                        // Not making calls to API if we don't have any tasks to complete.
                        jsonQueryPostgREST();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private String buildNotificationText() {
        // Build a line of text for each key in results HashMap.
        StringBuilder sb = new StringBuilder();
        ArrayList<String> resultValue, taskData;
        String name, description;
        int dist;
        for (String task : results.keySet()) {
            resultValue = results.get(task);
            taskData = tasks.get(task);
            name = resultValue.get(0); // 1st index is placename
            dist = (int) Math.ceil(Float.parseFloat(resultValue.get(1))); // 2nd index is distance
            description = taskData.get(0);
            sb.append(name + " : " + dist + "m" + " : " +description +"\n");
        }
        return sb.toString();
    }

    private void displayNotification() {
        //Check to ensure enough time has elapsed between Notifications.
        long currentTime = System.currentTimeMillis();
        long difference = Math.abs(currentTime - lastNotificationTime);


        if (!(difference >= DEVICE_NOTIFICATION_INTERVAL) && (!DEBUG)){
            return;
        }

        // At least 5 minutes have elapsed. Create Notification.

        lastNotificationTime = System.currentTimeMillis(); // Notification being displayed therefore update last notification time.
        String channelId = "LBDtask_notification_channel";
        String notificationText = buildNotificationText();
        String shorthandTitle = notificationText.substring(0, notificationText.indexOf("\n")) + "...";

        Intent resultIntent = new Intent(this, MapPlot.class);
        resultIntent.putExtra("results", results);

        double[] currentLocation = new double[]{deviceLat, deviceLong};
        resultIntent.putExtra("currentLocation", currentLocation);

        // The below PendingIntent needs to change to a new Activity that plots the places on a MAP.
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = (NotificationCompat.Builder) new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.drawable.ic_task_notification)
                .setContentTitle("Relevant location(s) nearby!")
                .setContentText(shorthandTitle)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationText))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

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

        notificationManager.notify(0, builder.build());
    }


    private void jsonQueryPostgREST() throws JSONException, UnsupportedEncodingException {
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("Prefer", "params=single-object");
        client.addHeader("Accept", "application/json");
        client.addHeader("Content-type", "application/json;charset=utf-8");

        JSONObject json = new JSONObject();

        json.put("xcoord", deviceLong);
        json.put("ycoord", deviceLat);

        StringEntity se = new StringEntity(json.toString());

        Log.d(TAG, "jsonQueryPostgREST: Using this url: " + BASE_URL);

        client.post(this, BASE_URL, se, "application/json;charset=utf-8", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers,
                                  JSONArray response) {
                Log.d(TAG, "JSON Size: " +response.length()+" : " + response.toString());

                if (response.length() == 0){
                    return; // No nearby points of interest found.
                }

                results.clear(); // Clear Hashmap as user may have moved so older points aren't necessary.

                try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            String name, classname, distance, xcoord, ycoord;

                            name = obj.get("fetchedname").toString();
                            classname = obj.get("fetchedclassname").toString();
                            distance = obj.get("fetcheddist").toString();
                            xcoord = obj.get("fetchedxcoord").toString();
                            ycoord = obj.get("fetchedycoord").toString();

                            if (!tasks.containsKey(classname)) {
                                // Not a relevant result
                                continue;
                            }

                            if (results == null || !results.containsKey(classname)) {
                                // First time encountering this key, therefore no comparison needed
                                ArrayList<String> value = new ArrayList<>();
                                value.add(name);
                                value.add(distance);
                                value.add(xcoord);
                                value.add(ycoord);
                                results.put(classname, value);
                                //Log.d(TAG, "onSuccess: RESULTS = " + results.get(classname));
                            }
                            else { // Seen this key; need to see if new result is closer
                                ArrayList<String> currentValue = results.get(classname);
                                double oldDistance = Double.parseDouble(currentValue.get(1));
                                double curDistance = Double.parseDouble(distance);
                                if (curDistance < oldDistance) {
                                    ArrayList<String> value = new ArrayList<>();
                                    value.add(name);
                                    value.add(distance);
                                    value.add(xcoord);
                                    value.add(ycoord);
                                    results.put(classname, value); // Replace old result with new, closer one
                                }
                            }
                        }
                        if (results != null){
                            for (String keyname : results.keySet()) {
                                String key = keyname;
                                String value = results.get(keyname).toString();
                                Log.d(TAG, "onSuccess: HASHMAP CONTENTS = Key: "+key+" VALUE: "+value);
                            }
                        }
                    displayNotification();
                } catch (Exception e) {

                    Log.e(TAG, e.toString());

                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable e,
                                  JSONObject response) {

                // for when the HTTP response is bad (e.g. 400)
                Log.d(TAG, "Request fail! Status code: " + statusCode);
                Log.d(TAG, "Fail response: " + response);
                Log.e(TAG, e.toString());

                Toast.makeText(DiaryLocationServices.this, "Request Failed",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    @SuppressLint("MissingPermission")
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
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("begin_DiaryLocationServices")) {

                    // The below section is for the Location Updating Functionality
                    beginLocationUpdates();
                    lastNotificationTime = System.currentTimeMillis() - DEVICE_NOTIFICATION_INTERVAL; // So that we don't need to wait 5 mins before displaying first notification.
                    tasks = (HashMap<String, ArrayList<String>>) intent.getSerializableExtra("tasks");
                    results = new HashMap<>();
                    if (tasks != null){
                        for(String classname : tasks.keySet()){
                            Log.d(TAG, "onStartCommand: classname = " + classname);
                        }
                    }

                    // This will depend on what option is selected in Main Activity.
                    BASE_URL = (String) intent.getSerializableExtra("url");

                    // This section is for the Activity Recognition Functionality.
                    receiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                           if (intent.getAction().equals(ActivityRecognitionConstants.DETECTED_ACTIVITY)) {
                               int type = intent.getIntExtra("type", -1);
                               int confidence = intent.getIntExtra("confidence", 0);
                               handleUserActivity(type, confidence);
                           }
                        }
                    };

                    startActivityRecognitionService();
                } else if (action.equals("terminate_DiaryLocationServices")) {
                    terminateLocationUpdates();
                    stopActivityRecognitionService();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startActivityRecognitionService() {
        // Start Service which provides Activity Recognition functionality.
        Intent intent = new Intent(DiaryLocationServices.this, ActivitiesBackgroundService.class);
        startService(intent);
    }

    private void stopActivityRecognitionService() {
        Intent intent = new Intent(DiaryLocationServices.this, ActivitiesBackgroundService.class);
        stopService(intent);
    }

    public void handleUserActivity(int type, int confidence){
        // Only changing the activity if we're confident enough in the new activity being proposed.
        if (confidence > ActivityRecognitionConstants.MIN_CONFIDENCE_LEVEL) {
            Toast.makeText(this, "Activity Detected! Type = " + type + " : Current Notification Interval = " + DEVICE_NOTIFICATION_INTERVAL, Toast.LENGTH_SHORT).show();

            switch (type) {
                case DetectedActivity.IN_VEHICLE: {
                    activityType = getString(R.string.activity_in_vehicle);
                    DEVICE_NOTIFICATION_INTERVAL = 240000; // New notification every 4 minutes.
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    activityType = getString(R.string.activity_on_bicycle);
                    DEVICE_NOTIFICATION_INTERVAL = 180000; // New notification every 3 minutes.
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    activityType = getString(R.string.activity_on_foot);
                    DEVICE_NOTIFICATION_INTERVAL = 120000; // New notification every 2 minutes.
                    break;
                }
                case DetectedActivity.RUNNING: {
                    activityType = getString(R.string.activity_running);
                    DEVICE_NOTIFICATION_INTERVAL = 240000; // New notification every 4 minutes.
                    break;
                }
                case DetectedActivity.STILL: {
                    activityType = getString(R.string.activity_still);
                    DEVICE_NOTIFICATION_INTERVAL = 300000; // New notification every 5 minutes.
                    break;
                }
                case DetectedActivity.TILTING: {
                    activityType = getString(R.string.activity_tilting);
                    DEVICE_NOTIFICATION_INTERVAL = 240000; // New notification every 4 minutes.
                    break;
                }
                case DetectedActivity.WALKING: {
                    activityType = getString(R.string.activity_walking);
                    DEVICE_NOTIFICATION_INTERVAL = 120000; // New notification every 2 minutes.
                    break;
                }
                case DetectedActivity.UNKNOWN: {
                    activityType = getString(R.string.activity_unknown);
                    DEVICE_NOTIFICATION_INTERVAL = 180000; // New notification every 3 minutes.
                    break;
                }
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
