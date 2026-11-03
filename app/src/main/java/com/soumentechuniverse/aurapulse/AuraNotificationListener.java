package com.soumentechuniverse.aurapulse;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.MediaMetadata;
import android.os.Build;
import android.os.Bundle;
import android.content.SharedPreferences;
import java.util.Set;
import android.content.Intent;

public class AuraNotificationListener extends NotificationListenerService {
    private static MediaController activeController;
    private static String activeMediaPackage;

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if(sbn==null || sbn.getPackageName().equals(getPackageName())) return;
        SharedPreferences prefs=getSharedPreferences("aura_settings",MODE_PRIVATE);
        Set<String> blocked=prefs.getStringSet("blocked_apps",java.util.Collections.<String>emptySet());
        if(blocked!=null && blocked.contains(sbn.getPackageName())) return;
        Notification n=sbn.getNotification();
        if(n==null || n.extras==null) return;
        Bundle extras=n.extras;

        if(Build.VERSION.SDK_INT>=21) {
            Object tokenValue=extras.get(Notification.EXTRA_MEDIA_SESSION);
            if(tokenValue instanceof MediaSession.Token) {
                try {
                    activeController=new MediaController(this,(MediaSession.Token)tokenValue);
                    activeMediaPackage=sbn.getPackageName();
                    MediaMetadata metadata=activeController.getMetadata();
                    if(metadata!=null) {
                        String title=metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                        String artist=metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                        if(artist==null) artist=metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
                        Intent media=new Intent(IslandOverlayService.ACTION_MEDIA);
                        media.setPackage(getPackageName());
                        media.putExtra(IslandOverlayService.EXTRA_TITLE,title==null?"Now Playing":title);
                        media.putExtra(IslandOverlayService.EXTRA_TEXT,artist==null?"Music playing":artist);
                        sendBroadcast(media);
                        return;
                    }
                } catch(Exception ignored) {}
            }
        }

        CharSequence title=extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text=extras.getCharSequence(Notification.EXTRA_TEXT);
        if(title==null || title.length()==0) return;
        Intent update=new Intent(IslandOverlayService.ACTION_NOTIFICATION);
        update.setPackage(getPackageName());
        String appLabel=sbn.getPackageName();
        try { appLabel=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(sbn.getPackageName(),0)).toString(); } catch(Exception ignored) {}
        update.putExtra(IslandOverlayService.EXTRA_TITLE,appLabel+": "+title.toString());
        update.putExtra(IslandOverlayService.EXTRA_TEXT,text==null?"New notification":text.toString());
        sendBroadcast(update);
    }


    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if(sbn!=null && activeMediaPackage!=null && activeMediaPackage.equals(sbn.getPackageName())) {
            activeController=null; activeMediaPackage=null;
            Intent stop=new Intent(IslandOverlayService.ACTION_MEDIA);
            stop.setPackage(getPackageName());
            stop.putExtra(IslandOverlayService.EXTRA_TITLE,"");
            sendBroadcast(stop);
        }
    }

    public static void togglePlayback() {
        if(activeController==null) return;
        try {
            android.media.session.PlaybackState state=activeController.getPlaybackState();
            if(state!=null && state.getState()==android.media.session.PlaybackState.STATE_PLAYING)
                activeController.getTransportControls().pause();
            else activeController.getTransportControls().play();
        } catch(Exception ignored) {}
    }
    public static void skipNext() {
        if(activeController!=null) try{activeController.getTransportControls().skipToNext();}catch(Exception ignored){}
    }
    public static void skipPrevious() {
        if(activeController!=null) try{activeController.getTransportControls().skipToPrevious();}catch(Exception ignored){}
    }
}