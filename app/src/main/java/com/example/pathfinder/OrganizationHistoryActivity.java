package com.example.pathfinder;

import android.content.Intent;
import android.graphics.Color;
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

public class OrganizationHistoryActivity extends AppCompatActivity {

    RecyclerView rvHistoryPosts;
    LinearLayout bottomHomeBtn, bottomPostBtn, bottomInternsBtn, bottomHistoryBtn, topBar;
    DBHelper dbHelper;
    String orgEmail;
    OrgHistoryPostAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_organization_history);

        rvHistoryPosts   = findViewById(R.id.rvHistoryPosts);
        bottomHomeBtn    = findViewById(R.id.bottomHomeBtn);
        bottomPostBtn    = findViewById(R.id.bottomPostBtn);
        bottomInternsBtn = findViewById(R.id.bottomInternsBtn);
        bottomHistoryBtn = findViewById(R.id.bottomHistoryBtn);
        topBar           = findViewById(R.id.historyTopBar);

        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
            int sb = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), sb + dp(16), v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        dbHelper = new DBHelper(this);
        orgEmail = getIntent().getStringExtra("email");

        if (orgEmail == null) {
            Toast.makeText(this, "Session Expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvHistoryPosts.setLayoutManager(new LinearLayoutManager(this));

        // Bottom Navigation Listeners
        bottomHomeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrganizationHomeActivity.class);
            intent.putExtra("email", orgEmail);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        bottomPostBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, PostActivity.class);
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

    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryPosts();
    }

    private void loadHistoryPosts() {
        List<DBHelper.OrgPost> posts = dbHelper.getPostsForOrg(orgEmail, false);
        adapter = new OrgHistoryPostAdapter(this, posts, orgEmail);
        rvHistoryPosts.setAdapter(adapter);
    }

    private int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }
}
