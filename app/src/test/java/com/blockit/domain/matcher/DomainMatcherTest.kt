package com.blockit.domain.matcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainMatcherTest {

    @Test
    fun extractCleanHost_stripsProtocolsPathsQueriesAndPorts() {
        assertEquals("news.ycombinator.com", DomainMatcher.extractCleanHost("https://news.ycombinator.com/item?id=123#top"))
        assertEquals("instagram.com", DomainMatcher.extractCleanHost("http://instagram.com:8080/explore"))
        assertEquals("reddit.com", DomainMatcher.extractCleanHost("reddit.com."))
        assertEquals("facebook.com", DomainMatcher.extractCleanHost("  HTTPS://FACEBOOK.COM/user/login  "))
        assertEquals("facebook.com", DomainMatcher.extractCleanHost("https://www.facebook.com/user/login"))
        assertEquals("youtube.com", DomainMatcher.extractCleanHost("www.youtube.com/watch?v=123"))
        assertEquals("tiktok.com", DomainMatcher.extractCleanHost("tiktok.com/tag/viral?sort=new"))
    }

    @Test
    fun matches_exactDomainMatches() {
        assertTrue(DomainMatcher.matches("instagram.com", "instagram.com"))
        assertTrue(DomainMatcher.matches("https://instagram.com", "instagram.com"))
        assertTrue(DomainMatcher.matches("INSTAGRAM.COM", "instagram.com"))
        assertFalse(DomainMatcher.matches("notinstagram.com", "instagram.com"))
    }

    @Test
    fun matches_subdomainCascade() {
        // Base domain rule automatically cascades to subdomains
        assertTrue(DomainMatcher.matches("m.youtube.com", "youtube.com"))
        assertTrue(DomainMatcher.matches("music.youtube.com", "youtube.com"))
        assertTrue(DomainMatcher.matches("video.cdn.youtube.com", "youtube.com"))
        assertTrue(DomainMatcher.matches("https://www.youtube.com/watch?v=123", "youtube.com"))

        // Does NOT match domains that just contain the word
        assertFalse(DomainMatcher.matches("fakeyoutube.com", "youtube.com"))
        assertFalse(DomainMatcher.matches("youtube.com.scam.net", "youtube.com"))
    }

    @Test
    fun matches_wildcardSubdomainPattern() {
        assertTrue(DomainMatcher.matches("twitter.com", "*.twitter.com"))
        assertTrue(DomainMatcher.matches("mobile.twitter.com", "*.twitter.com"))
        assertTrue(DomainMatcher.matches("api.v2.twitter.com", "*.twitter.com"))
        assertFalse(DomainMatcher.matches("twitters.com", "*.twitter.com"))
    }

    @Test
    fun matches_generalWildcards() {
        // Keyword substring wildcard
        assertTrue(DomainMatcher.matches("casinogames.com", "*casino*"))
        assertTrue(DomainMatcher.matches("online-casino.co.uk", "*casino*"))
        assertFalse(DomainMatcher.matches("boardgames.com", "*casino*"))

        // TLD wildcard
        assertTrue(DomainMatcher.matches("reddit.com", "reddit.*"))
        assertTrue(DomainMatcher.matches("reddit.co.uk", "reddit.*"))
        assertTrue(DomainMatcher.matches("reddit.io", "reddit.*"))
        assertFalse(DomainMatcher.matches("notreddit.com", "reddit.*"))
    }

    @Test
    fun isBlocked_matchesAnyInCollection() {
        val blockedList = listOf("tiktok.com", "*.instagram.com", "*gambling*")

        assertTrue(DomainMatcher.isBlocked("https://www.tiktok.com/@user", blockedList))
        assertTrue(DomainMatcher.isBlocked("reels.instagram.com", blockedList))
        assertTrue(DomainMatcher.isBlocked("super-gambling-hub.org", blockedList))
        assertFalse(DomainMatcher.isBlocked("wikipedia.org", blockedList))
        assertFalse(DomainMatcher.isBlocked("github.com", blockedList))
    }

    @Test
    fun emptyAndBlankInputs_handledSafely() {
        assertFalse(DomainMatcher.matches("", "example.com"))
        assertFalse(DomainMatcher.matches("example.com", ""))
        assertFalse(DomainMatcher.matches("   ", "   "))
        assertFalse(DomainMatcher.isBlocked("example.com", emptyList()))
    }
}
