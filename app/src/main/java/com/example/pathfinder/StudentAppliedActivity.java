package com.example.pathfinder;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StudentAppliedActivity extends AppCompatActivity {

    RecyclerView rvApplied;
    LinearLayout topBar;
    ImageView btnBack;
    TextView tvEmpty;
    DBHelper dbHelper;
    String studentEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Match the same edge-to-edge style as StudentHomeActivity
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_student_applied);

        topBar       = findViewById(R.id.topBar);
        btnBack      = findViewById(R.id.btnBack);
        rvApplied    = findViewById(R.id.rvApplied);
        tvEmpty      = findViewById(R.id.tvEmpty);

        dbHelper     = new DBHelper(this);
        studentEmail = getIntent().getStringExtra("email");

        // Push top bar below status bar — same pattern as StudentHomeActivity
        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
            int sb = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), sb + dp(16),
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        btnBack.setOnClickListener(v -> finish());

        rvApplied.setLayoutManager(new LinearLayoutManager(this));
        loadAppliedPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppliedPosts();
    }

    private void loadAppliedPosts() {
        List<Post> applied = dbHelper.getAppliedPosts(studentEmail);

        if (applied.isEmpty()) {
            rvApplied.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvApplied.setVisibility(View.VISIBLE);

            // Fetch which posts the student has been recruited for
            List<DBHelper.RecruitmentEntry> recruitments =
                    dbHelper.getRecruitmentsForStudent(studentEmail);

            // Applied mode: shows "Accepted" banner on recruited cards
            PostAdapter adapter = new PostAdapter(
                    this, applied, studentEmail, null, true, recruitments);
            rvApplied.setAdapter(adapter);
        }
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}