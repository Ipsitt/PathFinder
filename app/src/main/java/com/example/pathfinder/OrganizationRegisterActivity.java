package com.example.pathfinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class OrganizationRegisterActivity extends AppCompatActivity {

    EditText etOrgName, etOrgEmail, etOrgPassword;
    Button btnOrgRegister;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organization_register);

        etOrgName = findViewById(R.id.etOrgName);
        etOrgEmail = findViewById(R.id.etOrgEmail);
        etOrgPassword = findViewById(R.id.etOrgPassword);
        btnOrgRegister = findViewById(R.id.btnOrgRegister);

        dbHelper = new DBHelper(this);

        btnOrgRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etOrgName.getText().toString().trim();
                String email = etOrgEmail.getText().toString().trim();
                String password = etOrgPassword.getText().toString().trim();

                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(OrganizationRegisterActivity.this,
                            "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dbHelper.orgExists(email)) {
                    Toast.makeText(OrganizationRegisterActivity.this,
                            "Email already registered", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean success = dbHelper.insertOrg(name, email, password);

                if (success) {
                    Toast.makeText(OrganizationRegisterActivity.this,
                            "Registration successful! Welcome.", Toast.LENGTH_SHORT).show();
                    // Go directly to home
                    Intent intent = new Intent(OrganizationRegisterActivity.this, OrganizationHomeActivity.class);
                    intent.putExtra("email", email);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(OrganizationRegisterActivity.this,
                            "Registration failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}