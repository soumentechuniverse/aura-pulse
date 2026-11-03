package com.soumentechuniverse.aurapulse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        boolean enabled = context.getSharedPreferences("aura_settings", Context.MODE_PRIVATE)
            .getBoolean("always_on", true);
        if (!enabled || (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context))) return;
        Intent service = new Intent(context, IslandOverlayService.class);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service);
        else context.startService(service);
    }
}