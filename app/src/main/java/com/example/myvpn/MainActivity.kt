package com.example.myvpn

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.util.UUID
import kotlin.concurrent.thread

enum class Status { DISCONNECTED, CONNECTING, CONNECTED }

class MainActivity : ComponentActivity() {

    private lateinit var backend: GoBackend
    private lateinit var store: ServerStore
    private val servers = mutableStateListOf<Server>()
    private var selectedId by mutableStateOf<String?>(null)
    private var status by mutableStateOf(Status.DISCONNECTED)
    private var pending: Server? = null

    private val tunnel = object : Tunnel {
        override fun getName() = "myvpn"
        override fun onStateChange(newState: Tunnel.State) {
            runOnUiThread {
                status = if (newState == Tunnel.State.UP) Status.CONNECTED else Status.DISCONNECTED
            }
        }
    }

    private val vpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            val s = pending
            pending = null
            if (r.resultCode == Activity.RESULT_OK && s != null) start(s) else toast("VPN permission denied")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backend = GoBackend(applicationContext)
        store = ServerStore(this)
        servers.addAll(store.load())
        selectedId = store.selectedId?.takeIf { id -> servers.any { it.id == id } } ?: servers.firstOrNull()?.id
        try {
            if (backend.getState(tunnel) == Tunnel.State.UP) status = Status.CONNECTED
        } catch (_: Exception) {}
        setContent { App() }
    }

    // ---------- VPN control ----------

    private fun connect(s: Server) {
        val intent = GoBackend.VpnService.prepare(this)
        if (intent != null) { pending = s; vpnPermission.launch(intent) } else start(s)
    }

    private fun start(s: Server) {
        status = Status.CONNECTING
        thread {
            try {
                if (backend.getState(tunnel) == Tunnel.State.UP) backend.setState(tunnel, Tunnel.State.DOWN, null)
                backend.setState(tunnel, Tunnel.State.UP, Config.parse(s.config.byteInputStream()))
            } catch (e: Exception) {
                runOnUiThread { status = Status.DISCONNECTED; toast("Error: ${e.message}") }
            }
        }
    }

    private fun disconnect() {
        thread {
            try { backend.setState(tunnel, Tunnel.State.DOWN, null) }
            catch (e: Exception) { runOnUiThread { toast("Error: ${e.message}") } }
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()

    private fun select(s: Server) {
        selectedId = s.id
        store.selectedId = s.id
        if (status == Status.CONNECTED) connect(s) // switch server live
    }

    private fun onPower() {
        val s = servers.find { it.id == selectedId }
        when {
            s == null -> toast("Add a server first")
            status == Status.CONNECTED -> disconnect()
            status == Status.DISCONNECTED -> connect(s)
        }
    }

    // ---------- UI ----------

    @Composable
    private fun App() {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Color(0xFF4F8CFF),
                background = Color(0xFF0B1020),
                surface = Color(0xFF151B2E),
            )
        ) { Home() }
    }

    @Composable
    private fun Home() {
        var showAdd by remember { mutableStateOf(false) }
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Text("MyVPN", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(20.dp))
            PowerButton()
            Spacer(Modifier.height(24.dp))
            Text("Servers", color = Color(0xFF9AA4BF), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            if (servers.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "No servers yet.\nTap “Add server” and paste a WireGuard config.",
                        color = Color(0xFF9AA4BF)
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(servers, key = { it.id }) { ServerRow(it) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add server")
            }
        }
        if (showAdd) {
            AddServerDialog(
                onDismiss = { showAdd = false },
                onSave = { s ->
                    servers.add(s)
                    store.save(servers.toList())
                    if (selectedId == null) { selectedId = s.id; store.selectedId = s.id }
                    showAdd = false
                }
            )
        }
    }

    @Composable
    private fun PowerButton() {
        val target = when (status) {
            Status.CONNECTED -> Color(0xFF22C55E)
            Status.CONNECTING -> Color(0xFFF59E0B)
            Status.DISCONNECTED -> Color(0xFF4F8CFF)
        }
        val color by animateColorAsState(target, label = "power")
        val label = when (status) {
            Status.CONNECTED -> "Disconnect"
            Status.CONNECTING -> "Connecting…"
            Status.DISCONNECTED -> "Connect"
        }
        val selected = servers.find { it.id == selectedId }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(180.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(144.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable(enabled = status != Status.CONNECTING) { onPower() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                when (status) {
                    Status.CONNECTED -> "Protected"
                    Status.CONNECTING -> "Connecting…"
                    Status.DISCONNECTED -> "Not connected"
                },
                color = color, fontWeight = FontWeight.SemiBold, fontSize = 16.sp
            )
            if (selected != null) {
                val c = countryOf(selected.countryCode)
                Text("${c.flag}  ${c.name} · ${selected.name}", color = Color(0xFF9AA4BF), fontSize = 14.sp)
            }
        }
    }

    @Composable
    private fun ServerRow(s: Server) {
        val c = countryOf(s.countryCode)
        val isSel = s.id == selectedId
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSel) Color(0xFF1E2A4A) else MaterialTheme.colorScheme.surface)
                .clickable { select(s) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(c.flag, fontSize = 30.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(s.name, color = Color(0xFF9AA4BF), fontSize = 13.sp)
            }
            if (isSel) Text("●", color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = {
                if (isSel && status != Status.DISCONNECTED) disconnect()
                servers.remove(s)
                store.save(servers.toList())
                if (isSel) { selectedId = servers.firstOrNull()?.id; store.selectedId = selectedId }
            }) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFF9AA4BF)) }
        }
    }

    @Composable
    private fun AddServerDialog(onDismiss: () -> Unit, onSave: (Server) -> Unit) {
        var country by remember { mutableStateOf(COUNTRIES.first()) }
        var menu by remember { mutableStateOf(false) }
        var name by remember { mutableStateOf("") }
        var config by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }
        val clipboard = LocalClipboardManager.current

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add server") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box {
                        OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("${country.flag}  ${country.name}")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            COUNTRIES.forEach {
                                DropdownMenuItem(
                                    text = { Text("${it.flag}  ${it.name}") },
                                    onClick = { country = it; menu = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Name (e.g. Frankfurt 1)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = config, onValueChange = { config = it },
                        label = { Text("WireGuard config") }, minLines = 5, maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { clipboard.getText()?.text?.let { config = it } }) {
                        Text("Paste from clipboard")
                    }
                    err?.let { Text(it, color = Color(0xFFEF4444), fontSize = 13.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cfg = config.trim()
                    when {
                        !cfg.contains("[Interface]") || !cfg.contains("[Peer]") ->
                            err = "Config must contain [Interface] and [Peer]"
                        else -> try {
                            Config.parse(cfg.byteInputStream())
                            onSave(
                                Server(
                                    UUID.randomUUID().toString(),
                                    name.ifBlank { country.name },
                                    country.code, cfg
                                )
                            )
                        } catch (e: Exception) { err = "Invalid config: ${e.message}" }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
    }
}
