package com.mohdrozaimi.glass.data

enum class ThemeId {
    LIQUID, HONOR;

    companion object {
        fun from(s: String?): ThemeId = entries.firstOrNull { it.name == s } ?: LIQUID
    }
}

enum class WidgetKind(val label: String) {
    CLOCK("Jam"),
    CALENDAR("Kalendar"),
    WEATHER("Cuaca"),
    MAP("Peta"),
    SEARCH("Carian Google"),
    PRAYER("Waktu Solat"),
    GREETING("Sapaan");

    companion object {
        fun from(s: String): WidgetKind? = entries.firstOrNull { it.name == s }
    }
}

/** Satu item di home screen. Kedudukan & saiz dalam unit petak grid. */
sealed class HomeItem {
    abstract val id: String
    abstract val x: Int
    abstract val y: Int
    abstract val w: Int
    abstract val h: Int

    abstract fun at(x: Int, y: Int): HomeItem
    abstract fun sized(w: Int, h: Int): HomeItem

    data class AppItem(
        override val id: String, override val x: Int, override val y: Int,
        override val w: Int, override val h: Int, val pkg: String,
    ) : HomeItem() {
        override fun at(x: Int, y: Int) = copy(x = x, y = y)
        override fun sized(w: Int, h: Int) = copy(w = w, h = h)
    }

    data class GlassWidget(
        override val id: String, override val x: Int, override val y: Int,
        override val w: Int, override val h: Int, val kind: WidgetKind,
    ) : HomeItem() {
        override fun at(x: Int, y: Int) = copy(x = x, y = y)
        override fun sized(w: Int, h: Int) = copy(w = w, h = h)
    }

    data class SystemWidget(
        override val id: String, override val x: Int, override val y: Int,
        override val w: Int, override val h: Int, val appWidgetId: Int, val provider: String,
    ) : HomeItem() {
        override fun at(x: Int, y: Int) = copy(x = x, y = y)
        override fun sized(w: Int, h: Int) = copy(w = w, h = h)
    }
}

object HomeGrid {
    const val COLS = 4
    const val ROWS = 6

    /** Saiz yang boleh dipilih (lebar x tinggi). */
    val SIZES: List<Pair<Int, Int>> = listOf(1 to 1, 2 to 1, 2 to 2, 4 to 1, 4 to 2, 4 to 3)

    fun fits(item: HomeItem): Boolean =
        item.x >= 0 && item.y >= 0 && item.w >= 1 && item.h >= 1 &&
            item.x + item.w <= COLS && item.y + item.h <= ROWS

    fun overlaps(a: HomeItem, b: HomeItem): Boolean =
        a.x < b.x + b.w && b.x < a.x + a.w && a.y < b.y + b.h && b.y < a.y + a.h

    fun canPlace(items: List<HomeItem>, item: HomeItem): Boolean =
        fits(item) && items.none { it.id != item.id && overlaps(it, item) }

    fun firstFree(items: List<HomeItem>, w: Int, h: Int): Pair<Int, Int>? {
        for (y in 0 until ROWS) for (x in 0 until COLS) {
            val probe = HomeItem.AppItem("\u0000probe", x, y, w, h, "")
            if (canPlace(items, probe)) return x to y
        }
        return null
    }

    fun add(items: List<HomeItem>, item: HomeItem): List<HomeItem> =
        if (items.none { it.id == item.id } && canPlace(items, item)) items + item else items

    private fun replace(items: List<HomeItem>, id: String, f: (HomeItem) -> HomeItem): List<HomeItem> {
        val old = items.firstOrNull { it.id == id } ?: return items
        val new = f(old)
        if (!canPlace(items, new)) return items
        return items.map { if (it.id == id) new else it }
    }

    fun move(items: List<HomeItem>, id: String, x: Int, y: Int) = replace(items, id) { it.at(x, y) }

    fun resize(items: List<HomeItem>, id: String, w: Int, h: Int) = replace(items, id) { it.sized(w, h) }

    fun remove(items: List<HomeItem>, id: String): List<HomeItem> = items.filterNot { it.id == id }

    fun replaceApp(items: List<HomeItem>, id: String, pkg: String): List<HomeItem> =
        items.map { if (it.id == id && it is HomeItem.AppItem) it.copy(pkg = pkg) else it }

    /** Saiz yang muat jika item ini diubah saiz (kekal di kedudukan sama). */
    fun sizesThatFit(items: List<HomeItem>, id: String): List<Pair<Int, Int>> {
        val item = items.firstOrNull { it.id == id } ?: return emptyList()
        return SIZES.filter { (w, h) -> canPlace(items, item.sized(w, h)) }
    }

    // ---- simpan / baca (format baris ringkas, tanpa JSON) ----
    fun encode(items: List<HomeItem>): String = items.joinToString("\n") { i ->
        val base = "${i.id}|${i.x}|${i.y}|${i.w}|${i.h}"
        when (i) {
            is HomeItem.AppItem -> "A|$base|${i.pkg}"
            is HomeItem.GlassWidget -> "G|$base|${i.kind.name}"
            is HomeItem.SystemWidget -> "S|$base|${i.appWidgetId}|${i.provider}"
        }
    }

    fun decode(text: String): List<HomeItem> = text.lines().mapNotNull { line ->
        val p = line.split("|")
        if (p.size < 7) return@mapNotNull null
        val id = p[1]
        val x = p[2].toIntOrNull() ?: return@mapNotNull null
        val y = p[3].toIntOrNull() ?: return@mapNotNull null
        val w = p[4].toIntOrNull() ?: return@mapNotNull null
        val h = p[5].toIntOrNull() ?: return@mapNotNull null
        when (p[0]) {
            "A" -> HomeItem.AppItem(id, x, y, w, h, p[6])
            "G" -> WidgetKind.from(p[6])?.let { HomeItem.GlassWidget(id, x, y, w, h, it) }
            "S" -> if (p.size >= 8) p[6].toIntOrNull()?.let { HomeItem.SystemWidget(id, x, y, w, h, it, p[7]) } else null
            else -> null
        }
    }

    fun defaultLayout(theme: ThemeId): List<HomeItem> = when (theme) {
        ThemeId.LIQUID -> listOf(
            HomeItem.GlassWidget("clock", 0, 0, 4, 2, WidgetKind.CLOCK),
            HomeItem.GlassWidget("calendar", 0, 2, 2, 2, WidgetKind.CALENDAR),
            HomeItem.GlassWidget("weather", 2, 2, 2, 2, WidgetKind.WEATHER),
            HomeItem.GlassWidget("map", 0, 4, 2, 2, WidgetKind.MAP),
            HomeItem.GlassWidget("search", 2, 4, 2, 1, WidgetKind.SEARCH),
            HomeItem.GlassWidget("prayer", 2, 5, 2, 1, WidgetKind.PRAYER),
        )
        ThemeId.HONOR -> listOf(
            HomeItem.GlassWidget("prayer", 0, 0, 4, 2, WidgetKind.PRAYER),
            HomeItem.GlassWidget("clock", 0, 2, 4, 1, WidgetKind.CLOCK),
            HomeItem.GlassWidget("search", 0, 5, 4, 1, WidgetKind.SEARCH),
        )
    }

    fun defaultSize(kind: WidgetKind): Pair<Int, Int> = when (kind) {
        WidgetKind.CLOCK -> 4 to 2
        WidgetKind.SEARCH -> 4 to 1
        WidgetKind.PRAYER -> 4 to 1
        WidgetKind.GREETING -> 4 to 1
        else -> 2 to 2
    }
}
