package packages.loader

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import androidx.annotation.IntDef
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PackageData(val icon: Drawable, val label: CharSequence, val packageName: String)

object Packages {
    const val TRIM_LOW = 0
    const val TRIM_CRITICAL = 1
    const val TRIM_ALL = 2

    private lateinit var pm: PackageManager
    private lateinit var cache: LruCache<ResolveInfo, PackageData>

    private fun init(context: Context) {
        pm = context.packageManager
        cache = object : LruCache<ResolveInfo, PackageData>(200) {
            override fun create(key: ResolveInfo): PackageData =
                PackageData(key.loadIcon(pm), key.loadLabel(pm), key.activityInfo.packageName)
        }
    }

    suspend fun load(context: Context, info: ResolveInfo): PackageData = withContext(Dispatchers.IO) {
        if (!Packages::pm.isInitialized) {
            synchronized(this@Packages) {
                if (!Packages::pm.isInitialized) {
                    init(context.applicationContext)
                }
            }
        }
        cache.get(info)!!
    }

    fun trimCache(@TrimLevel level: Int) {
        if (!Packages::cache.isInitialized) return
        when (level) {
            TRIM_LOW ->
                cache.trimToSize(cache.maxSize() / 2)
            TRIM_CRITICAL ->
                cache.trimToSize(cache.maxSize() / 10)
            TRIM_ALL ->
                cache.trimToSize(0)
        }
    }

    @IntDef(
        TRIM_LOW,
        TRIM_CRITICAL,
        TRIM_ALL
    )
    annotation class TrimLevel
}
