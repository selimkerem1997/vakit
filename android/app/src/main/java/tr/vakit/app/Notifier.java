package tr.vakit.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Normal bildirimler: vakit girdi, spor saati; ustten cizme izni yoksa sorular da buradan. */
final class Notifier {
    private Notifier() {}

    static final String CH_ASK = "sorular", CH_NOTE = "hatirlatmalar";

    static void channels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel a = new NotificationChannel(CH_ASK, "Sorular", NotificationManager.IMPORTANCE_HIGH);
        a.setDescription("Namaz, su ve spor soruları");
        nm.createNotificationChannel(a);
        NotificationChannel n = new NotificationChannel(CH_NOTE, "Hatırlatmalar", NotificationManager.IMPORTANCE_HIGH);
        n.setDescription("Vakit girdi, spor saati");
        nm.createNotificationChannel(n);
    }

    private static PendingIntent open(Context c) {
        Intent i = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 1, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static int nid(JSONObject ev) { return (ev.optString("dk") + "|" + ev.optString("id")).hashCode(); }

    static void note(Context c, JSONObject ev) {
        channels(c);
        String text = ev.optString("text");
        Notification n = new Notification.Builder(c, CH_NOTE)
                .setSmallIcon(R.drawable.ic_stat)
                .setLargeIcon(BitmapFactory.decodeResource(c.getResources(), R.drawable.face_neutral))
                .setContentTitle(ev.optString("title"))
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(open(c))
                .setAutoCancel(true)
                .build();
        post(c, nid(ev), n);
    }

    /** Ustten cizme izni yoksa: soruyu dugmeli bildirim olarak gonder (en fazla 3 dugme). */
    static void ask(Context c, JSONObject ev) {
        channels(c);
        int id = nid(ev);
        String q = ev.optString("q");
        Notification.Builder b = new Notification.Builder(c, CH_ASK)
                .setSmallIcon(R.drawable.ic_stat)
                .setLargeIcon(BitmapFactory.decodeResource(c.getResources(), R.drawable.face_neutral))
                .setContentTitle(ev.optString("title"))
                .setContentText(q)
                .setStyle(new Notification.BigTextStyle().bigText(q))
                .setContentIntent(open(c))
                .setAutoCancel(true);
        JSONArray opts = ev.optJSONArray("opts");
        List<JSONArray> yes = new ArrayList<>(), no = new ArrayList<>();
        if (opts != null) for (int i = 0; i < opts.length(); i++) {
            JSONArray o = opts.optJSONArray(i);
            if (o != null) (o.optInt(1) > 0 ? yes : no).add(o);
        }
        List<JSONArray> pick = new ArrayList<>(yes.subList(0, Math.min(no.isEmpty() ? 3 : 2, yes.size())));
        if (!no.isEmpty()) pick.add(no.get(0));
        for (int k = 0; k < pick.size(); k++) {
            JSONArray o = pick.get(k);
            Intent i = new Intent(c, AnswerReceiver.class)
                    .putExtra("ev", ev.toString()).putExtra("val", o.optInt(1)).putExtra("nid", id);
            PendingIntent pi = PendingIntent.getBroadcast(c, id * 8 + k, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            b.addAction(new Notification.Action.Builder((Icon) null, o.optString(0), pi).build());
        }
        post(c, id, b.build());
    }

    private static void post(Context c, int id, Notification n) {
        if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        c.getSystemService(NotificationManager.class).notify(id, n);
    }
}
