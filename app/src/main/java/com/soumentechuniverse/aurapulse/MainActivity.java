package com.soumentechuniverse.aurapulse;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), Color.rgb(39, 53, 72));
        return d;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private void space(LinearLayout root, int height) {
        View v = new View(this);
        root.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(8, 11, 20));
        getWindow().setNavigationBarColor(Color.rgb(8, 11, 20));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(22), dp(22), dp(24));
        page.setBackgroundColor(Color.rgb(8, 11, 20));
        TextView brand = text("AURA  PULSE", 13, Color.rgb(101, 232, 255), true);
        brand.setLetterSpacing(.18f);
        page.addView(brand);
        space(page, 8);
        page.addView(text("Your phone.\nA little more alive.", 30, Color.WHITE, true));
        space(page, 8);
        page.addView(text("A smart island for the moments that matter.", 14, Color.rgb(151, 164, 184), false));
        space(page, 26);
        LinearLayout island = new LinearLayout(this);
        island.setOrientation(LinearLayout.VERTICAL);
        island.setGravity(Gravity.CENTER);
        island.setPadding(dp(20), dp(18), dp(20), dp(18));
        island.setBackground(shape(Color.BLACK, 28));
        TextView live = text("●  AURA ISLAND", 11, Color.rgb(101, 232, 255), true);
        live.setLetterSpacing(.12f);
        island.addView(live);
        space(island, 10);
        island.addView(text("Nothing playing", 18, Color.WHITE, true));
        space(island, 4);
        island.addView(text("Your live activities appear here", 12, Color.rgb(133, 146, 165), false));
        page.addView(island, new LinearLayout.LayoutParams(-1, dp(132)));
        space(page, 28);
        page.addView(text("QUICK FEATURES", 12, Color.rgb(133, 146, 165), true));
        space(page, 12);
        addFeature(page, "♫", "Music capsule", "Media controls and playback status");
        addFeature(page, "ϟ", "Charging pulse", "Battery and charging status");
        addFeature(page, "◉", "Smart notifications", "A compact view of important alerts");
        addFeature(page, "◷", "Timer & activities", "Keep useful live activities close");
        space(page, 16);
        page.addView(text("More features will unlock as permissions and Android support are added.", 12, Color.rgb(133, 146, 165), false));
        setContentView(page);
    }
    private void addFeature(LinearLayout root, String icon, String title, String sub) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(shape(Color.rgb(17, 23, 36), 18));
        TextView badge = text(icon, 22, Color.rgb(101, 232, 255), true);
        badge.setGravity(Gravity.CENTER);
        row.addView(badge, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(12), 0, 0, 0);
        labels.addView(text(title, 15, Color.WHITE, true));
        labels.addView(text(sub, 11, Color.rgb(145, 158, 178), false));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(row, new LinearLayout.LayoutParams(-1, dp(70)));
        space(root, 10);
        row.setOnClickListener(v -> Toast.makeText(this, title + " — coming soon", Toast.LENGTH_SHORT).show());
    }
}
