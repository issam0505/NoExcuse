package com.example.noexcuse;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.noexcuse.database.AppViewModel;
import com.example.noexcuse.database.GymPlan;
import com.example.noexcuse.database.PlannedExercise;
import com.example.noexcuse.database.WeekUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeekPlanActivity extends AppCompatActivity {

    private static final String[] DAYS = {
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY",
            "FRIDAY", "SATURDAY", "SUNDAY"
    };
    private static final String[] DAY_LABELS = {
            "Monday", "Tuesday", "Wednesday", "Thursday",
            "Friday", "Saturday", "Sunday"
    };

    // ─── UI ───────────────────────────────────────────────────────────────
    private TextView  tvWeekRange;
    private TextView  tvTotalExercises;
    private TextView  tvTodayWorkout;
    private ImageView ivTodayIcon;
    private TextView  tvActiveDays;

    private ExtendedFloatingActionButton fabSave;
    private FrameLayout btnEditMode, btnBack;

    private final Map<String, DayCardHolder> cardMap = new HashMap<>();

    // ─── Data ─────────────────────────────────────────────────────────────
    private AppViewModel viewModel;
    private String       currentWeekStart;

    private final Map<String, GymPlan>  workoutPlanMap  = new HashMap<>();
    private final Map<Integer, Integer> planExCountMap  = new HashMap<>();

    // ─── FIX: cache exercise LiveData bach manzidouch observers f kol update ──
    private final Map<Integer, LiveData<List<PlannedExercise>>> exerciseLiveDataMap = new HashMap<>();

    private boolean editModeActive = false;

    private static final int[] CARD_ANIM_RES = {
            R.anim.card_flip_in,
            R.anim.card_zoom_spring,
            R.anim.card_swing_in,
            R.anim.card_flip_in,
            R.anim.card_zoom_spring,
            R.anim.card_swing_in,
            R.anim.card_zoom_spring
    };

    private static final long CARD_STAGGER_DELAY_MS = 80;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_week_plan);

        currentWeekStart = WeekUtils.getCurrentWeekStart();
        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        bindViews();
        setupWeekHeader();
        setupCardHolders();
        observePlans();

        btnBack.setOnClickListener(v -> finish());
        btnEditMode.setOnClickListener(v -> toggleEditMode());

        fabSave.setOnClickListener(v -> {
            Toast.makeText(this, "Plan saved ✅", Toast.LENGTH_SHORT).show();
            toggleEditMode();
        });
    }

    // onResume — Room LiveData automatic, makhassnach ndir ay haja hna

    // ─────────────────────────────────────────────────────────────────────
    //  BIND
    // ─────────────────────────────────────────────────────────────────────

    private void bindViews() {
        tvWeekRange      = findViewById(R.id.tvWeekRange);
        tvTotalExercises = findViewById(R.id.tvTotalExercises);
        tvTodayWorkout   = findViewById(R.id.tvTodayWorkout);
        ivTodayIcon      = findViewById(R.id.ivTodayIcon);
        tvActiveDays     = findViewById(R.id.tvActiveDays);
        fabSave          = findViewById(R.id.fabSave);
        btnEditMode      = findViewById(R.id.btnEditMode);
        btnBack          = findViewById(R.id.btnBack);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HEADER
    // ─────────────────────────────────────────────────────────────────────

    private void setupWeekHeader() {
        try {
            SimpleDateFormat sdfIn  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat sdfOut = new SimpleDateFormat("dd MMM", Locale.ENGLISH);
            Date monDate = sdfIn.parse(currentWeekStart);
            Calendar cal = Calendar.getInstance();
            cal.setTime(monDate);
            String monStr = sdfOut.format(monDate);
            cal.add(Calendar.DAY_OF_WEEK, 6);
            String sunStr = sdfOut.format(cal.getTime()) + " " + cal.get(Calendar.YEAR);
            tvWeekRange.setText(monStr + " – " + sunStr);
        } catch (ParseException ignored) {
            tvWeekRange.setText(currentWeekStart);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SETUP 7 CARDS
    // ─────────────────────────────────────────────────────────────────────

    private void setupCardHolders() {
        int[] cardResIds = {
                R.id.cardMonday, R.id.cardTuesday, R.id.cardWednesday,
                R.id.cardThursday, R.id.cardFriday, R.id.cardSaturday,
                R.id.cardSunday
        };

        for (int i = 0; i < 7; i++) {
            MaterialCardView card = findViewById(cardResIds[i]);
            card.setAlpha(0f);

            DayCardHolder holder = new DayCardHolder(card, DAYS[i], DAY_LABELS[i]);
            cardMap.put(DAYS[i], holder);
            holder.setRestDay();

            final int index   = i;
            final int animRes = CARD_ANIM_RES[i];
            card.postDelayed(() -> {
                card.setAlpha(1f);
                Animation anim = AnimationUtils.loadAnimation(this, animRes);
                anim.setFillAfter(true);
                card.startAnimation(anim);
            }, index * CARD_STAGGER_DELAY_MS);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  OBSERVE PLANS
    // ─────────────────────────────────────────────────────────────────────

    private void observePlans() {
        viewModel.getPlansForWeek(currentWeekStart).observe(this, plans -> {

            workoutPlanMap.clear();
            planExCountMap.clear();
            // ─── FIX: clear exercise LiveData cache f kol plans update ────
            exerciseLiveDataMap.clear();

            for (String day : DAYS) {
                DayCardHolder h = cardMap.get(day);
                if (h != null) h.setRestDay();
            }

            updateTodayCard(null);

            if (plans == null || plans.isEmpty()) {
                refreshGlobalStats();
                return;
            }

            String todayKey = WeekUtils.getTodayDayOfWeek();

            for (GymPlan plan : plans) {
                boolean isRest = "Rest Day".equals(plan.bodyPart) || plan.bodyPart == null;

                if (!isRest) {
                    workoutPlanMap.put(plan.dayOfWeek, plan);
                }

                DayCardHolder holder = cardMap.get(plan.dayOfWeek);
                if (holder == null) continue;

                if (isRest) {
                    holder.setRestDay(plan);
                } else {
                    holder.setWorkoutDay(plan);
                }

                if (todayKey.equals(plan.dayOfWeek)) {
                    updateTodayCard(isRest ? null : plan);
                }

                if (!isRest) {
                    // ─── FIX: observe marra wahda gher — manzidouch observers ────
                    if (!exerciseLiveDataMap.containsKey(plan.id)) {
                        LiveData<List<PlannedExercise>> exLiveData =
                                viewModel.getExercisesForPlan(plan.id);
                        exerciseLiveDataMap.put(plan.id, exLiveData);

                        exLiveData.observe(this, exercises -> {
                            if (exercises == null) return;
                            holder.bindExercises(exercises);
                            holder.setStats(exercises.size());
                            planExCountMap.put(plan.id, exercises.size());
                            refreshGlobalStats();
                        });
                    }
                }
            }

            refreshGlobalStats();
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    //  GLOBAL STATS
    // ─────────────────────────────────────────────────────────────────────

    private void refreshGlobalStats() {
        int totalEx = 0;
        for (int c : planExCountMap.values()) totalEx += c;
        tvTotalExercises.setText(String.valueOf(totalEx));

        int activeDays = workoutPlanMap.size();
        tvActiveDays.setText(String.valueOf(activeDays));
    }

    private void updateTodayCard(GymPlan todayPlan) {
        if (todayPlan == null) {
            tvTodayWorkout.setText("Rest");
            tvTodayWorkout.setTextColor(0xFF6B7280);
            ivTodayIcon.setImageResource(R.drawable.ic_fitness_center);
            ivTodayIcon.setColorFilter(0xFF6B7280);
        } else {
            tvTodayWorkout.setText(todayPlan.bodyPart);
            tvTodayWorkout.setTextColor(0xFFFF6D00);
            ivTodayIcon.setImageResource(R.drawable.ic_fitness_center);
            ivTodayIcon.setColorFilter(0xFFFF6D00);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  EDIT MODE
    // ─────────────────────────────────────────────────────────────────────

    private void toggleEditMode() {
        editModeActive = !editModeActive;

        for (String day : DAYS) {
            DayCardHolder h = cardMap.get(day);
            if (h != null) h.setEditMode(editModeActive);
        }

        fabSave.setVisibility(editModeActive ? View.VISIBLE : View.GONE);

        ImageView ivEditIcon = btnEditMode.findViewById(R.id.ivEditIcon);
        if (ivEditIcon != null) {
            ivEditIcon.setImageResource(editModeActive
                    ? R.drawable.close_24px
                    : R.drawable.ic_edit);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  INNER — DayCardHolder
    // ─────────────────────────────────────────────────────────────────────

    private class DayCardHolder {

        final MaterialCardView card;
        final String           dayKey;
        final String           dayLabel;

        final RelativeLayout rowHeader;
        final LinearLayout   layoutExercises;
        final LinearLayout   rowStats;
        final LinearLayout   rowEditActions;
        final View           accentBar;
        final TextView       tvDayName;
        final TextView       tvBodyPart;
        final TextView       tvStartTime;
        final TextView       tvExCount;
        final ImageView      ivExpand;
        final View           btnEditTime;
        final View           btnEditExercises;

        boolean expanded  = false;
        boolean isRestDay = true;
        GymPlan currentPlan = null;

        DayCardHolder(MaterialCardView card, String dayKey, String dayLabel) {
            this.card     = card;
            this.dayKey   = dayKey;
            this.dayLabel = dayLabel;

            rowHeader       = card.findViewById(R.id.rowHeader);
            layoutExercises = card.findViewById(R.id.layoutExercises);
            rowStats        = card.findViewById(R.id.rowStats);
            rowEditActions  = card.findViewById(R.id.rowEditActions);
            accentBar       = card.findViewById(R.id.accentBar);
            tvDayName       = card.findViewById(R.id.tvDayName);
            tvBodyPart      = card.findViewById(R.id.tvBodyPart);
            tvStartTime     = card.findViewById(R.id.tvStartTime);
            tvExCount       = card.findViewById(R.id.tvExCount);
            ivExpand        = card.findViewById(R.id.ivExpand);
            btnEditTime      = card.findViewById(R.id.btnEditTime);
            btnEditExercises = card.findViewById(R.id.btnEditExercises);

            tvDayName.setText(dayLabel.toUpperCase());

            rowHeader.setOnClickListener(v -> toggleExpand());
            btnEditTime.setOnClickListener(v -> showTimePickerForPlan());

            btnEditExercises.setOnClickListener(v -> {
                if (currentPlan == null) {
                    GymPlan restPlan = new GymPlan();
                    restPlan.dayOfWeek     = dayKey;
                    restPlan.weekStartDate = currentWeekStart;
                    restPlan.bodyPart      = "Rest Day";
                    restPlan.startTime     = "";

                    viewModel.addGymPlan(restPlan, newId -> runOnUiThread(() -> {
                        restPlan.id  = newId;
                        currentPlan  = restPlan;

                        Intent intent = new Intent(WeekPlanActivity.this, EditDayPlanActivity.class);
                        intent.putExtra("PLAN_ID",         newId);
                        intent.putExtra("PLAN_DAY",        dayKey);
                        intent.putExtra("PLAN_BODY_PART",  "Rest Day");
                        intent.putExtra("PLAN_START_TIME", "");
                        intent.putExtra("WEEK_START",      currentWeekStart);
                        startActivity(intent);
                    }));
                    return;
                }
                Intent intent = new Intent(WeekPlanActivity.this, EditDayPlanActivity.class);
                intent.putExtra("PLAN_ID",         currentPlan.id);
                intent.putExtra("PLAN_DAY",        currentPlan.dayOfWeek);
                intent.putExtra("PLAN_BODY_PART",  currentPlan.bodyPart);
                intent.putExtra("PLAN_START_TIME", currentPlan.startTime != null ? currentPlan.startTime : "");
                intent.putExtra("WEEK_START",      currentWeekStart);
                startActivity(intent);
            });
        }

        void setRestDay() {
            isRestDay = true;
            tvBodyPart.setText("Rest Day");
            tvBodyPart.setTextColor(0xFF6B7280);
            tvStartTime.setText("—");
            accentBar.setBackgroundColor(0xFF2A2A2A);
            card.setStrokeColor(0xFF2A2A2A);
            layoutExercises.setVisibility(View.GONE);
            rowStats.setVisibility(View.GONE);
            ivExpand.setVisibility(View.GONE);
        }

        void setRestDay(GymPlan plan) {
            currentPlan = plan;
            setRestDay();
        }

        void setWorkoutDay(GymPlan plan) {
            isRestDay   = false;
            currentPlan = plan;

            tvBodyPart.setText(plan.bodyPart != null ? plan.bodyPart : "Gym");
            tvBodyPart.setTextColor(0xFFF9FAFB);
            tvStartTime.setText(plan.startTime != null && !plan.startTime.isEmpty()
                    ? plan.startTime : "--:--");

            accentBar.setBackgroundColor(0xFFFF6D00);
            card.setStrokeColor(0xFF2A2A2A);
            ivExpand.setVisibility(View.VISIBLE);
        }

        void setStats(int exCount) {
            tvExCount.setText(exCount + (exCount == 1 ? " exercise" : " exercises"));
            rowStats.setVisibility(!isRestDay ? View.VISIBLE : View.GONE);
        }

        void bindExercises(List<PlannedExercise> exercises) {
            layoutExercises.removeAllViews();
            for (PlannedExercise ex : exercises) {
                TextView tv = new TextView(WeekPlanActivity.this);
                String label = ex.isCardio
                        ? "• " + ex.exerciseName + "  (" + ex.durationMinutes + " min cardio)"
                        : "• " + ex.exerciseName + "  ×" + ex.setsTarget + " sets";
                tv.setText(label);
                tv.setTextColor(0xFF9CA3AF);
                tv.setTextSize(13f);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = 8;
                tv.setLayoutParams(lp);
                layoutExercises.addView(tv);
            }
        }

        void toggleExpand() {
            if (isRestDay) return;
            expanded = !expanded;

            float from = expanded ? 0f : 180f;
            float to   = expanded ? 180f : 0f;
            RotateAnimation rotate = new RotateAnimation(from, to,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(200);
            rotate.setFillAfter(true);
            ivExpand.startAnimation(rotate);

            layoutExercises.setVisibility(expanded ? View.VISIBLE : View.GONE);
        }

        void setEditMode(boolean active) {
            if (!active) {
                rowEditActions.setVisibility(View.GONE);
                return;
            }
            rowEditActions.setVisibility(View.VISIBLE);
            btnEditTime.setVisibility(isRestDay ? View.GONE : View.VISIBLE);
        }

        private void showTimePickerForPlan() {
            if (currentPlan == null) return;

            int initHour = 7, initMin = 0;
            if (currentPlan.startTime != null && currentPlan.startTime.contains(":")) {
                try {
                    String[] parts = currentPlan.startTime.split(":");
                    initHour = Integer.parseInt(parts[0]);
                    initMin  = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {}
            }

            new TimePickerDialog(WeekPlanActivity.this, (tp, hour, minute) -> {
                String newTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                currentPlan.startTime = newTime;
                viewModel.updateGymPlan(currentPlan);
                tvStartTime.setText(newTime);
                Toast.makeText(WeekPlanActivity.this,
                        dayLabel + " time updated ✅", Toast.LENGTH_SHORT).show();
            }, initHour, initMin, true).show();
        }
    }
}