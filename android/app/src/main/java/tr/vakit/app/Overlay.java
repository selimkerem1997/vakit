package tr.vakit.app;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.Locale;

/** Hangi uygulama acik olursa olsun ekranin ortasina cikan soru kutusu. */
final class Overlay {
    private Overlay() {}

    private static final ArrayDeque<JSONObject> queue = new ArrayDeque<>();
    private static View current;

    static void show(Context ctx, JSONObject ev) {
        Context c = ctx.getApplicationContext();
        if (current != null) { queue.add(ev); return; }
        build(c, ev);
    }

    private static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    private static void build(final Context c, final JSONObject ev) {
        final WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        boolean dark = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        final int surface = dark ? 0xFF151D1A : 0xFFFFFFFF;
        final int ink = dark ? 0xFFE6EDE8 : 0xFF15201B;
        final int muted = dark ? 0xFF93A199 : 0xFF5A675F;
        final int soft = dark ? 0xFF1B2521 : 0xFFF1F4EF;
        String type = ev.optString("type");
        final int accent = "w".equals(type) ? (dark ? 0xFF66B4E0 : 0xFF2870A0)
                : "s".equals(type) ? (dark ? 0xFFEC8962 : 0xFFB44E26)
                : (dark ? 0xFFD8B569 : 0xFF8E6D22);
        final int onAccent = dark ? 0xFF0D1311 : 0xFFFFFFFF;

        final FrameLayout root = new FrameLayout(c);
        root.setBackgroundColor(0x99000000);

        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(c, 20), dp(c, 20), dp(c, 20), dp(c, 16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(surface);
        bg.setCornerRadius(dp(c, 28));
        card.setBackground(bg);
        card.setElevation(dp(c, 16));
        int w = Math.min(c.getResources().getDisplayMetrics().widthPixels - dp(c, 40), dp(c, 380));
        root.addView(card, new FrameLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        LinearLayout head = new LinearLayout(c);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        final ImageView face = new ImageView(c);
        face.setImageResource(R.drawable.face_neutral);
        head.addView(face, new LinearLayout.LayoutParams(dp(c, 68), dp(c, 68)));
        TextView title = new TextView(c);
        title.setText(ev.optString("title").toUpperCase(new Locale("tr", "TR")));
        title.setTextColor(accent);
        title.setTextSize(13);
        title.setLetterSpacing(0.08f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tlp.leftMargin = dp(c, 14);
        head.addView(title, tlp);
        card.addView(head);

        final TextView q = new TextView(c);
        q.setText(ev.optString("q"));
        q.setTextColor(ink);
        q.setTextSize(19);
        q.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qlp.topMargin = dp(c, 14);
        card.addView(q, qlp);

        final LinearLayout btns = new LinearLayout(c);
        btns.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(c, 18);
        card.addView(btns, blp);

        LinearLayout yesRow = row(c), noRow = row(c);
        JSONArray opts = ev.optJSONArray("opts");
        if (opts != null) for (int i = 0; i < opts.length(); i++) {
            JSONArray o = opts.optJSONArray(i);
            if (o == null) continue;
            final int val = o.optInt(1);
            TextView b = val > 0 ? button(c, o.optString(0), accent, onAccent, true) : button(c, o.optString(0), soft, ink, true);
            b.setOnClickListener(v -> answer(c, wm, root, ev, val, face, q, btns));
            (val > 0 ? yesRow : noRow).addView(b, cell(c));
        }
        TextView later = button(c, "Sonra sor", 0x00000000, muted, false);
        later.setOnClickListener(v -> { if (!ev.optBoolean("test")) Scheduler.snooze(c, ev); close(c, wm, root); });
        noRow.addView(later, cell(c));
        btns.addView(yesRow);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = dp(c, 8);
        btns.addView(noRow, nlp);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        try {
            wm.addView(root, lp);
            current = root;
        } catch (Exception e) {   // izin geri alinmis olabilir
            Notifier.ask(c, ev);
            return;
        }
        card.setScaleX(.92f); card.setScaleY(.92f); card.setAlpha(0f);
        card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(220).start();
        buzz(c);
    }

    private static void answer(Context c, WindowManager wm, View root, JSONObject ev, int val,
                               ImageView face, TextView q, View btns) {
        if (!ev.optBoolean("test")) {
            Store.addAnswer(c, ev, val);
            MainActivity.refresh();
        }
        face.setImageResource(val > 0 ? R.drawable.face_happy : R.drawable.face_pity);
        q.setText(ev.optString(val > 0 ? "yes" : "no"));
        btns.setVisibility(View.GONE);
        if (val == 0) ObjectAnimator.ofFloat(face, "rotation", 0, -9, 7, -5, 3, 0).setDuration(1100).start();
        root.postDelayed(() -> close(c, wm, root), val > 0 ? 2600 : 3800);
    }

    private static void close(Context c, WindowManager wm, View root) {
        try { wm.removeView(root); } catch (Exception ignored) {}
        current = null;
        JSONObject nx;
        while ((nx = queue.poll()) != null) {
            if (nx.optBoolean("test") || !Store.answered(c, nx)) { build(c, nx); return; }
        }
    }

    private static LinearLayout row(Context c) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        return r;
    }

    private static LinearLayout.LayoutParams cell(Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(c, 50), 1);
        lp.leftMargin = dp(c, 3);
        lp.rightMargin = dp(c, 3);
        return lp;
    }

    private static TextView button(Context c, String label, int fill, int text, boolean bold) {
        TextView b = new TextView(c);
        b.setText(label);
        b.setTextColor(text);
        b.setTextSize(15);
        b.setGravity(Gravity.CENTER);
        b.setMaxLines(1);
        if (bold) b.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(c, 25));
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(0xFFFFFFFF);
        mask.setCornerRadius(dp(c, 25));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000), shape, mask));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private static void buzz(Context c) {
        try {
            Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Exception ignored) {}
    }
}
