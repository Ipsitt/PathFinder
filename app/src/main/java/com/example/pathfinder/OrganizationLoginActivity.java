package com.example.pathfinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class OrganizationLoginActivity extends AppCompatActivity {

    // ── Hardcoded admin credentials ───────────────────────────────────────────
    private static final String ADMIN_EMAIL    = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvOrgRegister;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organization_login);

        etEmail      = findViewById(R.id.etOrgEmail);
        etPassword   = findViewById(R.id.etOrgPassword);
        btnLogin     = findViewById(R.id.btnOrgLogin);
        tvOrgRegister = findViewById(R.id.tvOrgRegister);

        dbHelper = new DBHelper(this);

        btnLogin.setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // ── Admin check (hardcoded, bypasses DB) ──────────────────────────
            if (email.equals(ADMIN_EMAIL) && password.equals(ADMIN_PASSWORD)) {
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