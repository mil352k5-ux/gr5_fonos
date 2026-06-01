package com.example.myapplication;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.utils.FonosApiManager;

import org.json.JSONArray;
import org.json.JSONObject;

public class PlayerActivity extends AppCompatActivity {

    private ImageView imgPlayerCover;
    private TextView tvPlayerTitle, tvPlayerAuthor, tvPlayerStatus, tvPlayPause, tvClosePlayer;
    private SeekBar seekAudio;

    private FonosApiManager apiManager;
    private MediaPlayer mediaPlayer;
    private boolean isPrepared = false;
    private boolean isPlaying = false;

    private String bookId, title, author, coverUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        apiManager = new FonosApiManager(this);

        imgPlayerCover = findViewById(R.id.imgPlayerCover);
        tvPlayerTitle = findViewById(R.id.tvPlayerTitle);
        tvPlayerAuthor = findViewById(R.id.tvPlayerAuthor);
        tvPlayerStatus = findViewById(R.id.tvPlayerStatus);
        tvPlayPause = findViewById(R.id.tvPlayPause);
        tvClosePlayer = findViewById(R.id.tvClosePlayer);
        seekAudio = findViewById(R.id.seekAudio);

        bookId = getIntent().getStringExtra("book_id");
        title = getIntent().getStringExtra("title");
        author = getIntent().getStringExtra("author");
        coverUrl = getIntent().getStringExtra("cover_url");

        tvPlayerTitle.setText(title);
        tvPlayerAuthor.setText(author);
        tvPlayerStatus.setText("Đang tải chương đầu tiên...");

        Glide.with(this)
                .load(coverUrl)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .into(imgPlayerCover);

        tvClosePlayer.setOnClickListener(v -> finish());

        tvPlayPause.setOnClickListener(v -> {
            if (!isPrepared) {
                Toast.makeText(this, "Audio chưa sẵn sàng", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isPlaying) {
                mediaPlayer.pause();
                isPlaying = false;
                tvPlayPause.setText("▶");
            } else {
                mediaPlayer.start();
                isPlaying = true;
                tvPlayPause.setText("Ⅱ");
            }
        });

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
                    String audioUrl = chapter.optString("audio_url", "");
                    String chapterTitle = chapter.optString("title", "Chương 1");

                    tvPlayerStatus.setText(chapterTitle);

                    prepareAudio(audioUrl);

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

    private void prepareAudio(String audioUrl) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );

            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                seekAudio.setMax(mp.getDuration());
                tvPlayerStatus.setText(tvPlayerStatus.getText() + " • Sẵn sàng nghe");
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                tvPlayPause.setText("▶");
                seekAudio.setProgress(0);
            });

        } catch (Exception e) {
            tvPlayerStatus.setText("Audio URL không hợp lệ");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}