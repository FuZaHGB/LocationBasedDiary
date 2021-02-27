package com.example.locationbaseddiary;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    static int FINE_LOCATION_REQUEST_CODE = 988;
    static int COARSE_LOCATION_REQUEST_CODE = 989;

    private Button logout;
    private Toolbar toolbar;
    private RecyclerView taskList;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore fStore;
    private FirestoreRecyclerAdapter<TaskItem, TaskViewHolder> taskAdapter;
    private HashSet<String> taskClassnames;
    private boolean UIReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logout = (Button) findViewById(R.id.btn_Logout);
        logout.setOnClickListener(this);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Location Based Diary");

        mAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        taskClassnames = new HashSet<>();

        Query query = fStore.collection("Tasks").document(user.getUid()).collection("myTasks").orderBy("Description", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<TaskItem> allTasks = new FirestoreRecyclerOptions.Builder<TaskItem>().
                setQuery(query, TaskItem.class)
                .build();

        taskAdapter = new FirestoreRecyclerAdapter<TaskItem, TaskViewHolder>(allTasks) {
            @Override
            protected void onBindViewHolder(@NonNull TaskViewHolder holder, int position, @NonNull TaskItem model) {
                holder.taskDesc.setText(model.getDescription());
                Log.d(TAG, "onBindViewHolder: taskDesc = "+model.getDescription());
                holder.taskClass.setText(model.getTask_Classname());
                holder.taskDateTime.setText(model.getDateTime_Task_Creation());
            }

            @NonNull
            @Override
            public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_view, parent, false);
                Log.d(TAG, "onCreateViewHolder: Created a view for a note");
                return new TaskViewHolder(view);
            }
        };

        taskList = findViewById(R.id.taskList);
        taskList.setLayoutManager(new LinearLayoutManager(this));
        taskList.setAdapter(taskAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.addTask_Btn){
            Toast.makeText(this, "Add button has been pressed", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, AddTask.class);
            stopLocationServices(); // Required so that the DiaryLocationServices classname hashset can be refreshed, otherwise we may be missing entries.
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        taskAdapter.startListening();
        startupLocationRequirementsChecks();
    }

    private void startupLocationRequirementsChecks(){
        if (verifyDeviceLocationEnabled(MainActivity.this)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED){
                forceUpdateClassnames(); // We're able to begin location tracking!
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
        for(String classname : taskClassnames){
            Log.d(TAG, "onBindViewHolder: classname = " + classname);
        }
        taskAdapter.stopListening();
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
        if (requestCode == FINE_LOCATION_REQUEST_CODE || requestCode == COARSE_LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted
                forceUpdateClassnames();
            } else {
                requestLocationFeaturesDialog();
            }
        }
    }

    public void startLocationServices() {
        Log.d(TAG, "startLocationServices: taskclassnames size = " + taskClassnames.size());
        if (!isDiaryLocationServiceRunning()){
            Intent intent = new Intent(getApplicationContext(), DiaryLocationServices.class);
            intent.setAction("begin_DiaryLocationServices");
            intent.putExtra("classnames", taskClassnames);
            startService(intent);
            Toast.makeText(this, "Diary Location Services Started", Toast.LENGTH_SHORT).show();
        }
    }

    public void forceUpdateClassnames() {
        /*
            Method is required due to firestore being asynchronous and therefore not always updating
            classnames hashset before DiaryLocationServices intent is created.
         */
        Log.d(TAG, "forceUpdateClassnames: called");
        fStore.collection("Tasks").document(user.getUid()).collection("myTasks").addSnapshotListener(new EventListener<QuerySnapshot>() {
                                         @Override
                                         public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                             if (value != null) {
                                                 Log.d(TAG, "onEvent: value size = "+value.size());
                                                 for (DocumentSnapshot doc : value.getDocuments()) {
                                                     Log.d(TAG, "onEvent: " + doc.get("Task_Classname"));
                                                     taskClassnames.add(doc.get("Task_Classname").toString());
                                                 }
                                                 startLocationServices();
                                             }
                                             else {
                                                 TextView firestoreError = findViewById(R.id.firestoreError_TextView);
                                                 firestoreError.setVisibility(View.VISIBLE);
                                                 Toast.makeText(MainActivity.this, "Please restart the application!", Toast.LENGTH_SHORT).show();
                                             }
                                         }
                                     }
        );
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_Logout:
                logoutUser();
                break;
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        stopLocationServices();
        startActivity(new Intent(this, LoginActivity.class));
    }

    public class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskDesc, taskClass, taskDateTime;
        View view;
        CardView mCardView;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);

            taskDesc = itemView.findViewById(R.id.textViewTaskDescription);
            taskClass = itemView.findViewById(R.id.textViewTaskClass);
            taskDateTime = itemView.findViewById(R.id.textViewTaskDateTime);

            mCardView = itemView.findViewById(R.id.taskCard);
            view = itemView;
        }
    }
}