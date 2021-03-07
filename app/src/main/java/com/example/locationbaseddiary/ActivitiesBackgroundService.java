package com.example.locationbaseddiary;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class ActivitiesBackgroundService extends Service {

    /**
     * This functionality was adopted and transformed from the following tutorial:
     * https://www.androidhive.info/2017/12/android-user-activity-recognition-still-walking-running-driving-etc/
     */

    private static final String TAG = "ActivitiesBkgrndService";

    private Intent mActivitiesIntentService;
    private PendingIntent mPendingIntent;
    private ActivityRecognitionClient activityRecognitionClient;

    IBinder mBinder = new ActivitiesBackgroundService.LocalBinder();

    public class LocalBinder extends Binder {
        public ActivitiesBackgroundService getServerInstance() {
            return ActivitiesBackgroundService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        activityRecognitionClient = new ActivityRecognitionClient(this);
        mActivitiesIntentService = new Intent(this, ActivitiesIntentService.class);
        mPendingIntent = PendingIntent.getService(this, 1, mActivitiesIntentService, PendingIntent.FLAG_UPDATE_CURRENT);
        requestActivityUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public void requestActivityUpdates(){
        Task<Void> task = activityRecognitionClient.requestActivityUpdates(
                ActivityRecognitionConstants.DETECTION_INTERVAL_IN_MILLISECONDS,
                mPendingIntent);

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "onSuccess: Successfully requested activity updates");
            }
        });
        
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: Failed to request activity updates.");
            }
        });
    }

    public void removeActivityUpdates(){
        Task<Void> task = activityRecognitionClient.removeActivityUpdates(
                mPendingIntent);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "onSuccess: Successfully removed activity updates");
            }
        });
        
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: Failed to remove activity updates!");
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeActivityUpdates();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
