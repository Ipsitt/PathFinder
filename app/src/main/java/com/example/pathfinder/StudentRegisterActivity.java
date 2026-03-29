package com.example.pathfinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StudentRegisterActivity extends AppCompatActivity {

    EditText etStudentName, etStudentEmail, etStudentPassword;
    Button btnStudentRegister;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register);

        etStudentName = findViewById(R.id.etStudentName);
        etStudentEmail = findViewById(R.id.etStudentEmail);
        etStudentPassword = findViewById(R.id.etStudentPassword);
        btnStudentRegister = findViewById(R.id.btnStudentRegister);

        dbHelper = new DBHelper(this);

        btnStudentRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etStudentName.getText().toString().trim();
                String email = etStudentEmail.getText().toString().trim();
                String password = etStudentPassword.getText().toString().trim();

                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(StudentRegisterActivity.this,
                            "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dbHelper.studentExists(email)) {
                    Toast.makeText(StudentRegisterActivity.this,
                            "Email already registered", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean success = dbHelper.insertStudent(name, email, password);

                if (success) {
                    Toast.makeText(StudentRegisterActivity.this,
                            "Registration successful! Please log in.", Toast.LENGTH_SHORT).show();
                    // Go back to login
                    Intent intent = new Intent(StudentRegisterActivity.this, StudentLoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(StudentRegisterActivity.this,
                            "Registration failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}