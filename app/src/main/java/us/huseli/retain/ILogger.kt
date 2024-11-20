package us.huseli.retain

import android.util.Log
import us.huseli.retaintheme.utils.ILogger
import us.huseli.retaintheme.utils.LogInstance

interface ILogger : ILogger {
    override fun shouldLog(log: LogInstance): Boolean {
        return log.force || log.priority >= Log.ERROR || BuildConfig.DEBUG
    }
}
