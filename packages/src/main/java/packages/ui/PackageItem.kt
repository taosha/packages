package packages.ui

import android.content.pm.ResolveInfo
import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import packages.loader.PackageData
import packages.loader.Packages

@Composable
fun PackageItem(
    info: ResolveInfo,
    query: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val data = produceState<PackageData?>(initialValue = null, key1 = info) {
        value = Packages.load(context, info)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (data.value != null) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                },
                update = { view ->
                    view.setImageDrawable(data.value!!.icon)
                },
                modifier = Modifier.size(40.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = highlight(data.value?.label?.toString() ?: "", query),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = highlight(data.value?.packageName ?: info.activityInfo.packageName, query),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

fun highlight(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return buildAnnotatedString { append(text) }
    
    return buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var startIndex = 0
        var index = lowerText.indexOf(lowerQuery)
        
        while (index >= 0) {
            append(text.substring(startIndex, index))
            withStyle(style = SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                append(text.substring(index, index + query.length))
            }
            startIndex = index + query.length
            index = lowerText.indexOf(lowerQuery, startIndex)
        }
        append(text.substring(startIndex))
    }
}

