package com.example.mclauncher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mclauncher.account.OfflineAccounts
import com.example.mclauncher.instance.InstanceManager
import com.example.mclauncher.model.Instance
import com.example.mclauncher.model.Renderer

@Composable
fun MainScreen(manager: InstanceManager, onLaunch: (Instance) -> Unit) {
    var instances by remember { mutableStateOf(manager.list()) }
    var username by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var account by remember { mutableStateOf<String?>(null) }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        Text("Offline account", style = MaterialTheme.typography.titleMedium)
        Row {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                runCatching { OfflineAccounts.create(username) }
                    .onSuccess { account = it.username; error = null }
                    .onFailure { error = it.message }
            }) { Text("Set") }
        }
        account?.let { Text("Playing as $it") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(16.dp))
        Row {
            Text("Instances", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Button(onClick = {
                manager.create("New instance", "1.20.4")
                instances = manager.list()
            }) { Text("+ New") }
        }

        LazyColumn {
            items(instances) { inst ->
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(inst.name, style = MaterialTheme.typography.titleSmall)
                        Text("${inst.gameVersion} · ${inst.loader} · ${inst.render.renderer}")
                        Row {
                            TextButton(onClick = { onLaunch(inst) }) { Text("Launch") }
                            TextButton(onClick = {
                                manager.update(inst.copy(render = inst.render.copy(renderer = Renderer.VULKAN_ZINK)))
                                instances = manager.list()
                            }) { Text("Use Zink") }
                            TextButton(onClick = {
                                manager.delete(inst.id)
                                instances = manager.list()
                            }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}
