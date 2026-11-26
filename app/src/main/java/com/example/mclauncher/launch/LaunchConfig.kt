package com.example.mclauncher.launch

import com.example.mclauncher.model.Instance
import com.example.mclauncher.model.OfflineAccount
import com.example.mclauncher.model.Renderer
import java.io.File

/** Everything the on-device Java runtime needs to start the game. */
data class LaunchConfig(
    val gameDir: File,
    val javaArgs: List<String>,
    val gameArgs: List<String>,
    val env: Map<String, String>
)

object LaunchBuilder {
    fun build(inst: Instance, account: OfflineAccount, instanceDir: File, runtimeHome: File): LaunchConfig {
        val r = inst.render

        val rendererEnv = when (r.renderer) {
            Renderer.OPENGL_ES -> mapOf("LIBGL_ES" to "3")
            Renderer.VULKAN_ZINK -> mapOf("MESA_LOADER_DRIVER_OVERRIDE" to "zink", "LIBGL_ES" to "3")
            Renderer.SOFTWARE -> mapOf("LIBGL_ALWAYS_SOFTWARE" to "1")
        }

        val javaArgs = buildList {
            addAll(r.jvmArgs.split(" ").filter { it.isNotBlank() })
            add("-Djava.library.path=${runtimeHome.absolutePath}/lib")
            add("-Duser.home=${instanceDir.absolutePath}")
        }

        val gameArgs = listOf(
            "--username", account.username,
            "--uuid", account.uuid.replace("-", ""),
            "--accessToken", "0",
            "--userType", "legacy",
            "--version", inst.gameVersion,
            "--gameDir", instanceDir.absolutePath,
            "--renderDistance", r.renderDistance.toString(),
            "--fps", r.maxFps.toString()
        )

        return LaunchConfig(
            gameDir = instanceDir,
            javaArgs = javaArgs,
            gameArgs = gameArgs,
            env = rendererEnv + ("RESOLUTION_SCALE" to r.resolutionScale.toString())
        )
    }
}
