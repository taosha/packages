package packages.loader

import android.os.AsyncTask
import android.view.View
import java.lang.ref.WeakReference
import java.util.concurrent.Executor

class AsyncLoader<Param, Result>(
    val executor: Executor? = null,
    val tag: Int,
    val param: Param,
    val load: (Param) -> Result?
) : Loader<Result> {
    override fun <Target : View> into(target: Target, resolve: (Target, Result?) -> Unit) {
        (target.getTag(tag) as Cancelable?)?.cancel(false)
        if (executor != null) {
            ImageLoaderTask(target, tag, load, resolve).executeOnExecutor(executor, param)
        } else {
            ImageLoaderTask(target, tag, load, resolve).execute(param)
        }
    }

    interface Cancelable {
        fun cancel(b: Boolean): Boolean
    }

    class ImageLoaderTask<Param, Result, Target : View>(
        view: Target,
        val tag: Int,
        val load: (Param) -> Result?,
        val resolve: (Target, Result?) -> Unit
    ) :
        AsyncTask<Param, Any?, Result?>(), Cancelable {
        val ref = WeakReference(view)

        override fun doInBackground(vararg params: Param): Result? = load(params[0])

        override fun onPreExecute() {
            ref.get()?.setTag(tag, this)
        }

        override fun onPostExecute(result: Result?) {
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
