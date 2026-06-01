package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ExploreActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);
        
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navBooks).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
            
        findViewById(R.id.navExplore).setOnClickListener(v -> {
            // Đã ở trang khám phá
        });

        findViewById(R.id.navChallenge).setOnClickListener(v -> {
            startActivity(new Intent(this, ChallengeActivity.class));
            finish();
        });

        findViewById(R.id.navLibrary).setOnClickListener(v -> {
            startActivity(new Intent(this, LibraryActivity.class));
            finish();
        });
    }
}
