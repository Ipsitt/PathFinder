package com.example.pathfinder;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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

    Set<Integer> studentTagIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Let our content draw behind the status bar so we can control the space ourselves
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_student_home);

        rvPosts   = findViewById(R.id.rvPosts);
        etSearch  = findViewById(R.id.etSearch);
        btnMenu   = findViewById(R.id.btnMenu);
        topBar    = findViewById(R.id.topBar);

        dbHelper     = new DBHelper(this);
        studentEmail = getIntent().getStringExtra("email");

        if (studentEmail != null) {
            studentTagIds.addAll(dbHelper.getStudentTagIds(studentEmail));
        }

        // ── Push top bar down by the status bar height (fixes camera hole) ──
        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            // Add the status bar height as extra top padding on top of the existing 16dp
            v.setPadding(
                    v.getPaddingLeft(),
                    statusBarHeight + (int)(16 * getResources().getDisplayMetrics().density),
                    v.getPaddingRight(),
                    v.getPaddingBottom());
            return insets;
        });

        rvPosts.setLayoutManager(new LinearLayoutManager(this));

        List<Post> ranked = getRankedPosts(null);
        adapter = new PostAdapter(this, ranked, post ->
                Toast.makeText(this, "Clicked: " + post.title, Toast.LENGTH_SHORT).show());
        rvPosts.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                adapter.updatePosts(getRankedPosts(q.isEmpty() ? null : q));
            }
        });

        btnMenu.setOnClickListener(v -> showPopupMenu());
    }

    @Override
    protected void onResume() {
        super.onResume();
        String q = etSearch.getText().toString().trim();
        adapter.updatePosts(getRankedPosts(q.isEmpty() ? null : q));
    }

    // ── Jaccard Similarity Recommendation ────────────────────────────────
    //
    //  J(student, post) = |student_tags ∩ post_tags| / |student_tags ∪ post_tags|
    //
    //  Posts are ranked descending by score. Posts with zero overlap still
    //  appear at the bottom — nothing is hidden.
    // ─────────────────────────────────────────────────────────────────────

    private List<Post> getRankedPosts(String searchQuery) {
        List<Post> posts = dbHelper.getPostsWithTags(searchQuery);
        if (studentTagIds.isEmpty()) return posts;

        List<ScoredPost> scored = new ArrayList<>();
        for (Post post : posts) {
            Set<Integer> postTagSet = new HashSet<>();
            if (post.tags != null) {
                for (DBHelper.Tag t : post.tags) postTagSet.add(t.id);
            }
            scored.add(new ScoredPost(post, jaccardSimilarity(studentTagIds, postTagSet)));
        }

        Collections.sort(scored, (a, b) -> Double.compare(b.score, a.score));

        List<Post> ranked = new ArrayList<>();
        for (ScoredPost sp : scored) ranked.add(sp.post);
        return ranked;
    }

    private double jaccardSimilarity(Set<Integer> a, Set<Integer> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        Set<Integer> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<Integer> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    private static class ScoredPost {
        Post post; double score;
        ScoredPost(Post p, double s) { post = p; score = s; }
    }

    // ── Popup menu ────────────────────────────────────────────────────────

    private void showPopupMenu() {
        PopupMenu popup = new PopupMenu(this, btnMenu);
        popup.inflate(R.menu.menu_student_home);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_posts) {
                etSearch.setText("");
                adapter.updatePosts(getRankedPosts(null));
                return true;
            }
            if (id == R.id.menu_logout) {
                Intent intent = new Intent(this, StudentLoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                return true;
            }
            if (id == R.id.menu_profile)  Toast.makeText(this, "Profile — coming soon",  Toast.LENGTH_SHORT).show();
            if (id == R.id.menu_applied)  Toast.makeText(this, "Applied — coming soon",  Toast.LENGTH_SHORT).show();
            if (id == R.id.menu_requests) Toast.makeText(this, "Requests — coming soon", Toast.LENGTH_SHORT).show();
            if (id == R.id.menu_history)  Toast.makeText(this, "History — coming soon",  Toast.LENGTH_SHORT).show();
            return true;
        });

        popup.show();
    }
}