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
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.flexbox.FlexboxLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class OrgHistoryInternsActivity extends AppCompatActivity {

    DBHelper dbHelper;
    String orgEmail;
    int postId;
    String postTitle;
    LinearLayout internsContainer, internsTopBar;
    
    // State for certificate upload
    private String pendingUploadStudentEmail;

    private final ActivityResultLauncher<Intent> certificatePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {
                            Uri imageUri = result.getData().getData();
                            try {
                                Bitmap bitmap = decodeBitmap(imageUri);
                                bitmap = scaleBitmap(bitmap, 1024);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                                byte[] certBytes = baos.toByteArray();
                                
                                if (pendingUploadStudentEmail != null) {
                                    boolean success = dbHelper.updateRecruitmentCertificate(postId, pendingUploadStudentEmail, certBytes);
                                    if(success) {
                                        Toast.makeText(this, "Certificate uploaded successfully.", Toast.LENGTH_SHORT).show();
                                        // Auto re-open the dialog to view the newly uploaded certificate
                                        showCertificateDialog(pendingUploadStudentEmail, certBytes, false);
                                    } else {
                                        Toast.makeText(this, "Failed to update database.", Toast.LENGTH_SHORT).show();
                                    }
                                    pendingUploadStudentEmail = null; // reset
                                }
                            } catch (IOException e) {
                                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_org_history_interns);

        internsContainer = findViewById(R.id.internsContainer);
        internsTopBar    = findViewById(R.id.internsTopBar);
        ImageView btnBack = findViewById(R.id.btnInternsBack);

        ViewCompat.setOnApplyWindowInsetsListener(internsTopBar, (v, insets) -> {
            int sb = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), sb + dp(16), v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        dbHelper = new DBHelper(this);
        orgEmail = getIntent().getStringExtra("email");
        postId   = getIntent().getIntExtra("postId", -1);
        postTitle= getIntent().getStringExtra("postTitle");

        btnBack.setOnClickListener(v -> finish());

        if (orgEmail == null || postId == -1) {
            Toast.makeText(this, "Session Expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadInterns();
    }

    private void loadInterns() {
        internsContainer.removeAllViews();
        List<String> recruitedEmails = dbHelper.getRecruitedStudentsForPost(postId);

        // Header Card
        CardView headerCard = new CardView(this);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-1, -2);
        hl.setMargins(dp(16), dp(16), dp(16), dp(16));
        headerCard.setLayoutParams(hl);
        headerCard.setRadius(dp(12));
        headerCard.setCardElevation(dp(2));
        
        LinearLayout headerInner = new LinearLayout(this);
        headerInner.setOrientation(LinearLayout.VERTICAL);
        headerInner.setPadding(dp(16), dp(16), dp(16), dp(16));
        headerCard.addView(headerInner);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(postTitle != null ? postTitle : "Post ID: " + postId);
        tvTitle.setTextSize(18f);
        tvTitle.setTextColor(Color.parseColor("#1E293B"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        headerInner.addView(tvTitle);
        
        TextView tvCount = new TextView(this);
        tvCount.setText(recruitedEmails.size() + (recruitedEmails.size() == 1 ? " intern hired" : " interns hired"));
        tvCount.setTextColor(Color.parseColor("#166534"));
        tvCount.setTextSize(14f);
        tvCount.setPadding(0, dp(4), 0, dp(4));
        headerInner.addView(tvCount);

        internsContainer.addView(headerCard);

        if (recruitedEmails.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No interns recruited yet.");
            empty.setTextColor(Color.parseColor("#64748B"));
            empty.setTextSize(16f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(40), dp(20), 0);
            internsContainer.addView(empty);
            return;
        }

        CardView listCard = new CardView(this);
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(-1, -2);
        ll.setMargins(dp(16), 0, dp(16), dp(16));
        listCard.setLayoutParams(ll);
        listCard.setRadius(dp(12));
        listCard.setCardElevation(dp(2));

        LinearLayout listInner = new LinearLayout(this);
        listInner.setOrientation(LinearLayout.VERTICAL);
        listInner.setPadding(dp(16), dp(8), dp(16), dp(8));
        listCard.addView(listInner);

        for (String email : recruitedEmails) {
            DBHelper.StudentProfile profile = dbHelper.getStudentProfile(email);
            listInner.addView(buildStudentRow(profile, email));
        }

        internsContainer.addView(listCard);
    }

    private View buildStudentRow(DBHelper.StudentProfile profile, String studentEmail) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        // Horizontal: avatar + name/email block
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
        circle.setColor(Color.parseColor("#DBEAFE"));
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
        tvName.setTextColor(Color.parseColor("#1E293B"));
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        textBlock.addView(tvName);
        TextView tvEmail = new TextView(this);
        tvEmail.setText(studentEmail);
        tvEmail.setTextSize(12f);
        tvEmail.setTextColor(Color.parseColor("#64748B"));
        textBlock.addView(tvEmail);
        hRow.addView(textBlock);
        
        // Buttons Row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams btnRowLp = new LinearLayout.LayoutParams(-1, -2);
        btnRowLp.topMargin = dp(8);
        btnRow.setLayoutParams(btnRowLp);
        
        // Profile button
        Button btnProfile = new Button(this);
        LinearLayout.LayoutParams btnProfileLp = new LinearLayout.LayoutParams(dp(80), dp(36));
        btnProfile.setLayoutParams(btnProfileLp);
        btnProfile.setText("Profile");
        btnProfile.setTextSize(11f);
        btnProfile.setAllCaps(false);
        btnProfile.setTextColor(Color.WHITE);
        btnProfile.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#1E3A8A"))); // Dark blue
        btnProfile.setOnClickListener(v -> showStudentProfile(profile));
        btnRow.addView(btnProfile);

        // Certificate button
        Button btnCertificate = new Button(this);
        LinearLayout.LayoutParams btnCertLp = new LinearLayout.LayoutParams(dp(100), dp(36));
        btnCertLp.setMarginStart(dp(8));
        btnCertificate.setLayoutParams(btnCertLp);
        btnCertificate.setText("Certificate");
        btnCertificate.setTextSize(11f);
        btnCertificate.setAllCaps(false);
        btnCertificate.setTextColor(Color.WHITE);
        btnCertificate.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#047857"))); // Emerald Green
        btnCertificate.setOnClickListener(v -> handleCertificateClick(studentEmail));
        btnRow.addView(btnCertificate);

        row.addView(btnRow);

        // Divider
        View divider = new View(this);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(-1, 1);
        divLp.topMargin = dp(8);
        divider.setLayoutParams(divLp);
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
        row.addView(divider);

        return row;
    }
    
    private void handleCertificateClick(String studentEmail) {
        boolean completed = dbHelper.isPostCompleted(postId);
        byte[] certBytes = dbHelper.getRecruitmentCertificate(postId, studentEmail);
        
        if (certBytes == null || certBytes.length == 0) {
            if (completed) {
                Toast.makeText(this, "Post is completed. Cannot upload new certificate.", Toast.LENGTH_SHORT).show();
            } else {
                pendingUploadStudentEmail = studentEmail;
                launchImagePicker();
            }
        } else {
            showCertificateDialog(studentEmail, certBytes, completed);
        }
    }

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        certificatePickerLauncher.launch(intent);
    }
    
    private void showCertificateDialog(String studentEmail, byte[] certBytes, boolean isCompleted) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_certificate);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
        
        ImageView imgView = dialog.findViewById(R.id.dialogImgCertificate);
        Bitmap bmp = BitmapFactory.decodeByteArray(certBytes, 0, certBytes.length);
        imgView.setImageBitmap(bmp);
        
        Button btnReupload = dialog.findViewById(R.id.dialogBtnReupload);
        Button btnClose    = dialog.findViewById(R.id.dialogBtnClose);
        
        if (isCompleted) {
            btnReupload.setVisibility(View.GONE);
        }
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnReupload.setOnClickListener(v -> {
            dialog.dismiss();
            pendingUploadStudentEmail = studentEmail;
            launchImagePicker();
        });
        
        dialog.show();
    }

    private void showStudentProfile(DBHelper.StudentProfile profile) {
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
                catch (Exception e) { tagColor = Color.parseColor("#94A3B8"); }
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.RECTANGLE);
                gd.setCornerRadius(dp(20));
                gd.setColor(tagColor);
                chip.setBackground(gd);
                double lum = (0.299 * Color.red(tagColor) + 0.587 * Color.green(tagColor)
                        + 0.114 * Color.blue(tagColor)) / 255;
                chip.setTextColor(lum < 0.55 ? Color.WHITE : Color.parseColor("#1E293B"));
                tagsLayout.addView(chip);
            }
        }

        // Close button
        dialog.findViewById(R.id.dialogBtnClose).setOnClickListener(v -> dialog.dismiss());
        
        // Hide recruit button
        Button btnRecruit = dialog.findViewById(R.id.dialogBtnRecruit);
        // We might not have this button in the dialog properly accessible if it's GONE by default. Let's make sure it is GONE.
        btnRecruit.setVisibility(View.GONE);

        dialog.show();
    }
    
    private Bitmap decodeBitmap(Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> decoder.setMutableRequired(true));
        } else {
            try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(is);
            }
        }
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int maxSize) {
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        if (w <= maxSize && h <= maxSize) return bitmap;
        float scale = Math.min((float) maxSize / w, (float) maxSize / h);
        return Bitmap.createScaledBitmap(bitmap, Math.round(w * scale), Math.round(h * scale), true);
    }

    private int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }
}
