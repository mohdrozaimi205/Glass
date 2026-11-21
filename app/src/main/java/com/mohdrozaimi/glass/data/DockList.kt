package com.mohdrozaimi.glass.data

/** Apps di dok bawah (maksimum 6). */
object DockList {
    const val MAX = 6

    fun add(list: List<String>, pkg: String): List<String> =
        if (list.size >= MAX || pkg in list) list else list + pkg

    fun remove(list: List<String>, pkg: String): List<String> = list - pkg

    fun encode(list: List<String>): String = list.joinToString("|")

    fun decode(text: String?): List<String> =
        text?.split("|")?.filter { it.isNotBlank() }?.take(MAX) ?: emptyList()
}
