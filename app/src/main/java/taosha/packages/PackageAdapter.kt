package taosha.packages

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import taosha.loader.Packages
import taosha.packages.databinding.PackageItemBinding
import java.util.*

class AppInfoDiffCallback : DiffUtil.ItemCallback<ResolveInfo>() {
    override fun areItemsTheSame(oldItem: ResolveInfo, newItem: ResolveInfo): Boolean {
        return oldItem.activityInfo.packageName == newItem.activityInfo.packageName
    }

    override fun areContentsTheSame(oldItem: ResolveInfo, newItem: ResolveInfo): Boolean {
        return oldItem.activityInfo.packageName == newItem.activityInfo.packageName
    }

    override fun getChangePayload(oldItem: ResolveInfo, newItem: ResolveInfo): Any {
        return newItem
    }
}

class PackageAdapter(private val context: Context) :
    ListAdapter<ResolveInfo, PackageViewHolder>(AppInfoDiffCallback()) {

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
        submitList(
            query?.trim()?.lowercase()?.let { q ->
                apps.filter {
                    it.activityInfo.packageName.lowercase().contains(q)
                            || it.loadLabel(context.packageManager).contains(q)
                }
            } ?: apps
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
            holder.render(it, query)
        }
    }

    fun setFilter(query: String?) {
        this.query = query
        applyFilter()
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

        val builder = SpannableStringBuilder()
        builder.append(text)
        val h = highlighted!!.lowercase(Locale.getDefault())
        val s = text.lowercase(Locale.getDefault())
        var i = -1
        do {
            val span = ForegroundColorSpan(Color.RED)
            i = s.indexOf(h, i + 1)
            if (i >= 0) {
                builder.setSpan(span, i, i + h.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } while (i >= 0)
        return builder
    }
}

