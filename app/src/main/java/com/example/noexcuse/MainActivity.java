package com.example.noexcuse;

import android.Manifest;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.noexcuse.database.AppViewModel;
import com.example.noexcuse.database.DailyTask;
import com.example.noexcuse.database.EducationTask;
import com.example.noexcuse.database.GymPlan;
import com.example.noexcuse.database.WeekUtils;
import com.example.noexcuse.utils.NotificationHelper;
import com.example.noexcuse.utils.NotifScheduler;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final int    REQ_TASK   = 101;
    private static final int    REQ_EDU    = 102;
    private static final String PREFS_NAME = "noexcuse_prefs";
    private static final String KEY_GYM    = "gym_mode_enabled";
    private static final String KEY_EDU    = "edu_mode_enabled";
    private static final String KEY_NOTIF_ASKED = "notif_permission_asked";

    private ActivityResultLauncher<String> notifPermissionLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private FloatingActionButton fabAdd;
    private RecyclerView         recyclerView;
    private TextView             tvDate, tvMotivation;
    private DrawerLayout         drawerLayout;
    private ImageView            btnMenu, iconClock;
    private Button               btnDash, btnSettings, btnAI;
    private com.google.android.material.button.MaterialButton btnPlanGym;
    private Switch               swGym, swEdu;
    private TaskAdapter          taskAdapter;

    public boolean isGymModeEnabled       = false;
    public boolean isEducationModeEnabled = false;

    private AppViewModel viewModel;

    private List<DailyTask>     cachedDailyTasks = new ArrayList<>();
    private List<EducationTask> cachedEduTasks   = new ArrayList<>();
    private List<GymPlan>       cachedGymPlans   = new ArrayList<>();

    public final Set<String> verifiedIds = new HashSet<>();

    private String                   observedWeek  = null;
    private Observer<List<GymPlan>>  gymObserver   = null;
    private LiveData<List<GymPlan>>  gymLiveData   = null;
    private BottomSheetDialog profileSheet;
    private TextView tvProfileName, tvProfileEmail, tvProfileBirthday, tvProfileCity, tvProfileUid, tvProfileStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationHelper.createChannels(this);

        notifPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit().putBoolean(KEY_NOTIF_ASKED, true).apply();
                    if (isGranted) {
                        Toast.makeText(this, "Notifications enabled ✅", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Notifications disabled ❌", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (fine || coarse) {
                        updateProfileCity();
                    } else if (tvProfileCity != null) {
                        tvProfileCity.setText("Location permission denied");
                    }
                }
        );

        drawerLayout = findViewById(R.id.drawer_layout);
        btnMenu      = findViewById(R.id.btnMenu);
        iconClock    = findViewById(R.id.iconClock);
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

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(new Date()));

        String[] quotes = getResources().getStringArray(R.array.motivation_quotes);
        if (quotes.length > 0) {
            tvMotivation.setText(quotes[new Random().nextInt(quotes.length)]);
        }

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
        
        // ─── Launch AlarmActivity from iconClock ──────────────────────────
        iconClock.setOnClickListener(v -> {
            Intent intent = new Intent(this, AlarmActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnDash.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.END));
        btnSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            showProfileSettings();
        });
        btnAI.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            startActivity(new Intent(this, AiChatActivity.class));
        });
        btnPlanGym.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            startActivity(new Intent(this, WeekPlanActivity.class));
        });

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isGymModeEnabled       = prefs.getBoolean(KEY_GYM, false);
        isEducationModeEnabled = prefs.getBoolean(KEY_EDU, false);

        swGym.setOnCheckedChangeListener(null);
        swEdu.setOnCheckedChangeListener(null);
        swGym.setChecked(isGymModeEnabled);
        swEdu.setChecked(isEducationModeEnabled);

        swGym.setOnCheckedChangeListener((btn, checked) -> {
            isGymModeEnabled = checked;
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().putBoolean(KEY_GYM, checked).apply();
            refreshAdapter();
        });
        swEdu.setOnCheckedChangeListener((btn, checked) -> {
            isEducationModeEnabled = checked;
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().putBoolean(KEY_EDU, checked).apply();
            refreshAdapter();
        });

        fabAdd.setOnClickListener(v -> openSmartAddMenu());

        askNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.deleteExpiredTasks();

        String currentWeek = WeekUtils.getCurrentWeekStart();

        if (gymLiveData != null && gymObserver != null) {
            gymLiveData.removeObserver(gymObserver);
        }

        observedWeek = currentWeek;

        gymObserver = plans -> {
            cachedGymPlans = plans != null ? plans : new ArrayList<>();
            refreshAdapter();
        };

        gymLiveData = viewModel.getPlansForWeek(currentWeek);
        gymLiveData.observe(this, gymObserver);
    }

    private void refreshAdapter() {
        List<TaskItem> merged = new ArrayList<>();
        verifiedIds.clear();

        for (DailyTask t : cachedDailyTasks) {
            merged.add(new TaskItem(t));
            if (t.isDone) verifiedIds.add("DAILY_" + t.id);
        }

        if (isEducationModeEnabled) {
            for (EducationTask e : cachedEduTasks) {
                merged.add(new TaskItem(e));
                if (e.isDone) verifiedIds.add("EDU_" + e.id);
            }
        }

        if (isGymModeEnabled) {
            String todayKey = WeekUtils.getTodayDayOfWeek();
            for (GymPlan plan : cachedGymPlans) {
                if (todayKey.equals(plan.dayOfWeek)) {
                    if (plan.bodyPart != null && !plan.bodyPart.equals("Rest Day")) {
                        merged.add(new TaskItem(plan));

                        if (plan.startTime != null && !plan.startTime.isEmpty()) {
                            try {
                                String[] parts = plan.startTime.split(":");
                                Calendar gymCal = Calendar.getInstance();
                                gymCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
                                gymCal.set(Calendar.MINUTE,      Integer.parseInt(parts[1]));
                                gymCal.set(Calendar.SECOND, 0);
                                long gymMillis = gymCal.getTimeInMillis();
                                NotifScheduler.schedule(this, "GYM", plan.id, plan.bodyPart,
                                        gymMillis, plan.startTime);
                            } catch (Exception ignored) { }
                        }
                    }
                    break;
                }
            }
        }

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
    }

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

            if (calEnd.getTimeInMillis() <= calStart.getTimeInMillis()) {
                calEnd.add(Calendar.DAY_OF_MONTH, 1);
            }
            edu.endTime     = calEnd.getTimeInMillis();
            edu.isFocusMode = false;
            edu.isDone      = false;
            viewModel.addEducation(edu);

            SimpleDateFormat sdfE = new SimpleDateFormat("HH:mm", Locale.getDefault());
            NotifScheduler.schedule(this, "EDU", edu.id, edu.moduleName,
                    edu.startTime, sdfE.format(new Date(edu.startTime)));

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

            SimpleDateFormat sdfT = new SimpleDateFormat("HH:mm", Locale.getDefault());
            NotifScheduler.schedule(this, "TASK", task.id, task.title,
                    task.taskTime, sdfT.format(new Date(task.taskTime)));

            Toast.makeText(this, "Task Added! 🔥", Toast.LENGTH_SHORT).show();
            sheet.dismiss();
        });

        sheet.show();
    }

    private void showProfileSettings() {
        profileSheet = new BottomSheetDialog(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(22), dp(22), dp(20));
        content.setBackgroundColor(Color.rgb(17, 17, 17));

        TextView title = new TextView(this);
        title.setText("Profile");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(title);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(0, dp(18), 0, dp(18));

        TextView avatar = new TextView(this);
        avatar.setText("NE");
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(20f);
        avatar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        avatar.setGravity(android.view.Gravity.CENTER);
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(Color.rgb(76, 175, 80));
        avatar.setBackground(avatarBg);
        header.addView(avatar, new LinearLayout.LayoutParams(dp(64), dp(64)));

        LinearLayout nameBox = new LinearLayout(this);
        nameBox.setOrientation(LinearLayout.VERTICAL);
        nameBox.setPadding(dp(14), 0, 0, 0);
        tvProfileName = new TextView(this);
        tvProfileName.setText("Loading user...");
        tvProfileName.setTextColor(Color.WHITE);
        tvProfileName.setTextSize(20f);
        tvProfileName.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tvProfileEmail = new TextView(this);
        tvProfileEmail.setText("Email loading...");
        tvProfileEmail.setTextColor(Color.rgb(170, 170, 170));
        tvProfileEmail.setTextSize(14f);
        nameBox.addView(tvProfileName);
        nameBox.addView(tvProfileEmail);
        header.addView(nameBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        content.addView(header);

        tvProfileBirthday = makeProfileValue("Birthday", "Loading...");
        tvProfileCity = makeProfileValue("City", "Checking location...");
        tvProfileUid = makeProfileValue("UID", getUserUid());
        content.addView(tvProfileBirthday);
        content.addView(tvProfileCity);
        content.addView(tvProfileUid);
        tvProfileStatus = new TextView(this);
        tvProfileStatus.setText("Fetching MongoDB profile...");
        tvProfileStatus.setTextColor(Color.rgb(120, 120, 120));
        tvProfileStatus.setTextSize(13f);
        tvProfileStatus.setPadding(0, dp(10), 0, 0);
        content.addView(tvProfileStatus);

        MaterialButton refresh = new MaterialButton(this);
        refresh.setText("Refresh");
        refresh.setAllCaps(false);
        refresh.setTextColor(Color.WHITE);
        refresh.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(76, 175, 80)));
        refresh.setOnClickListener(v -> {
            loadProfileFromMongo();
            requestLocationForProfile();
        });
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        refreshParams.setMargins(0, dp(18), 0, 0);
        content.addView(refresh, refreshParams);

        profileSheet.setContentView(content);
        styleBottomSheet(profileSheet, content);
        profileSheet.show();

        loadProfileFromMongo();
        requestLocationForProfile();
    }

    private TextView makeProfileValue(String label, String value) {
        TextView view = new TextView(this);
        view.setText(label + "\n" + value);
        view.setTextColor(Color.WHITE);
        view.setTextSize(15f);
        view.setLineSpacing(3f, 1.0f);
        view.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(28, 28, 28));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.rgb(48, 48, 48));
        view.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        view.setLayoutParams(params);
        return view;
    }

    private void loadProfileFromMongo() {
        String uid = getUserUid();
        tvProfileUid.setText("UID\n" + uid);
        if ("local_user".equals(uid)) {
            tvProfileStatus.setText("No saved UID found in SharedPreferences.");
            return;
        }

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getUserData(uid).enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(Call<java.util.Map<String, Object>> call, Response<java.util.Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    tvProfileStatus.setText("Could not load MongoDB profile.");
                    return;
                }

                java.util.Map<String, Object> user = response.body();
                String firstName = valueAsString(user.get("firstName"), "");
                String lastName = valueAsString(user.get("lastName"), "");
                String email = valueAsString(user.get("email"), "No email");
                String birthDate = valueAsString(user.get("birthDate"), "No birthday");
                String fullName = (firstName + " " + lastName).trim();
                if (fullName.isEmpty()) fullName = "NoExcuse User";

                tvProfileName.setText(fullName);
                tvProfileEmail.setText(email);
                tvProfileBirthday.setText("Birthday\n" + birthDate);
                tvProfileStatus.setText("MongoDB profile loaded.");

                getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        .edit()
                        .putString("firstName", firstName)
                        .putString("lastName", lastName)
                        .putString("email", email)
                        .putString("birthDate", birthDate)
                        .apply();
            }

            @Override
            public void onFailure(Call<java.util.Map<String, Object>> call, Throwable t) {
                tvProfileStatus.setText("Network error while loading MongoDB profile.");
            }
        });
    }

    private void requestLocationForProfile() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (fine || coarse) {
            updateProfileCity();
            return;
        }

        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void updateProfileCity() {
        if (tvProfileCity == null) return;
        tvProfileCity.setText("City\nLocating...");

        new Thread(() -> {
            String city = resolveCurrentCity();
            runOnUiThread(() -> tvProfileCity.setText("City\n" + city));
        }).start();
    }

    private String resolveCurrentCity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return "Location permission needed";
        }

        try {
            LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (manager == null) return "Location unavailable";

            Location location = null;
            for (String provider : manager.getProviders(true)) {
                Location candidate = manager.getLastKnownLocation(provider);
                if (candidate != null && (location == null || candidate.getTime() > location.getTime())) {
                    location = candidate;
                }
            }
            if (location == null) return "Location unavailable";

            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses == null || addresses.isEmpty()) return "Unknown city";

            Address address = addresses.get(0);
            String city = address.getLocality();
            if (city == null || city.trim().isEmpty()) city = address.getSubAdminArea();
            if (city == null || city.trim().isEmpty()) city = address.getAdminArea();
            return city != null && !city.trim().isEmpty() ? city : "Unknown city";
        } catch (Exception e) {
            return "Location unavailable";
        }
    }

    private String getUserUid() {
        String uid = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("uid", null);
        return uid != null && !uid.trim().isEmpty() ? uid : "local_user";
    }

    private String valueAsString(Object value, String fallback) {
        if (value == null) return fallback;
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equals(text) ? fallback : text;
    }

    private void askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean alreadyAsked = prefs.getBoolean(KEY_NOTIF_ASKED, false);
        if (alreadyAsked) return;

        boolean alreadyGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;

        if (!alreadyGranted) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            prefs.edit().putBoolean(KEY_NOTIF_ASKED, true).apply();
        }
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
