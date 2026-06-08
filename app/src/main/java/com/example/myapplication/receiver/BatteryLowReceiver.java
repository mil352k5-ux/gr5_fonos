package com.example.myapplication.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.myapplication.utils.NotificationHelper;

public class BatteryLowReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) {
            NotificationHelper.showNotification(
                    context,
                    1002,
                    "Pin yếu",
                    "Thiết bị đang yếu pin. Hãy sạc pin để tiếp tục đọc sách."
            );
        }
    }
}
