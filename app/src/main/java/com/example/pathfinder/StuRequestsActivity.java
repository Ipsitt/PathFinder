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

import java.util.List;

// Recruitment requests screen for students.

public class StuRequestsActivity extends AppCompatActivity {

    LinearLayout requestsContainer, topBar;
    DBHelper dbHelper;
    String studentEmail;

    // Initializes the recruitment requests screen.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_stu_requests);

        topBar = findViewById(R.id.topBar);
        requestsContainer = findViewById(R.id.requestsContainer);

        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
            int statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarInset + dp(16),
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        dbHelper = new DBHelper(this);
        studentEmail = getIntent().getStringExtra("email");

        if (studentEmail != null) {
            dbHelper.markStudentRequestsSeen(studentEmail);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // Refreshes the recruitment requests list.
    @Override
    protected void onResume() {
        super.onResume();
        loadRequests();
    }

    // Loads recruit requests for the student.
    private void loadRequests() {
        requestsContainer.removeAllViews();
        List<DBHelper.RecruitRequest> requests = dbHelper.getRecruitRequestsForStudent(studentEmail);

        if (requests.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No recruit requests yet.");
            empty.setTextColor(getColor(R.color.text_secondary));
            empty.setTextSize(15f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(60), 0, 0);
            requestsContainer.addView(empty);
            return;
        }

        for (DBHelper.RecruitRequest request : requests) {
            requestsContainer.addView(buildRequestCard(request));
        }
    }

    // Builds a recruit request card.
    private View buildRequestCard(DBHelper.RecruitRequest req) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(dp(16), 0, dp(16), dp(12));
        card.setLayoutParams(cardParams);
        card.setRadius(dp(12));
        card.setCardElevation(dp(3));
        card.setCardBackgroundColor(getColor(R.color.surface_card));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(inner);

        TextView tvOrg = new TextView(this);
        tvOrg.setText(req.orgName);
        tvOrg.setTextSize(13f);
        tvOrg.setTextColor(getColor(R.color.secondary_accent));
        tvOrg.setTypeface(null, android.graphics.Typeface.BOLD);
        inner.addView(tvOrg);

        TextView tvPost = new TextView(this);
        tvPost.setText(req.postTitle);
        tvPost.setTextSize(16f);
        tvPost.setTextColor(getColor(R.color.text_primary));
        tvPost.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams postParams = new LinearLayout.LayoutParams(-1, -2);
        postParams.topMargin = dp(2);
        postParams.bottomMargin = dp(12);
        tvPost.setLayoutParams(postParams);
        inner.addView(tvPost);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(req.status);
        tvStatus.setTextSize(12f);
        tvStatus.setPadding(dp(12), dp(4), dp(12), dp(4));
        tvStatus.setBackground(makeStatusBackground(req.status, tvStatus));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2, -2);
        statusParams.bottomMargin = dp(12);
        tvStatus.setLayoutParams(statusParams);
        inner.addView(tvStatus);

        if ("Pending".equals(req.status)) {
            LinearLayout buttonRow = new LinearLayout(this);
            buttonRow.setOrientation(LinearLayout.HORIZONTAL);

            Button btnAccept = buildRequestActionButton("Accept", R.color.status_success);
            LinearLayout.LayoutParams acceptParams = new LinearLayout.LayoutParams(0, dp(40), 1f);
            acceptParams.setMarginEnd(dp(8));
            btnAccept.setLayoutParams(acceptParams);
            btnAccept.setOnClickListener(v -> {
                boolean updated = dbHelper.respondToRecruitRequest(
                        req.id, req.postId, studentEmail, req.orgEmail, "Accepted");
                if (updated) {
                    Toast.makeText(this, "Request accepted!", Toast.LENGTH_SHORT).show();
                    loadRequests();
                }
            });

            Button btnReject = buildRequestActionButton("Reject", R.color.status_error);
            btnReject.setLayoutParams(new LinearLayout.LayoutParams(0, dp(40), 1f));
            btnReject.setOnClickListener(v -> {
                boolean updated = dbHelper.respondToRecruitRequest(
                        req.id, req.postId, studentEmail, req.orgEmail, "Rejected");
                if (updated) {
                    Toast.makeText(this, "Request rejected.", Toast.LENGTH_SHORT).show();
                    loadRequests();
                }
            });

            buttonRow.addView(btnAccept);
            buttonRow.addView(btnReject);
            inner.addView(buttonRow);
        }

        card.setOnClickListener(v -> showRequestDetails(req));
        return card;
    }

    // Creates a colored status background and text color.
    private GradientDrawable makeStatusBackground(String status, TextView tvStatus) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(20));

        switch (status) {
            case "Accepted":
                background.setColor(getColor(R.color.status_success_light));
                tvStatus.setTextColor(getColor(R.color.status_success));
                break;
            case "Rejected":
                background.setColor(getColor(R.color.status_error_light));
                tvStatus.setTextColor(getColor(R.color.status_error));
                break;
            default:
                background.setColor(getColor(R.color.status_warning_light));
                tvStatus.setTextColor(getColor(R.color.status_warning));
                break;
        }
        return background;
    }

    // Builds one action button for a request card.
    private Button buildRequestActionButton(String text, int colorRes) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(getColor(R.color.white));
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(colorRes)));
        return button;
    }

    // Shows a popup with full internship request details.
    private void showRequestDetails(DBHelper.RecruitRequest req) {
        StuPost post = dbHelper.getPostById(req.postId);
        if (post == null) {
            Toast.makeText(this, "Post details not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_stu_request_detail);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }

        ImageView requestOrgPhoto = dialog.findViewById(R.id.requestOrgPhoto);
        TextView requestOrgName = dialog.findViewById(R.id.requestOrgName);
        TextView requestOrgEmail = dialog.findViewById(R.id.requestOrgEmail);
        TextView requestPostTitle = dialog.findViewById(R.id.requestPostTitle);
        TextView requestPostStipend = dialog.findViewById(R.id.requestPostStipend);
        TextView requestPostDuration = dialog.findViewById(R.id.requestPostDuration);
        TextView requestPostDescription = dialog.findViewById(R.id.requestPostDescription);

        requestOrgName.setText(post.orgName);
        requestOrgEmail.setText(post.orgEmail);
        requestPostTitle.setText(post.title);
        requestPostStipend.setText((post.stipend != null && !post.stipend.isEmpty()) ? "Rs. " + post.stipend : "Unpaid");
        requestPostDuration.setText(post.timePeriod != null && !post.timePeriod.isEmpty() ? post.timePeriod : "Duration TBD");
        requestPostDescription.setText(post.description != null ? post.description : "No description available.");

        if (post.orgImage != null && post.orgImage.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(post.orgImage, 0, post.orgImage.length);
            requestOrgPhoto.setImageBitmap(bitmap);
        } else {
            requestOrgPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        View orgHeader = dialog.findViewById(R.id.requestOrgHeader);
        View.OnClickListener orgClickListener = v -> showOrganizationInfoDialog(post.orgEmail);
        orgHeader.setOnClickListener(orgClickListener);
        requestOrgPhoto.setOnClickListener(orgClickListener);
        requestOrgName.setOnClickListener(orgClickListener);

        dialog.findViewById(R.id.dialogBtnCloseRequest).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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

    // Converts dp units to pixels.
    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
