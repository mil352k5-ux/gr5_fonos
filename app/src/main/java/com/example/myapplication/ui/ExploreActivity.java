package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.utils.MiniPlayerController;

public class ExploreActivity extends AppCompatActivity {
    
    private MiniPlayerController miniPlayerController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);
        
        findViewById(R.id.cardAudiobooks).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.putExtra("initial_tab", "audiobook");
            startActivity(intent);
            finish();
        });

        findViewById(R.id.cardEbooks).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.putExtra("initial_tab", "ebook");
            startActivity(intent);
            finish();
        });

        findViewById(R.id.cardSummary).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.putExtra("initial_tab", "summary");
            startActivity(intent);
            finish();
        });

        // Other Category Cards
        findViewById(R.id.cardPodCourse).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchAndCategoryActivity.class);
            intent.putExtra("category_filter", "PodCourse");
            startActivity(intent);
        });

        findViewById(R.id.cardEnglishBooks).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchAndCategoryActivity.class);
            intent.putExtra("category_filter", "Sách Tiếng Anh");
            startActivity(intent);
        });

        findViewById(R.id.cardMeditation).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchAndCategoryActivity.class);
            intent.putExtra("category_filter", "Thiền");
            startActivity(intent);
        });

        findViewById(R.id.cardSleepMusic).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchAndCategoryActivity.class);
            intent.putExtra("category_filter", "Truyện Ngủ");
            startActivity(intent);
        });

        findViewById(R.id.cardPodcast).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchAndCategoryActivity.class);
            intent.putExtra("category_filter", "Podcast");
            startActivity(intent);
        });

        // Search Edit Text
        android.widget.EditText etSearch = findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.setOnEditorActionListener((view, actionId, event) -> {
                String query = etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    Intent intent = new Intent(ExploreActivity.this, SearchAndCategoryActivity.class);
                    intent.putExtra("search_query", query);
                    startActivity(intent);
                }
                return true;
            });
        }

        // Profile Icon click
        View profileIcon = findViewById(R.id.profile_icon);
        if (profileIcon != null) {
            profileIcon.setOnClickListener(v -> {
                Intent intent = new Intent(ExploreActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        setupBottomNavigation();

        miniPlayerController = new MiniPlayerController(this);
        miniPlayerController.init();
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

    @Override
    protected void onStart() {
        super.onStart();
        if (miniPlayerController != null) {
            miniPlayerController.onStart();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (miniPlayerController != null) {
            miniPlayerController.onStop();
        }
    }
}
