package com.example.pathfinder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.flexbox.FlexboxLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StudentProfileActivity extends AppCompatActivity {

    ImageView imgProfilePic;
    EditText etStudentName, etStudentEmail, etStudentPassword,
            etStudentAge, etStudentCourse, etStudentPhone;
    Button btnStudentRegister;
    TextView tvTagCount, tvGoToLogin;
    FlexboxLayout tagFlexbox;

    DBHelper dbHelper;
    String studentEmail;
    byte[] selectedPhotoBytes = null; // null = keep existing photo
    final List<Integer> selectedTagIds = new ArrayList<>();
    List<DBHelper.Tag> allTags = new ArrayList<>();

    private static final int MAX_TAGS = 5;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK
                        && result.getData() != null
                        && result.getData().getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        Bitmap bitmap = decodeBitmap(uri);
                        bitmap = scaleBitmap(bitmap, 512);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                        selectedPhotoBytes = baos.toByteArray();
                        imgProfilePic.setImageBitmap(bitmap);
                        imgProfilePic.setPadding(0, 0, 0, 0);
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register); // reuse registration layout

        imgProfilePic      = findViewById(R.id.imgProfilePic);
        etStudentName      = findViewById(R.id.etStudentName);
        etStudentAge       = findViewById(R.id.etStudentAge);
        etStudentCourse    = findViewById(R.id.etStudentCourse);
        etStudentPhone     = findViewById(R.id.etStudentPhone);
        etStudentEmail     = findViewById(R.id.etStudentEmail);
        etStudentPassword  = findViewById(R.id.etStudentPassword);
        btnStudentRegister = findViewById(R.id.btnStudentRegister);
        tvTagCount         = findViewById(R.id.tvTagCount);
        tvGoToLogin        = findViewById(R.id.tvGoToLogin);
        tagFlexbox         = findViewById(R.id.tagFlexbox);

        dbHelper     = new DBHelper(this);
        studentEmail = getIntent().getStringExtra("email");

        // ── Adapt UI for edit mode ──────────────────────────────────────────
        btnStudentRegister.setText("Save Changes");
        tvGoToLogin.setText("← Back");
        tvGoToLogin.setOnClickListener(v -> finish());

        // Email is the primary key — lock it
        etStudentEmail.setEnabled(false);
        etStudentEmail.setAlpha(0.5f);

        // Password change not handled here — hide it
        etStudentPassword.setVisibility(View.GONE);

        // ── Pre-fill fields from DB using getStudentProfile ─────────────────
        DBHelper.StudentProfile profile = dbHelper.getStudentProfile(studentEmail);
        if (profile != null) {
            etStudentName.setText(profile.name);
            etStudentAge.setText(profile.age);
            etStudentCourse.setText(profile.course);
            etStudentPhone.setText(profile.phone);
            etStudentEmail.setText(profile.email);

            if (profile.photo != null) {
                Bitmap bmp = BitmapFactory.decodeByteArray(
                        profile.photo, 0, profile.photo.length);
                imgProfilePic.setImageBitmap(bmp);
                imgProfilePic.setPadding(0, 0, 0, 0);
            }
        }

        // ── Photo picker ────────────────────────────────────────────────────
        imgProfilePic.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // ── Build tag chips with existing selections pre-highlighted ────────
        buildTagChips();

        btnStudentRegister.setOnClickListener(v -> attemptSave());
    }

    private void buildTagChips() {
        // Load student's current tag IDs first
        selectedTagIds.clear();
        selectedTagIds.addAll(dbHelper.getStudentTagIds(studentEmail));

        allTags = dbHelper.getAllTags();
        tagFlexbox.removeAllViews();

        for (DBHelper.Tag tag : allTags) {
            TextView chip = new TextView(this);
            FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(10), dp(10));
            chip.setLayoutParams(lp);
            chip.setText(tag.label);
            chip.setTextSize(13f);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            chip.setClickable(true);
            chip.setFocusable(true);

            // Pre-highlight existing selections
            applyChipStyle(chip, tag, selectedTagIds.contains(tag.id));

            chip.setOnClickListener(v -> {
                boolean isSelected = selectedTagIds.contains(tag.id);
                if (isSelected) {
                    selectedTagIds.remove((Integer) tag.id);
                    applyChipStyle(chip, tag, false);
                } else {
                    if (selectedTagIds.size() >= MAX_TAGS) {
                        Toast.makeText(this, "You can only pick 5 tags", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    selectedTagIds.add(tag.id);
                    applyChipStyle(chip, tag, true);
                }
                tvTagCount.setText(selectedTagIds.size() + " / 5 selected");
                tvTagCount.setTextColor(selectedTagIds.size() == MAX_TAGS
                        ? Color.parseColor("#16A34A") : Color.parseColor("#2563EB"));
            });

            tagFlexbox.addView(chip);
        }

        // Sync counter with pre-loaded selections
        tvTagCount.setText(selectedTagIds.size() + " / 5 selected");
        tvTagCount.setTextColor(selectedTagIds.size() == MAX_TAGS
                ? Color.parseColor("#16A34A") : Color.parseColor("#2563EB"));
    }

    private void applyChipStyle(TextView chip, DBHelper.Tag tag, boolean selected) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(dp(32));
        int tagColor;
        try { tagColor = Color.parseColor(tag.color); }
        catch (Exception e) { tagColor = Color.parseColor("#94A3B8"); }

        if (selected) {
            gd.setColor(tagColor);
            gd.setStroke(0, Color.TRANSPARENT);
            chip.setTextColor(isColorDark(tagColor) ? Color.WHITE : Color.parseColor("#1E293B"));
        } else {
            gd.setColor(Color.parseColor("#F1F5F9"));
            gd.setStroke(dp(2), tagColor);
            chip.setTextColor(tagColor);
        }
        chip.setBackground(gd);
    }

    private void attemptSave() {
        String name   = etStudentName.getText().toString().trim();
        String age    = etStudentAge.getText().toString().trim();
        String course = etStudentCourse.getText().toString().trim();
        String phone  = etStudentPhone.getText().toString().trim();

        if (name.isEmpty() || age.isEmpty() || course.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTagIds.size() != MAX_TAGS) {
            Toast.makeText(this, "Please select exactly 5 interest tags", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = dbHelper.updateStudent(
                studentEmail, name, age, course, phone, selectedPhotoBytes);

        if (success) {
            dbHelper.saveStudentTags(studentEmail, selectedTagIds);
            Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Update failed. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Bitmap decodeBitmap(Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source,
                    (decoder, info, src) -> decoder.setMutableRequired(true));
        } else {
            try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(is);
            }
        }
    }

    private Bitmap scaleBitmap(Bitmap bmp, int maxSize) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        if (w <= maxSize && h <= maxSize) return bmp;
        float scale = Math.min((float) maxSize / w, (float) maxSize / h);
        return Bitmap.createScaledBitmap(bmp, Math.round(w * scale), Math.round(h * scale), true);
    }

    private boolean isColorDark(int color) {
        double lum = (0.299 * Color.red(color) + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255;
        return lum < 0.55;
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}