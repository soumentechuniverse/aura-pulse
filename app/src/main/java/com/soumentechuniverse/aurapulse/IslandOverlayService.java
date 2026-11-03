package com.soumentechuniverse.aurapulse;

import android.app.Service;
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
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IslandOverlayService extends Service {
    public static final String ACTION_NOTIFICATION = "com.soumentechuniverse.aurapulse.NOTIFICATION";
    public static final String ACTION_MEDIA = "com.soumentechuniverse.aurapulse.MEDIA";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_TEXT = "text";
    private WindowManager windowManager;
    private LinearLayout capsule, mediaControls;
    private TextView titleView, detailView;
    private BroadcastReceiver updates;

    @Override public void onCreate() {
        super.onCreate();
        windowManager=(WindowManager)getSystemService(WINDOW_SERVICE);
        createCapsule();
        IntentFilter filter=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ACTION_NOTIFICATION);
        filter.addAction(ACTION_MEDIA);
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
                        titleView.setText(title);
                        detailView.setText(body==null?"New notification":body);
                        mediaControls.setVisibility(View.GONE);
                    }
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
        shape.setColor(Color.rgb(0,0,0)); shape.setCornerRadius(dp(30));
        shape.setStroke(dp(1),Color.rgb(38,48,62)); capsule.setBackground(shape);
        titleView=new TextView(this); titleView.setText("AURA ISLAND"); titleView.setTextColor(Color.rgb(101,232,255));
        titleView.setTextSize(12); titleView.setTypeface(Typeface.DEFAULT,Typeface.BOLD); titleView.setMaxLines(1);
        detailView=new TextView(this); detailView.setText("Live activities appear here"); detailView.setTextColor(Color.WHITE);
        detailView.setTextSize(11); detailView.setMaxLines(1);
        capsule.addView(titleView); capsule.addView(detailView);
        mediaControls=new LinearLayout(this);
        mediaControls.setGravity(Gravity.CENTER); mediaControls.setOrientation(LinearLayout.HORIZONTAL);
        mediaControls.addView(controlButton("⏮", () -> AuraNotificationListener.skipPrevious()));
        mediaControls.addView(controlButton("▶ / ❚❚", () -> AuraNotificationListener.togglePlayback()));
        mediaControls.addView(controlButton("⏭", () -> AuraNotificationListener.skipNext()));
        mediaControls.setVisibility(View.GONE);
        capsule.addView(mediaControls);
        int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;
        params.y=dp(10);
        windowManager.addView(capsule,params);
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){return START_STICKY;}
    @Override public void onDestroy(){
        if(updates!=null) try{unregisterReceiver(updates);}catch(Exception ignored){}
        if(capsule!=null && windowManager!=null) try{windowManager.removeView(capsule);}catch(Exception ignored){}
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}