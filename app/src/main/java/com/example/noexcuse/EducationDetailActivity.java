package com.example.noexcuse;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.noexcuse.database.AppViewModel;
import com.example.noexcuse.database.EducationTask;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class EducationDetailActivity extends AppCompatActivity {

    public static final String EXTRA_VERIFIED_ID = "VERIFIED_EDU_ID";

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

        // ── Drawer ───────────────────────────────────────────────────────
        DrawerLayout drawerLayout    = findViewById(R.id.drawer_layout);
        android.widget.ImageView btnMenu = findViewById(R.id.btnMenu);
        Button       btnStudyHelper  = findViewById(R.id.btnStudyHelper);
        Switch       swFocusMode     = findViewById(R.id.swFocusMode);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        int eduId = getIntent().getIntExtra("EDU_ID", -1);

        if (eduId == -1) {
            Toast.makeText(this, "Error: session not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        AppViewModel viewModel = new ViewModelProvider(this).get(AppViewModel.class);
        btnBack.setOnClickListener(v -> finish());

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

                // ── Focus Mode Switch — init depuis DB ────────────────────
                swFocusMode.setChecked(edu.isFocusMode);
                swFocusMode.setOnCheckedChangeListener((btn, checked) -> {
                    edu.isFocusMode = checked;
                    viewModel.updateEducation(edu);
                    Toast.makeText(this,
                            checked ? "Focus Mode ON 🎯" : "Focus Mode OFF",
                            Toast.LENGTH_SHORT).show();
                    drawerLayout.closeDrawer(GravityCompat.END);
                });

                // ── Study Helper ──────────────────────────────────────────
                btnStudyHelper.setOnClickListener(v -> {
                    drawerLayout.closeDrawer(GravityCompat.END);
                    Toast.makeText(this, "Study Helper coming soon 🤖", Toast.LENGTH_SHORT).show();
                    // TODO: ouvre StudyHelperActivity ou BottomSheet AI
                });

                // ── Status refresh ────────────────────────────────────────
                Runnable refreshStatus = () -> {
                    if (edu.isDone) {
                        tvStatus.setText("Done");
                        tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                        tvStatus.setBackgroundResource(R.drawable.bg_status_done);
                        btnDone.setEnabled(false);
                        btnDone.setAlpha(0.4f);
                    } else {
                        tvStatus.setText("Pending");
                        tvStatus.setTextColor(Color.parseColor("#F59E0B"));
                        tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                        btnDone.setEnabled(true);
                        btnDone.setAlpha(1f);
                    }
                };
                refreshStatus.run();

                // ── Mark as Done ──────────────────────────────────────────
                btnDone.setOnClickListener(v -> {
                    edu.isDone = true;
                    viewModel.updateEducation(edu);
                    refreshStatus.run();

                    Intent result = new Intent();
                    result.putExtra(EXTRA_VERIFIED_ID, edu.id);
                    setResult(Activity.RESULT_OK, result);
                });

                // ── Delete ────────────────────────────────────────────────
                btnDelete.setOnClickListener(v -> {
                    viewModel.deleteEducation(edu);
                    contentLayout.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .postDelayed(this::finish, 800);
                });
            });
        });
    }
}