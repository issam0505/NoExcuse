package com.example.noexcuse;

import android.Manifest;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.noexcuse.database.AppDatabase;
import com.example.noexcuse.database.SleepSettings;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;
import java.util.concurrent.Executors;

public class SleepAlarmFragment extends Fragment {

    private static final int REQ_PERMISSIONS        = 201;
    private static final int REQ_OVERLAY_PERMISSION = 202;

    // Colors
    private static final int COLOR_INACTIVE   = 0xFF444444;
    private static final int COLOR_STATUS_ON  = 0xFF4CAF82;
    private static final int COLOR_STATUS_OFF = 0xFF2A2A2A;

    private TextView       tvWakeTime;
    private TextView       tvSleepTime;
    private TextView       tvAlarmStatus;
    private TextView       tvSleepDuration;
    private View           cardWakeTime;
    private View           cardSleepTime;
    private View           viewStatusDot;
    private com.google.android.material.switchmaterial.SwitchMaterial switchAlarm;
    private MaterialButton btnSave;

    private int     wakeHour    = 7;
    private int     wakeMinute  = 0;
    private int     sleepHour   = 23;
    private int     sleepMinute = 0;
    private boolean isAlarmOn   = false;

    // Prevents saving during DB load
    private boolean isLoadingData = false;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sleep_alarm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvWakeTime      = view.findViewById(R.id.tvWakeTime);
        tvSleepTime     = view.findViewById(R.id.tvSleepTime);
        tvAlarmStatus   = view.findViewById(R.id.tvAlarmStatus);
        tvSleepDuration = view.findViewById(R.id.tvSleepDuration);
        cardWakeTime    = view.findViewById(R.id.cardWakeTime);
        cardSleepTime   = view.findViewById(R.id.cardSleepTime);
        viewStatusDot   = view.findViewById(R.id.viewStatusDot);
        switchAlarm     = view.findViewById(R.id.switchAlarm);
        btnSave         = view.findViewById(R.id.btnSaveAlarm);

        cardWakeTime.setOnClickListener(v -> pickTime(true));
        cardSleepTime.setOnClickListener(v -> pickTime(false));

        switchAlarm.setOnCheckedChangeListener((btn, checked) -> {
            if (isLoadingData) return;
            isAlarmOn = checked;
            updateToggleUI();
            persistSelection();
        });

        btnSave.setOnClickListener(v -> checkAndSave());
        loadSavedSettings();
    }

    // ── UI Updates ─────────────────────────────────────────────────

    private void updateToggleUI() {
        // Sync switch state without triggering listener
        switchAlarm.setOnCheckedChangeListener(null);
        switchAlarm.setChecked(isAlarmOn);
        switchAlarm.setOnCheckedChangeListener((btn, checked) -> {
            if (isLoadingData) return;
            isAlarmOn = checked;
            updateToggleUI();
            persistSelection();
        });

        if (isAlarmOn) {
            tvAlarmStatus.setText("Alarm is active");
            tvAlarmStatus.setTextColor(COLOR_STATUS_ON);
            viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(COLOR_STATUS_ON));
        } else {
            tvAlarmStatus.setText("Alarm is off");
            tvAlarmStatus.setTextColor(COLOR_INACTIVE);
            viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(COLOR_STATUS_OFF));
        }
        updateSleepDuration();
    }

    /**
     * Calculates and displays the sleep duration between sleepTime and wakeTime.
     * Handles overnight spans (e.g. 23:00 → 07:00 = 8h 00m).
     * Also shows the recommended bedtime based on entered times.
     */
    private void updateSleepDuration() {
        int sleepTotalMins = sleepHour * 60 + sleepMinute;
        int wakeTotalMins  = wakeHour  * 60 + wakeMinute;

        int diffMins = wakeTotalMins - sleepTotalMins;
        if (diffMins <= 0) diffMins += 24 * 60; // overnight wrap

        int hours = diffMins / 60;
        int mins  = diffMins % 60;

        if (tvSleepDuration != null) {
            String duration = (mins == 0)
                    ? String.format(Locale.US, "%dh 00m", hours)
                    : String.format(Locale.US, "%dh %02dm", hours, mins);
            tvSleepDuration.setText(duration);
        }
    }

    // ── Data Loading ───────────────────────────────────────────────

    private void loadSavedSettings() {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (getContext() == null) return;
            SleepSettings saved = AppDatabase.getInstance(requireContext())
                    .sleepDao()
                    .getSleepSettings();

            if (saved != null) {
                try {
                    String[] wake  = saved.wakeUpTime.split(":");
                    wakeHour   = Integer.parseInt(wake[0]);
                    wakeMinute = Integer.parseInt(wake[1]);

                    String[] sleep = saved.sleepTime.split(":");
                    sleepHour   = Integer.parseInt(sleep[0]);
                    sleepMinute = Integer.parseInt(sleep[1]);
                } catch (Exception ignored) {}

                isAlarmOn = saved.isAlarmOn;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isLoadingData = true;
                        tvWakeTime.setText(saved.wakeUpTime);
                        tvSleepTime.setText(saved.sleepTime);
                        updateToggleUI();
                        isLoadingData = false;
                        updateSleepDuration(); // recalculate after real values are set
                    });
                }
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateToggleUI();
                        updateSleepDuration();
                    });
                }
            }
        });
    }

    // ── Time Picker ────────────────────────────────────────────────

    private void pickTime(boolean isWakeUp) {
        int hour   = isWakeUp ? wakeHour   : sleepHour;
        int minute = isWakeUp ? wakeMinute : sleepMinute;

        new TimePickerDialog(requireContext(), (tp, h, m) -> {
            if (isWakeUp) {
                wakeHour   = h;
                wakeMinute = m;
                tvWakeTime.setText(String.format(Locale.US, "%02d:%02d", h, m));
            } else {
                sleepHour   = h;
                sleepMinute = m;
                tvSleepTime.setText(String.format(Locale.US, "%02d:%02d", h, m));
            }
            updateSleepDuration();
            persistSelection();
        }, hour, minute, true).show();
    }

    // ── Persistence ────────────────────────────────────────────────

    private void persistSelection() {
        if (getContext() == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            SleepSettings s = new SleepSettings();
            s.id           = 1;
            s.sleepTime    = String.format(Locale.US, "%02d:%02d", sleepHour,  sleepMinute);
            s.wakeUpTime   = String.format(Locale.US, "%02d:%02d", wakeHour,   wakeMinute);
            s.isAlarmOn    = isAlarmOn;
            s.isQRRequired = true;
            AppDatabase.getInstance(requireContext()).sleepDao().insertSleepSettings(s);
        });
    }

    // ── Save + Permission checks ───────────────────────────────────

    private void checkAndSave() {
        if (!isAlarmOn) {
            applyAndSave(false);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(requireContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + requireContext().getPackageName()));
                startActivityForResult(intent, REQ_OVERLAY_PERMISSION);
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_PERMISSIONS);
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) requireContext()
                    .getSystemService(Context.ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                return;
            }
        }

        applyAndSave(true);
    }

    private void applyAndSave(boolean alarmOn) {
        if (alarmOn) {
            WakeUpScheduler.schedule(requireContext(), wakeHour, wakeMinute);
            Toast.makeText(requireContext(), "⏰ Alarm saved & active!", Toast.LENGTH_SHORT).show();
        } else {
            WakeUpScheduler.cancel(requireContext());
            Toast.makeText(requireContext(), "Alarm disabled", Toast.LENGTH_SHORT).show();
        }
        persistSelection();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_OVERLAY_PERMISSION) {
            checkAndSave();
        }
    }
}
