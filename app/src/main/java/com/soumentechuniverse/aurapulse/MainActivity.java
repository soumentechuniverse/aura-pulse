package com.soumentechuniverse.aurapulse;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.content.SharedPreferences;
import android.text.InputType;
import java.util.*;

public class MainActivity extends Activity {
    private boolean lightTheme=false;
    private String currentTab="Home";
    private SharedPreferences prefs;
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private int pageColor(){return lightTheme?Color.rgb(246,249,253):Color.rgb(9,13,20);}
    private int panelColor(){return lightTheme?Color.WHITE:Color.rgb(24,32,43);}
    private int textColor(){return lightTheme?Color.rgb(22,31,45):Color.WHITE;}
    private int mutedColor(){return lightTheme?Color.rgb(91,105,122):Color.rgb(166,179,196);}
    private GradientDrawable shape(int color,int radius,int stroke){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(1),stroke);return d;
    }
    private TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private void gap(LinearLayout p,int h){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("aura_settings",MODE_PRIVATE);lightTheme=prefs.getBoolean("light_theme",false);showTab("Home");}
    @Override protected void onResume(){super.onResume();if(prefs!=null){lightTheme=prefs.getBoolean("light_theme",false);showTab(currentTab);}}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout panel(LinearLayout root){
        LinearLayout p=vertical();p.setPadding(dp(16),dp(16),dp(16),dp(16));p.setBackground(shape(panelColor(),22,lightTheme?Color.rgb(225,231,239):Color.rgb(35,48,62)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(14);root.addView(p,lp);return p;
    }
    private void heading(LinearLayout root,String s){TextView t=text(s,13,Color.rgb(91,211,245),true);root.addView(t);gap(root,10);}
    private void button(LinearLayout root,String s,Runnable action){
        TextView b=text(s,13,textColor(),true);b.setGravity(Gravity.CENTER);b.setPadding(dp(12),dp(12),dp(12),dp(12));
        b.setBackground(shape(lightTheme?Color.rgb(231,239,248):Color.rgb(48,62,80),14,0));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(3),0,dp(3),0);root.addView(b,lp);b.setOnClickListener(v->action.run());
    }
    private void action(LinearLayout root,String icon,String title,String sub,Runnable run){
        LinearLayout row=vertical();row.setPadding(dp(14),dp(13),dp(14),dp(13));row.setBackground(shape(lightTheme?Color.rgb(255,255,255):Color.rgb(17,24,34),16,0));
        row.addView(text(icon+"   "+title,15,textColor(),true));gap(row,4);row.addView(text(sub,12,mutedColor(),false));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);root.addView(row,lp);row.setOnClickListener(v->run.run());
    }
    private void showTab(String tab){
        currentTab=tab;lightTheme=prefs.getBoolean("light_theme",false);
        getWindow().setStatusBarColor(pageColor());getWindow().setNavigationBarColor(pageColor());
        LinearLayout base=vertical();base.setBackgroundColor(pageColor());
        ScrollView scroll=new ScrollView(this);LinearLayout content=vertical();content.setPadding(dp(20),dp(22),dp(20),dp(18));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        base.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if("Home".equals(tab))home(content);else if("Activities".equals(tab))activities(content);else if("Customize".equals(tab))customize(content);else if("Apps".equals(tab))apps(content);else about(content);
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(5),dp(8),dp(5),dp(8));nav.setBackgroundColor(lightTheme?Color.WHITE:Color.rgb(17,24,32));
        String[] tabs={"Home","Activities","Customize","Apps","About"};String[] icons={"⌂","◉","☷","▦","ⓘ"};
        for(int i=0;i<tabs.length;i++){final String name=tabs[i];LinearLayout item=vertical();item.setGravity(Gravity.CENTER);TextView ic=text(icons[i],21,name.equals(tab)?Color.rgb(117,199,255):mutedColor(),true);ic.setGravity(Gravity.CENTER);TextView tx=text(name,11,name.equals(tab)?textColor():mutedColor(),true);tx.setGravity(Gravity.CENTER);item.addView(ic);item.addView(tx);if(name.equals(tab))item.setBackground(shape(lightTheme?Color.rgb(225,239,253):Color.rgb(48,64,83),18,0));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(58),1);nav.addView(item,lp);item.setOnClickListener(v->showTab(name));}
        base.addView(nav,new LinearLayout.LayoutParams(-1,dp(66)));setContentView(base);
    }
    private void home(LinearLayout root){
        root.addView(text("A U R A   P U L S E",13,Color.rgb(91,211,245),true));gap(root,13);
        root.addView(text("Your phone.\nA little more alive.",31,textColor(),true));gap(root,7);
        root.addView(text("A complete Dynamic Island experience for Android.",14,mutedColor(),false));gap(root,20);
        LinearLayout p=panel(root);p.setGravity(Gravity.CENTER);p.setBackground(shape(Color.BLACK,34,Color.rgb(61,76,94)));
        TextView capsule=text("●     AURA PULSE       ▮▮",14,Color.WHITE,true);capsule.setGravity(Gravity.CENTER);capsule.setPadding(dp(24),dp(13),dp(24),dp(13));capsule.setBackground(shape(Color.rgb(34,39,49),40,Color.rgb(88,103,125)));p.addView(capsule);gap(p,12);
        p.addView(text("Live capsule preview",14,Color.WHITE,true));p.addView(text("Camera-centered • Music • Notifications",11,Color.LTGRAY,false));
        heading(root,"ISLAND CONTROL");
        action(root,"◉","Island is on / off","Show or hide the floating capsule",()->{if(Settings.canDrawOverlays(this))stopService(new Intent(this,IslandOverlayService.class));else requestOverlay();});
        action(root,"▶","Start AURA Island","Display the capsule over your apps",this::startIsland);
        action(root,"◷","Start countdown timer","Choose a timer from 1 to 1440 minutes",this::showTimerDialog);
        heading(root,"QUICK SWITCHES");switchRow(root,"Notifications","show_notifications",true);switchRow(root,"Music & media","show_media",true);switchRow(root,"Calls","show_calls",true);switchRow(root,"Charging","show_charging",true);switchRow(root,"Earbuds","show_earbuds",true);switchRow(root,"Timer","show_timer",true);
        heading(root,"PREVIEW STYLE");LinearLayout row=new LinearLayout(this);button(row,"Pill",()->saveString("preview_style","pill"));button(row,"Banner",()->saveString("preview_style","banner"));button(row,"Card",()->saveString("preview_style","card"));root.addView(row);
    }
    private void activities(LinearLayout root){
        root.addView(text("Activities",29,textColor(),true));gap(root,6);root.addView(text("Choose an activity to preview or control.",14,mutedColor(),false));gap(root,18);
        action(root,"▣","Messages","Notification previews and app alerts",()->showTab("Apps"));
        action(root,"♫","Music & Media","Track title, artist, artwork and playback controls",()->Toast.makeText(this,"Music appears when a supported player is playing.",Toast.LENGTH_LONG).show());
        action(root,"◷","Timer","Start a live countdown",this::showTimerDialog);
        action(root,"⚡","Charging","Battery level and charging status",()->Toast.makeText(this,"Charging activity appears when the phone is connected to power.",Toast.LENGTH_SHORT).show());
        action(root,"☎","Calls","Incoming and ongoing call status",()->openAppDetails());
        action(root,"♬","Earbuds","Wired and Bluetooth audio connection",()->Toast.makeText(this,"Connect an audio device to preview this activity.",Toast.LENGTH_SHORT).show());
    }
    private void customize(LinearLayout root){
        root.addView(text("Customize",29,textColor(),true));gap(root,6);root.addView(text("Make the island match your phone.",14,mutedColor(),false));gap(root,18);
        LinearLayout p=panel(root);p.addView(text("Capsule width",16,textColor(),true));LinearLayout r=new LinearLayout(this);button(r,"Compact",()->setInt("capsule_width",190));button(r,"Standard",()->setInt("capsule_width",240));button(r,"Wide",()->setInt("capsule_width",290));p.addView(r);gap(p,10);
        p.addView(text("Accent colour",16,textColor(),true));LinearLayout c=new LinearLayout(this);button(c,"Cyan",()->setInt("accent",Color.rgb(101,232,255)));button(c,"Purple",()->setInt("accent",Color.rgb(190,130,255)));button(c,"Green",()->setInt("accent",Color.rgb(100,240,170)));p.addView(c);gap(p,10);
        LinearLayout c2=new LinearLayout(this);button(c2,"Gold",()->setInt("accent",Color.rgb(255,204,102)));button(c2,"Pink",()->setInt("accent",Color.rgb(255,130,180)));button(c2,"Blue",()->setInt("accent",Color.rgb(100,165,255)));p.addView(c2);gap(p,10);
        p.addView(text("Capsule shape",16,textColor(),true));LinearLayout s=new LinearLayout(this);button(s,"Pill",()->setInt("corner_radius",40));button(s,"Rounded",()->setInt("corner_radius",22));button(s,"Soft",()->setInt("corner_radius",14));p.addView(s);gap(p,10);
        p.addView(text("Position",16,textColor(),true));LinearLayout pos=new LinearLayout(this);button(pos,"Top",()->setInt("position_mode",0));button(pos,"Center",()->setInt("position_mode",1));button(pos,"Bottom",()->setInt("position_mode",2));p.addView(pos);gap(p,10);
        p.addView(text("Theme",16,textColor(),true));LinearLayout th=new LinearLayout(this);button(th,"Dark",()->{prefs.edit().putBoolean("light_theme",false).apply();restartUi();});button(th,"Light",()->{prefs.edit().putBoolean("light_theme",true).apply();restartUi();});p.addView(th);
        action(root,"↻","Refresh capsule","Apply your appearance settings",this::refreshIsland);
    }
    private void apps(LinearLayout root){
        root.addView(text("Apps & Notifications",27,textColor(),true));gap(root,6);root.addView(text("Choose which apps may show notifications in AURA PULSE. Checked apps are blocked.",13,mutedColor(),false));gap(root,15);
        LinearLayout p=panel(root);Intent query=new Intent(Intent.ACTION_MAIN,null);query.addCategory(Intent.CATEGORY_LAUNCHER);List<ResolveInfo> list=getPackageManager().queryIntentActivities(query,0);
        list.sort((a,b)->String.valueOf(a.loadLabel(getPackageManager())).compareToIgnoreCase(String.valueOf(b.loadLabel(getPackageManager()))));
        Set<String> old=prefs.getStringSet("blocked_apps",new HashSet<String>());final HashSet<String> blocked=new HashSet<>(old==null?new HashSet<String>():old);
        for(ResolveInfo info:list){String pkg=info.activityInfo.packageName;if(pkg.equals(getPackageName()))continue;CheckBox cb=new CheckBox(this);cb.setText(info.loadLabel(getPackageManager()));cb.setTextColor(textColor());cb.setTextSize(14);cb.setChecked(blocked.contains(pkg));cb.setOnCheckedChangeListener((v,checked)->{if(checked)blocked.add(pkg);else blocked.remove(pkg);prefs.edit().putStringSet("blocked_apps",new HashSet<>(blocked)).apply();});p.addView(cb);}
        action(root,"⚙","Notification access","Open Android notification access settings",()->startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
    }
    private void about(LinearLayout root){
        root.addView(text("About AURA PULSE",27,textColor(),true));gap(root,6);root.addView(text("Your personal Dynamic Island for Android.",14,mutedColor(),false));gap(root,18);
        LinearLayout p=panel(root);p.setGravity(Gravity.CENTER);TextView icon=text("◉",36,Color.WHITE,true);icon.setGravity(Gravity.CENTER);icon.setBackground(shape(Color.rgb(139,55,230),22,0));p.addView(icon,new LinearLayout.LayoutParams(dp(76),dp(76)));gap(p,12);p.addView(text("AURA PULSE",21,textColor(),true));p.addView(text("Version 1.3",13,mutedColor(),false));gap(p,8);p.addView(text("Developed by Soumen Mondal",14,Color.rgb(91,211,245),true));gap(p,5);p.addView(text("Created for Android • Offline-first",12,mutedColor(),false));
        action(root,"ⓘ","App details","Permissions, battery, storage and Android app settings",this::openAppDetails);
        action(root,"🔋","Battery settings","Open Android battery settings",()->openSettings("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"));
        action(root,"▤","Data usage","Open Android data usage settings",()->openSettings("android.settings.DATA_USAGE_SETTINGS"));
        action(root,"◷","Screen time","Open Android usage access settings",()->openSettings("android.settings.USAGE_ACCESS_SETTINGS"));
        action(root,"文","App language","Open Android app language settings",()->openSettings("android.settings.APP_LOCALE_SETTINGS"));
        action(root,"🔔","Notification settings","Manage AURA PULSE notifications",()->openSettings("android.settings.APP_NOTIFICATION_SETTINGS"));
        action(root,"🔒","Privacy","AURA PULSE is designed to work locally without an internet connection.",()->new AlertDialog.Builder(this).setTitle("Privacy").setMessage("Notification and display features run on your device. Android notification access is required to show selected notifications.").setPositiveButton("OK",null).show());
    }
    private void switchRow(LinearLayout root,String title,String key,boolean def){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(9),dp(8),dp(9));row.setBackground(shape(panelColor(),15,0));TextView t=text(title,15,textColor(),false);row.addView(t,new LinearLayout.LayoutParams(0,-2,1));Switch sw=new Switch(this);sw.setChecked(prefs.getBoolean(key,def));sw.setOnCheckedChangeListener((b,on)->prefs.edit().putBoolean(key,on).apply());row.addView(sw);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(7);root.addView(row,lp);
    }
    private void setInt(String key,int value){prefs.edit().putInt(key,value).apply();refreshIsland();Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show();}
    private void saveString(String key,String value){prefs.edit().putString(key,value).apply();Toast.makeText(this,"Preview style saved",Toast.LENGTH_SHORT).show();}
    private void restartUi(){showTab("Customize");}
    private void refreshIsland(){if(Build.VERSION.SDK_INT>=23&&Settings.canDrawOverlays(this)){stopService(new Intent(this,IslandOverlayService.class));startService(new Intent(this,IslandOverlayService.class));}}
    private void showTimerDialog(){EditText input=new EditText(this);input.setHint("Minutes (e.g. 5)");input.setInputType(InputType.TYPE_CLASS_NUMBER);new AlertDialog.Builder(this).setTitle("AURA Countdown").setMessage("Choose 1 to 1440 minutes").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Start",(d,w)->{try{long m=Long.parseLong(input.getText().toString().trim());if(m<1||m>1440)throw new Exception();Intent i=new Intent(this,IslandOverlayService.class);i.setAction(IslandOverlayService.ACTION_TIMER);i.putExtra("seconds",m*60);if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this))requestOverlay();else startService(i);}catch(Exception e){Toast.makeText(this,"Enter 1 to 1440 minutes",Toast.LENGTH_SHORT).show();}}).show();}
    private void requestOverlay(){if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this)){Intent i=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()));startActivity(i);}else Toast.makeText(this,"Display access is already enabled",Toast.LENGTH_SHORT).show();}
    private void startIsland(){if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this)){requestOverlay();return;}startService(new Intent(this,IslandOverlayService.class));Toast.makeText(this,"AURA Island started",Toast.LENGTH_SHORT).show();}
    private void openAppDetails(){Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()));startActivity(i);}
    private void openSettings(String action){try{startActivity(new Intent(action));}catch(Exception e){openAppDetails();}}
}