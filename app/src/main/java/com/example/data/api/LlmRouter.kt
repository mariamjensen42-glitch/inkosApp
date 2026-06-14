package com.example.data.api

object LlmRouter {
    val isApiKeyAvailable: Boolean
        get() = when (LlmPreferences.provider) {
            "DEEPSEEK" -> DeepSeekService.isApiKeyAvailable
            "XIAOMI_MIMO" -> XiaomiMimoService.isApiKeyAvailable
            else -> GeminiService.isApiKeyAvailable
        }

    val currentProviderLabel: String
        get() = when (LlmPreferences.provider) {
            "DEEPSEEK" -> "DeepSeek (${LlmPreferences.deepSeekModel})"
            "XIAOMI_MIMO" -> "Xiaomi MiMo (${LlmPreferences.xiaomiMimoModel})"
            else -> "Google Gemini (gemini-3.5-flash)"
        }

    suspend fun generateContent(systemInstructions: String, prompt: String, requireJson: Boolean = false): String {
        return when (LlmPreferences.provider) {
            "DEEPSEEK" -> DeepSeekService.generateContent(systemInstructions, prompt, requireJson)
            "XIAOMI_MIMO" -> XiaomiMimoService.generateContent(systemInstructions, prompt, requireJson)
            else -> GeminiService.generateContent(systemInstructions, prompt, requireJson)
        }
    }
}
