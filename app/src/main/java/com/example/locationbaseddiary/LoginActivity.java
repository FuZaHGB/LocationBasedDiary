package com.example.locationbaseddiary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "Register";

    private TextView btn_Register;
    private EditText editTextEmail, editTextPassword;
    private Button btn_Login;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // User is signed in
            // DEBUG Toast.makeText(this, "User is already signed in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        } else {
            // User is signed out
            Log.d(TAG, "onAuthStateChanged:signed_out");
        }

        editTextEmail = (EditText) findViewById(R.id.editTextEmailAddress);
        editTextPassword = (EditText) findViewById(R.id.editTextPassword);

        btn_Register = (TextView) findViewById(R.id.register_textView);
        btn_Register.setOnClickListener(this);

        btn_Login = (Button) findViewById(R.id.btn_Login);
        btn_Login.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_textView:
                // User has pressed the 'Register' textView beneath the login button
                startActivity(new Intent(this, RegisterActivity.class));
                break;

            case R.id.btn_Login:
                // User has pressed the 'Login' button
                loginUser();
                break;
        }
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString();
        String password = editTextPassword.getText().toString();

        // Email verification. Making sure field is not null + is correct email address format.
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please input a valid email address.");
            editTextEmail.requestFocus();
            return;
        }

        if (password.isEmpty() || password.length() < 8) {
            editTextPassword.setError("Please input a valid Password with a minimum of 8 characters.");
            editTextPassword.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (task.isSuccessful()) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                }
                else {
                    Toast.makeText(LoginActivity.this, "Login attempt failed. Please try again!", Toast.LENGTH_SHORT).show();
                    try {
                        throw task.getException();
                    } catch(Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        });
    }
}