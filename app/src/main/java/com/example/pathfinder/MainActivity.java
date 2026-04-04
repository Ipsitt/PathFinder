package com.example.pathfinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

// Landing screen for choosing student, organization, or admin login.

public class MainActivity extends AppCompatActivity {

    Button btnOrg, btnStudent;

    // Initializes the landing screen and restores the saved session.
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
                intent = new Intent(this, OrgHomeActivity.class);
            } else {
                intent = new Intent(this, StuHomeActivity.class);
            }
            intent.putExtra("email", email);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Find buttons
        btnOrg = findViewById(R.id.btnOrg);
        btnStudent = findViewById(R.id.btnStudent);

        // Org button click
        btnOrg.setOnClickListener(new View.OnClickListener() {
    // Opens the organization login screen.
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OrgLoginActivity.class);
                startActivity(intent);
            }
        });

        // Student button click (this was missing)
        btnStudent.setOnClickListener(new View.OnClickListener() {
    // Opens the student login screen.
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StuLoginActivity.class);
                startActivity(intent);
            }
        });
    }
}
