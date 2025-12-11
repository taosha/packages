package packages.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.app.ShareCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import packages.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<ResolveInfo>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val currentOnAppsChanged by rememberUpdatedState {
        // Trigger reload
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val loadedApps = context.packageManager.queryIntentActivities(intent, 0)
        // We need to sort or filter? Original didn't seem to sort explicitly in refresh() but maybe standard order.
        apps = loadedApps
    }

    // Load apps initially
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            currentOnAppsChanged()
        }
    }

    // Register receiver
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                currentOnAppsChanged()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            // Original code registered these actions.
            // Note: ACTION_PACKAGE_ADDED/REMOVED require data scheme "package" to work properly in manifest or filter?
            // Original code:
            // filter.addAction(Intent.ACTION_PACKAGE_REMOVED)
            // filter.addAction(Intent.ACTION_PACKAGE_ADDED)
            // It didn't specify scheme in code shown, but usually it's needed.
            // Assuming original worked or relied on something else, but let's stick to original intent filter config.
            addDataScheme("package") 
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val filteredApps = remember(apps, query) {
        if (query.isBlank()) apps else {
            val q = query.lowercase()
            apps.filter {
                it.activityInfo.packageName.lowercase().contains(q) ||
                        it.loadLabel(context.packageManager).toString().lowercase().contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text(stringResource(id = R.string.search_hint)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp)
                                .padding(end = 12.dp),
                            shape = RoundedCornerShape(999.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (query.isNotBlank()) {
                                            query = ""
                                        } else {
                                            isSearchActive = false
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (query.isNotBlank()) "Clear Search" else "Close Search"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                cursorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                                focusedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                                focusedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                                focusedContainerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(stringResource(id = R.string.app_name))
                    }
                },
                actions = {
                    if (isSearchActive) {
                        // Trailing icon in the search field handles clear/close.
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 8.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredApps) { app ->
                PackageItem(info = app, query = query) {
                    val label = app.loadLabel(context.packageManager)
                    val pkg = app.activityInfo.packageName
                    val shareIntent = ShareCompat.IntentBuilder(context)
                        .setType("text/plain")
                        .setText("$label: $pkg")
                        .intent
                    context.startActivity(Intent.createChooser(shareIntent, "Share"))
                }
            }
        }
    }
}

