package com.example.noexcuse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 1. جيب الـ SharedPreferences ديال اللغة وديال المستخدم
            SharedPreferences langPrefs = getSharedPreferences("MyApp", MODE_PRIVATE);
            SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

            String lang = langPrefs.getString("lang", null);
            String uid = userPrefs.getString("uid", null); // كنجيبو الـ UID اللي خازنين حنا

            Intent intent;

            // 2. المنطق:
            if (lang == null) {
                // إيلا ماختارش اللغة
                intent = new Intent(SplashActivity.this, LanguageActivity.class);
            } else if (uid == null) {
                // إيلا ماكانش الـ UID مخزن، يعني ماكاينش Login، صيفطو للـ Register
                intent = new Intent(SplashActivity.this, RegisterActivity.class);
            } else {
                // إيلا كاين الـ UID، يعني User ديجا Logged in، صيفطو للـ Main
                intent = new Intent(SplashActivity.this, MainActivity.class);
            }

            startActivity(intent);
            finish();

        }, 2000); // 2 ثواني
    }
}