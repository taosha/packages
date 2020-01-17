package taosha.loader

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import androidx.annotation.IntDef
import androidx.collection.LruCache
import taosha.packages.R
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

data class PackageData(val icon: Drawable, val label: CharSequence, val packageName: String)

object Packages {
    const val TRIM_LOW = 0
    const val TRIM_CRITICAL = 1
    const val TRIM_ALL = 2

    private val executor by lazy {
        ThreadPoolExecutor(
            0, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            SynchronousQueue()
        ) { runnable ->
            val t = Thread(runnable)
            t.priority = 2
            t
        }
    }
    private lateinit var pm: PackageManager
    private lateinit var cache: LruCache<ResolveInfo, PackageData>

    private fun init(context: Context) {
        pm = context.packageManager
        cache = object : LruCache<ResolveInfo, PackageData>(200) {
            override fun create(key: ResolveInfo): PackageData =
                PackageData(key.loadIcon(pm), key.loadLabel(pm), key.activityInfo.packageName)
        }
    }

    fun with(context: Context): LoaderProvider<ResolveInfo, PackageData> {
        if (!Packages::pm.isInitialized) {
            synchronized(this) {
                if (!Packages::pm.isInitialized) {
                    init(context.applicationContext)
                }
            }
        }
        return object : LoaderProvider<ResolveInfo, PackageData> {
            override fun load(param: ResolveInfo): Loader<PackageData> =
                AsyncLoader(
                    executor = executor,
                    tag = R.id.tag_loader,
                    param = param,
                    load = cache::get
                )
        }
    }

    fun trimCache(@TrimLevel level: Int) {
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



