package app.simplecloud.plugin.registration.bungee

import app.simplecloud.plugin.registration.shared.ServerRegistrationPlugin
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.plugin.Plugin

class BungeeServerRegistrationPlugin: Plugin() {
    private val plugin = ServerRegistrationPlugin(BungeeServerRegisterer())
    override fun onEnable() {
        ProxyServer.getInstance().configurationAdapter.servers.clear()
        ProxyServer.getInstance().servers.clear()
        for(info in ProxyServer.getInstance().configurationAdapter.listeners) {
            info.serverPriority.clear()
        }
        plugin.start(ProxyServer.getInstance().logger)
    }
}