package com.example.pathfinder;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class PostActivity extends AppCompatActivity {

    EditText etTitle, etDescription, etStipend, etTimePeriod;
    Spinner spTags;
    Button btnPost;

    DBHelper dbHelper;
    List<DBHelper.Tag> allTags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etStipend = findViewById(R.id.etStipend);
        etTimePeriod = findViewById(R.id.etTimePeriod);
        spTags = findViewById(R.id.spTags);
        btnPost = findViewById(R.id.btnPost);

        dbHelper = new DBHelper(this);

        loadTags();

        btnPost.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            String stipend = etStipend.getText().toString().trim();
            String period = etTimePeriod.getText().toString().trim();
            DBHelper.Tag tag = allTags.get(spTags.getSelectedItemPosition());

            if (title.isEmpty() || desc.isEmpty() || stipend.isEmpty() || period.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Here you can insert into a "posts" table, or just show a Toast for demo
            Toast.makeText(this,
                    "Post created:\n" + title + " [" + tag.label + "]",
                    Toast.LENGTH_SHORT).show();

            finish(); // go back after posting
        });
    }

    private void loadTags() {
        allTags = dbHelper.getAllTags();
        String[] tagLabels = new String[allTags.size()];
        for (int i = 0; i < allTags.size(); i++) tagLabels[i] = allTags.get(i).label;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, tagLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTags.setAdapter(adapter);
    }
}