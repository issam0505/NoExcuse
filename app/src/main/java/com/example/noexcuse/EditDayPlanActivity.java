package com.example.noexcuse;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.noexcuse.database.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditDayPlanActivity extends AppCompatActivity {

    private static final String[] DAYS = {
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY",
            "FRIDAY", "SATURDAY", "SUNDAY"
    };
    private static final String[] DAY_LABELS = {
            "Mon", "Tue", "Wed", "Thu",
            "Fri", "Sat", "Sun"
    };

    // Colors — kullshi gris, gher selected orange
    private static final int COLOR_CHIP_DEFAULT    = 0xFF1F1F1F; // bg gris foncé — kol chip
    private static final int COLOR_CHIP_SELECTED   = 0xFFFF6D00; // bg orange — selected chip only
    private static final int COLOR_CHIP_OCCUPIED   = 0xFF1F1F1F; // même bg que default
    private static final int COLOR_CHIP_REST       = 0xFF1F1F1F; // même bg que default
    private static final int COLOR_CHIP_CONFLICT   = 0xFF2A1500; // légèrement amber — swap only
    private static final int COLOR_TEXT_DEFAULT    = 0xFF6B7280; // gris — kol chip
    private static final int COLOR_TEXT_SELECTED   = 0xFF111111; // noir — selected chip (sur bg orange)
    private static final int COLOR_TEXT_OCCUPIED   = 0xFF6B7280; // même gris que default
    private static final int COLOR_TEXT_REST       = 0xFF6B7280; // même gris que default
    private static final int COLOR_TEXT_CONFLICT   = 0xFFFFAB40; // amber — swap warning chips

    private TextView              tvTitle;
    private ChipGroup             chipGroupDays;
    private TextInputEditText     etBodyPart;
    private RecyclerView          rvExercises;
    private ExtendedFloatingActionButton fabAddExercise;
    private MaterialButton        btnSave;
    private View                  btnBack;
    private TextView              tvSwapWarning;

    private AppViewModel viewModel;
    private int          planId;
    private String       originalDay;
    private String       selectedDay;
    private String       weekStart;

    private final GymPlan currentPlan = new GymPlan();

    private ExerciseEditAdapter adapter;
    private final List<PlannedExercise> exerciseList = new ArrayList<>();

    // Map: dayKey → GymPlan (lil days li 3andhom plans f had semana)
    private final Map<String, GymPlan> occupiedDays = new HashMap<>();
    private final Map<String, GymPlan> restDays     = new HashMap<>(); // rest day plans (bodyPart null/"Rest Day")
    private GymPlan conflictingPlan = null;

    // Chips map pour update couleurs facilement
    private final Map<String, Chip> chipMap = new HashMap<>();

    private boolean isRestDay = false;  // true ida bodyPart null wla "Rest Day"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_day_plan);

        planId      = getIntent().getIntExtra("PLAN_ID", -1);
        originalDay = getIntent().getStringExtra("PLAN_DAY");
        selectedDay = originalDay;
        weekStart   = getIntent().getStringExtra("WEEK_START");

        if (planId == -1 || originalDay == null || weekStart == null) {
            Toast.makeText(this, "Error: missing plan data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentPlan.id            = planId;
        currentPlan.weekStartDate = weekStart;
        currentPlan.dayOfWeek     = originalDay;
        currentPlan.bodyPart      = getIntent().getStringExtra("PLAN_BODY_PART");
        currentPlan.startTime     = getIntent().getStringExtra("PLAN_START_TIME");

        // Detect rest day at load time — but we re-check dynamically on save
        isRestDay = (currentPlan.bodyPart == null || currentPlan.bodyPart.equals("Rest Day"));

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        bindViews();
        setupRecyclerView();
        loadPlanData();

        // D'abord charger les plans de la semaine pour savoir les jours occupés
        loadWeekPlansAndSetupChips();

        btnBack.setOnClickListener(v -> handleBack());
        fabAddExercise.setOnClickListener(v -> showExerciseDialog(null, -1));
        btnSave.setOnClickListener(v -> saveAllChanges());
    }

    private void bindViews() {
        tvTitle        = findViewById(R.id.tvEditTitle);
        chipGroupDays  = findViewById(R.id.chipGroupDays);
        etBodyPart     = findViewById(R.id.etBodyPart);
        rvExercises    = findViewById(R.id.rvExercises);
        fabAddExercise = findViewById(R.id.fabAddExercise);
        btnSave        = findViewById(R.id.btnSaveChanges);
        btnBack        = findViewById(R.id.btnBack);
        tvSwapWarning  = findViewById(R.id.tvSwapWarning);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  LOAD WEEK PLANS → build chips avec couleurs
    // ─────────────────────────────────────────────────────────────────────

    private void loadWeekPlansAndSetupChips() {
        viewModel.getPlansForWeek(weekStart).observe(this, plans -> {
            occupiedDays.clear();
            restDays.clear();
            if (plans != null) {
                for (GymPlan p : plans) {
                    if (p.dayOfWeek != null) {
                        boolean planIsRest = (p.bodyPart == null || p.bodyPart.equals("Rest Day"));
                        if (!planIsRest) {
                            occupiedDays.put(p.dayOfWeek, p);
                        } else {
                            restDays.put(p.dayOfWeek, p); // track rest days separately
                        }
                    }
                }
            }
            setupDayChips();
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SETUP CHIPS — couleurs selon l'état de chaque jour
    // ─────────────────────────────────────────────────────────────────────

    private void setupDayChips() {
        chipGroupDays.removeAllViews();
        chipGroupDays.setSingleSelection(true);
        chipMap.clear();

        for (int i = 0; i < DAYS.length; i++) {
            String dayKey   = DAYS[i];
            String dayLabel = DAY_LABELS[i];

            Chip chip = new Chip(this);
            chip.setTag(dayKey);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setRippleColorResource(android.R.color.transparent);

            // Determiner l'état du chip
            boolean isCurrentDay  = dayKey.equals(originalDay);
            boolean isOccupied    = occupiedDays.containsKey(dayKey);

            // Check if this day has a rest plan (plan exists but bodyPart = "Rest Day" or null)
            boolean isRestDayChip = false;
            if (isCurrentDay) {
                isRestDayChip = isRestDay;
            } else {
                // Check plans for rest days too (we need all plans, not just non-rest)
                // occupiedDays only has non-rest plans, so if not occupied + not empty → might be rest
                // We track rest days separately via restDays map (added below)
                isRestDayChip = restDays.containsKey(dayKey);
            }

            String chipText = dayLabel;
            if (isOccupied && !isCurrentDay) {
                // Afficher le body part en subtitle — ex: "Tue\nLegs"
                GymPlan occupiedPlan = occupiedDays.get(dayKey);
                String bodyPart = (occupiedPlan != null && occupiedPlan.bodyPart != null)
                        ? occupiedPlan.bodyPart : "";
                if (!bodyPart.isEmpty() && bodyPart.length() > 8) {
                    bodyPart = bodyPart.substring(0, 8) + "…";
                }
                chipText = dayLabel + (bodyPart.isEmpty() ? "" : "\n" + bodyPart);
            } else if (isRestDayChip && !isCurrentDay) {
                chipText = dayLabel + "\nRest";
            } else if (isCurrentDay && isRestDay) {
                chipText = dayLabel + "\nRest";
            }
            chip.setText(chipText);

            // Couleur initiale:
            // - selectedDay → toujours orange vif
            // - occupied (non-selected) → dark orange bg
            // - rest day (non-selected) → dark blue-grey discret
            // - empty → gris foncé
            if (dayKey.equals(selectedDay)) {
                applyChipStyle(chip, COLOR_CHIP_SELECTED, COLOR_TEXT_SELECTED);
            } else if (isOccupied) {
                applyChipStyle(chip, COLOR_CHIP_OCCUPIED, COLOR_TEXT_OCCUPIED);
            } else if (isRestDayChip) {
                applyChipStyle(chip, COLOR_CHIP_REST, COLOR_TEXT_REST);
            } else {
                applyChipStyle(chip, COLOR_CHIP_DEFAULT, COLOR_TEXT_DEFAULT);
            }

            chip.setChecked(dayKey.equals(selectedDay));

            final String dayKeyCopy = dayKey;
            chip.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) onDaySelected(dayKeyCopy);
            });

            chipGroupDays.addView(chip);
            chipMap.put(dayKey, chip);
        }
    }

    private void applyChipStyle(Chip chip, int bgColor, int textColor) {
        chip.setChipBackgroundColor(
                android.content.res.ColorStateList.valueOf(bgColor));
        chip.setTextColor(textColor);
        chip.setChipStrokeWidth(1.5f);
        // Selected → orange stroke, everything else → subtle grey
        int strokeColor = (bgColor == COLOR_CHIP_SELECTED) ? 0xFFFF6D00 : 0xFF333333;
        chip.setChipStrokeColor(
                android.content.res.ColorStateList.valueOf(strokeColor));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  ON DAY SELECTED
    // ─────────────────────────────────────────────────────────────────────

    private void onDaySelected(String newDay) {
        if (newDay.equals(selectedDay)) return;
        // Proceed directly — no warning dialog for rest days
        proceedDaySelection(newDay);
    }

    private void proceedDaySelection(String newDay) {
        // Reset the previously selected chip to its natural color
        resetChipColor(selectedDay);

        selectedDay     = newDay;
        conflictingPlan = null;
        tvSwapWarning.setVisibility(View.GONE);

        // Paint the newly selected chip bright orange
        Chip newChip = chipMap.get(newDay);
        if (newChip != null) {
            applyChipStyle(newChip, COLOR_CHIP_SELECTED, COLOR_TEXT_SELECTED);
        }

        viewModel.getGymPlanForDayAndWeek(newDay, weekStart, plan -> {
            runOnUiThread(() -> {
                boolean planIsRest = plan != null
                        && (plan.bodyPart == null || plan.bodyPart.equals("Rest Day"));
                if (plan != null && plan.id != planId && !planIsRest) {
                    // CONFLICT — swap va se passer (rest days machi conflict)
                    conflictingPlan = plan;

                    // Both chips → amber to signal swap (machi rouge, 7san)
                    if (newChip != null) {
                        applyChipStyle(newChip, COLOR_CHIP_CONFLICT, COLOR_TEXT_CONFLICT);
                    }
                    Chip origChip = chipMap.get(originalDay);
                    if (origChip != null) {
                        applyChipStyle(origChip, COLOR_CHIP_CONFLICT, COLOR_TEXT_CONFLICT);
                    }

                    String currentBodyPart = etBodyPart.getText() != null
                            ? etBodyPart.getText().toString().trim() : "";
                    if (currentBodyPart.isEmpty()) {
                        currentBodyPart = (currentPlan.bodyPart != null
                                ? currentPlan.bodyPart : "Gym");
                    }
                    String conflictBodyPart = plan.bodyPart != null ? plan.bodyPart : "Gym";

                    String msg = "⚠️  Swap will happen:\n"
                            + "   " + toDayLabel(originalDay) + "  →  " + conflictBodyPart + "\n"
                            + "   " + toDayLabel(newDay)      + "  →  " + currentBodyPart;

                    tvSwapWarning.setText(msg);
                    tvSwapWarning.setBackgroundColor(0xFF2A1500); // dark amber bg (machi rouge)
                    tvSwapWarning.setTextColor(0xFFFFAB40);       // amber text
                    tvSwapWarning.setVisibility(View.VISIBLE);

                } else {
                    // Empty day — simple move, no swap
                    tvSwapWarning.setVisibility(View.GONE);
                }
            });
        });
    }

    /**
     * Reset a chip back to its natural color (occupied dark-orange or empty dark-grey).
     * Called when the user moves selection away from this chip.
     */
    private void resetChipColor(String dayKey) {
        Chip chip = chipMap.get(dayKey);
        if (chip == null) return;

        if (occupiedDays.containsKey(dayKey)) {
            applyChipStyle(chip, COLOR_CHIP_OCCUPIED, COLOR_TEXT_OCCUPIED);
        } else if (restDays.containsKey(dayKey)) {
            // Rest day — rj3 l style discret bhal kima kan
            GymPlan rp = restDays.get(dayKey);
            String restLabel = toDayLabel(dayKey) + "\nRest";
            chip.setText(restLabel);
            applyChipStyle(chip, COLOR_CHIP_REST, COLOR_TEXT_REST);
        } else {
            applyChipStyle(chip, COLOR_CHIP_DEFAULT, COLOR_TEXT_DEFAULT);
        }

        // If we had also painted originalDay amber (conflict state), reset it too
        if (!dayKey.equals(originalDay)) {
            Chip origChip = chipMap.get(originalDay);
            if (origChip != null && !originalDay.equals(selectedDay)) {
                // originalDay is always occupied (it has a plan)
                applyChipStyle(origChip, COLOR_CHIP_OCCUPIED, COLOR_TEXT_OCCUPIED);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  RECYCLER VIEW
    // ─────────────────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new ExerciseEditAdapter(exerciseList,
                this::onExerciseEdited,
                this::onExerciseDeleteRequested);

        rvExercises.setLayoutManager(new LinearLayoutManager(this));
        rvExercises.setAdapter(adapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder vh,
                                  RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getAdapterPosition();
                if (pos >= 0 && pos < exerciseList.size()) {
                    onExerciseDeleteRequested(exerciseList.get(pos));
                }
            }
        }).attachToRecyclerView(rvExercises);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  LOAD PLAN DATA
    // ─────────────────────────────────────────────────────────────────────

    private void loadPlanData() {
        if (currentPlan.bodyPart != null) etBodyPart.setText(currentPlan.bodyPart);
        tvTitle.setText("Edit  " + toDayLabel(originalDay));

        viewModel.getExercisesForPlan(planId).observe(this, exercises -> {
            exerciseList.clear();
            if (exercises != null) exerciseList.addAll(exercises);
            adapter.notifyDataSetChanged();
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    //  EXERCISE DIALOG
    // ─────────────────────────────────────────────────────────────────────

    private void showExerciseDialog(PlannedExercise existing, int position) {
        boolean isEdit = (existing != null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEdit ? "Edit Exercise" : "Add Exercise");

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_exercise_edit, null);
        builder.setView(dialogView);

        TextInputEditText etName         = dialogView.findViewById(R.id.etExerciseName);
        TextInputEditText etSets         = dialogView.findViewById(R.id.etSets);
        TextInputEditText etDuration     = dialogView.findViewById(R.id.etDuration);
        Switch            swCardio       = dialogView.findViewById(R.id.swIsCardio);
        LinearLayout      layoutSets     = dialogView.findViewById(R.id.layoutSets);
        LinearLayout      layoutDuration = dialogView.findViewById(R.id.layoutDuration);

        if (isEdit) {
            etName.setText(existing.exerciseName);
            swCardio.setChecked(existing.isCardio);
            if (existing.isCardio) {
                etDuration.setText(String.valueOf(existing.durationMinutes));
                layoutDuration.setVisibility(View.VISIBLE);
                layoutSets.setVisibility(View.GONE);
            } else {
                etSets.setText(String.valueOf(existing.setsTarget));
                layoutSets.setVisibility(View.VISIBLE);
                layoutDuration.setVisibility(View.GONE);
            }
        }

        swCardio.setOnCheckedChangeListener((btn, isCardio) -> {
            layoutSets.setVisibility(isCardio ? View.GONE : View.VISIBLE);
            layoutDuration.setVisibility(isCardio ? View.VISIBLE : View.GONE);
        });

        builder.setPositiveButton(isEdit ? "Save" : "Add", (dlg, which) -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                Toast.makeText(this, "Exercise name required", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isCardio = swCardio.isChecked();
            int sets = 0, duration = 0;
            try {
                if (isCardio) {
                    String d = etDuration.getText() != null ? etDuration.getText().toString() : "0";
                    duration = Integer.parseInt(d.isEmpty() ? "0" : d);
                } else {
                    String s = etSets.getText() != null ? etSets.getText().toString() : "0";
                    sets = Integer.parseInt(s.isEmpty() ? "0" : s);
                }
            } catch (NumberFormatException ignored) {}

            if (isEdit) {
                existing.exerciseName    = name;
                existing.setsTarget      = sets;
                existing.durationMinutes = duration;
                existing.isCardio        = isCardio;
                viewModel.updatePlannedExercise(existing);
                int idx = exerciseList.indexOf(existing);
                if (idx >= 0) adapter.notifyItemChanged(idx);
            } else {
                PlannedExercise ex = new PlannedExercise();
                ex.planId          = planId;
                ex.exerciseName    = name;
                ex.setsTarget      = sets;
                ex.durationMinutes = duration;
                ex.isCardio        = isCardio;
                viewModel.addPlannedExercise(ex, id -> runOnUiThread(() -> {
                    ex.id = id;
                    exerciseList.add(ex);
                    adapter.notifyItemInserted(exerciseList.size() - 1);
                }));
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  EXERCISE CALLBACKS
    // ─────────────────────────────────────────────────────────────────────

    private void onExerciseEdited(PlannedExercise exercise, int position) {
        showExerciseDialog(exercise, position);
    }

    private void onExerciseDeleteRequested(PlannedExercise exercise) {
        new AlertDialog.Builder(this)
                .setTitle("Delete exercise?")
                .setMessage("\"" + exercise.exerciseName + "\" ghadi t7eid?")
                .setPositiveButton("Delete", (dlg, w) -> {
                    viewModel.deletePlannedExercise(exercise);
                    int idx = exerciseList.indexOf(exercise);
                    if (idx >= 0) {
                        exerciseList.remove(idx);
                        adapter.notifyItemRemoved(idx);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SAVE
    // ─────────────────────────────────────────────────────────────────────

    private void saveAllChanges() {
        String newBodyPart = etBodyPart.getText() != null
                ? etBodyPart.getText().toString().trim() : "";

        if (!newBodyPart.isEmpty()) {
            currentPlan.bodyPart = newBodyPart;
        }
        currentPlan.dayOfWeek = selectedDay;

        boolean dayChanged = !selectedDay.equals(originalDay);

        if (conflictingPlan != null) {
            // SWAP: planA (currentPlan) → selectedDay, planB (conflictingPlan) → originalDay
            conflictingPlan.dayOfWeek = originalDay;
            // swapPlans: delete les 2 + insert les 2 — zero unique constraint crash
            viewModel.swapPlans(currentPlan, conflictingPlan);
            Toast.makeText(this,
                    "Swapped ✅  " + toDayLabel(originalDay) + " ↔ " + toDayLabel(selectedDay),
                    Toast.LENGTH_LONG).show();

        } else if (dayChanged) {
            // Move l nhar jdid bla swap — delete + insert
            viewModel.movePlanToDay(currentPlan);
            Toast.makeText(this, "Moved ✅  → " + toDayLabel(selectedDay),
                    Toast.LENGTH_SHORT).show();

        } else {
            // Gher bodyPart tbdel, nhar b7alu — safe UPDATE bssah
            viewModel.updateGymPlanBodyAndTime(currentPlan);
            Toast.makeText(this, "Saved ✅", Toast.LENGTH_SHORT).show();
        }

        setResult(RESULT_OK);
        finish();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  BACK / UNSAVED CHANGES
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Returns true if user changed something that wasn't saved yet.
     * Rest days are excluded — they have no plan data worth protecting.
     */
    private boolean hasUnsavedChanges() {
        // Bug 3 fix: check current bodyPart text, not cached isRestDay flag
        String currentText = etBodyPart.getText() != null
                ? etBodyPart.getText().toString().trim() : "";
        String originalBodyPart = getIntent().getStringExtra("PLAN_BODY_PART");
        if (originalBodyPart == null) originalBodyPart = "";

        // Day changed?
        if (!selectedDay.equals(originalDay)) return true;

        // Body part changed?
        if (!currentText.equals(originalBodyPart)) return true;

        return false;
    }

    private void handleBack() {
        if (hasUnsavedChanges()) {
            new AlertDialog.Builder(this)
                    .setTitle("Unsaved changes")
                    .setMessage("Kayn tbdilat ma-save-atch. Bghit tkhrej bla ma tsave?")
                    .setPositiveButton("Khrej", (dlg, w) -> finish())
                    .setNegativeButton("Rj3", null)
                    .show();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBack();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────

    private String toDayLabel(String dayKey) {
        for (int i = 0; i < DAYS.length; i++) {
            if (DAYS[i].equals(dayKey)) return DAY_LABELS[i];
        }
        return dayKey;
    }

    // ─── ExerciseEditAdapter ──────────────────────────────────────────────

    private static class ExerciseEditAdapter
            extends RecyclerView.Adapter<ExerciseEditAdapter.VH> {

        interface OnEditClick   { void onEdit(PlannedExercise ex, int pos); }
        interface OnDeleteClick { void onDelete(PlannedExercise ex); }

        private final List<PlannedExercise> list;
        private final OnEditClick           onEdit;
        private final OnDeleteClick         onDelete;

        ExerciseEditAdapter(List<PlannedExercise> list,
                            OnEditClick onEdit, OnDeleteClick onDelete) {
            this.list     = list;
            this.onEdit   = onEdit;
            this.onDelete = onDelete;
        }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_exercise_edit, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            PlannedExercise ex = list.get(position);
            holder.tvName.setText(ex.exerciseName);
            holder.tvDetail.setText(ex.isCardio
                    ? ex.durationMinutes + " min  •  Cardio"
                    : ex.setsTarget + " sets");
            holder.btnEdit.setOnClickListener(v -> onEdit.onEdit(ex, holder.getAdapterPosition()));
            holder.btnDelete.setOnClickListener(v -> onDelete.onDelete(ex));
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView    tvName, tvDetail;
            ImageButton btnEdit, btnDelete;

            VH(View itemView) {
                super(itemView);
                tvName    = itemView.findViewById(R.id.tvExName);
                tvDetail  = itemView.findViewById(R.id.tvExDetail);
                btnEdit   = itemView.findViewById(R.id.btnEditEx);
                btnDelete = itemView.findViewById(R.id.btnDeleteEx);
            }
        }
    }
}