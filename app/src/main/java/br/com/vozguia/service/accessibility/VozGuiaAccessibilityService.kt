package br.com.vozguia.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import br.com.vozguia.core.accessibility.action.ActionDispatcher
import br.com.vozguia.core.accessibility.matcher.SemanticNodeMatcher
import br.com.vozguia.core.accessibility.parser.AndroidAccessibilityNode
import br.com.vozguia.core.accessibility.parser.SemanticScreenTreeParser
import br.com.vozguia.core.accessibility.trigger.PhysicalTriggerInterceptor
import br.com.vozguia.core.accessibility.trigger.TriggerAction
import br.com.vozguia.core.actions.phone.DialResult
import br.com.vozguia.core.actions.phone.FamilyContact
import br.com.vozguia.core.actions.phone.PhoneAndVoipCallModule
import br.com.vozguia.core.actions.whatsapp.WhatsAppCopilotModule
import br.com.vozguia.core.actions.whatsapp.WhatsAppNotificationHolder
import br.com.vozguia.core.actions.youtube.YouTubeRadioModule
import br.com.vozguia.core.domain.AgentState
import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.feedback.AndroidHapticAudioFeedbackEngine
import br.com.vozguia.core.feedback.FeedbackEvent
import br.com.vozguia.core.feedback.HapticAudioFeedbackEngine
import br.com.vozguia.core.data.repository.EncryptedPrefsQuotaStorage
import br.com.vozguia.core.data.repository.PersistentQuotaRepository
import br.com.vozguia.core.guardrails.GuardrailPipeline
import br.com.vozguia.core.guardrails.GuardrailVerdict
import br.com.vozguia.core.guardrails.VozGuiaGuardrailFactory
import br.com.vozguia.core.voice.audio.AndroidAudioFocusHandler
import br.com.vozguia.core.voice.audio.AudioFocusCoordinator
import br.com.vozguia.core.voice.dialogue.DialogueResult
import br.com.vozguia.core.voice.dialogue.DialogueStateManager
import br.com.vozguia.core.voice.router.HybridIntentRouter
import br.com.vozguia.core.voice.stt.AndroidOnDeviceSttEngine
import br.com.vozguia.core.voice.stt.AntiEchoCoordinator
import br.com.vozguia.core.voice.stt.VadController
import br.com.vozguia.core.voice.stt.VoiceSttEngine
import br.com.vozguia.core.voice.tts.AndroidTextToSpeechSpeaker
import br.com.vozguia.core.voice.tts.HumanVoiceTtsEngine
import br.com.vozguia.VozGuiaApplication
import br.com.vozguia.core.caregiver.BatteryAndWatchdogMonitor
import br.com.vozguia.core.caregiver.EmergencySosDispatcher
import br.com.vozguia.core.caregiver.EmergencySosResult
import br.com.vozguia.core.routine.briefing.MorningBriefingOrchestrator
import br.com.vozguia.core.routine.media.RoutineHabitLearner
import br.com.vozguia.core.routine.phonetic.AliasLearningState
import br.com.vozguia.core.routine.phonetic.ConversationalAliasLearner
import br.com.vozguia.core.routine.phonetic.PhoneticAliasResolver
import br.com.vozguia.core.routine.phonetic.PhoneticMatchResult
import br.com.vozguia.core.routine.utilities.DailyAutonomyUtilities
import br.com.vozguia.core.routine.utilities.MedicineReminderController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Calendar

class VozGuiaAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val feedbackEngine: HapticAudioFeedbackEngine by lazy {
        AndroidHapticAudioFeedbackEngine(this, serviceScope)
    }

    private val screenParser = SemanticScreenTreeParser()
    val actionDispatcher = ActionDispatcher()
    val semanticNodeMatcher = SemanticNodeMatcher()

    // Épico 2: Motor de Áudio, Voz e Inteligência
    private val ttsSpeaker by lazy { AndroidTextToSpeechSpeaker(this) }

    val ttsEngine: HumanVoiceTtsEngine by lazy {
        HumanVoiceTtsEngine(
            speaker = ttsSpeaker
        )
    }

    val sttEngine: VoiceSttEngine by lazy {
        AndroidOnDeviceSttEngine(this)
    }

    val audioFocusCoordinator by lazy {
        AudioFocusCoordinator(AndroidAudioFocusHandler(this))
    }

    val vadController by lazy {
        VadController(serviceScope).apply {
            onSpeechCompleted = { onUserSpeechFinished() }
            onNoSpeechTimeout = { onSilenceTimeout() }
        }
    }

    val antiEchoCoordinator by lazy {
        AntiEchoCoordinator(serviceScope)
    }

    val intentRouter = HybridIntentRouter()
    val dialogueStateManager = DialogueStateManager()

    // Épico 3: Módulos de Ação Autônoma
    val whatsAppCopilot by lazy {
        WhatsAppCopilotModule(WhatsAppNotificationHolder.repository)
    }

    val phoneCallModule = PhoneAndVoipCallModule()
    val youtubeRadioModule = YouTubeRadioModule()

    // Épico 4 & 5: Memória de Rotina, Grafo Familiar e Resiliência do Cuidador
    val app by lazy { application as? VozGuiaApplication }
    val contactRepo by lazy { app?.contactRepository }
    val reminderRepo by lazy { app?.reminderRepository }
    val favoriteRepo by lazy { app?.favoriteRepository }
    val prefRepo by lazy { app?.preferenceRepository }
    val queryRepo by lazy { app?.queryRepository }

    val phoneticAliasResolver by lazy {
        contactRepo?.let { PhoneticAliasResolver(it) }
    }
    val conversationalAliasLearner by lazy {
        contactRepo?.let { ConversationalAliasLearner(it) }
    }
    val dailyAutonomyUtilities = DailyAutonomyUtilities()
    val medicineReminderController by lazy {
        reminderRepo?.let { MedicineReminderController(it) }
    }
    val routineHabitLearner by lazy {
        favoriteRepo?.let { RoutineHabitLearner(it) }
    }
    val morningBriefingOrchestrator by lazy {
        if (prefRepo != null && reminderRepo != null) {
            MorningBriefingOrchestrator(prefRepo!!, reminderRepo!!, dailyAutonomyUtilities)
        } else null
    }
    val batteryMonitor = BatteryAndWatchdogMonitor()
    val emergencySosDispatcher by lazy {
        contactRepo?.let { EmergencySosDispatcher(it) }
    }

    // Contatos familiares locais (Fallback caso DB ainda não tenha registros)
    var familyContacts: List<FamilyContact> = listOf(
        FamilyContact(
            id = "1",
            name = "Gabriel",
            phoneNumber = "+5511999990001",
            relationship = "Filho",
            nicknames = listOf("meu filho", "Gabriel", "biel"),
            isTrustedFamily = true
        )
    )

    var guardrailPipeline: GuardrailPipeline? = null

    private fun resolveGuardrailPipeline(): GuardrailPipeline {
        return guardrailPipeline ?: app?.guardrailPipeline ?: VozGuiaGuardrailFactory.createDefaultPipeline(
            PersistentQuotaRepository(EncryptedPrefsQuotaStorage(this))
        ).also { guardrailPipeline = it }
    }

    var currentState: AgentState = AgentState.IDLE
        private set

    var currentScreenSnapshot: ScreenSnapshot? = null
        private set

    private val physicalTriggerInterceptor by lazy {
        PhysicalTriggerInterceptor(
            coroutineScope = serviceScope,
            holdThresholdMs = 900L,
            tapThresholdMs = 500L
        ).apply {
            onHoldTriggered = { action ->
                if (action is TriggerAction.ActivateListening) {
                    onActivateListeningTriggered()
                }
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> onScreenWokeUp()
                Intent.ACTION_SCREEN_OFF -> onScreenTurnedOff()
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    val bm = getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
                    val level = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 50
                    val speech = batteryMonitor.onPowerConnected(level)
                    speakResponse(speech)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    val speech = batteryMonitor.onPowerDisconnected()
                    speakResponse(speech)
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == android.os.BatteryManager.BATTERY_STATUS_FULL
                    if (level >= 0 && scale > 0) {
                        val percent = (level * 100) / scale
                        val alert = batteryMonitor.onBatteryLevelChanged(percent, isCharging)
                        if (alert != null) {
                            speakResponse(alert)
                        }
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        guardrailPipeline = resolveGuardrailPipeline()

        serviceScope.launch {
            val savedSpeed = prefRepo?.getFloat("pref_tts_speed", 0.92f) ?: 0.92f
            ttsEngine.updateSpeechRate(savedSpeed)
        }

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, screenFilter)

        val batteryFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        registerReceiver(batteryReceiver, batteryFilter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // Extrai o snapshot da tela ativa
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            val accessibleRoot = AndroidAccessibilityNode(rootNode)
            currentScreenSnapshot = screenParser.parse(
                root = accessibleRoot,
                packageName = packageName
            )

            // Épico 3 (FEAT-3.3): Pulador Silencioso de Anúncios no YouTube (< 200ms)
            if (packageName == "com.google.android.youtube") {
                val skipAdButton = youtubeRadioModule.findAdSkipButton(currentScreenSnapshot!!)
                if (skipAdButton != null && skipAdButton.bounds != null) {
                    dispatchClickGesture(skipAdButton.bounds.centerX, skipAdButton.bounds.centerY)
                }

                // Continua reprodução se diálogo "Continuar assistindo?" aparecer
                val continueButton = youtubeRadioModule.findConfirmStillWatchingButton(currentScreenSnapshot!!)
                if (continueButton != null && continueButton.bounds != null) {
                    dispatchClickGesture(continueButton.bounds.centerX, continueButton.bounds.centerY)
                }
            }

            // Avalia o guardrail de segurança proativamente (ex: Financial Shield)
            serviceScope.launch {
                val verdict = resolveGuardrailPipeline().evaluate(
                    intent = UserIntent.StopOrCancel,
                    currentScreen = currentScreenSnapshot
                )

                if (verdict is GuardrailVerdict.Blocked && verdict.reason.contains("financeiro")) {
                    currentState = AgentState.IDLE
                    ttsEngine.stop()
                    sttEngine.stopListening()
                    feedbackEngine.stopHeartbeat()
                }
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false

        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_UP) {
            return super.onKeyEvent(event)
        }

        val isKeyDown = event.action == KeyEvent.ACTION_DOWN
        val eventTime = SystemClock.uptimeMillis()

        val action = physicalTriggerInterceptor.onKeyEvent(
            keyCode = keyCode,
            isKeyDown = isKeyDown,
            eventTimeMs = eventTime,
            currentState = currentState
        )

        return when (action) {
            TriggerAction.ActivateListening -> {
                onActivateListeningTriggered()
                true
            }

            TriggerAction.PanicCancel -> {
                onPanicCancelTriggered()
                true
            }

            TriggerAction.AdjustVolumeDown -> {
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                true
            }

            TriggerAction.AdjustVolumeUp -> {
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                true
            }

            TriggerAction.ConsumeQuietly -> true
            TriggerAction.PassThrough -> super.onKeyEvent(event)
        }
    }

    fun onActivateListeningTriggered() {
        serviceScope.launch {
            val savedSpeed = prefRepo?.getFloat("pref_tts_speed", 0.92f) ?: 0.92f
            ttsEngine.updateSpeechRate(savedSpeed)

            val calendar = Calendar.getInstance()
            if (morningBriefingOrchestrator?.shouldTriggerBriefing(calendar) == true) {
                val bm = getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
                val batteryLevel = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 80
                val briefing = morningBriefingOrchestrator!!.deliverBriefing(
                    calendar = calendar,
                    batteryPercent = batteryLevel
                )
                audioFocusCoordinator.acquireFocusForVoiceInteraction()
                speakResponse(briefing) {
                    antiEchoCoordinator.scheduleMicrophoneOpenAfterTts(
                        flushAudioBuffer = {},
                        onReadyToListen = { startVoiceCapture() }
                    )
                }
            } else {
                startVoiceCapture()
            }
        }
    }

    private fun startVoiceCapture(isDictation: Boolean = false) {
        currentState = AgentState.LISTENING
        audioFocusCoordinator.acquireFocusForVoiceInteraction()
        feedbackEngine.trigger(FeedbackEvent.LISTENING_START)
        vadController.startListening(isDictation = isDictation)
        sttEngine.startListening(
            isDictation = isDictation,
            onVoiceStart = { vadController.onVoiceActivityDetected() },
            onVoiceEnd = { vadController.onSilenceDetected() },
            onResult = { recognizedText ->
                vadController.stop()
                processRecognizedText(recognizedText)
            },
            onError = { _ ->
                vadController.stop()
                onSilenceTimeout()
            }
        )
    }

    fun onPanicCancelTriggered() {
        currentState = AgentState.IDLE
        ttsEngine.stop()
        sttEngine.stopListening()
        vadController.stop()
        feedbackEngine.stopHeartbeat()
        feedbackEngine.trigger(FeedbackEvent.CANCELLED)
        audioFocusCoordinator.releaseFocus(preservePause = false)
        dialogueStateManager.clearPendingConfirmation()
    }

    private fun onUserSpeechFinished() {
        currentState = AgentState.THINKING
        feedbackEngine.trigger(FeedbackEvent.THINKING_START)
        feedbackEngine.startHeartbeat()
    }

    private fun onSilenceTimeout() {
        currentState = AgentState.IDLE
        feedbackEngine.trigger(FeedbackEvent.CANCELLED)
        audioFocusCoordinator.releaseFocus(preservePause = false)
    }

    fun processRecognizedText(transcribedText: String) {
        feedbackEngine.stopHeartbeat()

        val intent = intentRouter.route(transcribedText)

        // Se há diálogo de confirmação aberto (AWAITING_CONFIRMATION)
        if (dialogueStateManager.currentState == AgentState.AWAITING_CONFIRMATION) {
            val dialogueResult = dialogueStateManager.handleIntent(intent)
            when (dialogueResult) {
                is DialogueResult.ActionExecuted -> {
                    feedbackEngine.trigger(FeedbackEvent.SUCCESS)
                    speakResponse("Pronto, comando executado.")
                    return
                }

                is DialogueResult.ActionCancelled -> {
                    feedbackEngine.trigger(FeedbackEvent.CANCELLED)
                    speakResponse("Tudo bem, cancelado.")
                    return
                }

                else -> {}
            }
        }

        // Se for pedido de repetição (FEAT-2.1)
        if (intent is UserIntent.RepeatLast) {
            currentState = AgentState.SPEAKING
            ttsEngine.repeatLast {
                currentState = AgentState.IDLE
                audioFocusCoordinator.releaseFocus(preservePause = false)
            }
            return
        }

        // Avaliação no pipeline de Guardrails
        serviceScope.launch {
            val verdict = resolveGuardrailPipeline().evaluate(intent, currentScreenSnapshot)

            when (verdict) {
                is GuardrailVerdict.Blocked -> {
                    feedbackEngine.trigger(FeedbackEvent.CANCELLED)
                    speakResponse(verdict.spokenMessage)
                }

                is GuardrailVerdict.RequiresVoiceConfirmation -> {
                    dialogueStateManager.setPendingConfirmation(
                        prompt = verdict.confirmationPrompt,
                        onConfirm = {
                            executeConfirmedAction(intent)
                        }
                    )
                    speakResponse(verdict.confirmationPrompt) {
                        antiEchoCoordinator.scheduleMicrophoneOpenAfterTts(
                            flushAudioBuffer = {},
                            onReadyToListen = {
                                startVoiceCapture(isDictation = false)
                            }
                        )
                    }
                }

                is GuardrailVerdict.Allowed -> {
                    dispatchAllowedIntent(intent)
                }
            }
        }
    }

    private fun executeConfirmedAction(intent: UserIntent) {
        when (intent) {
            is UserIntent.SendWhatsAppMessage -> {
                feedbackEngine.trigger(FeedbackEvent.SUCCESS)
                speakResponse("Mensagem enviada para ${intent.targetContact}.")
            }
            else -> {}
        }
    }

    private fun dispatchAllowedIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.TimeQuery -> {
                speakResponse(dailyAutonomyUtilities.formatTimeSpoken())
            }

            is UserIntent.DateQuery -> {
                speakResponse(dailyAutonomyUtilities.formatDateSpoken())
            }

            is UserIntent.BatteryQuery -> {
                val bm = getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
                val batteryLevel = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
                val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus = registerReceiver(null, ifilter)
                val status = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == android.os.BatteryManager.BATTERY_STATUS_FULL
                speakResponse(dailyAutonomyUtilities.formatBatterySpoken(batteryLevel, isCharging))
            }

            is UserIntent.EmergencySos -> {
                serviceScope.launch {
                    val result = emergencySosDispatcher?.dispatchEmergencySos()
                    if (result is EmergencySosResult.Executed) {
                        speakResponse(result.spokenAnnouncement) {
                            launchPhoneCall(
                                phoneNumber = result.phoneNumber,
                                useSpeakerphone = true,
                                maxVolume = result.maxSpeakerVolume
                            )
                        }
                    }
                }
            }

            is UserIntent.StopOrCancel -> {
                onPanicCancelTriggered()
            }

            is UserIntent.PauseMedia -> {
                audioFocusCoordinator.releaseFocus(preservePause = true)
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
                feedbackEngine.trigger(FeedbackEvent.SUCCESS)
                currentState = AgentState.IDLE
            }

            is UserIntent.ResumeMedia -> {
                audioFocusCoordinator.releaseFocus(preservePause = false)
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
                feedbackEngine.trigger(FeedbackEvent.SUCCESS)
                currentState = AgentState.IDLE
            }

            is UserIntent.NextMedia -> {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
                feedbackEngine.trigger(FeedbackEvent.SUCCESS)
                currentState = AgentState.IDLE
                audioFocusCoordinator.releaseFocus(preservePause = false)
            }

            // Épico 3 (FEAT-3.1): Leitura de Mensagens WhatsApp
            is UserIntent.ReadUnreadWhatsAppMessages -> {
                val summary = whatsAppCopilot.summarizeUnread(vipContacts = setOf("Gabriel"))
                speakResponse(summary.spokenSummary)
            }

            // Épico 3 (FEAT-3.1): Play em Áudio de Voz do WhatsApp
            is UserIntent.PlayWhatsAppAudio -> {
                val targetAudio = currentScreenSnapshot?.let { whatsAppCopilot.findVoiceNoteToPlay(it) }
                if (targetAudio != null && targetAudio.bounds != null) {
                    dispatchClickGesture(targetAudio.bounds.centerX, targetAudio.bounds.centerY)
                    feedbackEngine.trigger(FeedbackEvent.SUCCESS)
                    currentState = AgentState.IDLE
                    audioFocusCoordinator.releaseFocus(preservePause = false)
                } else {
                    speakResponse("Não encontrei nenhum áudio não ouvido nesta conversa.")
                }
            }

            // Épico 3 & 4 (FEAT-3.2, FEAT-4.1): Chamada Telefônica com Resolução Fonética e Apelidos
            is UserIntent.DirectCall -> {
                serviceScope.launch {
                    val phoneticResult = phoneticAliasResolver?.resolve(intent.targetName)
                    val contact = if (phoneticResult is PhoneticMatchResult.Found) phoneticResult.contact else null

                    if (contact != null) {
                        speakResponse("Ligando para ${contact.displayName}.") {
                            launchPhoneCall(phoneNumber = contact.phoneNumber, useSpeakerphone = true)
                        }
                    } else {
                        val dialResult = phoneCallModule.resolveCallTarget(
                            query = intent.targetName,
                            contacts = familyContacts,
                            isHeadsetConnected = false
                        )

                        when (dialResult) {
                            is DialResult.Initiated -> {
                                speakResponse(dialResult.spokenAnnouncement) {
                                    launchPhoneCall(
                                        phoneNumber = dialResult.contact.phoneNumber,
                                        useSpeakerphone = dialResult.useSpeakerphone
                                    )
                                }
                            }

                            is DialResult.Ambiguous -> {
                                speakResponse(dialResult.spokenPrompt)
                            }

                            is DialResult.NotFound -> {
                                val detectedRole = (phoneticResult as? PhoneticMatchResult.NotFound)?.detectedRole
                                if (detectedRole != null && conversationalAliasLearner != null) {
                                    val learningState = conversationalAliasLearner!!.startLearning(detectedRole)
                                    if (learningState is AliasLearningState.AwaitingCandidateName) {
                                        speakResponse(learningState.spokenPrompt)
                                    } else {
                                        speakResponse(dialResult.spokenMessage)
                                    }
                                } else {
                                    speakResponse(dialResult.spokenMessage)
                                }
                            }
                        }
                    }
                }
            }

            // Épico 3 & 4 (FEAT-3.3, FEAT-4.3): YouTube Busca & Modo Rádio com Hábitos
            is UserIntent.YouTubeSearchAndPlay -> {
                serviceScope.launch {
                    val isGenericQuery = intent.query.lowercase() in listOf(
                        "música", "musica", "minhas músicas", "minhas musicas", "meu programa",
                        "as notícias", "notícias", "noticias", "rádio", "radio"
                    )
                    val (finalQuery, announcement) = if (isGenericQuery && routineHabitLearner != null) {
                        val habit = routineHabitLearner!!.resolveGenericMediaCommand(intent.query)
                        habit.searchQuery to habit.spokenAnnouncement
                    } else {
                        intent.query to "Buscando ${intent.query} no YouTube."
                    }

                    speakResponse(announcement) {
                        val ytIntent = Intent(Intent.ACTION_SEARCH).apply {
                            setPackage("com.google.android.youtube")
                            putExtra("query", finalQuery)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            startActivity(ytIntent)
                        } catch (_: Exception) {}
                    }
                }
            }

            // Épico 3 (FEAT-3.3): Pular Anúncio no YouTube
            is UserIntent.YouTubeSkipAd -> {
                val skipAdButton = currentScreenSnapshot?.let { youtubeRadioModule.findAdSkipButton(it) }
                if (skipAdButton != null && skipAdButton.bounds != null) {
                    dispatchClickGesture(skipAdButton.bounds.centerX, skipAdButton.bounds.centerY)
                    feedbackEngine.trigger(FeedbackEvent.SUCCESS)
                } else {
                    speakResponse("Não encontrei botão de pular anúncio na tela.")
                }
            }

            // Épico 5 (FEAT-5.1 US-5.1.3): Log local de falas não compreendidas para diagnóstico do Gabriel
            is UserIntent.UnknownIntent -> {
                serviceScope.launch {
                    queryRepo?.logQuery(intent.rawTranscription, System.currentTimeMillis())
                }
                speakResponse("Desculpe, não entendi. Pode repetir?")
            }

            else -> {
                currentState = AgentState.IDLE
                audioFocusCoordinator.releaseFocus(preservePause = false)
            }
        }
    }

    private fun launchPhoneCall(
        phoneNumber: String,
        useSpeakerphone: Boolean = true,
        maxVolume: Boolean = false
    ) {
        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(callIntent)
            if (maxVolume) {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_VOICE_CALL,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                    0
                )
            }
            if (useSpeakerphone) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val speakerDevice = audioManager.availableCommunicationDevices
                        .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    if (speakerDevice != null) {
                        audioManager.setCommunicationDevice(speakerDevice)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = true
                }
            }
        } catch (_: Exception) {}
    }

    private fun dispatchClickGesture(x: Int, y: Int) {
        val clickPath = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(clickPath, 0, 80))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun speakResponse(text: String, onDone: (() -> Unit)? = null) {
        currentState = AgentState.SPEAKING
        ttsEngine.speak(text = text, isImportant = true) {
            if (onDone != null) {
                onDone()
            } else {
                currentState = AgentState.IDLE
                audioFocusCoordinator.releaseFocus(preservePause = false)
            }
        }
    }

    private fun onScreenWokeUp() {
        feedbackEngine.trigger(FeedbackEvent.WAKEUP)
    }

    private fun onScreenTurnedOff() {
        currentState = AgentState.IDLE
        ttsEngine.stop()
        sttEngine.stopListening()
        feedbackEngine.stopHeartbeat()
    }

    override fun onInterrupt() {
        currentState = AgentState.IDLE
        ttsEngine.stop()
        sttEngine.stopListening()
        feedbackEngine.stopHeartbeat()
    }

    override fun onDestroy() {
        super.onDestroy()
        sttEngine.destroy()
        ttsSpeaker.shutdown()
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
        serviceScope.cancel()
    }
}
