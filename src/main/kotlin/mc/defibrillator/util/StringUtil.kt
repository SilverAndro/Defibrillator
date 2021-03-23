package mc.defibrillator.util

fun String.cleanNumber(): String {
    val regex = Regex("^([\\-+]?[\\d\\.]+)\\w?")
    return regex.find(this)?.groupValues?.get(1) ?: "0"
}
