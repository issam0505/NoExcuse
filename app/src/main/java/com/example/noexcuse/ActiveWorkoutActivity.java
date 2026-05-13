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
import com.example.noexcuse.widget.CircularTimerView;
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
    private FrameLayout        warmupOverlay;
    private LinearLayout       workoutContent;
    private LinearLayout       exercisesContainer;
    private TextView           tvWorkoutBodyPart;
    private FrameLayout        restTimerOverlay;
    private CircularTimerView  circularTimer;
    private MaterialButton     btnAction;

    // ─── State ────────────────────────────────────────────────────────────
    private CountDownTimer restCountdown;
    private int currentExIndex = 0;
    private View currentCard   = null;

    // ⭐ KEY CHANGE: unit tracked PER EXERCISE (not global)
    // currentExUnitIsKg = unit selected by user for the CURRENT exercise card
    // This resets to true (kg) each time a new exercise card is shown,
    // but the user can toggle it independently for every exercise.
    // When collectCurrentExercise() runs, it reads THIS flag to know which unit
    // the user entered weights in — and converts to kg before saving if needed.
    private boolean currentExUnitIsKg = true;

    // ─── PENDING PERFORMANCES ─────────────────────────────────────────────
    private final List<GymPerformance> pendingPerformances = new ArrayList<>();

    private enum BtnState { START, NEXT, SAVE }
    private BtnState btnState = BtnState.START;

    private static final int REST_DURATION_MS = 2 * 60 * 1000;
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

        warmupOverlay      = findViewById(R.id.warmupOverlay);
        workoutContent     = findViewById(R.id.workoutContent);
        exercisesContainer = findViewById(R.id.exercisesWorkoutContainer);
        tvWorkoutBodyPart  = findViewById(R.id.tvWorkoutBodyPart);
        restTimerOverlay   = findViewById(R.id.restTimerOverlay);
        circularTimer      = findViewById(R.id.circularTimer);
        btnAction          = findViewById(R.id.btnStartWorkout);

        tvWorkoutBodyPart.setText(bodyPart != null ? bodyPart : "Workout");

        // ── Warmup top bar ────────────────────────────────────────────────
        TextView tvWarmupBodyPart = findViewById(R.id.tvWarmupBodyPart);
        tvWarmupBodyPart.setText(bodyPart != null ? bodyPart : "Workout");

        FrameLayout btnBackWarmup = findViewById(R.id.btnBackWarmup);
        btnBackWarmup.setOnClickListener(v -> finish());

        MaterialButton btnWarmupLetsGo = findViewById(R.id.btnWarmupLetsGo);
        MaterialButton btnWarmupSkip   = findViewById(R.id.btnWarmupSkip);
        btnWarmupLetsGo.setOnClickListener(v -> showWorkout());
        btnWarmupSkip.setOnClickListener(v   -> showWorkout());

        FrameLayout btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        applyStartStyle();
        btnAction.setOnClickListener(v -> onActionPressed());

        viewModel.getExercisesForPlan(planId).observe(this, list -> {
            if (list == null || list.isEmpty()) return;
            exercises = list;
        });
    }

    // ─── Style helpers ───────────────────────────────────────────────────

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

    // ─── Warm-up → workout ───────────────────────────────────────────────

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

    // ─── Action button ────────────────────────────────────────────────────

    private void onActionPressed() {
        switch (btnState) {
            case START:
                showExerciseAt(0);
                break;
            case NEXT:
                collectCurrentExercise();
                startRestThenShowNext();
                break;
            case SAVE:
                collectCurrentExercise();
                saveAllPendingToDatabase();
                finishWorkout();
                break;
        }
    }

    // ─── Show exercise card ───────────────────────────────────────────────

    private void showExerciseAt(int index) {
        if (index >= exercises.size()) { finishWorkout(); return; }

        PlannedExercise ex = exercises.get(index);
        if (ex.isCardio) { openCardioActivity(); return; }

        exercisesContainer.removeAllViews();
        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_workout_exercise, exercisesContainer, false);
        card.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

        TextView       tvName        = card.findViewById(R.id.tvExerciseName);
        LinearLayout   setsContainer = card.findViewById(R.id.setsContainer);
        MaterialButton btnUnit       = card.findViewById(R.id.btnUnitToggle);

        tvName.setText((ex.exerciseName != null ? ex.exerciseName : "—")
                + "\n" + (index + 1) + " / " + exercises.size());

        // ⭐ Reset unit to kg for each new exercise card
        currentExUnitIsKg = true;
        btnUnit.setText("kg");

        // ⭐ Toggle updates currentExUnitIsKg (scoped to this exercise)
        // The button label always reflects the current unit for this exercise.
        // Changing unit on exercise 2 does NOT affect exercise 1's saved data.
        btnUnit.setOnClickListener(v -> {
            currentExUnitIsKg = !currentExUnitIsKg;
            btnUnit.setText(currentExUnitIsKg ? "kg" : "lbs");
        });

        int setsCount = ex.setsTarget > 0 ? ex.setsTarget : 3;
        for (int s = 1; s <= setsCount; s++) {
            addSetRow(setsContainer, s);
        }

        exercisesContainer.addView(card);
        currentCard = card;

        boolean isLast = (index == exercises.size() - 1);
        if (isLast) applySaveStyle(); else applyNextStyle();
    }

    // ─── Add set row ──────────────────────────────────────────────────────

    private void addSetRow(LinearLayout setsContainer, int setNumber) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_workout_set_row, setsContainer, false);
        TextView tvSetNum = row.findViewById(R.id.tvSetNumber);
        tvSetNum.setText(String.valueOf(setNumber));
        setsContainer.addView(row);
    }

    // ─── Collect current exercise → pending ───────────────────────────────
    //
    // ⭐ UNIT LOGIC:
    //   - user enters weight in whichever unit they picked (kg or lbs)
    //   - we ALWAYS store in kg in the database (standard)
    //   - if user picked lbs → multiply by 0.453592 before saving
    //   - the unit toggle is per-exercise: exercise A can be lbs,
    //     exercise B can be kg — each is handled independently here
    //     because currentExUnitIsKg is read at collect-time (not at save-time)

    private void collectCurrentExercise() {
        if (currentCard == null || currentExIndex >= exercises.size()) return;

        PlannedExercise ex            = exercises.get(currentExIndex);
        LinearLayout    setsContainer = currentCard.findViewById(R.id.setsContainer);
        int             rowCount      = setsContainer.getChildCount();
        long            now           = System.currentTimeMillis();

        // ⭐ Capture the unit for THIS exercise at collect-time
        boolean thisExIsKg = currentExUnitIsKg;

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

            // ⭐ Convert lbs → kg if this exercise's unit was lbs
            if (!thisExIsKg) weight = weight * 0.453592f;

            GymPerformance perf       = new GymPerformance();
            perf.plannedExerciseId    = ex.id;
            perf.exerciseNameSnapshot = ex.exerciseName;
            perf.setNumber            = s + 1;
            perf.weight               = weight;   // always stored in kg
            perf.reps                 = reps;
            perf.date                 = now;

            pendingPerformances.add(perf);
        }

        // Advance index after collecting
        currentExIndex++;
    }

    // ─── Save ALL pending to database ─────────────────────────────────────

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

    // ─── Rest timer ───────────────────────────────────────────────────────

    private void startRestThenShowNext() {
        exercisesContainer.setVisibility(View.GONE);
        btnAction.setVisibility(View.GONE);

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
                showNextAfterRest();
            }
        }.start();
    }

    private void showNextAfterRest() {
        restTimerOverlay.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> {
                    restTimerOverlay.setVisibility(View.GONE);
                    exercisesContainer.setVisibility(View.VISIBLE);
                    btnAction.setVisibility(View.VISIBLE);
                    showExerciseAt(currentExIndex);
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
        if (requestCode == REQ_CARDIO) { setResult(resultCode); finish(); }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (restCountdown != null) restCountdown.cancel();
    }
}