package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ChallengeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);

        Button btnExplore = findViewById(R.id.btnExploreAudiobooks);
        if (btnExplore != null) {
            btnExplore.setOnClickListener(v -> {
                startActivity(new Intent(this, ExploreActivity.class));
                finish();
            });
        }

        View profileIcon = findViewById(R.id.profile_icon);
        if (profileIcon != null) {
            profileIcon.setOnClickListener(v -> Toast.makeText(this, "Mở trang hồ sơ cá nhân", Toast.LENGTH_SHORT).show());
        }

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navBooks).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
            
        findViewById(R.id.navExplore).setOnClickListener(v -> {
            startActivity(new Intent(this, ExploreActivity.class));
            finish();
        });

        findViewById(R.id.navChallenge).setOnClickListener(v -> {
            // Đã ở trang thử thách
        });

        findViewById(R.id.navLibrary).setOnClickListener(v -> {
            startActivity(new Intent(this, LibraryActivity.class));
            finish();
        });
    }
}
