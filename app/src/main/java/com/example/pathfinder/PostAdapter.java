package com.example.pathfinder;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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
import android.widget.Toast;

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
    private final String studentEmail;
    private final DBHelper dbHelper;

    /**
     * When true the adapter is being used inside StudentAppliedActivity.
     * In this mode clicking the card still opens PostDetailActivity (read-only),
     * and each card shows the "Accepted" button if the student was recruited.
     */
    private final boolean appliedMode;

    /** Recruitment map: postId -> orgEmail (only populated in appliedMode). */
    private final SparseArray<String> recruitmentMap = new SparseArray<>();

    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PostAdapter(Context context, List<Post> posts,
                       String studentEmail, OnPostClickListener listener) {
        this(context, posts, studentEmail, listener, false, null);
    }

    public PostAdapter(Context context, List<Post> posts,
                       String studentEmail, OnPostClickListener listener,
                       boolean appliedMode,
                       List<DBHelper.RecruitmentEntry> recruitments) {
        this.context = context;
        this.posts = posts;
        this.studentEmail = studentEmail;
        this.listener = listener;
        this.appliedMode = appliedMode;
        this.dbHelper = new DBHelper(context);

        if (recruitments != null) {
            for (DBHelper.RecruitmentEntry entry : recruitments) {
                recruitmentMap.put(entry.postId, entry.orgEmail);
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_card, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);

        holder.tvTitle.setText(post.title != null ? post.title : "");
        holder.tvOrgName.setText(post.orgName != null ? post.orgName : "");

        String stipend = (post.stipend != null && !post.stipend.isEmpty())
                ? "Rs. " + post.stipend : "Unpaid";
        holder.tvStipend.setText(stipend);

        holder.tvDuration.setText(
                (post.timePeriod != null && !post.timePeriod.isEmpty())
                        ? post.timePeriod : "Duration TBD");
        holder.tvDescription.setText(post.description != null ? post.description : "");

        final String tagKey = "post_" + post.id;
        holder.imgOrgPhoto.setTag(tagKey);
        holder.imgOrgPhoto.setImageResource(android.R.drawable.ic_menu_agenda);

        if (post.orgImage != null && post.orgImage.length > 0) {
            final byte[] bytes = post.orgImage;
            imageExecutor.execute(() -> {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                mainHandler.post(() -> {
                    if (bitmap != null && tagKey.equals(holder.imgOrgPhoto.getTag())) {
                        holder.imgOrgPhoto.setImageBitmap(bitmap);
                    }
                });
            });
        }

        holder.tagChipsContainer.removeAllViews();
        if (post.tags != null) {
            for (DBHelper.Tag tag : post.tags) {
                holder.tagChipsContainer.addView(makeTagChip(tag));
            }
        }

        if (appliedMode && holder.btnAccepted != null) {
            String orgEmailForPost = recruitmentMap.get(post.id);
            if (orgEmailForPost != null) {
                holder.btnAccepted.setVisibility(View.VISIBLE);
                holder.btnAccepted.setAllCaps(false);

                final String orgMail = orgEmailForPost;
                final String orgName = (post.orgName != null) ? post.orgName : "the recruiter";

                holder.btnAccepted.setOnClickListener(v -> {
                    Toast.makeText(context,
                            "Accepted by " + orgName + ". Send an email to "
                                    + orgMail + " to communicate further!",
                            Toast.LENGTH_SHORT).show();

                    String studentName = studentEmail;
                    if (studentEmail != null && !studentEmail.isEmpty()) {
                        DBHelper.StudentProfile profile = dbHelper.getStudentProfile(studentEmail);
                        if (profile != null && profile.name != null && !profile.name.trim().isEmpty()) {
                            studentName = profile.name.trim();
                        }
                    }

                    String body = "Dear Sir/Madam,\n\n"
                            + "Thank you for offering me the opportunity to join "
                            + orgName
                            + " as an intern. I am pleased to accept the offer and look forward to gaining valuable experience with your organization.\n\n"
                            + "Please let me know if there are any formalities or documents I need to complete before joining.\n\n"
                            + "Thank you once again for this opportunity.\n\n"
                            + "Sincerely,\n"
                            + studentName;

                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("message/rfc822");
                    emailIntent.setPackage("com.google.android.gm");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{orgMail});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT,
                            "Internship Acceptance Confirmation");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, body);

                    mainHandler.postDelayed(() -> {
                        try {
                            context.startActivity(emailIntent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(context,
                                    "Gmail is not installed on this device.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }, 3000);
                });
            } else {
                holder.btnAccepted.setVisibility(View.GONE);
                holder.btnAccepted.setOnClickListener(null);
            }
        } else if (holder.btnAccepted != null) {
            holder.btnAccepted.setVisibility(View.GONE);
            holder.btnAccepted.setOnClickListener(null);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostClick(post);
                return;
            }

            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("post_id", post.id);
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(6);
        chip.setLayoutParams(params);
        chip.setText(tag.label);
        chip.setTextSize(11f);

        int bgColor;
        try {
            bgColor = Color.parseColor(tag.color);
        } catch (Exception e) {
            bgColor = context.getColor(R.color.text_secondary);
        }

        chip.setTextColor(
                isColorDark(bgColor) ? Color.WHITE : context.getColor(R.color.text_primary));

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(99f);
        bg.setColor(bgColor);
        chip.setBackground(bg);
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
        TextView tvTitle;
        TextView tvOrgName;
        TextView tvStipend;
        TextView tvDuration;
        TextView tvDescription;
        LinearLayout tagChipsContainer;
        Button btnAccepted;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgOrgPhoto = itemView.findViewById(R.id.imgOrgPhoto);
            tvTitle = itemView.findViewById(R.id.tvPostTitle);
            tvOrgName = itemView.findViewById(R.id.tvOrgName);
            tvStipend = itemView.findViewById(R.id.tvStipend);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tagChipsContainer = itemView.findViewById(R.id.tagChipsContainer);
            btnAccepted = itemView.findViewById(R.id.btnAccepted);
        }
    }
}
