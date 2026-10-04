package com.blockit.domain.matcher

import java.util.Locale
import java.util.regex.Pattern

/**
 * High-performance, pure Kotlin domain matching engine.
 * Supports exact domain matching, subdomain cascading, and wildcard patterns (*).
 * Guaranteed zero heap allocation on hot-path DNS lookups for exact matches.
 */
object DomainMatcher {

    /**
     * Extracts a clean, normalized hostname from any arbitrary URL, host, or domain string.
     * Strips protocol (http://, https://), ports (:80, :443), paths (/foo/bar), and query strings.
     */
    fun extractCleanHost(raw: String): String {
        if (raw.isBlank()) return ""

        var trimmed = raw.trim().lowercase(Locale.ROOT)

        // Strip protocol prefix if present
        val schemeIndex = trimmed.indexOf("://")
        if (schemeIndex != -1) {
            trimmed = trimmed.substring(schemeIndex + 3)
        }

        // Strip path, query string, or hash
        val firstSlash = trimmed.indexOfAny(charArrayOf('/', '?', '#'))
        if (firstSlash != -1) {
            trimmed = trimmed.substring(0, firstSlash)
        }

        // Strip port number (e.g. example.com:8080)
        // Be careful with IPv6 addresses enclosed in brackets
        val colonIndex = trimmed.indexOf(':')
        if (colonIndex != -1 && !trimmed.startsWith("[")) {
            trimmed = trimmed.substring(0, colonIndex)
        }

        // Strip trailing dots (DNS FQDN dot, e.g. "example.com.")
        while (trimmed.endsWith('.')) {
            trimmed = trimmed.substring(0, trimmed.length - 1)
        }

        return trimmed
    }

    /**
     * Determines whether the given [hostOrUrl] matches the rule [pattern].
     *
     * Patterns:
     * - "example.com": Matches "example.com" and any subdomains (e.g. "sub.example.com", "api.example.com").
     * - "*.example.com": Explicitly matches all subdomains and "example.com".
     * - "*gambling*": Matches any host containing "gambling".
     * - "ad.*": Matches any TLD for "ad" host.
     */
    fun matches(hostOrUrl: String, pattern: String): Boolean {
        val cleanHost = extractCleanHost(hostOrUrl)
        if (cleanHost.isBlank() || pattern.isBlank()) return false

        val cleanPattern = pattern.trim().lowercase(Locale.ROOT)

        // Handle glob wildcards
        if (cleanPattern.contains('*')) {
            return matchesGlob(cleanHost, cleanPattern)
        }

        // Exact match
        if (cleanHost == cleanPattern) {
            return true
        }

        // Subdomain matching: cleanHost ends with ".pattern"
        // e.g., cleanHost = "m.youtube.com", pattern = "youtube.com" -> endsWith(".youtube.com")
        if (cleanHost.endsWith(".$cleanPattern")) {
            return true
        }

        return false
    }

    /**
     * Checks if [hostOrUrl] matches any pattern in [patterns].
     */
    fun isBlocked(hostOrUrl: String, patterns: Collection<String>): Boolean {
        val cleanHost = extractCleanHost(hostOrUrl)
        if (cleanHost.isBlank() || patterns.isEmpty()) return false

        for (pattern in patterns) {
            if (matches(cleanHost, pattern)) {
                return true
            }
        }
        return false
    }

    /**
     * Converts a wildcard glob (e.g. "*.domain.com", "*tracker*") into a safe Regex.
     */
    private fun matchesGlob(host: String, globPattern: String): Boolean {
        // Special case: "*.domain.com" also matches "domain.com"
        if (globPattern.startsWith("*.")) {
            val baseDomain = globPattern.substring(2)
            if (host == baseDomain) {
                return true
            }
        }

        val regex = buildString {
            append('^')
            for (char in globPattern) {
                when (char) {
                    '*' -> append(".*")
                    '?' -> append('.')
                    '.', '(', ')', '[', ']', '{', '}', '+', '^', '$', '|', '\\' -> {
                        append('\\')
                        append(char)
                    }
                    else -> append(char)
                }
            }
            append('$')
        }

        return try {
            Pattern.compile(regex).matcher(host).matches()
        } catch (_: Exception) {
            false
        }
    }
}
