package com.example.pathfinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// Login screen for students.

public class StuLoginActivity extends AppCompatActivity {

    EditText etStudentEmail, etStudentPassword;
    Button btnStudentLogin;
    TextView tvStudentRegister;
    DBHelper dbHelper;

    // Initializes the student login screen.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stu_login);

        etStudentEmail = findViewById(R.id.etStudentEmail);
        etStudentPassword = findViewById(R.id.etStudentPassword);
        btnStudentLogin = findViewById(R.id.btnStudentLogin);
        tvStudentRegister = findViewById(R.id.tvStudentRegister);

        dbHelper = new DBHelper(this);

        btnStudentLogin.setOnClickListener(new View.OnClickListener() {
            // Validates the form and signs the student in.
            @Override
            public void onClick(View v) {
                String email = etStudentEmail.getText().toString().trim();
                String password = etStudentPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(StuLoginActivity.this,
                            "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean valid = dbHelper.checkStudentLogin(email, password);

                if (valid) {
                    // Save session
                    getSharedPreferences("PathFinderPrefs", MODE_PRIVATE).edit()
                            .putString("logged_in_email", email)
                            .putString("user_type", "student")
                            .apply();

                    Toast.makeText(StuLoginActivity.this,
                            "Login successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(StuLoginActivity.this, StuHomeActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(StuLoginActivity.this,
                            "Invalid email or password", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvStudentRegister.setOnClickListener(new View.OnClickListener() {
    // Opens the student registration screen.
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StuLoginActivity.this, StuRegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}
