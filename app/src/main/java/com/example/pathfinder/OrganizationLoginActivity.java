package com.example.pathfinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class OrganizationLoginActivity extends AppCompatActivity {

    EditText etOrgEmail, etOrgPassword;
    Button btnOrgLogin;
    TextView tvOrgRegister;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organization_login);

        etOrgEmail = findViewById(R.id.etOrgEmail);
        etOrgPassword = findViewById(R.id.etOrgPassword);
        btnOrgLogin = findViewById(R.id.btnOrgLogin);
        tvOrgRegister = findViewById(R.id.tvOrgRegister);

        dbHelper = new DBHelper(this);

        btnOrgLogin.setOnClickListener(v -> {
            String email = etOrgEmail.getText().toString().trim();
            String password = etOrgPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // ── Admin check (using DB) ──────────────────────────
            if (dbHelper.checkAdminLogin(email, password)) {
                Intent intent = new Intent(this, AdminActivity.class);
                startActivity(intent);
                finish();
                return;
            }

            // ── Normal org login ──────────────────────────────────────────────
            if (dbHelper.checkOrgLogin(email, password)) {
                Intent intent = new Intent(this, OrganizationHomeActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });

        tvOrgRegister.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizationRegisterActivity.class)));
    }
}