package com.example.mclauncher.instance

import android.content.Context
import com.example.mclauncher.model.Instance
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class InstanceManager(context: Context) {
    private val root = File(context.filesDir, "instances").apply { mkdirs() }
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val index = File(root, "instances.json")

    fun list(): List<Instance> =
        if (index.exists()) json.decodeFromString(index.readText()) else emptyList()

    fun create(name: String, version: String, loader: String = "vanilla"): Instance {
        val inst = Instance(UUID.randomUUID().toString(), name, version, loader)
        save(list() + inst)
        dirFor(inst).apply {
            mkdirs()
            File(this, "mods").mkdirs()
            File(this, "resourcepacks").mkdirs()
        }
        return inst
    }

    fun update(updated: Instance) {
        save(list().map { if (it.id == updated.id) updated else it })
    }

    fun delete(id: String) {
        save(list().filterNot { it.id == id })
        dirFor(id).deleteRecursively()
    }

    fun dirFor(inst: Instance) = dirFor(inst.id)
    private fun dirFor(id: String) = File(root, id)

    private fun save(instances: List<Instance>) {
        index.writeText(json.encodeToString(instances))
    }
}
