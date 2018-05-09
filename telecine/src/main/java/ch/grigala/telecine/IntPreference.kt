package ch.grigala.telecine

import android.content.SharedPreferences

internal class IntPreference @JvmOverloads constructor(
        private val preferences: SharedPreferences,
        private val key: String,
        private val defaultValue: Int = 0) {

    val isSet: Boolean get() = preferences.contains(key)

    fun get(): Int {
        return preferences.getInt(key, defaultValue)
    }

    fun set(value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    fun delete() {
        preferences.edit().remove(key).apply()
    }
}