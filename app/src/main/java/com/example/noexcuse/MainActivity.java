package com.example.noexcuse;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.noexcuse.database.AppViewModel;
import com.example.noexcuse.database.DailyTask;
import com.example.noexcuse.database.EducationTask;
import com.example.noexcuse.database.GymPlan;
import com.example.noexcuse.database.WeekUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_TASK = 101;
    private static final int REQ_EDU  = 102;

    // ── SharedPreferences keys ────────────────────────────────────────────
    private static final String PREFS_NAME   = "noexcuse_prefs";
    private static final String KEY_GYM_MODE = "gym_mode_enabled";
    private static final String KEY_EDU_MODE = "edu_mode_enabled";

    private FloatingActionButton fabAdd;
    private RecyclerView         recyclerView;
    private TextView             tvDate, tvMotivation;
    private DrawerLayout         drawerLayout;
    private ImageView            btnMenu;
    private Button               btnDash, btnSettings, btnAI;
    private com.google.android.material.button.MaterialButton btnPlanGym;
    private Switch               swGym, swEdu;
    private TaskAdapter          taskAdapter;

    public boolean isGymModeEnabled       = false;
    public boolean isEducationModeEnabled = false;

    private SharedPreferences prefs;
    private AppViewModel      viewModel;

    private List<DailyTask>     cachedDailyTasks = new ArrayList<>();
    private List<EducationTask> cachedEduTasks   = new ArrayList<>();
    private List<GymPlan>       cachedGymPlans   = new ArrayList<>();

    public final Set<String> verifiedIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        btnMenu      = findViewById(R.id.btnMenu);
        fabAdd       = findViewById(R.id.fabAdd);
        recyclerView = findViewById(R.id.recyclerView);
        tvDate       = findViewById(R.id.tvDate);
        tvMotivation = findViewById(R.id.tvMotivation);
        btnDash      = findViewById(R.id.btnDash);
        btnSettings  = findViewById(R.id.btnSettings);
        btnAI        = findViewById(R.id.btnAI);
        btnPlanGym   = findViewById(R.id.btnPlanGym);
        swGym        = findViewById(R.id.swGym);
        swEdu        = findViewById(R.id.swEdu);

        // ── FIX 1: Load saved switch states mn SharedPreferences ──────────
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isGymModeEnabled       = prefs.getBoolean(KEY_GYM_MODE, false);
        isEducationModeEnabled = prefs.getBoolean(KEY_EDU_MODE, false);

        // Set switch UI 9bal ma nzid listener — bach mayfire-ch 3la frag
        swGym.setOnCheckedChangeListener(null);
        swEdu.setOnCheckedChangeListener(null);
        swGym.setChecked(isGymModeEnabled);
        swEdu.setChecked(isEducationModeEnabled);

        // ── FIX 1: Save kol ma tbdel ─────────────────────────────────────
        swGym.setOnCheckedChangeListener((btn, checked) -> {
            isGymModeEnabled = checked;
            prefs.edit().putBoolean(KEY_GYM_MODE, checked).apply();
        });
        swEdu.setOnCheckedChangeListener((btn, checked) -> {
            isEducationModeEnabled = checked;
            prefs.edit().putBoolean(KEY_EDU_MODE, checked).apply();
        });

        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter();
        recyclerView.setAdapter(taskAdapter);

        LayoutAnimationController anim = AnimationUtils.loadLayoutAnimation(
                this, R.anim.recycler_fall_layout);
        recyclerView.setLayoutAnimation(anim);

        viewModel.pendingTasks.observe(this, tasks -> {
            cachedDailyTasks = tasks != null ? tasks : new ArrayList<>();
            refreshAdapter();
        });

        viewModel.pendingEducation.observe(this, tasks -> {
            cachedEduTasks = tasks != null ? tasks : new ArrayList<>();
            refreshAdapter();
        });

        // ── AUTO-COPY: semana jdida → copy plans + exercises mn semana li fazat ──
        String currentWeek  = WeekUtils.getCurrentWeekStart();
        String previousWeek = WeekUtils.getPreviousWeekStart();

        viewModel.copyPlansIfNewWeek(currentWeek, previousWeek, copied -> {
            if (copied) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Plan dyal had semana t-copy mn semana li fazat ✅",
                                Toast.LENGTH_SHORT).show()
                );
            }
        });

        // ★ GYM — observe plans dyal semana had semana
        viewModel.getPlansForWeek(currentWeek).observe(this, plans -> {
            cachedGymPlans = plans != null ? plans : new ArrayList<>();
            refreshAdapter();
        });

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(new Date()));

        String[] quotes = getResources().getStringArray(R.array.motivation_quotes);
        if (quotes.length > 0) {
            tvMotivation.setText(quotes[new Random().nextInt(quotes.length)]);
        }

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
        btnDash.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.END));
        btnSettings.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.END));
        btnAI.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.END));
        btnPlanGym.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            startActivity(new Intent(this, WeekPlanActivity.class));
        });

        fabAdd.setOnClickListener(v -> openSmartAddMenu());
    }

    /**
     * ── FIX 2: Tasks dyal gym + education katl3o DIMA ────────────────────
     * Switch gha kat-control FAB menu wach itle3 option gym/edu,
     * MACHI visibility f liste — tasks kaytl3o dima switch wla la.
     */
    private void refreshAdapter() {
        List<TaskItem> merged = new ArrayList<>();
        verifiedIds.clear();

        // ─── Daily tasks — dima ────────────────────────────────────────
        for (DailyTask t : cachedDailyTasks) {
            merged.add(new TaskItem(t));
            if (t.isDone) verifiedIds.add("DAILY_" + t.id);
        }

        // ─── Education tasks — DIMA (machi conditional 3la switch) ────
        for (EducationTask e : cachedEduTasks) {
            merged.add(new TaskItem(e));
            if (e.isDone) verifiedIds.add("EDU_" + e.id);
        }

        // ─── GYM — gha plan dyal had nhar — DIMA ──────────────────────
        String todayKey = WeekUtils.getTodayDayOfWeek();
        for (GymPlan plan : cachedGymPlans) {
            if (todayKey.equals(plan.dayOfWeek)) {
                if (plan.bodyPart != null && !plan.bodyPart.equals("Rest Day")) {
                    merged.add(new TaskItem(plan));
                }
                break;
            }
        }

        // pending فوق (sorted by time) — done تحت (sorted by time)
        Collections.sort(merged, (a, b) -> {
            boolean aDone = isDoneItem(a);
            boolean bDone = isDoneItem(b);
            if (aDone != bDone) return aDone ? 1 : -1;
            return Long.compare(a.getSortTime(), b.getSortTime());
        });

        taskAdapter.setVerifiedIds(verifiedIds);
        taskAdapter.setItems(merged);
        recyclerView.scheduleLayoutAnimation();
    }

    private boolean isDoneItem(TaskItem item) {
        if (item.type == TaskItem.Type.GYM) return false;
        return item.type == TaskItem.Type.DAILY
                ? item.dailyTask.isDone
                : item.eduTask.isDone;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // LiveData كتحدث لوحدها — refreshAdapter كتتسمى أوتوماتيك
    }

    /**
     * Switch gha kat-control wach itle3 option gym/edu f had menu.
     * Ila off les 2 → directly openDailyTaskDialog.
     */
    private void openSmartAddMenu() {
        if (!isGymModeEnabled && !isEducationModeEnabled) {
            openDailyTaskDialog();
            return;
        }

        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_choose_mode, null);
        sheet.setContentView(view);
        styleBottomSheet(sheet, view);

        MaterialButton btnDaily = view.findViewById(R.id.btnModeDaily);
        MaterialButton btnGym   = view.findViewById(R.id.btnModeGym);
        MaterialButton btnEdu   = view.findViewById(R.id.btnModeEdu);

        btnGym.setVisibility(isGymModeEnabled ? View.VISIBLE : View.GONE);
        btnEdu.setVisibility(isEducationModeEnabled ? View.VISIBLE : View.GONE);

        btnDaily.setOnClickListener(v -> { sheet.dismiss(); openDailyTaskDialog(); });
        btnGym.setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(new Intent(this, GymSetupActivity.class));
        });
        btnEdu.setOnClickListener(v -> { sheet.dismiss(); openEducationDialog(); });

        sheet.show();
    }

    private void openEducationDialog() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_education, null);
        sheet.setContentView(view);
        styleBottomSheet(sheet, view);

        TextInputEditText etModule    = view.findViewById(R.id.etModuleName);
        TextInputEditText etPlan      = view.findViewById(R.id.etStudyPlan);
        TextInputEditText etStartTime = view.findViewById(R.id.etStartTime);
        TextInputEditText etEndTime   = view.findViewById(R.id.etEndTime);
        MaterialButton    btnSave     = view.findViewById(R.id.btnSaveSession);

        Calendar calStart = Calendar.getInstance();
        Calendar calEnd   = Calendar.getInstance();

        etStartTime.setOnClickListener(v ->
                new TimePickerDialog(this, (tp, hour, minute) -> {
                    calStart.set(Calendar.HOUR_OF_DAY, hour);
                    calStart.set(Calendar.MINUTE, minute);
                    calStart.set(Calendar.SECOND, 0);
                    etStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
                }, calStart.get(Calendar.HOUR_OF_DAY), calStart.get(Calendar.MINUTE), true).show()
        );

        etEndTime.setOnClickListener(v ->
                new TimePickerDialog(this, (tp, hour, minute) -> {
                    calEnd.set(Calendar.HOUR_OF_DAY, hour);
                    calEnd.set(Calendar.MINUTE, minute);
                    calEnd.set(Calendar.SECOND, 0);
                    etEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
                }, calEnd.get(Calendar.HOUR_OF_DAY), calEnd.get(Calendar.MINUTE), true).show()
        );

        btnSave.setOnClickListener(v -> {
            String moduleName = etModule.getText() != null ? etModule.getText().toString().trim() : "";
            String studyPlan  = etPlan.getText() != null ? etPlan.getText().toString().trim() : "";
            String startStr   = etStartTime.getText() != null ? etStartTime.getText().toString().trim() : "";
            String endStr     = etEndTime.getText() != null ? etEndTime.getText().toString().trim() : "";

            if (moduleName.isEmpty() || startStr.isEmpty() || endStr.isEmpty()) {
                Toast.makeText(this, "Fill Module, Start & End time", Toast.LENGTH_SHORT).show();
                return;
            }

            EducationTask edu = new EducationTask();
            edu.moduleName  = moduleName;
            edu.studyPlan   = studyPlan;
            edu.startTime   = calStart.getTimeInMillis();
            edu.endTime     = calEnd.getTimeInMillis();
            edu.isFocusMode = false;
            edu.isDone      = false;
            viewModel.addEducation(edu);

            Toast.makeText(this, "Study Session Saved! 📘", Toast.LENGTH_SHORT).show();
            sheet.dismiss();
        });

        sheet.show();
    }

    private void openDailyTaskDialog() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        sheet.setContentView(view);
        styleBottomSheet(sheet, view);

        TextInputEditText etTask  = view.findViewById(R.id.etTaskName);
        TextInputEditText etDesc  = view.findViewById(R.id.etTaskDescription);
        TextInputEditText etTime  = view.findViewById(R.id.etTaskTime);
        MaterialButton    btnSave = view.findViewById(R.id.btnSaveTask);

        Calendar calendar = Calendar.getInstance();

        etTime.setOnClickListener(v ->
                new TimePickerDialog(this, (tp, hour, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);
                    etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        );

        btnSave.setOnClickListener(v -> {
            String taskName = etTask.getText() != null ? etTask.getText().toString().trim() : "";
            String taskDesc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
            String taskTime = etTime.getText() != null ? etTime.getText().toString().trim() : "";

            if (taskName.isEmpty() || taskTime.isEmpty()) {
                Toast.makeText(this, "Fill Name and Time", Toast.LENGTH_SHORT).show();
                return;
            }

            DailyTask task = new DailyTask();
            task.title       = taskName;
            task.description = taskDesc;
            task.taskTime    = calendar.getTimeInMillis();
            task.isDone      = false;
            viewModel.addTask(task);

            Toast.makeText(this, "Task Added! 🔥", Toast.LENGTH_SHORT).show();
            sheet.dismiss();
        });

        sheet.show();
    }

    private void styleBottomSheet(BottomSheetDialog sheet, View view) {
        sheet.setOnShowListener(dialog -> {
            View bottomSheet = (View) view.getParent();
            if (bottomSheet != null) {
                ((View) bottomSheet.getParent()).setBackgroundColor(Color.TRANSPARENT);
                bottomSheet.setBackgroundColor(Color.parseColor("#111111"));
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
    }
}