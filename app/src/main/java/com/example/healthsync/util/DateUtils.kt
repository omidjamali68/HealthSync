package com.example.healthsync.util

import androidx.appcompat.app.AppCompatDelegate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

object DateUtils {

    fun isPersianMode(): Boolean {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        return if (!appLocales.isEmpty) {
            appLocales[0]?.language == "fa"
        } else {
            Locale.getDefault().language == "fa"
        }
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): IntArray {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        val gy2 = gy - 1600
        val gm2 = gm - 1
        val gd2 = gd - 1

        var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
        for (i in 0 until gm2) {
            gDayNo += gDaysInMonth[i + 1]
        }
        if (gm2 > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo += 1
        }
        gDayNo += gd2

        var jDayNo = gDayNo - 79

        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        var jd = 0
        val daysInJMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        for (i in 0..11) {
            if (jDayNo < daysInJMonth[i]) {
                jm = i + 1
                jd = jDayNo + 1
                break
            }
            jDayNo -= daysInJMonth[i]
        }

        return intArrayOf(jy, jm, jd)
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): IntArray {
        val jy1 = jy - 979
        val jm1 = jm - 1
        val jd1 = jd - 1

        var jDayNo = 365 * jy1 + (jy1 / 33) * 8 + ((jy1 % 33) + 3) / 4
        val daysInJMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        for (i in 0 until jm1) {
            jDayNo += daysInJMonth[i]
        }
        jDayNo += jd1

        var gDayNo = jDayNo + 79

        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524
            if (gDayNo >= 365) {
                gDayNo++
            } else {
                leap = false
            }
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        val gDaysInMonth = intArrayOf(0, 31, if (leap && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 12 && gDayNo >= gDaysInMonth[gm + 1]) {
            gDayNo -= gDaysInMonth[gm + 1]
            gm++
        }
        val gd = gDayNo + 1
        return intArrayOf(gy, gm + 1, gd)
    }

    fun convertJalaliStringToGregorian(dateStr: String): String {
        val clean = dateStr.trim().toEnglishDigits()
        if (clean.isBlank()) return ""
        val parts = clean.split("/", "-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull() ?: return dateStr
            val m = parts[1].toIntOrNull() ?: return dateStr
            val d = parts[2].toIntOrNull() ?: return dateStr
            if (y in 1300..1500 && m in 1..12 && d in 1..31) {
                val (gy, gm, gd) = jalaliToGregorian(y, m, d)
                return String.format(Locale.ROOT, "%04d-%02d-%02d", gy, gm, gd)
            }
        }
        return dateStr
    }

    fun String.toEnglishDigits(): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = this
        for (i in 0..9) {
            result = result.replace(persianDigits[i], ('0' + i)).replace(arabicDigits[i], ('0' + i))
        }
        return result
    }

    fun String.toPersianDigits(): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (ch in this) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatDateTime(timestamp: Long, isPersian: Boolean = isPersianMode()): String {
        val zdt = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        return formatZonedDateTime(zdt, isPersian)
    }

    fun formatIsoDateTime(isoString: String, isPersian: Boolean = isPersianMode()): String {
        if (isoString.isBlank()) return ""
        val zdt = parseIsoToZonedDateTime(isoString) ?: return if (isPersian) isoString.toPersianDigits() else isoString
        val hasTime = isoString.contains("T") || isoString.contains(" ")
        return formatZonedDateTime(zdt, isPersian, includeTime = hasTime)
    }

    fun parseIsoToZonedDateTime(isoString: String): ZonedDateTime? {
        val clean = isoString.trim()
        if (clean.isBlank()) return null
        val normalized = clean.replace(" ", "T").replace("/", "-")
        return runCatching {
            if (normalized.endsWith("Z", ignoreCase = true) ||
                normalized.substringAfter("T", "").contains("+") ||
                normalized.substringAfter("T", "").contains("-")) {
                ZonedDateTime.parse(normalized).withZoneSameInstant(ZoneId.systemDefault())
            } else {
                LocalDateTime.parse(normalized).atZone(ZoneId.systemDefault())
            }
        }.getOrElse {
            runCatching {
                Instant.parse(normalized).atZone(ZoneId.systemDefault())
            }.getOrElse {
                runCatching {
                    OffsetDateTime.parse(normalized).toZonedDateTime().withZoneSameInstant(ZoneId.systemDefault())
                }.getOrElse {
                    runCatching {
                        LocalDateTime.parse(normalized).atZone(ZoneId.systemDefault())
                    }.getOrElse {
                        runCatching {
                            LocalDate.parse(normalized).atStartOfDay(ZoneId.systemDefault())
                        }.getOrNull()
                    }
                }
            }
        }
    }

    fun formatZonedDateTime(zdt: ZonedDateTime, isPersian: Boolean = isPersianMode(), includeTime: Boolean = true): String {
        return if (isPersian) {
            val (jy, jm, jd) = gregorianToJalali(zdt.year, zdt.monthValue, zdt.dayOfMonth)
            val dateStr = String.format(Locale.ROOT, "%04d/%02d/%02d", jy, jm, jd)
            val formatted = if (includeTime) {
                val timeStr = String.format(Locale.ROOT, "%02d:%02d", zdt.hour, zdt.minute)
                "$dateStr $timeStr"
            } else {
                dateStr
            }
            formatted.toPersianDigits()
        } else {
            val dateStr = String.format(Locale.ROOT, "%04d/%02d/%02d", zdt.year, zdt.monthValue, zdt.dayOfMonth)
            if (includeTime) {
                val timeStr = String.format(Locale.ROOT, "%02d:%02d", zdt.hour, zdt.minute)
                "$dateStr $timeStr"
            } else {
                dateStr
            }
        }
    }
}
