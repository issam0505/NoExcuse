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

        SharedPreferences langPrefs    = getSharedPreferences("MyApp", MODE_PRIVATE);
        SharedPreferences welcomePrefs = getSharedPreferences("welcomPrefs", MODE_PRIVATE);

        // ✅ اقرا lang بـ null كـ default — باش نعرفو واش المستخدم خيار لغة ولا لا
        String savedLang = langPrefs.getString("lang", null);

        // طبق اللغة — ولو null كنستعملو "en" كـ fallback فغير setLocale
        setLocale(savedLang != null ? savedLang : "en");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            SharedPreferences userPrefs  = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String uid          = userPrefs.getString("uid", null);
            boolean welcomeShown = welcomePrefs.getBoolean("shown", false);

            Intent intent;

            if (savedLang == null) {
                // 1️⃣ مخيارش لغة بعد → LanguageActivity
                intent = new Intent(SplashActivity.this, LanguageActivity.class);
            } else if (uid == null) {
                // 2️⃣ مسجلش → RegisterActivity
                intent = new Intent(SplashActivity.this, RegisterActivity.class);
            } else if (!welcomeShown) {
                // 3️⃣ مازال ماشافش Welcome → WelcomeActivity
                intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            } else {
                // 4️⃣ كل شي تمام → MainActivity
                intent = new Intent(SplashActivity.this, MainActivity.class);
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