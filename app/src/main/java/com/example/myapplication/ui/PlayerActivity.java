package com.example.myapplication.ui;

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
import com.example.myapplication.R;
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

    private String bookId, title, author, coverUrl;
    private String currentAudioUrl = "";

    private final Handler sleepTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable sleepTimerRunnable;
    private float currentSpeed = 1.0f;

    private AudioPlayerService audioService;
    private boolean isBound = false;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private FonosApiManager apiManager;
    private org.json.JSONArray chaptersArray = null;
    private int currentChapterIndex = -1;
    private int[] chapterStartPositionsMs = null;
    private int[] chapterDurationsMs = null;
    private int bookTotalDurationMs = 0;


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
                if (chaptersArray != null && chapterStartPositionsMs != null && chapterDurationsMs != null && currentChapterIndex >= 0) {
                    int currentCumulative = chapterStartPositionsMs[currentChapterIndex] + audioService.getCurrentPosition();
                    int targetProgress = Math.max(0, currentCumulative - 5000);

                    int targetChapterIndex = 0;
                    for (int i = 0; i < chaptersArray.length(); i++) {
                        int start = chapterStartPositionsMs[i];
                        int end = start + chapterDurationsMs[i];
                        if (targetProgress >= start && targetProgress <= end) {
                            targetChapterIndex = i;
                            break;
                        }
                        if (i == chaptersArray.length() - 1) {
                            targetChapterIndex = i;
                        }
                    }

                    int offsetInChapter = targetProgress - chapterStartPositionsMs[targetChapterIndex];
                    if (offsetInChapter < 0) offsetInChapter = 0;

                    if (targetChapterIndex == currentChapterIndex) {
                        audioService.seekTo(offsetInChapter);
                        capNhatThoiGianSeekbarGiaoDien();
                    } else {
                        currentChapterIndex = targetChapterIndex;
                        playChapterAndSeek(currentChapterIndex, offsetInChapter);
                    }
                } else {
                    int current = audioService.getCurrentPosition();
                    audioService.seekTo(Math.max(0, current - 5000));
                    capNhatThoiGianSeekbarGiaoDien();
                }
            } else {
                Toast.makeText(this, "Audio chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            }
        });

        tvForward30.setOnClickListener(v -> {
            if (isBound && audioService != null && audioService.isPrepared()) {
                if (chaptersArray != null && chapterStartPositionsMs != null && chapterDurationsMs != null && currentChapterIndex >= 0) {
                    int currentCumulative = chapterStartPositionsMs[currentChapterIndex] + audioService.getCurrentPosition();
                    int limit = bookTotalDurationMs > 0 ? bookTotalDurationMs : audioService.getDuration();
                    int targetProgress = Math.min(limit, currentCumulative + 5000);

                    int targetChapterIndex = 0;
                    for (int i = 0; i < chaptersArray.length(); i++) {
                        int start = chapterStartPositionsMs[i];
                        int end = start + chapterDurationsMs[i];
                        if (targetProgress >= start && targetProgress <= end) {
                            targetChapterIndex = i;
                            break;
                        }
                        if (i == chaptersArray.length() - 1) {
                            targetChapterIndex = i;
                        }
                    }

                    int offsetInChapter = targetProgress - chapterStartPositionsMs[targetChapterIndex];
                    if (offsetInChapter < 0) offsetInChapter = 0;

                    if (targetChapterIndex == currentChapterIndex) {
                        audioService.seekTo(offsetInChapter);
                        capNhatThoiGianSeekbarGiaoDien();
                    } else {
                        currentChapterIndex = targetChapterIndex;
                        playChapterAndSeek(currentChapterIndex, offsetInChapter);
                    }
                } else {
                    int current = audioService.getCurrentPosition();
                    int total = audioService.getDuration();
                    audioService.seekTo(Math.min(total, current + 5000));
                    capNhatThoiGianSeekbarGiaoDien();
                }
            } else {
                Toast.makeText(this, "Audio chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            }
        });

        seekAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                progressHandler.removeCallbacks(progressRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (chaptersArray != null && chapterStartPositionsMs != null && chapterDurationsMs != null) {
                    int progress = seekBar.getProgress();
                    int targetChapterIndex = 0;
                    for (int i = 0; i < chaptersArray.length(); i++) {
                        int start = chapterStartPositionsMs[i];
                        int end = start + chapterDurationsMs[i];
                        if (progress >= start && progress <= end) {
                            targetChapterIndex = i;
                            break;
                        }
                        if (i == chaptersArray.length() - 1) {
                            targetChapterIndex = i;
                        }
                    }

                    int offsetInChapter = progress - chapterStartPositionsMs[targetChapterIndex];
                    if (offsetInChapter < 0) offsetInChapter = 0;

                    if (targetChapterIndex == currentChapterIndex) {
                        if (isBound && audioService != null && audioService.isPrepared()) {
                            audioService.seekTo(offsetInChapter);
                        }
                    } else {
                        currentChapterIndex = targetChapterIndex;
                        playChapterAndSeek(currentChapterIndex, offsetInChapter);
                    }
                } else {
                    if (isBound && audioService != null && audioService.isPrepared()) {
                        audioService.seekTo(seekBar.getProgress());
                    }
                }
                progressHandler.post(progressRunnable);
            }
        });

        tvPrevious.setOnClickListener(v -> {
            if (chaptersArray == null || chaptersArray.length() == 0) {
                Toast.makeText(this, "Chưa có danh sách chương", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentChapterIndex > 0) {
                currentChapterIndex--;
                playChapter(currentChapterIndex);
            } else {
                Toast.makeText(this, "Đây là chương đầu tiên", Toast.LENGTH_SHORT).show();
            }
        });

        tvNext.setOnClickListener(v -> {
            if (chaptersArray == null || chaptersArray.length() == 0) {
                Toast.makeText(this, "Chưa có danh sách chương", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentChapterIndex < chaptersArray.length() - 1) {
                currentChapterIndex++;
                playChapter(currentChapterIndex);
            } else {
                Toast.makeText(this, "Đây là chương cuối cùng", Toast.LENGTH_SHORT).show();
            }
        });

        android.view.View tvSleepTimer = findViewById(R.id.tvSleepTimer);
        if (tvSleepTimer != null) {
            tvSleepTimer.setOnClickListener(v -> showSleepTimerDialog());
        }

        android.view.View tvChapter = findViewById(R.id.tvChapter);
        if (tvChapter != null) {
            tvChapter.setOnClickListener(v -> showChaptersDialog());
        }

        android.view.View tvSpeed = findViewById(R.id.tvSpeed);
        if (tvSpeed != null) {
            tvSpeed.setOnClickListener(v -> showSpeedDialog());
        }

        if (bookId != null && !bookId.isEmpty()) {
            addToRecentBooks(bookId, title, author, coverUrl);
        }

        loadChapters();
    }

    private void addToRecentBooks(String bookId, String title, String author, String coverUrl) {
        if (bookId == null || bookId.isEmpty()) return;
        try {
            android.content.SharedPreferences recentsPref = getSharedPreferences("FonosRecents", MODE_PRIVATE);
            String existingRecentsJson = recentsPref.getString("recent_books", "[]");
            org.json.JSONArray array = new org.json.JSONArray(existingRecentsJson);
            
            String description = "";
            String category = "";
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                if (obj.optString("id", "").equals(bookId)) {
                    description = obj.optString("description", "");
                    category = obj.optString("category", "");
                    break;
                }
            }

            org.json.JSONArray newArray = new org.json.JSONArray();

            org.json.JSONObject currentBook = new org.json.JSONObject();
            currentBook.put("id", bookId);
            currentBook.put("title", title);
            currentBook.put("author", author);
            currentBook.put("description", description);
            currentBook.put("category", category);
            currentBook.put("cover_url", coverUrl == null ? "" : coverUrl);

            newArray.put(currentBook);

            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                if (!obj.optString("id", "").equals(bookId)) {
                    newArray.put(obj);
                }
            }

            org.json.JSONArray finalArray = new org.json.JSONArray();
            for (int i = 0; i < Math.min(newArray.length(), 20); i++) {
                finalArray.put(newArray.getJSONObject(i));
            }

            recentsPref.edit().putString("recent_books", finalArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadChapters() {
        tvPlayerStatus.setText("Đang tải danh sách chương...");
        apiManager.getChaptersByBookId(bookId, new FonosApiManager.ApiCallback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    chaptersArray = new JSONArray(responseBody);
                    if (chaptersArray.length() == 0) {
                        tvPlayerStatus.setText("Sách này chưa có audio");
                        return;
                    }

                    int totalSumSec = 0;
                    for (int i = 0; i < chaptersArray.length(); i++) {
                        totalSumSec += chaptersArray.getJSONObject(i).optInt("duration", 0);
                    }

                    if (totalSumSec > 0) {
                        initChapterTimelineWithSum();
                        proceedWithPlayback();
                    } else {
                        initChapterTimelineWithDefault();
                        proceedWithPlayback();
                        fetchBookDurationAndHeal();
                    }

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

    private void initChapterTimelineWithSum() {
        if (chaptersArray == null) return;
        int count = chaptersArray.length();
        chapterDurationsMs = new int[count];
        chapterStartPositionsMs = new int[count];
        for (int i = 0; i < count; i++) {
            try {
                JSONObject chapter = chaptersArray.getJSONObject(i);
                int durationSeconds = chapter.optInt("duration", 0);
                chapterDurationsMs[i] = durationSeconds * 1000;
            } catch (Exception e) {
                chapterDurationsMs[i] = 0;
            }
        }
        recalculatePositions();
    }

    private void initChapterTimelineWithDefault() {
        if (chaptersArray == null) return;
        int count = chaptersArray.length();
        chapterDurationsMs = new int[count];
        chapterStartPositionsMs = new int[count];
        for (int i = 0; i < count; i++) {
            chapterDurationsMs[i] = 10 * 60 * 1000; // default 10 mins
        }
        recalculatePositions();
    }

    private void recalculatePositions() {
        if (chaptersArray == null || chapterDurationsMs == null || chapterStartPositionsMs == null) return;
        int count = chaptersArray.length();
        int currentOffset = 0;
        for (int i = 0; i < count; i++) {
            chapterStartPositionsMs[i] = currentOffset;
            currentOffset += chapterDurationsMs[i];
        }
        bookTotalDurationMs = currentOffset;
    }

    private void fetchBookDurationAndHeal() {
        apiManager.getBookTotalDuration(bookId, new FonosApiManager.ApiCallback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    org.json.JSONArray array = new org.json.JSONArray(responseBody);
                    if (array.length() > 0) {
                        JSONObject obj = array.getJSONObject(0);
                        int totalDurationSec = obj.optInt("total_duration", 0);
                        if (totalDurationSec > 0 && chaptersArray != null) {
                            int count = chaptersArray.length();
                            int totalDurationMs = totalDurationSec * 1000;
                            int baseline = totalDurationMs / count;
                            for (int i = 0; i < count; i++) {
                                chapterDurationsMs[i] = baseline;
                            }
                            chapterDurationsMs[count - 1] = totalDurationMs - (baseline * (count - 1));
                            recalculatePositions();
                            capNhatThoiGianSeekbarGiaoDien();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }

    private void proceedWithPlayback() {
        boolean alreadyPlaying = false;
        if (isBound && audioService != null) {
            String serviceBookId = audioService.getCurrentBookId();
            if (serviceBookId != null && serviceBookId.equals(bookId)) {
                alreadyPlaying = true;
            }
        }

        if (alreadyPlaying) {
            syncWithService();
            capNhatThoiGianSeekbarGiaoDien();
        } else {
            currentChapterIndex = 0;
            playChapter(currentChapterIndex);
        }
    }

    private void syncWithService() {
        if (isBound && audioService != null && chaptersArray != null) {
            String serviceBookId = audioService.getCurrentBookId();
            if (serviceBookId != null && serviceBookId.equals(bookId)) {
                String serviceAudioUrl = audioService.getCurrentAudioUrl();
                if (serviceAudioUrl != null && !serviceAudioUrl.isEmpty()) {
                    for (int i = 0; i < chaptersArray.length(); i++) {
                        try {
                            JSONObject chapter = chaptersArray.getJSONObject(i);
                            String storagePath = chapter.optString("storage_path", "");
                            String chapterUrl = "";
                            if (!storagePath.isEmpty()) {
                                chapterUrl = com.example.myapplication.utils.SupabaseConfig.SUPABASE_URL + "/storage/v1/object/public/audios/" + storagePath;
                            } else {
                                chapterUrl = chapter.optString("audio_url", "");
                                if (chapterUrl.contains("sentufkmrzokwtkgtmvc.supabase.co")) {
                                    chapterUrl = chapterUrl.replace("sentufkmrzokwtkgtmvc.supabase.co", "sentufkmrzokwktgtmvc.supabase.co");
                                }
                            }
                            if (chapterUrl.equals(serviceAudioUrl)) {
                                currentChapterIndex = i;
                                currentAudioUrl = serviceAudioUrl;
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    private void playChapter(int index) {
        playChapterAndSeek(index, 0);
    }

    private void playChapterAndSeek(int index, int seekOffsetMs) {
        if (chaptersArray == null || index < 0 || index >= chaptersArray.length()) return;
        try {
            JSONObject chapter = chaptersArray.getJSONObject(index);
            String storagePath = chapter.optString("storage_path", "");
            String chapterTitle = chapter.optString("title", "Chương " + (index + 1));

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
            khoiDongPhatNhacServiceNgamAndSeek(currentAudioUrl, seekOffsetMs);
            Toast.makeText(this, "Đang phát " + chapterTitle, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            tvPlayerStatus.setText("Lỗi phát chương");
        }
    }

    private void khoiDongPhatNhacServiceNgam(String audioUrl) {
        playChapterAndSeek(currentChapterIndex, 0);
    }

    private void khoiDongPhatNhacServiceNgamAndSeek(String audioUrl, int seekOffsetMs) {
        Intent intent = new Intent(this, AudioPlayerService.class);

        intent.setAction(AudioPlayerService.ACTION_PLAY_NEW);
        intent.putExtra(AudioPlayerService.EXTRA_AUDIO_URL, audioUrl);
        intent.putExtra(AudioPlayerService.EXTRA_BOOK_ID, bookId);
        intent.putExtra(AudioPlayerService.EXTRA_TITLE, title);
        intent.putExtra(AudioPlayerService.EXTRA_AUTHOR, author);
        intent.putExtra(AudioPlayerService.EXTRA_COVER_URL, coverUrl);
        intent.putExtra("seek_to_position", seekOffsetMs);

        ContextCompat.startForegroundService(this, intent);

        tvPlayPause.setText("Ⅱ");
        tvPlayerStatus.setText("Đang phát bằng Foreground Service");
    }


    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, AudioPlayerService.class);
        bindService(intent, ketNoiDongBoVoiServiceTrinhPhat, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(ketNoiDongBoVoiServiceTrinhPhat);
            isBound = false;
        }
        progressHandler.removeCallbacks(progressRunnable);
    }

    private final ServiceConnection ketNoiDongBoVoiServiceTrinhPhat = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioPlayerService.LocalBinder binder = (AudioPlayerService.LocalBinder) service;
            audioService = binder.getService();
            isBound = true;
            syncWithService();
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
                    capNhatThoiGianSeekbarGiaoDien();
                }
                progressHandler.postDelayed(this, 1000);
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void capNhatThoiGianSeekbarGiaoDien() {
        if (audioService == null) return;
        
        if (chaptersArray != null && currentChapterIndex >= 0 && currentChapterIndex < chaptersArray.length()) {
            if (audioService.isPrepared()) {
                int serviceDur = audioService.getDuration();
                if (serviceDur > 0 && chapterDurationsMs != null && chapterDurationsMs[currentChapterIndex] != serviceDur) {
                    chapterDurationsMs[currentChapterIndex] = serviceDur;
                    recalculatePositions();
                }
            }
        }

        int current = audioService.getCurrentPosition();
        int cumulativeProgress = 0;
        if (chapterStartPositionsMs != null && currentChapterIndex >= 0 && currentChapterIndex < chapterStartPositionsMs.length) {
            cumulativeProgress = chapterStartPositionsMs[currentChapterIndex] + current;
        } else {
            cumulativeProgress = current;
        }

        int displayTotal = bookTotalDurationMs > 0 ? bookTotalDurationMs : audioService.getDuration();

        seekAudio.setMax(displayTotal);
        seekAudio.setProgress(cumulativeProgress);

        tvCurrentTime.setText(formatTime(cumulativeProgress));
        tvTotalTime.setText(formatTime(displayTotal));

        String chapterTitle = "";
        if (chaptersArray != null && currentChapterIndex >= 0 && currentChapterIndex < chaptersArray.length()) {
            try {
                JSONObject chapter = chaptersArray.getJSONObject(currentChapterIndex);
                chapterTitle = chapter.optString("title", "");
            } catch (Exception ignored) {}
        }

        if (audioService.isPlaying()) {
            tvPlayPause.setText("Ⅱ");
            if (!chapterTitle.isEmpty()) {
                tvPlayerStatus.setText(chapterTitle + " (Đang phát)");
            } else {
                tvPlayerStatus.setText("Đang phát");
            }
        } else {
            tvPlayPause.setText("▶");
            if (!chapterTitle.isEmpty()) {
                tvPlayerStatus.setText(chapterTitle + " (Đang tạm dừng)");
            } else {
                tvPlayerStatus.setText("Đang tạm dừng");
            }
        }

        float speed = audioService.getPlaybackSpeed();
        TextView tvSpeedValue = findViewById(R.id.tvSpeedValue);
        if (tvSpeedValue != null) {
            tvSpeedValue.setText(String.format(Locale.getDefault(), "%.2fx", speed));
        }
    }

    private String formatTime(int ms) {
        int totalSeconds = ms / 1000;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
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

    private void showSleepTimerDialog() {
        String[] options = {"Tắt hẹn giờ", "5 phút", "15 phút", "30 phút", "45 phút", "60 phút"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Hẹn giờ tắt")
                .setItems(options, (dialog, which) -> {
                    sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
                    if (which == 0) {
                        Toast.makeText(this, "Đã tắt hẹn giờ", Toast.LENGTH_SHORT).show();
                    } else {
                        int minutes = which == 1 ? 5 : which == 2 ? 15 : which == 3 ? 30 : which == 4 ? 45 : 60;
                        Toast.makeText(this, "Sẽ tắt sau " + minutes + " phút", Toast.LENGTH_SHORT).show();
                        sleepTimerRunnable = () -> {
                            if (isBound && audioService != null) {
                                Intent intent = new Intent(this, AudioPlayerService.class);
                                intent.setAction(AudioPlayerService.ACTION_STOP);
                                startService(intent);
                                finish();
                            }
                        };
                        sleepTimerHandler.postDelayed(sleepTimerRunnable, minutes * 60 * 1000L);
                    }
                })
                .show();
    }

    private void showChaptersDialog() {
        if (chaptersArray == null || chaptersArray.length() == 0) {
            Toast.makeText(this, "Chưa tải xong danh sách chương hoặc sách không có chương", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String[] chapterTitles = new String[chaptersArray.length()];
            for (int i = 0; i < chaptersArray.length(); i++) {
                JSONObject chapter = chaptersArray.getJSONObject(i);
                chapterTitles[i] = chapter.optString("title", "Chương " + (i + 1));
            }

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Danh sách chương")
                    .setItems(chapterTitles, (dialog, which) -> {
                        currentChapterIndex = which;
                        playChapter(currentChapterIndex);
                    })
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi hiển thị danh sách chương", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSpeedDialog() {
        String[] options = {"0.75x", "1.0x (Mặc định)", "1.25x", "1.5x", "1.75x", "2.0x"};
        float[] speeds = {0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tốc độ phát")
                .setItems(options, (dialog, which) -> {
                    currentSpeed = speeds[which];
                    if (isBound && audioService != null) {
                        audioService.setPlaybackSpeed(currentSpeed);
                    }
                    TextView tvSpeedValue = findViewById(R.id.tvSpeedValue);
                    if (tvSpeedValue != null) {
                        if (currentSpeed == 1.0f) {
                            tvSpeedValue.setText("1.0x");
                        } else {
                            tvSpeedValue.setText(currentSpeed + "x");
                        }
                    }
                })
                .show();
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
