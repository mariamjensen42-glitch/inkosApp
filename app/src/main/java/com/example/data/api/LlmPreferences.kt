package com.example.data.api

import android.content.Context
import android.content.SharedPreferences

object LlmPreferences {
    private const val PREFS_NAME = "inkos_llm_preferences"
    
    private const val KEY_PROVIDER = "provider" // "GEMINI", "DEEPSEEK", or "XIAOMI_MIMO"
    private const val KEY_DEEPSEEK_KEY = "deepseek_key"
    private const val KEY_DEEPSEEK_BASE_URL = "deepseek_base_url"
    private const val KEY_DEEPSEEK_MODEL = "deepseek_model"
    private const val KEY_XIAOMI_MIMO_KEY = "xiaomi_mimo_key"
    private const val KEY_XIAOMI_MIMO_BASE_URL = "xiaomi_mimo_base_url"
    private const val KEY_XIAOMI_MIMO_MODEL = "xiaomi_mimo_model"
    private const val KEY_TEMPERATURE = "temperature"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var provider: String
        get() = prefs.getString(KEY_PROVIDER, "GEMINI") ?: "GEMINI"
        set(value) = prefs.edit().putString(KEY_PROVIDER, value).apply()

    var deepSeekKey: String
        get() = prefs.getString(KEY_DEEPSEEK_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEEPSEEK_KEY, value).apply()

    var deepSeekBaseUrl: String
        get() = prefs.getString(KEY_DEEPSEEK_BASE_URL, "https://api.deepseek.com") ?: "https://api.deepseek.com"
        set(value) = prefs.edit().putString(KEY_DEEPSEEK_BASE_URL, value).apply()

    var deepSeekModel: String
        get() = prefs.getString(KEY_DEEPSEEK_MODEL, "deepseek-v4-flash") ?: "deepseek-v4-flash"
        set(value) = prefs.edit().putString(KEY_DEEPSEEK_MODEL, value).apply()

    var xiaomiMimoKey: String
        get() = prefs.getString(KEY_XIAOMI_MIMO_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_XIAOMI_MIMO_KEY, value).apply()

    var xiaomiMimoBaseUrl: String
        get() = prefs.getString(KEY_XIAOMI_MIMO_BASE_URL, "https://api-ai.xiaomi.com/v1") ?: "https://api-ai.xiaomi.com/v1"
        set(value) = prefs.edit().putString(KEY_XIAOMI_MIMO_BASE_URL, value).apply()

    var xiaomiMimoModel: String
        get() = prefs.getString(KEY_XIAOMI_MIMO_MODEL, "mimo-v2-pro") ?: "mimo-v2-pro"
        set(value) = prefs.edit().putString(KEY_XIAOMI_MIMO_MODEL, value).apply()

    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_TEMPERATURE, value).apply()
}
