package tr.vakit.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.lang.ref.WeakReference;

/** Arayuzun kendisi yayindaki sitedir; boylece tasarim degisince uygulamayi yeniden kurmak gerekmez. */
public class MainActivity extends Activity {
    static final String HOST = "selimkerem1997.github.io";
    static final String URL = "https://" + HOST + "/vakit/";

    private static WeakReference<MainActivity> live = new WeakReference<>(null);
    WebView web;

    /** Acilir kutudan cevap gelince acik arayuzu tazele. */
    static void refresh() {
        MainActivity a = live.get();
        if (a != null) a.runOnUiThread(() -> { if (a.web != null) a.web.evaluateJavascript("window.vakitResume && vakitResume()", null); });
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        live = new WeakReference<>(this);
        Notifier.channels(this);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Bridge(this), "VakitApp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (HOST.equals(u.getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;
            }
        });
        if (saved != null) web.restoreState(saved);
        else web.loadUrl(URL);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !Store.flag(this, "notifAsked")) {
            Store.setFlag(this, "notifAsked");
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        Scheduler.next(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        web.resumeTimers();
        refresh();
    }

    @Override
    protected void onPause() {
        web.onPause();
        web.pauseTimers();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        refresh();
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (live.get() == this) live = new WeakReference<>(null);
        web.destroy();
        super.onDestroy();
    }
}
