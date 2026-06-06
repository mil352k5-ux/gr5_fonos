package com.example.myapplication;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class BookDetailActivity extends AppCompatActivity {

    private ImageView imgBookCover;
    private TextView tvBookTitle, tvBookAuthor, tvBookDescription, tvBookCategory, tvListenNow, tvBack;


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
}