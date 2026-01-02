package com.example.pathfinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class OrganizationLoginActivity extends AppCompatActivity {

    EditText etOrgEmail, etOrgPassword;
    Button btnOrgLogin;
    TextView tvOrgRegister; // Register link

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organization_login);

        // Initialize views
        etOrgEmail = findViewById(R.id.etOrgEmail);
        etOrgPassword = findViewById(R.id.etOrgPassword);
        btnOrgLogin = findViewById(R.id.btnOrgLogin);
        tvOrgRegister = findViewById(R.id.tvOrgRegister);

        // Login button click (dummy for now)
        btnOrgLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { }
        });

        // Register link click
        tvOrgRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open Organization Registration screen
                Intent intent = new Intent(OrganizationLoginActivity.this, OrganizationRegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}
