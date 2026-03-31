package com.example.pathfinder;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.ArrayList;
import java.util.List;

public class EditPostActivity extends AppCompatActivity {

    EditText etTitle, etDescription, etStipend, etTimePeriod;
    Spinner spTagDropdown;
    TextView tvSelectedTags;
    Button btnUpdatePost;
    ImageView btnEditBack;
    LinearLayout editPostTopBar;

    DBHelper dbHelper;
    List<DBHelper.Tag> allTags;
    List<DBHelper.Tag> selectedTags = new ArrayList<>();
    
    int postId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
                
        setContentView(R.layout.activity_edit_post);

        dbHelper = new DBHelper(this);

        etTitle = findViewById(R.id.etEditTitle);
        etDescription = findViewById(R.id.etEditDescription);
        etStipend = findViewById(R.id.etEditStipend);
        etTimePeriod = findViewById(R.id.etEditTimePeriod);
        spTagDropdown = findViewById(R.id.spEditTagDropdown);
        tvSelectedTags = findViewById(R.id.tvEditSelectedTags);
        btnUpdatePost = findViewById(R.id.btnUpdatePost);
        btnEditBack = findViewById(R.id.btnEditBack);
        editPostTopBar = findViewById(R.id.editPostTopBar);

        ViewCompat.setOnApplyWindowInsetsListener(editPostTopBar, (v, insets) -> {
            int sb = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), sb + dp(16), v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        btnEditBack.setOnClickListener(v -> finish());

        postId = getIntent().getIntExtra("postId", -1);
        if (postId == -1) {
            Toast.makeText(this, "Error loading post", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etTitle.setText(getIntent().getStringExtra("title"));
        etDescription.setText(getIntent().getStringExtra("description"));
        etStipend.setText(getIntent().getStringExtra("stipend"));
        etTimePeriod.setText(getIntent().getStringExtra("timePeriod"));
        
        loadTagsIntoSpinner();

        List<Integer> existingTagIds = getExistingTagsForPost(postId);
        for(Integer id : existingTagIds) {
            for(DBHelper.Tag t : allTags) {
                if(t.id == id) {
                    selectedTags.add(t);
                    break;
                }
            }
        }
        updateSelectedTagsText();

        spTagDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) return;
                DBHelper.Tag tag = allTags.get(position - 1);
                if (selectedTags.contains(tag)) {
                    Toast.makeText(EditPostActivity.this, "Tag already added", Toast.LENGTH_SHORT).show();
                } else {
                    if (selectedTags.size() >= 5) {
                        selectedTags.remove(0);
                    }
                    selectedTags.add(tag);
                    updateSelectedTagsText();
                }
                spTagDropdown.setSelection(0);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnUpdatePost.setOnClickListener(v -> updatePost());
    }
    
    private List<Integer> getExistingTagsForPost(int id) {
        List<Integer> ids = new ArrayList<>();
        android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
        android.database.Cursor c = db.rawQuery("SELECT tag_id FROM post_tags WHERE post_id=?", new String[]{String.valueOf(id)});
        while(c.moveToNext()){
            ids.add(c.getInt(0));
        }
        c.close();
        return ids;
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

    private void updatePost() {
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

        boolean result = dbHelper.updatePost(postId, title, desc, stipend, period, tagIds);
        if (result) {
            Toast.makeText(this, "Post successfully updated!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to update post.", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
