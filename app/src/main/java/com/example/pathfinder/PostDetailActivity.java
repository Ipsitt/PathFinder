package com.example.pathfinder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.flexbox.FlexboxLayout; // Added Import

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostDetailActivity extends AppCompatActivity {

    //This is a joke.

    DBHelper dbHelper;
    String studentEmail;
    int postId;

    ImageView imgDetailOrgPhoto, btnBack;
    TextView tvDetailOrgName, tvDetailOrgEmail, tvDetailTitle,
            tvDetailStipend, tvDetailDuration, tvDetailDescription;

    // Fixed: detailTagsContainer is now FlexboxLayout
    FlexboxLayout detailTagsContainer;
    LinearLayout detailTopBar;
    Button btnSignUp;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_post_detail);

        imgDetailOrgPhoto    = findViewById(R.id.imgDetailOrgPhoto);
        btnBack              = findViewById(R.id.btnBack);
        tvDetailOrgName      = findViewById(R.id.tvDetailOrgName);
        tvDetailOrgEmail     = findViewById(R.id.tvDetailOrgEmail);
        tvDetailTitle        = findViewById(R.id.tvDetailTitle);
        tvDetailStipend      = findViewById(R.id.tvDetailStipend);
        tvDetailDuration     = findViewById(R.id.tvDetailDuration);
        tvDetailDescription  = findViewById(R.id.tvDetailDescription);

        // Fixed: Finding the view as FlexboxLayout
        detailTagsContainer  = findViewById(R.id.detailTagsContainer);

        btnSignUp            = findViewById(R.id.btnSignUp);
        detailTopBar         = findViewById(R.id.detailTopBar);

        // Push top bar below status bar
        ViewCompat.setOnApplyWindowInsetsListener(detailTopBar, (v, insets) -> {
            int sb = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), sb + dp(16), v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        dbHelper     = new DBHelper(this);
        postId       = getIntent().getIntExtra("post_id", -1);
        studentEmail = getIntent().getStringExtra("student_email");

        btnBack.setOnClickListener(v -> finish());

        if (postId == -1) {
            Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPost();
    }

    private void loadPost() {
        List<Post> all = dbHelper.getAllPostsWithImages();
        Post post = null;
        for (Post p : all) { if (p.id == postId) { post = p; break; } }
        if (post == null) {
            Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final Post finalPost = post;

        tvDetailTitle.setText(post.title);
        tvDetailOrgName.setText(post.orgName);
        tvDetailOrgEmail.setText(post.orgEmail);
        tvDetailStipend.setText("Rs. " + (post.stipend != null && !post.stipend.isEmpty() ? post.stipend : "Unpaid"));
        tvDetailDuration.setText(post.timePeriod != null && !post.timePeriod.isEmpty() ? post.timePeriod : "Duration TBD");
        tvDetailDescription.setText(post.description);

        if (post.orgImage != null && post.orgImage.length > 0) {
            final byte[] bytes = post.orgImage;
            executor.execute(() -> {
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                mainHandler.post(() -> { if (bmp != null) imgDetailOrgPhoto.setImageBitmap(bmp); });
            });
        } else {
            imgDetailOrgPhoto.setImageResource(android.R.drawable.ic_menu_agenda);
        }

        // Tags
        detailTagsContainer.removeAllViews();
        if (post.tags != null) {
            for (DBHelper.Tag tag : post.tags) {
                detailTagsContainer.addView(makeTagChip(tag));
            }
        }

        updateSignUpButton(finalPost);

        btnSignUp.setOnClickListener(v -> {
            if (studentEmail == null || studentEmail.isEmpty()) {
                Toast.makeText(this, "Log in to sign up", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean success = dbHelper.applyToPost(postId, studentEmail);
            if (success) {
                Toast.makeText(this, "Successfully signed up!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Already signed up", Toast.LENGTH_SHORT).show();
            }
            updateSignUpButton(finalPost);
        });
    }

    private void updateSignUpButton(Post post) {
        if (studentEmail != null && dbHelper.hasApplied(post.id, studentEmail)) {
            btnSignUp.setText("Signed Up ✓");
            btnSignUp.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#16A34A")));
            btnSignUp.setEnabled(false);
        } else {
            btnSignUp.setText("Sign Up");
            btnSignUp.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#F59E0B")));
            btnSignUp.setEnabled(true);
        }
    }

    private View makeTagChip(DBHelper.Tag tag) {
        TextView chip = new TextView(this);

        // Fixed: Use FlexboxLayout.LayoutParams instead of LinearLayout
        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT);

        lp.setMargins(0, 0, dp(8), dp(6)); // Better spacing control for Flexbox
        chip.setLayoutParams(lp);

        chip.setText(tag.label);
        chip.setTextSize(12f);

        int bgColor;
        try { bgColor = Color.parseColor(tag.color); }
        catch (Exception e) { bgColor = Color.parseColor("#94A3B8"); }

        chip.setTextColor(isColorDark(bgColor) ? Color.WHITE : Color.parseColor("#1E293B"));
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(99f);
        gd.setColor(bgColor);
        chip.setBackground(gd);
        chip.setPadding(dp(10), dp(4), dp(10), dp(4));
        return chip;
    }

    private boolean isColorDark(int color) {
        return (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255 < 0.55;
    }

    private int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }
}