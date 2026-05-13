package com.example.noexcuse;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.noexcuse.database.AppViewModel;
import com.google.android.material.button.MaterialButton;

public class GymDetailActivity extends AppCompatActivity {

    private static final int REQ_WORKOUT = 200;

    private AppViewModel viewModel;
    private TextView     tvStatus;
    private int          planId;
    private String       bodyPart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym_detail);

        FrameLayout    btnBack      = findViewById(R.id.btnBack);
        TextView       tvBodyPart   = findViewById(R.id.tvBodyPart);
        TextView       tvStartTime  = findViewById(R.id.tvStartTime);
        tvStatus                    = findViewById(R.id.tvStatus);
        TextView       tvExCount    = findViewById(R.id.tvExCount);
        MaterialButton btnLetsGo    = findViewById(R.id.btnLetsGo);
        MaterialButton btnHistory   = findViewById(R.id.btnHistory);   // ⭐ boutton jdid
        DrawerLayout   drawerLayout = findViewById(R.id.drawer_layout);
        ImageView      btnMenu      = findViewById(R.id.btnMenu);
        MaterialButton btnEditPlan  = findViewById(R.id.btnEditPlan);
        MaterialButton btnWeekPlan  = findViewById(R.id.btnWeekPlan);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        btnBack.setOnClickListener(v -> finish());
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        planId   = getIntent().getIntExtra("PLAN_ID", -1);
        bodyPart = getIntent().getStringExtra("PLAN_BODY_PART");
        String startTime = getIntent().getStringExtra("PLAN_START_TIME");

        if (planId == -1) { finish(); return; }

        tvBodyPart.setText(bodyPart != null ? bodyPart : "Gym");
        tvStartTime.setText(startTime != null && !startTime.isEmpty() ? startTime : "--:--");
        tvStatus.setText("Pending");

        viewModel.getExercisesForPlan(planId).observe(this, exercises -> {
            int count = exercises != null ? exercises.size() : 0;
            tvExCount.setText(count + " exercise" + (count > 1 ? "s" : ""));
        });

        // ── Let's Go ──────────────────────────────────────────────────────
        btnLetsGo.setOnClickListener(v -> {
            Intent intent = new Intent(this, ActiveWorkoutActivity.class);
            intent.putExtra("PLAN_ID",        planId);
            intent.putExtra("PLAN_BODY_PART", bodyPart);
            startActivityForResult(intent, REQ_WORKOUT);
        });

        // ── View History ──────────────────────────────────────────────────
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkoutHistoryActivity.class);
            intent.putExtra("PLAN_ID",        planId);
            intent.putExtra("PLAN_BODY_PART", bodyPart);
            startActivity(intent);
        });

        // ── Drawer buttons ────────────────────────────────────────────────
        btnEditPlan.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            getSharedPreferences("gym_prefs", MODE_PRIVATE)
                    .edit().remove("gym_plan_week").apply();
            startActivity(new Intent(this, GymSetupActivity.class));
            finish();
        });

        btnWeekPlan.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            startActivity(new Intent(this, WeekPlanActivity.class));
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_WORKOUT && resultCode == RESULT_OK) {
            tvStatus.setText("Done ✓");
            tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            tvStatus.setBackgroundResource(R.drawable.bg_status_done);
        }
    }
}