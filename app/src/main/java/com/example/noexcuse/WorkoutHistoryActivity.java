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
import com.example.noexcuse.database.PlannedExercise;
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

    // Map exerciseId → isCardio, built once exercises load
    private final Map<Integer, Boolean>  isCardioMap  = new LinkedHashMap<>();
    private final Map<Integer, String>   exNameMap    = new LinkedHashMap<>();
    // Set of exerciseIds that were planned for this session
    private final List<Integer>          plannedExIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_history);

        planId   = getIntent().getIntExtra("PLAN_ID", -1);
        bodyPart = getIntent().getStringExtra("PLAN_BODY_PART");
        if (planId == -1) { finish(); return; }

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        FrameLayout btnBack = findViewById(R.id.btnBack);
        tvBodyPartTitle     = findViewById(R.id.tvBodyPartTitle);
        tvTotalSessions     = findViewById(R.id.tvTotalSessions);
        tvTotalSets         = findViewById(R.id.tvTotalSets);
        tvBestWeight        = findViewById(R.id.tvBestWeight);
        historyContainer    = findViewById(R.id.historyContainer);
        emptyState          = findViewById(R.id.emptyState);

        btnBack.setOnClickListener(v -> finish());
        tvBodyPartTitle.setText(bodyPart != null ? bodyPart : "Workout");

        viewModel.getExercisesForPlan(planId).observe(this, exercises -> {
            if (exercises == null || exercises.isEmpty()) { showEmpty(); return; }

            isCardioMap.clear();
            exNameMap.clear();
            plannedExIds.clear();
            for (PlannedExercise ex : exercises) {
                isCardioMap.put(ex.id, ex.isCardio);
                exNameMap.put(ex.id, ex.exerciseName != null ? ex.exerciseName : "Exercise");
                plannedExIds.add(ex.id);
            }

            List<Integer> exIds = new ArrayList<>(plannedExIds);
            viewModel.getPerformancesForExercises(exIds, perfs ->
                    runOnUiThread(() -> renderHistory(perfs)));
        });
    }

    // ─── Render ──────────────────────────────────────────────────────────

    private void renderHistory(List<GymPerformance> perfs) {
        if (perfs == null || perfs.isEmpty()) { showEmpty(); return; }

        emptyState.setVisibility(View.GONE);
        historyContainer.setVisibility(View.VISIBLE);

        // ── Summary stats (only regular exercises count for sets/best weight) ──
        int   totalSets    = 0;
        float bestWeightKg = 0f;
        String bestExName  = "";

        Map<String, List<GymPerformance>> byDay = new LinkedHashMap<>();
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (GymPerformance p : perfs) {
            String day = dayFmt.format(new Date(p.date > 0 ? p.date : System.currentTimeMillis()));
            byDay.computeIfAbsent(day, k -> new ArrayList<>()).add(p);

            boolean isCardio = Boolean.TRUE.equals(isCardioMap.get(p.plannedExerciseId));
            if (!isCardio) {
                totalSets++;
                if (p.weight > bestWeightKg) {
                    bestWeightKg = p.weight;
                    bestExName   = p.exerciseNameSnapshot != null ? p.exerciseNameSnapshot : "";
                }
            }
        }

        int totalSessions = byDay.size();

        tvTotalSessions.setText(String.valueOf(totalSessions));
        tvTotalSets.setText(String.valueOf(totalSets));

        if (bestWeightKg > 0) {
            float lbs = bestWeightKg / 0.453592f;
            tvBestWeight.setText(String.format(Locale.getDefault(),
                    "%.1f kg / %.1f lbs", bestWeightKg, lbs));
        } else {
            tvBestWeight.setText("—");
        }

        // ── Render sessions (newest first) ───────────────────────────────
        historyContainer.removeAllViews();
        List<String> days = new ArrayList<>(byDay.keySet());
        Collections.sort(days, Collections.reverseOrder());

        SimpleDateFormat displayFmt = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());

        // Also detect which planned exercises have NO record on this day
        // so we can show "Skipped" for them.
        for (String day : days) {
            List<GymPerformance> dayPerfs = byDay.get(day);

            String displayDate = day;
            try { displayDate = displayFmt.format(dayFmt.parse(day)); }
            catch (Exception ignored) {}

            View card = LayoutInflater.from(this)
                    .inflate(R.layout.item_history_session, historyContainer, false);

            TextView     tvDate     = card.findViewById(R.id.tvSessionDate);
            TextView     tvSetCount = card.findViewById(R.id.tvSessionSetCount);
            LinearLayout exRows     = card.findViewById(R.id.sessionExerciseRows);

            tvDate.setText(displayDate);

            // Count only regular sets for the badge
            long regularSets = dayPerfs.stream()
                    .filter(p -> !Boolean.TRUE.equals(isCardioMap.get(p.plannedExerciseId)))
                    .count();
            long cardioCount = dayPerfs.stream()
                    .filter(p -> Boolean.TRUE.equals(isCardioMap.get(p.plannedExerciseId)))
                    .count();

            String badge = "";
            if (regularSets > 0) badge += regularSets + " sets";
            if (cardioCount > 0) badge += (badge.isEmpty() ? "" : " · ") + cardioCount + " cardio";
            tvSetCount.setText(badge.isEmpty() ? "—" : badge);

            // Group by exercise name within the day
            Map<String, List<GymPerformance>> byEx = new LinkedHashMap<>();
            for (GymPerformance p : dayPerfs) {
                String name = p.exerciseNameSnapshot != null ? p.exerciseNameSnapshot : "Exercise";
                byEx.computeIfAbsent(name, k -> new ArrayList<>()).add(p);
            }

            // ── Regular exercises ─────────────────────────────────────────
            for (Map.Entry<String, List<GymPerformance>> entry : byEx.entrySet()) {
                GymPerformance first     = entry.getValue().get(0);
                boolean        isCardio  = Boolean.TRUE.equals(isCardioMap.get(first.plannedExerciseId));

                View exRow = LayoutInflater.from(this)
                        .inflate(R.layout.item_history_exercise_row, exRows, false);

                TextView tvExName      = exRow.findViewById(R.id.tvHistoryExName);
                TextView tvSetsSummary = exRow.findViewById(R.id.tvHistorySetsSummary);

                tvExName.setText(entry.getKey());

                if (isCardio) {
                    // ── Cardio row: show time recorded ───────────────────
                    int secs = first.durationRecordedSeconds;
                    tvSetsSummary.setText("⏱ " + formatSeconds(secs));
                    tvSetsSummary.setTextColor(0xFFFF6D00); // orange accent
                } else {
                    // ── Regular row: sets detail ──────────────────────────
                    StringBuilder sb = new StringBuilder();
                    for (GymPerformance p : entry.getValue()) {
                        if (sb.length() > 0) sb.append("\n");
                        float lbs = p.weight / 0.453592f;
                        sb.append(String.format(Locale.getDefault(),
                                "Set %d: %d reps @ %.1f kg (%.1f lbs)",
                                p.setNumber, p.reps, p.weight, lbs));
                    }
                    tvSetsSummary.setText(sb.toString());
                }

                exRows.addView(exRow);
            }

            // ── Skipped exercises: planned but NO record that day ─────────
            // Collect exercise IDs that have a record this day
            List<Integer> recordedIds = new ArrayList<>();
            for (GymPerformance p : dayPerfs) recordedIds.add(p.plannedExerciseId);

            for (Integer exId : plannedExIds) {
                if (!recordedIds.contains(exId)) {
                    // This exercise was planned but not done this day
                    String name = exNameMap.getOrDefault(exId, "Exercise");

                    View exRow = LayoutInflater.from(this)
                            .inflate(R.layout.item_history_exercise_row, exRows, false);

                    TextView tvExName      = exRow.findViewById(R.id.tvHistoryExName);
                    TextView tvSetsSummary = exRow.findViewById(R.id.tvHistorySetsSummary);

                    tvExName.setText(name);
                    tvSetsSummary.setText("— skipped");
                    tvSetsSummary.setTextColor(0xFF374151);

                    exRows.addView(exRow);
                }
            }

            historyContainer.addView(card);
        }
    }

    // ─── Empty state ──────────────────────────────────────────────────────

    private void showEmpty() {
        emptyState.setVisibility(View.VISIBLE);
        historyContainer.setVisibility(View.GONE);
        tvTotalSessions.setText("0");
        tvTotalSets.setText("0");
        tvBestWeight.setText("—");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private String formatSeconds(int totalSec) {
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec);
    }
}