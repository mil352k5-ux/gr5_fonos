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

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.example.myapplication.ui.PlayerActivity;
import com.example.myapplication.R;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

public class AudioPlayerService extends Service {

    public static final String ACTION_PLAY_NEW = "PLAY_NEW";
    public static final String ACTION_TOGGLE = "TOGGLE";
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_REWIND = "REWIND";
    public static final String ACTION_FORWARD = "FORWARD";

    public static final String EXTRA_AUDIO_URL = "audio_url";
    public static final String EXTRA_BOOK_ID = "book_id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_AUTHOR = "author";
    public static final String EXTRA_COVER_URL = "cover_url";

    private static final String CHANNEL_ID = "fonos_audio_channel";
    private static final int NOTIFICATION_ID = 1001;

    private MediaSessionCompat mediaSession;
    private MediaPlayer mediaPlayer;
    private boolean isPrepared = false;
    private boolean isPlaying = false;

    private String currentTitle = "Đang nghe sách";
    private String currentAuthor = "Fonos GR5";
    private String currentBookId = "";
    private String currentCoverUrl = "";
    private String currentStatus = "Đang chuẩn bị phát...";
    private Bitmap currentCoverBitmap = null;
    private String currentAudioUrl = "";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mediaSession = new MediaSessionCompat(this, "AudioPlayerService");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                chuyenTrangThaiPhatTamDung();
            }

            @Override
            public void onPause() {
                chuyenTrangThaiPhatTamDung();
            }

            @Override
            public void onStop() {
                dungPhatVaTatServiceNgam();
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo((int) pos);
            }

            @Override
            public void onRewind() {
                seekTo(Math.max(0, getCurrentPosition() - 5000));
                updateNotification(currentStatus);
            }

            @Override
            public void onFastForward() {
                seekTo(Math.min(getDuration(), getCurrentPosition() + 5000));
                updateNotification(currentStatus);
            }

            @Override
            public void onSkipToPrevious() {
                seekTo(Math.max(0, getCurrentPosition() - 5000));
                updateNotification(currentStatus);
            }

            @Override
            public void onSkipToNext() {
                seekTo(Math.min(getDuration(), getCurrentPosition() + 5000));
                updateNotification(currentStatus);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }

        String action = intent.getAction();

        if (ACTION_PLAY_NEW.equals(action)) {
            String audioUrl = intent.getStringExtra(EXTRA_AUDIO_URL);
            int seekToPos = intent.getIntExtra("seek_to_position", 0);

            currentBookId = intent.getStringExtra(EXTRA_BOOK_ID);
            currentTitle = intent.getStringExtra(EXTRA_TITLE);
            currentAuthor = intent.getStringExtra(EXTRA_AUTHOR);
            currentCoverUrl = intent.getStringExtra(EXTRA_COVER_URL);

            if (currentTitle == null) currentTitle = "Đang nghe sách";
            if (currentAuthor == null) currentAuthor = "Fonos GR5";

            currentCoverBitmap = null;
            loadCoverBitmap();

            startForeground(NOTIFICATION_ID, taoGiaoDienNotificationMedia("Đang chuẩn bị audio..."));
            phatNhacMoiChuanBiNgam(audioUrl, seekToPos);
        }

        if (ACTION_TOGGLE.equals(action)) {
            chuyenTrangThaiPhatTamDung();
        }

        if (ACTION_STOP.equals(action)) {
            dungPhatVaTatServiceNgam();
        }

        if (ACTION_REWIND.equals(action)) {
            seekTo(Math.max(0, getCurrentPosition() - 5000));
            updateNotification(currentStatus);
        }

        if (ACTION_FORWARD.equals(action)) {
            seekTo(Math.min(getDuration(), getCurrentPosition() + 5000));
            updateNotification(currentStatus);
        }

        return START_STICKY;
    }

    private void loadCoverBitmap() {
        if (currentCoverUrl != null && !currentCoverUrl.isEmpty()) {
            Glide.with(getApplicationContext())
                    .asBitmap()
                    .load(currentCoverUrl)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            currentCoverBitmap = resource;
                            updateNotification(currentStatus);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
        }
    }

    private void dongBoTrangThaiLenHeThong() {
        if (mediaSession == null) return;

        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentAuthor)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration());

        if (currentCoverBitmap != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentCoverBitmap);
        }
        mediaSession.setMetadata(metadataBuilder.build());

        long actions = PlaybackStateCompat.ACTION_PLAY 
                | PlaybackStateCompat.ACTION_PAUSE 
                | PlaybackStateCompat.ACTION_STOP
                | PlaybackStateCompat.ACTION_SEEK_TO
                | PlaybackStateCompat.ACTION_REWIND
                | PlaybackStateCompat.ACTION_FAST_FORWARD
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        int state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;

        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, getCurrentPosition(), 1.0f)
                .build();

        mediaSession.setPlaybackState(playbackState);
        mediaSession.setActive(true);
    }

    private void phatNhacMoiChuanBiNgam(String audioUrl, int seekToPos) {
        android.util.Log.d("AudioPlayerService", "playNewAudio called with URL: " + audioUrl + ", seekToPos: " + seekToPos);
        this.currentAudioUrl = audioUrl;
        giaiPhongBoNhoTrinhPhat();

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
                android.util.Log.d("AudioPlayerService", "MediaPlayer is prepared, starting playback");
                isPrepared = true;
                if (seekToPos > 0) {
                    mp.seekTo(seekToPos);
                }
                mp.start();
                isPlaying = true;
                dongBoTrangThaiLenHeThong();
                updateNotification("Đang phát");
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                android.util.Log.d("AudioPlayerService", "MediaPlayer completed playback");
                isPlaying = false;
                dongBoTrangThaiLenHeThong();
                updateNotification("Đã phát xong");

                // Gửi broadcast thông báo nghe xong chương sách
                Intent finishedIntent = new Intent(this, com.example.myapplication.receiver.ChapterFinishedReceiver.class);
                finishedIntent.putExtra("notification_title", "Đã nghe hết chương");
                finishedIntent.putExtra("notification_body", "Bạn đã nghe hết sách \"" + currentTitle + "\"");
                sendBroadcast(finishedIntent);
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                android.util.Log.e("AudioPlayerService", "MediaPlayer error: what = " + what + ", extra = " + extra);
                isPrepared = false;
                isPlaying = false;
                dongBoTrangThaiLenHeThong();
                updateNotification("Lỗi phát nhạc (code " + what + ")");
                return true;
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            android.util.Log.e("AudioPlayerService", "Exception playing audio: " + e.getMessage(), e);
            isPlaying = false;
            dongBoTrangThaiLenHeThong();
            updateNotification("Không mở được audio");
        }
    }

    private void chuyenTrangThaiPhatTamDung() {
        if (mediaPlayer == null || !isPrepared) return;

        if (isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            dongBoTrangThaiLenHeThong();
            updateNotification("Đang tạm dừng");
        } else {
            mediaPlayer.start();
            isPlaying = true;
            dongBoTrangThaiLenHeThong();
            updateNotification("Đang phát");
        }
    }

    private void dungPhatVaTatServiceNgam() {
        giaiPhongBoNhoTrinhPhat();
        stopForeground(true);
        stopSelf();
    }

    private void updateNotification(String status) {
        this.currentStatus = status;
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify(NOTIFICATION_ID, taoGiaoDienNotificationMedia(status));
        }
    }

    private Notification taoGiaoDienNotificationMedia(String status) {
        this.currentStatus = status;
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

        PendingIntent rewindPendingIntent = PendingIntent.getService(
                this,
                4,
                new Intent(this, AudioPlayerService.class).setAction(ACTION_REWIND),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent forwardPendingIntent = PendingIntent.getService(
                this,
                5,
                new Intent(this, AudioPlayerService.class).setAction(ACTION_FORWARD),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String toggleText = isPlaying ? "Pause" : "Play";
        int toggleIcon = isPlaying
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(currentTitle)
                .setContentText(status + " • " + currentAuthor)
                .setContentIntent(openPlayerPendingIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(isPlaying)
                .addAction(android.R.drawable.ic_media_rew, "Rewind", rewindPendingIntent)
                .addAction(toggleIcon, toggleText, togglePendingIntent)
                .addAction(android.R.drawable.ic_media_ff, "Forward", forwardPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));

        if (currentCoverBitmap != null) {
            builder.setLargeIcon(currentCoverBitmap);
        }

        return builder.build();
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

    private void giaiPhongBoNhoTrinhPhat() {
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

        if (mediaSession != null) {
            mediaSession.setActive(false);
        }
    }

    @Override
    public void onDestroy() {
        giaiPhongBoNhoTrinhPhat();
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        super.onDestroy();
    }

    public class LocalBinder extends android.os.Binder {
        public AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void seekTo(int positionMs) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(positionMs);
            dongBoTrangThaiLenHeThong();
        }
    }

    public void setPlaybackSpeed(float speed) {
        if (mediaPlayer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                boolean wasPlaying = mediaPlayer.isPlaying();
                android.media.PlaybackParams params = mediaPlayer.getPlaybackParams();
                params.setSpeed(speed);
                mediaPlayer.setPlaybackParams(params);
                if (!wasPlaying) {
                    mediaPlayer.pause();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public float getPlaybackSpeed() {
        if (mediaPlayer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isPrepared) {
            try {
                return mediaPlayer.getPlaybackParams().getSpeed();
            } catch (Exception e) {
                return 1.0f;
            }
        }
        return 1.0f;
    }

    public boolean isPrepared() {
        return isPrepared;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public String getCurrentAuthor() {
        return currentAuthor;
    }

    public String getCurrentBookId() {
        return currentBookId;
    }

    public String getCurrentCoverUrl() {
        return currentCoverUrl;
    }

    public String getCurrentAudioUrl() {
        return currentAudioUrl;
    }
}