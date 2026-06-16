package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.controller.FonosApiManager;
import com.example.myapplication.model.Book;
import com.example.myapplication.utils.SupabaseConfig;
import com.example.myapplication.utils.MiniPlayerController;
import com.example.myapplication.adapter.LibraryAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LibraryActivity extends AppCompatActivity {

    private FonosApiManager apiManager;
    private RecyclerView rvLibraryBooks;
    private LinearLayout layoutEmptyState;
    private LibraryAdapter libraryAdapter;
    private List<Book> bookList;
    private MiniPlayerController miniPlayerController;

    private enum LibTab {
        RECENT, FAVORITES
    }
    private LibTab activeTab = LibTab.RECENT;

    private View tabRecent, tabFavorites;
    private TextView ivTabRecentCircle, ivTabFavoritesCircle;
    private TextView tvTabRecentLabel, tvTabFavoritesLabel;
    private TextView tvLibraryTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        apiManager = new FonosApiManager(this);
        rvLibraryBooks = findViewById(R.id.rvLibraryBooks);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        tabRecent = findViewById(R.id.tabRecent);
        tabFavorites = findViewById(R.id.tabFavorites);
        ivTabRecentCircle = findViewById(R.id.ivTabRecentCircle);
        ivTabFavoritesCircle = findViewById(R.id.ivTabFavoritesCircle);
        tvTabRecentLabel = findViewById(R.id.tvTabRecentLabel);
        tvTabFavoritesLabel = findViewById(R.id.tvTabFavoritesLabel);
        tvLibraryTitle = findViewById(R.id.tvLibraryTitle);

        bookList = new ArrayList<>();
        libraryAdapter = new LibraryAdapter(bookList, new LibraryAdapter.OnBookClickListener() {
            @Override
            public void onBookClick(Book book) {
                Intent intent = new Intent(LibraryActivity.this, BookDetailActivity.class);
                intent.putExtra("id", book.getId());
                intent.putExtra("title", book.getTitle());
                intent.putExtra("author", book.getAuthor());
                intent.putExtra("description", book.getDescription());
                intent.putExtra("category", book.getCategory());
                intent.putExtra("cover_url", book.getCoverUrl());
                intent.putExtra("book_type", "audiobook");
                startActivity(intent);
            }

            @Override
            public void onPlayClick(Book book) {
                Intent intent = new Intent(LibraryActivity.this, PlayerActivity.class);
                intent.putExtra("book_id", book.getId());
                intent.putExtra("title", book.getTitle());
                intent.putExtra("author", book.getAuthor());
                intent.putExtra("cover_url", book.getCoverUrl());
                startActivity(intent);
            }
        });

        rvLibraryBooks.setLayoutManager(new LinearLayoutManager(this));
        rvLibraryBooks.setAdapter(libraryAdapter);

        if (tabRecent != null) {
            tabRecent.setOnClickListener(v -> switchTab(LibTab.RECENT));
        }
        if (tabFavorites != null) {
            tabFavorites.setOnClickListener(v -> switchTab(LibTab.FAVORITES));
        }

        View profileIcon = findViewById(R.id.profile_icon);
        if (profileIcon != null) {
            profileIcon.setOnClickListener(v -> {
                Intent intent = new Intent(LibraryActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        setupBottomNavigation();
        switchTab(LibTab.RECENT);

        miniPlayerController = new MiniPlayerController(this);
        miniPlayerController.init();
    }

    private void switchTab(LibTab tab) {
        activeTab = tab;

        // Reset all styles
        if (ivTabRecentCircle != null) {
            ivTabRecentCircle.setBackgroundResource(R.drawable.circle_background_grey);
            ivTabRecentCircle.setTextColor(getResources().getColor(android.R.color.white));
        }
        if (tvTabRecentLabel != null) {
            tvTabRecentLabel.setTextColor(0xFFAFC7E4);
            tvTabRecentLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        if (ivTabFavoritesCircle != null) {
            ivTabFavoritesCircle.setBackgroundResource(R.drawable.circle_background_grey);
            ivTabFavoritesCircle.setTextColor(getResources().getColor(android.R.color.white));
        }
        if (tvTabFavoritesLabel != null) {
            tvTabFavoritesLabel.setTextColor(0xFFAFC7E4);
            tvTabFavoritesLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        // Apply selected styles and load data
        if (tab == LibTab.RECENT) {
            if (ivTabRecentCircle != null) {
                ivTabRecentCircle.setBackgroundResource(R.drawable.circle_background);
                ivTabRecentCircle.setTextColor(0xFFFF7043);
            }
            if (tvTabRecentLabel != null) {
                tvTabRecentLabel.setTextColor(0xFFFFFFFF);
                tvTabRecentLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            }
            if (tvLibraryTitle != null) {
                tvLibraryTitle.setText("Gần đây");
            }
            loadLibraryBooks();
        } else if (tab == LibTab.FAVORITES) {
            if (ivTabFavoritesCircle != null) {
                ivTabFavoritesCircle.setBackgroundResource(R.drawable.circle_background);
                ivTabFavoritesCircle.setTextColor(0xFFFF7043);
            }
            if (tvTabFavoritesLabel != null) {
                tvTabFavoritesLabel.setTextColor(0xFFFFFFFF);
                tvTabFavoritesLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            }
            loadFavoriteBooks();
        }
    }

    private void loadFavoriteBooks() {
        bookList.clear();
        SharedPreferences favoritesPref = getSharedPreferences("FonosFavorites", MODE_PRIVATE);

        Map<String, ?> allEntries = favoritesPref.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("book_info_")) {
                String bookJson = (String) entry.getValue();
                try {
                    JSONObject obj = new JSONObject(bookJson);
                    Book book = new Book(
                            obj.optString("id", ""),
                            obj.optString("title", "No title"),
                            obj.optString("author", "Unknown author"),
                            obj.optString("narrator", "Fonos Voice"),
                            obj.optString("description", ""),
                            obj.optString("category", "General"),
                            obj.optString("cover_url", ""),
                            obj.optInt("total_duration", 0),
                            obj.optBoolean("is_premium", false)
                    );
                    bookList.add(book);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        libraryAdapter.notifyDataSetChanged();

        if (tvLibraryTitle != null) {
            tvLibraryTitle.setText("Yêu thích (" + bookList.size() + ")");
        }

        if (bookList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvLibraryBooks.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvLibraryBooks.setVisibility(View.VISIBLE);
        }
    }

    private void loadLibraryBooks() {
        bookList.clear();
        SharedPreferences recentsPref = getSharedPreferences("FonosRecents", MODE_PRIVATE);
        String recentBooksJson = recentsPref.getString("recent_books", "[]");

        try {
            JSONArray array = new JSONArray(recentBooksJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Book book = new Book(
                        obj.optString("id", ""),
                        obj.optString("title", "No title"),
                        obj.optString("author", "Unknown author"),
                        obj.optString("narrator", "Fonos Voice"),
                        obj.optString("description", ""),
                        obj.optString("category", "General"),
                        obj.optString("cover_url", ""),
                        obj.optInt("total_duration", 0),
                        obj.optBoolean("is_premium", false)
                );
                bookList.add(book);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        libraryAdapter.notifyDataSetChanged();

        if (tvLibraryTitle != null) {
            tvLibraryTitle.setText("Gần đây");
        }

        if (bookList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvLibraryBooks.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvLibraryBooks.setVisibility(View.VISIBLE);
        }
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



        findViewById(R.id.navLibrary).setOnClickListener(v -> {
            // Đã ở trang thư viện
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
