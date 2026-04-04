package com.example.pathfinder;

import android.app.AlertDialog;
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

// Profile editing screen for students.

public class StuProfileActivity extends AppCompatActivity {

    ImageView imgProfilePic;
    EditText etStudentName, etStudentEmail, etStudentPassword,
            etStudentAge, etStudentCourse, etStudentPhone;
    Button btnStudentRegister, btnDeleteStudentAccount;
    TextView tvTagCount, tvGoToLogin, tvStudentHeaderTitle;
    FlexboxLayout tagFlexbox;

    DBHelper dbHelper;
    String studentEmail;
    byte[] selectedPhotoBytes = null;
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

    // Initializes the student profile screen.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stu_register);

        imgProfilePic = findViewById(R.id.imgProfilePic);
        etStudentName = findViewById(R.id.etStudentName);
        etStudentAge = findViewById(R.id.etStudentAge);
        etStudentCourse = findViewById(R.id.etStudentCourse);
        etStudentPhone = findViewById(R.id.etStudentPhone);
        etStudentEmail = findViewById(R.id.etStudentEmail);
        etStudentPassword = findViewById(R.id.etStudentPassword);
        btnStudentRegister = findViewById(R.id.btnStudentRegister);
        btnDeleteStudentAccount = findViewById(R.id.btnDeleteStudentAccount);
        tvTagCount = findViewById(R.id.tvTagCount);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
        tvStudentHeaderTitle = findViewById(R.id.tvStudentHeaderTitle);
        tagFlexbox = findViewById(R.id.tagFlexbox);

        dbHelper = new DBHelper(this);
        studentEmail = getIntent().getStringExtra("email");

        configureEditMode();
        loadStudentProfile();
        bindPhotoPicker();
        buildTagChips();

        btnStudentRegister.setOnClickListener(v -> attemptSave());
        btnDeleteStudentAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
    }

    // Adapts the shared registration layout for profile editing.
    private void configureEditMode() {
        tvStudentHeaderTitle.setText("Edit Account");
        btnStudentRegister.setText("Save Changes");
        btnDeleteStudentAccount.setVisibility(View.VISIBLE);
        tvGoToLogin.setText("<- Back");
        tvGoToLogin.setOnClickListener(v -> finish());

        etStudentEmail.setEnabled(false);
        etStudentEmail.setAlpha(0.5f);
        etStudentPassword.setVisibility(View.GONE);
    }

    // Loads the current student details into the form.
    private void loadStudentProfile() {
        DBHelper.StudentProfile profile = dbHelper.getStudentProfile(studentEmail);
        if (profile == null) {
            Toast.makeText(this, "Profile not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etStudentName.setText(profile.name);
        etStudentAge.setText(profile.age);
        etStudentCourse.setText(profile.course);
        etStudentPhone.setText(profile.phone);
        etStudentEmail.setText(profile.email);

        if (profile.photo != null && profile.photo.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(profile.photo, 0, profile.photo.length);
            imgProfilePic.setImageBitmap(bitmap);
            imgProfilePic.setPadding(0, 0, 0, 0);
        }
    }

    // Opens the image picker when the profile photo is tapped.
    private void bindPhotoPicker() {
        imgProfilePic.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });
    }

    // Builds selectable tag chips.
    private void buildTagChips() {
        selectedTagIds.clear();
        selectedTagIds.addAll(dbHelper.getStudentTagIds(studentEmail));

        allTags = dbHelper.getAllTags();
        tagFlexbox.removeAllViews();

        for (DBHelper.Tag tag : allTags) {
            TextView chip = new TextView(this);
            FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, 0, dp(10), dp(10));
            chip.setLayoutParams(layoutParams);
            chip.setText(tag.label);
            chip.setTextSize(13f);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            chip.setClickable(true);
            chip.setFocusable(true);

            applyChipStyle(chip, tag, selectedTagIds.contains(tag.id));

            chip.setOnClickListener(v -> toggleTagSelection(chip, tag));
            tagFlexbox.addView(chip);
        }

        updateTagCounter();
    }

    // Toggles the selected state for one interest tag.
    private void toggleTagSelection(TextView chip, DBHelper.Tag tag) {
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

        updateTagCounter();
    }

    // Updates the tag selection counter.
    private void updateTagCounter() {
        tvTagCount.setText(selectedTagIds.size() + " / 5 selected");
        tvTagCount.setTextColor(selectedTagIds.size() == MAX_TAGS
                ? getColor(R.color.status_success) : getColor(R.color.secondary_accent));
    }

    // Styles a tag chip based on whether it is selected.
    private void applyChipStyle(TextView chip, DBHelper.Tag tag, boolean selected) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(32));

        int tagColor;
        try {
            tagColor = Color.parseColor(tag.color);
        } catch (Exception e) {
            tagColor = getColor(R.color.text_secondary);
        }

        if (selected) {
            background.setColor(tagColor);
            background.setStroke(0, Color.TRANSPARENT);
            chip.setTextColor(isColorDark(tagColor) ? Color.WHITE : getColor(R.color.text_primary));
        } else {
            background.setColor(getColor(R.color.surface_alt));
            background.setStroke(dp(2), tagColor);
            chip.setTextColor(tagColor);
        }
        chip.setBackground(background);
    }

    // Validates the form and saves the student profile.
    private void attemptSave() {
        String name = etStudentName.getText().toString().trim();
        String age = etStudentAge.getText().toString().trim();
        String course = etStudentCourse.getText().toString().trim();
        String phone = etStudentPhone.getText().toString().trim();

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

    // Shows a confirmation dialog before deleting the account.
    private void showDeleteAccountConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete account?")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Deletes the student account and clears the session.
    private void deleteAccount() {
        boolean deleted = dbHelper.deleteStudent(studentEmail);
        if (!deleted) {
            Toast.makeText(this, "Could not delete account. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        getSharedPreferences("PathFinderPrefs", MODE_PRIVATE).edit().clear().apply();
        Toast.makeText(this, "Account deleted.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Decodes an image from the selected URI.
    private Bitmap decodeBitmap(Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source,
                    (decoder, info, src) -> decoder.setMutableRequired(true));
        } else {
            try (java.io.InputStream inputStream = getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(inputStream);
            }
        }
    }

    // Resizes a profile photo before saving.
    private Bitmap scaleBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float scale = Math.min((float) maxSize / width, (float) maxSize / height);
        return Bitmap.createScaledBitmap(bitmap, Math.round(width * scale), Math.round(height * scale), true);
    }

    // Checks whether a tag color needs white text.
    private boolean isColorDark(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255;
        return luminance < 0.55;
    }

    // Converts dp units to pixels.
    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
