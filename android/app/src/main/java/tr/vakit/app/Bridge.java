package tr.vakit.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

/** Web arayuzunun cagirabildigi telefon islevleri (JS'de window.VakitApp). */
final class Bridge {
    private final MainActivity a;

    Bridge(MainActivity a) { this.a = a; }

    private String pkg() { return a.getPackageName(); }

    private void go(Intent i) {
        a.runOnUiThread(() -> { try { a.startActivity(i); } catch (Exception ignored) {} });
    }

    @JavascriptInterface public String load() { return Store.state(a); }

    @JavascriptInterface public void save(String s) { Store.setState(a, s); }

    @JavascriptInterface public String takeAnswers() { return Store.takeAnswers(a); }

    @JavascriptInterface public void setSchedule(String json) { Scheduler.replace(a, json); }

    @JavascriptInterface
    public String status() {
        JSONObject o = new JSONObject();
        try {
            o.put("overlay", Settings.canDrawOverlays(a));
            o.put("notif", a.getSystemService(NotificationManager.class).areNotificationsEnabled());
            AlarmManager am = a.getSystemService(AlarmManager.class);
            o.put("exact", Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms());
            PowerManager pm = a.getSystemService(PowerManager.class);
            o.put("battery", pm.isIgnoringBatteryOptimizations(pkg()));
            o.put("next", Scheduler.nextTs(a));
            o.put("count", Store.schedule(a).length());
        } catch (JSONException ignored) {}
        return o.toString();
    }

    @JavascriptInterface
    public void requestOverlay() {
        go(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + pkg())));
    }

    @JavascriptInterface
    public void requestNotif() {
        if (Build.VERSION.SDK_INT >= 33 && !Store.flag(a, "notifAsked2")
                && a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Store.setFlag(a, "notifAsked2");
            a.runOnUiThread(() -> a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2));
        } else {
            go(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg()));
        }
    }

    @JavascriptInterface
    public void requestExact() {
        if (Build.VERSION.SDK_INT >= 31) go(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + pkg())));
    }

    @JavascriptInterface
    public void requestBattery() {
        go(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + pkg())));
    }

    /** Ayarlardaki "dene" dugmesi: soruyu hemen goster (cevap kaydedilmez). */
    @JavascriptInterface
    public void testAsk(String json) {
        a.runOnUiThread(() -> {
            try {
                JSONObject ev = new JSONObject(json);
                ev.put("test", true);
                AlarmReceiver.show(a, ev);
            } catch (JSONException ignored) {}
        });
    }

    /** Yedek: JSON'u paylasma menusuyle disari ver (Drive, WhatsApp, e-posta...). */
    @JavascriptInterface
    public void share(String text) {
        Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, "Vakit yedeği").putExtra(Intent.EXTRA_TEXT, text);
        go(Intent.createChooser(i, "Yedeği kaydet"));
    }
}
