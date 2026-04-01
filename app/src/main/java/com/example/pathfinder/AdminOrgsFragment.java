package com.example.pathfinder;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.List;

public class AdminOrgsFragment extends Fragment {

    LinearLayout orgsTableBody;
    DBHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_orgs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        orgsTableBody = view.findViewById(R.id.orgsTableBody);
        dbHelper = new DBHelper(requireContext());
        loadOrgs();
    }

    private void loadOrgs() {
        orgsTableBody.removeAllViews();
        List<String> emails = dbHelper.getAllOrgEmails();

        if (emails.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No organizations registered yet.");
            empty.setPadding(16, 24, 16, 24);
            empty.setTextColor(requireContext().getColor(R.color.text_secondary));
            orgsTableBody.addView(empty);
            return;
        }

        for (int i = 0; i < emails.size(); i++) {
            String email = emails.get(i);
            LinearLayout row = buildRow(email, i);
            orgsTableBody.addView(row);

            // Divider
            View divider = new View(getContext());
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(requireContext().getColor(R.color.divider));
            orgsTableBody.addView(divider);
        }
    }

    private LinearLayout buildRow(String email, int index) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 8, 0, 8);
        row.setBackgroundColor(index % 2 == 0
                ? requireContext().getColor(R.color.surface_card) : requireContext().getColor(R.color.surface_alt));

        // Email cell
        TextView tvEmail = new TextView(getContext());
        LinearLayout.LayoutParams emailParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f);
        tvEmail.setLayoutParams(emailParams);
        tvEmail.setText(email);
        tvEmail.setTextSize(13f);
        tvEmail.setTextColor(requireContext().getColor(R.color.text_primary));
        tvEmail.setPadding(16, 8, 8, 8);
        tvEmail.setMaxLines(2);
        row.addView(tvEmail);

        // Login button
        Button btnLogin = new Button(getContext());
        LinearLayout.LayoutParams loginParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        loginParams.setMargins(4, 4, 4, 4);
        btnLogin.setLayoutParams(loginParams);
        btnLogin.setText("Login");
        btnLogin.setTextSize(12f);
        btnLogin.setPadding(4, 4, 4, 4);
        btnLogin.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.status_success)));
        btnLogin.setTextColor(Color.WHITE);
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), OrganizationHomeActivity.class);
            intent.putExtra("email", email);
            startActivity(intent);
        });
        row.addView(btnLogin);

        // Delete button
        Button btnDelete = new Button(getContext());
        LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        delParams.setMargins(4, 4, 4, 4);
        btnDelete.setLayoutParams(delParams);
        btnDelete.setText("Delete");
        btnDelete.setTextSize(12f);
        btnDelete.setPadding(4, 4, 4, 4);
        btnDelete.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.status_error)));
        btnDelete.setTextColor(Color.WHITE);
        btnDelete.setOnClickListener(v -> confirmDelete(email));
        row.addView(btnDelete);

        return row;
    }

    private void confirmDelete(String email) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Organization")
                .setMessage("Delete \"" + email + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean deleted = dbHelper.deleteOrg(email);
                    if (deleted) {
                        Toast.makeText(getContext(), "Organization deleted", Toast.LENGTH_SHORT).show();
                        loadOrgs();
                    } else {
                        Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}