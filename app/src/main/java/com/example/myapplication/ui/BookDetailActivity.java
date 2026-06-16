package com.example.myapplication.ui;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.ui.EbookReaderActivity;
import com.example.myapplication.ui.PlayerActivity;
import com.bumptech.glide.Glide;

public class    BookDetailActivity extends AppCompatActivity {

    private ImageView imgBookCover;
    private TextView tvBookTitle, tvBookAuthor, tvBookDescription, tvBookCategory, tvListenNow, tvBack;
    private int selectedRating = 0;
    private TextView[] stars = new TextView[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        imgBookCover = findViewById(R.id.imgBookCover);
        tvBookTitle = findViewById(R.id.tvBookTitle);
        tvBookAuthor = findViewById(R.id.tvBookAuthor);
        tvBookDescription = findViewById(R.id.tvBookDescription);
        tvBookCategory = findViewById(R.id.tvBookCategory);
        tvListenNow = findViewById(R.id.tvListenNow);
        tvBack = findViewById(R.id.tvBack);
        TextView btnFavorite = findViewById(R.id.btnFavorite);

        String title = getIntent().getStringExtra("title");
        String author = getIntent().getStringExtra("author");
        String description = getIntent().getStringExtra("description");
        String category = getIntent().getStringExtra("category");
        String coverUrl = getIntent().getStringExtra("cover_url");
        String bookId = getIntent().getStringExtra("id");
        String bookType = getIntent().getStringExtra("book_type");
        boolean isEbook = "ebook".equals(bookType);

        if (btnFavorite != null) {
            android.content.SharedPreferences favoritesPref = getSharedPreferences("FonosFavorites", MODE_PRIVATE);
            boolean isFavorited = favoritesPref.getBoolean(bookId, false);
            
            btnFavorite.setText(isFavorited ? "❤️" : "♡");
            
            btnFavorite.setOnClickListener(v -> {
                boolean nowFavorited = !favoritesPref.getBoolean(bookId, false);
                favoritesPref.edit().putBoolean(bookId, nowFavorited).apply();
                
                if (nowFavorited) {
                    try {
                        org.json.JSONObject favBook = new org.json.JSONObject();
                        favBook.put("id", bookId);
                        favBook.put("title", title);
                        favBook.put("author", author);
                        favBook.put("description", description);
                        favBook.put("category", category);
                        favBook.put("cover_url", coverUrl);
                        favoritesPref.edit().putString("book_info_" + bookId, favBook.toString()).apply();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    btnFavorite.setText("❤️");
                    Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                } else {
                    favoritesPref.edit().remove("book_info_" + bookId).apply();
                    btnFavorite.setText("♡");
                    Toast.makeText(this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                }
            });
        }

        tvBookTitle.setText(title);
        tvBookAuthor.setText(author);
        tvBookDescription.setText(description);
        tvBookCategory.setText(category);
        tvBack.setOnClickListener(v -> finish());

        // Xem thêm / Thu gọn logic
        TextView tvSeeMore = findViewById(R.id.tvSeeMore);
        if (tvSeeMore != null) {
            tvBookDescription.post(() -> {
                if (tvBookDescription.getLineCount() > 3) {
                    tvBookDescription.setMaxLines(3);
                    tvBookDescription.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    tvSeeMore.setVisibility(android.view.View.VISIBLE);
                    tvSeeMore.setOnClickListener(v -> {
                        if (tvBookDescription.getMaxLines() == 3) {
                            tvBookDescription.setMaxLines(Integer.MAX_VALUE);
                            tvSeeMore.setText("Thu gọn");
                        } else {
                            tvBookDescription.setMaxLines(3);
                            tvSeeMore.setText("Xem thêm");
                        }
                    });
                } else {
                    tvSeeMore.setVisibility(android.view.View.GONE);
                }
            });
        }

        // Custom Star Selection
        stars[0] = findViewById(R.id.tvStar1);
        stars[1] = findViewById(R.id.tvStar2);
        stars[2] = findViewById(R.id.tvStar3);
        stars[3] = findViewById(R.id.tvStar4);
        stars[4] = findViewById(R.id.tvStar5);

        for (int i = 0; i < 5; i++) {
            final int index = i;
            if (stars[i] != null) {
                stars[i].setOnClickListener(v -> selectStars(index + 1));
            }
        }

        android.widget.LinearLayout llReviewsList = findViewById(R.id.llReviewsList);
        android.widget.EditText etReviewContent = findViewById(R.id.etReviewContent);
        android.view.View btnSubmitReview = findViewById(R.id.btnSubmitReview);

        if (llReviewsList != null) {
            loadSavedReviews(bookId, llReviewsList);
        }

        if (btnSubmitReview != null && etReviewContent != null && llReviewsList != null) {
            btnSubmitReview.setOnClickListener(v -> {
                String content = etReviewContent.getText().toString().trim();
                if (selectedRating == 0) {
                    Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (content.isEmpty()) {
                    etReviewContent.setError("Nhận xét không được để trống");
                    return;
                }

                android.content.SharedPreferences userPref = getSharedPreferences("FonosSession", MODE_PRIVATE);
                String userEmail = userPref.getString("email", "Người dùng");
                String userName = userEmail.contains("@") ? userEmail.split("@")[0] : userEmail;

                addReviewToLayout(llReviewsList, userName, "Vừa xong", selectedRating, content);
                saveReviewLocally(bookId, userName, selectedRating, content);

                etReviewContent.setText("");
                selectStars(0);
                Toast.makeText(this, "Cảm ơn bạn đã gửi đánh giá!", Toast.LENGTH_SHORT).show();
            });
        }

        if (isEbook) {
            tvListenNow.setText("Đọc ngay");
        } else {
            tvListenNow.setText("Nghe ngay");
        }

        ImageView imgMiniCover = findViewById(R.id.imgMiniCover);

        Glide.with(this)
                .load(coverUrl)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .into(imgBookCover);

        if (imgMiniCover != null) {
            Glide.with(this)
                    .load(coverUrl)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .into(imgMiniCover);
        }

        if (bookId != null && !bookId.isEmpty()) {
            addToRecentBooks(bookId, title, author, description, category, coverUrl);
        }

        tvListenNow.setOnClickListener(v -> {
            if (isEbook) {
                Intent intent = new Intent(BookDetailActivity.this, EbookReaderActivity.class);
                intent.putExtra("book_id", bookId);
                intent.putExtra("title", title);
                intent.putExtra("author", author);
                intent.putExtra("description", description);
                intent.putExtra("cover_url", coverUrl);
                startActivity(intent);
            } else {
                Intent intent = new Intent(BookDetailActivity.this, PlayerActivity.class);
                intent.putExtra("book_id", bookId);
                intent.putExtra("title", title);
                intent.putExtra("author", author);
                intent.putExtra("cover_url", coverUrl);
                startActivity(intent);
            }
        });
    }

    private void addToRecentBooks(String bookId, String title, String author, String description, String category, String coverUrl) {
        if (bookId == null || bookId.isEmpty()) return;
        try {
            android.content.SharedPreferences recentsPref = getSharedPreferences("FonosRecents", MODE_PRIVATE);
            String existingRecentsJson = recentsPref.getString("recent_books", "[]");
            org.json.JSONArray array = new org.json.JSONArray(existingRecentsJson);
            org.json.JSONArray newArray = new org.json.JSONArray();

            // Create new object
            org.json.JSONObject currentBook = new org.json.JSONObject();
            currentBook.put("id", bookId);
            currentBook.put("title", title);
            currentBook.put("author", author);
            currentBook.put("description", description == null ? "" : description);
            currentBook.put("category", category == null ? "" : category);
            currentBook.put("cover_url", coverUrl == null ? "" : coverUrl);

            // Add the current book first (index 0)
            newArray.put(currentBook);

            // Add other books, skipping duplicates
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                if (!obj.optString("id", "").equals(bookId)) {
                    newArray.put(obj);
                }
            }

            // Limit to top 20 recent items
            org.json.JSONArray finalArray = new org.json.JSONArray();
            for (int i = 0; i < Math.min(newArray.length(), 20); i++) {
                finalArray.put(newArray.getJSONObject(i));
            }

            recentsPref.edit().putString("recent_books", finalArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void selectStars(int rating) {
        selectedRating = rating;
        for (int i = 0; i < 5; i++) {
            if (stars[i] != null) {
                stars[i].setText(i < rating ? "★" : "☆");
            }
        }
    }

    private void saveReviewLocally(String bookId, String userName, int rating, String content) {
        try {
            android.content.SharedPreferences reviewsPref = getSharedPreferences("BookReviews", MODE_PRIVATE);
            String existingReviewsJson = reviewsPref.getString(bookId, "[]");
            org.json.JSONArray array = new org.json.JSONArray(existingReviewsJson);

            org.json.JSONObject newReview = new org.json.JSONObject();
            newReview.put("userName", userName);
            newReview.put("rating", rating);
            newReview.put("content", content);
            newReview.put("time", "Vừa xong");

            array.put(newReview);
            reviewsPref.edit().putString(bookId, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSavedReviews(String bookId, android.widget.LinearLayout llReviewsList) {
        try {
            android.content.SharedPreferences reviewsPref = getSharedPreferences("BookReviews", MODE_PRIVATE);
            String existingReviewsJson = reviewsPref.getString(bookId, "[]");
            org.json.JSONArray array = new org.json.JSONArray(existingReviewsJson);

            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject review = array.getJSONObject(i);
                String userName = review.getString("userName");
                int rating = review.getInt("rating");
                String content = review.getString("content");
                String time = review.getString("time");

                addReviewToLayout(llReviewsList, userName, time, rating, content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addReviewToLayout(android.widget.LinearLayout container, String userName, String time, int rating, String content) {
        android.widget.LinearLayout reviewCard = new android.widget.LinearLayout(this);
        android.widget.LinearLayout.LayoutParams cardParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, (int) (16 * getResources().getDisplayMetrics().density), 0, 0);
        reviewCard.setLayoutParams(cardParams);
        reviewCard.setOrientation(android.widget.LinearLayout.VERTICAL);
        reviewCard.setBackgroundResource(R.drawable.bg_review_card);
        reviewCard.setPadding(
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density)
        );

        android.widget.LinearLayout header = new android.widget.LinearLayout(this);
        header.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView avatar = new TextView(this);
        android.widget.LinearLayout.LayoutParams avatarParams = new android.widget.LinearLayout.LayoutParams(
                (int) (36 * getResources().getDisplayMetrics().density),
                (int) (36 * getResources().getDisplayMetrics().density)
        );
        avatar.setLayoutParams(avatarParams);
        avatar.setBackgroundResource(R.drawable.circle_background);
        avatar.setGravity(android.view.Gravity.CENTER);
        avatar.setText("👨‍🚀");
        avatar.setTextSize(20);

        android.widget.LinearLayout nameTimeContainer = new android.widget.LinearLayout(this);
        android.widget.LinearLayout.LayoutParams nameTimeParams = new android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        nameTimeParams.setMarginStart((int) (10 * getResources().getDisplayMetrics().density));
        nameTimeContainer.setLayoutParams(nameTimeParams);
        nameTimeContainer.setOrientation(android.widget.LinearLayout.VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText(userName);
        tvName.setTextColor(0xFF142B4A);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setTextSize(14);

        TextView tvTime = new TextView(this);
        tvTime.setText(time);
        tvTime.setTextColor(0xFF7E8A9A);
        tvTime.setTextSize(11);

        nameTimeContainer.addView(tvName);
        nameTimeContainer.addView(tvTime);

        header.addView(avatar);
        header.addView(nameTimeContainer);

        android.widget.LinearLayout ratingLayout = new android.widget.LinearLayout(this);
        android.widget.LinearLayout.LayoutParams ratingParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        ratingParams.setMargins(0, (int) (8 * getResources().getDisplayMetrics().density), 0, 0);
        ratingLayout.setLayoutParams(ratingParams);
        ratingLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        ratingLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvStars = new TextView(this);
        tvStars.setText("⭐ " + rating + "/5");
        tvStars.setTextColor(0xFFFFB300);
        tvStars.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStars.setTextSize(13);

        ratingLayout.addView(tvStars);

        TextView tvContent = new TextView(this);
        android.widget.LinearLayout.LayoutParams contentParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        contentParams.setMargins(0, (int) (6 * getResources().getDisplayMetrics().density), 0, 0);
        tvContent.setLayoutParams(contentParams);
        tvContent.setText(content);
        tvContent.setTextColor(0xFF4A5F7A);
        tvContent.setTextSize(13);

        reviewCard.addView(header);
        reviewCard.addView(ratingLayout);
        reviewCard.addView(tvContent);

        container.addView(reviewCard, 0);
    }
}
