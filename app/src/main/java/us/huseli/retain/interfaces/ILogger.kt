package us.huseli.retain.interfaces

import android.util.Log
import us.huseli.retain.BuildConfig
import us.huseli.retaintheme.utils.ILogger
import us.huseli.retaintheme.utils.LogInstance

interface ILogger : ILogger {
    override fun shouldLog(log: LogInstance): Boolean {
        return log.force || log.priority >= Log.ERROR || BuildConfig.DEBUG
    }
}
