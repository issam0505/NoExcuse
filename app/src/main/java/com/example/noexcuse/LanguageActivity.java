package com.example.noexcuse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class LanguageActivity extends AppCompatActivity {

    Button btnFr, btnEn, btnAr;
    CardView cardLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);


        cardLanguage = findViewById(R.id.cardLanguage);
        btnFr = findViewById(R.id.btnFr);
        btnEn = findViewById(R.id.btnEn);
        btnAr = findViewById(R.id.btnAr);

        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade);
        cardLanguage.startAnimation(slideUp);

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