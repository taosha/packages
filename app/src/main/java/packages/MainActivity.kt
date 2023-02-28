package packages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.WindowCompat
import packages.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            (binding.recyclerView.adapter as PackageAdapter).refresh()
        }
    }

    @ColorInt
    fun getColorFromAttr(
        @AttrRes attrColor: Int,
        typedValue: TypedValue = TypedValue(),
        resolveRefs: Boolean = true
    ): Int {
        theme.resolveAttribute(attrColor, typedValue, resolveRefs)
        return typedValue.data
    }

    private fun isNightMode(): Boolean =
        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                insetsController?.isAppearanceLightStatusBars =
                    !resources.configuration.isNightModeActive
                window.statusBarColor = getColorFromAttr(R.attr.colorOnPrimary)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                insetsController?.isAppearanceLightStatusBars = !isNightMode()
                window.statusBarColor = getColorFromAttr(R.attr.colorOnPrimary)
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.recyclerView.adapter = PackageAdapter(this)
        registerPackageReceiver()
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        filter.addAction(Intent.ACTION_PACKAGE_ADDED)
        registerReceiver(packageReceiver, filter)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val searchView = menu.findItem(R.id.action_search)?.actionView as SearchView
        searchView.queryHint = getText(R.string.search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                (binding.recyclerView.adapter as PackageAdapter).setFilter(newText)
                return true
            }
        })
        return true
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(packageReceiver)
    }
}
