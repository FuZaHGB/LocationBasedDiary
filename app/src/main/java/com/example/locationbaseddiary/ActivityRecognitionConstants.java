package com.example.locationbaseddiary;

public class ActivityRecognitionConstants {

    public static final String DETECTED_ACTIVITY = "activity_intent";

    // How often ActivityRecognitionService should be checked for an update.
    static final long DETECTION_INTERVAL_IN_MILLISECONDS = 30 * 1000;

    // Minimum threshold confidence level; if less than this ignore.
    public static final int MIN_CONFIDENCE_LEVEL = 75;
}
