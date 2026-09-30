package com.braineer.nuresult

import com.braineer.nuresult.model.ResultLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ResultLinksTest {

    @Test
    fun sameServer_ignoresSchemeWwwCaseAndTrailingSlash() {
        assertTrue(ResultLinks.sameServer("https://www.educationboardresults.gov.bd/", "http://educationboardresults.gov.bd"))
        assertTrue(ResultLinks.sameServer("http://103.230.104.203/", "http://103.230.104.203"))
        assertTrue(ResultLinks.sameServer("https://EboardResults.com/v2/home/", "https://eboardresults.com/v2/home"))
    }

    @Test
    fun sameServer_keepsDifferentPathsQueriesAndHostsApart() {
        assertFalse(ResultLinks.sameServer("https://eboardresults.com/v2/home", "https://eboardresults.com/"))
        assertFalse(ResultLinks.sameServer("https://eboardresults.com/v2/home", "https://eboardresults.com/v2/home?lang=bn"))
        assertFalse(ResultLinks.sameServer("http://103.230.104.203/", "http://103.230.104.222/"))
        assertFalse(ResultLinks.sameServer("http://103.230.104.203:8080/", "http://103.230.104.203/"))
    }

    @Test
    fun dedupe_keepsFirstOccurrenceInOrder() {
        val input = listOf(
            "https://www.educationboardresults.gov.bd/",
            "http://103.230.104.203/",
            "http://educationboardresults.gov.bd",
            "not a url",
            "http://103.230.104.203",
            "https://eboardresults.com/v2/home",
        )
        assertEquals(
            listOf(
                "https://www.educationboardresults.gov.bd/",
                "http://103.230.104.203/",
                "https://eboardresults.com/v2/home",
            ),
            ResultLinks.dedupe(input)
        )
    }

    @Test
    fun shippedConfig_hasNoDuplicateServers() {
        // Guards config/result_links.json and the bundled copy against accidental duplicates
        listOf("../config/result_links.json", "src/main/res/raw/result_links.json").forEach { path ->
            val exams = org.json.JSONObject(File(path).readText()).getJSONObject("exams")
            exams.keys().forEach { exam ->
                val array = exams.getJSONArray(exam)
                val urls = (0 until array.length()).map { array.getString(it) }
                assertEquals("Duplicate server in $path → $exam", urls, ResultLinks.dedupe(urls))
            }
        }
    }
}
