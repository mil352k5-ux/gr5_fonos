package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.Book;
import com.example.myapplication.controller.FonosApiManager;
import com.example.myapplication.utils.SupabaseConfig;

import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private FonosApiManager apiManager;
    private RecyclerView rvBooks, rvBestSellers;
    private BookAdapter bookAdapter;
    private BestSellerAdapter bestSellerAdapter;
    private List<Book> bookList, bestSellerList;
    private String token = SupabaseConfig.SUPABASE_KEY;
    private final int PAGE_SIZE = 12;
    private int currentOffset = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    private enum TabState {
        AUDIOBOOKS, EBOOKS, SUMMARY
    }
    private TabState currentTab = TabState.AUDIOBOOKS;

    private View tabAudiobooksContainer, tabEbookContainer, tabSummaryContainer;
    private TextView tvTabAudiobooks, tvTabEbook, tvTabSummary;
    private View indicatorAudiobooks, indicatorEbook, indicatorSummary;
    private TextView tvBestSellersHeader, tvBooksForYouTitle, tvBooksForYouSubtitle;
    private View layoutCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        apiManager = new FonosApiManager(this);

        rvBooks = findViewById(R.id.rvBooks);
        rvBestSellers = findViewById(R.id.rvBestSellers);

        bookList = new ArrayList<>();
        bestSellerList = new ArrayList<>();

        bookAdapter = new BookAdapter(bookList, book -> {
            openBookDetail(book);
        });

        bestSellerAdapter = new BestSellerAdapter(bestSellerList, book -> {
            openBookDetail(book);
        });

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvBooks.setLayoutManager(layoutManager);
        rvBooks.setAdapter(bookAdapter);

        LinearLayoutManager bestSellerLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvBestSellers.setLayoutManager(bestSellerLayoutManager);
        rvBestSellers.setAdapter(bestSellerAdapter);

        new LinearSnapHelper().attachToRecyclerView(rvBestSellers);

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

        // Bind Tab Views
        tabAudiobooksContainer = findViewById(R.id.tab_audiobooks_container);
        tabEbookContainer = findViewById(R.id.tab_ebook_container);
        tabSummaryContainer = findViewById(R.id.tab_summary_container);

        tvTabAudiobooks = findViewById(R.id.tvTabAudiobooks);
        tvTabEbook = findViewById(R.id.tvTabEbook);
        tvTabSummary = findViewById(R.id.tvTabSummary);

        indicatorAudiobooks = findViewById(R.id.indicator_audiobooks);
        indicatorEbook = findViewById(R.id.indicator_ebook);
        indicatorSummary = findViewById(R.id.indicator_summary);

        tvBestSellersHeader = findViewById(R.id.tvBestSellersHeader);
        tvBooksForYouTitle = findViewById(R.id.tvBooksForYouTitle);
        tvBooksForYouSubtitle = findViewById(R.id.tvBooksForYouSubtitle);
        layoutCategories = findViewById(R.id.layoutCategories);

        if (tabAudiobooksContainer != null) {
            tabAudiobooksContainer.setOnClickListener(v -> switchTab(TabState.AUDIOBOOKS));
        }
        if (tabEbookContainer != null) {
            tabEbookContainer.setOnClickListener(v -> switchTab(TabState.EBOOKS));
        }
        if (tabSummaryContainer != null) {
            tabSummaryContainer.setOnClickListener(v -> switchTab(TabState.SUMMARY));
        }

        setupBottomNavigation();

        loadBestSellerBook();
        loadBooksPaged();
    }

    private void switchTab(TabState state) {
        currentTab = state;

        // Reset styling
        if (tvTabAudiobooks != null) {
            tvTabAudiobooks.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvTabAudiobooks.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (indicatorAudiobooks != null) {
            indicatorAudiobooks.setVisibility(View.INVISIBLE);
        }

        if (tvTabEbook != null) {
            tvTabEbook.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvTabEbook.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (indicatorEbook != null) {
            indicatorEbook.setVisibility(View.INVISIBLE);
        }

        if (tvTabSummary != null) {
            tvTabSummary.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvTabSummary.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (indicatorSummary != null) {
            indicatorSummary.setVisibility(View.INVISIBLE);
        }

        // Apply selected styling and update UI titles/sections
        switch (state) {
            case AUDIOBOOKS:
                if (tvTabAudiobooks != null) {
                    tvTabAudiobooks.setTextColor(getResources().getColor(android.R.color.black));
                    tvTabAudiobooks.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                if (indicatorAudiobooks != null) {
                    indicatorAudiobooks.setVisibility(View.VISIBLE);
                }
                
                if (tvBestSellersHeader != null) tvBestSellersHeader.setVisibility(View.GONE);
                if (layoutCategories != null) layoutCategories.setVisibility(View.GONE);
                
                if (tvBooksForYouTitle != null) tvBooksForYouTitle.setText(R.string.books_for_you_title);
                if (tvBooksForYouSubtitle != null) tvBooksForYouSubtitle.setText(R.string.books_for_you_subtitle);
                break;

            case EBOOKS:
                if (tvTabEbook != null) {
                    tvTabEbook.setTextColor(getResources().getColor(android.R.color.black));
                    tvTabEbook.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                if (indicatorEbook != null) {
                    indicatorEbook.setVisibility(View.VISIBLE);
                }
                
                if (tvBestSellersHeader != null) {
                    tvBestSellersHeader.setText("Top Ebook Thịnh Hành");
                    tvBestSellersHeader.setVisibility(View.VISIBLE);
                }
                if (layoutCategories != null) layoutCategories.setVisibility(View.VISIBLE);
                
                if (tvBooksForYouTitle != null) tvBooksForYouTitle.setText("Ebook Dành Cho Bạn");
                if (tvBooksForYouSubtitle != null) tvBooksForYouSubtitle.setText("Đọc sách mọi lúc mọi nơi – bấm để đọc ngay");
                break;

            case SUMMARY:
                if (tvTabSummary != null) {
                    tvTabSummary.setTextColor(getResources().getColor(android.R.color.black));
                    tvTabSummary.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                if (indicatorSummary != null) {
                    indicatorSummary.setVisibility(View.VISIBLE);
                }
                
                if (tvBestSellersHeader != null) {
                    tvBestSellersHeader.setText("Top Tóm Tắt Thịnh Hành");
                    tvBestSellersHeader.setVisibility(View.VISIBLE);
                }
                if (layoutCategories != null) layoutCategories.setVisibility(View.GONE);
                
                if (tvBooksForYouTitle != null) tvBooksForYouTitle.setText("Tóm Tắt Sách Cho Bạn");
                if (tvBooksForYouSubtitle != null) tvBooksForYouSubtitle.setText("Nắm trọn ý chính trong 10 phút");
                break;
        }
    }

    private void openBookDetail(Book book) {
        Intent intent = new Intent(HomeActivity.this, BookDetailActivity.class);
        intent.putExtra("id", book.getId());
        intent.putExtra("title", book.getTitle());
        intent.putExtra("author", book.getAuthor());
        intent.putExtra("description", book.getDescription());
        intent.putExtra("category", book.getCategory());
        intent.putExtra("cover_url", book.getCoverUrl());
        
        if (currentTab == TabState.EBOOKS) {
            intent.putExtra("book_type", "ebook");
        } else if (currentTab == TabState.SUMMARY) {
            intent.putExtra("book_type", "summary");
        } else {
            intent.putExtra("book_type", "audiobook");
        }
        
        startActivity(intent);
    }

    private void loadBestSellerBook() {
        apiManager.getBestSellerBooks(token, 5, new FonosApiManager.ApiCallback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONArray array = new JSONArray(responseBody);

                    bestSellerList.clear();

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
                        bestSellerList.add(book);
                    }

                    bestSellerAdapter.notifyDataSetChanged();

                } catch (Exception e) {
                    Toast.makeText(HomeActivity.this, "Lỗi tải sách bán chạy", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(HomeActivity.this, "Không tải được bestseller", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBooksPaged() {
        if (isLoading || isLastPage) return;

        isLoading = true;

        apiManager.getBooksPaged(token, PAGE_SIZE, currentOffset, new FonosApiManager.ApiCallback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONArray array = new JSONArray(responseBody);

                    if (array.length() < PAGE_SIZE) {
                        isLastPage = true;
                    }

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

                    currentOffset += array.length();
                    isLoading = false;

                } catch (Exception e) {
                    isLoading = false;
                    Toast.makeText(HomeActivity.this, "Lỗi đọc dữ liệu sách", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                isLoading = false;
                Toast.makeText(HomeActivity.this, "Không tải được sách", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navBooks).setOnClickListener(v -> {
            // Đã ở trang Sách
        });

        findViewById(R.id.navExplore).setOnClickListener(v -> {
            startActivity(new Intent(this, ExploreActivity.class));
            finish();
        });

        View navChallenge = findViewById(R.id.navChallenge);
        if (navChallenge != null) {
            navChallenge.setOnClickListener(v -> {
                startActivity(new Intent(this, ChallengeActivity.class));
                finish();
            });
        }

        findViewById(R.id.navLibrary).setOnClickListener(v -> {
            startActivity(new Intent(this, LibraryActivity.class));
            finish();
        });
    }
}