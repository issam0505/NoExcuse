package com.example.noexcuse.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sleep_settings")
public class SleepSettings {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String sleepTime;       // 11:00 PM
    public String wakeUpTime;      // 08:00 AM
    public boolean isAlarmOn;
    public boolean isQRRequired;   // واش ضروري يسكاني الـ QR في بيت الراحة
    public long lastSleepDuration; // شحال نعس (باش الـ AI ديال الـ Gym يستعملها)
}