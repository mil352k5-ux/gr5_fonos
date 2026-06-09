package com.example.myapplication.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.myapplication.utils.NotificationHelper;

public class ChapterFinishedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String chapterName = intent.getStringExtra("chapter_name");
        String title = intent.getStringExtra("notification_title");
        String body = intent.getStringExtra("notification_body");

        if (chapterName == null || chapterName.isEmpty()) {
            chapterName = "chương hiện tại";
        }

        if (title == null || title.isEmpty()) {
            title = "Đã đọc hết chương";
        }

        if (body == null || body.isEmpty()) {
            body = "Bạn đã đọc hết " + chapterName;
        }

        NotificationHelper.showNotification(
                context,
                1001,
                title,
                body
        );
    }
}
