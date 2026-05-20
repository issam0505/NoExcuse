package com.example.noexcuse;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.noexcuse.database.AppViewModel;
import com.example.noexcuse.database.EducationTask;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class EducationDetailActivity extends AppCompatActivity {

    public static final String EXTRA_VERIFIED_ID = "VERIFIED_EDU_ID";
    private Switch swFocusMode;
    private String currentModuleName = "";

    // Launcher for Notification Permission (Android 13+)
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    checkUsageAndStartService();
                } else {
                    Toast.makeText(this, "Notification permission denied. Focus alerts won't show.", Toast.LENGTH_LONG).show();
                    swFocusMode.setChecked(false);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_education_detail);

        TextView       tvTitle       = findViewById(R.id.tvTitle);
        TextView       tvDesc        = findViewById(R.id.tvDesc);
        TextView       tvStartTime   = findViewById(R.id.tvStartTime);
        TextView       tvEndTime     = findViewById(R.id.tvEndTime);
        TextView       tvStatus      = findViewById(R.id.tvStatus);
        FrameLayout    btnBack       = findViewById(R.id.btnBack);
        MaterialButton btnDone       = findViewById(R.id.btnDone);
        MaterialButton btnDelete     = findViewById(R.id.btnDelete);
        ProgressBar    progressBar   = findViewById(R.id.progressBar);
        LinearLayout   contentLayout = findViewById(R.id.contentLayout);

        DrawerLayout drawerLayout    = findViewById(R.id.drawer_layout);
        android.widget.ImageView btnMenu = findViewById(R.id.btnMenu);
        Button       btnStudyHelper  = findViewById(R.id.btnStudyHelper);

        swFocusMode = findViewById(R.id.swFocusMode);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        int eduId = getIntent().getIntExtra("EDU_ID", -1);
        if (eduId == -1) {
            finish();
            return;
        }

        AppViewModel viewModel = new ViewModelProvider(this).get(AppViewModel.class);
        btnBack.setOnClickListener(v -> finish());

        Executors.newSingleThreadExecutor().execute(() -> {
            EducationTask edu = viewModel.getEducationById(eduId);

            runOnUiThread(() -> {
                if (edu == null) {
                    finish();
                    return;
                }

                currentModuleName = edu.moduleName;
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                tvTitle.setText(edu.moduleName);
                tvDesc.setText(edu.studyPlan != null && !edu.studyPlan.isEmpty() ? edu.studyPlan : "No study plan");
                tvStartTime.setText(sdf.format(new Date(edu.startTime)));
                tvEndTime.setText(sdf.format(new Date(edu.endTime)));

                swFocusMode.setChecked(edu.isFocusMode);

                swFocusMode.setOnCheckedChangeListener((btn, checked) -> {
                    if (checked) {
                        handleFocusModeEnable();
                    } else {
                        stopFocusService();
                        edu.isFocusMode = false;
                        viewModel.updateEducation(edu);
                    }
                });

                // Status UI logic...
                if (edu.isDone) {
                    tvStatus.setText("Done");
                    tvStatus.setBackgroundResource(R.drawable.bg_status_done);
                    btnDone.setEnabled(false);
                }

                btnDone.setOnClickListener(v -> {
                    edu.isDone = true;
                    viewModel.updateEducation(edu);
                    if (edu.isFocusMode) stopFocusService();
                    setResult(Activity.RESULT_OK);
                    finish();
                });
            });
        });
    }

    private void handleFocusModeEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        checkUsageAndStartService();
    }

    private void checkUsageAndStartService() {
        if (hasUsageStatsPermission()) {
            startFocusService(currentModuleName);
            Toast.makeText(this, "Focus Mode ON 🎯", Toast.LENGTH_SHORT).show();
        } else {
            swFocusMode.setChecked(false);
            showUsagePermissionDialog();
        }
    }

    private void startFocusService(String moduleName) {
        Intent intent = new Intent(this, FocusMonitorService.class);
        intent.setAction(FocusMonitorService.ACTION_START);
        intent.putExtra(FocusMonitorService.EXTRA_MODULE, moduleName);
        ContextCompat.startForegroundService(this, intent);
    }

    private void stopFocusService() {
        Intent intent = new Intent(this, FocusMonitorService.class);
        intent.setAction(FocusMonitorService.ACTION_STOP);
        startService(intent);
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager aom = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = aom.noteOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void showUsagePermissionDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Permission Required")
                .setMessage("Focus Mode needs 'Usage Access' to detect distractions.")
                .setPositiveButton("Settings", (d, w) -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
