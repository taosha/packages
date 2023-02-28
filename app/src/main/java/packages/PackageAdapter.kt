package packages

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import packages.databinding.PackageItemBinding
import packages.loader.Packages
import java.util.*


class PackageAdapter(private val context: Context) :
    ListAdapter<PackageAdapter.Item, PackageViewHolder>(ItemDiffCallback()) {

    private var apps: List<ResolveInfo> = listOf()
    private var query: String? = null

    init {
        refresh()
    }

    fun refresh() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK
        apps = context.packageManager.queryIntentActivities(intent, 0)
        applyFilter()
    }

    private fun applyFilter() {
        val result =
            query?.trim()?.lowercase()?.let { q ->
                apps.filter {
                    it.activityInfo.packageName.lowercase().contains(q)
                            || it.loadLabel(context.packageManager).contains(q)
                }
            } ?: apps
        submitList(
            result.map { Item(it, query) }
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        return PackageViewHolder(
            PackageItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        getItem(position)?.let {
            holder.render(it.appInfo, query)
        }
    }

    fun setFilter(query: String?) {
        this.query = query
        applyFilter()
    }

    data class Item(
        val appInfo: ResolveInfo,
        val query: String? = null,
    )

    class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.appInfo == newItem.appInfo
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: Item, newItem: Item): Any? {
            return newItem.query
        }
    }
}

class PackageViewHolder(private val binding: PackageItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    private var appName: String? = null
    private var packageName: String? = null
    private val context: Context
        get() = itemView.context

    init {
        itemView.setOnClickListener {
            val intent = ShareCompat.IntentBuilder(itemView.context)
                .setType("text/plain")
                .setText("$appName: $packageName")
                .intent
            context.startActivity(Intent.createChooser(intent, "Share"))
        }
    }

    fun render(info: ResolveInfo, q: String?) {
        Packages.with(context).load(info).into(itemView) { _, data ->
            binding.icon.setImageDrawable(data?.icon)
            binding.name.text = highlight(data?.label.toString(), q)
            binding.appId.text = highlight(data?.packageName.toString(), q)
            this.packageName = data?.packageName
            this.appName = data?.label.toString()
        }
    }

    private fun highlight(text: String, highlighted: String?): CharSequence {
        if (TextUtils.isEmpty(highlighted))
            return text

        val builder = SpannableStringBuilder(text)
        val h = highlighted!!.lowercase(Locale.getDefault())
        val s = text.lowercase(Locale.getDefault())
        var i = -1
        do {
            i = s.indexOf(h, i + 1)
            if (i >= 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    builder.setSpan(
                        TypefaceSpan(Typeface.DEFAULT_BOLD),
                        i,
                        i + h.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                builder.setSpan(
                    ForegroundColorSpan(Color.RED),
                    i,
                    i + h.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } while (i >= 0)
        return builder
    }
}

