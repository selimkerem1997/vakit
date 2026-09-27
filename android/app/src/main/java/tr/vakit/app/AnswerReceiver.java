package tr.vakit.app;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

/** Bildirimdeki dugmeye basildi: uygulamayi acmadan cevabi kaydet. */
public class AnswerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        try {
            JSONObject ev = new JSONObject(i.getStringExtra("ev"));
            int val = i.getIntExtra("val", 0);
            if (!ev.optBoolean("test")) {
                Store.addAnswer(c, ev, val);
                MainActivity.refresh();
            }
            c.getSystemService(NotificationManager.class).cancel(i.getIntExtra("nid", 0));
            Toast.makeText(c, ev.optString(val > 0 ? "yes" : "no"), Toast.LENGTH_LONG).show();
        } catch (JSONException | NullPointerException ignored) {}
    }
}
