package tr.vakit.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Arayuzun hesapladigi ~30 gunluk olay listesini saklar ve her seferinde yalnizca
 * siradaki olay icin tek bir alarm kurar. Alarm calinca bir sonrakini kurar.
 */
final class Scheduler {
    private Scheduler() {}

    static final long SNOOZE_MS = 20 * 60 * 1000L;

    /** Listeyi degistir; ertelenmis ("Sonra") sorular korunur. */
    static void replace(Context c, String json) {
        try {
            JSONArray in = new JSONArray(json);
            JSONArray old = Store.schedule(c);
            long now = System.currentTimeMillis();
            for (int i = 0; i < old.length(); i++) {
                JSONObject o = old.optJSONObject(i);
                if (o != null && o.optBoolean("snz") && o.optLong("ts") > now) in.put(o);
            }
            Store.setSchedule(c, in);
        } catch (JSONException ignored) {}
        next(c);
    }

    static void snooze(Context c, JSONObject ev) {
        try {
            JSONObject o = new JSONObject(ev.toString());
            o.put("ts", System.currentTimeMillis() + SNOOZE_MS);
            o.put("snz", true);
            JSONArray a = Store.schedule(c);
            a.put(o);
            Store.setSchedule(c, a);
        } catch (JSONException ignored) {}
        next(c);
    }

    static long nextTs(Context c) {
        JSONArray a = Store.schedule(c);
        long from = Math.max(Store.lastFired(c), System.currentTimeMillis()), best = Long.MAX_VALUE;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            long ts = o.optLong("ts");
            if (ts > from && ts < best) best = ts;
        }
        return best == Long.MAX_VALUE ? 0 : best;
    }

    static void next(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(c, 0, new Intent(c, AlarmReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        long t = nextTs(c);
        if (t == 0) return;
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
        }
    }
}
