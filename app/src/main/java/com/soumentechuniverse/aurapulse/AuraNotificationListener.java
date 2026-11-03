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
import org.json.JSONArray;
import org.json.JSONObject;

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

        if(Build.VERSION.SDK_INT>=21 && prefs.getBoolean("show_media",true)) {
            Object tokenValue=extras.get(Notification.EXTRA_MEDIA_SESSION);
            if(tokenValue instanceof MediaSession.Token) {
                try {
                    activeController=new MediaController(this,(MediaSession.Token)tokenValue);
                    activeMediaPackage=sbn.getPackageName();
                    android.media.session.PlaybackState playback=activeController.getPlaybackState();
                    MediaMetadata metadata=activeController.getMetadata();
                    if(metadata!=null) {
                        if(playback==null || (playback.getState()!=android.media.session.PlaybackState.STATE_PLAYING && playback.getState()!=android.media.session.PlaybackState.STATE_BUFFERING)) {
                            Intent paused=new Intent(IslandOverlayService.ACTION_MEDIA);
                            paused.setPackage(getPackageName());
                            paused.putExtra(IslandOverlayService.EXTRA_TITLE,"");
                            sendBroadcast(paused);
                            return;
                        }
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

        if(!prefs.getBoolean("show_notifications",true)) return;
        CharSequence title=extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text=extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if(text==null || text.length()==0) text=extras.getCharSequence(Notification.EXTRA_TEXT);
        if((title==null || title.length()==0) && n.tickerText!=null) title=n.tickerText;
        if((title==null || title.length()==0) && Build.VERSION.SDK_INT>=24) {
            CharSequence[] lines=extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if(lines!=null && lines.length>0) { title=lines[0]; if(lines.length>1) text=lines[lines.length-1]; }
        }
        if(title==null || title.length()==0) title="New notification";
        String appLabel=sbn.getPackageName();
        try { appLabel=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(sbn.getPackageName(),0)).toString(); } catch(Exception ignored) {}
        String fullTitle=appLabel+": "+title.toString();
        String body=text==null?"New notification":text.toString();
        if(body.length()>500) body=body.substring(0,500);
        if(prefs.getBoolean("island_service_running",false)) {
            Intent update=new Intent(IslandOverlayService.ACTION_NOTIFICATION);
            update.setPackage(getPackageName());
            update.putExtra(IslandOverlayService.EXTRA_TITLE,fullTitle);
            update.putExtra(IslandOverlayService.EXTRA_TEXT,body);
            sendBroadcast(update);
        } else {
            try {
                String raw=prefs.getString("notification_queue","[]");
                JSONArray queue=new JSONArray(raw);
                boolean duplicate=false;
                for(int i=0;i<queue.length();i++) {
                    JSONObject item=queue.optJSONObject(i);
                    if(item!=null && fullTitle.equals(item.optString("title")) && body.equals(item.optString("body"))) { duplicate=true; break; }
                }
                if(!duplicate) {
                    if(queue.length()>=30) queue.remove(0);
                    JSONObject item=new JSONObject(); item.put("title",fullTitle); item.put("body",body); queue.put(item);
                    prefs.edit().putString("notification_queue",queue.toString()).apply();
                }
            } catch(Exception ignored) {}
        }
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