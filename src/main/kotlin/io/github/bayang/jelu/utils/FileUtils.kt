package io.github.bayang.jelu.utils

fun imageName(title: String, bookId: String, extension: String): String {
    // Generate 8 character random string
    val cacheBuster = List(8) { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
    val imageName = "$title-$bookId.$cacheBuster.$extension"
    return imageName
}
