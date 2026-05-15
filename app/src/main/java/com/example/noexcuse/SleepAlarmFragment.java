package com.example.noexcuse;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import com.example.noexcuse.database.AppDatabase;
import com.example.noexcuse.database.SleepSettings;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * SleepAlarmFragment
 * ──────────────────────────────────────────────────────────────────
 * Allows the user to set:
 *   - Wake-up time (with a clock picker)
 *   - Sleep time (informational only, saved to DB for AI use)
 *   - Enable/disable the alarm
 *
 * Rules:
 *  • Only ONE alarm saved (not additive) — Save overwrites any existing one
 *  • Navigating back to this screen loads the saved time automatically
 *  • Camera permission asked only when alarm is enabled + QR mode active
 */
public class SleepAlarmFragment extends Fragment {

    private static final int CAMERA_REQ = 201;

    // UI
    private TextView     tvWakeTime;
    private TextView     tvSleepTime;
    private SwitchMaterial swAlarm;
    private MaterialButton btnSave;
    private MaterialButton btnQrInfo;

    // State
    private int wakeHour   = 7;
    private int wakeMinute = 0;
    private int sleepHour  = 23;
    private int sleepMinute= 0;
    private boolean pendingCameraCheck = false;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sleep_alarm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvWakeTime  = view.findViewById(R.id.tvWakeTime);
        tvSleepTime = view.findViewById(R.id.tvSleepTime);
        swAlarm     = view.findViewById(R.id.swAlarmEnabled);
        btnSave     = view.findViewById(R.id.btnSaveAlarm);
        btnQrInfo   = view.findViewById(R.id.btnQrInfo);

        // Load saved settings from DB (background)
        loadSavedSettings();

        // Wake-up time picker
        tvWakeTime.setOnClickListener(v -> pickTime(true));

        // Sleep time picker
        tvSleepTime.setOnClickListener(v -> pickTime(false));

        // Save button
        btnSave.setOnClickListener(v -> saveSettings());

        // QR info button
        if (btnQrInfo != null) {
            btnQrInfo.setOnClickListener(v -> showQrInfoDialog());
        }

        // Update switch state from WakeUpScheduler
        if (getContext() != null) {
            swAlarm.setChecked(WakeUpScheduler.isEnabled(requireContext()));
        }
    }

    private void pickTime(boolean isWakeUp) {
        int hour   = isWakeUp ? wakeHour   : sleepHour;
        int minute = isWakeUp ? wakeMinute : sleepMinute;

        TimePickerDialog dialog = new TimePickerDialog(requireContext(), (tp, h, m) -> {
            if (isWakeUp) {
                wakeHour   = h;
                wakeMinute = m;
                tvWakeTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
            } else {
                sleepHour   = h;
                sleepMinute = m;
                tvSleepTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
            }
        }, hour, minute, true);

        dialog.setTitle(isWakeUp ? "Wake-up time ⏰" : "Sleep time 🌙");
        dialog.show();
    }

    private void saveSettings() {
        boolean alarmOn = swAlarm.isChecked();

        if (alarmOn) {
            // Check camera permission first (needed for QR scanning)
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                pendingCameraCheck = true;
                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQ);
                return;
            }
        }

        applyAndSave(alarmOn);
    }

    private void applyAndSave(boolean alarmOn) {
        if (alarmOn) {
            WakeUpScheduler.schedule(requireContext(), wakeHour, wakeMinute);
            Toast.makeText(requireContext(),
                    String.format(Locale.getDefault(),
                            "⏰ Alarm set for %02d:%02d", wakeHour, wakeMinute),
                    Toast.LENGTH_SHORT).show();
        } else {
            WakeUpScheduler.cancel(requireContext());
            Toast.makeText(requireContext(), "Alarm disabled", Toast.LENGTH_SHORT).show();
        }

        // Persist to SleepSettings DB (for AI gym features)
        Executors.newSingleThreadExecutor().execute(() -> {
            SleepSettings s = new SleepSettings();
            s.sleepTime   = String.format(Locale.getDefault(), "%02d:%02d", sleepHour, sleepMinute);
            s.wakeUpTime  = String.format(Locale.getDefault(), "%02d:%02d", wakeHour, wakeMinute);
            s.isAlarmOn   = alarmOn;
            s.isQRRequired= true; // Always QR required
            s.lastSleepDuration = 0;

            AppDatabase db = AppDatabase.getInstance(requireContext());
            // Delete old and insert fresh (only 1 record allowed)
            db.sleepDao().insertSleepSettings(s);
        });
    }

    private void loadSavedSettings() {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (getContext() == null) return;
            SleepSettings saved = AppDatabase.getInstance(requireContext())
                    .sleepDao().getSleepSettings();

            if (saved != null) {
                // Parse times
                try {
                    String[] wake  = saved.wakeUpTime.split(":");
                    String[] sleep = saved.sleepTime.split(":");
                    wakeHour   = Integer.parseInt(wake[0]);
                    wakeMinute = Integer.parseInt(wake[1]);
                    sleepHour  = Integer.parseInt(sleep[0]);
                    sleepMinute= Integer.parseInt(sleep[1]);
                } catch (Exception ignored) {}
            }

            // Update UI on main thread
            requireActivity().runOnUiThread(() -> {
                tvWakeTime.setText(String.format(Locale.getDefault(),
                        "%02d:%02d", wakeHour, wakeMinute));
                tvSleepTime.setText(String.format(Locale.getDefault(),
                        "%02d:%02d", sleepHour, sleepMinute));
            });
        });
    }

    private void showQrInfoDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("📱 How does the QR alarm work?")
                .setMessage("1. The app provides a QR code sticker — print it and stick it in your bathroom or toilet.\n\n"
                        + "2. When your alarm fires, tap STOP to pause it.\n\n"
                        + "3. After 5 minutes, you'll get a notification: \"Are you awake?\"\n\n"
                        + "4. Tap YES to confirm — or the alarm will ring again!\n\n"
                        + "5. If you still don't respond, the alarm fires again — and this time you MUST scan the QR code in the bathroom to dismiss it.\n\n"
                        + "This guarantees you actually got out of bed! 💪")
                .setPositiveButton("Got it!", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQ) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (granted) {
                if (pendingCameraCheck) {
                    pendingCameraCheck = false;
                    applyAndSave(true);
                }
            } else {
                Toast.makeText(requireContext(),
                        "Camera permission is required for QR scan alarm. Please enable it in Settings.",
                        Toast.LENGTH_LONG).show();
                // Don't save — go back
            }
        }
    }
}