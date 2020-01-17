package taosha.packages

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.package_item.view.*
import taosha.loader.Packages
import java.util.*


private val View.activity: Activity?
    get() {
        var context = this.context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context as Activity
            } else {
                context = context.baseContext
            }
        }
        return null
    }

class PackageAdapter(val context: Context) : RecyclerView.Adapter<PackageViewHolder>() {
    private var list: MutableList<ResolveInfo>? = null
    private var queryResult: List<ResolveInfo>? = null
    private var q: String? = null

    init {
        refresh()
    }

    fun refresh() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK
        list = context.packageManager.queryIntentActivities(intent, 0)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        return PackageViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.package_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = queryResult?.size ?: list?.size ?: 0

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        holder.render(queryResult?.get(position) ?: list?.get(position)!!, q)
    }

    fun setFilter(query: String?) {
        if (TextUtils.isEmpty(query?.trim())) {
            q = null
            queryResult = null
        } else {
            q = query?.trim()?.toLowerCase(Locale.getDefault())
            queryResult = list?.filter { info ->
                info.activityInfo.packageName.toLowerCase().contains(q!!)
                        || info.loadLabel(context.packageManager).contains(q!!)
            }
        }
        notifyDataSetChanged()
    }
}

class PackageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val context: Context = itemView.context
    private var appName: String? = null
    private var packageName: String? = null

    init {
        itemView.setOnClickListener {
            val intent = ShareCompat.IntentBuilder.from(itemView.activity)
                .setType("text/plain")
                .setText("$appName: $packageName")
                .intent
            context.startActivity(Intent.createChooser(intent, "Share"))
        }
    }

    fun render(info: ResolveInfo, q: String?) {
        Packages.with(context).load(info).into(itemView, { view, data ->
            view.icon.setImageDrawable(data?.icon)
            view.name.text = highlight(data?.label.toString(), q)
            view.appId.text = highlight(data?.packageName!!, q)
            this.packageName = data?.packageName
            this.appName = data?.label.toString()
        })
    }

    private fun highlight(text: String, highlighted: String?): CharSequence? {
        if (TextUtils.isEmpty(highlighted))
            return text

        val builder = SpannableStringBuilder()
        builder.append(text)
        val h = highlighted!!.toLowerCase()
        val s = text.toLowerCase()
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

