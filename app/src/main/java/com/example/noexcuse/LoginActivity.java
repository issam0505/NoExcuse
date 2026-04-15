package com.example.noexcuse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putBoolean("isLoggedIn", true);
            editor.apply();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}