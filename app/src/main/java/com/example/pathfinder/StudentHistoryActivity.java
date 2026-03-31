package com.example.pathfinder;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.OutputStream;
import java.util.List;

public class StudentHistoryActivity extends AppCompatActivity {

    RecyclerView rvHistory;
    LinearLayout topBar;
    ImageView btnBack;
    TextView tvEmpty;
    DBHelper dbHelper;
    String studentEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_student_history);

        topBar       = findViewById(R.id.topBar);
        btnBack      = findViewById(R.id.btnBack);
        rvHistory    = findViewById(R.id.rvHistory);
        tvEmpty      = findViewById(R.id.tvEmpty);

        dbHelper     = new DBHelper(this);
        studentEmail = getIntent().getStringExtra("email");

        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
            int sb = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), sb + dp(16),
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        btnBack.setOnClickListener(v -> finish());
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        loadHistoryPosts();
    }

    private void loadHistoryPosts() {
        List<Post> history = dbHelper.getStudentHistoryPosts(studentEmail);

        if (history.isEmpty()) {
            rvHistory.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);

            StudentHistoryAdapter adapter = new StudentHistoryAdapter(
                    this, history, studentEmail,
                    this::saveImageToGallery
            );
            rvHistory.setAdapter(adapter);
        }
    }

    private void saveImageToGallery(Bitmap bitmap, String postTitle) {
        String filename = "Certificate_" + (postTitle != null ? postTitle.replaceAll("[^a-zA-Z0-9.-]", "_") : "Unknown") + ".png";
        
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PathFinder");

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    Toast.makeText(this, "Certificate saved to Gallery!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Could not create image file", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
