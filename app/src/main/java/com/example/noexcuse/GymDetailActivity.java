package com.example.noexcuse;

import android.content.Intent;
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

    private AppViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym_detail);

        // ── Views ────────────────────────────────────────────────────────────────
        FrameLayout    btnBack     = findViewById(R.id.btnBack);
        TextView       tvBodyPart  = findViewById(R.id.tvBodyPart);
        TextView       tvStartTime = findViewById(R.id.tvStartTime);
        TextView       tvStatus    = findViewById(R.id.tvStatus);
        TextView       tvExCount   = findViewById(R.id.tvExCount);
        MaterialButton btnLetsGo   = findViewById(R.id.btnLetsGo);
        DrawerLayout   drawerLayout = findViewById(R.id.drawer_layout);
        ImageView      btnMenu     = findViewById(R.id.btnMenu);
        MaterialButton btnEditPlan = findViewById(R.id.btnEditPlan);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        btnBack.setOnClickListener(v -> finish());
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        // ── Intent data ──────────────────────────────────────────────────────────
        int    planId    = getIntent().getIntExtra("PLAN_ID", -1);
        String bodyPart  = getIntent().getStringExtra("PLAN_BODY_PART");
        String startTime = getIntent().getStringExtra("PLAN_START_TIME");

        if (planId == -1) { finish(); return; }

        tvBodyPart.setText(bodyPart != null ? bodyPart : "Gym");
        tvStartTime.setText(startTime != null && !startTime.isEmpty() ? startTime : "--:--");
        tvStatus.setText("Pending");

        // ── Nbr exercises via LiveData ───────────────────────────────────────────
        viewModel.getExercisesForPlan(planId).observe(this, exercises -> {
            int count = exercises != null ? exercises.size() : 0;
            tvExCount.setText(count + " exercise" + (count > 1 ? "s" : ""));
        });

        // ── Let's Go ─────────────────────────────────────────────────────────────
        btnLetsGo.setOnClickListener(v -> {
            // TODO: ouvre ActiveWorkoutActivity f lmostagbal
            finish();
        });

        // ── Edit Plan (drawer) ───────────────────────────────────────────────────
        btnEditPlan.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            getSharedPreferences("gym_prefs", MODE_PRIVATE)
                    .edit().remove("gym_plan_week").apply();
            startActivity(new Intent(this, GymSetupActivity.class));
            finish();
        });
    }
}