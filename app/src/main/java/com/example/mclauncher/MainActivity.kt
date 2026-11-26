package com.example.mclauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.mclauncher.instance.InstanceManager
import com.example.mclauncher.launch.UnconfiguredRuntime
import com.example.mclauncher.mods.ModManager
import com.example.mclauncher.ui.MainScreen
import com.example.mclauncher.version.VersionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = InstanceManager(this)
        val versions = VersionManager(this)
        val mods = ModManager(this)
        setContent {
            MaterialTheme {
                MainScreen(manager, versions, mods, runtime = UnconfiguredRuntime)
            }
        }
    }
}
