package com.example.pathfinder;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
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

import com.google.android.flexbox.FlexboxLayout;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Internship post details screen for students.

public class StuPostDetailActivity extends AppCompatActivity {

    DBHelper dbHelper;
    String studentEmail;
    int postId;

    ImageView imgDetailOrgPhoto, btnBack;
    TextView tvDetailOrgName, tvDetailOrgEmail, tvDetailTitle,
            tvDetailStipend, tvDetailDuration, tvDetailDescription;

    FlexboxLayout detailTagsContainer;
    LinearLayout detailTopBar;
    Button btnSignUp;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Initializes the internship post details screen.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_stu_post_detail);

        imgDetailOrgPhoto = findViewById(R.id.imgDetailOrgPhoto);
        btnBack = findViewById(R.id.btnBack);
        tvDetailOrgName = findViewById(R.id.tvDetailOrgName);
        tvDetailOrgEmail = findViewById(R.id.tvDetailOrgEmail);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailStipend = findViewById(R.id.tvDetailStipend);
        tvDetailDuration = findViewById(R.id.tvDetailDuration);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        detailTagsContainer = findViewById(R.id.detailTagsContainer);
        btnSignUp = findViewById(R.id.btnSignUp);
        detailTopBar = findViewById(R.id.detailTopBar);

        ViewCompat.setOnApplyWindowInsetsListener(detailTopBar, (v, insets) -> {
            int statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarInset + dp(16),
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        dbHelper = new DBHelper(this);
        postId = getIntent().getIntExtra("post_id", -1);
        studentEmail = getIntent().getStringExtra("student_email");

        btnBack.setOnClickListener(v -> finish());

        if (postId == -1) {
            Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadStuPost();
    }

    // Loads the internship post details.
    private void loadStuPost() {
        StuPost post = dbHelper.getPostById(postId);
        if (post == null) {
            Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvDetailTitle.setText(post.title);
        tvDetailOrgName.setText(post.orgName);
        tvDetailOrgEmail.setText(post.orgEmail);
        tvDetailStipend.setText("Rs. " + (post.stipend != null && !post.stipend.isEmpty() ? post.stipend : "Unpaid"));
        tvDetailDuration.setText(post.timePeriod != null && !post.timePeriod.isEmpty() ? post.timePeriod : "Duration TBD");
        tvDetailDescription.setText(post.description);

        bindOrganizationProfileActions(post);
        loadOrganizationImage(post);
        renderTags(post.tags);
        updateSignUpButton(post);

        btnSignUp.setOnClickListener(v -> toggleApplication(post));
    }

    // Opens the organization profile dialog from the name, email, or image.
    private void bindOrganizationProfileActions(StuPost post) {
        View.OnClickListener listener = v -> showOrganizationInfoDialog(post.orgEmail);
        imgDetailOrgPhoto.setOnClickListener(listener);
        tvDetailOrgName.setOnClickListener(listener);
        tvDetailOrgEmail.setOnClickListener(listener);
    }

    // Loads the organization image without blocking the UI.
    private void loadOrganizationImage(StuPost post) {
        if (post.orgImage != null && post.orgImage.length > 0) {
            final byte[] bytes = post.orgImage;
            executor.execute(() -> {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                mainHandler.post(() -> {
                    if (!isFinishing() && !isDestroyed() && bitmap != null) {
                        imgDetailOrgPhoto.setImageBitmap(bitmap);
                    }
                });
            });
        } else {
            imgDetailOrgPhoto.setImageResource(android.R.drawable.ic_menu_agenda);
        }
    }

    // Draws tag chips for the internship post.
    private void renderTags(List<DBHelper.Tag> tags) {
        detailTagsContainer.removeAllViews();
        if (tags == null) {
            return;
        }

        for (DBHelper.Tag tag : tags) {
            detailTagsContainer.addView(makeTagChip(tag));
        }
    }

    // Applies or removes a student application for the current post.
    private void toggleApplication(StuPost post) {
        if (studentEmail == null || studentEmail.isEmpty()) {
            Toast.makeText(this, "Log in to sign up", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dbHelper.isRecruited(postId, studentEmail)
                || dbHelper.hasAcceptedRequest(postId, studentEmail)) {
            Toast.makeText(this, "You are already signed up via recruitment", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success;
        if (dbHelper.hasApplied(postId, studentEmail)) {
            success = dbHelper.unapplyFromStuPost(postId, studentEmail);
            Toast.makeText(this, success ? "Un-signed up successfully" : "Failed to un-sign up", Toast.LENGTH_SHORT).show();
        } else {
            success = dbHelper.applyToStuPost(postId, studentEmail);
            Toast.makeText(this, success ? "Successfully signed up!" : "Failed to sign up", Toast.LENGTH_SHORT).show();
        }
        updateSignUpButton(post);
    }

    // Updates the sign-up button state for the current post.
    private void updateSignUpButton(StuPost post) {
        if (studentEmail == null || studentEmail.isEmpty()) {
            btnSignUp.setText("Sign Up");
            btnSignUp.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getColor(R.color.primary_accent)));
            btnSignUp.setEnabled(true);
            return;
        }

        boolean isRecruited = dbHelper.isRecruited(post.id, studentEmail);
        boolean hasAccepted = dbHelper.hasAcceptedRequest(post.id, studentEmail);
        boolean hasApplied = dbHelper.hasApplied(post.id, studentEmail);

        if (isRecruited || hasAccepted) {
            btnSignUp.setText("Signed Up");
            btnSignUp.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getColor(R.color.status_success)));
            btnSignUp.setEnabled(false);
        } else if (hasApplied) {
            btnSignUp.setText("Signed Up (Tap to undo)");
            btnSignUp.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getColor(R.color.primary_bg)));
            btnSignUp.setEnabled(true);
        } else {
            btnSignUp.setText("Sign Up");
            btnSignUp.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getColor(R.color.primary_accent)));
            btnSignUp.setEnabled(true);
        }
    }

    // Shows organization details in a popup dialog.
    private void showOrganizationInfoDialog(String orgEmail) {
        DBHelper.Org org = dbHelper.getOrgByEmail(orgEmail);
        if (org == null) {
            Toast.makeText(this, "Organization details not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_org_info);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }

        ImageView dialogPhoto = dialog.findViewById(R.id.dialogOrgPhoto);
        TextView dialogName = dialog.findViewById(R.id.dialogOrgName);
        TextView dialogEmail = dialog.findViewById(R.id.dialogOrgEmail);
        TextView dialogDescription = dialog.findViewById(R.id.dialogOrgDescription);

        dialogName.setText(org.name != null && !org.name.isEmpty() ? org.name : "Organization");
        dialogEmail.setText(orgEmail);
        dialogDescription.setText(org.description != null && !org.description.isEmpty()
                ? org.description : "No description available.");

        if (org.image != null && org.image.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(org.image, 0, org.image.length);
            dialogPhoto.setImageBitmap(bitmap);
        } else {
            dialogPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        dialog.findViewById(R.id.dialogBtnCloseOrg).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Builds a tag chip view.
    private View makeTagChip(DBHelper.Tag tag) {
        TextView chip = new TextView(this);
        FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, dp(8), dp(6));
        chip.setLayoutParams(layoutParams);
        chip.setText(tag.label);
        chip.setTextSize(12f);

        int backgroundColor;
        try {
            backgroundColor = Color.parseColor(tag.color);
        } catch (Exception e) {
            backgroundColor = getColor(R.color.text_secondary);
        }

        chip.setTextColor(isColorDark(backgroundColor) ? Color.WHITE : getColor(R.color.text_primary));
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(99f);
        background.setColor(backgroundColor);
        chip.setBackground(background);
        chip.setPadding(dp(10), dp(4), dp(10), dp(4));
        return chip;
    }

    // Checks whether a tag color needs light text.
    private boolean isColorDark(int color) {
        return (0.299 * Color.red(color) + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255 < 0.55;
    }

    // Converts dp units to pixels.
    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // Stops background work when leaving the screen.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
