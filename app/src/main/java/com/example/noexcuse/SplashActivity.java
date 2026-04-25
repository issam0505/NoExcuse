package com.example.noexcuse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
                String lang = prefs.getString("lang", null);

                Intent intent;
                if (lang == null) {
                    intent = new Intent(SplashActivity.this, LanguageActivity.class);
                } else if (mAuth.getCurrentUser() == null) {
                    intent = new Intent(SplashActivity.this, RegisterActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, RegisterActivity.class);
                }

                startActivity(intent);
                finish();

            } catch (Exception e) {
               System.out.println(e.getMessage());
            }
        }, 2000); // 2 swani bach l-user ychouf loading screen
    }
}
