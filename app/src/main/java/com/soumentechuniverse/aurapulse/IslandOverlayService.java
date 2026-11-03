package com.soumentechuniverse.aurapulse;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.pm.ServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.media.AudioManager;
import android.os.CountDownTimer;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.SharedPreferences;
import android.view.MotionEvent;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IslandOverlayService extends Service {
    public static final String ACTION_NOTIFICATION = "com.soumentechuniverse.aurapulse.NOTIFICATION";
    public static final String ACTION_MEDIA = "com.soumentechuniverse.aurapulse.MEDIA";
    public static final String ACTION_TIMER = "com.soumentechuniverse.aurapulse.TIMER";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_TEXT = "text";
    private WindowManager windowManager;
    private LinearLayout capsule, mediaControls;
    private TextView titleView, detailView;
    private BroadcastReceiver updates;
    private final ArrayList<String[]> notificationQueue=new ArrayList<>();
    private boolean showingNotification=false;
    private CountDownTimer countdown;

    @Override public void onCreate() {
        super.onCreate();
        startIslandForeground();
        windowManager=(WindowManager)getSystemService(WINDOW_SERVICE);
        createCapsule();
        restoreQueue();
        if(!notificationQueue.isEmpty()) showQueuedNotification();
        IntentFilter filter=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ACTION_NOTIFICATION);
        filter.addAction(ACTION_MEDIA);
        filter.addAction(ACTION_TIMER);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        updates=new BroadcastReceiver() {
            @Override public void onReceive(Context context,Intent intent) {
                if(Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                    int level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
                    int scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,100);
                    int pct=(level>=0 && scale>0)?(level*100/scale):-1;
                    int status=intent.getIntExtra(BatteryManager.EXTRA_STATUS,-1);
                    boolean charging=status==BatteryManager.BATTERY_STATUS_CHARGING || status==BatteryManager.BATTERY_STATUS_FULL;
                    titleView.setText(charging?"⚡  Charging  •  "+pct+"%":"AURA ISLAND  •  "+pct+"% battery");
                    detailView.setText(charging?"Power connected":"Tap app to manage your island");
                    mediaControls.setVisibility(View.GONE);
                } else if(ACTION_NOTIFICATION.equals(intent.getAction())) {
                    String title=intent.getStringExtra(EXTRA_TITLE);
                    String body=intent.getStringExtra(EXTRA_TEXT);
                    if(title!=null && !title.trim().isEmpty()) {
                        notificationQueue.add(new String[]{title,body==null?"New notification":body});
                        saveQueue();
                        showQueuedNotification();
                    }
                } else if(Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
                    boolean plugged=intent.getIntExtra("state",0)==1;
                    titleView.setText(plugged?"🎧  Headphones connected":"AURA ISLAND");
                    detailView.setText(plugged?"Audio device connected":"Headphones disconnected");
                    mediaControls.setVisibility(View.GONE);
                } else if("android.intent.action.PHONE_STATE".equals(intent.getAction())) {
                    String state=intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if(TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                        titleView.setText("☎  Incoming call"); detailView.setText("Your phone is ringing");
                    } else if(TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                        titleView.setText("☎  Call in progress"); detailView.setText("Tap to expand controls");
                    } else if(TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                        titleView.setText("AURA ISLAND"); detailView.setText("Ready for live activities");
                    }
                    mediaControls.setVisibility(View.GONE);
                } else if(ACTION_TIMER.equals(intent.getAction())) {
                    long seconds=intent.getLongExtra("seconds",0);
                    if(countdown!=null) countdown.cancel();
                    if(seconds>0) {
                        countdown=new CountDownTimer(seconds*1000,1000) {
                            public void onTick(long left) {
                                long total=left/1000;
                                titleView.setText("◷  Timer  "+String.format(java.util.Locale.getDefault(),"%02d:%02d",total/60,total%60));
                                detailView.setText("Countdown running");
                                mediaControls.setVisibility(View.GONE);
                            }
                            public void onFinish() {
                                titleView.setText("⏰  Timer finished");
                                detailView.setText("Your countdown is complete");
                            }
                        }.start();
                    }
                } else if("android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) {
                    AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);
                    int current=audio==null?0:audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int max=audio==null?1:audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    titleView.setText("♫  Media volume  "+current+"/"+max);
                    detailView.setText("Use island controls to adjust volume");
                    mediaControls.setVisibility(View.VISIBLE);
                } else if(ACTION_MEDIA.equals(intent.getAction())) {
                    String title=intent.getStringExtra(EXTRA_TITLE);
                    String artist=intent.getStringExtra(EXTRA_TEXT);
                    titleView.setText("♫  "+(title==null?"Now Playing":title));
                    detailView.setText(artist==null?"Music":artist);
                    mediaControls.setVisibility(View.VISIBLE);
                }
            }
        };
        if(Build.VERSION.SDK_INT>=33) registerReceiver(updates,filter,Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(updates,filter);
        Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if(battery!=null) updates.onReceive(this,battery);
    }



    private void startIslandForeground() {
        final String channelId="aura_island_service";
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26) {
            NotificationChannel channel=new NotificationChannel(channelId,"AURA PULSE Island",NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps your enabled AURA Island available.");
            manager.createNotificationChannel(channel);
        }
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent pending=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,channelId):new Notification.Builder(this);
        Notification notification=builder.setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("AURA PULSE is active")
            .setContentText("Your live island is running")
            .setContentIntent(pending).setOngoing(true).setShowWhen(false).build();
        if(Build.VERSION.SDK_INT>=34) startForeground(71,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(71,notification);
    }

    private void saveQueue() {
        try {
            JSONArray array=new JSONArray();
            for(String[] item:notificationQueue) {
                JSONObject object=new JSONObject();
                object.put("title",item[0]); object.put("body",item[1]); array.put(object);
            }
            getSharedPreferences("aura_settings",MODE_PRIVATE).edit().putString("notification_queue",array.toString()).apply();
        } catch(Exception ignored) {}
    }
    private void restoreQueue() {
        try {
            String raw=getSharedPreferences("aura_settings",MODE_PRIVATE).getString("notification_queue","[]");
            JSONArray array=new JSONArray(raw);
            for(int i=0;i<array.length();i++) {
                JSONObject item=array.getJSONObject(i);
                notificationQueue.add(new String[]{item.optString("title"),item.optString("body")});
            }
        } catch(Exception ignored) {}
    }

    private void showQueuedNotification() {
        if(notificationQueue.isEmpty()) { showingNotification=false; return; }
        showingNotification=true;
        String[] item=notificationQueue.get(0);
        titleView.setText(item[0]);
        detailView.setText(item[1]+(notificationQueue.size()>1?"  •  +"+(notificationQueue.size()-1):""));
        mediaControls.setVisibility(View.GONE);
    }
    private void dismissCurrentNotification() {
        if(showingNotification && !notificationQueue.isEmpty()) {
            notificationQueue.remove(0);
            saveQueue();
            if(notificationQueue.isEmpty()) {
                showingNotification=false;
                titleView.setText("AURA ISLAND");
                detailView.setText("Notification dismissed");
            } else showQueuedNotification();
        }
    }
    private TextView controlButton(String label, Runnable action) {
        TextView button=new TextView(this);
        button.setText(label); button.setTextColor(Color.WHITE); button.setTextSize(17);
        button.setGravity(Gravity.CENTER); button.setPadding(dp(14),dp(3),dp(14),dp(3));
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void createCapsule() {
        capsule=new LinearLayout(this);
        capsule.setOrientation(LinearLayout.VERTICAL);
        capsule.setGravity(Gravity.CENTER);
        capsule.setPadding(dp(22),dp(11),dp(22),dp(9));
        GradientDrawable shape=new GradientDrawable();
        shape.setColor(Color.rgb(0,0,0)); shape.setCornerRadius(dp(getSharedPreferences("aura_settings",MODE_PRIVATE).getInt("corner_radius",30)));
        shape.setStroke(dp(1),Color.rgb(38,48,62)); capsule.setBackground(shape);
        titleView=new TextView(this); titleView.setText("AURA ISLAND"); titleView.setTextColor(Color.rgb(101,232,255));
        titleView.setTextSize(12); titleView.setTypeface(Typeface.DEFAULT,Typeface.BOLD); titleView.setMaxLines(1);
        SharedPreferences prefs=getSharedPreferences("aura_settings",MODE_PRIVATE);
        int accent=prefs.getInt("accent",Color.rgb(101,232,255));
        titleView.setTextColor(accent);
        detailView=new TextView(this); detailView.setText("Live activities appear here"); detailView.setTextColor(Color.WHITE);
        detailView.setTextSize(11); detailView.setMaxLines(1);
        capsule.addView(titleView); capsule.addView(detailView);
        mediaControls=new LinearLayout(this);
        mediaControls.setGravity(Gravity.CENTER); mediaControls.setOrientation(LinearLayout.HORIZONTAL);
        mediaControls.addView(controlButton("⏮", () -> AuraNotificationListener.skipPrevious()));
        mediaControls.addView(controlButton("▶ / ❚❚", () -> AuraNotificationListener.togglePlayback()));
        mediaControls.addView(controlButton("⏭", () -> AuraNotificationListener.skipNext()));
        mediaControls.addView(controlButton("－", () -> adjustVolume(false)));
        mediaControls.addView(controlButton("＋", () -> adjustVolume(true)));
        mediaControls.setVisibility(View.GONE);
        capsule.addView(mediaControls);
        int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;
        params.width=dp(getSharedPreferences("aura_settings",MODE_PRIVATE).getInt("capsule_width",240));
        params.y=dp(getSharedPreferences("aura_settings",MODE_PRIVATE).getInt("position_y",10));
        windowManager.addView(capsule,params);
        capsule.setOnClickListener(v -> {
            if(showingNotification) {
                if(notificationQueue.size()>1) { notificationQueue.add(notificationQueue.remove(0)); showQueuedNotification(); }
                else dismissCurrentNotification();
            } else mediaControls.setVisibility(mediaControls.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);
        });
        capsule.setOnLongClickListener(v -> { stopSelf(); return true; });
        capsule.setOnTouchListener(new View.OnTouchListener() {
            private int startX,startY; private float downX,downY; private long downAt;
            @Override public boolean onTouch(View v,MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    startX=params.x; startY=params.y; downX=event.getRawX(); downY=event.getRawY(); downAt=System.currentTimeMillis(); return true;
                }
                if(event.getAction()==MotionEvent.ACTION_MOVE) {
                    params.x=startX+(int)(event.getRawX()-downX);
                    params.y=startY+(int)(event.getRawY()-downY);
                    try{windowManager.updateViewLayout(capsule,params);}catch(Exception ignored){}
                    return true;
                }
                if(event.getAction()==MotionEvent.ACTION_UP) {
                    float dx=event.getRawX()-downX;
                    if(Math.abs(dx)>dp(85) && showingNotification) { dismissCurrentNotification(); return true; }
                    float dy=event.getRawY()-downY;
                    if(Math.abs(dx)<dp(10) && Math.abs(dy)<dp(10)) {
                        if(System.currentTimeMillis()-downAt>600) stopSelf();
                        else capsule.performClick();
                    }
                    return true;
                }
                return false;
            }
        });
    }
    private void adjustVolume(boolean up) {
        AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        if(audio!=null) audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,up?AudioManager.ADJUST_RAISE:AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null && ACTION_TIMER.equals(intent.getAction())) {
            Intent update=new Intent(ACTION_TIMER); update.setPackage(getPackageName());
            update.putExtra("seconds",intent.getLongExtra("seconds",0)); sendBroadcast(update);
        }
        return START_STICKY;
    }
    @Override public void onDestroy(){
        if(countdown!=null) countdown.cancel();
        if(updates!=null) try{unregisterReceiver(updates);}catch(Exception ignored){}
        if(capsule!=null && windowManager!=null) try{windowManager.removeView(capsule);}catch(Exception ignored){}
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}