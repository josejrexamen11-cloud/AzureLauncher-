package com.example.mclauncher.launch

import com.example.mclauncher.model.Instance
import com.example.mclauncher.model.OfflineAccount
import com.example.mclauncher.model.RenderSettings
import com.example.mclauncher.model.Renderer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/** Everything the on-device Java runtime needs to start the game. */
data class LaunchConfig(
    val gameDir: File,
    val javaArgs: List<String>,   // JVM flags, including -cp <classpath>
    val mainClass: String,
    val gameArgs: List<String>,
    val env: Map<String, String>
)

object LaunchBuilder {
    fun build(
        inst: Instance,
        account: OfflineAccount,
        instanceDir: File,
        minecraftRoot: File,
        versionJson: JsonObject,
        classpath: String
    ): LaunchConfig {
        val r = inst.render
        writeOptions(instanceDir, r)

        val rendererEnv = when (r.renderer) {
            Renderer.OPENGL_ES -> mapOf("LIBGL_ES" to "3")
            Renderer.VULKAN_ZINK -> mapOf("MESA_LOADER_DRIVER_OVERRIDE" to "zink", "LIBGL_ES" to "3")
            Renderer.SOFTWARE -> mapOf("LIBGL_ALWAYS_SOFTWARE" to "1")
        }

        val javaArgs = buildList {
            addAll(r.jvmArgs.split(" ").filter { it.isNotBlank() })
            add("-Duser.home=${instanceDir.absolutePath}")
            add("-cp")
            add(classpath)
        }

        val assetIndex = versionJson["assetIndex"]!!.jsonObject["id"]!!.jsonPrimitive.content
        val gameArgs = listOf(
            "--username", account.username,
            "--uuid", account.uuid.replace("-", ""),
            "--accessToken", "0",
            "--userType", "legacy",
            "--version", inst.gameVersion,
            "--gameDir", instanceDir.absolutePath,
            "--assetsDir", File(minecraftRoot, "assets").absolutePath,
            "--assetIndex", assetIndex
        )

        return LaunchConfig(
            gameDir = instanceDir,
            javaArgs = javaArgs,
            mainClass = versionJson["mainClass"]!!.jsonPrimitive.content,
            gameArgs = gameArgs,
            env = rendererEnv + ("RESOLUTION_SCALE" to r.resolutionScale.toString())
        )
    }

    /**
     * Render distance and FPS are game options, not launch flags. Vanilla rejects unknown
     * command-line arguments, so they are written to options.txt, keeping any other keys.
     */
    private fun writeOptions(instanceDir: File, r: RenderSettings) {
        val f = File(instanceDir, "options.txt")
        val map = linkedMapOf<String, String>()
        if (f.exists()) {
            f.readLines().forEach { line ->
                val i = line.indexOf(':')
                if (i > 0) map[line.substring(0, i)] = line.substring(i + 1)
            }
        }
        map["renderDistance"] = r.renderDistance.toString()
        map["maxFps"] = r.maxFps.toString()
        f.writeText(map.entries.joinToString("\n") { "${it.key}:${it.value}" } + "\n")
    }
}
