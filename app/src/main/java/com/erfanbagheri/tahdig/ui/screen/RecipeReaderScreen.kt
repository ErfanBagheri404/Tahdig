package com.erfanbagheri.tahdig.ui.screen

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.HtmlArticleExtractor
import com.erfanbagheri.tahdig.util.RecipeDraft
import com.erfanbagheri.tahdig.util.RecipeTextParser
import org.json.JSONObject

/**
 * #78 in-app reader: open a page, read it clean, «ذخیره دستور» sends the
 * extracted text through the SAME [RecipeTextParser] as paste-import, then
 * routes to the existing review screen. No new parse path, no new draft type.
 *
 * Extraction runs on the RENDERED DOM (`outerHTML` of the article/main node),
 * not the fetched markup: lazy content, script-driven comment toggles and
 * paywall teasers exist only after the page has run.
 *
 * ponytail: JS stays ON because that is the whole point of DOM extraction —
 * flip it off only if a page proves the reader unusable without it.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RecipeReaderScreen(
    url: String,
    onSave: (RecipeDraft) -> Unit,
    onBack: () -> Unit,
) {
    // Extracted once, on page finish: a re-extract after a JS mutation would
    // race the «ذخیره» tap.
    var extracted by remember(url) { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)

    Scaffold(
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = {
                        val draft = extracted?.let {
                            RecipeTextParser.parse(it).copy(sourceUrl = url)
                        } ?: return@Button
                        onSave(draft)
                    },
                    enabled = extracted != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(text = "ذخیره دستور", fontFamily = YekanBakh)
                }
            }
        },
    ) { padding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, pageUrl: String?) {
                            super.onPageFinished(view, pageUrl)
                            view.evaluateJavascript(ARTICLE_HTML_JS) { value ->
                                val html = unwrapJsonString(value)
                                if (html.isNotBlank()) {
                                    extracted = HtmlArticleExtractor.extract(html)
                                }
                            }
                        }
                    }
                    loadUrl(url)
                }
            },
            update = { /* one-shot: the URL is fixed per open */ },
        )
    }
}

/**
 * The rendered article (or main, or body) as HTML. Returning HTML rather than
 * `innerText` is deliberate: [HtmlArticleExtractor] strips ad/comment
 * CONTAINERS by selector, which is impossible once the markup is flattened to
 * text.
 */
private const val ARTICLE_HTML_JS =
    "(function(){var a=document.querySelector('article,main,[role=main]')||document.body;" +
        "return a?a.outerHTML:'';})()"

/** WebView hands back a JSON-quoted string; unwrap it without a JSON lib call per page. */
private fun unwrapJsonString(raw: String?): String {
    val v = raw ?: return ""
    if (v == "null" || v.length < 2 || !v.startsWith("\"")) return ""
    return try {
        JSONObject("{\"v\":$v}").getString("v")
    } catch (e: Exception) {
        ""
    }
}
