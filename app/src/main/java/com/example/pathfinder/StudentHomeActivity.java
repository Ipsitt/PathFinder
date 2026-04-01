package com.example.pathfinder;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StudentHomeActivity extends AppCompatActivity {

    RecyclerView rvPosts;
    EditText etSearch;
    ImageView btnMenu;
    LinearLayout topBar;
    PostAdapter adapter;
    DBHelper dbHelper;
    String studentEmail;

    List<Post> allPosts = new ArrayList<>();
    Set<Integer> studentTagIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_student_home);

        rvPosts      = findViewById(R.id.rvPosts);
        etSearch     = findViewById(R.id.etSearch);
        btnMenu      = findViewById(R.id.btnMenu);
        topBar       = findViewById(R.id.topBar);
        dbHelper     = new DBHelper(this);
        studentEmail = getIntent().getStringExtra("email");

        if (studentEmail != null) {
            studentTagIds.addAll(dbHelper.getStudentTagIds(studentEmail));
        }

        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
            int sb = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), sb + dp(16), v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        allPosts = dbHelper.getAllPostsWithImages();

        adapter = new PostAdapter(this, rankAndFilter(allPosts, null), studentEmail, null);
        rvPosts.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                adapter.updatePosts(rankAndFilter(allPosts, q.isEmpty() ? null : q));
            }
        });

        btnMenu.setOnClickListener(v -> showPopupMenu());
    }

    @Override
    protected void onResume() {
        super.onResume();
        allPosts = dbHelper.getAllPostsWithImages();
        String q = etSearch.getText().toString().trim();
        adapter.updatePosts(rankAndFilter(allPosts, q.isEmpty() ? null : q));
    }

    // ─────────────────────────────────────────────────────────────────────
    // SCORING: Composite Jaccard + Text Priority
    // ─────────────────────────────────────────────────────────────────────
    private List<Post> rankAndFilter(List<Post> source, String query) {
        String q = query != null ? query.toLowerCase().trim() : null;
        boolean hasQuery = q != null && !q.isEmpty();

        List<ScoredPost> scored = new ArrayList<>();
        for (Post post : source) {
            Set<Integer> postTagSet = new HashSet<>();
            if (post.tags != null) for (DBHelper.Tag t : post.tags) postTagSet.add(t.id);

            double jaccard = jaccardSimilarity(studentTagIds, postTagSet);
            double tagScore = jaccard * 40.0;
            boolean hasTagOverlap = jaccard > 0;
            double queryScore = 0;

            if (hasQuery) {
                boolean titleMatch = post.title != null && post.title.toLowerCase().contains(q);
                boolean descMatch  = post.description != null && post.description.toLowerCase().contains(q);
                boolean tagMatch   = false;
                if (post.tags != null)
                    for (DBHelper.Tag t : post.tags)
                        if (t.label != null && t.label.toLowerCase().contains(q)) { tagMatch = true; break; }

                boolean effectiveTitleMatch = titleMatch || tagMatch;

                if      (effectiveTitleMatch && hasTagOverlap) queryScore = 30;
                else if (descMatch           && hasTagOverlap) queryScore = 20;
                else if (effectiveTitleMatch)                  queryScore = 10;
                else if (descMatch)                            queryScore = 5;
                else continue; // no match — exclude
            }

            scored.add(new ScoredPost(post, tagScore + queryScore));
        }

        Collections.sort(scored, (a, b) -> Double.compare(b.score, a.score));
        List<Post> result = new ArrayList<>();
        for (ScoredPost sp : scored) result.add(sp.post);
        return result;
    }

    private double jaccardSimilarity(Set<Integer> a, Set<Integer> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        Set<Integer> inter = new HashSet<>(a); inter.retainAll(b);
        Set<Integer> union = new HashSet<>(a); union.addAll(b);
        return (double) inter.size() / union.size();
    }

    private static class ScoredPost { Post post; double score; ScoredPost(Post p, double s) { post=p; score=s; } }

    private void showPopupMenu() {
        PopupMenu popup = new PopupMenu(this, btnMenu);
        popup.inflate(R.menu.menu_student_home);
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_posts)    { etSearch.setText(""); adapter.updatePosts(rankAndFilter(allPosts, null)); return true; }
            if (id == R.id.menu_logout)   { Intent i = new Intent(this, StudentLoginActivity.class); i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); startActivity(i); return true; }
            if (id == R.id.menu_profile) {
                Intent intent = new Intent(this, StudentProfileActivity.class);
                intent.putExtra("email", studentEmail);
                startActivity(intent);
                return true;
            }
            if (id == R.id.menu_applied) {
                Intent intent = new Intent(this, StudentAppliedActivity.class);
                intent.putExtra("email", studentEmail);
                startActivity(intent);
                return true;
            }
            if (id == R.id.menu_requests) {
                Intent intent = new Intent(this, StudentRequestsActivity.class);
                intent.putExtra("email", studentEmail);
                startActivity(intent);
                return true;
            }

            if (id == R.id.menu_history) {
                Intent intent = new Intent(this, StudentHistoryActivity.class);
                intent.putExtra("email", studentEmail);
                startActivity(intent);
                return true;
            }
            return true;
        });
        popup.show();
    }

    private int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }
}