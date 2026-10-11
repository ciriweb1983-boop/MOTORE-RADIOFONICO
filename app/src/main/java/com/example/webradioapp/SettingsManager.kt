package com.example.webradioapp

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("RadioPrefs", Context.MODE_PRIVATE)

    var password: String
        get() = prefs.getString("password", "1234") ?: "1234"
        set(value) = prefs.edit().putString("password", value).apply()

    var spotFrequencyTracks: Int
        get() = prefs.getInt("spotFrequency", 5)
        set(value) = prefs.edit().putInt("spotFrequency", value).apply()

    var newsIntervalMinutes: Int
        get() = prefs.getInt("newsIntervalMinutes", 60)
        set(value) = prefs.edit().putInt("newsIntervalMinutes", value).apply()

    fun getFolderName(key: String, defaultValue: String): String {
        return prefs.getString("folder_$key", defaultValue) ?: defaultValue
    }

    fun setFolderName(key: String, value: String) {
        prefs.edit().putString("folder_$key", value).apply()
    }

    var playedHistory: Map<String, Long>
        get() {
            // FIX: Passati drasticamente a JSONObject strutturato per prevenire la rottura catastrofica da string.split causata da virgole nei nomi file uri originali, causa per cui la memoria si resettava al riavvio dispositivo!
            val str = prefs.getString("playedHistoryJSON", "{}") ?: "{}"
            return try {
                val json = JSONObject(str)
                val map = mutableMapOf<String, Long>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = json.getLong(k)
                }
                map
            } catch (e: Exception) { emptyMap() }
        }
        set(value) {
            try {
                val json = JSONObject()
                value.forEach { (k, v) -> json.put(k, v) }
                prefs.edit().putString("playedHistoryJSON", json.toString()).apply()
            } catch (e: Exception) {}
        }
}
