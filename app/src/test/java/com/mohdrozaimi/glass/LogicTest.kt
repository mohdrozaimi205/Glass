package com.mohdrozaimi.glass

import com.mohdrozaimi.glass.data.DockList
import com.mohdrozaimi.glass.data.HomeGrid
import com.mohdrozaimi.glass.data.HomeItem
import com.mohdrozaimi.glass.data.PrayerDay
import com.mohdrozaimi.glass.data.PrayerZones
import com.mohdrozaimi.glass.data.ThemeId
import com.mohdrozaimi.glass.data.WidgetKind
import com.mohdrozaimi.glass.data.formatCountdown
import com.mohdrozaimi.glass.data.formatDateBM
import com.mohdrozaimi.glass.data.nextPrayer
import com.mohdrozaimi.glass.data.searchUrl
import com.mohdrozaimi.glass.data.tileUrl
import com.mohdrozaimi.glass.glass.PerfMode
import com.mohdrozaimi.glass.notif.countBadges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PerfModeTest {
    @Test fun lowMemoryIsLite() = assertTrue(PerfMode.shouldLite(6L * PerfMode.GB, false))
    @Test fun highMemoryIsFull() = assertFalse(PerfMode.shouldLite(12L * PerfMode.GB, false))
    @Test fun lowRamFlagIsLite() = assertTrue(PerfMode.shouldLite(12L * PerfMode.GB, true))
    @Test fun overrideWins() {
        assertTrue(PerfMode.resolve("on", false))
        assertFalse(PerfMode.resolve("off", true))
        assertTrue(PerfMode.resolve("auto", true))
    }
}

class DockListTest {
    private val five = listOf("a", "b", "c", "d", "e")
    @Test fun addToFive() = assertEquals(6, DockList.add(five, "f").size)
    @Test fun seventhRejected() {
        val six = five + "f"
        assertEquals(six, DockList.add(six, "g"))
    }
    @Test fun duplicateRejected() = assertEquals(five, DockList.add(five, "a"))
    @Test fun removeWorks() = assertFalse("c" in DockList.remove(five, "c"))
    @Test fun roundTrip() = assertEquals(five, DockList.decode(DockList.encode(five)))
}

class HomeGridTest {
    private val a = HomeItem.AppItem("a", 0, 0, 1, 1, "pkg.a")
    private val w = HomeItem.GlassWidget("w", 0, 1, 2, 2, WidgetKind.WEATHER)
    private val items = listOf(a, w)

    @Test fun overlapRejected() = assertFalse(HomeGrid.canPlace(items, a.at(1, 1)))
    @Test fun outsideRejected() = assertFalse(HomeGrid.canPlace(items, a.at(3, 0).sized(2, 1)))
    @Test fun freeSpotAccepted() = assertTrue(HomeGrid.canPlace(items, a.at(3, 5)))

    @Test fun firstFreeOnFullGrid() {
        val full = (0 until HomeGrid.ROWS).flatMap { y ->
            (0 until HomeGrid.COLS).map { x -> HomeItem.AppItem("$x-$y", x, y, 1, 1, "p") }
        }
        assertNull(HomeGrid.firstFree(full, 1, 1))
    }

    @Test fun firstFreeFindsSpot() = assertEquals(1 to 0, HomeGrid.firstFree(items, 1, 1))

    @Test fun moveOntoOccupiedUnchanged() = assertEquals(items, HomeGrid.move(items, "a", 1, 2))
    @Test fun moveToFreeWorks() {
        val moved = HomeGrid.move(items, "a", 3, 3)
        assertEquals(3, moved.first { it.id == "a" }.x)
    }

    @Test fun resizeWhenFree() {
        val r = HomeGrid.resize(items, "w", 4, 2)
        assertEquals(4, r.first { it.id == "w" }.w)
    }

    @Test fun resizeBlocked() {
        val blocker = HomeItem.AppItem("b", 3, 2, 1, 1, "pkg.b")
        val withBlocker = items + blocker
        assertEquals(withBlocker, HomeGrid.resize(withBlocker, "w", 4, 2))
    }

    @Test fun replaceAppKeepsPosition() {
        val r = HomeGrid.replaceApp(items, "a", "pkg.z").first { it.id == "a" } as HomeItem.AppItem
        assertEquals("pkg.z", r.pkg)
        assertEquals(0, r.x)
        assertEquals(0, r.y)
    }

    @Test fun encodeDecodeRoundTrip() {
        val s = HomeItem.SystemWidget("s", 0, 3, 4, 2, 42, "com.x/com.x.Widget")
        val all = items + s
        assertEquals(all, HomeGrid.decode(HomeGrid.encode(all)))
    }

    @Test fun emptyDecodesEmpty() = assertTrue(HomeGrid.decode("").isEmpty())

    @Test fun defaultLayoutsValid() {
        for (t in ThemeId.entries) {
            val l = HomeGrid.defaultLayout(t)
            for (i in l) assertTrue("$t ${i.id}", HomeGrid.canPlace(l, i))
        }
    }
}

class PrayerTest {
    private val today = LocalDate.of(2026, 11, 21)
    private fun day(d: LocalDate) = PrayerDay(
        d,
        mapOf(
            "Subuh" to LocalTime.of(5, 50), "Zohor" to LocalTime.of(13, 5),
            "Asar" to LocalTime.of(16, 20), "Maghrib" to LocalTime.of(19, 5),
            "Isyak" to LocalTime.of(20, 15),
        ),
    )
    private val days = listOf(day(today), day(today.plusDays(1)))

    @Test fun afternoonIsAsar() {
        val n = nextPrayer(days, LocalDateTime.of(today, LocalTime.of(13, 0)))!!
        assertEquals("Zohor", n.first)
        val m = nextPrayer(days, LocalDateTime.of(today, LocalTime.of(14, 0)))!!
        assertEquals("Asar", m.first)
    }

    @Test fun afterIsyakIsSubuhTomorrow() {
        val n = nextPrayer(days, LocalDateTime.of(today, LocalTime.of(21, 30)))!!
        assertEquals("Subuh", n.first)
        assertEquals(today.plusDays(1), n.second.toLocalDate())
    }

    @Test fun noDataIsNull() = assertNull(nextPrayer(emptyList(), LocalDateTime.of(today, LocalTime.NOON)))

    @Test fun zonesPresent() {
        val codes = PrayerZones.all.map { it.first }
        assertTrue("SGR01" in codes)
        assertTrue("WLY01" in codes)
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test fun countdown() {
        assertEquals("1j 20m", formatCountdown(80))
        assertEquals("45m", formatCountdown(45))
    }

    @Test fun dateBM() = assertEquals("Sabtu, 21 November", formatDateBM(today))
}

class SmallUtilsTest {
    @Test fun blankSearchIsNull() = assertNull(searchUrl("   "))
    @Test fun searchEncodesAmp() =
        assertEquals("https://www.google.com/search?q=nasi+lemak+%26+teh", searchUrl("nasi lemak & teh"))
    @Test fun searchEncodesHash() = assertEquals("https://www.google.com/search?q=%231+cuaca", searchUrl("#1 cuaca"))
    @Test fun tileKl() = assertEquals("https://tile.openstreetmap.org/15/25639/16098.png", tileUrl(3.139, 101.687, 15))
    @Test fun badgesIgnoreOngoing() =
        assertEquals(mapOf("wa" to 2), countBadges(listOf("wa" to false, "wa" to false, "music" to true)))
}

class MotionTest {
    private val m = com.mohdrozaimi.glass.data.MotionPrefs()
    private val mo = com.mohdrozaimi.glass.data.Motion

    @Test fun normalSpeedKeepsDuration() = assertEquals(500, mo.scale(500, 1f))
    @Test fun doubleSpeedHalvesDuration() = assertEquals(250, mo.scale(500, 2f))
    @Test fun halfSpeedDoublesDuration() = assertEquals(1000, mo.scale(500, 0.5f))
    @Test fun speedIsClamped() {
        assertEquals(250, mo.scale(500, 10f))
        assertEquals(1000, mo.scale(500, 0.01f))
        assertEquals(500, mo.scale(500, Float.NaN))
    }
    @Test fun durationNeverZero() = assertEquals(1, mo.scale(0, 1f))
    @Test fun masterSwitchTurnsEverythingOff() {
        val off = m.copy(enabled = false)
        assertFalse(off.entranceOn || off.wavesOn || off.searchingOn || off.typingOn || off.pressOn)
    }
    @Test fun singleToggleOnlyAffectsItself() {
        val noWaves = m.copy(waves = false)
        assertFalse(noWaves.wavesOn)
        assertTrue(noWaves.entranceOn && noWaves.pressOn)
    }
    @Test fun labels() {
        assertEquals("1x", mo.speedLabel(1f))
        assertEquals("0.5x", mo.speedLabel(0.5f))
        assertEquals("0.75x", mo.speedLabel(0.75f))
        assertEquals("1.5x", mo.speedLabel(1.5f))
        assertEquals("2x", mo.speedLabel(2f))
    }
    @Test fun greetingByHour() {
        assertEquals("Selamat pagi", mo.salam(7))
        assertEquals("Selamat tengah hari", mo.salam(12))
        assertEquals("Selamat petang", mo.salam(16))
        assertEquals("Selamat malam", mo.salam(21))
        assertEquals("Selamat malam", mo.salam(3))
    }
    @Test fun greetingWidgetSize() =
        assertEquals(4 to 1, HomeGrid.defaultSize(WidgetKind.GREETING))
}

class UpdateTest {
    private val u = com.mohdrozaimi.glass.data.Update

    @Test fun newerPatch() = assertTrue(u.isNewer("1.1.7", "1.1.4"))
    @Test fun newerMinor() = assertTrue(u.isNewer("1.2.0", "1.1.9"))
    @Test fun sameIsNotNewer() = assertFalse(u.isNewer("1.1.4", "1.1.4"))
    @Test fun olderIsNotNewer() = assertFalse(u.isNewer("1.1.3", "1.1.4"))
    @Test fun numericNotText() = assertTrue(u.isNewer("1.1.10", "1.1.9"))
    @Test fun oldVersionNameWithoutBuild() = assertTrue(u.isNewer("1.1.5", "1.1"))
    @Test fun tagPrefixIgnored() = assertTrue(u.isNewer("v1.1.5", "1.1.4"))

    @Test fun parsesRelease() {
        val json = """{"tag_name":"v1.1.6","body":"Animasi baru","assets":[
            {"name":"notes.txt","browser_download_url":"x","size":1},
            {"name":"Glass.apk","browser_download_url":"https://e.x/Glass.apk","size":24457949}]}"""
        val r = u.parse(json)!!
        assertEquals("1.1.6", r.version)
        assertEquals("https://e.x/Glass.apk", r.apkUrl)
        assertEquals(24457949L, r.sizeBytes)
        assertEquals("Animasi baru", r.notes)
    }
    @Test fun noApkGivesNull() = assertNull(u.parse("""{"tag_name":"v1.1.6","assets":[]}"""))
    @Test fun sizeLabel() = assertEquals("23.3 MB", u.sizeLabel(24457949L))
}

class IconSizeTest {
    private val i = com.mohdrozaimi.glass.data.IconSize
    @Test fun sizesGrow() = assertTrue(i.dp("S") < i.dp("M") && i.dp("M") < i.dp("L"))
    @Test fun unknownIsMedium() = assertEquals(i.dp("M"), i.dp("??"))
}
