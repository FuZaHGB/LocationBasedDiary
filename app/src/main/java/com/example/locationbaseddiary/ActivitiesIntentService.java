package com.example.locationbaseddiary;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

public class ActivitiesIntentService extends IntentService {

    public static final String TAG = "ActivitiesIntentService";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    public ActivitiesIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(!ActivityRecognitionResult.hasResult(intent)) {
            // No result detected; permission could be denied.
            return;
        }
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

        ArrayList<DetectedActivity> detectedActivityArrayList = (ArrayList) result.getProbableActivities();

        for (DetectedActivity activity : detectedActivityArrayList) {
            Log.d(TAG, "Detected Activity: " + activity.getType() + " : " + activity.getConfidence());
            broadcastActivity(activity);
        }
    }

    private void broadcastActivity(DetectedActivity activity) {
        Intent intent = new Intent(ActivityRecognitionConstants.DETECTED_ACTIVITY);
        intent.putExtra("type", activity.getType());
        intent.putExtra("confidence", activity.getConfidence());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
