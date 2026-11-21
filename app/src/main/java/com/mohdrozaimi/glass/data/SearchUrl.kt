package com.mohdrozaimi.glass.data

import java.net.URLEncoder

/** URL carian Google, atau null jika carian kosong. */
fun searchUrl(query: String): String? {
    val q = query.trim()
    if (q.isEmpty()) return null
    return "https://www.google.com/search?q=" + URLEncoder.encode(q, "UTF-8")
}
