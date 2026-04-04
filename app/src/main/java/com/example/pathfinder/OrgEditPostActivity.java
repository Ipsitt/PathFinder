package com.example.pathfinder;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.List;

// Edit internship post screen for organizations.

public class OrgEditPostActivity extends AppCompatActivity {

    EditText etTitle, etDescription, etStipend, etTimePeriod;
    Spinner spTagDropdown;
    TextView tvSelectedTags;
    FlexboxLayout tagChipsContainer;
    Button btnUpdateStuPost;
    ImageView btnEditBack;
    LinearLayout editStuPostTopBar;

    DBHelper dbHelper;
    List<DBHelper.Tag> allTags;
    List<DBHelper.Tag> selectedTags = new ArrayList<>();
    
    int postId;

    // Initializes the internship post editor.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
                
        setContentView(R.layout.activity_org_edit_post);

        dbHelper = new DBHelper(this);

        etTitle = findViewById(R.id.etEditTitle);
        etDescription = findViewById(R.id.etEditDescription);
        etStipend = findViewById(R.id.etEditStipend);
        etTimePeriod = findViewById(R.id.etEditTimePeriod);
        spTagDropdown = findViewById(R.id.spEditTagDropdown);
        tvSelectedTags = findViewById(R.id.tvEditSelectedTags);
        tagChipsContainer = findViewById(R.id.editTagChipsContainer);
        btnUpdateStuPost = findViewById(R.id.btnUpdateStuPost);
        btnEditBack = findViewById(R.id.btnEditBack);
        editStuPostTopBar = findViewById(R.id.editStuPostTopBar);

        ViewCompat.setOnApplyWindowInsetsListener(editStuPostTopBar, (v, insets) -> {
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

        List<Integer> existingTagIds = getExistingTagsForStuPost(postId);
        for(Integer id : existingTagIds) {
            for(DBHelper.Tag t : allTags) {
                if(t.id == id) {
                    selectedTags.add(t);
                    break;
                }
            }
        }
        renderTagChips();

        spTagDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    // Adds the selected tag to the post.
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) return;
                DBHelper.Tag tag = allTags.get(position - 1);
                if (selectedTags.contains(tag)) {
                    Toast.makeText(OrgEditPostActivity.this, "Tag already added", Toast.LENGTH_SHORT).show();
                } else {
                    if (selectedTags.size() >= 5) {
                        selectedTags.remove(0);
                    }
                    selectedTags.add(tag);
                    renderTagChips();
                }
                spTagDropdown.setSelection(0);
            }
            // Keeps the current tag selection unchanged.
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnUpdateStuPost.setOnClickListener(v -> updatePost());
    }
    
    // Loads the tags already assigned to the post.
    private List<Integer> getExistingTagsForStuPost(int id) {
        List<Integer> ids = new ArrayList<>();
        android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
        android.database.Cursor c = db.rawQuery("SELECT tag_id FROM post_tags WHERE post_id=?", new String[]{String.valueOf(id)});
        while(c.moveToNext()){
            ids.add(c.getInt(0));
        }
        c.close();
        return ids;
    }

    // Loads available tags into the dropdown.
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

    // Shows selected tags as removable chips.
    private void renderTagChips() {
        tagChipsContainer.removeAllViews();

        if (selectedTags.isEmpty()) {
            tvSelectedTags.setVisibility(View.VISIBLE);
            tvSelectedTags.setText("No tags selected");
            return;
        }

        tvSelectedTags.setVisibility(View.GONE);

        for (DBHelper.Tag tag : new ArrayList<>(selectedTags)) {
            LinearLayout chip = new LinearLayout(this);
            FlexboxLayout.LayoutParams chipLp = new FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT);
            chipLp.setMargins(0, 0, dp(8), dp(8));
            chip.setLayoutParams(chipLp);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(android.view.Gravity.CENTER_VERTICAL);
            chip.setPadding(dp(10), dp(6), dp(6), dp(6));

            int tagColor;
            try { tagColor = Color.parseColor(tag.color); }
            catch (Exception e) { tagColor = Color.parseColor("#1E90FF"); }

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setShape(GradientDrawable.RECTANGLE);
            chipBg.setCornerRadius(dp(20));
            chipBg.setColor(tagColor);
            chip.setBackground(chipBg);

            double lum = (0.299 * Color.red(tagColor)
                    + 0.587 * Color.green(tagColor)
                    + 0.114 * Color.blue(tagColor)) / 255;
            int textColor = lum < 0.55 ? Color.WHITE : Color.parseColor("#1E293B");

            TextView tvLabel = new TextView(this);
            tvLabel.setText(tag.label);
            tvLabel.setTextSize(13f);
            tvLabel.setTextColor(textColor);
            chip.addView(tvLabel);

            TextView tvRemove = new TextView(this);
            tvRemove.setText("  x");
            tvRemove.setTextSize(13f);
            tvRemove.setTextColor(textColor);
            tvRemove.setPadding(0, 0, dp(4), 0);
            tvRemove.setClickable(true);
            tvRemove.setFocusable(true);
            tvRemove.setOnClickListener(v -> {
                selectedTags.remove(tag);
                renderTagChips();
            });
            chip.addView(tvRemove);

            tagChipsContainer.addView(chip);
        }
    }

    // Saves the edited internship post.
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

    // Converts dp units to pixels.
    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
