package com.example.noexcuse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);

        String lang = prefs.getString("lang", null);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (lang == null) {
            startActivity(new Intent(this, LanguageActivity.class));
        } else if (!isLoggedIn) {
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }

        finish();
    }
}