package com.example.locationbaseddiary;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddTask extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "AddTask";
    private static String[] classnames;

    private Toolbar toolbar;
    private EditText taskDesc;
    private Button addTask;
    private Calendar calendar;
    private String currentDateTime;
    private AutoCompleteTextView editText;
    private ArrayAdapter<String> adapter;

    private FirebaseUser user;
    private FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Location Based Diary");

        user = FirebaseAuth.getInstance().getCurrentUser();
        fStore = FirebaseFirestore.getInstance();

        taskDesc = findViewById(R.id.editTextTaskDesc);
        //taskClass = findViewById(R.id.editTextTaskClassname);

        addTask = (Button) findViewById(R.id.addTaskBtn);
        addTask.setOnClickListener(this);

        calendar = Calendar.getInstance();
        SimpleDateFormat time = new SimpleDateFormat("dd/MM/yyyy hh:mm aa");

        currentDateTime = time.format(calendar.getTime());
        //currentDateTime = calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.MONTH)
        //        + "/" + calendar.get(Calendar.YEAR) + " : " + calendar.get(Calendar.HOUR_OF_DAY) + ":" +
        //        calendar.get(Calendar.MINUTE);

        Log.d(TAG, "onCreate: "+currentDateTime);

        classnames = getResources().getStringArray(R.array.classnames);
        editText = findViewById(R.id.classnameTextView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1, classnames);
        editText.setAdapter(adapter);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.addTaskBtn:
                addTask();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_delete, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.menu_delete){
            Toast.makeText(this, "Task deleted.", Toast.LENGTH_SHORT).show();
            finish();
            startActivity(new Intent(AddTask.this, MainActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void addTask(){
        String taskDescription = taskDesc.getText().toString();
        String taskClassname = editText.getText().toString();


        if (taskDescription.isEmpty() || taskDescription.length() < 5) {
            taskDesc.setError("Please enter a valid Task Description, with a minimum of 5 characters");
            taskDesc.requestFocus();
            return;
        }

        if (taskClassname.isEmpty() || !Arrays.stream(classnames).anyMatch(taskClassname::equals)) {
            editText.setError("Select a valid classname for the task");
            editText.requestFocus();
            return;
        }

        DocumentReference docRef = fStore.collection("Tasks").document(user.getUid()).collection("myTasks").document();
        Map<String,Object> task = new HashMap<>();
        task.put("Description", taskDescription);
        task.put("Task_Classname", taskClassname);
        task.put("DateTime_Task_Creation", currentDateTime);

        docRef.set(task).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    Log.d(TAG, "onComplete: Task creation request successful");
                    finish();
                    startActivity(new Intent(AddTask.this, MainActivity.class));
                }
                else {
                    try {
                        throw task.getException();
                    } catch (FirebaseFirestoreException e){
                        e.printStackTrace();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}