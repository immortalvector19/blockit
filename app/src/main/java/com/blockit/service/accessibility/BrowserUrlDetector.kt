package com.blockit.service.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.blockit.domain.matcher.DomainMatcher

object BrowserUrlDetector {

    val SUPPORTED_BROWSERS = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.sec.android.app.sbrowser",
        "com.duckduckgo.mobile.android",
        "com.kiwibrowser.browser",
        "com.vivaldi.browser"
    )

    private val KNOWN_URL_VIEW_IDS = listOf(
        "com.android.chrome:id/url_bar",
        "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
        "com.brave.browser:id/url_bar",
        "com.microsoft.emmx:id/url_bar",
        "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        "com.opera.browser:id/url_field"
    )

    /**
     * Extracts current website URL / domain from a browser's Accessibility node hierarchy.
     */
    fun extractUrlFromBrowser(rootNode: AccessibilityNodeInfo, packageName: String): String? {
        if (!SUPPORTED_BROWSERS.contains(packageName)) return null

        // 1. Try known view resource IDs first for instant O(1) lookup
        for (viewId in KNOWN_URL_VIEW_IDS) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            if (!nodes.isNullOrEmpty()) {
                val node = nodes[0]
                val text = node.text?.toString() ?: node.contentDescription?.toString()
                if (!text.isNullOrBlank()) {
                    val host = DomainMatcher.extractCleanHost(text)
                    if (host.contains('.')) {
                        return host
                    }
                }
            }
        }

        // 2. Fallback: Depth-limited traversal to find address bar EditText
        return findUrlByTraversal(rootNode, depth = 0, maxDepth = 6)
    }

    private fun findUrlByTraversal(node: AccessibilityNodeInfo?, depth: Int, maxDepth: Int): String? {
        if (node == null || depth > maxDepth) return null

        val text = node.text?.toString()
        if (!text.isNullOrBlank() && (node.className == "android.widget.EditText" || node.isEditable)) {
            val host = DomainMatcher.extractCleanHost(text)
            if (host.contains('.') && !host.contains(' ') && host.length in 4..100) {
                return host
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findUrlByTraversal(child, depth + 1, maxDepth)
            if (result != null) return result
        }

        return null
    }
}
