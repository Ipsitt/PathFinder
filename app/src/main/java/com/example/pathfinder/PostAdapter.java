package com.example.pathfinder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    private final Context context;
    private List<Post> posts;
    private final OnPostClickListener listener;
    private final String studentEmail; // passed so detail screen knows who's viewing

    /**
     * When true the adapter is being used inside StudentAppliedActivity.
     * In this mode clicking the card still opens PostDetailActivity (read-only),
     * and each card shows the "Accepted" button if the student was recruited.
     */
    private final boolean appliedMode;

    /** Recruitment map: postId → orgEmail  (only populated in appliedMode) */
    private final SparseArray<String> recruitmentMap = new SparseArray<>();

    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ── Constructor used by StudentHomeActivity (normal browse mode) ───────
    public PostAdapter(Context context, List<Post> posts,
                       String studentEmail, OnPostClickListener listener) {
        this(context, posts, studentEmail, listener, false, null);
    }

    // ── Full constructor ───────────────────────────────────────────────────
    public PostAdapter(Context context, List<Post> posts,
                       String studentEmail, OnPostClickListener listener,
                       boolean appliedMode,
                       List<DBHelper.RecruitmentEntry> recruitments) {
        this.context      = context;
        this.posts        = posts;
        this.studentEmail = studentEmail;
        this.listener     = listener;
        this.appliedMode  = appliedMode;

        if (recruitments != null) {
            for (DBHelper.RecruitmentEntry e : recruitments) {
                recruitmentMap.put(e.postId, e.orgEmail);
            }
        }
    }

    public void updatePosts(List<Post> newPosts) {
        this.posts = newPosts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_post_card, parent, false);
        return new PostViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);

        holder.tvTitle.setText(post.title != null ? post.title : "");
        holder.tvOrgName.setText(post.orgName != null ? post.orgName : "");

        // ── Stipend with "Rs. " prefix ──
        String stipend = (post.stipend != null && !post.stipend.isEmpty())
                ? "Rs. " + post.stipend : "Unpaid";
        holder.tvStipend.setText(stipend);

        holder.tvDuration.setText(
                (post.timePeriod != null && !post.timePeriod.isEmpty())
                        ? post.timePeriod : "Duration TBD");
        holder.tvDescription.setText(post.description != null ? post.description : "");

        // ── Async image load ──
        final String tagKey = "post_" + post.id;
        holder.imgOrgPhoto.setTag(tagKey);
        holder.imgOrgPhoto.setImageResource(android.R.drawable.ic_menu_agenda);

        if (post.orgImage != null && post.orgImage.length > 0) {
            final byte[] bytes = post.orgImage;
            imageExecutor.execute(() -> {
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                mainHandler.post(() -> {
                    if (bmp != null && tagKey.equals(holder.imgOrgPhoto.getTag())) {
                        holder.imgOrgPhoto.setImageBitmap(bmp);
                    }
                });
            });
        }

        // ── Tag chips ──
        holder.tagChipsContainer.removeAllViews();
        if (post.tags != null) {
            for (DBHelper.Tag tag : post.tags) {
                holder.tagChipsContainer.addView(makeTagChip(tag));
            }
        }

        // ── "Accepted" banner (applied-mode only) ──
        if (appliedMode && holder.btnAccepted != null) {
            String orgEmailForPost = recruitmentMap.get(post.id);
            if (orgEmailForPost != null) {
                // Student was recruited for this post
                holder.btnAccepted.setVisibility(View.VISIBLE);
                holder.btnAccepted.setAllCaps(false);
                final String orgMail = orgEmailForPost;
                final String orgName = (post.orgName != null) ? post.orgName : "the recruiter";
                holder.btnAccepted.setOnClickListener(v -> {
                    new AlertDialog.Builder(context)
                            .setTitle("🎉 Congratulations!")
                            .setMessage("You have been accepted by the recruiter.\n\nSend an email to:\n" + orgMail + "\n\nto communicate further!")
                            .setPositiveButton("Got it!", (dialog, which) -> dialog.dismiss())
                            .setCancelable(true)
                            .show();
                });
            } else {
                holder.btnAccepted.setVisibility(View.GONE);
            }
        } else if (holder.btnAccepted != null) {
            holder.btnAccepted.setVisibility(View.GONE);
        }

        // ── Click → open PostDetailActivity ──
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("post_id",       post.id);
            intent.putExtra("student_email", studentEmail);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return posts == null ? 0 : posts.size();
    }

    private View makeTagChip(DBHelper.Tag tag) {
        TextView chip = new TextView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(6);
        chip.setLayoutParams(lp);
        chip.setText(tag.label);
        chip.setTextSize(11f);

        int bgColor;
        try { bgColor = Color.parseColor(tag.color); }
        catch (Exception e) { bgColor = context.getColor(R.color.text_secondary); }

        chip.setTextColor(isColorDark(bgColor) ? Color.WHITE : context.getColor(R.color.text_primary));
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(99f);
        gd.setColor(bgColor);
        chip.setBackground(gd);
        chip.setPadding(dp(8), dp(3), dp(8), dp(3));
        return chip;
    }

    private boolean isColorDark(int color) {
        double lum = (0.299 * Color.red(color)
                + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255;
        return lum < 0.55;
    }

    private int dp(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imgOrgPhoto;
        TextView tvTitle, tvOrgName, tvStipend, tvDuration, tvDescription;
        LinearLayout tagChipsContainer;
        Button btnAccepted;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgOrgPhoto       = itemView.findViewById(R.id.imgOrgPhoto);
            tvTitle           = itemView.findViewById(R.id.tvPostTitle);
            tvOrgName         = itemView.findViewById(R.id.tvOrgName);
            tvStipend         = itemView.findViewById(R.id.tvStipend);
            tvDuration        = itemView.findViewById(R.id.tvDuration);
            tvDescription     = itemView.findViewById(R.id.tvDescription);
            tagChipsContainer = itemView.findViewById(R.id.tagChipsContainer);
            btnAccepted       = itemView.findViewById(R.id.btnAccepted);
        }
    }
}