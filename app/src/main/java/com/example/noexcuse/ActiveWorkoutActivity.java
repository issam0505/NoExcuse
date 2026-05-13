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

    // ─── Data ─────────────────────────────────────────────────────────────
    private AppViewModel          viewModel;
    private int                   planId;
    private String                bodyPart;
    private List<PlannedExercise> exercises = new ArrayList<>();

    // ─── Views ────────────────────────────────────────────────────────────
    private FrameLayout  warmupOverlay;
    private LinearLayout workoutContent;
    private LinearLayout exercisesContainer;
    private TextView     tvWorkoutBodyPart;
    private FrameLayout  restTimerOverlay;
    private TextView     tvRestCountdown;
    private MaterialButton btnAction;

    // ─── State ────────────────────────────────────────────────────────────
    private CountDownTimer restCountdown;

    // index dyal exercise li banayin daba (0 = luwla)
    private int currentExIndex = 0;

    // hadi hiya l-card li banayin (katsejel men b3d f Save)
    private View currentCard = null;

    private boolean unitIsKg = true;

    // STATE MACHINE dyal btnAction:
    // START  → user ma-dar-sh start bada
    // NEXT   → user dar start, ikan ynavancer bين exercises
    // SAVE   → user wesel l-exercise la5ra, ighdi isejel
    private enum BtnState { START, NEXT, SAVE }
    private BtnState btnState = BtnState.START;

    private static final int REST_DURATION_MS = 2 * 60 * 1000; // 2 min
    private static final int REQ_CARDIO       = 300;

    // ─────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_workout);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        planId   = getIntent().getIntExtra("PLAN_ID", -1);
        bodyPart = getIntent().getStringExtra("PLAN_BODY_PART");
        if (planId == -1) { finish(); return; }

        // ── Bind views ───────────────────────────────────────────────────
        warmupOverlay      = findViewById(R.id.warmupOverlay);
        workoutContent     = findViewById(R.id.workoutContent);
        exercisesContainer = findViewById(R.id.exercisesWorkoutContainer);
        tvWorkoutBodyPart  = findViewById(R.id.tvWorkoutBodyPart);
        restTimerOverlay   = findViewById(R.id.restTimerOverlay);
        tvRestCountdown    = findViewById(R.id.tvRestCountdown);
        btnAction          = findViewById(R.id.btnStartWorkout);

        tvWorkoutBodyPart.setText(bodyPart != null ? bodyPart : "Workout");

        // ── Warm-up overlay ──────────────────────────────────────────────
        MaterialButton btnWarmupLetsGo = findViewById(R.id.btnWarmupLetsGo);
        MaterialButton btnWarmupSkip   = findViewById(R.id.btnWarmupSkip);
        btnWarmupLetsGo.setOnClickListener(v -> showWorkout());
        btnWarmupSkip.setOnClickListener(v   -> showWorkout());

        // ── Back button ──────────────────────────────────────────────────
        FrameLayout btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // ── Skip rest ────────────────────────────────────────────────────
        MaterialButton btnSkipRest = findViewById(R.id.btnSkipRest);
        btnSkipRest.setOnClickListener(v -> skipRest());

        // ── Action button — START state (kbir, vert) ─────────────────────
        applyStartStyle();
        btnAction.setOnClickListener(v -> onActionPressed());

        // ── Load exercises ───────────────────────────────────────────────
        viewModel.getExercisesForPlan(planId).observe(this, list -> {
            if (list == null || list.isEmpty()) return;
            exercises = list;
        });
    }

    // ─── Style helpers lil btn ───────────────────────────────────────────

    private void applyStartStyle() {
        btnState = BtnState.START;
        btnAction.setText("▶  START");
        btnAction.setTextSize(18f);
        btnAction.getLayoutParams().width  = dpToPx(160);
        btnAction.getLayoutParams().height = dpToPx(56);
        btnAction.requestLayout();
        btnAction.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#22C55E")));
    }

    private void applyNextStyle() {
        btnState = BtnState.NEXT;
        btnAction.setText("Next  →");
        btnAction.setTextSize(16f);
        btnAction.getLayoutParams().width  = dpToPx(160);
        btnAction.getLayoutParams().height = dpToPx(52);
        btnAction.requestLayout();
        btnAction.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#1D4ED8")));
    }

    private void applySaveStyle() {
        btnState = BtnState.SAVE;
        btnAction.setText("💾  Save");
        btnAction.setTextSize(16f);
        btnAction.getLayoutParams().width  = dpToPx(160);
        btnAction.getLayoutParams().height = dpToPx(52);
        btnAction.requestLayout();
        btnAction.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#FF6D00")));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ─── Warm-up → workout transition ────────────────────────────────────

    private void showWorkout() {
        warmupOverlay.animate()
                .alpha(0f)
                .setDuration(350)
                .withEndAction(() -> {
                    warmupOverlay.setVisibility(View.GONE);
                    workoutContent.setVisibility(View.VISIBLE);
                    workoutContent.setAlpha(0f);
                    workoutContent.animate().alpha(1f).setDuration(300).start();
                    // btnAction bqa f START state — user ghaydir click bach ibda
                })
                .start();
    }

    // ─── Action button pressed ────────────────────────────────────────────

    private void onActionPressed() {
        switch (btnState) {

            case START:
                // L-click luwla: byan exercise 0 u ibdal l btn l NEXT wla SAVE
                showExerciseAt(0);
                break;

            case NEXT:
                // Navancer l-exercise li ba3dha (bla save)
                currentExIndex++;
                showExerciseAt(currentExIndex);
                break;

            case SAVE:
                // L-exercise la5ra: sawi save u fermwork
                saveCurrentExercise();
                finishWorkout();
                break;
        }
    }

    // ─── Show exercise card at index ──────────────────────────────────────

    private void showExerciseAt(int index) {
        if (index >= exercises.size()) {
            finishWorkout();
            return;
        }

        PlannedExercise ex = exercises.get(index);

        // Cardio → fout l CardioWorkoutActivity
        if (ex.isCardio) {
            openCardioActivity();
            return;
        }

        // Byan l-card b animation
        exercisesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        View card = inflater.inflate(R.layout.item_workout_exercise, exercisesContainer, false);
        card.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

        TextView       tvName        = card.findViewById(R.id.tvExerciseName);
        LinearLayout   setsContainer = card.findViewById(R.id.setsContainer);
        MaterialButton btnUnit       = card.findViewById(R.id.btnUnitToggle);

        tvName.setText(ex.exerciseName != null ? ex.exerciseName : "—");

        // Byan numero d'exercice: "Exercise 2 / 5"
        tvName.setText((ex.exerciseName != null ? ex.exerciseName : "—")
                + "\n"
                + (index + 1) + " / " + exercises.size());

        unitIsKg = true;
        btnUnit.setText("kg");
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

        // ── Update btn state ──────────────────────────────────────────────
        // Ila hada huwa l-exercise la5er → SAVE, sinon → NEXT
        boolean isLast = (index == exercises.size() - 1);
        if (isLast) {
            applySaveStyle();
        } else {
            applyNextStyle();
        }
    }

    // ─── Add set row ──────────────────────────────────────────────────────

    private void addSetRow(LinearLayout setsContainer, int setNumber) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_workout_set_row, setsContainer, false);
        TextView tvSetNum = row.findViewById(R.id.tvSetNumber);
        tvSetNum.setText(String.valueOf(setNumber));
        setsContainer.addView(row);
    }

    // ─── Save l-exercise la5ra ────────────────────────────────────────────
    // MUHIM: gha kattsejel daba f Save (l-exercise la5ra) — machi kol exercise

    private void saveCurrentExercise() {
        if (currentCard == null || currentExIndex >= exercises.size()) return;

        PlannedExercise ex            = exercises.get(currentExIndex);
        LinearLayout    setsContainer = currentCard.findViewById(R.id.setsContainer);
        int             rowCount      = setsContainer.getChildCount();
        long            now           = System.currentTimeMillis();

        int savedCount = 0;

        for (int s = 0; s < rowCount; s++) {
            View              row      = setsContainer.getChildAt(s);
            TextInputEditText etWeight = row.findViewById(R.id.etWeight);
            TextInputEditText etReps   = row.findViewById(R.id.etReps);

            String wStr = etWeight.getText() != null ? etWeight.getText().toString().trim() : "";
            String rStr = etReps.getText()   != null ? etReps.getText().toString().trim()   : "";

            if (wStr.isEmpty() && rStr.isEmpty()) continue;

            float weight = 0f;
            int   reps   = 0;
            try { weight = Float.parseFloat(wStr); } catch (NumberFormatException ignored) {}
            try { reps   = Integer.parseInt(rStr);  } catch (NumberFormatException ignored) {}

            // Convert lbs → kg ila user khtar lbs
            if (!unitIsKg) weight = weight * 0.453592f;

            GymPerformance perf       = new GymPerformance();
            perf.plannedExerciseId    = ex.id;
            perf.exerciseNameSnapshot = ex.exerciseName;
            perf.setNumber            = s + 1;
            perf.weight               = weight;
            perf.reps                 = reps;
            perf.date                 = now;

            viewModel.addPerformance(perf);
            savedCount++;
        }

        if (savedCount > 0) {
            Toast.makeText(this, "Performance saved 💪", Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Rest timer (2 min) ───────────────────────────────────────────────
    // IMPORTANT: had l-fonction mabqatch tattsama automatiquement
    // User ighdi ydir Next manually — rest kayn gha ila user bga ytsena

    @SuppressWarnings("unused")
    private void startRestTimer() {
        restTimerOverlay.setVisibility(View.VISIBLE);
        restTimerOverlay.animate().alpha(1f).setDuration(300).start();

        exercisesContainer.setVisibility(View.GONE);
        btnAction.setVisibility(View.GONE);

        restCountdown = new CountDownTimer(REST_DURATION_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secs   = millisUntilFinished / 1000;
                long mins   = secs / 60;
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
                finishWorkout();
            } else {
                showExerciseAt(currentExIndex);
            }
        }).start();
    }

    // ─── Finish workout ───────────────────────────────────────────────────

    private void finishWorkout() {
        Toast.makeText(this, "Workout done! 🏆", Toast.LENGTH_LONG).show();
        setResult(RESULT_OK);
        finish();
    }

    // ─── Cardio redirect ──────────────────────────────────────────────────

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