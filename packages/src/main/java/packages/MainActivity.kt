package packages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.painter.rememberDrawablePainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import packages.loader.PackageData
import packages.loader.Packages

class MainActivity : ComponentActivity() {

    private val packageChangesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshPackages()
        }
    }

    private val packagesState = MutableStateFlow<List<AppEntry>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isLightTheme =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO
        insetsController.isAppearanceLightStatusBars = isLightTheme
        refreshPackages()
        registerPackageReceiver()

        setContent {
            val packages by packagesState.collectAsState()
            PackagesTheme {
                PackagesScreen(
                    entries = packages,
                    onShare = ::sharePackage
                )
            }
        }
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addDataScheme("package")
        }
        registerReceiver(packageChangesReceiver, filter)
    }

    private fun sharePackage(label: String, packageName: String) {
        val intent = ShareCompat.IntentBuilder(this)
            .setType("text/plain")
            .setText("$label: $packageName")
            .intent
        startActivity(Intent.createChooser(intent, getString(R.string.app_name)))
    }

    private fun refreshPackages() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.Default) {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                packageManager.queryIntentActivities(intent, 0)
                    .map { info ->
                        val label = info.loadLabel(packageManager).toString()
                        val packageName = info.activityInfo.packageName
                        AppEntry(info, label, packageName)
                    }
                    .sortedBy { it.label.lowercase() }
            }
            packagesState.value = result
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(packageChangesReceiver)
    }
}

@Composable
private fun PackagesScreen(
    entries: List<AppEntry>,
    onShare: (String, String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredEntries = rememberFilteredEntries(entries, query)
    val horizontalPadding = dimensionResource(R.dimen.horizontal_edge_spacing)
    val bottomPadding = dimensionResource(R.dimen.vertical_edge_spacing)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { PackagesTopBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = horizontalPadding)
        ) {
            Spacer(modifier = Modifier.size(12.dp))
            SearchField(query = query, onQueryChanged = { query = it })
            Spacer(modifier = Modifier.size(12.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredEntries, key = { it.packageName }) { entry ->
                    PackageRow(entry = entry, query = query, onShare = onShare)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackagesTopBar() {
    CenterAlignedTopAppBar(
        title = { Text(text = stringResource(R.string.app_name)) },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun SearchField(query: String, onQueryChanged: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = query,
        onValueChange = onQueryChanged,
        singleLine = true,
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
        label = { Text(text = stringResource(R.string.search_hint)) }
    )
}

@Composable
private fun PackageRow(entry: AppEntry, query: String, onShare: (String, String) -> Unit) {
    val context = LocalContext.current
    val packageData = loadPackageData(entry, context)
    val painter = packageData?.icon?.let { rememberDrawablePainter(drawable = it) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShare(entry.label, entry.packageName) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconOrPlaceholder(painter = painter)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = highlightMatches(entry.label, query), style = MaterialTheme.typography.titleMedium)
                Text(text = highlightMatches(entry.packageName, query), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun IconOrPlaceholder(painter: Painter?) {
    if (painter != null) {
        androidx.compose.foundation.Image(
            painter = painter,
            contentDescription = stringResource(R.string.item_app_icon),
            modifier = Modifier.size(48.dp)
        )
    } else {
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun loadPackageData(entry: AppEntry, context: Context): PackageData? {
    var data by remember { mutableStateOf<PackageData?>(null) }
    LaunchedEffect(entry) {
        data = Packages.loadPackageData(context, entry.info)
    }
    return data
}

@Composable
private fun highlightMatches(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)
    val lowerQuery = query.lowercase()
    val lowerText = text.lowercase()
    val builder = buildAnnotatedString {
        var startIndex = 0
        while (startIndex < text.length) {
            val index = lowerText.indexOf(lowerQuery, startIndex)
            if (index < 0) {
                append(text.substring(startIndex))
                break
            }
            append(text.substring(startIndex, index))
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                append(text.substring(index, index + lowerQuery.length))
            }
            startIndex = index + lowerQuery.length
        }
    }
    return builder
}

@Composable
private fun rememberFilteredEntries(entries: List<AppEntry>, query: String): List<AppEntry> =
    remember(entries, query) {
        val trimmedQuery = query.trim().lowercase()
        entries.filter {
            trimmedQuery.isEmpty() ||
                    it.packageName.lowercase().contains(trimmedQuery) ||
                    it.label.lowercase().contains(trimmedQuery)
        }
    }

data class AppEntry(
    val info: ResolveInfo,
    val label: String,
    val packageName: String
)
