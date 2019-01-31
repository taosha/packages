package taosha.loader

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.IntDef
import androidx.collection.LruCache
import taosha.packages.R
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object Icons {
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
    private lateinit var cache: LruCache<ResolveInfo, Drawable>

    private fun init(context: Context) {
        pm = context.applicationContext.packageManager
        cache = object : LruCache<ResolveInfo, Drawable>(200) {
            override fun create(key: ResolveInfo): Drawable = key.loadIcon(pm)
        }
    }

    fun with(context: Context): LoaderResolver<ResolveInfo, ImageView> {
        val applicationContext = context.applicationContext
        if (!Icons::pm.isInitialized) {
            synchronized(this) {
                if (!Icons::pm.isInitialized) {
                    init(applicationContext)
                }
            }
        }
        return object : LoaderResolver<ResolveInfo, ImageView> {
            override fun load(k: ResolveInfo): Loader<ImageView> =
                AsyncViewLoader(
                    tag = R.id.tag_loader,
                    k = k,
                    create = cache::get,
                    resolve = ImageView::setImageDrawable,
                    executor = executor
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


