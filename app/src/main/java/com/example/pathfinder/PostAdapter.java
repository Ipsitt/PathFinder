package com.example.pathfinder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    private final Context context;
    private List<Post> posts;
    private final OnPostClickListener listener;

    public PostAdapter(Context context, List<Post> posts, OnPostClickListener listener) {
        this.context  = context;
        this.posts    = posts;
        this.listener = listener;
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

        holder.tvTitle.setText(post.title);
        holder.tvOrgName.setText(post.orgName);
        holder.tvStipend.setText(post.stipend != null && !post.stipend.isEmpty()
                ? post.stipend : "Unpaid");
        holder.tvDuration.setText(post.timePeriod != null && !post.timePeriod.isEmpty()
                ? post.timePeriod : "Duration TBD");
        holder.tvDescription.setText(post.description);

        // Org photo — show from BLOB or fallback placeholder
        if (post.orgImage != null && post.orgImage.length > 0) {
            Bitmap bmp = BitmapFactory.decodeByteArray(post.orgImage, 0, post.orgImage.length);
            holder.imgOrgPhoto.setImageBitmap(bmp);
        } else {
            holder.imgOrgPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Build tag chips
        holder.tagChipsContainer.removeAllViews();
        if (post.tags != null) {
            for (DBHelper.Tag tag : post.tags) {
                holder.tagChipsContainer.addView(makeTagChip(tag));
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPostClick(post);
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

        // Parse the tag color for background; pick white or dark text for contrast
        int bgColor;
        try {
            bgColor = Color.parseColor(tag.color);
        } catch (Exception e) {
            bgColor = Color.parseColor("#94A3B8");
        }
        chip.setTextColor(isColorDark(bgColor) ? Color.WHITE : Color.parseColor("#1E293B"));

        // Rounded pill background using the tag color
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(32f);
        gd.setColor(bgColor);
        chip.setBackground(gd);

        int ph = dpToPx(6);
        int pv = dpToPx(3);
        chip.setPadding(ph, pv, ph, pv);

        return chip;
    }

    /** Returns true if the color is dark enough to warrant white text */
    private boolean isColorDark(int color) {
        double luminance = (0.299 * Color.red(color)
                + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255;
        return luminance < 0.55;
    }

    private int dpToPx(int dp) {
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