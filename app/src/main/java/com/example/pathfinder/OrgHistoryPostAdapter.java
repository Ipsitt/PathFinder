package com.example.pathfinder;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OrgHistoryPostAdapter extends RecyclerView.Adapter<OrgHistoryPostAdapter.ViewHolder> {

    private final Context context;
    private final List<DBHelper.OrgPost> posts;
    private final String orgEmail;

    public OrgHistoryPostAdapter(Context context, List<DBHelper.OrgPost> posts, String orgEmail) {
        this.context = context;
        this.posts = posts;
        this.orgEmail = orgEmail;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history_post_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DBHelper.OrgPost post = posts.get(position);

        holder.tvPostTitle.setText(post.title);
        holder.tvTotalInterns.setText("Total Interns: " + post.recruitedCount);
        holder.tvDescription.setText(post.description != null ? post.description : "");
        holder.tvStipend.setText("₹ " + (post.stipend != null ? post.stipend : ""));
        holder.tvDuration.setText(post.timePeriod != null ? post.timePeriod : "");

        holder.tagChipsContainer.removeAllViews();
        if (post.tags != null) {
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

                android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                gd.setColor(color);
                gd.setCornerRadius(30f);
                chip.setBackground(gd);
                
                double lum = (0.299 * android.graphics.Color.red(color) + 
                              0.587 * android.graphics.Color.green(color) + 
                              0.114 * android.graphics.Color.blue(color)) / 255;
                chip.setTextColor(lum < 0.55 ? android.graphics.Color.WHITE : android.graphics.Color.BLACK);
                
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                lp.setMarginEnd(paddingX);
                chip.setLayoutParams(lp);
                
                holder.tagChipsContainer.addView(chip);
            }
        }

        DBHelper dbHelper = new DBHelper(context);

        if (post.isCompleted) {
            holder.btnMarkComplete.setText("Completed ✓");
            holder.btnMarkComplete.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(context.getColor(R.color.status_success)));
            holder.btnMarkComplete.setEnabled(false);
            
            holder.btnEditPost.setAlpha(0.5f);
            holder.btnEditPost.setEnabled(false);
        } else {
            holder.btnMarkComplete.setText("Mark as Completed");
            holder.btnMarkComplete.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(context.getColor(R.color.status_error)));
            holder.btnMarkComplete.setEnabled(true);
            holder.btnMarkComplete.setOnClickListener(v -> {
                // Certificate validation constraint
                java.util.List<String> recruited = dbHelper.getRecruitedStudentsForPost(post.postId);
                
                if (recruited.isEmpty()) {
                    android.widget.Toast.makeText(context, "Cannot complete: No interns were recruited.", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                
                for (String email : recruited) {
                    byte[] cert = dbHelper.getRecruitmentCertificate(post.postId, email);
                    if (cert == null || cert.length == 0) {
                        android.widget.Toast.makeText(context, "Upload certificates for all recruits before completing.", android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                boolean marked = dbHelper.markPostCompleted(post.postId);
                if (marked) {
                    post.isCompleted = true;
                    notifyItemChanged(position);
                }
            });
            
            if (post.applicantCount > 0) {
                holder.btnEditPost.setAlpha(0.5f);
                holder.btnEditPost.setEnabled(false);
            } else {
                holder.btnEditPost.setAlpha(1.0f);
                holder.btnEditPost.setEnabled(true);
                holder.btnEditPost.setOnClickListener(v -> {
                    Intent intent = new Intent(context, EditPostActivity.class);
                    intent.putExtra("postId", post.postId);
                    intent.putExtra("title", post.title);
                    intent.putExtra("description", post.description);
                    intent.putExtra("stipend", post.stipend);
                    intent.putExtra("timePeriod", post.timePeriod);
                    context.startActivity(intent);
                });
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrgHistoryInternsActivity.class);
            intent.putExtra("email", orgEmail);
            intent.putExtra("postId", post.postId);
            intent.putExtra("postTitle", post.title);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPostTitle, tvTotalInterns, tvStipend, tvDuration, tvDescription;
        LinearLayout tagChipsContainer;
        Button btnMarkComplete, btnEditPost;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPostTitle     = itemView.findViewById(R.id.tvHistoryPostTitle);
            tvTotalInterns  = itemView.findViewById(R.id.tvTotalInterns);
            tvStipend       = itemView.findViewById(R.id.tvStipend);
            tvDuration      = itemView.findViewById(R.id.tvDuration);
            tvDescription   = itemView.findViewById(R.id.tvDescription);
            tagChipsContainer = itemView.findViewById(R.id.tagChipsContainer);
            btnMarkComplete = itemView.findViewById(R.id.btnMarkComplete);
            btnEditPost     = itemView.findViewById(R.id.btnEditPost);
        }
    }
}
