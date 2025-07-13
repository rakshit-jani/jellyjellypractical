package com.jellyjellypractical.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import com.jellyjellypractical.data.model.home.feed.SourceType
import com.jellyjellypractical.data.model.home.feed.VideoItem
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class WebScraper(private val context: Context) {

    private val TAG = "WebScraper"
    private var webView: WebView? = null
    private val collectedItems = mutableSetOf<VideoItem>()

    @SuppressLint("SetJavaScriptEnabled")
    fun scrapeVideos(maxClicks: Int = 2, onComplete: (List<VideoItem>) -> Unit) {
        collectedItems.clear()

        webView = WebView(context.applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115 Safari/537.36"
            WebView.setWebContentsDebuggingEnabled(true)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    clickNextAndScrape(view, 0, maxClicks, onComplete)
                }
            }

            loadUrl("https://jellyjelly.com/feed")
        }
    }

    private fun clickNextAndScrape(
        webView: WebView,
        clickCount: Int,
        maxClicks: Int,
        onComplete: (List<VideoItem>) -> Unit
    ) {
        if (clickCount >= maxClicks) {
            onComplete(collectedItems.toList())
            return
        }

        webView.evaluateJavascript(
            """(function() {
                var btn = document.querySelector('.css-131p7fu');
                if (btn) { btn.click(); return "Clicked"; }
                return "Not Found";
            })();"""
        ) { result ->

            Handler(Looper.getMainLooper()).postDelayed({
                webView.evaluateJavascript(
                    "(function() { return document.documentElement.outerHTML; })();"
                ) { html ->
                    parseHtml(html)
                    clickNextAndScrape(webView, clickCount + 1, maxClicks, onComplete)
                }
            }, 3000)
        }
    }

    private fun parseHtml(html: String?) {
        if (html == null) return

        val cleanedHtml = html
            .replace("\\u003C", "<")
            .replace("\\n", "")
            .replace("\\\"", "\"")

        val doc: Document = Jsoup.parse(cleanedHtml)

        val videoTags = doc.select("video")
        for (videoTag in videoTags) {
            val source = videoTag.selectFirst("source")?.attr("src")
            val poster = videoTag.attr("poster")
            if (!source.isNullOrBlank()) {
                if (source.isNotBlank() && collectedItems.none { it.videoUrl == source }) {
                    Log.d(TAG, "Click $source")
                    collectedItems.add(
                        VideoItem(
                            videoUrl = source,
                            posterUrl = poster,
                            sourceType = SourceType.VIDEO_TAG
                        )
                    )
                }
            }
        }
    }

    fun destroy() {
        webView?.destroy()
        webView = null
    }
}
