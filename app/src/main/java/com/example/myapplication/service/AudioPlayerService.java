package com.example.myapplication.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.myapplication.PlayerActivity;
import com.example.myapplication.R;

public class AudioPlayerService extends Service {

    public static final String ACTION_PLAY_NEW = "PLAY_NEW";
    public static final String ACTION_TOGGLE = "TOGGLE";
    public static final String ACTION_STOP = "STOP";

    public static final String EXTRA_AUDIO_URL = "audio_url";
    public static final String EXTRA_BOOK_ID = "book_id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_AUTHOR = "author";
    public static final String EXTRA_COVER_URL = "cover_url";

    private static final String CHANNEL_ID = "fonos_audio_channel";
    private static final int NOTIFICATION_ID = 1001;

    private MediaPlayer mediaPlayer;
    private boolean isPrepared = false;
    private boolean isPlaying = false;

    private String currentTitle = "Đang nghe sách";
    private String currentAuthor = "Fonos GR5";
    private String currentBookId = "";
    private String currentCoverUrl = "";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }

        String action = intent.getAction();

        if (ACTION_PLAY_NEW.equals(action)) {
            String audioUrl = intent.getStringExtra(EXTRA_AUDIO_URL);

            currentBookId = intent.getStringExtra(EXTRA_BOOK_ID);
            currentTitle = intent.getStringExtra(EXTRA_TITLE);
            currentAuthor = intent.getStringExtra(EXTRA_AUTHOR);
            currentCoverUrl = intent.getStringExtra(EXTRA_COVER_URL);

            if (currentTitle == null) currentTitle = "Đang nghe sách";
            if (currentAuthor == null) currentAuthor = "Fonos GR5";

            startForeground(NOTIFICATION_ID, buildNotification("Đang chuẩn bị audio..."));
            playNewAudio(audioUrl);
        }

        if (ACTION_TOGGLE.equals(action)) {
            togglePlayPause();
        }

        if (ACTION_STOP.equals(action)) {
            stopAudioAndService();
        }

        return START_STICKY;
    }

    private void playNewAudio(String audioUrl) {
        releasePlayer();

        try {
            mediaPlayer = new MediaPlayer();

            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );

            mediaPlayer.setDataSource(audioUrl);

            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                mp.start();
                isPlaying = true;
                updateNotification("Đang phát");
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                updateNotification("Đã phát xong");
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            isPlaying = false;
            updateNotification("Không mở được audio");
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null || !isPrepared) return;

        if (isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            updateNotification("Đang tạm dừng");
        } else {
            mediaPlayer.start();
            isPlaying = true;
            updateNotification("Đang phát");
        }
    }

    private void stopAudioAndService() {
        releasePlayer();
        stopForeground(true);
        stopSelf();
    }

    private void updateNotification(String status) {
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(status));
        }
    }

    private Notification buildNotification(String status) {
        Intent openPlayerIntent = new Intent(this, PlayerActivity.class);
        openPlayerIntent.putExtra("book_id", currentBookId);
        openPlayerIntent.putExtra("title", currentTitle);
        openPlayerIntent.putExtra("author", currentAuthor);
        openPlayerIntent.putExtra("cover_url", currentCoverUrl);

        PendingIntent openPlayerPendingIntent = PendingIntent.getActivity(
                this,
                1,
                openPlayerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent togglePendingIntent = PendingIntent.getService(
                this,
                2,
                new Intent(this, AudioPlayerService.class).setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                3,
                new Intent(this, AudioPlayerService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String toggleText = isPlaying ? "Pause" : "Play";
        int toggleIcon = isPlaying
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(currentTitle)
                .setContentText(status + " • " + currentAuthor)
                .setContentIntent(openPlayerPendingIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(isPlaying)
                .addAction(toggleIcon, toggleText, togglePendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Fonos Audio Player",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception ignored) {}

            mediaPlayer.release();
            mediaPlayer = null;
        }

        isPrepared = false;
        isPlaying = false;
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}