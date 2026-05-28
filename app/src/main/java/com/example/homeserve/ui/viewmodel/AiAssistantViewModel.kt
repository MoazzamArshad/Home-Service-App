package com.example.homeserve.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeserve.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class AiAssistantViewModel : ViewModel() {

    // Initialize the Gemini API client safely using BuildConfig field
    private val model: GenerativeModel? by lazy {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isNotBlank()) {
                GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = apiKey,
                    systemInstruction = content {
                        text(
                            "You are 'HomeServe AI', a friendly and expert home maintenance diagnostic assistant. " +
                            "Your goal is to help customers diagnose their home issues (plumbing, electrical, appliances, painting, cleaning, beauty) " +
                            "and recommend the correct service category to book. " +
                            "Keep your answers friendly, extremely concise (2 to 3 sentences maximum), and offer a clear recommendation. " +
                            "If you recommend a category, you MUST ALWAYS append '[RECOMMEND: <category_id>]' at the very end of your response (where <category_id> is one of: electrician, plumber, cleaning, appliance, beauty, painting). " +
                            "For example: if they have a leaking pipe, write your diagnosis, then append '[RECOMMEND: plumber]'."
                        )
                    }
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val _messages = MutableStateFlow<List<AiMessage>>(listOf(
        AiMessage("Hello! I am HomeServe AI. 🛠️ What home issue can I help you diagnose today?", isUser = false)
    ))
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // Append user message
        val userMsg = AiMessage(text, isUser = true)
        _messages.value = _messages.value + userMsg

        viewModelScope.launch {
            _isLoading.value = true
            val aiResponse = try {
                val currentModel = model
                if (currentModel != null) {
                    val response = currentModel.generateContent(text)
                    response.text ?: "I'm having trouble diagnosing that. Let's connect you directly with a technician!"
                } else {
                    "API Key is missing. Please set your Gemini API key in local.properties as detailed in the setup guide!"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "I had trouble connecting. Please check your internet connection and verify your API key."
            }
            
            // Append AI response
            _messages.value = _messages.value + AiMessage(aiResponse, isUser = false)
            _isLoading.value = false
        }
    }
}
