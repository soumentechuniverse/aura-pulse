package com.soumentechuniverse.aurapulse;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable bg(int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        d.setStroke(dp(1),Color.rgb(42,57,78)); return d;
    }
    private TextView label(String s,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private void gap(LinearLayout p,int h) { View v=new View(this); p.addView(v,new LinearLayout.LayoutParams(1,dp(h))); }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(8,11,20));
        getWindow().setNavigationBarColor(Color.rgb(8,11,20));
        ScrollView scroll=new ScrollView(this);
        LinearLayout page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22),dp(24),dp(22),dp(30)); page.setBackgroundColor(Color.rgb(8,11,20));
        scroll.addView(page);
        TextView brand=label("A U R A   P U L S E",13,Color.rgb(101,232,255),true); page.addView(brand);
        gap(page,16); page.addView(label("Your phone.\nA little more alive.",32,Color.WHITE,true));
        gap(page,8); page.addView(label("A live island that works with Android permissions.",14,Color.rgb(151,164,184),false));
        gap(page,24);
        LinearLayout preview=new LinearLayout(this); preview.setOrientation(LinearLayout.VERTICAL);
        preview.setGravity(Gravity.CENTER); preview.setPadding(dp(18),dp(18),dp(18),dp(18)); preview.setBackground(bg(Color.BLACK,28));
        preview.addView(label("●  AURA ISLAND",11,Color.rgb(101,232,255),true)); gap(preview,8);
        preview.addView(label("Live capsule preview",18,Color.WHITE,true));
        gap(page,18); page.addView(preview,new LinearLayout.LayoutParams(-1,dp(112)));
        gap(page,26); page.addView(label("GET STARTED",12,Color.rgb(101,232,255),true)); gap(page,10);
        addAction(page,"1","Allow display over other apps","Needed to show the island above other apps",v->requestOverlay());
        addAction(page,"2","Enable notification access","Lets the island show selected notification previews",v->startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        addAction(page,"3","Start AURA Island","Starts the live capsule when overlay access is allowed",v->startIsland());
        gap(page,18); page.addView(label("FEATURES",12,Color.rgb(101,232,255),true)); gap(page,10);
        addFeature(page,"♫","Music capsule","Notification-based media title preview");
        addFeature(page,"ϟ","Charging pulse","Live battery percentage and charging state");
        addFeature(page,"◉","Notification preview","Recent notification title and app name");
        gap(page,14);
        page.addView(label("Android may show different permission screens on different brands. You can turn access off anytime in system Settings.",12,Color.rgb(133,146,165),false));
        setContentView(scroll);
    }
    private void requestOverlay() {
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)) {
            Intent i=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()));
            startActivity(i);
        } else Toast.makeText(this,"Display access is already enabled",Toast.LENGTH_SHORT).show();
    }
    private void startIsland() {
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this,"Please allow display over other apps first",Toast.LENGTH_LONG).show(); requestOverlay(); return;
        }
        Intent i=new Intent(this,IslandOverlayService.class);
        if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
        Toast.makeText(this,"AURA Island started",Toast.LENGTH_SHORT).show();
    }
    private void addAction(LinearLayout root,String n,String title,String sub,View.OnClickListener click) {
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16),dp(14),dp(16),dp(14)); row.setBackground(bg(Color.rgb(17,23,36),18));
        row.addView(label(n+"  "+title,16,Color.WHITE,true)); gap(row,5);
        row.addView(label(sub,12,Color.rgb(145,158,178),false));
        root.addView(row,new LinearLayout.LayoutParams(-1,-2)); gap(root,10); row.setOnClickListener(click);
    }
    private void addFeature(LinearLayout root,String icon,String title,String sub) {
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14),dp(12),dp(14),dp(12)); row.setBackground(bg(Color.rgb(17,23,36),18));
        TextView badge=label(icon,22,Color.rgb(101,232,255),true); badge.setGravity(Gravity.CENTER);
        row.addView(badge,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout text=new LinearLayout(this); text.setOrientation(LinearLayout.VERTICAL); text.setPadding(dp(12),0,0,0);
        text.addView(label(title,15,Color.WHITE,true)); text.addView(label(sub,11,Color.rgb(145,158,178),false));
        row.addView(text,new LinearLayout.LayoutParams(0,-2,1)); root.addView(row); gap(root,9);
    }
}