package com.example.noexcuse;

import android.Manifest;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;
import java.util.concurrent.Executors;

public class SleepAlarmFragment extends Fragment {

    private static final int REQ_PERMISSIONS = 201;
    private static final int REQ_OVERLAY_PERMISSION = 202;

    private TextView     tvWakeTime;
    private TextView     tvSleepTime;
    private SwitchMaterial swAlarm;
    private MaterialButton btnSave;

    private int wakeHour   = 7;
    private int wakeMinute = 0;
    private int sleepHour  = 23;
    private int sleepMinute= 0;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sleep_alarm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvWakeTime  = view.findViewById(R.id.tvWakeTime);
        tvSleepTime = view.findViewById(R.id.tvSleepTime);
        swAlarm     = view.findViewById(R.id.swAlarmEnabled);
        btnSave     = view.findViewById(R.id.btnSaveAlarm);

        // ✅ تحميل الإعدادات المحفوظة عند فتح الواجهة
        loadSavedSettings();

        tvWakeTime.setOnClickListener(v -> pickTime(true));
        tvSleepTime.setOnClickListener(v -> pickTime(false));
        
        // ✅ حفظ الحالة فوراً عند تغيير المفتاح
        swAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> persistSelection());

        btnSave.setOnClickListener(v -> checkAndSave());
    }

    private void loadSavedSettings() {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (getContext() == null) return;
            SleepSettings saved = AppDatabase.getInstance(requireContext()).sleepDao().getSleepSettings();
            
            if (saved != null) {
                try {
                    String[] wake = saved.wakeUpTime.split(":");
                    wakeHour = Integer.parseInt(wake[0]);
                    wakeMinute = Integer.parseInt(wake[1]);
                    
                    String[] sleep = saved.sleepTime.split(":");
                    sleepHour = Integer.parseInt(sleep[0]);
                    sleepMinute = Integer.parseInt(sleep[1]);
                } catch (Exception ignored) {}

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvWakeTime.setText(saved.wakeUpTime);
                        tvSleepTime.setText(saved.sleepTime);
                        swAlarm.setChecked(saved.isAlarmOn);
                    });
                }
            }
        });
    }

    private void pickTime(boolean isWakeUp) {
        int hour = isWakeUp ? wakeHour : sleepHour;
        int minute = isWakeUp ? wakeMinute : sleepMinute;
        
        new TimePickerDialog(requireContext(), (tp, h, m) -> {
            if (isWakeUp) {
                wakeHour = h;
                wakeMinute = m;
                tvWakeTime.setText(String.format(Locale.US, "%02d:%02d", h, m));
            } else {
                sleepHour = h;
                sleepMinute = m;
                tvSleepTime.setText(String.format(Locale.US, "%02d:%02d", h, m));
            }
            // ✅ حفظ الاختيار فوراً لكي لا يضيع عند إغلاق التطبيق
            persistSelection();
        }, hour, minute, true).show();
    }

    private void persistSelection() {
        if (getContext() == null) return;
        boolean alarmOn = swAlarm.isChecked();
        Executors.newSingleThreadExecutor().execute(() -> {
            SleepSettings s = new SleepSettings();
            s.id = 1; // استخدام نفس المعرف لتحديث السجل الموجود دائماً
            // ✅ استخدام Locale.US لضمان تخزين أرقام لاتينية (0-9) دائماً
            s.sleepTime   = String.format(Locale.US, "%02d:%02d", sleepHour, sleepMinute);
            s.wakeUpTime  = String.format(Locale.US, "%02d:%02d", wakeHour, wakeMinute);
            s.isAlarmOn   = alarmOn;
            s.isQRRequired= true;
            AppDatabase.getInstance(requireContext()).sleepDao().insertSleepSettings(s);
        });
    }

    private void checkAndSave() {
        if (!swAlarm.isChecked()) {
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
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_PERMISSIONS);
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                return;
            }
        }

        applyAndSave(true);
    }

    private void applyAndSave(boolean alarmOn) {
        if (alarmOn) {
            WakeUpScheduler.schedule(requireContext(), wakeHour, wakeMinute);
            Toast.makeText(requireContext(), "⏰ Alarm Saved & Active!", Toast.LENGTH_SHORT).show();
        } else {
            WakeUpScheduler.cancel(requireContext());
            Toast.makeText(requireContext(), "Alarm Disabled", Toast.LENGTH_SHORT).show();
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
