package com.raisoft.grabi.ui.screens.autobot

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raisoft.grabi.R
import com.raisoft.grabi.data.vpn.autobot.VpnBookSource
import org.json.JSONTokener

@Composable
fun VpnBookHandoffScreen(
    onBack: () -> Unit = {},
    viewModel: AutoBotViewModel = hiltViewModel(),
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pasteText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val importResult by viewModel.importResult.collectAsStateWithLifecycle()
    val notFound = stringResource(R.string.vpnbook_not_found)
    val imported = stringResource(R.string.vpnbook_import_ok)

    LaunchedEffect(importResult) {
        when (val result = importResult) {
            null -> Unit
            "ok" -> {
                message = imported
                viewModel.consumeImportResult()
            }
            else -> {
                message = result
                viewModel.consumeImportResult()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
            Text(
                text = stringResource(R.string.vpnbook_handoff_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Text(
            text = stringResource(R.string.vpnbook_handoff_banner),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl(VpnBookSource.PAGE_URL)
                }
            },
            update = { webView = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    probeGeneratedConfig(webView) { text ->
                        if (text != null) {
                            viewModel.importAssisted(text, "vpnbook")
                        } else {
                            message = notFound
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.vpnbook_import_generated))
            }
            Button(
                onClick = { viewModel.importAssisted(pasteText, "vpnbook") },
                enabled = pasteText.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.vpnbook_import_paste))
            }
        }
        OutlinedTextField(
            value = pasteText,
            onValueChange = { pasteText = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            label = { Text(stringResource(R.string.vpnbook_paste_label)) },
        )
        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun probeGeneratedConfig(webView: WebView?, onResult: (String?) -> Unit) {
    val view = webView ?: return onResult(null)
    view.evaluateJavascript(PROBE_JS) { value ->
        val text = runCatching { JSONTokener(value).nextValue() as? String }.getOrNull()
        onResult(text?.takeIf { it.contains("[Interface]") && it.contains("[Peer]") })
    }
}

private const val PROBE_JS =
    "(function(){var els=document.querySelectorAll('textarea,pre,code');" +
        "for(var i=0;i<els.length;i++){var t=els[i].value||els[i].textContent||'';" +
        "if(t.indexOf('[Interface]')>=0&&t.indexOf('[Peer]')>=0){return t;}}return '';})()"