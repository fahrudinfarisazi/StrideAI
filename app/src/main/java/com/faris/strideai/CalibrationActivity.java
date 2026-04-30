package com.faris.strideai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class CalibrationActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Menyambungkan Java ini ke file desain XML Form Kalibrasi
        setContentView(R.layout.activity_calibration);

        // 2. Mencari tombol Synchronize berdasarkan ID-nya
        Button btnSync = findViewById(R.id.btnSync);

        // --- Fitur Pilih Gender ---
        com.google.android.material.card.MaterialCardView btnMale = findViewById(R.id.btnMale);
        com.google.android.material.card.MaterialCardView btnFemale = findViewById(R.id.btnFemale);
        com.google.android.material.card.MaterialCardView btnOther = findViewById(R.id.btnOther);

        android.widget.TextView tvMaleIcon = findViewById(R.id.tvMaleIcon);
        android.widget.TextView tvFemaleIcon = findViewById(R.id.tvFemaleIcon);
        android.widget.TextView tvOtherIcon = findViewById(R.id.tvOtherIcon);

        // Variable untuk menyimpan gender yang dipilih (default: Male)
        final String[] selectedGender = {"Male"};

        btnMale.setOnClickListener(v -> {
            selectedGender[0] = "Male";
            updateGenderUI(btnMale, btnFemale, btnOther, tvMaleIcon, tvFemaleIcon, tvOtherIcon, "Male");
        });

        btnFemale.setOnClickListener(v -> {
            selectedGender[0] = "Female";
            updateGenderUI(btnMale, btnFemale, btnOther, tvMaleIcon, tvFemaleIcon, tvOtherIcon, "Female");
        });

        btnOther.setOnClickListener(v -> {
            selectedGender[0] = "Other";
            updateGenderUI(btnMale, btnFemale, btnOther, tvMaleIcon, tvFemaleIcon, tvOtherIcon, "Other");
        });
        // --------------------------

        // 3. Memasang "telinga" agar tombol bereaksi saat diklik
        btnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.widget.EditText etHeight = findViewById(R.id.etHeight);
                android.widget.EditText etWeight = findViewById(R.id.etWeight);
                
                String heightStr = etHeight.getText().toString().trim();
                String weightStr = etWeight.getText().toString().trim();

                // Validation: Prevent continuing if empty
                if (heightStr.isEmpty() || weightStr.isEmpty()) {
                    android.widget.Toast.makeText(CalibrationActivity.this, "Tinggi dan Berat badan harus diisi!", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                // Menyimpan data profil (biometrik) untuk kalibrasi AI
                android.content.SharedPreferences prefs = getSharedPreferences("StrideAI_Prefs", MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putString("gender", selectedGender[0]);
                editor.putString("height", heightStr);
                editor.putString("weight", weightStr);
                editor.putBoolean("isCalibrated", true);
                editor.apply();

                // 4. Perintah untuk pindah ke MainActivity (Dashboard)
                Intent pindahKeDashboard = new Intent(CalibrationActivity.this, MainActivity.class);
                startActivity(pindahKeDashboard);

                // Menutup halaman form agar saat user pencet tombol 'Back' HP, tidak balik lagi ke form ini
                finish();
            }
        });

        // 5. Fitur Skip (I'LL DO THIS LATER)
        android.widget.TextView btnDoThisLater = findViewById(R.id.btnDoThisLater);
        if (btnDoThisLater != null) {
            btnDoThisLater.setOnClickListener(v -> {
                // Menandai bahwa sudah lewat layar kalibrasi (walau belum diisi lengkap) agar tidak terus-terusan muncul
                android.content.SharedPreferences prefs = getSharedPreferences("StrideAI_Prefs", MODE_PRIVATE);
                prefs.edit().putBoolean("isCalibrated", true).apply();

                Intent pindahKeDashboard = new Intent(CalibrationActivity.this, MainActivity.class);
                startActivity(pindahKeDashboard);
                finish();
            });
        }
    }

    private void updateGenderUI(com.google.android.material.card.MaterialCardView m, 
                                com.google.android.material.card.MaterialCardView f, 
                                com.google.android.material.card.MaterialCardView o,
                                android.widget.TextView im, 
                                android.widget.TextView ife, 
                                android.widget.TextView io,
                                String gender) {
        
        int neonBlue = getResources().getColor(R.color.neon_blue);
        int white = android.graphics.Color.WHITE;

        // Reset all
        m.setStrokeWidth(0);
        f.setStrokeWidth(0);
        o.setStrokeWidth(0);
        im.setTextColor(white);
        ife.setTextColor(white);
        io.setTextColor(white);

        // Highlight selected
        if (gender.equals("Male")) {
            m.setStrokeWidth(dpToPx(1));
            im.setTextColor(neonBlue);
        } else if (gender.equals("Female")) {
            f.setStrokeWidth(dpToPx(1));
            ife.setTextColor(neonBlue);
        } else {
            o.setStrokeWidth(dpToPx(1));
            io.setTextColor(neonBlue);
        }
    }



    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
