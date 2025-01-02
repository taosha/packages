package packages.loader

import android.view.View

interface Loader<Result> {
    fun <Target : View> into(target: Target, resolve: (Target, Result?) -> Unit)
}

interface LoaderProvider<Param, Result> {
    fun load(param: Param): Loader<Result>
}
