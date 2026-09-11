package io.legado.app.help.site.hotupub

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class HotuSignInReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                HotuAutoSignIn.ensureScheduled(context)
                val pending = goAsync()
                HotuAutoSignIn.runDueAsync {
                    pending.finish()
                }
            }

            HotuAutoSignIn.ACTION_DAILY_SIGN -> {
                val pending = goAsync()
                HotuAutoSignIn.runDueAsync {
                    pending.finish()
                }
            }
        }
    }
}
