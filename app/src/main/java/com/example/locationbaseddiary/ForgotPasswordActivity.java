package com.example.locationbaseddiary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ForgotPasswordActivity";

    private EditText editTextEmail;
    private Button resetPassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        editTextEmail = (EditText) findViewById(R.id.editTextEmailAddress_ForgotPass);

        resetPassword = (Button) findViewById(R.id.btn_ResetPass);
        resetPassword.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_ResetPass:
                resetPass();
                break;
        }
    }

    private void resetPass() {
        String email = editTextEmail.getText().toString().trim();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please input a valid email address.");
            editTextEmail.requestFocus();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()) {
                            Log.d(TAG, "Reset pass email sent");
                            Toast.makeText(ForgotPasswordActivity.this, "A password reset token has been sent to the email supplied.", Toast.LENGTH_SHORT);
                        }
                        else {
                            Toast.makeText(ForgotPasswordActivity.this, "Email is not registered.", Toast.LENGTH_SHORT);
                        }
                    }
                });
    }
}