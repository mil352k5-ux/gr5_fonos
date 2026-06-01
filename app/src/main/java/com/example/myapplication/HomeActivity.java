package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.BookAdapter;
import com.example.myapplication.model.Book;
import com.example.myapplication.utils.FonosApiManager;
import com.example.myapplication.utils.SupabaseConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private FonosApiManager apiManager;
    private RecyclerView rvBooks;
    private BookAdapter bookAdapter;
    private List<Book> bookList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        apiManager = new FonosApiManager(this);

        rvBooks = findViewById(R.id.rvBooks);
        bookList = new ArrayList<>();

        bookAdapter = new BookAdapter(bookList);

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

        rvBooks.setLayoutManager(layoutManager);
        rvBooks.setAdapter(bookAdapter);

        Button btnUpgrade = findViewById(R.id.btnUpgrade);
        if (btnUpgrade != null) {
            btnUpgrade.setOnClickListener(v ->
                    Toast.makeText(this, "Đã có dữ liệu sách trong Supabase", Toast.LENGTH_SHORT).show()
            );
        }

        View profileIcon = findViewById(R.id.profile_icon);
        if (profileIcon != null) {
            profileIcon.setOnClickListener(v ->
                    Toast.makeText(HomeActivity.this, "Mở trang cá nhân", Toast.LENGTH_SHORT).show()
            );
        }

        setupBottomNavigation();

        loadBooksFromSupabase();
    }

    private void loadBooksFromSupabase() {
        apiManager.getBooks(SupabaseConfig.SUPABASE_KEY, new FonosApiManager.ApiCallback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONArray array = new JSONArray(responseBody);

                    bookList.clear();

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

                    bookAdapter.notifyDataSetChanged();

                    Toast.makeText(
                            HomeActivity.this,
                            "Đã tải " + bookList.size() + " sách",
                            Toast.LENGTH_SHORT
                    ).show();

                } catch (Exception e) {
                    Toast.makeText(
                            HomeActivity.this,
                            "Lỗi parse books: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(
                        HomeActivity.this,
                        "Lỗi tải sách: " + errorMessage,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navBooks).setOnClickListener(v -> {
            // Đã ở trang chủ/sách
        });

        findViewById(R.id.navExplore).setOnClickListener(v -> {
            startActivity(new Intent(this, ExploreActivity.class));
            finish();
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