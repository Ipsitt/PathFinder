package com.example.pathfinder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Internship history cards for organization posts.

public class OrgHistoryPostAdapter extends RecyclerView.Adapter<OrgHistoryPostAdapter.ViewHolder> {

    private final Context context;
    private final List<DBHelper.OrgPost> posts;
    private final String orgEmail;
    private final DBHelper dbHelper;

    // Creates the adapter for organization history posts.
    public OrgHistoryPostAdapter(Context context, List<DBHelper.OrgPost> posts, String orgEmail) {
        this.context = context;
        this.posts = posts;
        this.orgEmail = orgEmail;
        this.dbHelper = new DBHelper(context);
    }

    // Inflates a history card for an organization post.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history_post_card, parent, false);
        return new ViewHolder(view);
    }

    // Loads a post into the history card UI.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DBHelper.OrgPost post = posts.get(position);

        holder.tvPostTitle.setText(post.title);
        holder.tvTotalInterns.setText("Total Interns: " + post.recruitedCount);
        holder.tvDescription.setText(post.description != null ? post.description : "");
        holder.tvStipend.setText((post.stipend != null && !post.stipend.isEmpty()) ? "Rs. " + post.stipend : "Unpaid");
        holder.tvDuration.setText(post.timePeriod != null ? post.timePeriod : "");

        renderTagChips(holder, post);
        bindPostActions(holder, post, position);

        holder.btnDeletePost.setOnClickListener(v -> showDeleteConfirmation(post));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrgHistoryInternsActivity.class);
            intent.putExtra("email", orgEmail);
            intent.putExtra("postId", post.postId);
            intent.putExtra("postTitle", post.title);
            context.startActivity(intent);
        });
    }

    // Draws colored tag chips for a history card.
    private void renderTagChips(@NonNull ViewHolder holder, DBHelper.OrgPost post) {
        holder.tagChipsContainer.removeAllViews();
        if (post.tags == null) {
            return;
        }

        for (DBHelper.Tag tag : post.tags) {
            TextView chip = new TextView(context);
            int paddingX = Math.round(10 * context.getResources().getDisplayMetrics().density);
            int paddingY = Math.round(4 * context.getResources().getDisplayMetrics().density);
            chip.setPadding(paddingX, paddingY, paddingX, paddingY);
            chip.setText(tag.label);
            chip.setTextSize(11f);

            int color;
            try {
                color = android.graphics.Color.parseColor(tag.color);
            } catch (Exception e) {
                color = android.graphics.Color.GRAY;
            }

            android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
            background.setColor(color);
            background.setCornerRadius(30f);
            chip.setBackground(background);

            double luminance = (0.299 * android.graphics.Color.red(color)
                    + 0.587 * android.graphics.Color.green(color)
                    + 0.114 * android.graphics.Color.blue(color)) / 255;
            chip.setTextColor(luminance < 0.55 ? android.graphics.Color.WHITE : android.graphics.Color.BLACK);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMarginEnd(paddingX);
            chip.setLayoutParams(layoutParams);

            holder.tagChipsContainer.addView(chip);
        }
    }

    // Sets up the edit and completion actions for a history card.
    private void bindPostActions(@NonNull ViewHolder holder, DBHelper.OrgPost post, int position) {
        if (post.isCompleted) {
            holder.btnMarkComplete.setText("Completed");
            holder.btnMarkComplete.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(context.getColor(R.color.status_success)));
            holder.btnMarkComplete.setEnabled(false);
            holder.btnEditPost.setAlpha(0.5f);
            holder.btnEditPost.setEnabled(false);
            holder.btnEditPost.setOnClickListener(null);
            return;
        }

        holder.btnMarkComplete.setText("Mark as Completed");
        holder.btnMarkComplete.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(context.getColor(R.color.status_error)));
        holder.btnMarkComplete.setEnabled(true);
        holder.btnMarkComplete.setOnClickListener(v -> markPostAsCompleted(post, position));

        if (post.applicantCount > 0) {
            holder.btnEditPost.setAlpha(0.5f);
            holder.btnEditPost.setEnabled(false);
            holder.btnEditPost.setOnClickListener(null);
        } else {
            holder.btnEditPost.setAlpha(1.0f);
            holder.btnEditPost.setEnabled(true);
            holder.btnEditPost.setOnClickListener(v -> openEditScreen(post));
        }
    }

    // Marks a post as completed after certificates are verified.
    private void markPostAsCompleted(DBHelper.OrgPost post, int position) {
        List<String> recruited = dbHelper.getRecruitedStudentsForPost(post.postId);

        if (recruited.isEmpty()) {
            Toast.makeText(context, "Cannot complete: No interns were recruited.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (String email : recruited) {
            byte[] cert = dbHelper.getRecruitmentCertificate(post.postId, email);
            if (cert == null || cert.length == 0) {
                Toast.makeText(context, "Upload certificates for all recruits before completing.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        boolean marked = dbHelper.markPostCompleted(post.postId);
        if (marked) {
            post.isCompleted = true;
            notifyItemChanged(position);
        }
    }

    // Opens the edit screen for the selected post.
    private void openEditScreen(DBHelper.OrgPost post) {
        Intent intent = new Intent(context, OrgEditPostActivity.class);
        intent.putExtra("postId", post.postId);
        intent.putExtra("title", post.title);
        intent.putExtra("description", post.description);
        intent.putExtra("stipend", post.stipend);
        intent.putExtra("timePeriod", post.timePeriod);
        context.startActivity(intent);
    }

    // Shows a confirmation dialog before removing a post.
    private void showDeleteConfirmation(DBHelper.OrgPost post) {
        if (hasHiredInterns(post)) {
            Toast.makeText(context, "Posts with hired interns cannot be deleted.", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle("Delete post?")
                .setMessage("Are you sure you want to delete " + post.title + "?")
                .setPositiveButton("Delete", (dialog, which) -> deletePost(post))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Deletes the selected post and removes it from the list.
    private void deletePost(DBHelper.OrgPost post) {
        int adapterPosition = posts.indexOf(post);
        if (adapterPosition == -1) {
            Toast.makeText(context, "Unable to find this post right now.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (hasHiredInterns(post)) {
            Toast.makeText(context, "Posts with hired interns cannot be deleted.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!dbHelper.deletePost(post.postId)) {
            Toast.makeText(context, "Post could not be deleted.", Toast.LENGTH_SHORT).show();
            return;
        }

        posts.remove(adapterPosition);
        notifyItemRemoved(adapterPosition);
        Toast.makeText(context, "Post deleted.", Toast.LENGTH_SHORT).show();
    }

    // Checks whether a post already has hired interns.
    private boolean hasHiredInterns(DBHelper.OrgPost post) {
        List<String> hiredInterns = dbHelper.getRecruitedStudentsForPost(post.postId);
        post.recruitedCount = hiredInterns.size();
        return !hiredInterns.isEmpty();
    }

    // Returns the number of history cards in the list.
    @Override
    public int getItemCount() {
        return posts.size();
    }

    // Holds the views for a single history card.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPostTitle, tvTotalInterns, tvStipend, tvDuration, tvDescription;
        LinearLayout tagChipsContainer;
        Button btnMarkComplete, btnEditPost;
        ImageButton btnDeletePost;

        // Caches the views used on a history post card.
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPostTitle = itemView.findViewById(R.id.tvHistoryPostTitle);
            tvTotalInterns = itemView.findViewById(R.id.tvTotalInterns);
            tvStipend = itemView.findViewById(R.id.tvStipend);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tagChipsContainer = itemView.findViewById(R.id.tagChipsContainer);
            btnMarkComplete = itemView.findViewById(R.id.btnMarkComplete);
            btnEditPost = itemView.findViewById(R.id.btnEditPost);
            btnDeletePost = itemView.findViewById(R.id.btnDeletePost);
        }
    }
}
