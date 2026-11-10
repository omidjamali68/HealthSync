package com.example.healthsync.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateFormatTest {

    @Test
    fun testMarch30_2026_JalaliConversion() {
        val (jy, jm, jd) = DateUtils.gregorianToJalali(2026, 3, 30)
        println("2026-03-30 -> $jy/$jm/$jd")
        assertEquals(1405, jy)
        assertEquals(1, jm)
        assertEquals(10, jd)
    }

    @Test
    fun testMarch21_2026_JalaliConversion() {
        val (jy, jm, jd) = DateUtils.gregorianToJalali(2026, 3, 21)
        println("2026-03-21 -> $jy/$jm/$jd")
        assertEquals(1405, jy)
        assertEquals(1, jm)
        assertEquals(1, jd)
    }

    @Test
    fun testMarch20_2026_JalaliConversion() {
        val (jy, jm, jd) = DateUtils.gregorianToJalali(2026, 3, 20)
        println("2026-03-20 -> $jy/$jm/$jd")
        assertEquals(1404, jy)
        assertEquals(12, jm)
        assertEquals(29, jd)
    }

    @Test
    fun testJan1_2024_JalaliConversion() {
        val (jy, jm, jd) = DateUtils.gregorianToJalali(2024, 1, 1)
        println("2024-01-01 -> $jy/$jm/$jd")
        assertEquals(1402, jy)
        assertEquals(10, jm)
        assertEquals(11, jd)
    }

    @Test
    fun testIsoFormattingPersian() {
        val iso = "2026-03-30T14:30:00Z"
        val formatted = DateUtils.formatIsoDateTime(iso, isPersian = true)
        println("ISO Persian: $formatted")
        assertTrue(formatted.startsWith("۱۴۰۵/۰۱/۱۰"))
    }

    @Test
    fun testIsoFormattingEnglish() {
        val iso = "2026-03-30T14:30:00Z"
        val formatted = DateUtils.formatIsoDateTime(iso, isPersian = false)
        println("ISO English: $formatted")
        assertTrue(formatted.startsWith("2026/03/30"))
    }
}
