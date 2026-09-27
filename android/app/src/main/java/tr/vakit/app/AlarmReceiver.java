package tr.vakit.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Siradaki alarm caldi: vakti gelen sorulari sor, hatirlatmalari goster, sonrakini kur. */
public class AlarmReceiver extends BroadcastReceiver {
    private static final long ASK_STALE = 8 * 3600 * 1000L;   // telefon kapaliydi vb.: eski soruyu sorma
    private static final long NOTE_STALE = 3600 * 1000L;

    @Override
    public void onReceive(Context c, Intent intent) {
        long now = System.currentTimeMillis(), last = Store.lastFired(c), upTo = now + 30_000;
        JSONArray all = Store.schedule(c), keep = new JSONArray();
        List<JSONObject> due = new ArrayList<>();
        for (int i = 0; i < all.length(); i++) {
            JSONObject o = all.optJSONObject(i);
            if (o == null) continue;
            long ts = o.optLong("ts");
            if (ts > last && ts <= upTo) due.add(o);
            if (ts > now - 86_400_000L) keep.put(o);   // bir gunden eskileri at
        }
        Store.setSchedule(c, keep);
        Store.setLastFired(c, upTo);
        Collections.sort(due, (a, b) -> Long.compare(a.optLong("ts"), b.optLong("ts")));

        for (JSONObject o : due) {
            long late = now - o.optLong("ts");
            if ("ask".equals(o.optString("kind"))) {
                if (late > ASK_STALE || Store.answered(c, o)) continue;
                show(c, o);
            } else if (late <= NOTE_STALE) {
                Notifier.note(c, o);
            }
        }
        Scheduler.next(c);
    }

    static void show(Context c, JSONObject ev) {
        if (Settings.canDrawOverlays(c)) Overlay.show(c, ev);
        else Notifier.ask(c, ev);
    }
}
