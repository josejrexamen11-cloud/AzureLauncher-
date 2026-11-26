package com.example.mclauncher.launch

/**
 * Starts the game on-device. Implement this with the Java runtime you choose
 * (for example, a PojavLauncher-based engine). It receives the full launch config.
 */
interface RuntimeLauncher {
    fun start(config: LaunchConfig)
}

/** Placeholder until a runtime is connected. Launch reports this as an error. */
object UnconfiguredRuntime : RuntimeLauncher {
    override fun start(config: LaunchConfig) {
        error("No Java runtime is connected yet. Implement RuntimeLauncher with your runtime.")
    }
}
