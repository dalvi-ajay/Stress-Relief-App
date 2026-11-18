package com.example.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

private const val TAG = "CalmVoiceController"

class CalmVoiceController(
    context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onSpeechError: (String) -> Unit,
    private val onSpeakingStateChanged: (Boolean) -> Unit,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var pendingUtterance: String? = null

    init {
        initTts()
    }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val tts = textToSpeech ?: return@TextToSpeech
                    val langResult = tts.setLanguage(Locale.getDefault())
                    if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                        langResult == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        tts.setLanguage(Locale.US)
                    }
                    tts.setSpeechRate(0.92f)
                    tts.setPitch(0.98f)
                    tts.setOnUtteranceProgressListener(
                        object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                mainHandler.post { onSpeakingStateChanged(true) }
                            }

                            override fun onDone(utteranceId: String?) {
                                mainHandler.post { onSpeakingStateChanged(false) }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                mainHandler.post { onSpeakingStateChanged(false) }
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                mainHandler.post { onSpeakingStateChanged(false) }
                            }
                        },
                    )
                    ttsReady = true
                    pendingUtterance?.let { queued ->
                        pendingUtterance = null
                        speak(queued)
                    }
                } else {
                    Log.w(TAG, "TextToSpeech initialization failed with status=$status")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TextToSpeech unavailable", e)
        }
    }

    fun isSpeechRecognitionAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(appContext)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query SpeechRecognizer availability", e)
            false
        }
    }

    fun startListening() {
        stopSpeaking()
        if (!isSpeechRecognitionAvailable()) {
            onSpeechError("On-device speech recognition is unavailable on this device. You can type below instead.")
            return
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                    setRecognitionListener(
                        object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {
                                onListeningStateChanged(true)
                            }

                            override fun onBeginningOfSpeech() = Unit
                            override fun onRmsChanged(rmsdB: Float) = Unit
                            override fun onBufferReceived(buffer: ByteArray?) = Unit

                            override fun onEndOfSpeech() {
                                onListeningStateChanged(false)
                            }

                            override fun onError(error: Int) {
                                onListeningStateChanged(false)
                                val message = mapSpeechError(error)
                                if (message != null) {
                                    onSpeechError(message)
                                }
                            }

                            override fun onResults(results: Bundle?) {
                                onListeningStateChanged(false)
                                val matches = results
                                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                val text = matches?.firstOrNull()?.trim().orEmpty()
                                if (text.isNotEmpty()) {
                                    onFinalResult(text)
                                } else {
                                    onSpeechError("Didn't catch anything clearly. Try speaking again or typing below.")
                                }
                            }

                            override fun onPartialResults(partialResults: Bundle?) {
                                val matches = partialResults
                                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                val partial = matches?.firstOrNull()?.trim().orEmpty()
                                if (partial.isNotEmpty()) {
                                    onPartialResult(partial)
                                }
                            }

                            override fun onEvent(eventType: Int, params: Bundle?) = Unit
                        },
                    )
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            onListeningStateChanged(true)
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            onListeningStateChanged(false)
            onSpeechError("Could not start microphone listening. Please try typing below.")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping SpeechRecognizer", e)
        }
        onListeningStateChanged(false)
    }

    fun speak(text: String) {
        val cleaned = text
            .replace("**", "")
            .replace("*", "")
            .replace("#", "")
            .trim()
        if (cleaned.isEmpty()) return

        if (!ttsReady) {
            pendingUtterance = cleaned
            return
        }
        try {
            val utteranceId = "calm_${System.currentTimeMillis()}"
            textToSpeech?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to speak response", e)
            onSpeakingStateChanged(false)
        }
    }

    fun stopSpeaking() {
        pendingUtterance = null
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop TextToSpeech", e)
        }
        onSpeakingStateChanged(false)
    }

    fun release() {
        stopListening()
        stopSpeaking()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying SpeechRecognizer", e)
        }
        speechRecognizer = null
        try {
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TextToSpeech", e)
        }
        textToSpeech = null
    }

    private fun mapSpeechError(errorCode: Int): String? {
        return when (errorCode) {
            SpeechRecognizer.ERROR_NO_MATCH ->
                "Didn't catch that. Tap the mic and try again, or type below."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "No speech heard. Tap the mic whenever you're ready."
            SpeechRecognizer.ERROR_AUDIO ->
                "Audio recording error. Check microphone access."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Microphone permission is required for voice input."
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Speech recognition network error. You can type your message below."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> null
            SpeechRecognizer.ERROR_CLIENT -> null
            else -> "Voice recognition unavailable right now. You can type below."
        }
    }
}
