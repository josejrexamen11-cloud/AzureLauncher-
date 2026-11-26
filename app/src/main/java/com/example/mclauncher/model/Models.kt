package com.example.mclauncher.model

import kotlinx.serialization.Serializable

@Serializable
data class OfflineAccount(
    val username: String,
    val uuid: String
)

@Serializable
enum class Renderer { OPENGL_ES, VULKAN_ZINK, SOFTWARE }

@Serializable
data class RenderSettings(
    val renderer: Renderer = Renderer.OPENGL_ES,
    val renderDistance: Int = 12,
    val maxFps: Int = 60,
    val resolutionScale: Float = 1.0f,
    val jvmArgs: String = "-Xmx2G"
)

@Serializable
data class Instance(
    val id: String,
    val name: String,
    val gameVersion: String,
    val loader: String = "vanilla",
    val render: RenderSettings = RenderSettings()
)
