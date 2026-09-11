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

    override fun onStartJob(params: JobParameters): Boolean {
        HotuAutoSignIn.runDueAsync {
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        // The executor cannot safely cancel an in-flight HTTP request here. Returning false avoids
        // creating a duplicate job while that request may still finish. Startup catch-up and the
        // next daily trigger will retry accounts whose site-day attempt was not recorded.
        return false
    }
}
