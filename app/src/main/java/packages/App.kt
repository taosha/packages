package packages

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import packages.loader.Packages

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(
            this,
            DynamicColorsOptions.Builder().setThemeOverlay(R.style.ThemeOverlay_Packages).build()
        )
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW ->
                Packages.trimCache(Packages.TRIM_LOW)
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_UI_HIDDEN ->
                Packages.trimCache(Packages.TRIM_CRITICAL)
            else ->
                Packages.trimCache(Packages.TRIM_ALL)
        }
    }
}
