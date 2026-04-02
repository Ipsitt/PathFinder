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

public class StudentRegisterActivity extends AppCompatActivity {

    ImageView imgProfilePic;
    EditText etStudentName, etStudentEmail, etStudentPassword,
            etStudentAge, etStudentCourse, etStudentPhone;
    Button btnStudentRegister;
    TextView tvTagCount, tvGoToLogin;
    FlexboxLayout tagFlexbox;

    DBHelper dbHelper;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register);

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

        dbHelper = new DBHelper(this);

        imgProfilePic.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        buildTagChips();
        btnStudentRegister.setOnClickListener(v -> attemptRegister());
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, StudentLoginActivity.class));
            finish();
        });
    }

    private void buildTagChips() {
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
            applyChipStyle(chip, tag, false);

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

    private void attemptRegister() {
        String name     = etStudentName.getText().toString().trim();
        String age      = etStudentAge.getText().toString().trim();
        String course   = etStudentCourse.getText().toString().trim();
        String phone    = etStudentPhone.getText().toString().trim();
        String email    = etStudentEmail.getText().toString().trim();
        String password = etStudentPassword.getText().toString().trim();

        if (name.isEmpty() || age.isEmpty() || course.isEmpty()
                || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Password validation: min 5 chars, 1 capital, 1 number
        if (!password.matches("^(?=.*[0-9])(?=.*[A-Z]).{5,}$")) {
            Toast.makeText(this, "Password must be at least 5 characters long, " +
                    "contain at least one number and one capital letter", Toast.LENGTH_LONG).show();
            return;
        }
        if (selectedTagIds.size() != MAX_TAGS) {
            Toast.makeText(this, "Please select exactly 5 interest tags", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dbHelper.studentExists(email)) {
            Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = dbHelper.insertStudent(
                name, email, password, age, course, phone, selectedPhotoBytes);

        if (success) {
            // Save session
            getSharedPreferences("PathFinderPrefs", MODE_PRIVATE).edit()
                    .putString("logged_in_email", email)
                    .putString("user_type", "student")
                    .apply();

            dbHelper.saveStudentTags(email, selectedTagIds);
            Toast.makeText(this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, StudentHomeActivity.class);
            intent.putExtra("email", email);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Registration failed. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

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