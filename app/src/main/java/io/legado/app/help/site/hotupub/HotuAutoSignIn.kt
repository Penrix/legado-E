package io.legado.app.help.site.hotupub

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import splitties.init.appCtx
import java.time.ZonedDateTime
import java.util.concurrent.Executors

object HotuAutoSignIn {

    const val ACTION_DAILY_SIGN = "io.legado.app.action.PENRIX_HOTU_DAILY_SIGN"
    private const val ALARM_REQUEST_CODE = 69081
    private const val JOB_ID = 69082
    private const val SIGN_HOUR = 8
    private const val SIGN_MINUTE = 30

    data class AccountResult(
        val account: HotuAccountPool.Account,
        val result: HotuSignInClient.Result
    )

    /**
     * Daily trigger at 08:30 in Hotu's own timezone. Device timezone/VPN location must not shift
     * the sign-in day boundary. The alarm never performs network work itself.
     */
    fun ensureScheduled(context: Context = appCtx) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(context)
        val now = ZonedDateTime.now(HotuAccountPool.siteZone())
        var next = now.toLocalDate()
            .atTime(SIGN_HOUR, SIGN_MINUTE)
            .atZone(HotuAccountPool.siteZone())
        if (!next.isAfter(now)) next = next.plusDays(1)

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    /**
     * Hand scheduled/background execution to JobScheduler so multi-account requests are not bound
     * by BroadcastReceiver's short execution window. Network connectivity is required.
     */
    fun scheduleDueJob(context: Context = appCtx) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val job = JobInfo.Builder(
            JOB_ID,
            ComponentName(context, HotuSignInJobService::class.java)
        )
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setMinimumLatency(0L)
            .build()
        scheduler.schedule(job)
    }

    /** Used while the App is already alive, including manual UI actions and startup catch-up. */
    fun runDueAsync(
        force: Boolean = false,
        onFinished: ((List<AccountResult>) -> Unit)? = null
    ) {
        EXECUTOR.execute {
            val results = runDue(force)
            if (onFinished != null) {
                MAIN.post { onFinished(results) }
            }
        }
    }

    fun runAccountAsync(
        accountId: String,
        onFinished: ((AccountResult?) -> Unit)? = null
    ) {
        EXECUTOR.execute {
            val account = HotuAccountPool.accounts().firstOrNull { it.id == accountId }
            val result = account?.let { signOne(it) }
            if (onFinished != null) MAIN.post { onFinished(result) }
        }
    }

    fun runDue(force: Boolean = false): List<AccountResult> {
        val today = HotuAccountPool.siteToday()
        return HotuAccountPool.accounts()
            .filter { force || HotuAccountPool.isDue(it, today) }
            .map { signOne(it) }
    }

    private fun signOne(account: HotuAccountPool.Account): AccountResult {
        val cookie = HotuAccountPool.cookie(account.id).orEmpty()
        val result = CLIENT.sign(cookie)
        result.updatedCookie?.takeIf { it.isNotBlank() }?.let {
            HotuAccountPool.updateCookie(account.id, it)
        }
        HotuAccountPool.markSignResult(
            account.id,
            result.status,
            result.message
        )
        val updated = HotuAccountPool.accounts().firstOrNull { it.id == account.id } ?: account
        return AccountResult(updated, result)
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, HotuSignInReceiver::class.java).apply {
            action = ACTION_DAILY_SIGN
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val CLIENT by lazy { HotuSignInClient() }
    private val EXECUTOR = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Penrix-Hotu-SignIn").apply { isDaemon = true }
    }
    private val MAIN = Handler(Looper.getMainLooper())
}
