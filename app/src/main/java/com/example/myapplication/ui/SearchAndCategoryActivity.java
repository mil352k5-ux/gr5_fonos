package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.SearchBookAdapter;
import com.example.myapplication.controller.FonosApiManager;
import com.example.myapplication.model.Book;
import com.example.myapplication.utils.MiniPlayerController;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SearchAndCategoryActivity extends AppCompatActivity {

    private TextView btnBack, tvTitle, tvSubtitle, tvEmptyMessage;
    private RecyclerView rvBooks;
    private ProgressBar pbLoading;
    private LinearLayout layoutEmptyState;

    private FonosApiManager apiManager;
    private SearchBookAdapter adapter;
    private List<Book> bookList;
    private MiniPlayerController miniPlayerController;

    private String searchQuery = null;
    private String categoryFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_and_category);

        apiManager = new FonosApiManager(this);

        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        rvBooks = findViewById(R.id.rvBooks);
        pbLoading = findViewById(R.id.pbLoading);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        bookList = new ArrayList<>();
        adapter = new SearchBookAdapter(bookList, new SearchBookAdapter.OnSearchClickListener() {
            @Override
            public void onBookClick(Book book) {
                Intent intent = new Intent(SearchAndCategoryActivity.this, BookDetailActivity.class);
                intent.putExtra("id", book.getId());
                intent.putExtra("title", book.getTitle());
                intent.putExtra("author", book.getAuthor());
                intent.putExtra("description", book.getDescription());
                intent.putExtra("category", book.getCategory());
                intent.putExtra("cover_url", book.getCoverUrl());
                intent.putExtra("book_type", "audiobook"); // Default type
                startActivity(intent);
            }

            @Override
            public void onPlayClick(Book book) {
                Intent intent = new Intent(SearchAndCategoryActivity.this, PlayerActivity.class);
                intent.putExtra("book_id", book.getId());
                intent.putExtra("title", book.getTitle());
                intent.putExtra("author", book.getAuthor());
                intent.putExtra("cover_url", book.getCoverUrl());
                startActivity(intent);
            }
        });

        rvBooks.setLayoutManager(new LinearLayoutManager(this));
        rvBooks.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        searchQuery = getIntent().getStringExtra("search_query");
        categoryFilter = getIntent().getStringExtra("category_filter");

        // Bind Mini Player
        miniPlayerController = new MiniPlayerController(this);
        miniPlayerController.init();

        loadData();
    }

    private void loadData() {
        pbLoading.setVisibility(View.VISIBLE);
        rvBooks.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);

        FonosApiManager.ApiCallback callback = new FonosApiManager.ApiCallback() {
            @Override
            public void onSuccess(String responseBody) {
                pbLoading.setVisibility(View.GONE);
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

                    adapter.notifyDataSetChanged();

                    if (bookList.isEmpty()) {
                        layoutEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyMessage.setText("Không tìm thấy cuốn sách nào.");
                    } else {
                        rvBooks.setVisibility(View.VISIBLE);
                    }

                } catch (Exception e) {
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyMessage.setText("Lỗi đọc dữ liệu từ máy chủ.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                pbLoading.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.VISIBLE);
                tvEmptyMessage.setText("Lỗi kết nối: " + errorMessage);
            }
        };

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            tvTitle.setText("Tìm kiếm");
            tvSubtitle.setText("Kết quả tìm kiếm cho: \"" + searchQuery + "\"");
            apiManager.searchBooks(searchQuery, callback);
        } else if (categoryFilter != null && !categoryFilter.trim().isEmpty()) {
            tvTitle.setText(categoryFilter);
            tvSubtitle.setText("Khám phá tủ sách thuộc chủ đề này");
            
            // Map Vietnamese user-friendly names to Database category names if needed
            String mappedCategory = mapCategory(categoryFilter);
            apiManager.getBooksByCategory(mappedCategory, callback);
        } else {
            pbLoading.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("Không có tham số tìm kiếm.");
        }
    }

    private String mapCategory(String displayCategory) {
        // Map category filter to database categories
        switch (displayCategory.toLowerCase()) {
            case "thiền":
                return "Body, Mind & Spirit";
            case "sách tiếng anh":
                return "Language Arts & Disciplines";
            case "kỹ năng":
                return "Psychology";
            case "tư duy":
                return "Philosophy";
            case "văn học":
                return "Fiction";
            case "nuôi dạy con":
                return "Family & Relationships";
            case "thiếu nhi":
                return "Children / Fantasy / Adventure";
            case "kinh doanh":
                return "Business & Economics";
            case "podcourse":
                return "Education";
            case "podcast":
                return "Performing Arts";
            case "truyện ngủ":
                return "Performing Arts";
            default:
                return displayCategory;
        }
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
