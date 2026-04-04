package com.example.pathfinder;

import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Internship history screen for organizations.

public class OrgHistoryActivity extends AppCompatActivity {

    RecyclerView rvHistoryStuPosts;
    LinearLayout bottomHomeBtn, bottomStuPostBtn, bottomInternsBtn, bottomHistoryBtn, topBar;
    DBHelper dbHelper;
    String orgEmail;
    OrgHistoryPostAdapter adapter;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isLoadingPosts = false;

    // Initializes the organization history screen.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_org_history);

        rvHistoryStuPosts   = findViewById(R.id.rvHistoryStuPosts);
        bottomHomeBtn    = findViewById(R.id.bottomHomeBtn);
        bottomStuPostBtn    = findViewById(R.id.bottomStuPostBtn);
        bottomInternsBtn = findViewById(R.id.bottomInternsBtn);
        bottomHistoryBtn = findViewById(R.id.bottomHistoryBtn);
        topBar           = findViewById(R.id.historyTopBar);

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

        dbHelper = new DBHelper(this);
        orgEmail = getIntent().getStringExtra("email");

        findViewById(R.id.btnMenu).setOnClickListener(v -> showPopupMenu(v));

        if (orgEmail == null) {
            Toast.makeText(this, "Session Expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvHistoryStuPosts.setLayoutManager(new LinearLayoutManager(this));

        // Bottom Navigation Listeners
        bottomHomeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrgHomeActivity.class);
            intent.putExtra("email", orgEmail);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        bottomStuPostBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrgPostActivity.class);
            intent.putExtra("email", orgEmail);
            startActivity(intent);
        });

        bottomInternsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrgRequestsActivity.class);
            intent.putExtra("email", orgEmail);
            startActivity(intent);
        });

        bottomHistoryBtn.setOnClickListener(v -> {
            // Already here
        });
    }

    // Refreshes the completed post list.
    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryStuPosts();
    }

    // Loads completed internship posts for the organization.
    private void loadHistoryStuPosts() {
        if (isLoadingPosts) {
            return;
        }

        isLoadingPosts = true;
        rvHistoryStuPosts.setEnabled(false);

        dbExecutor.execute(() -> {
            List<DBHelper.OrgPost> posts = dbHelper.getPostsForOrg(orgEmail, false);
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                adapter = new OrgHistoryPostAdapter(this, posts, orgEmail);
                rvHistoryStuPosts.setAdapter(adapter);
                rvHistoryStuPosts.setEnabled(true);
                isLoadingPosts = false;
            });
        });
    }

    // Shows the organization menu.
    private void showPopupMenu(View view) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, view);
        popup.getMenu().add("Logout");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Logout")) {
                getSharedPreferences("PathFinderPrefs", MODE_PRIVATE).edit().clear().apply();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                return true;
            }
            return false;
        });
        popup.show();
    }

    // Converts dp units to pixels.
    private int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }

    // Stops the background loader when the screen is destroyed.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdownNow();
    }
}
