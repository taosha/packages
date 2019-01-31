package taosha.packages

import android.app.Application
import taosha.loader.Icons

class App : Application() {
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW ->
                Icons.trimCache(Icons.TRIM_LOW)
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_UI_HIDDEN ->
                Icons.trimCache(Icons.TRIM_CRITICAL)
            else ->
                Icons.trimCache(Icons.TRIM_ALL)
        }
    }
}
