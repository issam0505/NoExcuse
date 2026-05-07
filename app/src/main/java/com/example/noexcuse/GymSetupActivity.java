package com.example.noexcuse;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.noexcuse.database.AppViewModel;
import com.example.noexcuse.database.GymPlan;
import com.example.noexcuse.database.PlannedExercise;
import com.example.noexcuse.database.WeekUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GymSetupActivity extends AppCompatActivity {

    // ─── Constantes ───────────────────────────────────────────────────────────

    private static final String PREFS_NAME     = "gym_prefs";
    private static final String KEY_SAVED_WEEK = "gym_plan_week";

    private static final String[] DAYS_ORDER = {
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    };
    private static final String[] DAYS_LABELS = {
            "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    };

    // ─── Body parts — multi-select possible (e.g. "Biceps + Triceps") ─────────
    private static final String[] BODY_PARTS = {
            "Chest", "Back", "Legs", "Shoulders",
            "Biceps", "Triceps", "Arms",
            "Core", "Abdo", "Full Body", "Cardio",
            "Push", "Pull", "Rest Day"
    };

    // ─── Colors inline ────────────────────────────────────────────────────────
    private static final int COLOR_ORANGE          = 0xFFFF6D00;
    private static final int COLOR_ORANGE_BG       = 0xFF120A00;
    private static final int COLOR_CARD_BG         = 0xFF131313;
    private static final int COLOR_CARD_STROKE     = 0xFF252525;
    private static final int COLOR_DOT_UNSEL       = 0xFF444444;
    private static final int COLOR_CHIP_UNSEL_BG   = 0xFF1E1E1E;
    private static final int COLOR_CHIP_SEL_TEXT   = 0xFF000000;
    private static final int COLOR_CHIP_UNSEL_TEXT = 0xFFAAAAAA;

    // ─── UI Step 1 ────────────────────────────────────────────────────────────

    private View           stepOneView;
    private LinearLayout   daysContainer;
    private MaterialButton btnNext;
    private ImageView      btnBackStepOne;

    // ─── UI Step 2 ────────────────────────────────────────────────────────────

    private View           stepTwoView;
    private TextView       tvCurrentDay;
    private LinearLayout   exercisesContainer;
    private MaterialButton btnAddExercise, btnNextDay, btnSaveAll;
    private ImageView      btnBackStepTwo;

    // ─── Data ─────────────────────────────────────────────────────────────────

    private AppViewModel viewModel;

    private static class DayEntry {
        String dayKey;
        String label;
        String bodyPart;
        List<ExerciseEntry> exercises = new ArrayList<>();
    }

    private static class ExerciseEntry {
        String  name;
        int     sets;
        int     durationMinutes; // lil Cardio
        boolean isCardio;
    }

    private final LinkedHashMap<String, DayEntry> selectedDays = new LinkedHashMap<>();
    private List<String> orderedSelectedKeys;
    private int currentDayIndex = 0;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym_setup);

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        stepOneView = findViewById(R.id.stepOneContainer);
        stepTwoView = findViewById(R.id.stepTwoContainer);

        initStepOne();
        checkExistingPlanAndStart();
    }

    // ─── CACHE CHECK ──────────────────────────────────────────────────────────

    private void checkExistingPlanAndStart() {
        String currentWeek = WeekUtils.getCurrentWeekStart();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedWeek = prefs.getString(KEY_SAVED_WEEK, "");

        if (savedWeek.equals(currentWeek)) {
            showReusePlanDialog();
        } else {
            showStepOne();
        }
    }

    private void showReusePlanDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Plan Already Exists 💪")
                .setMessage("You already set up a training plan for this week.\n\nDo you want to use the same plan again or create a new one?")
                .setPositiveButton("Use Same Plan", (d, w) -> {
                    Toast.makeText(this, "Plan loaded! Let's go 🔥", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("New Plan", (d, w) -> {
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit().remove(KEY_SAVED_WEEK).apply();
                    showStepOne();
                })
                .setCancelable(false)
                .show();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 1 — Sélection des jours + body parts
    // ═══════════════════════════════════════════════════════════════════════════

    private void initStepOne() {
        daysContainer  = findViewById(R.id.daysContainer);
        btnNext        = findViewById(R.id.btnNext);
        btnBackStepOne = findViewById(R.id.btnBackStepOne);

        // Back arrow — yrej3 l MainActivity
        btnBackStepOne.setOnClickListener(v -> finish());

        buildDayCards();

        btnNext.setOnClickListener(v -> {
            if (selectedDays.isEmpty()) {
                Toast.makeText(this, "Select at least one training day", Toast.LENGTH_SHORT).show();
                return;
            }
            for (DayEntry entry : selectedDays.values()) {
                if (entry.bodyPart == null || entry.bodyPart.isEmpty()) {
                    Toast.makeText(this, "Choose a muscle group for " + entry.label, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            startStepTwo();
        });
    }

    private void showStepOne() {
        stepOneView.setVisibility(View.VISIBLE);
        stepTwoView.setVisibility(View.GONE);
        stepOneView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
    }

    private void buildDayCards() {
        daysContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < DAYS_ORDER.length; i++) {
            final String dayKey   = DAYS_ORDER[i];
            final String dayLabel = DAYS_LABELS[i];

            View card = inflater.inflate(R.layout.item_day_card, daysContainer, false);

            TextView  tvDay      = card.findViewById(R.id.tvDayLabel);
            TextView  tvBodyPart = card.findViewById(R.id.tvBodyPart);
            ChipGroup chipGroup  = card.findViewById(R.id.chipGroupBodyParts);
            View      divider    = card.findViewById(R.id.chipDivider);

            tvDay.setText(dayLabel);
            chipGroup.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);

            buildBodyPartChips(chipGroup, dayKey, tvBodyPart);

            card.setOnClickListener(v -> {
                if (selectedDays.containsKey(dayKey)) {
                    // Désélectionner
                    selectedDays.remove(dayKey);
                    applyDayCardStyle(card, false);
                    chipGroup.setVisibility(View.GONE);
                    divider.setVisibility(View.GONE);
                    tvBodyPart.setText("");
                    // reset chips
                    for (int ci = 0; ci < chipGroup.getChildCount(); ci++) {
                        ((Chip) chipGroup.getChildAt(ci)).setChecked(false);
                    }
                } else {
                    // Sélectionner
                    DayEntry entry = new DayEntry();
                    entry.dayKey = dayKey;
                    entry.label  = dayLabel;
                    selectedDays.put(dayKey, entry);
                    applyDayCardStyle(card, true);
                    chipGroup.setVisibility(View.VISIBLE);
                    divider.setVisibility(View.VISIBLE);
                }
            });

            daysContainer.addView(card);
        }
    }

    /**
     * Multi-select chips — user y9der y5tar "Biceps + Triceps" bjoj
     * Rest Day u Cardio single-effect (mabghach tzid m3ahom)
     */
    private void buildBodyPartChips(ChipGroup chipGroup, String dayKey, TextView tvBodyPart) {
        chipGroup.removeAllViews();
        // ★ MULTI-SELECT — machi single selection
        chipGroup.setSingleSelection(false);

        ColorStateList chipBgColors = new ColorStateList(
                new int[][]{ new int[]{android.R.attr.state_checked}, new int[]{} },
                new int[]{ COLOR_ORANGE, COLOR_CHIP_UNSEL_BG }
        );
        ColorStateList chipTextColors = new ColorStateList(
                new int[][]{ new int[]{android.R.attr.state_checked}, new int[]{} },
                new int[]{ COLOR_CHIP_SEL_TEXT, COLOR_CHIP_UNSEL_TEXT }
        );

        for (String bp : BODY_PARTS) {
            Chip chip = new Chip(this);
            chip.setText(bp);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setChipBackgroundColor(chipBgColors);
            chip.setTextColor(chipTextColors);

            chip.setOnCheckedChangeListener((btn, checked) -> {
                if (!selectedDays.containsKey(dayKey)) return;
                DayEntry entry = selectedDays.get(dayKey);

                // Rest Day ou Cardio — single effect: désélectionne le reste
                if (checked && (bp.equals("Rest Day") || bp.equals("Cardio") ||
                        bp.equals("Full Body") || bp.equals("Push") || bp.equals("Pull"))) {
                    for (int ci = 0; ci < chipGroup.getChildCount(); ci++) {
                        Chip c = (Chip) chipGroup.getChildAt(ci);
                        if (c != chip) c.setChecked(false);
                    }
                }

                // Jma3 kol chips li m7adddin
                StringBuilder selected = new StringBuilder();
                for (int ci = 0; ci < chipGroup.getChildCount(); ci++) {
                    Chip c = (Chip) chipGroup.getChildAt(ci);
                    if (c.isChecked()) {
                        if (selected.length() > 0) selected.append(" + ");
                        selected.append(c.getText());
                    }
                }
                entry.bodyPart = selected.toString();
                tvBodyPart.setText(entry.bodyPart);
            });

            chipGroup.addView(chip);
        }
    }

    /**
     * ★ FIX — kayebddel selection dot + card style
     */
    private void applyDayCardStyle(View card, boolean selected) {
        MaterialCardView cardView = (MaterialCardView) card;
        View dot = card.findViewById(R.id.selectionDot);

        if (selected) {
            cardView.setStrokeColor(COLOR_ORANGE);
            cardView.setCardBackgroundColor(COLOR_ORANGE_BG);
            if (dot != null) dot.setBackgroundTintList(
                    ColorStateList.valueOf(COLOR_ORANGE));
        } else {
            cardView.setStrokeColor(COLOR_CARD_STROKE);
            cardView.setCardBackgroundColor(COLOR_CARD_BG);
            if (dot != null) dot.setBackgroundTintList(
                    ColorStateList.valueOf(COLOR_DOT_UNSEL));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 2 — Exercices planifiés par jour
    // ═══════════════════════════════════════════════════════════════════════════

    private void startStepTwo() {
        orderedSelectedKeys = new ArrayList<>(selectedDays.keySet());
        currentDayIndex     = 0;

        tvCurrentDay       = findViewById(R.id.tvCurrentDay);
        exercisesContainer = findViewById(R.id.exercisesContainer);
        btnAddExercise     = findViewById(R.id.btnAddExercise);
        btnNextDay         = findViewById(R.id.btnNextDay);
        btnSaveAll         = findViewById(R.id.btnSaveAll);
        btnBackStepTwo     = findViewById(R.id.btnBackStepTwo);

        stepOneView.setVisibility(View.GONE);
        stepTwoView.setVisibility(View.VISIBLE);
        stepTwoView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

        // Back arrow — yrej3 l Step 1
        btnBackStepTwo.setOnClickListener(v -> {
            stepTwoView.setVisibility(View.GONE);
            showStepOne();
        });

        loadCurrentDay();

        btnAddExercise.setOnClickListener(v -> addExerciseRow());

        btnNextDay.setOnClickListener(v -> {
            if (!validateAndSaveCurrentDayExercises()) return;
            currentDayIndex++;
            if (currentDayIndex < orderedSelectedKeys.size()) {
                loadCurrentDay();
            } else {
                saveAllToDatabase();
            }
        });

        btnSaveAll.setOnClickListener(v -> {
            if (!validateAndSaveCurrentDayExercises()) return;
            saveAllToDatabase();
        });
    }

    /**
     * ★ FIX — Rest Day: skip mbachers
     *       — Cardio: row b "Min" bla sets
     */
    private void loadCurrentDay() {
        String   key   = orderedSelectedKeys.get(currentDayIndex);
        DayEntry entry = selectedDays.get(key);

        // ★ Rest Day — makaynch exercises, skip
        if (entry.bodyPart != null && entry.bodyPart.equals("Rest Day")) {
            entry.exercises.clear();
            boolean isLast = (currentDayIndex == orderedSelectedKeys.size() - 1);
            if (isLast) {
                saveAllToDatabase();
            } else {
                currentDayIndex++;
                loadCurrentDay();
            }
            return;
        }

        boolean isLast = (currentDayIndex == orderedSelectedKeys.size() - 1);

        tvCurrentDay.setText(entry.label + "  —  " + entry.bodyPart);

        exercisesContainer.removeAllViews();
        for (ExerciseEntry ex : entry.exercises) {
            addExerciseRowWithData(ex.name, ex.sets, ex.durationMinutes, ex.isCardio);
        }
        if (entry.exercises.isEmpty()) {
            addExerciseRow();
        }

        // ★ Cardio — hide "Add Exercise" button ou changer label
        boolean dayIsCardio = isDayCardio(entry.bodyPart);
        btnAddExercise.setText(dayIsCardio ? "+ Add Cardio" : "+ Add Exercise");

        btnNextDay.setVisibility(isLast ? View.GONE : View.VISIBLE);
        btnSaveAll.setVisibility(isLast ? View.VISIBLE : View.GONE);

        TextView tvProgress = findViewById(R.id.tvDayProgress);
        tvProgress.setText((currentDayIndex + 1) + " / " + orderedSelectedKeys.size());
    }

    private void addExerciseRow() {
        String key = orderedSelectedKeys.get(currentDayIndex);
        DayEntry entry = selectedDays.get(key);
        boolean isCardio = isDayCardio(entry.bodyPart);
        addExerciseRowWithData("", isCardio ? 0 : 3, isCardio ? 30 : 0, isCardio);
    }

    /**
     * ★ FIX — Cardio row: icon 🏃, hint "Min", default 30 dqa2iq
     *         Normal row: icon 🏋, hint "Sets", default 3
     */
    private void addExerciseRowWithData(String name, int sets, int duration, boolean isCardio) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View row = inflater.inflate(R.layout.item_exercise_row, exercisesContainer, false);

        TextView          tvIcon    = row.findViewById(R.id.tvExerciseIcon);
        TextInputEditText etName    = row.findViewById(R.id.etExerciseName);
        TextInputLayout   tilSets   = row.findViewById(R.id.tilSetsOrDuration);
        TextInputEditText etSets    = row.findViewById(R.id.etSetsTarget);
        ImageView         btnDel    = row.findViewById(R.id.btnDeleteExercise);

        etName.setText(name);

        if (isCardio) {
            if (tvIcon  != null) tvIcon.setText("🏃");
            if (tilSets != null) tilSets.setHint("Min");
            etSets.setHint("Min");
            etSets.setText(duration > 0 ? String.valueOf(duration) : "30");
        } else {
            if (tvIcon  != null) tvIcon.setText("🏋");
            if (tilSets != null) tilSets.setHint("Sets");
            etSets.setHint("Sets");
            etSets.setText(sets > 0 ? String.valueOf(sets) : "");
        }

        btnDel.setOnClickListener(v -> {
            if (exercisesContainer.getChildCount() > 1) {
                exercisesContainer.removeView(row);
            } else {
                Toast.makeText(this, "At least 1 exercise needed", Toast.LENGTH_SHORT).show();
            }
        });

        row.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
        exercisesContainer.addView(row);
    }

    private boolean validateAndSaveCurrentDayExercises() {
        String   key   = orderedSelectedKeys.get(currentDayIndex);
        DayEntry entry = selectedDays.get(key);

        // Rest Day — makaynch validation
        if (entry.bodyPart != null && entry.bodyPart.equals("Rest Day")) {
            return true;
        }

        boolean dayIsCardio = isDayCardio(entry.bodyPart);
        entry.exercises.clear();

        for (int i = 0; i < exercisesContainer.getChildCount(); i++) {
            View row = exercisesContainer.getChildAt(i);
            TextInputEditText etName = row.findViewById(R.id.etExerciseName);
            TextInputEditText etSets = row.findViewById(R.id.etSetsTarget);

            String exName  = etName.getText() != null ? etName.getText().toString().trim() : "";
            String numStr  = etSets.getText()  != null ? etSets.getText().toString().trim()  : "";

            if (exName.isEmpty()) {
                Toast.makeText(this, "Enter a name for exercise " + (i + 1), Toast.LENGTH_SHORT).show();
                return false;
            }

            int num = dayIsCardio ? 30 : 3;
            try {
                if (!numStr.isEmpty()) num = Integer.parseInt(numStr);
            } catch (NumberFormatException e) {
                num = dayIsCardio ? 30 : 3;
            }

            ExerciseEntry ex = new ExerciseEntry();
            ex.name     = exName;
            ex.isCardio = dayIsCardio;
            if (dayIsCardio) {
                ex.durationMinutes = num;
                ex.sets            = 0;
            } else {
                ex.sets            = num;
                ex.durationMinutes = 0;
            }
            entry.exercises.add(ex);
        }

        if (entry.exercises.isEmpty()) {
            Toast.makeText(this, "Add at least one exercise for " + entry.label, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // ─── Helper — wach had nhar cardio? ──────────────────────────────────────

    private boolean isDayCardio(String bodyPart) {
        return bodyPart != null && bodyPart.contains("Cardio");
    }

    // ─── SAVE TO DATABASE ─────────────────────────────────────────────────────

    private void saveAllToDatabase() {
        String         weekStart = WeekUtils.getCurrentWeekStart();
        List<DayEntry> entries   = new ArrayList<>(selectedDays.values());
        AtomicInteger  pending   = new AtomicInteger(entries.size());

        for (DayEntry dayEntry : entries) {
            GymPlan plan       = new GymPlan();
            plan.dayOfWeek     = dayEntry.dayKey;
            plan.weekStartDate = weekStart;
            plan.bodyPart      = dayEntry.bodyPart;
            plan.startTime     = "";
            plan.isSynced      = false;

            viewModel.addGymPlan(plan, planId -> {
                // Rest Day — khawi, mashi exercises
                for (ExerciseEntry ex : dayEntry.exercises) {
                    PlannedExercise exercise = new PlannedExercise();
                    exercise.planId          = planId;
                    exercise.exerciseName    = ex.name;
                    exercise.setsTarget      = ex.sets;
                    exercise.durationMinutes = ex.durationMinutes;
                    exercise.isCardio        = ex.isCardio;
                    viewModel.addPlannedExercise(exercise, null);
                }
                if (pending.decrementAndGet() == 0) {
                    runOnUiThread(this::onSaveComplete);
                }
            });
        }
    }

    private void onSaveComplete() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_SAVED_WEEK, WeekUtils.getCurrentWeekStart())
                .apply();
        Toast.makeText(this, "Plan saved! Week is set 🔥", Toast.LENGTH_SHORT).show();
        finish();
    }
}