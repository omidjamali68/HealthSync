package com.example.healthsync.util

import androidx.appcompat.app.AppCompatDelegate
import java.time.Instant
import java.time.LocalDate
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
        val zdt = parseIsoToZonedDateTime(isoString) ?: return if (isPersian) isoString.toPersianDigits() else isoString
        val hasTime = isoString.contains("T")
        return formatZonedDateTime(zdt, isPersian, includeTime = hasTime)
    }

    fun parseIsoToZonedDateTime(isoString: String): ZonedDateTime? {
        return runCatching {
            ZonedDateTime.parse(isoString)
        }.getOrElse {
            runCatching {
                Instant.parse(isoString).atZone(ZoneId.systemDefault())
            }.getOrElse {
                runCatching {
                    OffsetDateTime.parse(isoString).toZonedDateTime()
                }.getOrElse {
                    runCatching {
                        LocalDate.parse(isoString).atStartOfDay(ZoneId.systemDefault())
                    }.getOrNull()
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
