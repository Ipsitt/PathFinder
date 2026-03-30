package com.example.pathfinder;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    DBHelper dbHelper;

    LinearLayout navTags, navOrgs, navUsers;
    FrameLayout contentFrame;
    int activeTab = 0;

    // Tags screen state
    String selectedTagColor = "#4CAF50";
    View colorPreviewBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DBHelper(this);

        // Root
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#F5F5F5"));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Title bar
        TextView titleBar = new TextView(this);
        titleBar.setText("Admin Panel");
        titleBar.setTextSize(18f);
        titleBar.setTextColor(Color.WHITE);
        titleBar.setTypeface(null, Typeface.BOLD);
        titleBar.setBackgroundColor(Color.parseColor("#1E3A8A"));
        titleBar.setGravity(Gravity.CENTER);
        titleBar.setPadding(dp(16), dp(14), dp(16), dp(14));
        root.addView(titleBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Content area
        contentFrame = new FrameLayout(this);
        root.addView(contentFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Bottom nav
        root.addView(buildBottomNav(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        setContentView(root);
        showTab(0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tab switching
    // ─────────────────────────────────────────────────────────────────────────

    void showTab(int tab) {
        activeTab = tab;
        contentFrame.removeAllViews();
        updateNavHighlight();
        if (tab == 0) contentFrame.addView(buildTagsScreen());
        else if (tab == 1) contentFrame.addView(buildOrgsScreen());
        else contentFrame.addView(buildUsersScreen());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAGS SCREEN
    // ─────────────────────────────────────────────────────────────────────────

    View buildTagsScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(80));

        // ── Tags dropdown ─────────────────────────────────────────────────────
        layout.addView(boldLabel("Tags:"));
        addGap(layout, 8);

        Spinner spinner = new Spinner(this);
        refreshTagSpinner(spinner);
        layout.addView(spinner, params(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        addGap(layout, 20);

        // ── Label input ───────────────────────────────────────────────────────
        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView labelTv = boldLabel("Label:");
        labelTv.setMinWidth(dp(72));
        EditText etLabel = new EditText(this);
        etLabel.setHint("Enter tag label");
        etLabel.setBackgroundResource(android.R.drawable.edit_text);
        etLabel.setPadding(dp(8), dp(6), dp(8), dp(6));
        labelRow.addView(labelTv);
        labelRow.addView(etLabel, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        layout.addView(labelRow, params(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // Pre-fill label + color when spinner selection changes
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                List<DBHelper.Tag> tags = dbHelper.getAllTags();
                if (!tags.isEmpty() && pos < tags.size()) {
                    DBHelper.Tag t = tags.get(pos);
                    etLabel.setText(t.label);
                    selectedTagColor = t.color;
                    if (colorPreviewBox != null)
                        setRoundedColor(colorPreviewBox, Color.parseColor(selectedTagColor));
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        addGap(layout, 20);

        // ── Color section ─────────────────────────────────────────────────────
        layout.addView(boldLabel("Color:"));
        addGap(layout, 8);

        // Preview box
        colorPreviewBox = new View(this);
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        previewLp.bottomMargin = dp(12);
        setRoundedColor(colorPreviewBox, Color.parseColor(selectedTagColor));
        layout.addView(colorPreviewBox, previewLp);

        // Color swatches grid (5 columns × 4 rows = 20 colours)
        int[] swatchColors = {
                0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB,
                0xFF1E88E5, 0xFF039BE5, 0xFF00ACC1, 0xFF00897B, 0xFF43A047,
                0xFF7CB342, 0xFFC0CA33, 0xFFFDD835, 0xFFFFB300, 0xFFFB8C00,
                0xFFF4511E, 0xFF6D4C41, 0xFF546E7A, 0xFF757575, 0xFF000000
        };
        int cols = 5;
        for (int r = 0; r < swatchColors.length / cols; r++) {
            LinearLayout rowL = new LinearLayout(this);
            rowL.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (idx >= swatchColors.length) break;
                final int col = swatchColors[idx];
                View sw = new View(this);
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, dp(46), 1f);
                sp.setMargins(dp(3), dp(3), dp(3), dp(3));
                setRoundedColor(sw, col);
                sw.setOnClickListener(v -> {
                    selectedTagColor = String.format("#%06X", (0xFFFFFF & col));
                    setRoundedColor(colorPreviewBox, col);
                });
                rowL.addView(sw, sp);
            }
            layout.addView(rowL, params(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        addGap(layout, 24);

        // ── Save button ───────────────────────────────────────────────────────
        Button btnSave = styledButton("Save Tag", "#1E3A8A");
        btnSave.setOnClickListener(v -> {
            String label = etLabel.getText().toString().trim();
            if (label.isEmpty()) {
                Toast.makeText(this, "Enter a label", Toast.LENGTH_SHORT).show();
                return;
            }
            if (dbHelper.saveTag(label, selectedTagColor)) {
                Toast.makeText(this, "Tag saved!", Toast.LENGTH_SHORT).show();
                etLabel.setText("");
                refreshTagSpinner(spinner);
            } else {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(btnSave, params(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        scroll.addView(layout);
        return scroll;
    }

    void refreshTagSpinner(Spinner spinner) {
        List<DBHelper.Tag> tags = dbHelper.getAllTags();
        List<String> labels = new ArrayList<>();
        if (tags.isEmpty()) labels.add("(no tags yet)");
        else for (DBHelper.Tag t : tags) labels.add(t.label);
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, labels);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(a);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORGANIZATIONS SCREEN
    // ─────────────────────────────────────────────────────────────────────────

    View buildOrgsScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(12), dp(12), dp(12), dp(80));

        TextView title = boldLabel("Organizations");
        title.setTextSize(18f);
        layout.addView(title);
        addGap(layout, 12);

        List<String> emails = dbHelper.getAllOrgEmails();
        if (emails.isEmpty()) {
            layout.addView(emptyMessage("No organizations registered yet."));
        } else {
            layout.addView(tableHeader());
            for (String email : emails) layout.addView(buildOrgRow(email));
        }

        scroll.addView(layout);
        return scroll;
    }

    View buildOrgRow(String email) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(Color.WHITE);
        row.setPadding(dp(6), dp(10), dp(6), dp(10));

        // Thin bottom border
        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.WHITE);
        border.setStroke(dp(1), Color.parseColor("#E0E0E0"));
        row.setBackground(border);

        TextView tvEmail = new TextView(this);
        tvEmail.setText(email);
        tvEmail.setTextSize(12.5f);
        tvEmail.setTextColor(Color.parseColor("#222222"));
        tvEmail.setPadding(dp(4), 0, dp(4), 0);
        row.addView(tvEmail, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 2f));

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bp.setMargins(dp(4), 0, dp(4), 0);

        Button btnLogin = smallButton("Login", "#1E88E5");
        btnLogin.setOnClickListener(v -> {
            Intent i = new Intent(this, OrganizationHomeActivity.class);
            i.putExtra("email", email);
            startActivity(i);
        });
        row.addView(btnLogin, bp);

        Button btnDel = smallButton("Delete", "#E53935");
        btnDel.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Delete Organization")
                        .setMessage("Are you sure you want to delete:\n" + email)
                        .setPositiveButton("Delete", (d, w) -> {
                            dbHelper.deleteOrg(email);
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                            showTab(1);
                        })
                        .setNegativeButton("Cancel", null)
                        .show());
        row.addView(btnDel, bp);

        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // USERS SCREEN
    // ─────────────────────────────────────────────────────────────────────────

    View buildUsersScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(12), dp(12), dp(12), dp(80));

        TextView title = boldLabel("Users (Students)");
        title.setTextSize(18f);
        layout.addView(title);
        addGap(layout, 12);

        List<String> emails = dbHelper.getAllStudentEmails();
        if (emails.isEmpty()) {
            layout.addView(emptyMessage("No students registered yet."));
        } else {
            layout.addView(tableHeader());
            for (String email : emails) layout.addView(buildUserRow(email));
        }

        scroll.addView(layout);
        return scroll;
    }

    View buildUserRow(String email) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(Color.WHITE);
        row.setPadding(dp(6), dp(10), dp(6), dp(10));

        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.WHITE);
        border.setStroke(dp(1), Color.parseColor("#E0E0E0"));
        row.setBackground(border);

        TextView tvEmail = new TextView(this);
        tvEmail.setText(email);
        tvEmail.setTextSize(12.5f);
        tvEmail.setTextColor(Color.parseColor("#222222"));
        tvEmail.setPadding(dp(4), 0, dp(4), 0);
        row.addView(tvEmail, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 2f));

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bp.setMargins(dp(4), 0, dp(4), 0);

        Button btnLogin = smallButton("Login", "#1E88E5");
        btnLogin.setOnClickListener(v -> {
            Intent i = new Intent(this, StudentHomeActivity.class);
            i.putExtra("email", email);
            startActivity(i);
        });
        row.addView(btnLogin, bp);

        Button btnDel = smallButton("Delete", "#E53935");
        btnDel.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Delete User")
                        .setMessage("Are you sure you want to delete:\n" + email)
                        .setPositiveButton("Delete", (d, w) -> {
                            dbHelper.deleteStudent(email);
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                            showTab(2);
                        })
                        .setNegativeButton("Cancel", null)
                        .show());
        row.addView(btnDel, bp);

        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BOTTOM NAV
    // ─────────────────────────────────────────────────────────────────────────

    LinearLayout buildBottomNav() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setBackgroundColor(Color.WHITE);
        bar.setElevation(dp(8));

        navTags  = navItem("\uD83C\uDFF7", "Tags",  () -> showTab(0));
        navOrgs  = navItem("\uD83C\uDFE2", "Orgs",  () -> showTab(1));
        navUsers = navItem("\uD83D\uDC64", "Users", () -> showTab(2));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        bar.addView(navTags, p);
        bar.addView(navOrgs, p);
        bar.addView(navUsers, p);
        return bar;
    }

    LinearLayout navItem(String emoji, String label, Runnable onClick) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(v -> onClick.run());

        TextView icon = new TextView(this);
        icon.setText(emoji);
        icon.setTextSize(20f);
        icon.setGravity(Gravity.CENTER);

        TextView txt = new TextView(this);
        txt.setText(label);
        txt.setTextSize(11f);
        txt.setGravity(Gravity.CENTER);

        item.addView(icon);
        item.addView(txt);
        return item;
    }

    void updateNavHighlight() {
        int active   = Color.parseColor("#1E3A8A");
        int inactive = Color.parseColor("#888888");
        tintNav(navTags,  activeTab == 0 ? active : inactive);
        tintNav(navOrgs,  activeTab == 1 ? active : inactive);
        tintNav(navUsers, activeTab == 2 ? active : inactive);
    }

    void tintNav(LinearLayout nav, int color) {
        for (int i = 0; i < nav.getChildCount(); i++) {
            View v = nav.getChildAt(i);
            if (v instanceof TextView) ((TextView) v).setTextColor(color);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ─────────────────────────────────────────────────────────────────────────

    View tableHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setBackgroundColor(Color.parseColor("#1E3A8A"));
        h.setPadding(dp(6), dp(10), dp(6), dp(10));
        String[] cols = {"Email", "Login", "Delete"};
        float[] wts   = {2f, 1f, 1f};
        for (int i = 0; i < cols.length; i++) {
            TextView tv = new TextView(this);
            tv.setText(cols[i]);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(13f);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setPadding(dp(4), 0, dp(4), 0);
            h.addView(tv, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, wts[i]));
        }
        return h;
    }

    TextView emptyMessage(String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextColor(Color.parseColor("#888888"));
        tv.setPadding(dp(8), dp(16), dp(8), dp(8));
        return tv;
    }

    TextView boldLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15f);
        tv.setTextColor(Color.parseColor("#222222"));
        tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    Button styledButton(String text, String colorHex) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorHex)));
        return b;
    }

    Button smallButton(String text, String colorHex) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(11f);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorHex)));
        b.setPadding(dp(4), dp(2), dp(4), dp(2));
        return b;
    }

    void setRoundedColor(View v, int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(10));
        gd.setStroke(dp(1), Color.parseColor("#BBBBBB"));
        v.setBackground(gd);
    }

    LinearLayout.LayoutParams params(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    void addGap(LinearLayout l, int dpVal) {
        View v = new View(this);
        l.addView(v, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(dpVal)));
    }

    int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }
}