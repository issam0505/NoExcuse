package com.example.noexcuse;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AlarmActivity extends AppCompatActivity {

    private TimePicker timePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        timePicker = findViewById(R.id.timePicker);
        Button btnSave = findViewById(R.id.btnSaveAlarm);

        btnSave.setOnClickListener(v -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();

            // هنا غادي تخدم بـ AlarmManager باش تبرمج المنبه
            Toast.makeText(this, "Alarm set for: " + hour + ":" + minute, Toast.LENGTH_SHORT).show();
            finish(); // باش يرجع لـ MainActivity
        });
    }
}