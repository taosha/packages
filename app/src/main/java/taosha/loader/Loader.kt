package taosha.loader

interface Loader<T> {
    fun into(target: T)
}

interface LoaderResolver<K, T> {
    fun load(k: K): Loader<T>
}
