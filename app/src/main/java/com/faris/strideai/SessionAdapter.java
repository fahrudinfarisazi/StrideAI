package com.faris.strideai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.faris.strideai.models.ActivitySession;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    private List<ActivitySession> sessionList;

    public SessionAdapter(List<ActivitySession> sessionList) {
        this.sessionList = sessionList;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        ActivitySession session = sessionList.get(position);
        
        holder.tvSessionTitle.setText(session.getTitle());
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.US);
        String dateStr = sdf.format(new Date(session.getTimestamp())).toUpperCase();
        holder.tvSessionDate.setText(dateStr);
        
        holder.tvSessionDistance.setText(String.format(Locale.US, "%.2f", session.getDistanceKm()));
        holder.tvSessionSteps.setText(String.format(Locale.US, "%,d", session.getSteps()));
        holder.tvSessionKcal.setText(String.valueOf(session.getCalories()));
        
        holder.btnShareSession.setOnClickListener(v -> {
            String shareText = String.format(Locale.US, 
                "Check out my %s on StrideAI!\nDistance: %.2f km\nSteps: %,d\nCalories: %d kcal\nDate: %s",
                session.getTitle(), session.getDistanceKm(), session.getSteps(), session.getCalories(), dateStr);
                
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My StrideAI Session");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            v.getContext().startActivity(Intent.createChooser(shareIntent, "Share session via"));
        });

        if (session.getMapSnapshotPath() != null) {
            java.io.File imgFile = new java.io.File(session.getMapSnapshotPath());
            if (imgFile.exists()) {
                holder.imgSessionMap.setImageURI(android.net.Uri.parse(session.getMapSnapshotPath()));
                holder.imgSessionMap.setImageTintList(null);
                holder.imgSessionMap.setPadding(0, 0, 0, 0);
                holder.imgSessionMap.setAlpha(1.0f);
            } else {
                setDefaultMapPlaceholder(holder);
            }
        } else {
            setDefaultMapPlaceholder(holder);
        }
    }

    private void setDefaultMapPlaceholder(SessionViewHolder holder) {
        holder.imgSessionMap.setImageResource(R.drawable.ic_intensity_graph);
        holder.imgSessionMap.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00D4FF")));
        int paddingDp = (int) (24 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
        holder.imgSessionMap.setPadding(paddingDp, paddingDp, paddingDp, paddingDp);
        holder.imgSessionMap.setAlpha(0.8f);
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    public static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView tvSessionTitle, tvSessionDate, tvSessionDistance, tvSessionSteps, tvSessionKcal;
        ImageView btnShareSession, imgSessionMap;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSessionTitle = itemView.findViewById(R.id.tvSessionTitle);
            tvSessionDate = itemView.findViewById(R.id.tvSessionDate);
            tvSessionDistance = itemView.findViewById(R.id.tvSessionDistance);
            tvSessionSteps = itemView.findViewById(R.id.tvSessionSteps);
            tvSessionKcal = itemView.findViewById(R.id.tvSessionKcal);
            btnShareSession = itemView.findViewById(R.id.btnShareSession);
            imgSessionMap = itemView.findViewById(R.id.imgSessionMap);
        }
    }
}
