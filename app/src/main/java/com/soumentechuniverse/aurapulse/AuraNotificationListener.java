package com.soumentechuniverse.aurapulse;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.os.Bundle;
import android.content.Intent;

public class AuraNotificationListener extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if(sbn==null || sbn.getPackageName().equals(getPackageName())) return;
        Notification n=sbn.getNotification();
        if(n==null || n.extras==null) return;
        Bundle extras=n.extras;
        CharSequence title=extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text=extras.getCharSequence(Notification.EXTRA_TEXT);
        if(title==null || title.length()==0) return;
        Intent update=new Intent(IslandOverlayService.ACTION_NOTIFICATION);
        update.setPackage(getPackageName());
        update.putExtra(IslandOverlayService.EXTRA_TITLE,title.toString());
        update.putExtra(IslandOverlayService.EXTRA_TEXT,text==null?"New notification":text.toString());
        sendBroadcast(update);
    }
}