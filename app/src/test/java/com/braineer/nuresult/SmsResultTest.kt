package com.braineer.nuresult

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsResultTest {

    @Test
    fun message_usesTeletalkFormat() {
        assertEquals("HSC DHA 123456 2026", SmsResult.message("HSC", "DHA", " 123456 ", "2026"))
        assertEquals("SSC TEC 654321 2026", SmsResult.message("SSC", "TEC", "654321", "2026"))
    }

    @Test
    fun message_sendsAlimAndDakhilToMadrasahBoard() {
        assertEquals("Alim MAD 111222 2026", SmsResult.message("Alim", "DHA", "111222", "2026"))
        assertEquals("Dakhil MAD 333444 2026", SmsResult.message("Dakhil", "CHI", "333444", "2026"))
    }
}
