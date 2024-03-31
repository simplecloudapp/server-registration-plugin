package app.simplecloud.plugin.registration.bungee

import app.simplecloud.plugin.registration.shared.ServerRegistrationPlugin
import net.md_5.bungee.api.plugin.Plugin

class BungeeServerRegistrationPlugin: Plugin() {
    private val plugin = ServerRegistrationPlugin(BungeeServerRegisterer())
    override fun onEnable() {
        plugin.start()
    }
}