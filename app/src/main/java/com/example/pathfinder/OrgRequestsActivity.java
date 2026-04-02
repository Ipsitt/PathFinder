package com.example.pathfinder;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.flexbox.FlexboxLayout;

import java.util.List;

public class OrgRequestsActivity extends AppCompatActivity {

    DBHelper dbHelper;
    String orgEmail;
    LinearLayout requestsContainer, requestsTopBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_org_requests);

        requestsContainer = findViewById(R.id.requestsContainer);
        requestsTopBar    = findViewById(R.id.requestsTopBar);
        ImageView btnBack = findViewById(R.id.btnRequestsBack);

        ViewCompat.setOnApplyWindowInsetsListener(requestsTopBar, (v, insets) -> {
            int sb = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), sb + dp(16), v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        dbHelper = new DBHelper(this);
        orgEmail = getIntent().getStringExtra("email");

        btnBack.setOnClickListener(v -> finish());

        if (orgEmail == null || orgEmail.isEmpty()) {
            Toast.makeText(this, "Session Expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper.markOrgUpdatesSeen(orgEmail);
        initBottomNav();
        loadRequests();
    }

    private void initBottomNav() {
        findViewById(R.id.bottomHomeBtn).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, OrganizationHomeActivity.class);
            intent.putExtra("email", orgEmail);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.bottomPostBtn).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, PostActivity.class);
            intent.putExtra("email", orgEmail);
            startActivity(intent);
        });

        // Current tab is Interns (Requests) - already there, but refresh
        findViewById(R.id.bottomInternsBtn).setOnClickListener(v -> loadRequests());

        findViewById(R.id.bottomHistoryBtn).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, OrganizationHistoryActivity.class);
            intent.putExtra("email", orgEmail);
            startActivity(intent);
        });

        // Hide dot since we just marked seen
        View dot = findViewById(R.id.dotOrgRequests);
        if (dot != null) dot.setVisibility(View.GONE);
    }

    private void loadRequests() {
        requestsContainer.removeAllViews();
        List<DBHelper.OrgPost> posts = dbHelper.getPostsForOrg(orgEmail, true);

        if (posts.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No posts found for:\n" + orgEmail);
            empty.setTextColor(getColor(R.color.text_secondary));
            empty.setTextSize(16f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(100), dp(20), 0);
            requestsContainer.addView(empty);
            return;
        }

        for (DBHelper.OrgPost op : posts) {
            requestsContainer.addView(buildPostCard(op));
        }
    }

    private View buildPostCard(DBHelper.OrgPost op) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(16), 0, dp(16), dp(12));
        card.setLayoutParams(lp);
        card.setRadius(dp(12));
        card.setCardElevation(dp(2));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(inner);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(op.title);
        tvTitle.setTextSize(18f);
        tvTitle.setTextColor(getColor(R.color.text_primary));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        inner.addView(tvTitle);

        TextView tvCount = new TextView(this);
        tvCount.setText(op.applicantCount + (op.applicantCount == 1 ? " applicant" : " applicants"));
        tvCount.setTextColor(getColor(R.color.secondary_bg));
        tvCount.setTextSize(12f);
        tvCount.setPadding(dp(10), dp(4), dp(10), dp(4));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(getColor(R.color.primary_accent_light));
        bg.setCornerRadius(dp(20));
        tvCount.setBackground(bg);

        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(-2, -2);
        countLp.topMargin = dp(8);
        countLp.bottomMargin = dp(12);
        tvCount.setLayoutParams(countLp);
        inner.addView(tvCount);

        if (op.applicantCount > 0) {
            List<String> applicants = dbHelper.getApplicantEmails(op.postId);
            for (String email : applicants) {
                DBHelper.StudentProfile profile = dbHelper.getStudentProfile(email);
                inner.addView(buildStudentRow(profile, email, op.postId));
            }
        }

        return card;
    }

    private View buildStudentRow(DBHelper.StudentProfile profile, String studentEmail, int postId) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        // Horizontal: avatar + name/email block + buttons
        LinearLayout hRow = new LinearLayout(this);
        hRow.setOrientation(LinearLayout.HORIZONTAL);
        hRow.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(hRow);

        // Small avatar
        ImageView avatar = new ImageView(this);
        int avatarSize = dp(40);
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(avatarSize, avatarSize);
        avatarLp.setMarginEnd(dp(10));
        avatar.setLayoutParams(avatarLp);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(getColor(R.color.primary_accent_light));
        avatar.setBackground(circle);
        avatar.setClipToOutline(true);
        if (profile != null && profile.photo != null && profile.photo.length > 0) {
            Bitmap bmp = BitmapFactory.decodeByteArray(profile.photo, 0, profile.photo.length);
            avatar.setImageBitmap(bmp);
        } else {
            avatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        }
        hRow.addView(avatar);

        // Name + email text block
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        String name = (profile != null && profile.name != null) ? profile.name : "Unknown";
        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(14f);
        tvName.setTextColor(getColor(R.color.text_primary));
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        textBlock.addView(tvName);
        TextView tvEmail = new TextView(this);
        tvEmail.setText(studentEmail);
        tvEmail.setTextSize(12f);
        tvEmail.setTextColor(getColor(R.color.text_secondary));
        textBlock.addView(tvEmail);
        hRow.addView(textBlock);

        // Profile button
        Button btnProfile = new Button(this);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(dp(80), dp(36));
        btnLp.setMarginStart(dp(6));
        btnProfile.setLayoutParams(btnLp);
        btnProfile.setText("Profile");
        btnProfile.setTextSize(11f);
        btnProfile.setAllCaps(false);
        btnProfile.setTextColor(Color.WHITE);
        btnProfile.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(getColor(R.color.primary_bg)));
        btnProfile.setOnClickListener(v -> showStudentProfile(profile, studentEmail, postId));
        hRow.addView(btnProfile);

        // Divider
        View divider = new View(this);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(-1, 1);
        divLp.topMargin = dp(6);
        divider.setLayoutParams(divLp);
        divider.setBackgroundColor(getColor(R.color.divider));
        row.addView(divider);

        return row;
    }

    private void showStudentProfile(DBHelper.StudentProfile profile, String studentEmail, int postId) {
        if (profile == null) {
            Toast.makeText(this, "Profile not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_student_profile);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }

        // Photo
        ImageView dialogPhoto = dialog.findViewById(R.id.dialogStudentPhoto);
        if (profile.photo != null && profile.photo.length > 0) {
            Bitmap bmp = BitmapFactory.decodeByteArray(profile.photo, 0, profile.photo.length);
            dialogPhoto.setImageBitmap(bmp);
        } else {
            dialogPhoto.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        // Basic fields
        ((TextView) dialog.findViewById(R.id.dialogStudentName)).setText(profile.name);
        ((TextView) dialog.findViewById(R.id.dialogStudentEmail)).setText(profile.email);
        ((TextView) dialog.findViewById(R.id.dialogStudentAge))
                .setText("🎂  Age: " + (profile.age != null ? profile.age : "—"));
        ((TextView) dialog.findViewById(R.id.dialogStudentCourse))
                .setText("📚  Course: " + (profile.course != null ? profile.course : "—"));
        ((TextView) dialog.findViewById(R.id.dialogStudentPhone))
                .setText("📞  Phone: " + (profile.phone != null ? profile.phone : "—"));

        // Tag chips
        FlexboxLayout tagsLayout = dialog.findViewById(R.id.dialogStudentTags);
        tagsLayout.removeAllViews();
        if (profile.tags != null) {
            for (DBHelper.Tag tag : profile.tags) {
                TextView chip = new TextView(this);
                FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                        FlexboxLayout.LayoutParams.WRAP_CONTENT,
                        FlexboxLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, dp(8), dp(8));
                chip.setLayoutParams(lp);
                chip.setText(tag.label);
                chip.setTextSize(12f);
                chip.setPadding(dp(12), dp(5), dp(12), dp(5));
                int tagColor;
                try { tagColor = Color.parseColor(tag.color); }
                catch (Exception e) { tagColor = getColor(R.color.text_secondary); }
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.RECTANGLE);
                gd.setCornerRadius(dp(20));
                gd.setColor(tagColor);
                chip.setBackground(gd);
                double lum = (0.299 * Color.red(tagColor) + 0.587 * Color.green(tagColor)
                        + 0.114 * Color.blue(tagColor)) / 255;
                chip.setTextColor(lum < 0.55 ? Color.WHITE : getColor(R.color.text_primary));
                tagsLayout.addView(chip);
            }
        }

        // Close button
        dialog.findViewById(R.id.dialogBtnClose).setOnClickListener(v -> dialog.dismiss());

        // Recruit button
        // Recruit button
        Button btnRecruit = dialog.findViewById(R.id.dialogBtnRecruit);

        // Fetch org name for the request
        DBHelper.Org org = dbHelper.getOrgByEmail(orgEmail);
        String orgName = org != null ? org.name : orgEmail;

        // Check current state — recruited directly OR via accepted request
        boolean alreadyRecruited = dbHelper.isRecruited(postId, studentEmail)
                || dbHelper.hasAcceptedRequest(postId, studentEmail);

        if (alreadyRecruited) {
            btnRecruit.setText("Recruited ✓");
            btnRecruit.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getColor(R.color.text_secondary)));
            btnRecruit.setEnabled(false);
        } else {
            // Get post title for the request
            String postTitle = "";
            List<DBHelper.OrgPost> posts = dbHelper.getPostsForOrg(orgEmail, true);
            for (DBHelper.OrgPost op : posts) {
                if (op.postId == postId) { postTitle = op.title; break; }
            }
            final String finalPostTitle = postTitle;

            btnRecruit.setOnClickListener(v -> {
                // Use sendRecruitRequest instead of recruitStudent directly
                // This handles auto-accept if student already applied
                boolean sent = dbHelper.sendRecruitRequest(
                        postId, studentEmail, orgEmail, orgName, finalPostTitle);

                if (sent) {
                    // Check if it was auto-accepted (student had already applied)
                    boolean wasAutoAccepted = dbHelper.isRecruited(postId, studentEmail);
                    btnRecruit.setText("Recruited ✓");
                    btnRecruit.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    getColor(R.color.text_secondary)));
                    btnRecruit.setEnabled(false);
                    if (wasAutoAccepted) {
                        Toast.makeText(this,
                                profile.name + " was already signed up — auto recruited!",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,
                                "Recruit request sent to " + profile.name,
                                Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                    loadRequests(); // refresh the list
                } else {
                    Toast.makeText(this, "Request already sent or student already recruited",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        dialog.show();
    }

    private int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }
}