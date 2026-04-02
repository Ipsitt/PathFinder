package com.example.pathfinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnOrganization, btnStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Session check
        android.content.SharedPreferences prefs = getSharedPreferences("PathFinderPrefs", MODE_PRIVATE);
        String email = prefs.getString("logged_in_email", null);
        String type = prefs.getString("user_type", null);

        if (email != null && type != null) {
            Intent intent;
            if (type.equals("admin")) {
                intent = new Intent(this, AdminActivity.class);
            } else if (type.equals("org")) {
                intent = new Intent(this, OrganizationHomeActivity.class);
            } else {
                intent = new Intent(this, StudentHomeActivity.class);
            }
            intent.putExtra("email", email);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Find buttons
        btnOrganization = findViewById(R.id.btnOrganization);
        btnStudent = findViewById(R.id.btnStudent);

        // Organization button click
        btnOrganization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OrganizationLoginActivity.class);
                startActivity(intent);
            }
        });

        // Student button click (this was missing)
        btnStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StudentLoginActivity.class);
                startActivity(intent);
            }
        });
    }
}
