package com.example.pathfinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StudentLoginActivity extends AppCompatActivity {

    EditText etStudentEmail, etStudentPassword;
    Button btnStudentLogin;
    TextView tvStudentRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        etStudentEmail = findViewById(R.id.etStudentEmail);
        etStudentPassword = findViewById(R.id.etStudentPassword);
        btnStudentLogin = findViewById(R.id.btnStudentLogin);
        tvStudentRegister = findViewById(R.id.tvStudentRegister);

        btnStudentLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = etStudentEmail.getText().toString().trim();
                String password = etStudentPassword.getText().toString().trim();

                if (email.equals("empty") && password.equals("empty")) {
                    Intent intent = new Intent(StudentLoginActivity.this, StudentHomeActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(StudentLoginActivity.this,
                            "Invalid email or password",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvStudentRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StudentLoginActivity.this, StudentRegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}
