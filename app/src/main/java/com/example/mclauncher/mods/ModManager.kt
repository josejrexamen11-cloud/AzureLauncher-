package com.example.mclauncher.mods

import android.content.Context
import android.net.Uri
import java.io.File

class ModManager(private val context: Context) {

    fun modsDir(instanceDir: File) = File(instanceDir, "mods").apply { mkdirs() }

    /** Copies a .jar picked via the Storage Access Framework into the instance. */
    fun importMod(uri: Uri, instanceDir: File): File {
        val name = queryName(uri) ?: "mod-${System.currentTimeMillis()}.jar"
        require(name.endsWith(".jar", ignoreCase = true)) { "Only .jar mods are supported." }
        val dest = File(modsDir(instanceDir), name.replace(Regex("[^A-Za-z0-9._-]"), "_"))
        context.contentResolver.openInputStream(uri)!!.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        }
        return dest
    }

    fun listMods(instanceDir: File): List<String> =
        modsDir(instanceDir).listFiles { f -> f.extension == "jar" }
            ?.map { it.name }?.sorted() ?: emptyList()

    fun removeMod(instanceDir: File, fileName: String) {
        File(modsDir(instanceDir), fileName).delete()
    }

    private fun queryName(uri: Uri): String? =
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex("_display_name")
            if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
        }
}
