package com.mohdrozaimi.glass

import com.mohdrozaimi.glass.data.Weather
import com.mohdrozaimi.glass.data.parseJakim
import com.mohdrozaimi.glass.data.parseOpenMeteo
import com.mohdrozaimi.glass.data.weatherLabel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ParsersTest {
    @Test fun openMeteo() = assertEquals(
        Weather(27, 3, "Berawan"),
        parseOpenMeteo("""{"current":{"temperature_2m":27.4,"weather_code":3}}"""),
    )

    @Test fun thunder() = assertEquals("Ribut petir", weatherLabel(95))

    @Test(expected = Exception::class)
    fun brokenJsonThrows() {
        parseOpenMeteo("{oops")
    }

    @Test fun jakim() {
        val json = """{"prayerTime":[{"hijri":"1448-05-30","date":"21-Nov-2026","day":"Saturday",
            "imsak":"05:41:00","fajr":"05:51:00","syuruk":"07:00:00","dhuhr":"13:02:00",
            "asr":"16:22:00","maghrib":"19:04:00","isha":"20:16:00"}],"status":"OK!","zone":"SGR01"}"""
        val days = parseJakim(json)
        assertEquals(1, days.size)
        assertEquals(LocalDate.of(2026, 11, 21), days[0].date)
        assertEquals(LocalTime.of(5, 51), days[0].times["Subuh"])
        assertEquals(LocalTime.of(20, 16), days[0].times["Isyak"])
        assertEquals(5, days[0].times.size)
    }
}
