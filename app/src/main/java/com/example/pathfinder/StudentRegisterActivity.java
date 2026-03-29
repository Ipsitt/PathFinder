package com.example.pathfinder;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StudentRegisterActivity extends AppCompatActivity {

    EditText etStudentEmail, etStudentPassword;
    Button btnStudentRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register);

        etStudentEmail = findViewById(R.id.etStudentEmail);
        etStudentPassword = findViewById(R.id.etStudentPassword);
        btnStudentRegister = findViewById(R.id.btnStudentRegister);

        btnStudentRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = etStudentEmail.getText().toString().trim();
                String password = etStudentPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(StudentRegisterActivity.this,
                            "Please fill all fields",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                DB db = new DB(StudentRegisterActivity.this);

                // 🔍 Check duplicate email
                if (db.emailExists(email)) {
                    Toast.makeText(StudentRegisterActivity.this,
                            "Email already registered",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean success = db.insertUser(email, password);

                if (success) {
                    Toast.makeText(StudentRegisterActivity.this,
                            "Registration successful",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(StudentRegisterActivity.this,
                            "Registration failed",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
