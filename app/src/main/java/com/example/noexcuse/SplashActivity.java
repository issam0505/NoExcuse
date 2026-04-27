package com.example.noexcuse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. طبق اللغة قبل ما يبان حتى شي حاجة
        SharedPreferences langPrefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        String lang = langPrefs.getString("lang", "en"); // "en" هي الديفولت
        setLocale(lang);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 2. التوجيه (Routing)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String uid = userPrefs.getString("uid", null);
            Intent intent;

            if (langPrefs.getString("lang", null) == null) {
                intent = new Intent(SplashActivity.this, LanguageActivity.class);
            } else if (uid == null) {
                intent = new Intent(SplashActivity.this, RegisterActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            }

            startActivity(intent);
            finish();

        }, 2000);
    }
    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }
}