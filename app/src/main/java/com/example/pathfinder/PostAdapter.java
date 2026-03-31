package com.example.pathfinder;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PostAdapter(Context context, List<Post> posts,
                       String studentEmail, OnPostClickListener listener) {
        this.context      = context;
        this.posts        = posts;
        this.studentEmail = studentEmail;
        this.listener     = listener;
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
        catch (Exception e) { bgColor = Color.parseColor("#94A3B8"); }

        chip.setTextColor(isColorDark(bgColor) ? Color.WHITE : Color.parseColor("#1E293B"));
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

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgOrgPhoto       = itemView.findViewById(R.id.imgOrgPhoto);
            tvTitle           = itemView.findViewById(R.id.tvPostTitle);
            tvOrgName         = itemView.findViewById(R.id.tvOrgName);
            tvStipend         = itemView.findViewById(R.id.tvStipend);
            tvDuration        = itemView.findViewById(R.id.tvDuration);
            tvDescription     = itemView.findViewById(R.id.tvDescription);
            tagChipsContainer = itemView.findViewById(R.id.tagChipsContainer);
        }
    }
}