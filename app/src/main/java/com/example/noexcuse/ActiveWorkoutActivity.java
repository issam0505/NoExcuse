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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.noexcuse.database.AppViewModel;
import com.example.noexcuse.database.GymPerformance;
import com.example.noexcuse.database.PlannedExercise;
import com.example.noexcuse.widget.CircularTimerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ActiveWorkoutActivity extends AppCompatActivity {

    private AppViewModel          viewModel;
    private int                   planId;
    private String                bodyPart;
    private List<PlannedExercise> exercises = new ArrayList<>();

    private FrameLayout        warmupOverlay;
    private LinearLayout       workoutContent;
    private LinearLayout       exercisesContainer;
    private TextView           tvWorkoutBodyPart;
    private FrameLayout        restTimerOverlay;
    private CircularTimerView  circularTimer;
    private MaterialButton     btnAction;

    private TextView tvCurrentExerciseName;
    private TextView tvSetCounter;

    private CountDownTimer restCountdown;

    // ── Navigation state ─────────────────────────────────────────────────
    private int currentExIndex  = 0;
    private int currentSetIndex = 0;
    private int totalSets       = 0;

    private View    currentCard       = null;
    private boolean currentExUnitIsKg = true;

    private final List<GymPerformance> pendingPerformances = new ArrayList<>();

    private enum BtnState { START, NEXT_SET, NEXT_EXERCISE, SAVE }
    private BtnState btnState = BtnState.START;

    private static final int REST_DURATION_MS = 2 * 60 * 1000;
    private static final int REQ_CARDIO       = 300;

    // ─────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_workout);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        planId   = getIntent().getIntExtra("PLAN_ID", -1);
        bodyPart = getIntent().getStringExtra("PLAN_BODY_PART");
        if (planId == -1) { finish(); return; }

        warmupOverlay          = findViewById(R.id.warmupOverlay);
        workoutContent         = findViewById(R.id.workoutContent);
        exercisesContainer     = findViewById(R.id.exercisesWorkoutContainer);
        tvWorkoutBodyPart      = findViewById(R.id.tvWorkoutBodyPart);
        restTimerOverlay       = findViewById(R.id.restTimerOverlay);
        circularTimer          = findViewById(R.id.circularTimer);
        btnAction              = findViewById(R.id.btnStartWorkout);
        tvCurrentExerciseName  = findViewById(R.id.tvCurrentExerciseName);
        tvSetCounter           = findViewById(R.id.tvSetCounter);

        tvWorkoutBodyPart.setText(bodyPart != null ? bodyPart : "Workout");

        TextView tvWarmupBodyPart = findViewById(R.id.tvWarmupBodyPart);
        tvWarmupBodyPart.setText(bodyPart != null ? bodyPart : "Workout");

        // Back buttons
        FrameLayout btnBackWarmup = findViewById(R.id.btnBackWarmup);
        btnBackWarmup.setOnClickListener(v -> finish());

        FrameLayout btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // ── Let's Go → open WarmupActivity ──────────────────────────────
        MaterialButton btnWarmupLetsGo = findViewById(R.id.btnWarmupLetsGo);
        btnWarmupLetsGo.setOnClickListener(v -> {
            Intent intent = new Intent(this, WarmupActivity.class);
            intent.putExtra("PLAN_ID",        planId);
            intent.putExtra("PLAN_BODY_PART", bodyPart);
            startActivity(intent);
        });

        // ── Skip → go directly to workout ───────────────────────────────
        MaterialButton btnWarmupSkip = findViewById(R.id.btnWarmupSkip);
        btnWarmupSkip.setOnClickListener(v -> showWorkout());

        applyStartStyle();
        btnAction.setOnClickListener(v -> onActionPressed());

        viewModel.getExercisesForPlan(planId).observe(this, list -> {
            if (list == null || list.isEmpty()) return;
            exercises = list;
        });
    }

    // ── Button styles ─────────────────────────────────────────────────────

    private void applyStartStyle() {
        btnState = BtnState.START;
        btnAction.setText("▶  START");
        btnAction.setTextSize(18f);
        setButtonSize(160, 56);
        btnAction.setBackgroundTintList(colorList("#22C55E"));
    }

    private void applyNextSetStyle() {
        btnState = BtnState.NEXT_SET;
        btnAction.setText("Next Set  →");
        btnAction.setTextSize(16f);
        setButtonSize(170, 52);
        btnAction.setBackgroundTintList(colorList("#1D4ED8"));
    }

    private void applyNextExerciseStyle() {
        btnState = BtnState.NEXT_EXERCISE;
        btnAction.setText("Next Exercise  →");
        btnAction.setTextSize(15f);
        setButtonSize(200, 52);
        btnAction.setBackgroundTintList(colorList("#7C3AED"));
    }

    private void applySaveStyle() {
        btnState = BtnState.SAVE;
        btnAction.setText("💾  Save");
        btnAction.setTextSize(16f);
        setButtonSize(160, 52);
        btnAction.setBackgroundTintList(colorList("#FF6D00"));
    }

    private void setButtonSize(int widthDp, int heightDp) {
        btnAction.getLayoutParams().width  = dpToPx(widthDp);
        btnAction.getLayoutParams().height = dpToPx(heightDp);
        btnAction.requestLayout();
    }

    private android.content.res.ColorStateList colorList(String hex) {
        return android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(hex));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ── Warmup overlay → Workout transition ──────────────────────────────

    private void showWorkout() {
        warmupOverlay.animate()
                .alpha(0f).setDuration(350)
                .withEndAction(() -> {
                    warmupOverlay.setVisibility(View.GONE);
                    workoutContent.setVisibility(View.VISIBLE);
                    workoutContent.setAlpha(0f);
                    workoutContent.animate().alpha(1f).setDuration(300).start();
                }).start();
    }

    // ── Main action button ────────────────────────────────────────────────

    private void onActionPressed() {
        switch (btnState) {
            case START:
                currentExIndex  = 0;
                currentSetIndex = 0;
                showCurrentSet();
                break;

            case NEXT_SET:
                collectCurrentSet();
                currentSetIndex++;
                startRestThenResume();
                break;

            case NEXT_EXERCISE:
                collectCurrentSet();
                currentExIndex++;
                currentSetIndex = 0;
                startRestThenResume();
                break;

            case SAVE:
                collectCurrentSet();
                saveAllPendingToDatabase();
                finishWorkout();
                break;
        }
    }

    // ── Render the single visible set row ─────────────────────────────────

    private void showCurrentSet() {
        if (currentExIndex >= exercises.size()) { finishWorkout(); return; }

        PlannedExercise ex = exercises.get(currentExIndex);

        if (ex.isCardio) { openCardioActivity(); return; }

        totalSets = ex.setsTarget > 0 ? ex.setsTarget : 3;

        // Exercise name header
        if (tvCurrentExerciseName != null) {
            tvCurrentExerciseName.setVisibility(View.VISIBLE);
            String label = (ex.exerciseName != null ? ex.exerciseName : "—")
                    + "  ·  Exercise " + (currentExIndex + 1) + " / " + exercises.size();
            tvCurrentExerciseName.setText(label);
        }

        // Set counter
        if (tvSetCounter != null) {
            tvSetCounter.setVisibility(View.VISIBLE);
            tvSetCounter.setText("Set " + (currentSetIndex + 1) + " / " + totalSets);
        }

        // Card with ONE set row
        exercisesContainer.removeAllViews();

        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_workout_exercise, exercisesContainer, false);
        card.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

        // Hide in-card exercise name (shown in header above)
        TextView tvName = card.findViewById(R.id.tvExerciseName);
        if (tvName != null) tvName.setVisibility(View.GONE);

        // Unit toggle
        MaterialButton btnUnit = card.findViewById(R.id.btnUnitToggle);
        // ── 👁 Show Exercise button ─────────────────────────────────────────────
        MaterialButton btnShowEx = card.findViewById(R.id.btnShowExercise);
        if (btnShowEx != null) {
            // ex.exerciseName est le nom stocké en DB (ex: "Bench Press")
            final String exName = (ex.exerciseName != null && !ex.exerciseName.isEmpty())
                    ? ex.exerciseName
                    : null;

            if (exName == null) {
                // Pas de nom → cacher le bouton
                btnShowEx.setVisibility(android.view.View.GONE);
            } else {
                btnShowEx.setVisibility(android.view.View.VISIBLE);
                btnShowEx.setOnClickListener(v -> {
                    android.content.Intent previewIntent =
                            new android.content.Intent(this, ExercisePreviewActivity.class);
                    previewIntent.putExtra("EXERCISE_NAME", exName);
                    previewIntent.putExtra("PLAN_BODY_PART", bodyPart);
                    startActivity(previewIntent);
                });
            }
        }
        if (btnUnit != null) {
            currentExUnitIsKg = true;
            btnUnit.setText("kg");
            btnUnit.setOnClickListener(v -> {
                currentExUnitIsKg = !currentExUnitIsKg;
                btnUnit.setText(currentExUnitIsKg ? "kg" : "lbs");
            });
        }

        // Exactly ONE set row
        LinearLayout setsContainer = card.findViewById(R.id.setsContainer);
        addSetRow(setsContainer, currentSetIndex + 1);

        exercisesContainer.addView(card);
        currentCard = card;

        // Button state
        boolean isLastSet      = (currentSetIndex == totalSets - 1);
        boolean isLastExercise = (currentExIndex  == exercises.size() - 1);

        if (!isLastSet) {
            applyNextSetStyle();
        } else if (!isLastExercise) {
            applyNextExerciseStyle();
        } else {
            applySaveStyle();
        }
    }

    private void addSetRow(LinearLayout container, int setNumber) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_workout_set_row, container, false);
        TextView tvSetNum = row.findViewById(R.id.tvSetNumber);
        if (tvSetNum != null) tvSetNum.setText(String.valueOf(setNumber));
        container.addView(row);
    }

    // ── Collect the single visible set row ────────────────────────────────

    private void collectCurrentSet() {
        if (currentCard == null || currentExIndex >= exercises.size()) return;

        PlannedExercise ex            = exercises.get(currentExIndex);
        LinearLayout    setsContainer = currentCard.findViewById(R.id.setsContainer);
        if (setsContainer == null || setsContainer.getChildCount() == 0) return;

        View              row      = setsContainer.getChildAt(0);
        TextInputEditText etWeight = row.findViewById(R.id.etWeight);
        TextInputEditText etReps   = row.findViewById(R.id.etReps);

        String wStr = etWeight != null && etWeight.getText() != null
                ? etWeight.getText().toString().trim() : "";
        String rStr = etReps   != null && etReps.getText()   != null
                ? etReps.getText().toString().trim()   : "";

        if (wStr.isEmpty() && rStr.isEmpty()) return;

        float weight = 0f;
        int   reps   = 0;
        try { weight = Float.parseFloat(wStr); } catch (NumberFormatException ignored) {}
        try { reps   = Integer.parseInt(rStr);  } catch (NumberFormatException ignored) {}

        if (!currentExUnitIsKg) weight = weight * 0.453592f;

        GymPerformance perf       = new GymPerformance();
        perf.plannedExerciseId    = ex.id;
        perf.exerciseNameSnapshot = ex.exerciseName;
        perf.setNumber            = currentSetIndex + 1;
        perf.weight               = weight;
        perf.reps                 = reps;
        perf.date                 = System.currentTimeMillis();

        pendingPerformances.add(perf);
    }

    // ── Save ──────────────────────────────────────────────────────────────

    private void saveAllPendingToDatabase() {
        if (pendingPerformances.isEmpty()) return;
        for (GymPerformance perf : pendingPerformances) {
            viewModel.addPerformance(perf);
        }
        Toast.makeText(this,
                "Workout saved! " + pendingPerformances.size() + " sets 💪",
                Toast.LENGTH_SHORT).show();
        pendingPerformances.clear();
    }

    // ── Rest timer ────────────────────────────────────────────────────────

    private void startRestThenResume() {
        exercisesContainer.setVisibility(View.GONE);
        btnAction.setVisibility(View.GONE);
        if (tvCurrentExerciseName != null) tvCurrentExerciseName.setVisibility(View.GONE);
        if (tvSetCounter != null)          tvSetCounter.setVisibility(View.GONE);

        circularTimer.setTotalSeconds(REST_DURATION_MS / 1000);
        restTimerOverlay.setVisibility(View.VISIBLE);
        restTimerOverlay.setAlpha(0f);
        restTimerOverlay.animate().alpha(1f).setDuration(300).start();

        restCountdown = new CountDownTimer(REST_DURATION_MS, 1000) {
            @Override public void onTick(long ms) {
                circularTimer.setRemainingSeconds((int)(ms / 1000));
            }
            @Override public void onFinish() {
                circularTimer.setRemainingSeconds(0);
                showAfterRest();
            }
        }.start();
    }

    private void showAfterRest() {
        restTimerOverlay.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> {
                    restTimerOverlay.setVisibility(View.GONE);
                    exercisesContainer.setVisibility(View.VISIBLE);
                    btnAction.setVisibility(View.VISIBLE);
                    showCurrentSet();
                }).start();
    }

    // ── Finish & Cardio ───────────────────────────────────────────────────

    private void finishWorkout() {
        Toast.makeText(this, "Workout done! 🏆", Toast.LENGTH_LONG).show();
        setResult(RESULT_OK);
        finish();
    }

    private void openCardioActivity() {
        Intent intent = new Intent(this, CardioWorkoutActivity.class);
        intent.putExtra("PLAN_ID",        planId);
        intent.putExtra("PLAN_BODY_PART", bodyPart);
        startActivityForResult(intent, REQ_CARDIO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CARDIO) { setResult(resultCode); finish(); }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (restCountdown != null) restCountdown.cancel();
    }
}