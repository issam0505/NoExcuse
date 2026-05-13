package com.example.noexcuse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.noexcuse.database.AppViewModel;
import com.example.noexcuse.database.GymPerformance;
import com.example.noexcuse.database.WeekUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WorkoutHistoryActivity extends AppCompatActivity {

    private AppViewModel viewModel;
    private String       bodyPart;
    private int          planId;

    private TextView     tvBodyPartTitle;
    private TextView     tvTotalSessions;
    private TextView     tvTotalSets;
    private TextView     tvBestWeight;
    private LinearLayout historyContainer;
    private LinearLayout emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_history);

        planId   = getIntent().getIntExtra("PLAN_ID", -1);
        bodyPart = getIntent().getStringExtra("PLAN_BODY_PART");
        if (planId == -1) { finish(); return; }

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        // ── Bind views ───────────────────────────────────────────────────
        FrameLayout btnBack    = findViewById(R.id.btnBack);
        tvBodyPartTitle        = findViewById(R.id.tvBodyPartTitle);
        tvTotalSessions        = findViewById(R.id.tvTotalSessions);
        tvTotalSets            = findViewById(R.id.tvTotalSets);
        tvBestWeight           = findViewById(R.id.tvBestWeight);
        historyContainer       = findViewById(R.id.historyContainer);
        emptyState             = findViewById(R.id.emptyState);

        btnBack.setOnClickListener(v -> finish());
        tvBodyPartTitle.setText(bodyPart != null ? bodyPart : "Workout");

        // ── Load performances ────────────────────────────────────────────
        // We load all performances for exercises linked to this plan
        viewModel.getExercisesForPlan(planId).observe(this, exercises -> {
            if (exercises == null || exercises.isEmpty()) {
                showEmpty();
                return;
            }

            // Collect all exercise IDs for this plan
            List<Integer> exIds = new ArrayList<>();
            for (com.example.noexcuse.database.PlannedExercise ex : exercises) {
                exIds.add(ex.id);
            }

            // Load all performances for those exercise IDs
            viewModel.getPerformancesForExercises(exIds, perfs -> {
                runOnUiThread(() -> renderHistory(perfs));
            });
        });
    }

    // ─── Render stats + history cards ────────────────────────────────────

    private void renderHistory(List<GymPerformance> perfs) {
        if (perfs == null || perfs.isEmpty()) {
            showEmpty();
            return;
        }

        emptyState.setVisibility(View.GONE);
        historyContainer.setVisibility(View.VISIBLE);

        // ── Compute summary stats ────────────────────────────────────────
        int   totalSets   = perfs.size();
        float bestWeightKg = 0f;
        String bestExName = "";

        // Group by date (day) → session count
        Map<String, List<GymPerformance>> byDay = new LinkedHashMap<>();
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (GymPerformance p : perfs) {
            String day = dayFmt.format(new Date(p.date > 0 ? p.date : System.currentTimeMillis()));
            if (!byDay.containsKey(day)) byDay.put(day, new ArrayList<>());
            byDay.get(day).add(p);

            if (p.weight > bestWeightKg) {
                bestWeightKg = p.weight;
                bestExName   = p.exerciseNameSnapshot != null ? p.exerciseNameSnapshot : "";
            }
        }

        int totalSessions = byDay.size();

        tvTotalSessions.setText(String.valueOf(totalSessions));
        tvTotalSets.setText(String.valueOf(totalSets));

        if (bestWeightKg > 0) {
            // Show both kg and lbs
            float lbs = bestWeightKg / 0.453592f;
            tvBestWeight.setText(String.format(Locale.getDefault(),
                    "%.1f kg / %.1f lbs", bestWeightKg, lbs));
        } else {
            tvBestWeight.setText("—");
        }

        // ── Render sessions newest-first ─────────────────────────────────
        historyContainer.removeAllViews();
        List<String> days = new ArrayList<>(byDay.keySet());
        Collections.sort(days, Collections.reverseOrder());

        SimpleDateFormat displayFmt = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());

        for (String day : days) {
            List<GymPerformance> dayPerfs = byDay.get(day);

            // Parse date for display
            String displayDate = day;
            try {
                Date d = dayFmt.parse(day);
                displayDate = displayFmt.format(d);
            } catch (Exception ignored) {}

            // Inflate session card
            View card = LayoutInflater.from(this)
                    .inflate(R.layout.item_history_session, historyContainer, false);

            TextView tvDate      = card.findViewById(R.id.tvSessionDate);
            TextView tvSetCount  = card.findViewById(R.id.tvSessionSetCount);
            LinearLayout exRows  = card.findViewById(R.id.sessionExerciseRows);

            tvDate.setText(displayDate);
            tvSetCount.setText(dayPerfs.size() + " sets");

            // Group by exercise name within this day
            Map<String, List<GymPerformance>> byEx = new LinkedHashMap<>();
            for (GymPerformance p : dayPerfs) {
                String name = p.exerciseNameSnapshot != null ? p.exerciseNameSnapshot : "Exercise";
                if (!byEx.containsKey(name)) byEx.put(name, new ArrayList<>());
                byEx.get(name).add(p);
            }

            for (Map.Entry<String, List<GymPerformance>> entry : byEx.entrySet()) {
                View exRow = LayoutInflater.from(this)
                        .inflate(R.layout.item_history_exercise_row, exRows, false);

                TextView tvExName   = exRow.findViewById(R.id.tvHistoryExName);
                TextView tvSetsSummary = exRow.findViewById(R.id.tvHistorySetsSummary);

                tvExName.setText(entry.getKey());

                // Build summary: "3×12 @ 60kg | 3×10 @ 65kg" etc.
                StringBuilder sb = new StringBuilder();
                for (GymPerformance p : entry.getValue()) {
                    if (sb.length() > 0) sb.append("  ·  ");
                    float lbs = p.weight / 0.453592f;
                    sb.append(String.format(Locale.getDefault(),
                            "Set %d: %d reps @ %.1fkg (%.1flbs)",
                            p.setNumber, p.reps, p.weight, lbs));
                }
                tvSetsSummary.setText(sb.toString());

                exRows.addView(exRow);
            }

            historyContainer.addView(card);
        }
    }

    private void showEmpty() {
        emptyState.setVisibility(View.VISIBLE);
        historyContainer.setVisibility(View.GONE);
        tvTotalSessions.setText("0");
        tvTotalSets.setText("0");
        tvBestWeight.setText("—");
    }
}