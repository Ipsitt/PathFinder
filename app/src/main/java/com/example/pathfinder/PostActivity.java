package com.example.pathfinder;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.List;

public class PostActivity extends AppCompatActivity {

    EditText etTitle, etDescription, etStipend, etTimePeriod;
    Spinner spTagDropdown;
    TextView tvSelectedTags;
    FlexboxLayout tagChipsContainer;
    Button btnPost;
    android.widget.LinearLayout bottomHomeBtn, bottomPostBtn,
            bottomInternsBtn, bottomHistoryBtn;

    DBHelper dbHelper;
    List<DBHelper.Tag> allTags;
    List<DBHelper.Tag> selectedTags = new ArrayList<>();
    String userEmail;
    String orgName = "Org Name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        new androidx.core.view.WindowInsetsControllerCompat(
                getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_post);

        View topBar = findViewById(R.id.postTopBar);
        // Status bar spacer height
        View statusBarSpacer = findViewById(R.id.statusBarSpacer);
        if (statusBarSpacer != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(statusBarSpacer, (v, insets) -> {
                androidx.core.graphics.Insets bars = insets.getInsets(
                        androidx.core.view.WindowInsetsCompat.Type.systemBars() |
                        androidx.core.view.WindowInsetsCompat.Type.displayCutout());
                v.getLayoutParams().height = bars.top;
                v.requestLayout();
                return insets;
            });
        }

        findViewById(R.id.btnMenu).setOnClickListener(v -> showPopupMenu(v));

        dbHelper = new DBHelper(this);

        etTitle           = findViewById(R.id.etTitle);
        etDescription     = findViewById(R.id.etDescription);
        etStipend         = findViewById(R.id.etStipend);
        etTimePeriod      = findViewById(R.id.etTimePeriod);
        spTagDropdown     = findViewById(R.id.spTagDropdown);
        tvSelectedTags    = findViewById(R.id.tvSelectedTags);
        tagChipsContainer = findViewById(R.id.tagChipsContainer);
        btnPost           = findViewById(R.id.btnPost);
        bottomHomeBtn     = findViewById(R.id.bottomHomeBtn);
        bottomPostBtn     = findViewById(R.id.bottomPostBtn);
        bottomInternsBtn  = findViewById(R.id.bottomInternsBtn);
        bottomHistoryBtn  = findViewById(R.id.bottomHistoryBtn);

        userEmail = getIntent().getStringExtra("email");
        if (userEmail != null && !userEmail.isEmpty()) {
            DBHelper.Org org = dbHelper.getOrgByEmail(userEmail);
            if (org != null && org.name != null && !org.name.isEmpty()) {
                orgName = org.name;
            }
        }

        loadTagsIntoSpinner();

        spTagDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (position == 0) return;

                DBHelper.Tag tag = allTags.get(position - 1);

                if (selectedTags.contains(tag)) {
                    Toast.makeText(PostActivity.this,
                            "Tag already added", Toast.LENGTH_SHORT).show();
                } else {
                    if (selectedTags.size() >= 5) {
                        selectedTags.remove(0);
                    }
                    selectedTags.add(tag);
                    renderTagChips();
                }

                spTagDropdown.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnPost.setOnClickListener(v -> submitPost());
        setupBottomNav();
    }

    // ── Render colored removable chips ────────────────────────────────────
    private void renderTagChips() {
        tagChipsContainer.removeAllViews();

        if (selectedTags.isEmpty()) {
            tvSelectedTags.setVisibility(View.VISIBLE);
            tvSelectedTags.setText("No tags selected");
            return;
        }

        tvSelectedTags.setVisibility(View.GONE);

        for (DBHelper.Tag tag : new ArrayList<>(selectedTags)) {
            // Outer chip layout: colored background + label + ✕ button
            android.widget.LinearLayout chip =
                    new android.widget.LinearLayout(this);
            FlexboxLayout.LayoutParams chipLp = new FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT);
            chipLp.setMargins(0, 0, dp(8), dp(8));
            chip.setLayoutParams(chipLp);
            chip.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            chip.setGravity(android.view.Gravity.CENTER_VERTICAL);
            chip.setPadding(dp(10), dp(6), dp(6), dp(6));

            // Parse tag color from DB
            int tagColor;
            try { tagColor = Color.parseColor(tag.color); }
            catch (Exception e) { tagColor = Color.parseColor("#1E90FF"); }

            // Chip background
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setShape(GradientDrawable.RECTANGLE);
            chipBg.setCornerRadius(dp(20));
            chipBg.setColor(tagColor);
            chip.setBackground(chipBg);

            // Determine text color based on background luminance
            double lum = (0.299 * Color.red(tagColor)
                    + 0.587 * Color.green(tagColor)
                    + 0.114 * Color.blue(tagColor)) / 255;
            int textColor = lum < 0.55 ? Color.WHITE : Color.parseColor("#1E293B");

            // Tag label
            TextView tvLabel = new TextView(this);
            tvLabel.setText(tag.label);
            tvLabel.setTextSize(13f);
            tvLabel.setTextColor(textColor);
            chip.addView(tvLabel);

            // ✕ remove button
            TextView tvRemove = new TextView(this);
            tvRemove.setText("  ✕");
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

    private void loadTagsIntoSpinner() {
        allTags = dbHelper.getAllTags();
        List<String> labels = new ArrayList<>();
        labels.add("Select a tag...");
        for (DBHelper.Tag t : allTags) labels.add(t.label);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spTagDropdown.setAdapter(adapter);
    }

    private void submitPost() {
        String title   = etTitle.getText().toString().trim();
        String desc    = etDescription.getText().toString().trim();
        String stipend = etStipend.getText().toString().trim();
        String period  = etTimePeriod.getText().toString().trim();

        if (title.isEmpty() || selectedTags.isEmpty()) {
            Toast.makeText(this,
                    "Title and at least one tag required",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        List<Integer> tagIds = new ArrayList<>();
        for (DBHelper.Tag t : selectedTags) tagIds.add(t.id);

        String finalEmail = (userEmail != null && !userEmail.isEmpty())
                ? userEmail : "org@example.com";
        long result = dbHelper.insertPost(
                title, desc, stipend, period, orgName, finalEmail, tagIds);
        if (result != -1) {
            Toast.makeText(this, "Post saved successfully!",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupBottomNav() {
        bottomHomeBtn.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(
                    this, OrganizationHomeActivity.class);
            intent.putExtra("email", userEmail);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
        bottomPostBtn.setOnClickListener(v ->
                Toast.makeText(this, "You are already here",
                        Toast.LENGTH_SHORT).show());
        bottomInternsBtn.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(
                    this, OrgRequestsActivity.class);
            intent.putExtra("email", userEmail);
            startActivity(intent);
        });
        bottomHistoryBtn.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(
                    this, OrganizationHistoryActivity.class);
            intent.putExtra("email", userEmail);
            startActivity(intent);
        });
    }

    private void showPopupMenu(View view) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, view);
        popup.getMenu().add("Logout");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Logout")) {
                getSharedPreferences("PathFinderPrefs", MODE_PRIVATE).edit().clear().apply();
                android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}