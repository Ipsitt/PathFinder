package com.example.pathfinder;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StudentHomeActivity extends AppCompatActivity {

    RecyclerView rvPosts;
    EditText etSearch;
    PostAdapter adapter;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        rvPosts   = findViewById(R.id.rvPosts);
        etSearch  = findViewById(R.id.etSearch);
        dbHelper  = new DBHelper(this);

        rvPosts.setLayoutManager(new LinearLayoutManager(this));

        // Load all posts initially
        List<Post> posts = dbHelper.getPostsWithTags(null);
        adapter = new PostAdapter(this, posts, post ->
                Toast.makeText(this, "Clicked: " + post.title, Toast.LENGTH_SHORT).show()
                // TODO: open PostDetailActivity and pass post.id
        );
        rvPosts.setAdapter(adapter);

        // Live search — filters as the user types
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                List<Post> filtered = dbHelper.getPostsWithTags(query.isEmpty() ? null : query);
                adapter.updatePosts(filtered);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh in case new posts were added while away
        String query = etSearch.getText().toString().trim();
        adapter.updatePosts(dbHelper.getPostsWithTags(query.isEmpty() ? null : query));
    }
}