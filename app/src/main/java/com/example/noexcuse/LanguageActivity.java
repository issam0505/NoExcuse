package com.example.noexcuse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class LanguageActivity extends AppCompatActivity {

    Button btnFr, btnEn, btnAr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        btnFr = findViewById(R.id.btnFr);
        btnEn = findViewById(R.id.btnEn);
        btnAr = findViewById(R.id.btnAr);

        btnFr.setOnClickListener(v -> saveLang("fr"));
        btnEn.setOnClickListener(v -> saveLang("en"));
        btnAr.setOnClickListener(v -> saveLang("ar"));
    }

    private void saveLang(String lang) {
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("lang", lang);
        editor.apply();

        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}