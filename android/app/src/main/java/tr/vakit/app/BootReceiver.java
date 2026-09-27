package tr.vakit.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Telefon yeniden baslayinca, saat degisince ya da uygulama guncellenince alarmi yeniden kur. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        Scheduler.next(c);
    }
}
