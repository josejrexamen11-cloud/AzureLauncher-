package com.example.mclauncher.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mclauncher.account.OfflineAccounts
import com.example.mclauncher.instance.InstanceManager
import com.example.mclauncher.launch.LaunchBuilder
import com.example.mclauncher.launch.RuntimeLauncher
import com.example.mclauncher.mods.ModManager
import com.example.mclauncher.model.Instance
import com.example.mclauncher.model.OfflineAccount
import com.example.mclauncher.model.Renderer
import com.example.mclauncher.version.VersionManager
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    manager: InstanceManager,
    versions: VersionManager,
    mods: ModManager,
    runtime: RuntimeLauncher
) {
    val scope = rememberCoroutineScope()
    var instances by remember { mutableStateOf(manager.list()) }
    var username by remember { mutableStateOf("") }
    var account by remember { mutableStateOf<OfflineAccount?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Instance?>(null) }
    var modNames by remember { mutableStateOf<List<String>>(emptyList()) }

    fun refresh() {
        instances = manager.list()
    }

    fun refreshMods(inst: Instance?) {
        modNames = inst?.let { mods.listMods(manager.dirFor(it)) } ?: emptyList()
    }

    val modPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val inst = selected
        if (uri != null && inst != null) {
            runCatching { mods.importMod(uri, manager.dirFor(inst)) }
                .onFailure { error = it.message }
            refreshMods(inst)
        }
    }

    fun newInstance() {
        scope.launch {
            error = null
            runCatching { manager.create("New instance", versions.latestRelease()) }
                .onFailure { error = it.message }
            refresh()
        }
    }

    fun startGame(inst: Instance) {
        val acc = account ?: run {
            error = "Set a username first."
            return
        }
        scope.launch {
            error = null
            status = "Preparing ${inst.gameVersion}…"
            runCatching {
                val vjson = versions.install(inst.gameVersion) { status = "Downloading $it…" }
                val cfg = LaunchBuilder.build(
                    inst, acc, manager.dirFor(inst), versions.root, vjson,
                    versions.classpath(inst.gameVersion, vjson)
                )
                status = "Starting…"
                runtime.start(cfg)
            }.onFailure { error = it.message ?: it.toString() }
            status = null
        }
    }

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
                    .onSuccess { account = it; error = null }
                    .onFailure { error = it.message }
            }) { Text("Set") }
        }
        account?.let { Text("Playing as ${it.username}") }
        status?.let { Text(it) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(16.dp))
        Row {
            Text("Instances", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Button(onClick = { newInstance() }) { Text("+ New") }
        }

        LazyColumn(Modifier.weight(1f)) {
            items(instances) { inst ->
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(inst.name, style = MaterialTheme.typography.titleSmall)
                        Text("${inst.gameVersion} · ${inst.loader} · ${inst.render.renderer}")
                        Row {
                            TextButton(onClick = { startGame(inst) }) { Text("Launch") }
                            TextButton(onClick = {
                                selected = inst
                                refreshMods(inst)
                            }) { Text("Mods") }
                            TextButton(onClick = {
                                manager.update(inst.copy(render = inst.render.copy(renderer = Renderer.VULKAN_ZINK)))
                                refresh()
                            }) { Text("Use Zink") }
                            TextButton(onClick = {
                                manager.delete(inst.id)
                                if (selected?.id == inst.id) {
                                    selected = null
                                    refreshMods(null)
                                }
                                refresh()
                            }) { Text("Delete") }
                        }
                    }
                }
            }
        }

        selected?.let { inst ->
            Spacer(Modifier.height(8.dp))
            Row {
                Text("Mods: ${inst.name}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Button(onClick = { modPicker.launch(arrayOf("*/*")) }) { Text("+ Add .jar") }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(modNames) { name ->
                    Row {
                        Text(name, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            mods.removeMod(manager.dirFor(inst), name)
                            refreshMods(inst)
                        }) { Text("Remove") }
                    }
                }
            }
        }
    }
}
