package io.github.bayang.jelu.utils

import com.github.slugify.Slugify
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

private val whitelist: Safelist = Safelist.basic().addTags("h1", "h2", "h3", "h4", "h5", "h6")

private val slugifier: Slugify = Slugify()

fun sanitizeHtml(input: String?): String {
    if (input.isNullOrBlank()) {
        return ""
    }
    return Jsoup.clean(input, whitelist)
}

fun slugify(input: String): String = slugifier.slugify(input)
