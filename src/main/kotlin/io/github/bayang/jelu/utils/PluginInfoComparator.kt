package io.github.bayang.jelu.utils

import io.github.bayang.jelu.dto.PluginInfo

class PluginInfoComparator {

    companion object : Comparator<PluginInfo> {
        override fun compare(a: PluginInfo, b: PluginInfo): Int = when {
            a.order != b.order -> b.order - a.order
            else -> a.name.compareTo(b.name)
        }
    }
}
