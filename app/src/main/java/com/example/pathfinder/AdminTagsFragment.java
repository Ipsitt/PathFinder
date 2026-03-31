package com.example.pathfinder;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class AdminTagsFragment extends Fragment {

    Spinner spinnerTags;
    EditText etTagLabel, etColorHex;
    ColorPickerView colorPicker;
    View colorPreview;
    Button btnSaveTag, btnDeleteTag;

    DBHelper dbHelper;
    List<DBHelper.Tag> tagList = new ArrayList<>();
    int selectedTagId = -1;

    // Prevents feedback loops between hex field and color picker
    private boolean updatingHexFromPicker = false;
    private boolean updatingPickerFromHex = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_tags, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerTags  = view.findViewById(R.id.spinnerTags);
        etTagLabel   = view.findViewById(R.id.etTagLabel);
        etColorHex   = view.findViewById(R.id.etColorHex);
        colorPicker  = view.findViewById(R.id.colorPicker);
        colorPreview = view.findViewById(R.id.colorPreview);
        btnSaveTag   = view.findViewById(R.id.btnSaveTag);
        btnDeleteTag = view.findViewById(R.id.btnDeleteTag);

        dbHelper = new DBHelper(requireContext());

        // Color picker → update swatch + hex field
        colorPicker.setOnColorChangedListener(color -> {
            if (updatingPickerFromHex) return;
            updatingHexFromPicker = true;
            String hex = String.format("#%06X", 0xFFFFFF & color);
            etColorHex.setText(hex);
            updateSwatch(color);
            updatingHexFromPicker = false;
        });

        // Hex field → update picker + swatch when user types a valid full hex
        etColorHex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (updatingHexFromPicker) return;
                String hex = s.toString().trim();
                if (hex.matches("#[0-9A-Fa-f]{6}")) {
                    try {
                        int color = Color.parseColor(hex);
                        updatingPickerFromHex = true;
                        colorPicker.setColor(color);
                        updateSwatch(color);
                        updatingPickerFromHex = false;
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        });

        loadTags();

        spinnerTags.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                if (pos == 0) {
                    selectedTagId = -1;
                    etTagLabel.setText("");
                    applyColor(Color.RED);
                } else {
                    DBHelper.Tag tag = tagList.get(pos - 1);
                    selectedTagId = tag.id;
                    etTagLabel.setText(tag.label);
                    try {
                        applyColor(Color.parseColor(tag.color));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSaveTag.setOnClickListener(v -> saveTag());
        btnDeleteTag.setOnClickListener(v -> deleteSelectedTag());
    }

    /** Apply a color to the picker, swatch, and hex field all at once */
    private void applyColor(int color) {
        updatingHexFromPicker = true;
        colorPicker.setColor(color);
        String hex = String.format("#%06X", 0xFFFFFF & color);
        etColorHex.setText(hex);
        updateSwatch(color);
        updatingHexFromPicker = false;
    }

    private void updateSwatch(int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(12f);
        gd.setColor(color);
        gd.setStroke(2, Color.parseColor("#CCCCCC"));
        colorPreview.setBackground(gd);
    }

    private void loadTags() {
        tagList = dbHelper.getAllTags();
        List<String> labels = new ArrayList<>();
        labels.add("— New Tag —");
        for (DBHelper.Tag t : tagList) labels.add(t.label);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTags.setAdapter(adapter);
    }

    private void saveTag() {
        String label = etTagLabel.getText().toString().trim();
        if (label.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a label", Toast.LENGTH_SHORT).show();
            return;
        }

        String hex = etColorHex.getText().toString().trim();
        if (!hex.matches("#[0-9A-Fa-f]{6}")) {
            hex = colorPicker.getSelectedColorHex();
        }

        boolean success = selectedTagId == -1
                ? dbHelper.saveTag(label, hex)
                : dbHelper.updateTag(selectedTagId, label, hex);

        if (success) {
            Toast.makeText(getContext(),
                    selectedTagId == -1 ? "Tag created!" : "Tag updated!",
                    Toast.LENGTH_SHORT).show();
            selectedTagId = -1;
            etTagLabel.setText("");
            loadTags();
        } else {
            Toast.makeText(getContext(), "Save failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSelectedTag() {
        if (selectedTagId == -1) {
            Toast.makeText(getContext(), "Select a tag to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean deleted = dbHelper.deleteTag(selectedTagId);
        if (deleted) {
            Toast.makeText(getContext(), "Tag deleted", Toast.LENGTH_SHORT).show();
            selectedTagId = -1;
            etTagLabel.setText("");
            loadTags();
        } else {
            Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show();
        }
    }
}