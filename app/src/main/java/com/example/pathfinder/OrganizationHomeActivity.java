package com.example.pathfinder;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.flexbox.FlexboxLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OrganizationHomeActivity extends AppCompatActivity {

    ImageView imgOrg;
    EditText etOrgName, etOrgDescription, etStudentSearch;
    Button btnUpdate;
    LinearLayout bottomHomeBtn, bottomPostBtn, bottomInternsBtn, bottomHistoryBtn;
    LinearLayout studentCardsContainer;

    DBHelper dbHelper;
    String orgEmail;
    private byte[] pendingImageBytes = null;
    private List<DBHelper.RankedStudent> allRankedStudents = new ArrayList<>();

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {
                            Uri imageUri = result.getData().getData();
                            try {
                                Bitmap bitmap = decodeBitmap(imageUri);
                                bitmap = scaleBitmap(bitmap, 512);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                                pendingImageBytes = baos.toByteArray();
                                imgOrg.setImageBitmap(bitmap);
                                Toast.makeText(this, "Image selected. Press Update to save.",
                                        Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                Toast.makeText(this, "Failed to load image",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organization_home);

        imgOrg               = findViewById(R.id.imgOrg);
        etOrgName            = findViewById(R.id.etOrgName);
        etOrgDescription     = findViewById(R.id.etOrgDescription);
        btnUpdate            = findViewById(R.id.btnUpdate);
        etStudentSearch      = findViewById(R.id.etStudentSearch);
        studentCardsContainer = findViewById(R.id.studentCardsContainer);
        bottomHomeBtn        = findViewById(R.id.bottomHomeBtn);
        bottomPostBtn        = findViewById(R.id.bottomPostBtn);
        bottomInternsBtn     = findViewById(R.id.bottomInternsBtn);
        bottomHistoryBtn     = findViewById(R.id.bottomHistoryBtn);

        dbHelper = new DBHelper(this);
        orgEmail = getIntent().getStringExtra("email");

        if (orgEmail == null) {
            Toast.makeText(this, "Error: org email not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadOrgData();

        imgOrg.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnUpdate.setOnClickListener(v -> {
            String name = etOrgName.getText().toString().trim();
            String desc = etOrgDescription.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean updated = dbHelper.updateOrgData(orgEmail, name, desc, pendingImageBytes);
            if (updated) {
                pendingImageBytes = null;
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                // Refresh student list — org name may affect post data
                loadStudentList();
            } else {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
            }
        });

        // Search with tiered scoring
        etStudentSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                if (q.isEmpty()) {
                    renderStudentCards(allRankedStudents);
                } else {
                    renderStudentCards(searchStudents(q));
                }
            }
        });

        bottomHomeBtn.setOnClickListener(v ->
                Toast.makeText(this, "You are already here", Toast.LENGTH_SHORT).show());
        bottomPostBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, PostActivity.class);
            intent.putExtra("email", orgEmail);
            startActivity(intent);
        });
        bottomInternsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrgRequestsActivity.class);
            intent.putExtra("email", orgEmail);
            startActivity(intent);
        });
        bottomHistoryBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrganizationHistoryActivity.class);
            intent.putExtra("email", orgEmail);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudentList();
    }

    private void loadOrgData() {
        DBHelper.Org org = dbHelper.getOrgByEmail(orgEmail);
        if (org != null) {
            etOrgName.setText(org.name);
            etOrgDescription.setText(org.description);
            if (org.image != null && org.image.length > 0) {
                imgOrg.setImageBitmap(
                        BitmapFactory.decodeByteArray(org.image, 0, org.image.length));
            }
        }
    }

    private void loadStudentList() {
        allRankedStudents = dbHelper.getRankedStudentsForOrg(orgEmail);
        String q = etStudentSearch.getText().toString().trim();
        if (q.isEmpty()) {
            renderStudentCards(allRankedStudents);
        } else {
            renderStudentCards(searchStudents(q));
        }
    }

    // ── Tiered search scoring ──────────────────────────────────────────────
    // Score 100 = name starts with query
    // Score 80  = name contains query
    // Score 60  = tag label exact match
    // Score 40  = tag label contains query
    // Tiebreaker: Jaccard score from TF-weighted ranking
    private List<DBHelper.RankedStudent> searchStudents(String query) {
        String q = query.toLowerCase().trim();
        List<DBHelper.RankedStudent> results = new ArrayList<>();

        for (DBHelper.RankedStudent rs : allRankedStudents) {
            String name = rs.profile.name != null
                    ? rs.profile.name.toLowerCase() : "";
            int searchScore = 0;

            if (name.startsWith(q))          searchScore = 100;
            else if (name.contains(q))       searchScore = 80;
            else {
                if (rs.profile.tags != null) {
                    for (DBHelper.Tag t : rs.profile.tags) {
                        String label = t.label != null ? t.label.toLowerCase() : "";
                        if (label.equals(q))       { searchScore = 60; break; }
                        if (label.contains(q))     { searchScore = 40; }
                    }
                }
            }

            if (searchScore > 0) {
                // Encode both scores: searchScore as primary, Jaccard as tiebreaker
                DBHelper.RankedStudent copy = new DBHelper.RankedStudent();
                copy.profile = rs.profile;
                copy.score   = searchScore + rs.score; // Jaccard is 0..1, safe tiebreaker
                results.add(copy);
            }
        }

        java.util.Collections.sort(results,
                (a, b) -> Double.compare(b.score, a.score));
        return results;
    }

    // ── Render student cards ──────────────────────────────────────────────
    private void renderStudentCards(List<DBHelper.RankedStudent> students) {
        studentCardsContainer.removeAllViews();

        if (students.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No students found.");
            empty.setTextColor(Color.parseColor("#94A3B8"));
            empty.setTextSize(14f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(24), 0, dp(24));
            studentCardsContainer.addView(empty);
            return;
        }

        for (DBHelper.RankedStudent rs : students) {
            studentCardsContainer.addView(buildStudentCard(rs));
        }
    }

    private View buildStudentCard(DBHelper.RankedStudent rs) {
        DBHelper.StudentProfile profile = rs.profile;

        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.setMargins(dp(4), dp(4), dp(4), dp(8));
        card.setLayoutParams(cardLp);
        card.setRadius(dp(12));
        card.setCardElevation(dp(3));
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.addView(inner);

        // ── Row 1: Avatar + name/course block ──────────────────────────────
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        inner.addView(topRow);

        ImageView avatar = new ImageView(this);
        int sz = dp(48);
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(sz, sz);
        avatarLp.setMarginEnd(dp(12));
        avatar.setLayoutParams(avatarLp);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.parseColor("#DBEAFE"));
        avatar.setBackground(circle);
        avatar.setClipToOutline(true);
        if (profile.photo != null && profile.photo.length > 0) {
            avatar.setImageBitmap(
                    BitmapFactory.decodeByteArray(profile.photo, 0, profile.photo.length));
        } else {
            avatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        }
        topRow.addView(avatar);

        LinearLayout nameBlock = new LinearLayout(this);
        nameBlock.setOrientation(LinearLayout.VERTICAL);
        nameBlock.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(profile.name != null ? profile.name : "Unknown");
        tvName.setTextSize(15f);
        tvName.setTextColor(Color.parseColor("#1E293B"));
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        nameBlock.addView(tvName);

        TextView tvCourse = new TextView(this);
        tvCourse.setText(profile.course != null ? profile.course : "");
        tvCourse.setTextSize(12f);
        tvCourse.setTextColor(Color.parseColor("#64748B"));
        nameBlock.addView(tvCourse);

        // Match score badge
        if (rs.score > 0) {
            TextView tvScore = new TextView(this);
            int matchPct = (int) Math.round(rs.score * 100);
            tvScore.setText(matchPct + "% match");
            tvScore.setTextSize(11f);
            tvScore.setTextColor(Color.parseColor("#166534"));
            GradientDrawable scoreBg = new GradientDrawable();
            scoreBg.setColor(Color.parseColor("#DCFCE7"));
            scoreBg.setCornerRadius(dp(20));
            tvScore.setBackground(scoreBg);
            tvScore.setPadding(dp(8), dp(3), dp(8), dp(3));
            LinearLayout.LayoutParams scoreLp = new LinearLayout.LayoutParams(-2, -2);
            scoreLp.topMargin = dp(4);
            tvScore.setLayoutParams(scoreLp);
            nameBlock.addView(tvScore);
        }
        topRow.addView(nameBlock);

        // ── Divider ─────────────────────────────────────────────────────────
        View divider = new View(this);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(-1, 1);
        divLp.topMargin = dp(10);
        divLp.bottomMargin = dp(10);
        divider.setLayoutParams(divLp);
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
        inner.addView(divider);

        // ── Tag chips ────────────────────────────────────────────────────────
        if (profile.tags != null && !profile.tags.isEmpty()) {
            LinearLayout tagRow = new LinearLayout(this);
            tagRow.setOrientation(LinearLayout.HORIZONTAL);
            android.widget.HorizontalScrollView hsv =
                    new android.widget.HorizontalScrollView(this);
            hsv.setHorizontalScrollBarEnabled(false);
            LinearLayout.LayoutParams hsvLp =
                    new LinearLayout.LayoutParams(-1, -2);
            hsvLp.bottomMargin = dp(10);
            hsv.setLayoutParams(hsvLp);
            hsv.addView(tagRow);
            inner.addView(hsv);

            for (DBHelper.Tag tag : profile.tags) {
                TextView chip = new TextView(this);
                LinearLayout.LayoutParams chipLp =
                        new LinearLayout.LayoutParams(-2, -2);
                chipLp.setMarginEnd(dp(6));
                chip.setLayoutParams(chipLp);
                chip.setText(tag.label);
                chip.setTextSize(11f);
                chip.setPadding(dp(8), dp(3), dp(8), dp(3));
                int tagColor;
                try { tagColor = Color.parseColor(tag.color); }
                catch (Exception e) { tagColor = Color.parseColor("#94A3B8"); }
                double lum = (0.299 * Color.red(tagColor) + 0.587 * Color.green(tagColor)
                        + 0.114 * Color.blue(tagColor)) / 255;
                chip.setTextColor(lum < 0.55 ? Color.WHITE
                        : Color.parseColor("#1E293B"));
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.RECTANGLE);
                gd.setCornerRadius(99f);
                gd.setColor(tagColor);
                chip.setBackground(gd);
                tagRow.addView(chip);
            }
        }

        // ── Recruit button ───────────────────────────────────────────────────
        Button btnRecruit = new Button(this);
        LinearLayout.LayoutParams btnLp =
                new LinearLayout.LayoutParams(-1, dp(42));
        btnRecruit.setLayoutParams(btnLp);
        btnRecruit.setText("Recruit");
        btnRecruit.setAllCaps(false);
        btnRecruit.setTextColor(Color.WHITE);
        btnRecruit.setTextSize(14f);
        btnRecruit.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#1E3A8A")));
        btnRecruit.setOnClickListener(v ->
                showRecruitPostPicker(profile));
        inner.addView(btnRecruit);

        return card;
    }

    // ── Post picker dialog ────────────────────────────────────────────────
    private void showRecruitPostPicker(DBHelper.StudentProfile profile) {
        List<DBHelper.OrgPost> activePosts =
                dbHelper.getPostsForOrg(orgEmail, true);

        if (activePosts.isEmpty()) {
            Toast.makeText(this, "No active posts to recruit for",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_recruit_post_picker);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(
                    android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.dialogPickerTitle);
        tvTitle.setText("Recruit " + profile.name + " for…");

        LinearLayout postsContainer =
                dialog.findViewById(R.id.dialogPostsContainer);
        postsContainer.removeAllViews();

        DBHelper.Org org = dbHelper.getOrgByEmail(orgEmail);
        String orgName = org != null ? org.name : orgEmail;

        for (DBHelper.OrgPost op : activePosts) {
            boolean alreadySent = dbHelper.hasRecruitRequest(
                    op.postId, profile.email);
            boolean alreadyRecruited = dbHelper.isRecruited(
                    op.postId, profile.email);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowLp =
                    new LinearLayout.LayoutParams(-1, -2);
            rowLp.setMargins(0, 0, 0, dp(10));
            row.setLayoutParams(rowLp);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setColor(Color.parseColor("#F8FAFC"));
            rowBg.setCornerRadius(dp(8));
            row.setBackground(rowBg);

            TextView tvPost = new TextView(this);
            tvPost.setText(op.title);
            tvPost.setTextSize(14f);
            tvPost.setTextColor(Color.parseColor("#1E293B"));
            tvPost.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(tvPost);

            Button btnSend = new Button(this);
            LinearLayout.LayoutParams sendLp =
                    new LinearLayout.LayoutParams(dp(90), dp(36));
            sendLp.setMarginStart(dp(8));
            btnSend.setLayoutParams(sendLp);
            btnSend.setAllCaps(false);
            btnSend.setTextSize(12f);
            btnSend.setTextColor(Color.WHITE);

            if (alreadyRecruited) {
                btnSend.setText("Recruited ✓");
                btnSend.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                Color.parseColor("#9CA3AF")));
                btnSend.setEnabled(false);
            } else if (alreadySent) {
                btnSend.setText("Sent ✓");
                btnSend.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                Color.parseColor("#D97706")));
                btnSend.setEnabled(false);
            } else {
                btnSend.setText("Send");
                btnSend.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                Color.parseColor("#1E3A8A")));
                final String finalOrgName = orgName;
                final DBHelper.OrgPost finalOp = op;
                btnSend.setOnClickListener(v -> {
                    boolean sent = dbHelper.sendRecruitRequest(
                            finalOp.postId, profile.email,
                            orgEmail, finalOrgName, finalOp.title);
                    if (sent) {
                        btnSend.setText("Sent ✓");
                        btnSend.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(
                                        Color.parseColor("#D97706")));
                        btnSend.setEnabled(false);
                        Toast.makeText(this,
                                "Request sent to " + profile.name,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Already sent",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
            row.addView(btnSend);
            postsContainer.addView(row);
        }

        dialog.findViewById(R.id.dialogPickerClose)
                .setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private Bitmap decodeBitmap(Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source =
                    ImageDecoder.createSource(getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source,
                    (decoder, info, src) -> decoder.setMutableRequired(true));
        } else {
            try (java.io.InputStream is =
                         getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(is);
            }
        }
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int maxSize) {
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        if (w <= maxSize && h <= maxSize) return bitmap;
        float scale = Math.min((float) maxSize / w, (float) maxSize / h);
        return Bitmap.createScaledBitmap(bitmap,
                Math.round(w * scale), Math.round(h * scale), true);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}