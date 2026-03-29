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
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        etStudentEmail = findViewById(R.id.etStudentEmail);
        etStudentPassword = findViewById(R.id.etStudentPassword);
        btnStudentLogin = findViewById(R.id.btnStudentLogin);
        tvStudentRegister = findViewById(R.id.tvStudentRegister);

        dbHelper = new DBHelper(this);

        btnStudentLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etStudentEmail.getText().toString().trim();
                String password = etStudentPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(StudentLoginActivity.this,
                            "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean valid = dbHelper.checkStudentLogin(email, password);

                if (valid) {
                    Toast.makeText(StudentLoginActivity.this,
                            "Login successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(StudentLoginActivity.this, StudentHomeActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(StudentLoginActivity.this,
                            "Invalid email or password", Toast.LENGTH_SHORT).show();
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