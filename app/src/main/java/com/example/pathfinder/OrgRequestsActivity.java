package com.example.pathfinder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.flexbox.FlexboxLayout;

import java.util.List;

public class OrgRequestsActivity extends AppCompatActivity {

    DBHelper dbHelper;
    String orgEmail;
    LinearLayout requestsContainer;
    LinearLayout requestsTopBar;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_org_requests);

        requestsContainer = findViewById(R.id.requestsContainer);
        requestsTopBar = findViewById(R.id.requestsTopBar);

        View statusBarSpacer = findViewById(R.id.statusBarSpacer);
        if (statusBarSpacer != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(statusBarSpacer, (v, insets) -> {
                androidx.core.graphics.Insets bars = insets.getInsets(
                        androidx.core.view.WindowInsetsCompat.Type.systemBars()
                                | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
                v.getLayoutParams().height = bars.top;
                v.requestLayout();
                return insets;
            });
        }

        dbHelper = new DBHelper(this);
        orgEmail = getIntent().getStringExtra("email");

        findViewById(R.id.btnMenu).setOnClickListener(this::showPopupMenu);

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
            Intent intent = new Intent(this, OrganizationHomeActivity.class);
            intent.putExtra("email", orgEmail);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.bottomPostBtn).setOnClickListener(v -> {
            Intent intent = new Intent(this, PostActivity.class);
            intent.putExtra("email", orgEmail);
            startActivity(intent);
        });

        findViewById(R.id.bottomInternsBtn).setOnClickListener(v -> loadRequests());

        findViewById(R.id.bottomHistoryBtn).setOnClickListener(v -> {
            Intent intent = new Intent(this, OrganizationHistoryActivity.class);
            intent.putExtra("email", orgEmail);
            startActivity(intent);
        });

        View dot = findViewById(R.id.dotOrgRequests);
        if (dot != null) {
            dot.setVisibility(View.GONE);
        }
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

        for (DBHelper.OrgPost post : posts) {
            requestsContainer.addView(buildPostCard(post));
        }
    }

    private View buildPostCard(DBHelper.OrgPost post) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(16), 0, dp(16), dp(12));
        card.setLayoutParams(params);
        card.setRadius(dp(12));
        card.setCardElevation(dp(2));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(inner);

        TextView title = new TextView(this);
        title.setText(post.title);
        title.setTextSize(18f);
        title.setTextColor(getColor(R.color.text_primary));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        inner.addView(title);

        TextView count = new TextView(this);
        count.setText(post.applicantCount + (post.applicantCount == 1 ? " applicant" : " applicants"));
        count.setTextColor(getColor(R.color.secondary_bg));
        count.setTextSize(12f);
        count.setPadding(dp(10), dp(4), dp(10), dp(4));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(getColor(R.color.primary_accent_light));
        badgeBg.setCornerRadius(dp(20));
        count.setBackground(badgeBg);

        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(-2, -2);
        countParams.topMargin = dp(8);
        countParams.bottomMargin = dp(12);
        count.setLayoutParams(countParams);
        inner.addView(count);

        if (post.applicantCount > 0) {
            List<String> applicants = dbHelper.getApplicantEmails(post.postId);
            for (String email : applicants) {
                DBHelper.StudentProfile profile = dbHelper.getStudentProfile(email);
                inner.addView(buildStudentRow(profile, email, post.postId));
            }
        }

        return card;
    }

    private View buildStudentRow(DBHelper.StudentProfile profile, String studentEmail, int postId) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(header);

        ImageView avatar = new ImageView(this);
        int avatarSize = dp(40);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(avatarSize, avatarSize);
        avatarParams.setMarginEnd(dp(10));
        avatar.setLayoutParams(avatarParams);
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
        header.addView(avatar);

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
        header.addView(textBlock);

        Button btnProfile = new Button(this);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(dp(80), dp(36));
        btnParams.setMarginStart(dp(6));
        btnProfile.setLayoutParams(btnParams);
        btnProfile.setText("Profile");
        btnProfile.setTextSize(11f);
        btnProfile.setAllCaps(false);
        btnProfile.setTextColor(Color.WHITE);
        btnProfile.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(getColor(R.color.primary_bg)));
        btnProfile.setOnClickListener(v -> showStudentProfile(profile, studentEmail, postId));
        header.addView(btnProfile);

        View divider = new View(this);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(-1, 1);
        divParams.topMargin = dp(6);
        divider.setLayoutParams(divParams);
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

        ImageView dialogPhoto = dialog.findViewById(R.id.dialogStudentPhoto);
        if (profile.photo != null && profile.photo.length > 0) {
            Bitmap bmp = BitmapFactory.decodeByteArray(profile.photo, 0, profile.photo.length);
            dialogPhoto.setImageBitmap(bmp);
        } else {
            dialogPhoto.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        ((TextView) dialog.findViewById(R.id.dialogStudentName)).setText(profile.name);
        ((TextView) dialog.findViewById(R.id.dialogStudentEmail)).setText(profile.email);
        ((TextView) dialog.findViewById(R.id.dialogStudentAge))
                .setText("Age: " + (profile.age != null ? profile.age : "-"));
        ((TextView) dialog.findViewById(R.id.dialogStudentCourse))
                .setText("Course: " + (profile.course != null ? profile.course : "-"));
        ((TextView) dialog.findViewById(R.id.dialogStudentPhone))
                .setText("Phone: " + (profile.phone != null ? profile.phone : "-"));

        FlexboxLayout tagsLayout = dialog.findViewById(R.id.dialogStudentTags);
        tagsLayout.removeAllViews();
        if (profile.tags != null) {
            for (DBHelper.Tag tag : profile.tags) {
                TextView chip = new TextView(this);
                FlexboxLayout.LayoutParams chipParams = new FlexboxLayout.LayoutParams(
                        FlexboxLayout.LayoutParams.WRAP_CONTENT,
                        FlexboxLayout.LayoutParams.WRAP_CONTENT);
                chipParams.setMargins(0, 0, dp(8), dp(8));
                chip.setLayoutParams(chipParams);
                chip.setText(tag.label);
                chip.setTextSize(12f);
                chip.setPadding(dp(12), dp(5), dp(12), dp(5));

                int tagColor;
                try {
                    tagColor = Color.parseColor(tag.color);
                } catch (Exception e) {
                    tagColor = getColor(R.color.text_secondary);
                }

                GradientDrawable chipBg = new GradientDrawable();
                chipBg.setShape(GradientDrawable.RECTANGLE);
                chipBg.setCornerRadius(dp(20));
                chipBg.setColor(tagColor);
                chip.setBackground(chipBg);

                double lum = (0.299 * Color.red(tagColor)
                        + 0.587 * Color.green(tagColor)
                        + 0.114 * Color.blue(tagColor)) / 255;
                chip.setTextColor(lum < 0.55 ? Color.WHITE : getColor(R.color.text_primary));
                tagsLayout.addView(chip);
            }
        }

        dialog.findViewById(R.id.dialogBtnClose).setOnClickListener(v -> dialog.dismiss());

        Button btnRecruit = dialog.findViewById(R.id.dialogBtnRecruit);
        DBHelper.Org org = dbHelper.getOrgByEmail(orgEmail);
        String orgName = org != null ? org.name : orgEmail;

        boolean alreadyRecruited = dbHelper.isRecruited(postId, studentEmail)
                || dbHelper.hasAcceptedRequest(postId, studentEmail);

        if (alreadyRecruited) {
            btnRecruit.setText("Recruited");
            btnRecruit.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getColor(R.color.text_secondary)));
            btnRecruit.setEnabled(false);
        } else {
            String postTitle = "";
            List<DBHelper.OrgPost> posts = dbHelper.getPostsForOrg(orgEmail, true);
            for (DBHelper.OrgPost post : posts) {
                if (post.postId == postId) {
                    postTitle = post.title;
                    break;
                }
            }

            final String finalPostTitle = postTitle;

            btnRecruit.setOnClickListener(v -> {
                boolean sent = dbHelper.sendRecruitRequest(
                        postId, studentEmail, orgEmail, orgName, finalPostTitle);

                if (sent) {
                    boolean wasAutoAccepted = dbHelper.isRecruited(postId, studentEmail);
                    btnRecruit.setText("Recruited");
                    btnRecruit.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(getColor(R.color.text_secondary)));
                    btnRecruit.setEnabled(false);

                    Toast.makeText(this,
                            profile.name + " was recruited successfully!",
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadRequests();

                    if (wasAutoAccepted) {
                        mainHandler.postDelayed(
                                () -> showContactStudentDialog(
                                        profile.name, studentEmail, orgName, finalPostTitle),
                                3000);
                    }
                } else {
                    Toast.makeText(this,
                            "Request already sent or student already recruited",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        dialog.show();
    }

    private void showContactStudentDialog(String studentName, String studentEmail,
                                          String orgName, String internshipTitle) {
        String safeStudentName = (studentName == null || studentName.trim().isEmpty())
                ? "the student" : studentName.trim();

        new AlertDialog.Builder(this)
                .setMessage("Would you like to contact " + safeStudentName
                        + " via email, or wait for them to email you?")
                .setPositiveButton("Yes", (dialog, which) ->
                        openGmailForOfferLetter(
                                studentEmail, safeStudentName, orgName, internshipTitle))
                .setNegativeButton("Wait for their email",
                        (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void openGmailForOfferLetter(String studentEmail, String studentName,
                                         String orgName, String internshipTitle) {
        String safeStudentName = (studentName == null || studentName.trim().isEmpty())
                ? "Student" : studentName.trim();
        String safeOrgName = (orgName == null || orgName.trim().isEmpty())
                ? orgEmail : orgName.trim();
        String safeInternshipTitle =
                (internshipTitle == null || internshipTitle.trim().isEmpty())
                        ? "Intern" : internshipTitle.trim();

        String body = "Dear " + safeStudentName + ",\n\n"
                + "We are pleased to offer you the position of "
                + safeInternshipTitle + " at " + safeOrgName
                + ". We believe this opportunity will allow you to gain valuable practical experience and contribute to our team.\n\n"
                + "Please confirm your acceptance of this offer by replying to this email. If you have any questions or require further information, feel free to contact us.\n\n"
                + "We look forward to having you with us.\n\n"
                + "Sincerely,\n"
                + safeOrgName;

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.setPackage("com.google.android.gm");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{studentEmail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Internship Offer Letter");
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(emailIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Gmail is not installed on this device.",
                    Toast.LENGTH_SHORT).show();
        }
    }

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

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
