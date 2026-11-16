package br.com.vozguia.presentation.caregiver

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.vozguia.core.caregiver.CaregiverDashboardViewModel
import br.com.vozguia.core.caregiver.MotorolaPermissionItem
import br.com.vozguia.core.routine.domain.MediaFavorite
import br.com.vozguia.core.routine.domain.RoutineReminder
import br.com.vozguia.core.routine.domain.TimePeriod
import br.com.vozguia.core.routine.domain.TrustedContact
import br.com.vozguia.core.routine.domain.UnrecognizedQuery

@Composable
fun CaregiverDashboardScreen(
    viewModel: CaregiverDashboardViewModel,
    onPreviewTts: (speed: Float, text: String) -> Unit = { _, _ -> },
    onRequestRuntimePermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Blindagem", "Família", "Rotina", "Diagnóstico")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        Text(
            text = "Modo Cuidador — Voz Guia",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Configuração e blindagem do aparelho para seu pai",
            fontSize = 14.sp,
            color = Color(0xFFAAAAAA)
        )

        Spacer(modifier = Modifier.height(16.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White,
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) Color(0xFF4CAF50) else Color.LightGray
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> PermissionsTab(
                permissions = uiState.permissions,
                onRefresh = { viewModel.refreshState() },
                onRequestRuntimePermissions = onRequestRuntimePermissions
            )
            1 -> ContactsTab(
                contacts = uiState.contacts,
                onAddContact = { name, phone, role, aliases, emergency ->
                    viewModel.addContact(name, phone, role, aliases, emergency)
                },
                onDeleteContact = { viewModel.deleteContact(it) }
            )
            2 -> RoutineTab(
                reminders = uiState.reminders,
                favorites = uiState.favorites,
                ttsSpeed = uiState.ttsSpeed,
                onAddReminder = { title, time -> viewModel.addReminder(title, time) },
                onDeleteReminder = { viewModel.deleteReminder(it) },
                onAddFavorite = { title, query, period -> viewModel.addFavorite(title, query, period) },
                onDeleteFavorite = { viewModel.deleteFavorite(it) },
                onSpeedChange = { viewModel.setTtsSpeed(it) },
                previewText = viewModel.getTtsPreviewText(),
                onPreviewTts = onPreviewTts
            )
            3 -> DiagnosticTab(
                queries = uiState.unrecognizedQueries,
                contacts = uiState.contacts,
                onLinkAlias = { qId, cId, alias ->
                    viewModel.linkUnrecognizedQueryAsContactAlias(qId, cId, alias)
                }
            )
        }
    }
}

@Composable
private fun darkTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color(0xFF81C784),
    unfocusedLabelColor = Color(0xFFAAAAAA),
    focusedBorderColor = Color(0xFF4CAF50),
    unfocusedBorderColor = Color(0xFF555555)
)

@Composable
fun PermissionsTab(
    permissions: List<MotorolaPermissionItem>,
    onRefresh: () -> Unit,
    onRequestRuntimePermissions: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onRequestRuntimePermissions,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Conceder Áudio/Tel")
                }
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🔄 Atualizar")
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF162436)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🔒 Divulgação Proeminente — Acessibilidade e Privacidade",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF90CAF9)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "O Voz Guia usa a API AccessibilityService exclusivamente como ferramenta assistiva para pessoas com deficiência visual: " +
                            "lê elementos da tela em voz alta, detecta o toque longo em Volume+ para ativação por voz e executa ações autorizadas (ligações, WhatsApp, YouTube).\n\n" +
                            "O Acesso às Notificações lê apenas mensagens do WhatsApp para leitura em voz alta. " +
                            "Apps bancários, senhas, códigos e fluxos de pagamento são bloqueados e ocultados automaticamente (Escudo Financeiro). " +
                            "Seus dados e rotinas ficam armazenados localmente no aparelho e não são coletados, vendidos ou compartilhados com terceiros.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = Color(0xFFE0E0E0)
                    )
                }
            }
        }

        items(permissions) { item ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (item.isGranted) Color(0xFF1B2E1B) else Color(0xFF332020)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (item.isGranted) "✅ ATIVO" else "⚠️ PENDENTE",
                            fontWeight = FontWeight.Bold,
                            color = if (item.isGranted) Color(0xFF81C784) else Color(0xFFFF8A80)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        fontSize = 13.sp,
                        color = Color(0xFFCCCCCC)
                    )
                    if (!item.isGranted) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (item.settingsActionIntent == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                                    onRequestRuntimePermissions()
                                }
                                openPermissionSettings(context, item.settingsActionIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Ativar Configuração")
                        }
                    }
                }
            }
        }
    }
}

private fun openPermissionSettings(context: Context, intentAction: String) {
    val packageUriActions = setOf(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    )
    try {
        val intent = if (intentAction in packageUriActions) {
            Intent(intentAction).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(intentAction).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val fallback = Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(fallback)
    }
}

@Composable
fun ContactsTab(
    contacts: List<TrustedContact>,
    onAddContact: (String, String, String, List<String>, Boolean) -> Unit,
    onDeleteContact: (TrustedContact) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var aliases by remember { mutableStateOf("") }
    var isEmergency by remember { mutableStateOf(false) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Adicionar Contato Confiável", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome (ex: Gabriel Silva)") },
                        singleLine = true,
                        colors = darkTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefone / WhatsApp (ex: 11999998888)") },
                        singleLine = true,
                        colors = darkTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text("Parentesco (ex: filho, esposa)") },
                        singleLine = true,
                        colors = darkTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = aliases,
                        onValueChange = { aliases = it },
                        label = { Text("Apelidos (ex: biel, meu filho)") },
                        singleLine = true,
                        colors = darkTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🚨 Contato Principal de Emergência (SOS)",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isEmergency,
                            onCheckedChange = { isEmergency = it }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                val aliasList = aliases.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                onAddContact(name, phone, role, aliasList, isEmergency)
                                name = ""; phone = ""; role = ""; aliases = ""; isEmergency = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Salvar Contato Familiar")
                    }
                }
            }
        }

        items(contacts) { contact ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF252525)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${contact.displayName} (${contact.relationshipRole})",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "📞 ${contact.phoneNumber}",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        if (contact.phoneticAliases.isNotEmpty()) {
                            Text(
                                text = "Apelidos: ${contact.phoneticAliases.joinToString(", ")}",
                                color = Color(0xFF81C784),
                                fontSize = 12.sp
                            )
                        }
                        if (contact.isEmergencyContact) {
                            Text(
                                text = "🚨 CONTATO DE EMERGÊNCIA SOS",
                                color = Color(0xFFFF5252),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Button(
                        onClick = { onDeleteContact(contact) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000))
                    ) {
                        Text("Excluir", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RoutineTab(
    reminders: List<RoutineReminder>,
    favorites: List<MediaFavorite>,
    ttsSpeed: Float,
    onAddReminder: (String, String) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onAddFavorite: (String, String, TimePeriod) -> Unit,
    onDeleteFavorite: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    previewText: String,
    onPreviewTts: (Float, String) -> Unit
) {
    var reminderTitle by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf("") }

    var favTitle by remember { mutableStateOf("") }
    var favQuery by remember { mutableStateOf("") }
    var favPeriod by remember { mutableStateOf(TimePeriod.MANHA) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Velocidade da Voz
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Velocidade da Voz do Assistente", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Velocidade atual: ${"%.2f".format(ttsSpeed)}x", color = Color(0xFF81C784), fontSize = 14.sp)
                    Slider(
                        value = ttsSpeed,
                        onValueChange = onSpeedChange,
                        valueRange = 0.75f..1.20f
                    )
                    Text("Prévia: \"$previewText\"", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onPreviewTts(ttsSpeed, previewText) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔊 Ouvir Teste")
                    }
                }
            }
        }

        // Favoritos do YouTube / Rádio Pessoal (US-5.1.2)
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Rádios e Músicas Favoritas (YouTube)", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = favTitle,
                        onValueChange = { favTitle = it },
                        label = { Text("Nome falado (ex: Moda de Viola)") },
                        singleLine = true,
                        colors = darkTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = favQuery,
                        onValueChange = { favQuery = it },
                        label = { Text("Busca YouTube (ex: sertanejo raiz)") },
                        singleLine = true,
                        colors = darkTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimePeriod.entries.forEach { period ->
                            FilterChip(
                                selected = favPeriod == period,
                                onClick = { favPeriod = period },
                                label = { Text(period.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2E7D32),
                                    selectedLabelColor = Color.White,
                                    labelColor = Color.LightGray
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (favTitle.isNotBlank() && favQuery.isNotBlank()) {
                                onAddFavorite(favTitle, favQuery, favPeriod)
                                favTitle = ""; favQuery = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Salvar Favorito no Turno")
                    }
                }
            }
        }

        items(favorites) { fav ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF252525))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("📻 ${fav.title} (${fav.timePeriod.name})", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Busca: ${fav.searchQuery}", color = Color(0xFF81C784), fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onDeleteFavorite(fav.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000))
                    ) {
                        Text("Excluir", fontSize = 12.sp)
                    }
                }
            }
        }

        // Adicionar Lembrete
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Novo Lembrete de Remédio", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = reminderTitle,
                        onValueChange = { reminderTitle = it },
                        label = { Text("Remédio (ex: Pressão)") },
                        singleLine = true,
                        colors = darkTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = { reminderTime = it },
                        label = { Text("Horário HH:MM (ex: 08:00)") },
                        singleLine = true,
                        colors = darkTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (reminderTitle.isNotBlank() && reminderTime.isNotBlank()) {
                                onAddReminder(reminderTitle, reminderTime)
                                reminderTitle = ""; reminderTime = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cadastrar Alarme de Remédio")
                    }
                }
            }
        }

        // Lista Lembretes
        items(reminders) { reminder ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF252525))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("💊 ${reminder.title}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Horário: ${reminder.timeHhMm}", color = Color(0xFF81C784), fontSize = 13.sp)
                    }
                    Button(
                        onClick = { onDeleteReminder(reminder.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000))
                    ) {
                        Text("Excluir", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticTab(
    queries: List<UnrecognizedQuery>,
    contacts: List<TrustedContact>,
    onLinkAlias: (Long, Long, String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = "Histórico de Falas Não Compreendidas",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            Text(
                text = "Permite identificar palavras ou apelidos que seu pai falou para associar a contatos com 1 clique.",
                fontSize = 12.sp,
                color = Color.LightGray
            )
        }

        if (queries.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhuma fala pendente registrada. O assistente compreendeu todos os comandos!", color = Color(0xFF81C784))
                    }
                }
            }
        } else {
            items(queries) { query ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF252525))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Fala transcrita: \"${query.transcribedText}\"", color = Color.White, fontWeight = FontWeight.Bold)
                        if (query.resolvedAction != null) {
                            Text("Status: ${query.resolvedAction}", color = Color(0xFF81C784), fontSize = 12.sp)
                        } else {
                            Text("Status: Não compreendido", color = Color(0xFFFF8A80), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Vincular como apelido para:", color = Color.LightGray, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                contacts.take(3).forEach { contact ->
                                    Button(
                                        onClick = { onLinkAlias(query.id, contact.id, query.transcribedText) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Text(contact.displayName, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
