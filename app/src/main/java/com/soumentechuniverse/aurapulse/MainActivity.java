package com.soumentechuniverse.aurapulse;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.EditText;
import android.text.InputType;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

public class MainActivity extends Activity {
    private boolean lightTheme=false;
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable bg(int color,int radius) {
        if(lightTheme && (color==Color.BLACK || color==Color.rgb(17,23,36))) color=Color.rgb(239,244,250);
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        d.setStroke(dp(1),Color.rgb(42,57,78)); return d;
    }
    private TextView label(String s,int size,int color,boolean bold) {
        if(lightTheme && color==Color.WHITE) color=Color.rgb(20,29,43);
        else if(lightTheme && color==Color.rgb(151,164,184)) color=Color.rgb(82,96,116);
        else if(lightTheme && color==Color.rgb(145,158,178)) color=Color.rgb(91,105,125);
        else if(lightTheme && color==Color.rgb(133,146,165)) color=Color.rgb(91,105,125);
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private void gap(LinearLayout p,int h) { View v=new View(this); p.addView(v,new LinearLayout.LayoutParams(1,dp(h))); }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        lightTheme=getSharedPreferences("aura_settings",MODE_PRIVATE).getBoolean("light_theme",false);
        buildScreen();
    }
    @Override protected void onResume(){super.onResume(); lightTheme=getSharedPreferences("aura_settings",MODE_PRIVATE).getBoolean("light_theme",false); buildScreen();}
    private void buildScreen() {
        int pageColor=lightTheme?Color.rgb(247,249,252):Color.rgb(8,11,20);
        getWindow().setStatusBarColor(pageColor);
        getWindow().setNavigationBarColor(pageColor);
        ScrollView scroll=new ScrollView(this);
        LinearLayout page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22),dp(24),dp(22),dp(30)); page.setBackgroundColor(pageColor);
        scroll.addView(page);
        page.addView(label("A U R A   P U L S E",13,Color.rgb(101,232,255),true));
        gap(page,16); page.addView(label("Your phone.\nA little more alive.",32,Color.WHITE,true));
        gap(page,8); page.addView(label("A Dynamic Island experience, made for Android.",14,Color.rgb(151,164,184),false));
        gap(page,24);
        LinearLayout preview=new LinearLayout(this); preview.setOrientation(LinearLayout.VERTICAL);
        preview.setGravity(Gravity.CENTER); preview.setPadding(dp(18),dp(18),dp(18),dp(18)); preview.setBackground(bg(Color.BLACK,28));
        preview.addView(label("●  AURA ISLAND",11,Color.rgb(101,232,255),true)); gap(preview,8);
        preview.addView(label("Live activities • Smart capsule",16,Color.WHITE,true));
        gap(page,18); page.addView(preview,new LinearLayout.LayoutParams(-1,dp(112)));
        gap(page,24); page.addView(label("SETUP & CONTROLS",12,Color.rgb(101,232,255),true)); gap(page,10);
        addAction(page,"1","Allow display over other apps","Required for the floating island",v->requestOverlay());
        addAction(page,"2","Enable notification access","Required for notifications and supported music players",v->startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        addAction(page,"3","Enable phone state access","Allows incoming and ongoing call status",v->requestPhonePermission());
        addAction(page,"4","Allow notifications","Shows the persistent AURA PULSE running indicator",v->requestNotificationPermission());
        addAction(page,"▶","Start AURA Island","Show the live capsule over other apps",v->startIsland());
        addAction(page,"■","Stop AURA Island","Hide the floating capsule",v->stopService(new Intent(this,IslandOverlayService.class)));
        addAction(page,"◷","Start a countdown timer","Show a live timer inside AURA Island",v->showTimerDialog());
        gap(page,18); page.addView(label("PERSONALIZE YOUR ISLAND",12,Color.rgb(101,232,255),true)); gap(page,10);
        addCustomization(page);
        gap(page,18); page.addView(label("NOTIFICATION APP FILTER",12,Color.rgb(101,232,255),true)); gap(page,6);
        page.addView(label("Turn off apps you do not want to appear in the island.",12,Color.rgb(145,158,178),false)); gap(page,10);
        addAppFilters(page);
        gap(page,18); page.addView(label("FEATURES",12,Color.rgb(101,232,255),true)); gap(page,10);
        addFeature(page,"♫","Music Island","Track information and playback buttons when supported");
        addFeature(page,"ϟ","Battery & Charging","Live battery percentage and charging state");
        addFeature(page,"◉","Notification Island","Notification previews from other apps");
        addFeature(page,"☎","Call Island","Incoming and ongoing call status (permission required)");
        addFeature(page,"♬","Headphone Detection","Wired headset connection status");
        gap(page,12);
        page.addView(label("Compatibility: Android 6.0 and newer. Some features depend on phone brand, Android version, app media-session support and permissions. Battery restrictions may affect background operation.",12,Color.rgb(133,146,165),false));
        setContentView(scroll);
    }

    private void addCustomization(LinearLayout root) {
        SharedPreferences p=getSharedPreferences("aura_settings",MODE_PRIVATE);
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(14),dp(14),dp(14),dp(14)); row.setBackground(bg(Color.rgb(17,23,36),18));
        row.addView(label("Capsule appearance",15,Color.WHITE,true)); gap(row,8);
        LinearLayout buttons=new LinearLayout(this); buttons.setOrientation(LinearLayout.HORIZONTAL);
        addSmallButton(buttons,"Compact",v->{p.edit().putInt("capsule_width",190).apply(); refreshIsland();});
        addSmallButton(buttons,"Standard",v->{p.edit().putInt("capsule_width",240).apply(); refreshIsland();});
        addSmallButton(buttons,"Wide",v->{p.edit().putInt("capsule_width",290).apply(); refreshIsland();});
        row.addView(buttons); gap(row,8);
        LinearLayout colors=new LinearLayout(this); colors.setOrientation(LinearLayout.HORIZONTAL);
        addSmallButton(colors,"Cyan",v->{p.edit().putInt("accent",Color.rgb(101,232,255)).apply();refreshIsland();});
        addSmallButton(colors,"Purple",v->{p.edit().putInt("accent",Color.rgb(190,130,255)).apply();refreshIsland();});
        addSmallButton(colors,"Green",v->{p.edit().putInt("accent",Color.rgb(100,240,170)).apply();refreshIsland();});
        row.addView(colors); gap(row,8);
        LinearLayout shapes=new LinearLayout(this); shapes.setOrientation(LinearLayout.HORIZONTAL);
        addSmallButton(shapes,"Pill",v->{p.edit().putInt("corner_radius",40).apply();refreshIsland();});
        addSmallButton(shapes,"Rounded",v->{p.edit().putInt("corner_radius",22).apply();refreshIsland();});
        addSmallButton(shapes,"Soft",v->{p.edit().putInt("corner_radius",14).apply();refreshIsland();});
        row.addView(shapes); gap(row,8);
        LinearLayout positions=new LinearLayout(this); positions.setOrientation(LinearLayout.HORIZONTAL);
        addSmallButton(positions,"Top",v->{p.edit().putInt("position_y",10).apply();refreshIsland();});
        addSmallButton(positions,"Lower",v->{p.edit().putInt("position_y",65).apply();refreshIsland();});
        addSmallButton(positions,"Bottom",v->{p.edit().putInt("position_y",120).apply();refreshIsland();});
        row.addView(positions); gap(row,8);
        LinearLayout theme=new LinearLayout(this); theme.setOrientation(LinearLayout.HORIZONTAL);
        addSmallButton(theme,"Dark",v->{p.edit().putBoolean("light_theme",false).apply();lightTheme=false;buildScreen();});
        addSmallButton(theme,"Light",v->{p.edit().putBoolean("light_theme",true).apply();lightTheme=true;buildScreen();});
        row.addView(theme);
        root.addView(row,new LinearLayout.LayoutParams(-1,-2));
    }
    private void addSmallButton(LinearLayout row,String text,View.OnClickListener click) {
        TextView b=label(text,12,Color.WHITE,true); b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10),dp(10),dp(10),dp(10)); b.setBackground(bg(Color.rgb(35,48,68),12));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); lp.setMargins(dp(3),0,dp(3),0);
        row.addView(b,lp); b.setOnClickListener(click);
    }
    private void refreshIsland() {
        if(Build.VERSION.SDK_INT>=23 && Settings.canDrawOverlays(this)) {
            stopService(new Intent(this,IslandOverlayService.class));
            startService(new Intent(this,IslandOverlayService.class));
        }
    }
    private void addAppFilters(LinearLayout root) {
        SharedPreferences prefs=getSharedPreferences("aura_settings",MODE_PRIVATE);
        Set<String> saved=prefs.getStringSet("blocked_apps",new HashSet<String>());
        final HashSet<String> blocked=new HashSet<>(saved==null?new HashSet<String>():saved);
        Intent query=new Intent(Intent.ACTION_MAIN,null); query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps=getPackageManager().queryIntentActivities(query,0);
        apps.sort((a,b)->String.valueOf(a.loadLabel(getPackageManager())).compareToIgnoreCase(String.valueOf(b.loadLabel(getPackageManager()))));
        int shown=0;
        for(ResolveInfo info:apps) {
            String pkg=info.activityInfo.packageName;
            if(pkg.equals(getPackageName())) continue;
            CheckBox cb=new CheckBox(this);
            cb.setText(info.loadLabel(getPackageManager())+"  ("+pkg+")");
            cb.setTextColor(Color.rgb(225,232,244)); cb.setTextSize(12);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.rgb(101,232,255)));
            cb.setChecked(blocked.contains(pkg));
            cb.setOnCheckedChangeListener((button,checked)->{
                if(checked) blocked.add(pkg); else blocked.remove(pkg);
                prefs.edit().putStringSet("blocked_apps",new HashSet<>(blocked)).apply();
            });
            root.addView(cb); shown++;
        }
        if(shown==0) root.addView(label("No launchable apps found.",12,Color.LTGRAY,false));
        root.addView(label("Checked apps are blocked. The list includes launchable apps on your phone.",11,Color.rgb(133,146,165),false));
    }


    private void showTimerDialog() {
        EditText input=new EditText(this);
        input.setHint("Minutes (e.g. 5)");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this).setTitle("AURA Countdown").setMessage("How many minutes?")
            .setView(input).setNegativeButton("Cancel",null).setPositiveButton("Start",(dialog,which)->{
                try {
                    long minutes=Long.parseLong(input.getText().toString().trim());
                    if(minutes<1 || minutes>1440) throw new NumberFormatException();
                    Intent timer=new Intent(this,IslandOverlayService.class);
                    timer.setAction(IslandOverlayService.ACTION_TIMER);
                    timer.putExtra("seconds",minutes*60);
                    if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)) requestOverlay();
                    else { startService(timer); Toast.makeText(this,"Countdown started",Toast.LENGTH_SHORT).show(); }
                } catch(Exception e) { Toast.makeText(this,"Enter 1 to 1440 minutes",Toast.LENGTH_SHORT).show(); }
            }).show();
    }

    private void requestOverlay() {
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)) {
            Intent i=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()));
            startActivity(i);
        } else Toast.makeText(this,"Display access is already enabled",Toast.LENGTH_SHORT).show();
    }
    private void requestPhonePermission() {
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},40);
        else Toast.makeText(this,"Phone state permission is already enabled",Toast.LENGTH_SHORT).show();
    }
    private void requestNotificationPermission() {
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},41);
        else Toast.makeText(this,"Notification permission is already enabled",Toast.LENGTH_SHORT).show();
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==41 && grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) startIsland();
    }
    private void startIsland() {
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission(); return;
        }
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this,"Allow display over other apps first",Toast.LENGTH_LONG).show(); requestOverlay(); return;
        }
        startService(new Intent(this,IslandOverlayService.class));
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