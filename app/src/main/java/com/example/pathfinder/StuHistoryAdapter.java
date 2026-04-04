package com.example.pathfinder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Adapter for internship history cards on the student history screen.

public class StuHistoryAdapter extends RecyclerView.Adapter<StuHistoryAdapter.ViewHolder> {

    public interface DownloadListener {
        // Handles certificate downloads.
        void onDownload(Bitmap bitmap, String postTitle);
    }

    private final Context context;
    private final List<StuPost> posts;
    private final String studentEmail;
    private final DownloadListener downloadListener;
    private final DBHelper dbHelper;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Creates the adapter for student history cards.
    public StuHistoryAdapter(Context context, List<StuPost> posts, String studentEmail, DownloadListener downloadListener) {
        this.context = context;
        this.posts = posts;
        this.studentEmail = studentEmail;
        this.downloadListener = downloadListener;
        this.dbHelper = new DBHelper(context);
    }

    // Inflates a history card.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_stu_history_card, parent, false);
        return new ViewHolder(v);
    }

    // Binds internship history data to a card.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StuPost post = posts.get(position);

        holder.tvPostTitle.setText(post.title != null ? post.title : "");
        holder.tvOrgName.setText(post.orgName != null ? post.orgName : "");
        holder.tvOrgEmail.setText(post.orgEmail != null ? post.orgEmail : "");
        holder.tvStipend.setText((post.stipend != null && !post.stipend.isEmpty()) ? "Rs. " + post.stipend : "Unpaid");
        holder.tvDuration.setText(post.timePeriod != null && !post.timePeriod.isEmpty() ? post.timePeriod : "Duration TBD");
        holder.tvDescription.setText(post.description != null ? post.description : "");

        final String tagKey = "hist_" + post.id;
        holder.imgOrgPhoto.setTag(tagKey);
        holder.imgOrgPhoto.setImageResource(android.R.drawable.ic_menu_gallery);

        if (post.orgImage != null && post.orgImage.length > 0) {
            final byte[] bytes = post.orgImage;
            executor.execute(() -> {
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                handler.post(() -> {
                    if (bmp != null && tagKey.equals(holder.imgOrgPhoto.getTag())) {
                        holder.imgOrgPhoto.setImageBitmap(bmp);
                    }
                });
            });
        }

        // Fetch certificate
        final String certTagKey = "cert_" + post.id;
        holder.imgCertificate.setTag(certTagKey);
        holder.imgCertificate.setImageResource(android.R.drawable.ic_dialog_info);
        holder.btnDownload.setEnabled(false);
        holder.btnDownload.setText("Loading...");

        executor.execute(() -> {
            byte[] certBytes = dbHelper.getRecruitmentCertificate(post.id, studentEmail);
            handler.post(() -> {
                if (!certTagKey.equals(holder.imgCertificate.getTag())) return;

                if (certBytes != null && certBytes.length > 0) {
                    Bitmap bmp = BitmapFactory.decodeByteArray(certBytes, 0, certBytes.length);
                    holder.imgCertificate.setImageBitmap(bmp);
                    holder.btnDownload.setText("Download Certificate");
                    holder.btnDownload.setEnabled(true);
                    holder.btnDownload.setBackgroundTintList(android.content.res.ColorStateList.valueOf(context.getColor(R.color.primary_accent)));
                    holder.btnDownload.setOnClickListener(v -> downloadListener.onDownload(bmp, post.title));
                } else {
                    holder.imgCertificate.setImageResource(android.R.drawable.ic_dialog_info);
                    holder.btnDownload.setText("No Certificate Available");
                    holder.btnDownload.setEnabled(false);
                    holder.btnDownload.setBackgroundTintList(android.content.res.ColorStateList.valueOf(context.getColor(R.color.text_secondary)));
                }
            });
        });
    }

    // Returns the number of history items.
    @Override
    public int getItemCount() {
        return posts == null ? 0 : posts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgOrgPhoto, imgCertificate;
        TextView tvPostTitle, tvOrgName, tvOrgEmail, tvStipend, tvDuration, tvDescription;
        Button btnDownload;

        // Caches views for a history card.
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgOrgPhoto    = itemView.findViewById(R.id.imgOrgPhoto);
            tvPostTitle    = itemView.findViewById(R.id.tvPostTitle);
            tvOrgName      = itemView.findViewById(R.id.tvOrgName);
            tvOrgEmail     = itemView.findViewById(R.id.tvOrgEmail);
            tvStipend      = itemView.findViewById(R.id.tvStipend);
            tvDuration     = itemView.findViewById(R.id.tvDuration);
            tvDescription  = itemView.findViewById(R.id.tvDescription);
            imgCertificate = itemView.findViewById(R.id.imgCertificate);
            btnDownload    = itemView.findViewById(R.id.btnDownload);
        }
    }
}
