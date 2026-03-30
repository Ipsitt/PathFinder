package com.example.pathfinder;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class PostActivity extends AppCompatActivity {

    EditText etTitle, etDescription, etStipend, etTimePeriod;
    Spinner spTagDropdown;
    TextView tvSelectedTags;
    Button btnPost;

    DBHelper dbHelper;
    List<DBHelper.Tag> allTags;
    List<DBHelper.Tag> selectedTags = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        dbHelper = new DBHelper(this);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etStipend = findViewById(R.id.etStipend);
        etTimePeriod = findViewById(R.id.etTimePeriod);
        spTagDropdown = findViewById(R.id.spTagDropdown);
        tvSelectedTags = findViewById(R.id.tvSelectedTags);
        btnPost = findViewById(R.id.btnPost);

        loadTagsIntoSpinner();

        spTagDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 1. Ignore the "Select a tag..." hint
                if (position == 0) return;

                // 2. Get the selected tag object
                DBHelper.Tag tag = allTags.get(position - 1);

                // 3. Check if it's already in the list
                if (selectedTags.contains(tag)) {
                    Toast.makeText(PostActivity.this, "Tag already added", Toast.LENGTH_SHORT).show();
                } else {
                    // 4. Replacement Logic: If we hit 5, remove the first one (index 0)
                    if (selectedTags.size() >= 5) {
                        selectedTags.remove(0);
                    }

                    // 5. Add the new tag at the end
                    selectedTags.add(tag);
                    updateSelectedTagsText();
                }

                // 6. Reset spinner to hint position
                spTagDropdown.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnPost.setOnClickListener(v -> submitPost());
    }

    private void loadTagsIntoSpinner() {
        allTags = dbHelper.getAllTags();
        List<String> labels = new ArrayList<>();
        labels.add("Select a tag...");

        for (DBHelper.Tag t : allTags) {
            labels.add(t.label);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTagDropdown.setAdapter(adapter);
    }

    private void updateSelectedTagsText() {
        if (selectedTags.isEmpty()) {
            tvSelectedTags.setText("Selected Tags: None");
            return;
        }
        StringBuilder sb = new StringBuilder("Selected Tags: ");
        for (int i = 0; i < selectedTags.size(); i++) {
            sb.append(selectedTags.get(i).label);
            if (i < selectedTags.size() - 1) sb.append(", ");
        }
        tvSelectedTags.setText(sb.toString());
    }

    private void submitPost() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String stipend = etStipend.getText().toString().trim();
        String period = etTimePeriod.getText().toString().trim();

        if (title.isEmpty() || selectedTags.isEmpty()) {
            Toast.makeText(this, "Title and at least one tag required", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Integer> tagIds = new ArrayList<>();
        for (DBHelper.Tag t : selectedTags) tagIds.add(t.id);

        long result = dbHelper.insertPost(title, desc, stipend, period, "Org Name", "org@example.com", tagIds);
        if (result != -1) {
            Toast.makeText(this, "Post saved successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}