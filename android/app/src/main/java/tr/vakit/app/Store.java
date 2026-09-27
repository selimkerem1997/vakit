package tr.vakit.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Telefon ici kayit. "state" web arayuzunun tum durumudur ve yalnizca arayuz yazar.
 * Acilir kutudan gelen cevaplar "answers" kuyruguna eklenir; arayuz acilinca bunlari
 * isleyip kuyrugu bosaltir. Boylece iki taraf birbirinin yazdigini ezmez.
 */
final class Store {
    private Store() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("vakit", Context.MODE_PRIVATE);
    }

    static synchronized String state(Context c) { return p(c).getString("state", ""); }

    static synchronized void setState(Context c, String s) { p(c).edit().putString("state", s).apply(); }

    static synchronized JSONArray schedule(Context c) {
        try { return new JSONArray(p(c).getString("schedule", "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }

    static synchronized void setSchedule(Context c, JSONArray a) { p(c).edit().putString("schedule", a.toString()).apply(); }

    static synchronized String takeAnswers(Context c) {
        String s = p(c).getString("answers", "[]");
        p(c).edit().putString("answers", "[]").commit();
        return s;
    }

    static synchronized void addAnswer(Context c, JSONObject ev, int val) {
        try {
            JSONArray a = new JSONArray(p(c).getString("answers", "[]"));
            JSONObject o = new JSONObject();
            o.put("dk", ev.optString("dk"));
            o.put("id", ev.optString("id"));
            o.put("type", ev.optString("type"));
            o.put("val", val);
            o.put("glass", ev.optInt("glass", 250));
            o.put("at", System.currentTimeMillis());
            a.put(o);
            p(c).edit().putString("answers", a.toString()).commit();
        } catch (JSONException ignored) {}
    }

    /** Bu soru cevaplandi mi (kuyrukta ya da arayuz durumunda), ya da namaz zaten isaretli mi? */
    static synchronized boolean answered(Context c, JSONObject ev) {
        String dk = ev.optString("dk"), id = ev.optString("id");
        try {
            JSONArray a = new JSONArray(p(c).getString("answers", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                if (dk.equals(o.optString("dk")) && id.equals(o.optString("id"))) return true;
            }
        } catch (JSONException ignored) {}
        try {
            JSONObject days = new JSONObject(state(c)).optJSONObject("days");
            JSONObject day = days == null ? null : days.optJSONObject(dk);
            if (day == null) return false;
            JSONObject ask = day.optJSONObject("ask");
            if (ask != null && ask.has(id)) return true;
            if ("p".equals(ev.optString("type"))) {
                JSONObject pr = day.optJSONObject("p");
                return pr != null && pr.optBoolean(ev.optString("key"), false);
            }
        } catch (JSONException ignored) {}
        return false;
    }

    static synchronized long lastFired(Context c) { return p(c).getLong("lastFired", 0); }

    static synchronized void setLastFired(Context c, long t) { p(c).edit().putLong("lastFired", t).apply(); }

    static synchronized boolean flag(Context c, String k) { return p(c).getBoolean(k, false); }

    static synchronized void setFlag(Context c, String k) { p(c).edit().putBoolean(k, true).apply(); }
}
