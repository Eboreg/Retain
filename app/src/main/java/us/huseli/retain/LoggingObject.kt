package us.huseli.retain

import android.util.Log

interface LoggingObject {
    fun log(msg: String, level: Int = Log.INFO) {
        if (BuildConfig.DEBUG) Log.println(
            level,
            "${javaClass.simpleName}<${System.identityHashCode(this)}>",
            "[${Thread.currentThread().name}] $msg"
        )
    }
}