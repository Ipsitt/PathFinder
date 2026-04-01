package com.example.pathfinder;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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

public class StudentRequestsActivity extends AppCompatActivity {

    LinearLayout requestsContainer, topBar;
    DBHelper dbHelper;
    String studentEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_student_requests);

        topBar             = findViewById(R.id.topBar);
        requestsContainer  = findViewById(R.id.requestsContainer);

        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
            int sb = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), sb + dp(16),
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        dbHelper     = new DBHelper(this);
        studentEmail = getIntent().getStringExtra("email");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRequests();
    }

    private void loadRequests() {
        requestsContainer.removeAllViews();
        List<DBHelper.RecruitRequest> requests =
                dbHelper.getRecruitRequestsForStudent(studentEmail);

        if (requests.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No recruit requests yet.");
            empty.setTextColor(getColor(R.color.text_secondary));
            empty.setTextSize(15f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(60), 0, 0);
            requestsContainer.addView(empty);
            return;
        }

        for (DBHelper.RecruitRequest req : requests) {
            requestsContainer.addView(buildRequestCard(req));
        }
    }

    private View buildRequestCard(DBHelper.RecruitRequest req) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.setMargins(dp(16), 0, dp(16), dp(12));
        card.setLayoutParams(cardLp);
        card.setRadius(dp(12));
        card.setCardElevation(dp(3));
        card.setCardBackgroundColor(getColor(R.color.surface_card));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(inner);

        // Org name
        TextView tvOrg = new TextView(this);
        tvOrg.setText(req.orgName);
        tvOrg.setTextSize(13f);
        tvOrg.setTextColor(getColor(R.color.secondary_accent));
        tvOrg.setTypeface(null, android.graphics.Typeface.BOLD);
        inner.addView(tvOrg);

        // Post title
        TextView tvPost = new TextView(this);
        tvPost.setText(req.postTitle);
        tvPost.setTextSize(16f);
        tvPost.setTextColor(getColor(R.color.text_primary));
        tvPost.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams postLp = new LinearLayout.LayoutParams(-1, -2);
        postLp.topMargin = dp(2);
        postLp.bottomMargin = dp(12);
        tvPost.setLayoutParams(postLp);
        inner.addView(tvPost);

        // Status badge
        TextView tvStatus = new TextView(this);
        tvStatus.setText(req.status);
        tvStatus.setTextSize(12f);
        tvStatus.setPadding(dp(12), dp(4), dp(12), dp(4));
        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setCornerRadius(dp(20));
        switch (req.status) {
            case "Accepted":
                statusBg.setColor(getColor(R.color.status_success_light));
                tvStatus.setTextColor(getColor(R.color.status_success));
                break;
            case "Rejected":
                statusBg.setColor(getColor(R.color.status_error_light));
                tvStatus.setTextColor(getColor(R.color.status_error));
                break;
            default:
                statusBg.setColor(getColor(R.color.status_warning_light));
                tvStatus.setTextColor(getColor(R.color.status_warning));
                break;
        }
        tvStatus.setBackground(statusBg);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-2, -2);
        statusLp.bottomMargin = dp(12);
        tvStatus.setLayoutParams(statusLp);
        inner.addView(tvStatus);

        // Accept / Reject buttons — only if pending
        if ("Pending".equals(req.status)) {
            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);

            Button btnAccept = new Button(this);
            LinearLayout.LayoutParams acceptLp =
                    new LinearLayout.LayoutParams(0, dp(40), 1f);
            acceptLp.setMarginEnd(dp(8));
            btnAccept.setLayoutParams(acceptLp);
            btnAccept.setText("Accept");
            btnAccept.setAllCaps(false);
            btnAccept.setTextColor(getColor(R.color.white));
            btnAccept.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getColor(R.color.status_success)));
            btnAccept.setOnClickListener(v -> {
                boolean ok = dbHelper.respondToRecruitRequest(
                        req.id, req.postId, studentEmail,
                        req.orgEmail, "Accepted");
                if (ok) {
                    Toast.makeText(this, "Request accepted!", Toast.LENGTH_SHORT).show();
                    loadRequests();
                }
            });

            Button btnReject = new Button(this);
            LinearLayout.LayoutParams rejectLp =
                    new LinearLayout.LayoutParams(0, dp(40), 1f);
            btnReject.setLayoutParams(rejectLp);
            btnReject.setText("Reject");
            btnReject.setAllCaps(false);
            btnReject.setTextColor(getColor(R.color.white));
            btnReject.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getColor(R.color.status_error)));
            btnReject.setOnClickListener(v -> {
                boolean ok = dbHelper.respondToRecruitRequest(
                        req.id, req.postId, studentEmail,
                        req.orgEmail, "Rejected");
                if (ok) {
                    Toast.makeText(this, "Request rejected.", Toast.LENGTH_SHORT).show();
                    loadRequests();
                }
            });

            btnRow.addView(btnAccept);
            btnRow.addView(btnReject);
            inner.addView(btnRow);
        }

        return card;
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}