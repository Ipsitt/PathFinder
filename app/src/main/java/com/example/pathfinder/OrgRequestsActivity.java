package com.example.pathfinder;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrgRequestsActivity extends AppCompatActivity {

    DBHelper dbHelper;
    String orgEmail;
    LinearLayout requestsContainer, requestsTopBar;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_org_requests);

        requestsContainer = findViewById(R.id.requestsContainer);
        requestsTopBar    = findViewById(R.id.requestsTopBar);
        ImageView btnBack = findViewById(R.id.btnRequestsBack);

        ViewCompat.setOnApplyWindowInsetsListener(requestsTopBar, (v, insets) -> {
            int sb = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), sb + dp(16), v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        dbHelper = new DBHelper(this);
        orgEmail = getIntent().getStringExtra("email");

        btnBack.setOnClickListener(v -> finish());

        if (orgEmail == null || orgEmail.isEmpty()) {
            Toast.makeText(this, "Session Expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadRequests();
    }

    private void loadRequests() {
        requestsContainer.removeAllViews();
        List<DBHelper.OrgPost> posts = dbHelper.getPostsForOrg(orgEmail);

        if (posts.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No posts found for:\n" + orgEmail);
            empty.setTextColor(Color.parseColor("#64748B"));
            empty.setTextSize(16f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(100), dp(20), 0);
            requestsContainer.addView(empty);
            return;
        }

        for (DBHelper.OrgPost op : posts) {
            requestsContainer.addView(buildPostCard(op));
        }
    }

    private View buildPostCard(DBHelper.OrgPost op) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(16), 0, dp(16), dp(12));
        card.setLayoutParams(lp);
        card.setRadius(dp(12));
        card.setCardElevation(dp(2));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(inner);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(op.title);
        tvTitle.setTextSize(18f);
        tvTitle.setTextColor(Color.parseColor("#1E293B"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        inner.addView(tvTitle);

        TextView tvCount = new TextView(this);
        tvCount.setText(op.applicantCount + (op.applicantCount == 1 ? " applicant" : " applicants"));
        tvCount.setTextColor(Color.parseColor("#1E40AF"));
        tvCount.setTextSize(12f);
        tvCount.setPadding(dp(10), dp(4), dp(10), dp(4));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#DBEAFE"));
        bg.setCornerRadius(dp(20));
        tvCount.setBackground(bg);

        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(-2, -2);
        countLp.topMargin = dp(8);
        countLp.bottomMargin = dp(12);
        tvCount.setLayoutParams(countLp);
        inner.addView(tvCount);

        if (op.applicantCount > 0) {
            List<String> applicants = dbHelper.getApplicantEmails(op.postId);
            for (String email : applicants) {
                DBHelper.StudentProfile profile = dbHelper.getStudentProfile(email);
                inner.addView(buildStudentRow(profile, email));
            }
        }

        return card;
    }

    private View buildStudentRow(DBHelper.StudentProfile profile, String email) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView tvInfo = new TextView(this);
        tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        String name = (profile != null) ? profile.name : "Unknown";
        tvInfo.setText(name + "\n" + email);
        tvInfo.setTextSize(13f);
        row.addView(tvInfo);

        Button btn = new Button(this);
        btn.setLayoutParams(new LinearLayout.LayoutParams(dp(80), dp(36)));
        btn.setText("Profile");
        btn.setTextSize(11f);
        btn.setAllCaps(false);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E3A8A")));
        btn.setOnClickListener(v -> showStudentProfile(profile));
        row.addView(btn);

        return row;
    }

    private void showStudentProfile(DBHelper.StudentProfile profile) {
        if (profile == null) return;
        // ... (Dialog code from previous version)
    }

    private int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }
}