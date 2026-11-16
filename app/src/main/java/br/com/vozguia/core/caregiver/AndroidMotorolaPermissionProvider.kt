package br.com.vozguia.core.caregiver

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.vozguia.service.accessibility.VozGuiaAccessibilityService

class AndroidMotorolaPermissionProvider(
    private val context: Context
) : MotorolaPermissionProvider {

    override fun getPermissionsStatus(): List<MotorolaPermissionItem> {
        val packageName = context.packageName

        // 1. Acessibilidade
        val accessibilityEnabled = isAccessibilityServiceEnabled(context)

        // 2. Notificações (NotificationListenerService)
        val notificationsEnabled = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(packageName)

        // 3. Fazer ligações telefônicas
        val callPhoneGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        // 4. Gravar áudio (STT)
        val recordAudioGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        // 5. Sobreposição de tela (System Alert Window)
        val overlayGranted = Settings.canDrawOverlays(context)

        // 6. Blindagem de bateria (Ignorar otimizações de bateria Motorola)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIgnored = powerManager?.isIgnoringBatteryOptimizations(packageName) ?: false

        return listOf(
            MotorolaPermissionItem(
                title = "Serviço de Acessibilidade",
                description = "Permite ao assistente ler os elementos da tela e executar ações no WhatsApp e Telefone.",
                isGranted = accessibilityEnabled,
                settingsActionIntent = Settings.ACTION_ACCESSIBILITY_SETTINGS
            ),
            MotorolaPermissionItem(
                title = "Acesso a Notificações",
                description = "Permite identificar quem enviou mensagens e áudios de voz no WhatsApp.",
                isGranted = notificationsEnabled,
                settingsActionIntent = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            ),
            MotorolaPermissionItem(
                title = "Fazer Chamadas Telefônicas",
                description = "Permite discar diretamente para familiares e contatos de emergência.",
                isGranted = callPhoneGranted,
                settingsActionIntent = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            ),
            MotorolaPermissionItem(
                title = "Microfone (Reconhecimento de Voz)",
                description = "Permite ao assistente escutar comandos por voz 100% offline no aparelho.",
                isGranted = recordAudioGranted,
                settingsActionIntent = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            ),
            MotorolaPermissionItem(
                title = "Sobrepor a Outros Apps",
                description = "Garante que o escudo protetor e os alertas táteis funcionem sobre qualquer app.",
                isGranted = overlayGranted,
                settingsActionIntent = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            ),
            MotorolaPermissionItem(
                title = "Bateria Irrestrita (Motorola)",
                description = "Impede que o sistema de economia de bateria da Motorola feche o assistente em segundo plano.",
                isGranted = batteryIgnored,
                settingsActionIntent = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            )
        )
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = ComponentName(context, VozGuiaAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(":").any { raw ->
            ComponentName.unflattenFromString(raw.trim()) == expectedComponent
        }
    }
}
