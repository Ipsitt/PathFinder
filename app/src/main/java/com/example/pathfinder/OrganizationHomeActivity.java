package com.example.pathfinder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class OrganizationHomeActivity extends AppCompatActivity {

    ImageView imgOrg;
    EditText etOrgName, etOrgDescription;
        Button btnUpdate;

    // Bottom Nav Buttons
    LinearLayout bottomHomeBtn, bottomPostBtn, bottomInternsBtn, bottomHistoryBtn;

    DBHelper dbHelper;
    String orgEmail;
    private byte[] pendingImageBytes = null;

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
                                Toast.makeText(this, "Image selected. Press Update to save.", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organization_home);

        // Initialize Main Views
        imgOrg           = findViewById(R.id.imgOrg);
        etOrgName        = findViewById(R.id.etOrgName);
        etOrgDescription = findViewById(R.id.etOrgDescription);
        btnUpdate        = findViewById(R.id.btnUpdate);

        // Initialize Bottom Nav Views
        bottomHomeBtn    = findViewById(R.id.bottomHomeBtn);
        bottomPostBtn    = findViewById(R.id.bottomPostBtn);
        bottomInternsBtn = findViewById(R.id.bottomInternsBtn);
        bottomHistoryBtn = findViewById(R.id.bottomHistoryBtn);

        dbHelper = new DBHelper(this);
        orgEmail = getIntent().getStringExtra("email");

        if (orgEmail == null) {
            Toast.makeText(this, "Error: org email not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadOrgData();

        // --- Listeners ---

        imgOrg.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnUpdate.setOnClickListener(v -> {
            String name = etOrgName.getText().toString().trim();
            String desc = etOrgDescription.getText().toString().trim();
            if (name.isEmpty()) { Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show(); return; }
            boolean updated = dbHelper.updateOrgData(orgEmail, name, desc, pendingImageBytes);
            if (updated) {
                pendingImageBytes = null;
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
            }
        });

        // --- Bottom Navigation Listeners ---
        
        bottomHomeBtn.setOnClickListener(v -> {
            Toast.makeText(this, "You are already here", Toast.LENGTH_SHORT).show();
        });

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

    private void loadOrgData() {
        DBHelper.Org org = dbHelper.getOrgByEmail(orgEmail);
        if (org != null) {
            etOrgName.setText(org.name);
            etOrgDescription.setText(org.description);
            if (org.image != null && org.image.length > 0) {
                imgOrg.setImageBitmap(BitmapFactory.decodeByteArray(org.image, 0, org.image.length));
            }
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

    private Bitmap scaleBitmap(Bitmap bitmap, int maxSize) {
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        if (w <= maxSize && h <= maxSize) return bitmap;
        float scale = Math.min((float) maxSize / w, (float) maxSize / h);
        return Bitmap.createScaledBitmap(bitmap, Math.round(w * scale), Math.round(h * scale), true);
    }
}