package com.example.noexcuse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import com.example.noexcuse.database.AppDatabase;
import com.example.noexcuse.database.User;
import java.util.Locale;
public class RegisterActivity extends AppCompatActivity {

    EditText etFirstName, etLastName, etEmail, etBirthDate, etPassword, etConfirmPassword;
    Button btnRegister;
    AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyLanguage();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etBirthDate = findViewById(R.id.etBirthDate);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "NoExcuseDB")
                .allowMainThreadQueries().fallbackToDestructiveMigration().build();

        btnRegister.setOnClickListener(v -> {
            String fName = etFirstName.getText().toString();
            String lName = etLastName.getText().toString();
            String email = etEmail.getText().toString();
            String birthDate = etBirthDate.getText().toString();
            String pass = etPassword.getText().toString();
            String confirm = etConfirmPassword.getText().toString();

            if (fName.isEmpty() || lName.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_fields), Toast.LENGTH_SHORT).show();
            } else if (!pass.equals(confirm)) {
                Toast.makeText(this, getString(R.string.error_pass_match), Toast.LENGTH_SHORT).show();
            } else {
                User user = new User();
                user.firstName = fName; // مطابق للـ Entity
                user.lastName = lName;  // مطابق للـ Entity
                user.email = email;
                user.password = pass;
                user.birthDate = birthDate;

                db.userDao().insert(user);
                getSharedPreferences("MyApp", MODE_PRIVATE).edit().putBoolean("isRegistered", true).apply();

                Toast.makeText(this, getString(R.string.success_msg), Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
        });
    }

    private void applyLanguage() {
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        String lang = prefs.getString("lang", "en");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}