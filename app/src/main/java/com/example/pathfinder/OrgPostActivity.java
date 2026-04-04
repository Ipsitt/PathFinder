package com.example.pathfinder;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Internship post creation screen for organizations.

public class OrgPostActivity extends AppCompatActivity {

    EditText etTitle, etDescription, etStipend, etTimePeriod;
    Spinner spTagDropdown;
    TextView tvSelectedTags;
    FlexboxLayout tagChipsContainer;
    Button btnStuPost;
    android.widget.LinearLayout bottomHomeBtn, bottomStuPostBtn,
            bottomInternsBtn, bottomHistoryBtn;

    DBHelper dbHelper;
    List<DBHelper.Tag> allTags;
    List<DBHelper.Tag> selectedTags = new ArrayList<>();
    String userEmail;
    String orgName = "Org Name";
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isSaving = false;

    // Initializes the internship post creation screen.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        new androidx.core.view.WindowInsetsControllerCompat(
                getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_org_post);

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
        btnStuPost           = findViewById(R.id.btnStuPost);
        bottomHomeBtn     = findViewById(R.id.bottomHomeBtn);
        bottomStuPostBtn     = findViewById(R.id.bottomStuPostBtn);
        bottomInternsBtn  = findViewById(R.id.bottomInternsBtn);
        bottomHistoryBtn  = findViewById(R.id.bottomHistoryBtn);
        spTagDropdown.setEnabled(false);
        btnStuPost.setEnabled(false);

        userEmail = getIntent().getStringExtra("email");
        loadInitialData();

        spTagDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            // Adds the selected tag to the post.
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (allTags == null || allTags.isEmpty()) return;
                if (position == 0) return;

                DBHelper.Tag tag = allTags.get(position - 1);

                if (selectedTags.contains(tag)) {
                    Toast.makeText(OrgPostActivity.this,
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

            // Keeps the current selection unchanged.
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnStuPost.setOnClickListener(v -> submitStuPost());
        setupBottomNav();
    }

    // Loads organization details and tags without blocking the UI.
    private void loadInitialData() {
        dbExecutor.execute(() -> {
            String loadedOrgName = orgName;
            if (userEmail != null && !userEmail.isEmpty()) {
                DBHelper.Org org = dbHelper.getOrgByEmail(userEmail);
                if (org != null && org.name != null && !org.name.isEmpty()) {
                    loadedOrgName = org.name;
                }
            }
            List<DBHelper.Tag> tags = dbHelper.getAllTags();

            final String finalOrgName = loadedOrgName;
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                orgName = finalOrgName;
                loadTagsIntoSpinner(tags);
                spTagDropdown.setEnabled(true);
                btnStuPost.setEnabled(true);
            });
        });
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

    // Loads available tags into the dropdown.
    private void loadTagsIntoSpinner(List<DBHelper.Tag> tags) {
        allTags = tags != null ? tags : new ArrayList<>();
        List<String> labels = new ArrayList<>();
        labels.add("Select a tag...");
        for (DBHelper.Tag t : allTags) labels.add(t.label);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spTagDropdown.setAdapter(adapter);
    }

    // Creates a new internship post.
    private void submitStuPost() {
        if (isSaving) return;

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
        isSaving = true;
        btnStuPost.setEnabled(false);

        dbExecutor.execute(() -> {
            long result = dbHelper.insertPost(
                    title, desc, stipend, period, orgName, finalEmail, tagIds);

            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                isSaving = false;
                btnStuPost.setEnabled(true);

                if (result != -1) {
                    Toast.makeText(this, "Post saved successfully!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to save post.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // Sets up the organization bottom navigation.
    private void setupBottomNav() {
        bottomHomeBtn.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(
                    this, OrgHomeActivity.class);
            intent.putExtra("email", userEmail);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
        bottomStuPostBtn.setOnClickListener(v ->
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
                    this, OrgHistoryActivity.class);
            intent.putExtra("email", userEmail);
            startActivity(intent);
        });
    }

    // Shows the organization menu.
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

    // Converts dp units to pixels.
    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // Releases the background executor when the screen closes.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdownNow();
    }
}
