package com.example.noexcuse;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.noexcuse.database.AppViewModel;
import com.example.noexcuse.database.EducationTask;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class EducationDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_education_detail);

        TextView       tvTitle       = findViewById(R.id.tvTitle);
        TextView       tvDesc        = findViewById(R.id.tvDesc);
        TextView       tvStartTime   = findViewById(R.id.tvStartTime);
        TextView       tvEndTime     = findViewById(R.id.tvEndTime);
        TextView       tvStatus      = findViewById(R.id.tvStatus);
        FrameLayout    btnBack       = findViewById(R.id.btnBack);
        MaterialButton btnDone       = findViewById(R.id.btnDone);
        MaterialButton btnDelete     = findViewById(R.id.btnDelete);
        ProgressBar    progressBar   = findViewById(R.id.progressBar);
        LinearLayout   contentLayout = findViewById(R.id.contentLayout);

        // Njibu bas l'id - data kamilat tji mn DB
        int eduId = getIntent().getIntExtra("EDU_ID", -1);

        if (eduId == -1) {
            Toast.makeText(this, "Error: session not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        AppViewModel viewModel = new ViewModelProvider(this).get(AppViewModel.class);
        btnBack.setOnClickListener(v -> finish());

        // ── Njib EducationTask mn DB b id ────────────────────────────────
        Executors.newSingleThreadExecutor().execute(() -> {
            EducationTask edu = viewModel.getEducationById(eduId);

            runOnUiThread(() -> {
                if (edu == null) {
                    Toast.makeText(this, "Session not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

                tvTitle.setText(edu.moduleName);
                tvDesc.setText(edu.studyPlan != null && !edu.studyPlan.isEmpty()
                        ? edu.studyPlan : "No study plan provided");
                tvStartTime.setText(sdf.format(new Date(edu.startTime)));
                tvEndTime.setText(sdf.format(new Date(edu.endTime)));

                // Status badge
                if (edu.isDone) {
                    tvStatus.setText("Done");
                    tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_done);
                } else {
                    tvStatus.setText("Pending");
                    tvStatus.setTextColor(Color.parseColor("#F59E0B"));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                }

                // ── Mark as Done ─────────────────────────────────────────────
                btnDone.setOnClickListener(v -> {
                    edu.isDone = true;
                    viewModel.updateEducation(edu);   // update f education_tasks direkt
                    contentLayout.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 800);
                });

                // ── Delete ───────────────────────────────────────────────────
                btnDelete.setOnClickListener(v -> {
                    viewModel.deleteEducation(edu);   // delete mn education_tasks direkt
                    contentLayout.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 800);
                });
            });
        });
    }
}