package io.legado.app.help.site.hotupub

import android.app.job.JobParameters
import android.app.job.JobService

/**
 * Durable execution host for scheduled Hotu account-pool sign-in.
 *
 * BroadcastReceiver only schedules this job. Network requests run here so multiple accounts are
 * not constrained by BroadcastReceiver's short execution window.
 */
class HotuSignInJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        HotuAutoSignIn.runDueAsync {
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        // Account attempts are idempotent per Hotu site day. Ask JobScheduler to retry when the
        // system interrupts the job before our callback completes.
        return true
    }
}
