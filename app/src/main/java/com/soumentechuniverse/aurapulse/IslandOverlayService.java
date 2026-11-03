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
import android.media.AudioDeviceInfo;
import android.media.AudioDeviceCallback;
import android.os.Handler;
import android.os.CountDownTimer;
import android.os.SystemClock;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.SharedPreferences;
import android.view.MotionEvent;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.DisplayCutout;
import android.graphics.Rect;
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
    private boolean expanded=false;
    private boolean chargingActive=false,headsetActive=false,callActive=false,mediaActive=false,timerActive=false;
    private CountDownTimer countdown;
    private long timerEndElapsed=0;
    private final Handler uiHandler=new Handler();
    private AudioManager audioManager;
    private AudioDeviceCallback audioCallback;

    @Override public void onCreate() {
        super.onCreate();
        startIslandForeground();
        getSharedPreferences("aura_settings",MODE_PRIVATE).edit().putBoolean("island_service_running",true).apply();
        windowManager=(WindowManager)getSystemService(WINDOW_SERVICE);
        createCapsule();
        registerAudioDeviceMonitoring();
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
                    boolean charging=(status==BatteryManager.BATTERY_STATUS_CHARGING || status==BatteryManager.BATTERY_STATUS_FULL) && getSharedPreferences("aura_settings",MODE_PRIVATE).getBoolean("show_charging",true);
                    chargingActive=charging;
                    if(charging) {
                        titleView.setText("⚡  Charging  •  "+pct+"%");
                        detailView.setText("Power connected");
                        mediaControls.setVisibility(View.GONE);
                        showCapsuleAnimated();
                    } else if(!mediaActive && !callActive && !headsetActive && notificationQueue.isEmpty() && !timerActive) {
                        showIdleBattery(pct);
                        hideIfIdle();
                    }
                } else if(ACTION_NOTIFICATION.equals(intent.getAction())) {
                    String title=intent.getStringExtra(EXTRA_TITLE);
                    String body=intent.getStringExtra(EXTRA_TEXT);
                    if(getSharedPreferences("aura_settings",MODE_PRIVATE).getBoolean("show_notifications",true) && title!=null && !title.trim().isEmpty()) {
                        String safeBody=body==null?"New notification":body;
                        boolean duplicate=false;
                        for(String[] queued:notificationQueue) {
                            if(queued[0].equals(title) && queued[1].equals(safeBody)) { duplicate=true; break; }
                        }
                        if(!duplicate) {
                            if(notificationQueue.size()>=30) notificationQueue.remove(0);
                            notificationQueue.add(new String[]{title,safeBody});
                            showCapsuleAnimated();
                            saveQueue();
                            showQueuedNotification();
                        }
                    }
                } else if(Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
                    boolean plugged=intent.getIntExtra("state",0)==1 && getSharedPreferences("aura_settings",MODE_PRIVATE).getBoolean("show_earbuds",true);
                    headsetActive=plugged;
                    titleView.setText(plugged?"🎧  Headphones connected":"AURA ISLAND");
                    detailView.setText(plugged?"Audio device connected":"Headphones disconnected");
                    mediaControls.setVisibility(View.GONE);
                    if(plugged) showCapsuleAnimated(); else hideIfIdle();
                } else if("android.intent.action.PHONE_STATE".equals(intent.getAction())) {
                    String state=intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if(!getSharedPreferences("aura_settings",MODE_PRIVATE).getBoolean("show_calls",true)) { callActive=false; hideIfIdle(); } else if(TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                        callActive=true; showCapsuleAnimated();
                        titleView.setText("☎  Incoming call"); detailView.setText("Your phone is ringing");
                    } else if(TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                        callActive=true; showCapsuleAnimated();
                        titleView.setText("☎  Call in progress"); detailView.setText("Tap to expand controls");
                    } else if(TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                        callActive=false;
                        titleView.setText("AURA ISLAND"); detailView.setText("Ready for live activities");
                        hideIfIdle();
                    }
                    mediaControls.setVisibility(View.GONE);
                } else if(ACTION_TIMER.equals(intent.getAction())) {
                    long seconds=intent.getLongExtra("seconds",0);
                    if(countdown!=null) countdown.cancel();
                    if(seconds>0 && getSharedPreferences("aura_settings",MODE_PRIVATE).getBoolean("show_timer",true)) startCountdown(seconds);
                } else if("android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) {
                    AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);
                    int current=audio==null?0:audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int max=audio==null?1:audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    showCapsuleAnimated();
                    titleView.setText("♫  Media volume  "+current+"/"+max);
                    detailView.setText("Volume changed");
                    mediaControls.setVisibility(View.GONE);
                    uiHandler.removeCallbacksAndMessages(null);
                    uiHandler.postDelayed(() -> hideIfIdle(), 2500L);
                } else if(ACTION_MEDIA.equals(intent.getAction())) {
                    String title=intent.getStringExtra(EXTRA_TITLE);
                    String artist=intent.getStringExtra(EXTRA_TEXT);
                    mediaActive=title!=null && !title.isEmpty() && getSharedPreferences("aura_settings",MODE_PRIVATE).getBoolean("show_media",true);
                    if(mediaActive) {
                        showCapsuleAnimated();
                        titleView.setText("♫  "+title);
                        detailView.setText(artist==null?"Music":artist);
                        mediaControls.setVisibility(View.VISIBLE);
                    } else {
                        mediaControls.setVisibility(View.GONE);
                        if(!notificationQueue.isEmpty()) showQueuedNotification();
                        else if(chargingActive) {
                            titleView.setText("⚡  Charging");
                            detailView.setText("Power connected");
                            showCapsuleAnimated();
                        } else if(callActive) {
                            titleView.setText("☎  Call in progress");
                            detailView.setText("Call active");
                            showCapsuleAnimated();
                        } else hideIfIdle();
                    }
                }
            }
        };
        if(Build.VERSION.SDK_INT>=33) registerReceiver(updates,filter,Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(updates,filter);
        Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if(battery!=null) updates.onReceive(this,battery);
        Intent headset=registerReceiver(null,new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        if(headset!=null) updates.onReceive(this,headset);
        long savedEnd=getSharedPreferences("aura_settings",MODE_PRIVATE).getLong("timer_end_elapsed",0L);
        long remaining=savedEnd-SystemClock.elapsedRealtime();
        if(remaining>0) startCountdown(Math.max(1L,(remaining+999L)/1000L));
        else getSharedPreferences("aura_settings",MODE_PRIVATE).edit().remove("timer_end_elapsed").apply();
    }



    private void startCountdown(long seconds) {
        if (countdown != null) countdown.cancel();
        timerActive = true;
        timerEndElapsed = SystemClock.elapsedRealtime() + seconds * 1000L;
        getSharedPreferences("aura_settings", MODE_PRIVATE).edit()
            .putLong("timer_end_elapsed", timerEndElapsed).apply();
        showCapsuleAnimated();
        countdown = new CountDownTimer(seconds * 1000L, 1000L) {
            public void onTick(long left) {
                long total = left / 1000L;
                titleView.setText("◷  Timer  " + String.format(java.util.Locale.getDefault(), "%02d:%02d", total / 60, total % 60));
                detailView.setText("Countdown running");
                mediaControls.setVisibility(View.GONE);
            }
            public void onFinish() {
                timerActive = false;
                timerEndElapsed = 0;
                getSharedPreferences("aura_settings", MODE_PRIVATE).edit().remove("timer_end_elapsed").apply();
                titleView.setText("⏰  Timer finished");
                detailView.setText("Your countdown is complete");
                uiHandler.postDelayed(() -> hideIfIdle(), 5000L);
            }
        }.start();
    }

    private void registerAudioDeviceMonitoring() {
        if(Build.VERSION.SDK_INT>=23) {
            audioManager=(AudioManager)getSystemService(AUDIO_SERVICE);
            audioCallback=new AudioDeviceCallback() {
                @Override public void onAudioDevicesAdded(AudioDeviceInfo[] devices) { updateHeadphoneState(); }
                @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] devices) { updateHeadphoneState(); }
            };
            audioManager.registerAudioDeviceCallback(audioCallback,new Handler(getMainLooper()));
            updateHeadphoneState();
        }
    }
    private void updateHeadphoneState() {
        if(Build.VERSION.SDK_INT<23 || audioManager==null) return;
        boolean connected=false;
        for(AudioDeviceInfo device:audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            int type=device.getType();
            if(type==AudioDeviceInfo.TYPE_WIRED_HEADPHONES || type==AudioDeviceInfo.TYPE_WIRED_HEADSET || type==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || type==AudioDeviceInfo.TYPE_BLUETOOTH_SCO || (Build.VERSION.SDK_INT>=31 && type==AudioDeviceInfo.TYPE_BLE_HEADSET)) { connected=true; break; }
        }
        headsetActive=connected;
        if(connected && getSharedPreferences("aura_settings",MODE_PRIVATE).getBoolean("show_earbuds",true)) {
            showCapsuleAnimated();
            titleView.setText("🎧  Headphones connected");
            detailView.setText("Audio device connected");
        } else hideIfIdle();
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
            for(int i=Math.max(0,array.length()-30);i<array.length();i++) {
                JSONObject item=array.getJSONObject(i);
                notificationQueue.add(new String[]{item.optString("title"),item.optString("body")});
            }
        } catch(Exception ignored) {}
    }

    private void showQueuedNotification() {
        if(notificationQueue.isEmpty()) { showingNotification=false; return; }
        showingNotification=true;
        showCapsuleAnimated();
        String[] item=notificationQueue.get(0);
        titleView.setText(item[0]);
        expanded=false;
        detailView.setMaxLines(1);
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
                hideIfIdle();
            } else showQueuedNotification();
        }
    }
    private void showCapsuleAnimated() {
        if (capsule == null) return;
        boolean wasHidden = capsule.getVisibility() != View.VISIBLE;
        capsule.setVisibility(View.VISIBLE);
        if (wasHidden) {
            capsule.setAlpha(0f);
            capsule.setScaleX(0.88f);
            capsule.setScaleY(0.88f);
            capsule.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(220).start();
        }
    }
    private void showIdleBattery(int pct) {
        if(titleView==null || detailView==null) return;
        titleView.setText("AURA PULSE  •  "+(pct>=0?pct+"%":"Ready"));
        detailView.setText("Always-on island");
        mediaControls.setVisibility(View.GONE);
    }
    private void hideIfIdle() {
        boolean idle=notificationQueue.isEmpty() && !chargingActive && !headsetActive && !callActive && !mediaActive && !timerActive;
        boolean alwaysOn=getSharedPreferences("aura_settings",MODE_PRIVATE).getBoolean("always_on",true);
        if(idle && alwaysOn) {
            Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int pct=-1;
            if(battery!=null) { int level=battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1); int scale=battery.getIntExtra(BatteryManager.EXTRA_SCALE,100); if(level>=0&&scale>0)pct=level*100/scale; }
            showIdleBattery(pct);
            showCapsuleAnimated();
        } else if(idle) capsule.setVisibility(View.GONE);
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
        SharedPreferences appearance=getSharedPreferences("aura_settings",MODE_PRIVATE);
        int accent=appearance.getInt("accent",Color.rgb(101,232,255));
        GradientDrawable shape=new GradientDrawable();
        shape.setColor(Color.rgb(0,0,0)); shape.setCornerRadius(dp(appearance.getInt("corner_radius",30)));
        shape.setStroke(dp(1),accent); capsule.setBackground(shape);
        titleView=new TextView(this); titleView.setText("AURA ISLAND"); titleView.setTextColor(Color.rgb(101,232,255));
        titleView.setTextSize(12); titleView.setTypeface(Typeface.DEFAULT,Typeface.BOLD); titleView.setMaxLines(1);
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
        // Fixed Dynamic Island placement: centered on the physical front-camera cutout.
        params.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;
        params.width=dp(getSharedPreferences("aura_settings",MODE_PRIVATE).getInt("capsule_width",200));
        params.y=getCameraAlignedTopOffset();
        windowManager.addView(capsule,params);
        boolean alwaysOn=getSharedPreferences("aura_settings",MODE_PRIVATE).getBoolean("always_on",true);
        capsule.setVisibility(alwaysOn?View.VISIBLE:View.GONE);
        if(alwaysOn) showIdleBattery(-1);
        capsule.setOnClickListener(v -> {
            if(showingNotification) {
                expanded=!expanded;
                detailView.setMaxLines(expanded?6:1);
                if(!notificationQueue.isEmpty()) detailView.setText(notificationQueue.get(0)[1]+(notificationQueue.size()>1?"  •  +"+(notificationQueue.size()-1):""));
            } else mediaControls.setVisibility(mediaControls.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);
        });
        // Gestures affect content only; the capsule is fixed and cannot be dragged.
        capsule.setOnTouchListener(new View.OnTouchListener() {
            private float downX,downY;
            @Override public boolean onTouch(View v,MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    downX=event.getRawX(); downY=event.getRawY(); return true;
                }
                if(event.getAction()==MotionEvent.ACTION_UP) {
                    float dx=event.getRawX()-downX;
                    float dy=event.getRawY()-downY;
                    if(Math.abs(dx)>dp(85) && Math.abs(dx)>Math.abs(dy) && showingNotification) {
                        dismissCurrentNotification(); return true;
                    }
                    if(dy < -dp(55) && Math.abs(dy)>Math.abs(dx)) {
                        expanded=false;
                        detailView.setMaxLines(1);
                        mediaControls.setVisibility(View.GONE);
                        if(showingNotification && !notificationQueue.isEmpty()) {
                            String[] item=notificationQueue.get(0);
                            detailView.setText(item[1]+(notificationQueue.size()>1?"  •  +"+(notificationQueue.size()-1):""));
                        }
                        return true;
                    }
                    if(Math.abs(dx)<dp(10) && Math.abs(dy)<dp(10)) capsule.performClick();
                    return true;
                }
                return true;
            }
        });
    }
    private int getCameraAlignedTopOffset() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                android.view.WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
                DisplayCutout cutout = metrics.getWindowInsets().getDisplayCutout();
                if (cutout != null && !cutout.getBoundingRects().isEmpty()) {
                    Rect best = null;
                    for (Rect rect : cutout.getBoundingRects()) {
                        if (rect != null && rect.width() > 0 && (best == null || rect.width() > best.width())) best = rect;
                    }
                    if (best != null) return Math.max(0, best.centerY() - dp(21));
                }
            } catch (Exception ignored) {}
        }
        return dp(0);
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
        uiHandler.removeCallbacksAndMessages(null);
        if(updates!=null) try{unregisterReceiver(updates);}catch(Exception ignored){}
        if(capsule!=null && windowManager!=null) try{windowManager.removeView(capsule);}catch(Exception ignored){}
        getSharedPreferences("aura_settings",MODE_PRIVATE).edit().putBoolean("island_service_running",false).apply();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}