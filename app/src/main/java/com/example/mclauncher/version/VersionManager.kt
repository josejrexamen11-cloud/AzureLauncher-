package com.example.mclauncher.version

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.URL
import java.security.MessageDigest

private const val MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
private const val ASSET_BASE = "https://resources.download.minecraft.net"

private fun JsonObject.str(key: String): String = this[key]!!.jsonPrimitive.content

class VersionManager(context: Context) {
    val root = File(context.filesDir, "minecraft").apply { mkdirs() }
    private val versions = File(root, "versions").apply { mkdirs() }
    private val libraries = File(root, "libraries").apply { mkdirs() }
    private val assets = File(root, "assets").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun latestRelease(): String = withContext(Dispatchers.IO) {
        manifest()["latest"]!!.jsonObject.str("release")
    }

    /**
     * Downloads the client jar, libraries and assets for [versionId], verifying SHA-1
     * for each file. Already-present, valid files are skipped. Returns the version JSON.
     */
    suspend fun install(versionId: String, onProgress: (String) -> Unit = {}): JsonObject =
        withContext(Dispatchers.IO) {
            val entry = manifest()["versions"]!!.jsonArray
                .map { it.jsonObject }
                .firstOrNull { it.str("id") == versionId }
                ?: error("Unknown Minecraft version: $versionId")

            val versionDir = File(versions, versionId).apply { mkdirs() }
            val versionFile = File(versionDir, "$versionId.json")
            download(entry.str("url"), versionFile, entry.str("sha1"))
            val v = json.parseToJsonElement(versionFile.readText()).jsonObject

            onProgress("client")
            val client = v["downloads"]!!.jsonObject["client"]!!.jsonObject
            download(client.str("url"), File(versionDir, "$versionId.jar"), client.str("sha1"))

            onProgress("libraries")
            libraryArtifacts(v).forEach { a ->
                download(a.str("url"), File(libraries, a.str("path")), a.str("sha1"))
            }

            onProgress("assets")
            val idx = v["assetIndex"]!!.jsonObject
            val indexFile = File(assets, "indexes/${idx.str("id")}.json")
            download(idx.str("url"), indexFile, idx.str("sha1"))
            val objects = json.parseToJsonElement(indexFile.readText()).jsonObject["objects"]!!.jsonObject
            objects.values.forEach { o ->
                val hash = o.jsonObject.str("hash")
                download("$ASSET_BASE/${hash.take(2)}/$hash", File(assets, "objects/${hash.take(2)}/$hash"), hash)
            }
            v
        }

    /** Classpath for `-cp`: every library plus the client jar. Requires [install] first. */
    fun classpath(versionId: String, v: JsonObject): String {
        val libs = libraryArtifacts(v).map { File(libraries, it.str("path")).absolutePath }
        val client = File(versions, "$versionId/$versionId.jar").absolutePath
        return (libs + client).joinToString(File.pathSeparator)
    }

    private fun manifest(): JsonObject =
        json.parseToJsonElement(URL(MANIFEST_URL).readText()).jsonObject

    private fun libraryArtifacts(v: JsonObject): List<JsonObject> =
        v["libraries"]?.jsonArray.orEmpty()
            .map { it.jsonObject }
            .filter { rulesAllow(it["rules"]?.jsonArray) }
            .mapNotNull { it["downloads"]?.jsonObject?.get("artifact")?.jsonObject }

    /**
     * Simplified rule check: rules without an OS clause apply to everything,
     * and OS clauses are matched against "linux" (Android's kernel name in Mojang's data).
     * Mojang's native-library classifiers are not handled here; the runtime must supply natives.
     */
    private fun rulesAllow(rules: JsonArray?): Boolean {
        if (rules == null) return true
        var allowed = false
        for (r in rules) {
            val o = r.jsonObject
            val osName = o["os"]?.jsonObject?.get("name")?.jsonPrimitive?.content
            if (osName == null || osName == "linux") {
                allowed = o.str("action") == "allow"
            }
        }
        return allowed
    }

    private fun download(url: String, dest: File, sha1: String) {
        if (dest.exists() && sha1Of(dest) == sha1) return
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, dest.name + ".part")
        URL(url).openStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
        check(sha1Of(tmp) == sha1) { "Hash mismatch for ${dest.name}" }
        dest.delete()
        check(tmp.renameTo(dest)) { "Could not save ${dest.name}" }
    }

    private fun sha1Of(f: File): String {
        val md = MessageDigest.getInstance("SHA-1")
        f.inputStream().use { s ->
            val buf = ByteArray(8192)
            while (true) {
                val n = s.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
