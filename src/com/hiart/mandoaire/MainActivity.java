package com.hiart.mandoaire;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int BG = 0xFF101418, CARD = 0xFF1C232B, TEXT = 0xFFECEFF1,
            MUTED = 0xFF90A4AE, ACCENT = 0xFF29B6F6, ON = 0xFF2E7D32, OFF = 0xFFC62828;

    private static final String[] MODE_NAMES = {"Frío", "Seco", "Auto", "Calor", "Ventilador"};
    private static final String[] PROTO_NAMES = {"Protocolo A (Coolix)", "Protocolo B (Midea)"};

    private ConsumerIrManager ir;
    private SharedPreferences prefs;
    private int protocol, mode, temp;

    private TextView tempView, status;
    private final Button[] modeButtons = new Button[MODE_NAMES.length];
    private final Button[] protoButtons = new Button[PROTO_NAMES.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ir = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        prefs = getPreferences(MODE_PRIVATE);
        protocol = prefs.getInt("protocol", 0);
        mode = prefs.getInt("mode", IrCodes.MODE_COOL);
        temp = prefs.getInt("temp", 24);
        buildUi();
        refresh();
        if (ir == null || !ir.hasIrEmitter()) {
            status.setText("⚠ Este teléfono no tiene emisor de infrarrojos. La app no podrá controlar el aire.");
            status.setTextColor(0xFFFFB74D);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));

        TextView title = text("Mando Aire", 26, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);
        root.addView(text("Mundo Clima y equipos de base Midea", 14, MUTED));

        Button power = bigButton("ENCENDER", ON);
        power.setOnClickListener(v -> sendPower(true));
        Button off = bigButton("APAGAR", OFF);
        off.setOnClickListener(v -> sendPower(false));
        LinearLayout powerRow = row();
        powerRow.addView(power, weighted());
        powerRow.addView(off, weighted());
        root.addView(powerRow, margins(0, 20, 0, 8));

        // Temperatura
        root.addView(label("Temperatura"));
        LinearLayout tempRow = row();
        tempRow.setGravity(Gravity.CENTER_VERTICAL);
        Button minus = chip("−");
        minus.setTextSize(28);
        minus.setOnClickListener(v -> changeTemp(-1));
        Button plus = chip("+");
        plus.setTextSize(28);
        plus.setOnClickListener(v -> changeTemp(1));
        tempView = text("", 44, TEXT);
        tempView.setGravity(Gravity.CENTER);
        tempView.setTypeface(Typeface.DEFAULT_BOLD);
        tempRow.addView(minus, new LinearLayout.LayoutParams(dp(72), dp(64)));
        tempRow.addView(tempView, weighted());
        tempRow.addView(plus, new LinearLayout.LayoutParams(dp(72), dp(64)));
        root.addView(tempRow);

        // Modo
        root.addView(label("Modo"));
        LinearLayout modeRow1 = row(), modeRow2 = row();
        for (int i = 0; i < MODE_NAMES.length; i++) {
            final int m = i;
            modeButtons[i] = chip(MODE_NAMES[i]);
            modeButtons[i].setOnClickListener(v -> { mode = m; save(); refresh(); sendPower(true); });
            (i < 3 ? modeRow1 : modeRow2).addView(modeButtons[i], weighted());
        }
        root.addView(modeRow1);
        root.addView(modeRow2);

        // Protocolo
        root.addView(label("Protocolo (si el aire no responde, prueba el otro)"));
        for (int i = 0; i < PROTO_NAMES.length; i++) {
            final int p = i;
            protoButtons[i] = chip(PROTO_NAMES[i]);
            protoButtons[i].setOnClickListener(v -> { protocol = p; save(); refresh(); });
            root.addView(protoButtons[i], margins(0, 0, 0, 4));
        }

        status = text("Apunta el teléfono hacia el aire acondicionado.", 14, MUTED);
        root.addView(status, margins(0, 20, 0, 0));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);
        if (android.os.Build.VERSION.SDK_INT >= 21) getWindow().setStatusBarColor(BG);
    }

    private void changeTemp(int delta) {
        int t = Math.max(IrCodes.TEMP_MIN, Math.min(IrCodes.TEMP_MAX, temp + delta));
        if (t == temp) return;
        temp = t;
        save();
        refresh();
        sendPower(true);
    }

    private void sendPower(boolean on) {
        if (ir == null || !ir.hasIrEmitter()) {
            status.setText("⚠ Sin emisor de infrarrojos: no se puede enviar.");
            return;
        }
        int[] pattern;
        String code;
        if (protocol == 0) {
            int state = on ? IrCodes.coolixState(mode, temp) : IrCodes.COOLIX_OFF;
            pattern = IrCodes.coolixPattern(state);
            code = String.format("%06X", state);
        } else {
            long state = IrCodes.mideaState(on, mode, temp);
            pattern = IrCodes.mideaPattern(state);
            code = String.format("%012X", state);
        }
        try {
            ir.transmit(IrCodes.CARRIER_HZ, pattern);
            status.setText((on ? "Enviado: encender, " + MODE_NAMES[mode] + " " + temp + "°C"
                    : "Enviado: apagar") + "\nCódigo " + code);
            status.setTextColor(MUTED);
        } catch (Exception e) {
            status.setText("Error al enviar: " + e.getMessage());
        }
    }

    private void refresh() {
        tempView.setText(mode == IrCodes.MODE_FAN ? "—" : temp + "°C");
        for (int i = 0; i < modeButtons.length; i++) select(modeButtons[i], i == mode);
        for (int i = 0; i < protoButtons.length; i++) select(protoButtons[i], i == protocol);
    }

    private void save() {
        prefs.edit().putInt("protocol", protocol).putInt("mode", mode).putInt("temp", temp).apply();
    }

    // ---------------- Helpers de UI ----------------

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    private TextView label(String s) {
        TextView t = text(s, 13, MUTED);
        t.setPadding(0, dp(18), 0, dp(6));
        return t;
    }

    private Button bigButton(String s, int color) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(18);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(Color.WHITE);
        b.setBackground(rounded(color, 0));
        b.setMinHeight(dp(72));
        return b;
    }

    private Button chip(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(TEXT);
        return b;
    }

    private void select(Button b, boolean selected) {
        b.setBackground(rounded(CARD, selected ? ACCENT : 0));
        b.setTextColor(selected ? ACCENT : TEXT);
    }

    private GradientDrawable rounded(int fill, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(14));
        if (stroke != 0) d.setStroke(dp(2), stroke);
        return d;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        return lp;
    }

    private LinearLayout.LayoutParams margins(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(l), dp(t), dp(r), dp(b));
        return lp;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
