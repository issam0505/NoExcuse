package com.example.noexcuse;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.noexcuse.database.AppViewModel;
import com.example.noexcuse.database.GymPerformance;
import com.example.noexcuse.database.PlannedExercise;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ActiveWorkoutActivity extends AppCompatActivity {

    // ─── Data ─────────────────────────────────────────────────────────────────
    private AppViewModel          viewModel;
    private int                   planId;
    private String                bodyPart;
    private List<PlannedExercise> exercises = new ArrayList<>();

    // ─── Views ────────────────────────────────────────────────────────────────
    private FrameLayout  warmupOverlay;
    private LinearLayout workoutContent;
    private LinearLayout exercisesContainer;
    private TextView     tvWorkoutBodyPart;

    // Rest timer views (inside workoutContent)
    private FrameLayout  restTimerOverlay;   // covers screen with crono
    private TextView     tvRestCountdown;
    private CountDownTimer restCountdown;

    // Action button (Start → Save → Next → Finish)
    private MaterialButton btnAction;

    // ─── State ────────────────────────────────────────────────────────────────
    private int     currentExIndex = 0;   // l-exercise li banayin daba
    private boolean unitIsKg       = true;
    private View    currentCard    = null; // l-card li bana daba

    private static final int REST_DURATION_MS = 2 * 60 * 1000; // 2 dqa2iq
    private static final int REQ_CARDIO       = 300;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_workout);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        planId   = getIntent().getIntExtra("PLAN_ID", -1);
        bodyPart = getIntent().getStringExtra("PLAN_BODY_PART");
        if (planId == -1) { finish(); return; }

        // Views
        warmupOverlay      = findViewById(R.id.warmupOverlay);
        workoutContent     = findViewById(R.id.workoutContent);
        exercisesContainer = findViewById(R.id.exercisesWorkoutContainer);
        tvWorkoutBodyPart  = findViewById(R.id.tvWorkoutBodyPart);
        restTimerOverlay   = findViewById(R.id.restTimerOverlay);
        tvRestCountdown    = findViewById(R.id.tvRestCountdown);
        btnAction          = findViewById(R.id.btnStartWorkout);

        tvWorkoutBodyPart.setText(bodyPart != null ? bodyPart : "Workout");

        // ── Warm-up overlay ──────────────────────────────────────────────────
        MaterialButton btnWarmupLetsGo = findViewById(R.id.btnWarmupLetsGo);
        MaterialButton btnWarmupSkip   = findViewById(R.id.btnWarmupSkip);
        btnWarmupLetsGo.setOnClickListener(v -> showWorkout());
        btnWarmupSkip.setOnClickListener(v   -> showWorkout());

        // ── Back button ──────────────────────────────────────────────────────
        FrameLayout btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // ── Skip rest timer ──────────────────────────────────────────────────
        MaterialButton btnSkipRest = findViewById(R.id.btnSkipRest);
        btnSkipRest.setOnClickListener(v -> skipRest());

        // ── Action button (Start / Save / Next / Finish) ─────────────────────
        // L-start: gha Start visible, exercices hidden
        btnAction.setText("▶");
        btnAction.setOnClickListener(v -> onActionPressed());

        // ── Load exercises from DB ───────────────────────────────────────────
        viewModel.getExercisesForPlan(planId).observe(this, list -> {
            if (list == null || list.isEmpty()) return;
            exercises = list;
        });
    }

    // ─── Warm-up → workout transition ────────────────────────────────────────

    private void showWorkout() {
        warmupOverlay.animate()
                .alpha(0f)
                .setDuration(350)
                .withEndAction(() -> {
                    warmupOverlay.setVisibility(View.GONE);
                    workoutContent.setVisibility(View.VISIBLE);
                    workoutContent.setAlpha(0f);
                    workoutContent.animate().alpha(1f).setDuration(300).start();
                    // Hna gha btnAction b Start, ma-ka-yban-sh ga3 shi card
                })
                .start();
    }

    // ─── Action button pressed ────────────────────────────────────────────────

    private void onActionPressed() {
        String label = btnAction.getText().toString();

        if (label.equals("▶") || label.equals("Next →")) {
            // Show l-exercise li jadiya (ou luwla)
            showExerciseAt(currentExIndex);

        } else if (label.equals("Save")) {
            // Save + start rest timer
            saveCurrentExercise();
            currentExIndex++;
            startRestTimer();
        }
    }

    // ─── Show exercise card at index ──────────────────────────────────────────

    private void showExerciseAt(int index) {
        if (index >= exercises.size()) {
            // Kmlu exercises kolhom
            finishWorkout();
            return;
        }

        PlannedExercise ex = exercises.get(index);

        // Cardio day
        if (ex.isCardio) {
            openCardioActivity();
            return;
        }

        exercisesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        View card = inflater.inflate(R.layout.item_workout_exercise, exercisesContainer, false);
        card.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

        TextView     tvName        = card.findViewById(R.id.tvExerciseName);
        LinearLayout setsContainer = card.findViewById(R.id.setsContainer);
        MaterialButton btnUnit     = card.findViewById(R.id.btnUnitToggle);

        tvName.setText(ex.exerciseName != null ? ex.exerciseName : "—");

        unitIsKg = true;
        btnUnit.setOnClickListener(v -> {
            unitIsKg = !unitIsKg;
            btnUnit.setText(unitIsKg ? "kg" : "lbs");
        });

        int setsCount = ex.setsTarget > 0 ? ex.setsTarget : 3;
        for (int s = 1; s <= setsCount; s++) {
            addSetRow(setsContainer, s);
        }

        exercisesContainer.addView(card);
        currentCard = card;

        // Update bouton: Save
        btnAction.setText("Save");
        btnAction.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF6D00")));
    }

    // ─── Add set row ──────────────────────────────────────────────────────────

    private void addSetRow(LinearLayout setsContainer, int setNumber) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_workout_set_row, setsContainer, false);
        TextView tvSetNum = row.findViewById(R.id.tvSetNumber);
        tvSetNum.setText(String.valueOf(setNumber));
        setsContainer.addView(row);
    }

    // ─── Save current exercise's sets ─────────────────────────────────────────

    private void saveCurrentExercise() {
        if (currentCard == null || currentExIndex >= exercises.size()) return;

        PlannedExercise ex         = exercises.get(currentExIndex);
        LinearLayout    setsContainer = currentCard.findViewById(R.id.setsContainer);
        int             rowCount   = setsContainer.getChildCount();
        long            now        = System.currentTimeMillis();

        for (int s = 0; s < rowCount; s++) {
            View             row      = setsContainer.getChildAt(s);
            TextInputEditText etWeight = row.findViewById(R.id.etWeight);
            TextInputEditText etReps   = row.findViewById(R.id.etReps);

            String wStr = etWeight.getText() != null ? etWeight.getText().toString().trim() : "";
            String rStr = etReps.getText()   != null ? etReps.getText().toString().trim()   : "";

            if (wStr.isEmpty() && rStr.isEmpty()) continue;

            float weight = 0f;
            int   reps   = 0;
            try { weight = Float.parseFloat(wStr); } catch (NumberFormatException ignored) {}
            try { reps   = Integer.parseInt(rStr);  } catch (NumberFormatException ignored) {}

            if (!unitIsKg) weight = weight * 0.453592f;

            GymPerformance perf         = new GymPerformance();
            perf.plannedExerciseId      = ex.id;
            perf.exerciseNameSnapshot   = ex.exerciseName;
            perf.setNumber              = s + 1;
            perf.weight                 = weight;
            perf.reps                   = reps;
            perf.date                   = now;   // ⭐ date dyal tsejil

            viewModel.addPerformance(perf);
        }

        Toast.makeText(this, "Set saved 💪", Toast.LENGTH_SHORT).show();
    }

    // ─── Rest timer (2 min) ───────────────────────────────────────────────────

    private void startRestTimer() {
        restTimerOverlay.setVisibility(View.VISIBLE);
        restTimerOverlay.animate().alpha(1f).setDuration(300).start();

        // Hide exercise card + action button during rest
        exercisesContainer.setVisibility(View.GONE);
        btnAction.setVisibility(View.GONE);

        restCountdown = new CountDownTimer(REST_DURATION_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secs  = millisUntilFinished / 1000;
                long mins  = secs / 60;
                long secRem = secs % 60;
                tvRestCountdown.setText(
                        String.format(java.util.Locale.getDefault(), "%d:%02d", mins, secRem));
            }
            @Override
            public void onFinish() {
                tvRestCountdown.setText("0:00");
                showNextAfterRest();
            }
        }.start();
    }

    private void skipRest() {
        if (restCountdown != null) restCountdown.cancel();
        showNextAfterRest();
    }

    private void showNextAfterRest() {
        restTimerOverlay.animate().alpha(0f).setDuration(300).withEndAction(() -> {
            restTimerOverlay.setVisibility(View.GONE);
            exercisesContainer.setVisibility(View.VISIBLE);
            btnAction.setVisibility(View.VISIBLE);

            if (currentExIndex >= exercises.size()) {
                // Mabqach shi exercise
                finishWorkout();
            } else {
                // Byan l-exercise li jadiya direct (bla ma ytsenna dabs)
                showExerciseAt(currentExIndex);
            }
        }).start();
    }

    // ─── Finish workout ───────────────────────────────────────────────────────

    private void finishWorkout() {
        Toast.makeText(this, "Workout done! 🏆", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    // ─── Cardio redirect ──────────────────────────────────────────────────────

    private void openCardioActivity() {
        Intent intent = new Intent(this, CardioWorkoutActivity.class);
        intent.putExtra("PLAN_ID",        planId);
        intent.putExtra("PLAN_BODY_PART", bodyPart);
        startActivityForResult(intent, REQ_CARDIO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CARDIO) {
            setResult(resultCode);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (restCountdown != null) restCountdown.cancel();
    }
}