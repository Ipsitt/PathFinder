package com.example.pathfinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class OrganizationRegisterActivity extends AppCompatActivity {

    Button btnOrgRegister;
    TextView tvOrgBackLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organization_register);

        // Initialize views
        btnOrgRegister = findViewById(R.id.btnOrgRegister);


        // Register button click listener
        btnOrgRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Placeholder for registration logic
                // You can add actual logic here later
            }
        });


    }
}
