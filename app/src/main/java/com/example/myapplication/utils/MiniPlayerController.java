package com.example.myapplication.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.myapplication.PlayerActivity;
import com.example.myapplication.R;
import com.example.myapplication.service.AudioPlayerService;

public class MiniPlayerController {

    private final Activity activity;
    private View miniPlayerContainer;
    private ProgressBar miniProgressBar;
    private ImageView imgMiniCover;
    private TextView tvMiniTitle, tvMiniAuthor;
    private ImageView btnMiniPlayPause, btnMiniClose;
    private ImageView btnMiniRewind, btnMiniForward;

    private AudioPlayerService audioService;
    private boolean isBound = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBound && audioService != null && audioService.isPrepared()) {
                updateProgress();
                updatePlayPauseIcon();
                if (miniPlayerContainer.getVisibility() == View.GONE) {
                    miniPlayerContainer.setVisibility(View.VISIBLE);
                }
            } else if (isBound && (audioService == null || !audioService.isPrepared())) {
                if (miniPlayerContainer != null && miniPlayerContainer.getVisibility() == View.VISIBLE) {
                    miniPlayerContainer.setVisibility(View.GONE);
                }
            }
            handler.postDelayed(this, 1000);
        }
    };

    public MiniPlayerController(Activity activity) {
        this.activity = activity;
    }

    public void init() {
        miniPlayerContainer = activity.findViewById(R.id.mini_player_container);
        if (miniPlayerContainer == null) return;

        miniProgressBar = activity.findViewById(R.id.mini_progress_bar);
        imgMiniCover = activity.findViewById(R.id.imgMiniCover);
        tvMiniTitle = activity.findViewById(R.id.tvMiniTitle);
        tvMiniAuthor = activity.findViewById(R.id.tvMiniAuthor);
        btnMiniPlayPause = activity.findViewById(R.id.btnMiniPlayPause);
        btnMiniClose = activity.findViewById(R.id.btnMiniClose);
        btnMiniRewind = activity.findViewById(R.id.btnMiniRewind);
        btnMiniForward = activity.findViewById(R.id.btnMiniForward);

        miniPlayerContainer.setOnClickListener(v -> {
            if (isBound && audioService != null && audioService.isPrepared()) {
                Intent intent = new Intent(activity, PlayerActivity.class);
                intent.putExtra("book_id", audioService.getCurrentBookId());
                intent.putExtra("title", audioService.getCurrentTitle());
                intent.putExtra("author", audioService.getCurrentAuthor());
                intent.putExtra("cover_url", audioService.getCurrentCoverUrl());
                activity.startActivity(intent);
            }
        });

        if (btnMiniPlayPause != null) {
            btnMiniPlayPause.setOnClickListener(v -> {
                if (isBound && audioService != null && audioService.isPrepared()) {
                    audioService.startService(new Intent(activity, AudioPlayerService.class).setAction(AudioPlayerService.ACTION_TOGGLE));
                    handler.postDelayed(this::updatePlayPauseIcon, 100);
                }
            });
        }

        if (btnMiniRewind != null) {
            btnMiniRewind.setOnClickListener(v -> {
                if (isBound && audioService != null && audioService.isPrepared()) {
                    audioService.startService(new Intent(activity, AudioPlayerService.class).setAction(AudioPlayerService.ACTION_REWIND));
                }
            });
        }

        if (btnMiniForward != null) {
            btnMiniForward.setOnClickListener(v -> {
                if (isBound && audioService != null && audioService.isPrepared()) {
                    audioService.startService(new Intent(activity, AudioPlayerService.class).setAction(AudioPlayerService.ACTION_FORWARD));
                }
            });
        }

        if (btnMiniClose != null) {
            btnMiniClose.setOnClickListener(v -> {
                if (isBound && audioService != null) {
                    audioService.startService(new Intent(activity, AudioPlayerService.class).setAction(AudioPlayerService.ACTION_STOP));
                    miniPlayerContainer.setVisibility(View.GONE);
                }
            });
        }
    }

    public void onStart() {
        if (miniPlayerContainer == null) return;
        Intent intent = new Intent(activity, AudioPlayerService.class);
        activity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void onStop() {
        handler.removeCallbacks(progressRunnable);
        if (isBound) {
            activity.unbindService(serviceConnection);
            isBound = false;
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioPlayerService.LocalBinder binder = (AudioPlayerService.LocalBinder) service;
            audioService = binder.getService();
            isBound = true;
            updateUI();
            handler.post(progressRunnable);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            audioService = null;
        }
    };

    public void updateUI() {
        if (!isBound || audioService == null || !audioService.isPrepared()) {
            if (miniPlayerContainer != null) {
                miniPlayerContainer.setVisibility(View.GONE);
            }
            return;
        }

        if (miniPlayerContainer != null) {
            miniPlayerContainer.setVisibility(View.VISIBLE);
        }

        if (tvMiniTitle != null) tvMiniTitle.setText(audioService.getCurrentTitle());
        if (tvMiniAuthor != null) tvMiniAuthor.setText(audioService.getCurrentAuthor());

        updatePlayPauseIcon();

        if (imgMiniCover != null) {
            Glide.with(activity)
                    .load(audioService.getCurrentCoverUrl())
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .into(imgMiniCover);
        }

        updateProgress();
    }

    private void updatePlayPauseIcon() {
        if (btnMiniPlayPause != null && audioService != null) {
            if (audioService.isPlaying()) {
                btnMiniPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                btnMiniPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }
        }
    }

    private void updateProgress() {
        if (isBound && audioService != null && audioService.isPrepared()) {
            int duration = audioService.getDuration();
            int current = audioService.getCurrentPosition();
            if (duration > 0 && miniProgressBar != null) {
                int progress = (int) (((float) current / duration) * 100);
                miniProgressBar.setProgress(progress);
            }
        }
    }
}
