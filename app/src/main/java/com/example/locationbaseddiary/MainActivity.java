package com.example.locationbaseddiary;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 987;

    private Boolean advancedFunc = false;
    private String url;


    private Toolbar toolbar;
    private RecyclerView taskList;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore fStore;
    private FirestoreRecyclerAdapter<TaskItem, TaskViewHolder> taskAdapter;
    private HashMap<String, ArrayList<String>> tasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        url = "http://188.166.145.15:3000/rpc/intersectclosest";

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Location Based Diary");

        mAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        tasks = new HashMap<>();

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

                ImageView deleteIcon = holder.view.findViewById(R.id.deleteIcon);
                deleteIcon.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onClick(View v) {
                        final String taskID = taskAdapter.getSnapshots().getSnapshot(holder.getAdapterPosition()).getId(); //.getAdapterPosition keeps track of current position (i.e. after a task is deleted), not just position at creation.
                        PopupMenu menu = new PopupMenu(v.getContext(),v);
                        menu.setGravity(Gravity.END);
                        menu.getMenu().add("Delete").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                DocumentReference docRef = fStore.collection("Tasks").document(user.getUid()).collection("myTasks").document(taskID);
                                docRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(MainActivity.this, "Task deleted successfully!", Toast.LENGTH_SHORT).show();

                                        // Restart location services to update classname hashmap
                                        stopLocationServices();
                                        forceUpdateClassnames(url);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MainActivity.this, "There was an error deleting the task!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                return false;
                            }
                        });
                        menu.show();
                    }
                });
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
        inflater.inflate(R.menu.menu_logout, menu);
        inflater.inflate(R.menu.menu_switchurl, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addTask_Btn:
                //Toast.makeText(this, "Add button has been pressed", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, AddTask.class);
                stopLocationServices(); // Required so that the DiaryLocationServices classname hashset can be refreshed, otherwise we may be missing entries.
                startActivity(intent);
                break;

            case R.id.logout_btn:
                //Toast.makeText(this, "Logout button pressed", Toast.LENGTH_SHORT).show();
                logoutUser();
                break;

            case R.id.switchTask_Btn:
                stopLocationServices();

                if (advancedFunc) {
                    url = "http://188.166.145.15:3000/rpc/intersectclosest";
                    Toast.makeText(this, "Intersect Func", Toast.LENGTH_SHORT).show();
                }
                else {
                    url = "http://188.166.145.15:3000/rpc/getclosest";
                    Toast.makeText(this, "Basic Func", Toast.LENGTH_SHORT).show();
                }

                advancedFunc = !advancedFunc;

                forceUpdateClassnames(url);
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
            permissionsCheck();
        }
        else {
            requestLocationFeaturesDialog();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Called. Application is probably running in Background...");
        for(String classname : tasks.keySet()){
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 3rd Party library to make life easier when dealing with Android Permissions.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(PERMISSIONS_REQUEST_CODE)
    public void permissionsCheck() {
        //Toast.makeText(this, "Permissions Check called", Toast.LENGTH_SHORT).show();
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.FOREGROUND_SERVICE, Manifest.permission.ACTIVITY_RECOGNITION};
        if (EasyPermissions.hasPermissions(this, perms)){
            //Toast.makeText(this, "Permissions have been granted!", Toast.LENGTH_SHORT).show();
            forceUpdateClassnames(url);
        }
        else{
            EasyPermissions.requestPermissions(this, "The following permissions are required for the application to function.",
                    PERMISSIONS_REQUEST_CODE, perms);
        }
    }

    public void startLocationServices(String url) {
        //Log.d(TAG, "startLocationServices: taskclassnames size = " + taskClassnames.size());
        Log.d(TAG, "startLocationServices: taskclassnames size = " + tasks.size());
        if (!isDiaryLocationServiceRunning()){
            Intent intent = new Intent(getApplicationContext(), DiaryLocationServices.class);
            intent.setAction("begin_DiaryLocationServices");
            intent.putExtra("tasks", tasks);
            intent.putExtra("url", url);
            startService(intent);
            //Toast.makeText(this, "Diary Location Services Started", Toast.LENGTH_SHORT).show();
        }
    }

    public void forceUpdateClassnames(String url) {
        /**
          *  Method is required due to firestore being asynchronous and therefore not always updating
          *  classnames hashset before DiaryLocationServices intent is created.
         */
        Log.d(TAG, "forceUpdateClassnames: called");
        fStore.collection("Tasks").document(user.getUid()).collection("myTasks").addSnapshotListener(new EventListener<QuerySnapshot>() {
                                         @Override
                                         public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                             if (value != null) {
                                                 Log.d(TAG, "onEvent: value size = "+value.size());
                                                 for (DocumentSnapshot doc : value.getDocuments()) {
                                                     Log.d(TAG, "onEvent: " + doc.get("Task_Classname"));
                                                     //taskClassnames.add(doc.get("Task_Classname").toString());
                                                     String classname = doc.get("Task_Classname").toString();
                                                     String taskDesc = doc.get("Description").toString();
                                                     ArrayList<String> temp;
                                                     if (tasks.containsKey(classname)) { // Need to append to Value arraylist instead of direct insert
                                                         temp = tasks.get(classname);
                                                         temp.add(taskDesc); // Append description to task ArrayList.
                                                     }
                                                     else{ // First time seeing this key so can just insert straight into tasks
                                                         temp = new ArrayList<String>();
                                                         temp.add(taskDesc);
                                                     }
                                                     tasks.put(classname, temp);
                                                 }
                                                 startLocationServices(url); // By default, use the intersect function.
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
        return; // Now redundant with logout button in Menu.
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