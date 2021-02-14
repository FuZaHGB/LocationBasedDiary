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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
//import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth mAuth;
    private static final String TAG = "Register";

    private TextView btn_Register;
    private EditText editTextEmail, editTextPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        editTextEmail = (EditText) findViewById(R.id.editTextEmailAddress_RegisterActivity);
        editTextPassword = (EditText) findViewById(R.id.editTextPassword_RegisterActivity);

        btn_Register = (Button) findViewById(R.id.btn_Register);
        btn_Register.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_Register:
                registerUser();
                break;
        }
    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
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


        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if(task.isSuccessful()) {
                            //updateFirebaseUser();
                            Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                            finish();
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        }
                        else {
                            //Toast.makeText(Register.this, "An error has occurred during registration. Please try again.", Toast.LENGTH_SHORT).show();
                            try {
                                throw task.getException();
                            } catch(FirebaseAuthWeakPasswordException e) {
                                editTextPassword.setError("Password is too weak!");
                                editTextPassword.requestFocus();
                            } catch(FirebaseAuthInvalidCredentialsException e) {
                                editTextEmail.setError("Email address is invalid, please enter a valid email address.");
                                editTextEmail.requestFocus();
                            } catch(FirebaseAuthUserCollisionException e) {
                                editTextEmail.setError("A user with this email address is already registered.");
                                editTextEmail.requestFocus();
                            } catch(Exception e) {
                                Log.e(TAG, e.getMessage());
                            }
                        }
                    }
                });
    }

    /*private void updateFirebaseUser() {
        FirebaseDatabase.getInstance().getReference("Users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(editTextEmail.getText()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if (task.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(RegisterActivity.this, "An error has occurred during registration. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    } */
}