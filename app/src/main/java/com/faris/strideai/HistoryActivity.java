package com.faris.strideai;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.faris.strideai.models.ActivitySession;
import com.faris.strideai.utils.SessionManager;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarHistory), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top + 24, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        List<ActivitySession> sessions = SessionManager.getSessions(this);
        
        TextView tvTotalSteps = findViewById(R.id.tvTotalSteps);
        RecyclerView rvRecentSessions = findViewById(R.id.rvRecentSessions);
        TextView tvEmptyState = findViewById(R.id.tvEmptyState);
        View btnLoadMore = findViewById(R.id.btnLoadMore);
        LinearLayout chartBarsLayout = findViewById(R.id.chartBarsLayout);

        int totalSteps = 0;
        for (ActivitySession session : sessions) {
            totalSteps += session.getSteps();
        }
        tvTotalSteps.setText(String.format("%,d", totalSteps));

        if (sessions.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvRecentSessions.setVisibility(View.GONE);
            btnLoadMore.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvRecentSessions.setVisibility(View.VISIBLE);
            btnLoadMore.setVisibility(View.VISIBLE);
            
            SessionAdapter adapter = new SessionAdapter(sessions);
            rvRecentSessions.setLayoutManager(new LinearLayoutManager(this));
            rvRecentSessions.setAdapter(adapter);
        }
        
        // Setup empty chart bars since user wants it completely empty initially
        setupEmptyChart(chartBarsLayout);
    }
    
    private void setupEmptyChart(LinearLayout chartBarsLayout) {
        chartBarsLayout.removeAllViews();
        // 7 days
        for (int i=0; i<7; i++) {
            View barContainer = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 0, 1.0f);
            params.setMargins(8, 0, 8, 0);
            barContainer.setLayoutParams(params);
            barContainer.setBackgroundColor(android.graphics.Color.parseColor("#263238")); // Dark grey placeholder
            chartBarsLayout.addView(barContainer);
        }
    }
}
