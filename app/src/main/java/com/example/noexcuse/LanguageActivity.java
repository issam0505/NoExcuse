package com.example.noexcuse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

public class LanguageActivity extends AppCompatActivity {

    MaterialButton btnFr, btnEn, btnAr;
    CardView cardLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        cardLanguage = findViewById(R.id.cardLanguage);
        btnFr = findViewById(R.id.btnFr);
        btnEn = findViewById(R.id.btnEn);
        btnAr = findViewById(R.id.btnAr);

        // Animation
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade);
        cardLanguage.startAnimation(slideUp);

        btnFr.setOnClickListener(v -> selectButton(btnFr, "fr"));
        btnEn.setOnClickListener(v -> selectButton(btnEn, "en"));
        btnAr.setOnClickListener(v -> selectButton(btnAr, "ar"));
    }

    private void selectButton(MaterialButton selectedBtn, String lang) {

        MaterialButton[] buttons = {btnFr, btnEn, btnAr};

        for (MaterialButton b : buttons) {
            b.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#3D3D3D")));
            b.setStrokeWidth(1);
        }

        selectedBtn.setStrokeColor(ColorStateList.valueOf(Color.WHITE));
        selectedBtn.setStrokeWidth(4);

        selectedBtn.postDelayed(() -> saveLang(lang), 300);
    }

    private void saveLang(String lang) {

        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        prefs.edit().putString("lang", lang).apply();

        // نمر مباشرة للـ Register (أول مرة)
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
    }
}