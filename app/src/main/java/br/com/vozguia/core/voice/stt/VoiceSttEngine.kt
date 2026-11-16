package br.com.vozguia.core.voice.stt

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Contrato de reconhecimento de fala (STT Offline-First em pt-BR).
 */
interface VoiceSttEngine {
    fun startListening(
        isDictation: Boolean = false,
        onVoiceStart: () -> Unit,
        onVoiceEnd: () -> Unit,
        onResult: (String) -> Unit,
        onError: (Int) -> Unit,
        onPartialResult: (List<String>) -> Unit = {},
        onAllResults: (List<String>) -> Unit = {}
    )
    fun stopListening()
    fun destroy()
}

/**
 * Implementação nativa Android utilizando SpeechRecognizer On-Device (API 31+)
 * com fallback automático para SpeechRecognizer offline padrão do sistema em pt-BR.
 */
class AndroidOnDeviceSttEngine(
    private val context: Context
) : VoiceSttEngine {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null

    override fun startListening(
        isDictation: Boolean,
        onVoiceStart: () -> Unit,
        onVoiceEnd: () -> Unit,
        onResult: (String) -> Unit,
        onError: (Int) -> Unit,
        onPartialResult: (List<String>) -> Unit,
        onAllResults: (List<String>) -> Unit
    ) {
        mainHandler.post {
            try {
                recognizer?.destroy()
                recognizer = createBestRecognizer().apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {
                            onVoiceStart()
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            if (rmsdB > 2.5f) {
                                onVoiceStart()
                            }
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            onVoiceEnd()
                        }

                        override fun onError(error: Int) {
                            onError(error)
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
                            val bestMatch = matches.firstOrNull()?.trim() ?: ""
                            if (bestMatch.isNotBlank()) {
                                onAllResults(matches)
                                onResult(bestMatch)
                            } else {
                                onError(SpeechRecognizer.ERROR_NO_MATCH)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.filter { it.isNotBlank() }
                                ?: emptyList()
                            if (partials.isNotEmpty()) {
                                onPartialResult(partials)
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val silenceLength = if (isDictation) 1500L else 900L
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceLength)
                }

                recognizer?.startListening(intent)
            } catch (_: Exception) {
                onError(SpeechRecognizer.ERROR_CLIENT)
            }
        }
    }

    private fun createBestRecognizer(): SpeechRecognizer {
        val appContext = context.applicationContext
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
        } else {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                recognizer?.stopListening()
            } catch (_: Exception) {}
        }
    }

    override fun destroy() {
        mainHandler.post {
            try {
                recognizer?.destroy()
            } catch (_: Exception) {}
            recognizer = null
        }
    }
}
