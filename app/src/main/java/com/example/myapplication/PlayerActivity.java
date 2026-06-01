package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.myapplication.service.AudioPlayerService;
import com.example.myapplication.utils.FonosApiManager;

import org.json.JSONArray;
import org.json.JSONObject;

public class PlayerActivity extends AppCompatActivity {

    private ImageView imgPlayerCover;
    private TextView tvPlayerTitle, tvPlayerAuthor, tvPlayerStatus;
    private TextView tvPlayPause, tvClosePlayer;
    private TextView tvCurrentTime, tvTotalTime;
    private TextView tvRewind15, tvForward30, tvPrevious, tvNext;
    private SeekBar seekAudio;

    private FonosApiManager apiManager;

    private String bookId, title, author, coverUrl;
    private String currentAudioUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        requestNotificationPermissionIfNeeded();

        apiManager = new FonosApiManager(this);

        imgPlayerCover = findViewById(R.id.imgPlayerCover);
        tvPlayerTitle = findViewById(R.id.tvPlayerTitle);
        tvPlayerAuthor = findViewById(R.id.tvPlayerAuthor);
        tvPlayerStatus = findViewById(R.id.tvPlayerStatus);
        tvPlayPause = findViewById(R.id.tvPlayPause);
        tvClosePlayer = findViewById(R.id.tvClosePlayer);
        seekAudio = findViewById(R.id.seekAudio);

        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvRewind15 = findViewById(R.id.tvRewind15);
        tvForward30 = findViewById(R.id.tvForward30);
        tvPrevious = findViewById(R.id.tvPrevious);
        tvNext = findViewById(R.id.tvNext);

        bookId = getIntent().getStringExtra("book_id");
        title = getIntent().getStringExtra("title");
        author = getIntent().getStringExtra("author");
        coverUrl = getIntent().getStringExtra("cover_url");

        if (title == null) title = "Tên sách";
        if (author == null) author = "Tác giả";

        tvPlayerTitle.setText(title);
        tvPlayerAuthor.setText(author);
        tvPlayerStatus.setText("Đang tải chương đầu tiên...");
        tvPlayPause.setText("▶");
        tvCurrentTime.setText("00:00");
        tvTotalTime.setText("-00:00");

        Glide.with(this)
                .load(coverUrl)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .into(imgPlayerCover);

        tvClosePlayer.setOnClickListener(v -> finish());

        tvPlayPause.setOnClickListener(v -> {
            if (currentAudioUrl == null || currentAudioUrl.isEmpty()) {
                Toast.makeText(this, "Audio chưa sẵn sàng", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, AudioPlayerService.class);
            intent.setAction(AudioPlayerService.ACTION_TOGGLE);
            ContextCompat.startForegroundService(this, intent);

            if (tvPlayPause.getText().toString().equals("▶")) {
                tvPlayPause.setText("Ⅱ");
                tvPlayerStatus.setText("Đang phát");
            } else {
                tvPlayPause.setText("▶");
                tvPlayerStatus.setText("Đang tạm dừng");
            }
        });

        tvRewind15.setOnClickListener(v ->
                Toast.makeText(this, "Tua lại 15s sẽ làm ở bước bind service", Toast.LENGTH_SHORT).show()
        );

        tvForward30.setOnClickListener(v ->
                Toast.makeText(this, "Tua tới 30s sẽ làm ở bước bind service", Toast.LENGTH_SHORT).show()
        );

        tvPrevious.setOnClickListener(v ->
                Toast.makeText(this, "Chương trước sẽ làm sau", Toast.LENGTH_SHORT).show()
        );

        tvNext.setOnClickListener(v ->
                Toast.makeText(this, "Chương tiếp theo sẽ làm sau", Toast.LENGTH_SHORT).show()
        );

        loadFirstChapter();
    }

    private void loadFirstChapter() {
        apiManager.getFirstChapterByBookId(bookId, new FonosApiManager.ApiCallback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONArray array = new JSONArray(responseBody);

                    if (array.length() == 0) {
                        tvPlayerStatus.setText("Sách này chưa có audio");
                        return;
                    }

                    JSONObject chapter = array.getJSONObject(0);

                    currentAudioUrl = chapter.optString("audio_url", "");
                    String chapterTitle = chapter.optString("title", "Chương 1");

                    if (currentAudioUrl.isEmpty()) {
                        tvPlayerStatus.setText("Chương này chưa có audio_url");
                        return;
                    }

                    tvPlayerStatus.setText(chapterTitle);
                    startAudioService(currentAudioUrl);

                } catch (Exception e) {
                    tvPlayerStatus.setText("Lỗi đọc dữ liệu audio");
                }
            }

            @Override
            public void onError(String errorMessage) {
                tvPlayerStatus.setText("Không tải được audio");
            }
        });
    }

    private void startAudioService(String audioUrl) {
        Intent intent = new Intent(this, AudioPlayerService.class);

        intent.setAction(AudioPlayerService.ACTION_PLAY_NEW);
        intent.putExtra(AudioPlayerService.EXTRA_AUDIO_URL, audioUrl);
        intent.putExtra(AudioPlayerService.EXTRA_BOOK_ID, bookId);
        intent.putExtra(AudioPlayerService.EXTRA_TITLE, title);
        intent.putExtra(AudioPlayerService.EXTRA_AUTHOR, author);
        intent.putExtra(AudioPlayerService.EXTRA_COVER_URL, coverUrl);

        ContextCompat.startForegroundService(this, intent);

        tvPlayPause.setText("Ⅱ");
        tvPlayerStatus.setText("Đang phát bằng Foreground Service");
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }
    }
}