package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ChapterFinishedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String chapterName = intent.getStringExtra("chapter_name");

        if (chapterName == null || chapterName.isEmpty()) {
            chapterName = "chương hiện tại";
        }

        NotificationHelper.showNotification(
                context,
                1001,
                "Đã đọc hết chương",
                "Bạn đã đọc hết " + chapterName
        );
    }
}