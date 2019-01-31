package taosha.loader

import android.os.AsyncTask
import android.view.View
import java.lang.ref.WeakReference
import java.util.concurrent.Executor

class AsyncViewLoader<K, D, V : View>(
        val tag: Int,
        val k: K,
        val create: (k: K) -> D?,
        val resolve: (t: V, v: D?) -> Unit,
        val executor: Executor? = null
) : Loader<V> {
    override fun into(view: V) {
        (view.getTag(tag) as Cancelable?)?.cancel(false)
        if (executor != null) {
            ImageLoaderTask(tag, create, resolve, view).executeOnExecutor(executor, k)
        } else {
            ImageLoaderTask(tag, create, resolve, view).execute(k)
        }
    }

    interface Cancelable {
        fun cancel(b: Boolean): Boolean
    }

    class ImageLoaderTask<K, D, V : View>(
            val tag: Int,
            val create: (k: K) -> D?,
            val resolve: (t: V, v: D?) -> Unit,
            view: V
    ) :
            AsyncTask<K, Any, D?>(), Cancelable {
        val ref = WeakReference(view)

        override fun doInBackground(vararg params: K): D? = create(params[0])

        override fun onPreExecute() {
            ref.get()?.setTag(tag, this)
        }

        override fun onPostExecute(result: D?) {
            val view = ref.get()
            if (view?.getTag(tag) == this) {
                if (!isCancelled) {
                    resolve(view, result)
                }
                view.setTag(tag, null)
            }
        }
    }
}
