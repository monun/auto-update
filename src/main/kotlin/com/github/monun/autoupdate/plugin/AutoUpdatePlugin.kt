package com.github.monun.autoupdate.plugin

import com.github.monun.tap.config.Config
import com.github.monun.tap.config.RangeInt
import com.github.monun.tap.config.computeConfig
import net.kyori.adventure.identity.Identified
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * @author Monun
 */
class AutoUpdatePlugin : JavaPlugin(), Runnable {
    private lateinit var updates: MutableList<Pair<Plugin, File>>

    @Config
    private var updateAction = UpdateAction.RELOAD

    @Config
    @RangeInt(min = 0)
    private var countdownTicks = 20

    private var updateTicks = 0

    override fun onEnable() {
        computeConfig(File(dataFolder, "config.yml"))

        server.scheduler.let { scheduler ->
            scheduler.runTaskTimer(this, this, 1L, 1L)
            scheduler.runTask(this, Runnable {
                updates = server.pluginManager.plugins.mapTo(ArrayList()) { plugin ->
                    val file = plugin.file
                    val updateFolder = File(file.parentFile, "update")
                    plugin to File(updateFolder, file.name)
                }
            })
        }
    }

    override fun run() {
        if (updateTicks > 0 && --updateTicks <= 0) {
            val server = server
            if (updateAction == UpdateAction.RELOAD) {
                server.dispatchCommand(server.consoleSender, "reload confirm")
            } else {
                server.shutdown()
            }
        }

        updates.removeIf { update ->
            if (update.second.exists()) {
                server.sendMessage(Component.text("${ChatColor.LIGHT_PURPLE}Found update file for plugin ${update.first.name}."))
                server.sendMessage(Component.text("${ChatColor.LIGHT_PURPLE}Server will ${updateAction.name.toLowerCase()} in ${countdownTicks.div(20)} seconds."))
                updateTicks = countdownTicks
                true
            } else false
        }
    }
}

private val Plugin.file: File
    get() {
        return JavaPlugin::class.java.getDeclaredField("file").apply {
            isAccessible = true
        }.get(this) as File
    }

enum class UpdateAction {
    RELOAD,
    RESTART
}