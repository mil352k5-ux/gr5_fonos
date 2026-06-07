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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.myapplication.service.AudioPlayerService;
import com.example.myapplication.controller.FonosApiManager;

import org.json.JSONArray;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.util.Locale;

public class PlayerActivity extends AppCompatActivity {

    private ImageView imgPlayerCover;
    private ImageView imgPlayerBackground;
    private TextView tvPlayerTitle, tvPlayerAuthor, tvPlayerStatus;
    private TextView tvPlayPause, tvClosePlayer;
    private TextView tvCurrentTime, tvTotalTime;
    private TextView tvRewind15, tvForward30, tvPrevious, tvNext;
    private SeekBar seekAudio;

    private FonosApiManager apiManager;

    private String bookId, title, author, coverUrl;
    private String currentAudioUrl = "";

    private AudioPlayerService audioService;
    private boolean isBound = false;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        requestNotificationPermissionIfNeeded();

        apiManager = new FonosApiManager(this);

        imgPlayerCover = findViewById(R.id.imgPlayerCover);
        imgPlayerBackground = findViewById(R.id.imgPlayerBackground);
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

        if (imgPlayerBackground != null) {
            Glide.with(this)
                    .asBitmap()
                    .load(coverUrl)
                    .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                            Bitmap blurred = blurBitmap(resource, 25f);
                            if (blurred != null) {
                                imgPlayerBackground.setImageBitmap(blurred);
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
                    });
        }

        tvClosePlayer.setOnClickListener(v -> finish());

        // Update skip labels to 5s
        tvRewind15.setText("↺5");
        tvForward30.setText("5↻");

        tvPlayPause.setOnClickListener(v -> {
            if (currentAudioUrl == null || currentAudioUrl.isEmpty()) {
                Toast.makeText(this, "Audio chưa sẵn sàng", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, AudioPlayerService.class);
            intent.setAction(AudioPlayerService.ACTION_TOGGLE);
            ContextCompat.startForegroundService(this, intent);
        });

        tvRewind15.setOnClickListener(v -> {
            if (isBound && audioService != null && audioService.isPrepared()) {
                int current = audioService.getCurrentPosition();
                audioService.seekTo(Math.max(0, current - 5000));
                updatePlaybackProgressUI();
            } else {
                Toast.makeText(this, "Audio chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            }
        });

        tvForward30.setOnClickListener(v -> {
            if (isBound && audioService != null && audioService.isPrepared()) {
                int current = audioService.getCurrentPosition();
                int total = audioService.getDuration();
                audioService.seekTo(Math.min(total, current + 5000));
                updatePlaybackProgressUI();
            } else {
                Toast.makeText(this, "Audio chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            }
        });

        seekAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound && audioService != null && audioService.isPrepared()) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                progressHandler.removeCallbacks(progressRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isBound && audioService != null && audioService.isPrepared()) {
                    audioService.seekTo(seekBar.getProgress());
                }
                progressHandler.post(progressRunnable);
            }
        });

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

                    String storagePath = chapter.optString("storage_path", "");
                    String chapterTitle = chapter.optString("title", "Chương 1");

                    if (!storagePath.isEmpty()) {
                        currentAudioUrl = com.example.myapplication.utils.SupabaseConfig.SUPABASE_URL + "/storage/v1/object/public/audios/" + storagePath;
                    } else {
                        currentAudioUrl = chapter.optString("audio_url", "");
                        if (currentAudioUrl.contains("sentufkmrzokwtkgtmvc.supabase.co")) {
                            currentAudioUrl = currentAudioUrl.replace("sentufkmrzokwtkgtmvc.supabase.co", "sentufkmrzokwktgtmvc.supabase.co");
                        }
                    }

                    if (currentAudioUrl.isEmpty()) {
                        tvPlayerStatus.setText("Chương này chưa có audio");
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

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, AudioPlayerService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        progressHandler.removeCallbacks(progressRunnable);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioPlayerService.LocalBinder binder = (AudioPlayerService.LocalBinder) service;
            audioService = binder.getService();
            isBound = true;
            setupServiceUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            audioService = null;
        }
    };

    private void setupServiceUI() {
        if (audioService == null) return;

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBound && audioService != null && audioService.isPrepared()) {
                    updatePlaybackProgressUI();
                }
                progressHandler.postDelayed(this, 1000);
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void updatePlaybackProgressUI() {
        if (audioService == null) return;
        
        int current = audioService.getCurrentPosition();
        int total = audioService.getDuration();

        seekAudio.setMax(total);
        seekAudio.setProgress(current);

        tvCurrentTime.setText(formatTime(current));
        tvTotalTime.setText(formatTime(total));

        if (audioService.isPlaying()) {
            tvPlayPause.setText("Ⅱ");
            tvPlayerStatus.setText("Đang phát");
        } else {
            tvPlayPause.setText("▶");
            tvPlayerStatus.setText("Đang tạm dừng");
        }
    }

    private String formatTime(int ms) {
        int minutes = (ms / 1000) / 60;
        int seconds = (ms / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private Bitmap blurBitmap(Bitmap bitmap, float radius) {
        if (bitmap == null) return null;

        int width = Math.round(bitmap.getWidth() * 0.2f);
        int height = Math.round(bitmap.getHeight() * 0.2f);
        if (width <= 0 || height <= 0) return bitmap;

        Bitmap inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        try {
            android.renderscript.RenderScript rs = android.renderscript.RenderScript.create(this);
            android.renderscript.Allocation theIntrinsicInput = android.renderscript.Allocation.createFromBitmap(rs, inputBitmap);
            android.renderscript.Allocation theIntrinsicOutput = android.renderscript.Allocation.createFromBitmap(rs, outputBitmap);
            android.renderscript.ScriptIntrinsicBlur theBlur = android.renderscript.ScriptIntrinsicBlur.create(rs, android.renderscript.Element.U8_4(rs));
            theBlur.setRadius(radius);
            theBlur.setInput(theIntrinsicInput);
            theBlur.forEach(theIntrinsicOutput);
            theIntrinsicOutput.copyTo(outputBitmap);
            rs.destroy();
            return outputBitmap;
        } catch (Exception e) {
            return inputBitmap;
        }
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